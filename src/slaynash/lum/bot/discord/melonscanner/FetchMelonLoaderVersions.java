package slaynash.lum.bot.discord.melonscanner;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.util.zip.ZipFile;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import slaynash.lum.bot.ConfigManager;
import slaynash.lum.bot.DBConnectionManagerLum;

public class FetchMelonLoaderVersions {
    public static void start() {
        // Fetch MelonLoader releases and nightly builds every 15 minutes
        Thread t = new Thread(() -> {
            while (true) {
                mlReleases();
                mlNightly();
                llReleases();
                try {
                    Thread.sleep(15 * 60 * 1000);
                }
                catch (Exception ignored) { }
            }
        }, "FetchMelonLoaderVersions");
        t.setDaemon(true);
        t.start();
    }

    public static void mlReleases() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/repos/LavaGang/MelonLoader/releases"))
                .header("Authorization", "Bearer " + ConfigManager.gitHubApiKey)
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                System.out.println("Failed to fetch MelonLoader versions from GH: " + response.statusCode());
                return;
            }
            JsonArray json = JsonParser.parseString(response.body()).getAsJsonArray();
            for (JsonElement element : json) {
                JsonObject obj = element.getAsJsonObject();
                if (obj.get("draft").getAsBoolean()) continue;
                String version = obj.get("tag_name").getAsString().substring(1);
                String htmlURL = obj.get("html_url").getAsString();
                ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT * FROM `MLhash` WHERE `Version` = ? AND `Nightly` = 0 AND `Android` = 0", version);
                if (rs.next()) {
                    DBConnectionManagerLum.closeRequest(rs);
                    continue;
                }
                DBConnectionManagerLum.closeRequest(rs);
                for (JsonElement asset : obj.get("assets").getAsJsonArray()) {
                    JsonObject assetObj = asset.getAsJsonObject();
                    if (assetObj.get("name").getAsString().equals("MelonLoader.x64.zip")) {
                        String downloadURL = assetObj.get("browser_download_url").getAsString();
                        downloadAndHash(downloadURL, version, htmlURL, false, false);
                        break;
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void llReleases() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/repos/LemonLoader/MelonLoader/releases"))
                .header("Authorization", "Bearer " + ConfigManager.gitHubApiKey)
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                System.out.println("Failed to fetch LemonLoader versions from GH: " + response.statusCode());
                return;
            }
            JsonArray json = JsonParser.parseString(response.body()).getAsJsonArray();
            for (JsonElement element : json) {
                JsonObject obj = element.getAsJsonObject();
                if (obj.get("draft").getAsBoolean()) continue;
                String version = obj.get("tag_name").getAsString();
                String htmlURL = obj.get("html_url").getAsString();
                ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT * FROM `MLhash` WHERE `Version` = ? AND `Nightly` = 0 AND `Android` = 1", version);
                if (rs.next()) {
                    DBConnectionManagerLum.closeRequest(rs);
                    continue;
                }
                DBConnectionManagerLum.closeRequest(rs);
                for (JsonElement asset : obj.get("assets").getAsJsonArray()) {
                    JsonObject assetObj = asset.getAsJsonObject();
                    if (assetObj.get("name").getAsString().equals("melon_data.zip")) {
                        String downloadURL = assetObj.get("browser_download_url").getAsString();
                        downloadAndHash(downloadURL, version, htmlURL, false, true);
                        break;
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void mlNightly() {
        // fetch builds from actions
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/repos/LavaGang/MelonLoader/actions/workflows/5411546/runs?status=success&exclude_pull_requests=true"))
                .header("Authorization", "Bearer " + ConfigManager.gitHubApiKey)
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                System.out.println("Failed to fetch MelonLoader versions from Actions: " + response.statusCode());
                return;
            }
            JsonArray json = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("workflow_runs");
            for (JsonElement element : json) {
                JsonObject obj = element.getAsJsonObject();
                if (!obj.get("name").getAsString().contains("ci")) continue;
                String runID = obj.get("id").getAsString();
                String version = obj.get("name").getAsString().split(" ")[0];
                String htmlURL = obj.get("html_url").getAsString();
                ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT * FROM `MLhash` WHERE `Version` = ? AND `Nightly` = 1 AND `Android` = 0", version);
                if (!rs.next())
                    getArtifacts(runID, version, htmlURL);
                DBConnectionManagerLum.closeRequest(rs);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void getArtifacts(String runID, String version, String htmlURL) {
        // fetch artifacts from actions
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/repos/LavaGang/MelonLoader/actions/runs/" + runID + "/artifacts"))
                .header("Authorization", "Bearer " + ConfigManager.gitHubApiKey)
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                System.out.println("Failed to fetch MelonLoader versions from Actions: " + response.statusCode());
                return;
            }
            JsonArray json = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("artifacts");
            for (JsonElement element : json) {
                JsonObject obj = element.getAsJsonObject();
                if (obj.get("name").getAsString().equals("MelonLoader.Windows.x64.CI.Release")) {
                    String downloadURL = obj.get("archive_download_url").getAsString();
                    downloadAndHash(downloadURL, version, htmlURL, true, false);
                    break;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public static void downloadAndHash(String zipURL, String version, String htmlURL, boolean ci, boolean android) throws Exception {
        System.out.println("Downloading MelonLoader version " + version + " from " + zipURL);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(zipURL))
            .header("Authorization", "Bearer " + ConfigManager.gitHubApiKey)
            .method("GET", HttpRequest.BodyPublishers.noBody())
            .build();
        HttpResponse<byte[]> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofByteArray());

        // If downloading from Azure, you can't have the Authorization header
        if (response.statusCode() == 302) {
            request = HttpRequest.newBuilder()
                .uri(URI.create(response.headers().firstValue("Location").get()))
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();
            response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofByteArray());
        }

        Path ml = Files.createTempFile("ML", ".zip");
        String net35 = "NULL";
        String net6  = "NULL";

        try {
            Files.write(ml, response.body());
            ZipFile zipFile = new ZipFile(ml.toString());
            if (zipFile.getEntry("MelonLoader/MelonLoader.dll") != null) {
                net35 = MelonScannerApisManager.bytesToHex(MessageDigest.getInstance("SHA-256").digest(zipFile.getInputStream(zipFile.getEntry("MelonLoader/MelonLoader.dll")).readAllBytes()));
            }
            else if (zipFile.getEntry("MelonLoader/net35/MelonLoader.dll") != null) {
                net35 = MelonScannerApisManager.bytesToHex(MessageDigest.getInstance("SHA-256").digest(zipFile.getInputStream(zipFile.getEntry("MelonLoader/net35/MelonLoader.dll")).readAllBytes()));
            }
            if (zipFile.getEntry("MelonLoader/net6/MelonLoader.dll") != null) {
                net6 = MelonScannerApisManager.bytesToHex(MessageDigest.getInstance("SHA-256").digest(zipFile.getInputStream(zipFile.getEntry("MelonLoader/net6/MelonLoader.dll")).readAllBytes()));
            }
            else if (zipFile.getEntry("MelonLoader/net8/MelonLoader.dll") != null) {
                net6 = MelonScannerApisManager.bytesToHex(MessageDigest.getInstance("SHA-256").digest(zipFile.getInputStream(zipFile.getEntry("MelonLoader/net8/MelonLoader.dll")).readAllBytes()));
            }
            DBConnectionManagerLum.sendUpdate("INSERT INTO `MLhash` (`Version`, `Hash35`, `Hash6`, `Nightly`, `Android`, `DL`) VALUES (?, ?, ?, ?, ?)", version, net35, net6, ci ? "1" : "0", android ? "1" : "0", htmlURL);
            System.out.println("Added MelonLoader version " + version + " to the database");
            zipFile.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            ml.toFile().delete();
        }
    }
}