package slaynash.lum.bot.uvm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import slaynash.lum.bot.ConfigManager;
import slaynash.lum.bot.discord.JDAManager;
import slaynash.lum.bot.utils.ExceptionUtils;

public class UnityDownloader {

    private static final HttpClient httpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .followRedirects(Redirect.ALWAYS)
        .connectTimeout(Duration.ofSeconds(30))
        .build();

    private static final int maxVersions = 100;
    private static final HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("https://services.unity.com/graphql"))
        .header("Content-Type", "application/json")
        .method("POST", HttpRequest.BodyPublishers.ofString("{\"query\":\"query GetVersions($limit:Int!,$skip:Int!){getUnityReleases(limit:$limit,skip:$skip,entitlements:[XLTS]){pageInfo{hasNextPage}edges{node{version,shortRevision,releaseDate,unityHubDeepLink,stream}}}}\",\"operationName\":\"GetVersions\",\"variables\":{\"limit\": " + maxVersions + ",\"skip\": 0}}"))
        .setHeader("User-Agent", "LUM Bot " + ConfigManager.commitHash)
        .timeout(Duration.ofSeconds(30))
        .build();

    private static final Map<String, List<String>> installedVersions = new HashMap<>();


    public static void loadInstalledVersionCache() {
        try {
            System.out.println("Loading versions cache");
            installedVersions.clear();
            installedVersions.putAll(UnityUtils.gson.fromJson(Files.readString(Paths.get("unityversionsmonitor/unityInstallCache.json")), new TypeToken<HashMap<String, ArrayList<String>>>(){}.getType()));
            System.out.println("Done loading versions cache");
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to load unity installation cache", e);
        }
    }

    public static void saveInstalledVersionCache(String unityVersion, String architecture) {
        List<String> installedArchitectures = installedVersions.computeIfAbsent(unityVersion, k -> new ArrayList<>());
        installedArchitectures.add(architecture);

        saveInstalledVersionCache();
    }

    public static void saveInstalledVersionCache() {
        try {
            Files.write(Paths.get("unityversionsmonitor/unityInstallCache.json"), UnityUtils.gson.toJson(installedVersions).getBytes());
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to save unity installation cache", e);
        }
    }



    public static List<UnityVersion> fetchUnityVersions() throws InterruptedException {
        JsonObject json;

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            json = JsonParser.parseString(new String(response.body())).getAsJsonObject();
        }
        catch (IOException e) {
            ExceptionUtils.reportException("Failed to fetch Unity versions", e);
            return null;
        }

        List<UnityVersion> unityVersions = new ArrayList<>();

        for (JsonElement element : json.getAsJsonObject("data").getAsJsonObject("getUnityReleases").getAsJsonArray("edges")) {
            JsonObject node = element.getAsJsonObject().get("node").getAsJsonObject();
            String foundUrl;
            String foundVersion = node.get("version").getAsString();
            String versionId = node.get("shortRevision").getAsString();
            String stream = node.get("stream").getAsString();

            String fullVersion = foundVersion;
            if (foundVersion.contains("f"))
                foundVersion = foundVersion.split("f")[0];

            String urlIl2CppWin = null;

            if (foundVersion.startsWith("20") || foundVersion.startsWith("6000")) {
                if (foundVersion.startsWith("2017.1"))
                    continue;

                if (foundVersion.startsWith("2017.2")) {
                    foundUrl = "https://beta.unity3d.com/download/" + versionId + "/MacEditorTargetInstaller/UnitySetup-Windows-Support-for-Editor-" + fullVersion + ".pkg";
                }
                else if (foundVersion.startsWith("2017")) {
                    foundUrl = "https://download.unity3d.com/download_unity/" + versionId + "/MacEditorTargetInstaller/UnitySetup-Windows-Support-for-Editor-" + fullVersion + ".pkg";
                }
                else {
                    foundUrl = "https://download.unity3d.com/download_unity/" + versionId + "/MacEditorTargetInstaller/UnitySetup-Windows-Mono-Support-for-Editor-" + fullVersion + ".pkg";
                    urlIl2CppWin = "https://download.unity3d.com/download_unity/" + versionId + "/TargetSupportInstaller/UnitySetup-Windows-IL2CPP-Support-for-Editor-" + fullVersion + ".exe";
                }
            }
            else
                continue;

            boolean alreadyHasVersion = false;
            for (UnityVersion uv : unityVersions) {
                if (uv.version.equals(foundVersion)) {
                    alreadyHasVersion = true;
                    break;
                }
            }

            if (!alreadyHasVersion)
                unityVersions.add(new UnityVersion(stream, foundVersion, fullVersion, foundUrl, urlIl2CppWin));
        }

        return unityVersions;
    }

    public static void filterNewVersionsAndLog(List<UnityVersion> versions) {
        for (int i = versions.size() - 1; i >= 0; i--) {
            if (installedVersions.containsKey(versions.get(i).version))
                versions.remove(i);
        }

        System.out.println("unity3d.com returned " + versions.size() + " new versions");

        if (!versions.isEmpty()) {
            StringBuilder message = new StringBuilder("New Unity version published:");
            for (UnityVersion newVersion : versions.subList(0, Math.min(20, versions.size()))) {
                String type = "whats-new/";
                if (newVersion.fullVersion.contains("a")) type = "alpha/";
                if (newVersion.fullVersion.contains("b")) type = "beta/";
                message.append("\n- ").append(newVersion.version).append(" ").append(newVersion.stream).append(" [Release Notes](<https://unity.com/releases/editor/").append(type).append(newVersion.version).append("#notes>)");
            }
            JDAManager.getJDA().getTextChannelById(876466104036393060L /* #lum-status */).sendMessage(message.toString()).queue();  // may want to move this over to just #unity-version-updates
            JDAManager.getJDA().getNewsChannelById(979786573010833418L /* #unity-version-updates */).sendMessage(message.toString()).queue(s -> s.crosspost().queue());
        }
    }

    public static void downloadUnity(UnityVersion uv) throws InterruptedException {

        File targetFile = new File(UnityUtils.downloadPath + "/" + uv.version);
        File targetFileTmp = new File(UnityUtils.downloadPath + "/" + uv.version + "_tmp");
        installedVersions.remove(uv.version);
        if (targetFile.exists()) {
            try (Stream<Path> filesToDelete = Files.walk(targetFile.toPath())) {
                filesToDelete.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
                saveInstalledVersionCache();
            }
            catch (IOException e) {
                ExceptionUtils.reportException("Failed to delete unity folder " + uv.version, e);
                return;
            }
        }

        if (targetFileTmp.exists()) {
            try (Stream<Path> filesToDelete = Files.walk(targetFileTmp.toPath())) {
                filesToDelete.sorted(Comparator.reverseOrder())
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
                FileChannel fileChannel = fileOutputStream.getChannel())
            {
                ReadableByteChannel readableByteChannel = Channels.newChannel(new URL(uv.downloadUrl).openStream());
                fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            }
            catch (IOException e) {
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
                FileChannel fileChannel = fileOutputStream.getChannel())
            {
                ReadableByteChannel readableByteChannel = Channels.newChannel(new URL(uv.downloadUrlIl2CppWin).openStream());
                fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            }
            catch (IOException e) {
                ExceptionUtils.reportException("Failed to download unity version " + uv.version + " (il2cpp)", e);
                return;
            }

            boolean useNSISBIExtractor = uv.version.startsWith("202") && !uv.version.startsWith("2020.1");
            extractFilesFromArchive(uv, true, useNSISBIExtractor);

            saveInstalledVersionCache(uv.version, "windows il2cpp");
        }

        try (Stream<Path> filesToDelete = Files.walk(targetFileTmp.toPath())) {
            filesToDelete.sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
        catch (IOException e) {
            ExceptionUtils.reportException("Failed to delete unity temp folder " + uv.version, e);
            return;
        }
        finally {
            new File("unityversionsmonitor/unitydownload_" + uv.version + ".dat").delete();
            new File("unityversionsmonitor/Payload~").delete();
        }
    }

    public static void extractFilesFromArchive(UnityVersion version, boolean isil2cpp, boolean useNSISExtractor) throws InterruptedException {
        String internalPath = "Variations";
        String monoManagedSubpath = UnityUtils.getMonoManagedSubpath(version.version);

        if (version.version.startsWith("3.")) {
            internalPath = "Data/PlaybackEngines/";
        }
        else if (version.version.startsWith("4.")) {
            if (version.version.startsWith("4.5") ||
                version.version.startsWith("4.6") ||
                version.version.startsWith("4.7"))
            {
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
        if (!new File(UnityUtils.downloadPath).exists())
            if (!(new File(UnityUtils.downloadPath).mkdir())) {
                ExceptionUtils.reportException("Failed to create UVM directory");
                return;
            }
        try {
            if (!extractFiles(UnityUtils.downloadPath + "/" + version.version + "_tmp", "unityversionsmonitor/unitydownload_" + version.version + ".dat", internalPathZip, !isil2cpp && version.version.startsWith("20"), useNSISExtractor, true)) {
                ExceptionUtils.reportException("Failed to extract Unity version " + version.version + " (" + (isil2cpp ? "il2cpp" : "mono") + ")");
                return;
            }
        }
        catch (IOException e) {
            ExceptionUtils.reportException("Failed to extract Unity version " + version.version + " (" + (isil2cpp ? "il2cpp" : "mono") + ")", e);
            return;
        }
        String tomoveFolder = UnityUtils.downloadPath + "/" + version.version + "_tmp";
        if (isil2cpp)
            tomoveFolder = new File(tomoveFolder).listFiles(File::isDirectory)[0].getPath();
        tomoveFolder += "/" + internalPath;
        System.out.println("Moving " + tomoveFolder + " to " + UnityUtils.downloadPath + "/" + version.version);
        moveDirectory(new File(tomoveFolder), new File(UnityUtils.downloadPath + "/" + version.version));
    }

    private static boolean extractFiles(String outputPath, String zipPath, String internalPath, boolean isPkg, boolean useNSISBIExtractor, boolean keepFilePath) throws IOException, InterruptedException {
        if (useNSISBIExtractor)
            return runProgram("UnityNSISReader", "sh", "-c", "mono unityversionsmonitor/UnityNSISReader.exe \"-f" + zipPath + "\" \"-o" + outputPath + "\" \"-r" + internalPath + "\"") == 0;

        if (isPkg) {
            if (runProgram("7z", "sh", "-c", "7z " + (keepFilePath ? "x" : "e") + " \"" + zipPath + "\" -ounityversionsmonitor \"Payload~\" -y") != 0)
                return false;

            return runProgram("7z", "sh", "-c", "7z " + (keepFilePath ? "x" : "e") + " \"unityversionsmonitor/Payload~\" -o\"" + outputPath + "\" " + internalPath + " -y") == 0;
        }

        return runProgram("7z", "sh", "-c", "7z " + (keepFilePath ? "x" : "e") + " \"" + zipPath + "\" -o\"" + outputPath + "\" " + internalPath + " -y") == 0;
    }

    private static int runProgram(String name, String... command) throws IOException, InterruptedException {
        StringBuilder printCmd = new StringBuilder();
        for (String param : command)
            printCmd.append("\"").append(param.replace("\"", "\\\"")).append("\" ");
        System.out.println("Running command: " + printCmd);
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
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
        if (files == null)
            return;
        for (File file : files)
            file.renameTo(new File(targetDirPath + File.separator + file.getName()));
    }
}
