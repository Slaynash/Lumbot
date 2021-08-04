package slaynash.lum.bot.discord;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.google.gson.Gson;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import slaynash.lum.bot.discord.melonscanner.MelonScannerApisManager;
import slaynash.lum.bot.utils.ExceptionUtils;

public class VRCApiVersionScanner {

    private static final Gson gson = new Gson();

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    private static String lastBVT, lastDG;

    public static void init() {
        Thread t = new Thread(() -> {

            HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://api.vrchat.cloud/api/1/config"))
                .setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:89.0) Gecko/20100101 Firefox/89.0")
                .timeout(Duration.ofSeconds(30))
                .build();

            while (true) {
                try {
                    HttpResponse<String> response = MelonScannerApisManager.downloadRequest(httpClient, request, "VRChat API");

                    VRCAPIConfig config = gson.fromJson(response.body(), VRCAPIConfig.class);

                    if (lastBVT == null) {
                        lastBVT = config.buildVersionTag;
                        lastDG = config.deploymentGroup;
                    }
                    else if (!lastBVT.equals(config.buildVersionTag)) {

                        EmbedBuilder eb = new EmbedBuilder();
                        eb.setTitle("VRCAPI Updated");
                        eb.addField("Old Build Version Tag", "[" + lastDG + "] " + lastBVT, false);
                        eb.addField("New Build Version Tag", "[" + config.deploymentGroup + "] " + config.buildVersionTag, false);
                        MessageEmbed embed = eb.build();

                        JDAManager.getJDA().getGuildById(673663870136746046L /* Modders & Chill */).getTextChannelById(829441182508515348L /* #bot-update-spam */).sendMessageEmbeds(embed).queue();

                        lastBVT = config.buildVersionTag;
                        lastDG = config.deploymentGroup;
                    }
                }
                catch (Exception e) {
                    ExceptionUtils.reportException("Failed to fetch VRCAPI:", e);
                }

                try {
                    Thread.sleep(60 * 1000);
                }
                catch (Exception ignored) { }
            }

        }, "VRCApiVersionScanner");
        t.setDaemon(true);
        t.start();
    }

    private static class VRCAPIConfig {
        public String deploymentGroup;
        public String buildVersionTag;
    }

}
