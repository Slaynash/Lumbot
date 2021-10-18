package slaynash.lum.bot;

import java.awt.Color;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import mono.cecil.AssemblyDefinition;
import mono.cecil.FieldDefinition;
import mono.cecil.ModuleDefinition;
import mono.cecil.ReaderParameters;
import mono.cecil.ReadingMode;
import mono.cecil.TypeDefinition;
import slaynash.lum.bot.discord.JDAManager;
import slaynash.lum.bot.utils.ExceptionUtils;
import slaynash.lum.bot.utils.Utils;

public class UnityVersionMonitor {
    public static final String LOG_IDENTIFIER = "UnityVersionMonitor";

    private static final String hrefIdentifier = "<a href=\"https://download.unity3d.com/";

    private static final String downloadPath = "/mnt/hdd3t/unity_versions";

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .followRedirects(Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private static final List<UnityVersion> unityVersions = new ArrayList<>();

    private static final Map<String, List<String>> installedVersions = new HashMap<>();

    private static final List<UnityICall> icalls = new ArrayList<>() {
        {
            add(new UnityICall("UnityEngine.GL::get_sRGBWrite", "UnityEngine.CoreModule", "System.Boolean", new String[] {}));
            add(new UnityICall("UnityEngine.ImageConversion::LoadImage", "UnityEngine.ImageConversionModule", "System.Boolean", new String[] { "UnityEngine.Texture2D", "System.Byte[]", "System.Boolean" }));
            add(new UnityICall("UnityEngine.Graphics::Internal_DrawMeshNow1_Injected", "UnityEngine.CoreModule", "System.Void", new String[] { "UnityEngine.Mesh", "System.Int32", "ref UnityEngine.Vector3", "ref UnityEngine.Quaternion" }));
            add(new UnityICall("UnityEngine.Texture::GetDataWidth", "UnityEngine.CoreModule", "System.Int32", new String[] { "UnityEngine.Texture" }));
            add(new UnityICall("UnityEngine.Texture::GetDataHeight", "UnityEngine.CoreModule", "System.Int32", new String[] { "UnityEngine.Texture" }));
            add(new UnityICall("UnityEngine.Texture::set_filterMode", "UnityEngine.CoreModule", "System.Void", new String[] { "UnityEngine.FilterMode" }));
            add(new UnityICall("UnityEngine.Texture2D::SetPixelsImpl", "UnityEngine.CoreModule", "System.Void", new String[] { "System.Texture2D", "System.Int32", "System.Int32", "System.Int32", "System.Int32", "UnityEngine.Color[]", "System.Int32", "System.Int32" }));
            add(new UnityICall("UnityEngine.TextGenerator::get_vertexCount", "UnityEngine.TextRenderingModule", "System.Int32", new String[] { "UnityEngine.TextGenerator" }));
            add(new UnityICall("UnityEngine.TextGenerator::GetVerticesArray", "UnityEngine.TextRenderingModule", "System.UIVertex[]", new String[] { "UnityEngine.TextGenerator" }));
        }
    };

    public static void start() {

        loadInstalledVersionCache();

        Thread thread = new Thread(() -> {

            boolean firstRun = true;

            while (true) {

                if (firstRun)
                    firstRun = false;
                else
                    try {
                        Thread.sleep(60 * 60 * 1000);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }

                try {
                    // fetch unity versions

                    HttpRequest request = HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create("https://unity3d.com/get-unity/download/archive"))
                        .setHeader("User-Agent", "LUM Bot")
                        .timeout(Duration.ofSeconds(30))
                        .build();

                    String pagedata;

                    try {
                        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                        pagedata = response.body();
                    }
                    catch (Exception e) {
                        ExceptionUtils.reportException("Failed to fetch Unity versions", e);
                        continue;
                    }

                    String[] pageLines = pagedata.split("[\r\n]");
                    for (String line : pageLines) {
                        if (line.isEmpty() || line.contains("Samsung"))
                            continue;

                        int hrefIdentifierIndex;
                        if ((hrefIdentifierIndex = line.indexOf(hrefIdentifier)) < 0)
                            continue;

                        String setupIdentifier = "UnitySetup64-";
                        if (!line.contains(setupIdentifier) && !line.contains(setupIdentifier = "UnitySetup-"))
                            continue;

                        String subline = line.substring(hrefIdentifierIndex + hrefIdentifier.length());
                        String foundUrl = "https://download.unity3d.com/" + subline.split("\"", 2)[0];
                        int extensionIndex;
                        if ((extensionIndex = subline.indexOf(".exe")) < 0)
                            continue;

                        String foundVersion = subline.substring(0, extensionIndex);
                        foundVersion = foundVersion.substring(foundVersion.lastIndexOf(setupIdentifier) + setupIdentifier.length());

                        String fullVersion = foundVersion;
                        if (foundVersion.contains("f"))
                            foundVersion = foundVersion.split("f")[0];

                        String urlIl2CppWin = null;
                        if (foundVersion.startsWith("20")) {
                            String versionId = subline.split("/")[1];
                            if (foundVersion.startsWith("2017"))
                                foundUrl = "https://download.unity3d.com/download_unity/" + versionId + "/MacEditorTargetInstaller/UnitySetup-Windows-Support-for-Editor-" + fullVersion + ".pkg";
                            else
                                foundUrl = "https://download.unity3d.com/download_unity/" + versionId + "/MacEditorTargetInstaller/UnitySetup-Windows-Mono-Support-for-Editor-" + fullVersion + ".pkg";

                            if (!foundVersion.startsWith("2017"))
                                urlIl2CppWin = "https://download.unity3d.com/download_unity/" + versionId + "/TargetSupportInstaller/UnitySetup-Windows-IL2CPP-Support-for-Editor-" + fullVersion + ".exe";
                        }

                        if (foundVersion.startsWith("5.3")) // We don't care about versions earlier than 5.4.0
                            break;

                        unityVersions.add(new UnityVersion(foundVersion, fullVersion, foundUrl, urlIl2CppWin));
                    }

                    List<UnityVersion> newVersions = new ArrayList<>();

                    for (UnityVersion unityVersion : unityVersions)
                        if (!installedVersions.containsKey(unityVersion.version))
                            newVersions.add(unityVersion);

                    System.out.println("unity3d.com returned " + newVersions.size() + " new versions");

                    if (newVersions.size() > 0 && newVersions.size() < 10) {
                        StringBuilder message = new StringBuilder("New Unity version published:");
                        for (UnityVersion newVersion : newVersions)
                            message.append("\n - ").append(newVersion.version);
                        JDAManager.getJDA().getGuildById(633588473433030666L /* Slaynash's Workbench */).getTextChannelById(876466104036393060L /* #lum-status */).sendMessage(message.toString()).queue();
                    }

                    for (UnityVersion newVersion : newVersions)
                        downloadUnity(newVersion);

                    // run tools sanity checks

                    for (UnityVersion newVersion : newVersions) {
                        runHashChecker(newVersion.version);
                        runICallChecker(newVersion.version);
                        runMonoStructChecker(newVersion.version);
                        // VFTables Checker
                    }
                }
                catch (Exception e) {
                    ExceptionUtils.reportException("Unhandled exception in UnityVersionMonitor", e);
                }
            }

        }, "UnityVersionMonitor");
        thread.setDaemon(true);
        thread.start();
    }

    private static void downloadUnity(UnityVersion uv) {
        File targetFile = new File(downloadPath + "/" + uv.version);
        File targetFileTmp = new File(downloadPath + "/" + uv.version + "_tmp");
        if (targetFile.exists()) {
            try {
                Files.walk(targetFile.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
            catch (IOException e) {
                ExceptionUtils.reportException("Failed to delete unity folder " + uv.version, e);
                return;
            }
        }

        if (targetFileTmp.exists()) {
            try {
                Files.walk(targetFileTmp.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
            catch (IOException e) {
                ExceptionUtils.reportException("Failed to delete unity temp folder " + uv.version, e);
                return;
            }
        }

        List<String> installedArchitectures = installedVersions.get(uv.version);

        if (installedArchitectures == null || !installedArchitectures.contains("windows mono")) {

            System.out.println("Downloading " + uv.downloadUrl);
            try (
                FileOutputStream fileOutputStream = new FileOutputStream("unityversionsmonitor/unitydownload_" + uv.version + ".dat");
                FileChannel fileChannel = fileOutputStream.getChannel()
            ) {
                ReadableByteChannel readableByteChannel = Channels.newChannel(new URL(uv.downloadUrl).openStream());
                fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            }
            catch (Exception e) {
                ExceptionUtils.reportException("Failed to download unity version " + uv.version + " (mono)", e);
                return;
            }

            extractFilesFromArchive(uv, false, false);

            saveInstalledVersionCache(uv.version, "windows mono");

        }

        if ((installedArchitectures == null || !installedArchitectures.contains("windows il2cpp")) && uv.downloadUrlIl2CppWin != null) {

            System.out.println("Downloading " + uv.downloadUrlIl2CppWin);
            try (
                FileOutputStream fileOutputStream = new FileOutputStream("unityversionsmonitor/unitydownload_" + uv.version + ".dat");
                FileChannel fileChannel = fileOutputStream.getChannel()
            ) {
                ReadableByteChannel readableByteChannel = Channels.newChannel(new URL(uv.downloadUrlIl2CppWin).openStream());
                fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            }
            catch (Exception e) {
                ExceptionUtils.reportException("Failed to download unity version " + uv.version + " (il2cpp)", e);
                return;
            }

            boolean useNSISBIExtractor = (uv.version.startsWith("2020") && !uv.version.startsWith("2020.1")) || uv.version.startsWith("2021");
            extractFilesFromArchive(uv, true, useNSISBIExtractor);

            saveInstalledVersionCache(uv.version, "windows il2cpp");
        }

        try {
            Files.walk(targetFileTmp.toPath())
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
        catch (IOException e) {
            ExceptionUtils.reportException("Failed to delete unity temp folder " + uv.version, e);
            return;
        }

        new File("unitydownload_" + uv.version + ".dat").delete();
        new File("Payload~").delete();
    }

    public static void loadInstalledVersionCache() {
        try {
            System.out.println("Loading versions cache");
            installedVersions.putAll(gson.fromJson(Files.readString(Paths.get("unityversionsmonitor/unityInstallCache.json")), new TypeToken<HashMap<String, ArrayList<String>>>(){}.getType()));
            System.out.println("Done loading versions cache");
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to load unity installation cache", e);
        }
    }

    public static void saveInstalledVersionCache(String unityVersion, String architecture) {
        List<String> installedArchitectures = installedVersions.get(unityVersion);
        if (installedArchitectures == null)
            installedVersions.put(unityVersion, installedArchitectures = new ArrayList<>());
        installedArchitectures.add(architecture);

        try {
            Files.write(Paths.get("unityversionsmonitor/unityInstallCache.json"), gson.toJson(installedVersions).getBytes());
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to save unity installation cache", e);
        }
    }

    public static void extractFilesFromArchive(UnityVersion version, boolean isil2cpp, boolean useNSISExtractor) {
        String internalPath = "Variations";
        String monoManagedSubpath = getMonoManagedSubpath(version.version);

        if (version.version.startsWith("3.")) {
            internalPath = "Data/PlaybackEngines/";
        }
        else if (version.version.startsWith("4.")) {
            if (version.version.startsWith("4.5") ||
                version.version.startsWith("4.6") ||
                version.version.startsWith("4.7")) {
                internalPath = "Data/PlaybackEngines/windowsstandalonesupport/Variations";
            }
            else {
                internalPath = "Data/PlaybackEngines/";
            }
        }
        else if (version.version.startsWith("5.")) {
            if (version.version.startsWith("5.3")) {
                internalPath = "Editor/Data/PlaybackEngines/WebPlayer/";
            }
            else {
                internalPath = "Editor/Data/PlaybackEngines/windowsstandalonesupport/Variations";
            }
        }


        String internalPathZip;
        if (useNSISExtractor) {
            internalPathZip = "\\\\$_OUTDIR/Variations/(.*_il2cpp/UnityPlayer.*(dll|pdb)|" + monoManagedSubpath + "/.*dll)";
        }
        else {
            internalPathZip = version.version.startsWith("20") ? (version.version.startsWith("2017.1") ? "./" : (isil2cpp ? "\\$INSTDIR\\$*/" : "./")) : "";
            internalPathZip += internalPath;
            internalPathZip = "\"" + internalPathZip + (version.version.startsWith("20") && !version.version.startsWith("2017.1") ? "/*/UnityPlayer.dll" : "/*/*.exe") + "\" \"" + internalPathZip + "/*/UnityPlayer*.pdb\" \"" + internalPathZip + "/" + monoManagedSubpath + "/*.dll\"";
        }

        System.out.println("Extracting DLLs from Archive");
        if (!new File(downloadPath).exists())
            new File(downloadPath).mkdir();
        try {
            if (!extractFiles(downloadPath + "/" + version.version + "_tmp", "unityversionsmonitor/unitydownload_" + version.version + ".dat", internalPathZip, !isil2cpp && version.version.startsWith("20"), useNSISExtractor, true)) {
                ExceptionUtils.reportException("Failed to extract Unity version " + version.version + " (" + (isil2cpp ? "il2cpp" : "mono") + ")");
                return;
            }
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to extract Unity version " + version.version + " (" + (isil2cpp ? "il2cpp" : "mono") + ")", e);
            return;
        }
        String tomoveFolder = downloadPath + "/" + version.version + "_tmp";
        if (isil2cpp)
            tomoveFolder = new File(tomoveFolder).listFiles(File::isDirectory)[0].getPath();
        tomoveFolder += "/" + internalPath;
        moveDirectory(new File(tomoveFolder), new File(downloadPath + "/" + version.version));
    }

    private static boolean extractFiles(String outputPath, String zipPath, String internalPath, boolean isPkg, boolean useNSISBIExtractor, boolean keepFilePath) throws IOException, InterruptedException {
        if (useNSISBIExtractor)
            return runProgram("UnityNSISReader", "sh", "-c", "mono unityversionsmonitor/UnityNSISReader.exe \"-f" + zipPath + "\" \"-o" + outputPath + "\" \"-r" + internalPath + "\"") == 0;

        if (isPkg) {
            if (runProgram("7z", "sh", "-c", "7z " + (keepFilePath ? "x" : "e") + " \"" + zipPath + "\" \"Payload~\" -y") != 0)
                return false;

            return runProgram("7z", "sh", "-c", "7z " + (keepFilePath ? "x" : "e") + " \"Payload~\" -o\"" + outputPath + "\" " + internalPath + " -y") == 0;
        }

        return runProgram("7z", "sh", "-c", "7z " + (keepFilePath ? "x" : "e") + " \"" + zipPath + "\" -o\"" + outputPath + "\" " + internalPath + " -y") == 0;
    }

    private static int runProgram(String name, String... command) throws IOException, InterruptedException {
        String printCmd = "";
        for (String param : command)
            printCmd += "\"" + param.replace("\"", "\\\"") + "\" ";
        System.out.println("Running command: " + printCmd);
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line = "";
            while ((line = br.readLine()) != null)
                System.out.println("[" + name + "] " + line);
        }

        return p.waitFor();
    }

    private static void moveDirectory(File src, File dest) {
        if (!dest.exists())
            dest.mkdirs();
        String targetDirPath = dest.getAbsolutePath();
        File[] files = src.listFiles();
        for (File file : files)
            file.renameTo(new File(targetDirPath + File.separator + file.getName()));
    }

    private static void runHashChecker(String unityVersion)  throws IOException, InterruptedException {
        Map<String, Map<String, Integer>> results = null;

        System.out.println("Running command: \"sh\" \"-c\" \"mono unityversionsmonitor/HashChecker.exe --uv=" + unityVersion + " --nhro\"");
        ProcessBuilder pb = new ProcessBuilder("sh", "-c", "mono unityversionsmonitor/HashChecker.exe --uv=" + unityVersion + " --nhro");
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line = "";
            while ((line = br.readLine()) != null) {
                System.out.println("[HashChecker] " + line);
                if (line.startsWith("RESULT_")) {
                    String[] resultParts = line.substring("RESULT_".length()).split(" ", 2);
                    results = gson.fromJson(resultParts[1], new TypeToken<HashMap<String, HashMap<String, Integer>>>(){}.getType());
                }
            }
        }

        p.waitFor();

        if (results == null) {
            JDAManager.getJDA().getGuildById(633588473433030666L /* Slaynash's Workbench */).getTextChannelById(876466104036393060L /* #lum-status */).sendMessageEmbeds(
                Utils.wrapMessageInEmbed("HashChecker has reported no result for Unity " + unityVersion, Color.red)
            ).queue();
            return;
        }

        String reports = "";
        for (Entry<String, Map<String, Integer>> arch : results.entrySet()) {
            for (Entry<String, Integer> hash : arch.getValue().entrySet()) {
                if (hash.getValue() > 1)
                    reports += arch.getKey() + " - " + hash.getKey() + ": " + hash.getValue() + " results\n";
                else if (hash.getValue() == 0)
                    reports += arch.getKey() + " - " + hash.getKey() + ": Hash not valid\n";
                else if (hash.getValue() == -1)
                    reports += arch.getKey() + " - " + hash.getKey() + ": No hash for this version\n";
                else if (hash.getValue() == -2)
                    reports += arch.getKey() + " - " + hash.getKey() + ": File not found\n";
            }
        }

        if (reports.length() > 0) {
            JDAManager.getJDA().getGuildById(633588473433030666L /* Slaynash's Workbench */).getTextChannelById(876466104036393060L /* #lum-status */).sendMessageEmbeds(
                Utils.wrapMessageInEmbed("Failed to validate all hashes for Unity " + unityVersion + ":\n\n" + reports, Color.red)
            ).queue();
        }
        else {
            JDAManager.getJDA().getGuildById(633588473433030666L /* Slaynash's Workbench */).getTextChannelById(876466104036393060L /* #lum-status */).sendMessageEmbeds(
                Utils.wrapMessageInEmbed("Hash check succeeded for Unity " + unityVersion, Color.green)
            ).queue();
        }
    }

    private static final byte[] unityStringStart = "UnityEng".getBytes(StandardCharsets.UTF_8);
    public static void runICallChecker(String unityVersion) throws IOException {

        // TODO ICall Check
        // 1. Lookup for the word in UnityPlayer.dll
        List<UnityICall> icalls = new ArrayList<UnityICall>(UnityVersionMonitor.icalls); // We cache the icall list to avoid ConcurrentModificationExceptions
        boolean[] icallFounds = new boolean[icalls.size()];
        int icallFoundCount = 0;
        byte[] fileData = Files.readAllBytes(new File(downloadPath + "/" + unityVersion + "/win64_nondevelopment_mono/UnityPlayer.dll").toPath());
        System.out.println("fileData.length: " + fileData.length);
        int remainingDataLength = fileData.length;
        boolean insideUnityEngineStrings = false;
        for (int i = 0; i < fileData.length - 8; i += 8, remainingDataLength -= 8) {
            if (!insideUnityEngineStrings) {
                if (Arrays.equals(fileData, i, i + 8, unityStringStart, 0, 8)) {
                    System.out.println("startOfUnityEngineStrings is at offset " + i);
                    insideUnityEngineStrings = true;
                }
                else
                    continue;
            }

            if (insideUnityEngineStrings) {
                for (int j = 0; j < icalls.size(); ++j) {
                    UnityICall icall = icalls.get(j);
                    int icallUtf8Length = icall.icallUtf8.length;

                    if (remainingDataLength >= icallUtf8Length && Arrays.equals(fileData, i, i + icallUtf8Length, icall.icallUtf8, 0, icallUtf8Length)) {
                        if (!icallFounds[j]) {
                            System.out.println("Icall " + icall.icall + " found at offset " + i);
                            icallFounds[j] = true;
                            ++icallFoundCount;
                            
                            if (icallFoundCount == icalls.size())
                                break;
                        }
                    }
                }
            }
        }

        System.out.println("Found " + icallFoundCount + " / " + icalls.size() + " icalls");
        if (icallFoundCount == icalls.size()) {
            JDAManager.getJDA().getGuildById(633588473433030666L /* Slaynash's Workbench */).getTextChannelById(876466104036393060L /* #lum-status */).sendMessageEmbeds(
                Utils.wrapMessageInEmbed("ICall check succeeded for Unity " + unityVersion, Color.green)
            ).queue();
        }
        else {
            String reports = "```";
            for (int i = 0; i < icallFounds.length; ++i) {
                if (!icallFounds[i])
                    reports += "\n" + icalls.get(i).icall;
            }
            reports += "```";

            JDAManager.getJDA().getGuildById(633588473433030666L /* Slaynash's Workbench */).getTextChannelById(876466104036393060L /* #lum-status */).sendMessageEmbeds(
                Utils.wrapMessageInEmbed("Failed to validate the following icalls for Unity " + unityVersion + ":\n\n" + reports, Color.red)
            ).queue();
        }
        
        // 2. Check if the signature matches in the Unity Managed Assemblies
        String unityManaged = downloadPath + "/" + unityVersion + "/" + getMonoManagedSubpath(unityVersion);
        // 3. Send result
    }

    public static void runMonoStructChecker(String unityVersion) {
        // TODO MonoStruct checker

        // 1. Fetch struct (json array:field of object:{string:type, string:name} - [{"Type1", "field1"}, {"Type2": "field2"}, ...])

        AssemblyDefinition ad = AssemblyDefinition.readAssembly(downloadPath + "/" + unityVersion + "/" + getMonoManagedSubpath(unityVersion) + "/" + "UnityEngine.CoreModule" + ".dll", new ReaderParameters(ReadingMode.Deferred, new CecilAssemblyResolverProvider.AssemblyResolver()));
        ModuleDefinition mainModule = ad.getMainModule();

        TypeDefinition typeDefinition = mainModule.getType("UnityEngine.Internal_DrawTextureArguments");
        if (typeDefinition == null) {
            JDAManager.getJDA().getGuildById(633588473433030666L /* Slaynash's Workbench */).getTextChannelById(876466104036393060L /* #lum-status */).sendMessageEmbeds(
                Utils.wrapMessageInEmbed("Failed to validate the following structs for Unity " + unityVersion + ":\n\n" + "Internal_DrawTextureArguments", Color.red)
            ).queue();
            return;
        }

        List<String> fields = new ArrayList<>();

        for (FieldDefinition fieldDef : typeDefinition.getFields()) {
            fields.add(fieldDef.getFieldType().getFullName() + " " + fieldDef.getName());
        }

        ad.dispose();


        /* JSONC
        [
            {
                "minimalUnityVersion": ["2017.2.0", "2018.1.0"],
                "fields": [
                    {"UnityEngine.Rect", "screenRect"},
                    {"UnityEngine.Rect", "sourceRect"}
                    // ...
                ]
            }
            // ...
        ]
        */

        // 2. Get struct of latest version and compare
        // 3. Report change if not matching, else report OK

        String report = "";
        
        report += "Internal_DrawTextureArguments\n```\n";
        for (String field : fields)
            report += field + "\n";
        report += "```";

        JDAManager.getJDA().getGuildById(633588473433030666L /* Slaynash's Workbench */).getTextChannelById(876466104036393060L /* #lum-status */).sendMessageEmbeds(
            Utils.wrapMessageInEmbed("The following structs were found for Unity " + unityVersion + ":\n\n" + report, Color.gray)
        ).queue();
    }

    private static String getMonoManagedSubpath(String version) {
        String monoManagedSubpath = "win64_nondevelopment_mono/Data";

        if (version.startsWith("3.")) {
            monoManagedSubpath = "windows64standaloneplayer";
        }
        else if (version.startsWith("4.")) {
            if (version.startsWith("4.5") ||
                version.startsWith("4.6") ||
                version.startsWith("4.7")) {
                monoManagedSubpath = "win64_nondevelopment/Data";
            }
            else {
                monoManagedSubpath = "windows64standaloneplayer";
            }
        }
        else if (version.startsWith("5.")) {
            if (version.startsWith("5.3")) {
                monoManagedSubpath = "win64_nondevelopment_mono/Data";
            }
            else {
                monoManagedSubpath = "win64_nondevelopment/Data";
            }
        }

        return monoManagedSubpath + "/Managed";
    }

    private static class UnityVersion {
        public final String version;
        public final String fullVersion;
        public final String downloadUrl;
        public final String downloadUrlIl2CppWin;

        public UnityVersion(String version, String fullVersion, String downloadUrl, String downloadUrlIl2CppWin) {
            this.version = version;
            this.fullVersion = fullVersion;
            this.downloadUrl = downloadUrl;
            this.downloadUrlIl2CppWin = downloadUrlIl2CppWin;
        }
    }

    private static class UnityICall {
        public String icall;
        public byte[] icallUtf8;
        public String assemblyName;
        public String returnType;
        public String[] parameters;

        public UnityICall(String icall, String assemblyName, String returnType, String[] parameters) {
            this.icall = icall;
            this.icallUtf8 = icall.getBytes(StandardCharsets.UTF_8);
            this.assemblyName = assemblyName;
            this.returnType = returnType;
            this.parameters = parameters;
        }
    }

}
