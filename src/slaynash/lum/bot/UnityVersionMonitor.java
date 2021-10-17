package slaynash.lum.bot;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import slaynash.lum.bot.discord.JDAManager;
import slaynash.lum.bot.utils.ExceptionUtils;

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

    private static Map<String, List<String>> installedVersions = new HashMap<>();

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
                FileOutputStream fileOutputStream = new FileOutputStream("unitydownload_" + uv.version + ".dat");
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
            try (
                FileOutputStream fileOutputStream = new FileOutputStream("unitydownload_" + uv.version + ".dat");
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
            installedVersions = gson.fromJson(Files.readString(Paths.get("unityInstallCache.json")), new TypeToken<HashMap<String, ArrayList<String>>>(){}.getType());
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
            Files.write(Paths.get("unityInstallCache.json"), gson.toJson(installedVersions).getBytes());
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to save unity installation cache", e);
        }
    }

    public static void extractFilesFromArchive(UnityVersion version, boolean isil2cpp, boolean useNSISExtractor) {
        String internalPath = "Variations";

        if (version.version.startsWith("3."))
            internalPath = "Data/PlaybackEngines";
        else if (version.version.startsWith("4.")) {
            if (version.version.startsWith("4.5") ||
                version.version.startsWith("4.6") ||
                version.version.startsWith("4.7"))
                internalPath = "Data/PlaybackEngines/windowsstandalonesupport/Variations";
            else
                internalPath = "Data/PlaybackEngines/";
        }
        else if (version.version.startsWith("5.")) {
            if (version.version.startsWith("5.3"))
                internalPath = "Editor/Data/PlaybackEngines/WebPlayer/";
            else
                internalPath = "Editor/Data/PlaybackEngines/windowsstandalonesupport/Variations";
        }


        String internalPathZip;
        if (useNSISExtractor) {
            internalPathZip = "\\$_OUTDIR/Variations/.*_il2cpp/UnityPlayer.*(dll|pdb)";
        }
        else {
            internalPathZip = version.version.startsWith("20") ? (version.version.startsWith("2017.1") ? "./" : (isil2cpp ? "\\$INSTDIR\\$*/" : "./")) : "";
        }
        internalPathZip += internalPath;
        internalPathZip = "\"" + internalPathZip + (version.version.startsWith("20") && !version.version.startsWith("2017.1") ? "/*/UnityPlayer.dll" : "/*/*.exe") + "\" \"" + internalPathZip + "/*/UnityPlayer*.pdb\"";

        System.out.println("Extracting DLLs from Archive");
        if (!new File(downloadPath).exists())
            new File(downloadPath).mkdir();
        try {
            if (!extractFiles(downloadPath + "/" + version.version + "_tmp", "unitydownload_" + version.version + ".dat", internalPathZip, !isil2cpp && version.version.startsWith("20"), useNSISExtractor, true)) {
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
        if (useNSISBIExtractor) {
            return Runtime.getRuntime().exec(new String[]{"sh", "-c", "mono UnityNSISReader.exe \"-f" + zipPath + "\" \"-o" + outputPath + "\" \"-r" + internalPath + "\""}).waitFor() == 0;
        }

        if (isPkg) {
            if (Runtime.getRuntime().exec(new String[] {"sh", "-c", "7z " + (keepFilePath ? "x" : "e") + " \"" + zipPath + "\" \"Payload~\" -y"}).waitFor() != 0)
                return false;

            return Runtime.getRuntime().exec(new String[]{"sh", "-c", "7z " + (keepFilePath ? "x" : "e") + " \"Payload~\" -o\"" + outputPath + "\" " + internalPath + " -y"}).waitFor() == 0;
        }
        else
            return Runtime.getRuntime().exec(new String[]{"sh", "-c", "7z " + (keepFilePath ? "x" : "e") + " \"" + zipPath + "\" -o\"" + outputPath + "\" " + internalPath + " -y"}).waitFor() == 0;
    }

    private static void moveDirectory(File src, File dest) {
        if (!dest.exists())
            dest.mkdirs();
        String targetDirPath = dest.getAbsolutePath();
        File[] files = src.listFiles();
        for (File file : files)
            file.renameTo(new File(targetDirPath + File.separator + file.getName()));
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

}
