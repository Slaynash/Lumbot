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
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import slaynash.lum.bot.discord.JDAManager;
import slaynash.lum.bot.utils.ExceptionUtils;

public class UnityVersionMonitor {

    private static String hrefIdentifier = "<a href=\"https://download.unity3d.com/";

    private static String downloadPath = "unity_versions";

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .followRedirects(Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private static List<UnityVersion> unityVersions = new ArrayList<>();

    private static Map<String, List<String>> installedVersions = new HashMap<>();

    public static void start() {
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

                // fetch unity versions

                HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create("https://unity3d.com/get-unity/download/archive"))
                    .setHeader("User-Agent", "LUM Bot")
                    .timeout(Duration.ofSeconds(30))
                    .build();

                String pagedata = null;

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

                    int hrefIdentifierIndex = -1;
                    if ((hrefIdentifierIndex = line.indexOf(hrefIdentifier)) < 0)
                        continue;

                    String setupIdentifier = "UnitySetup64-";
                    if (!line.contains(setupIdentifier) && !line.contains(setupIdentifier = "UnitySetup-"))
                        continue;

                    String subline = line.substring(hrefIdentifierIndex + hrefIdentifier.length());
                    String foundUrl = "https://download.unity3d.com/" + subline.split("\"", 2)[0];
                    int extensionIndex = -1;
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

                        if (!foundVersion.startsWith("2017") && !(foundVersion.startsWith("2020") || foundVersion.startsWith("2020.1")) && !foundVersion.startsWith("2021"))
                            urlIl2CppWin = "https://download.unity3d.com/download_unity/" + versionId + "/TargetSupportInstaller/UnitySetup-Windows-IL2CPP-Support-for-Editor-" + fullVersion + ".exe";
                    }

                    unityVersions.add(new UnityVersion(foundVersion, fullVersion, foundUrl, urlIl2CppWin));
                }

                List<UnityVersion> newVersions = new ArrayList<>();

                for (UnityVersion unityVersion : unityVersions)
                    if (!installedVersions.containsKey(unityVersion.version))
                        newVersions.add(unityVersion);

                if (installedVersions.size() > 0) {
                    String message = "New Unity version published:";
                    for (UnityVersion newVersion : newVersions)
                        message += "\n - " + newVersion.version;
                    JDAManager.getJDA().getGuildById(633588473433030666L /* Slaynash's Workbench */).getTextChannelById(876466104036393060L /* #lum-status */).sendMessage(message).queue();
                }

                for (UnityVersion newVersion : newVersions)
                    downloadUnity(newVersion);

                // run tools sanity checks
            }

        }, "UnityVersionMonitor");
        thread.setDaemon(true);
        thread.start();
    }

    private static void downloadUnity(UnityVersion uv) {
        File targetFile = new File(downloadPath + "/" + uv.version);
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

        System.out.println("Downloading " + uv.downloadUrl);
        try (FileOutputStream fileOutputStream = new FileOutputStream("unitydownload_" + uv.version + ".dat")) {
            ReadableByteChannel readableByteChannel = Channels.newChannel(new URL(uv.downloadUrl).openStream());
            fileOutputStream.getChannel()
                .transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to download unity version " + uv.version + " (mono)", e);
            return;
        }

        extractFilesFromArchive(uv, false);

        if (uv.downloadUrlIl2CppWin != null) {
            try (FileOutputStream fileOutputStream = new FileOutputStream("unitydownload_" + uv.version + ".dat")) {
                ReadableByteChannel readableByteChannel = Channels.newChannel(new URL(uv.downloadUrlIl2CppWin).openStream());
                fileOutputStream.getChannel()
                    .transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            }
            catch (Exception e) {
                ExceptionUtils.reportException("Failed to download unity version " + uv.version + " (il2cpp)", e);
                return;
            }

            extractFilesFromArchive(uv, true);
        }

        new File("unitydownload_" + uv.version + ".dat").delete();
    }

    public static void extractFilesFromArchive(UnityVersion version, boolean isil2cpp) {
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

        String internalPathZip = version.version.startsWith("20") ? (version.version.startsWith("2017.1") ? "./" : (isil2cpp ? "$INSTDIR$*/" : "./")) : "";
        internalPathZip += internalPath;
        internalPathZip = "\"" + internalPathZip + (version.version.startsWith("20") && !version.version.startsWith("2017.1") ? "/*/UnityPlayer.dll" : "/*/*.exe") + "\" \"" + internalPathZip + "/*/UnityPlayer*.pdb\"";

        System.out.println("[UnityVersionMonitor] Extracting DLLs from Archive");
        if (!new File(downloadPath).exists())
            new File(downloadPath).mkdir();
        try {
            if (!extractFiles(downloadPath + "/" + version.version + "_tmp", "unitydownload_" + version.version + ".dat", internalPathZip, !isil2cpp && version.version.startsWith("20"), true)) {
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

    private static boolean extractFiles(String outputPath, String zipPath, String internalPath, boolean isPkg, boolean keepFilePath) throws IOException, InterruptedException {
        if (isPkg) {
            if (Runtime.getRuntime().exec("7z " + (keepFilePath ? "x" : "e") + " \"" + zipPath + "\" \"Payload~\" -y").waitFor() != 0)
                return false;

            if (Runtime.getRuntime().exec("7z " + (keepFilePath ? "x" : "e") + " \"Payload~\" -o\"" + outputPath + "\" " + internalPath + " -y").waitFor() != 0)
                return false;
        }
        else
            if (Runtime.getRuntime().exec("7z " + (keepFilePath ? "x" : "e") + " \"" + zipPath + " -o\"" + outputPath + "\" " + internalPath + " -y").waitFor() != 0)
                return false;

        return true;
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
        public String version;
        public String fullVersion;
        public String downloadUrl;
        public String downloadUrlIl2CppWin;

        public UnityVersion(String version, String fullVersion, String downloadUrl, String downloadUrlIl2CppWin) {
            this.version = version;
            this.fullVersion = fullVersion;
            this.downloadUrl = downloadUrl;
            this.downloadUrlIl2CppWin = downloadUrlIl2CppWin;
        }
    }

}
