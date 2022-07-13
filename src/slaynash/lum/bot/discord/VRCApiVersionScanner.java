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
            .connectTimeout(Duration.ofSeconds(45))
            .build();

    private static String secondLastBVT, lastBVT, lastDG;

    public static void init() {
        Thread t = new Thread(() -> {

            HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://api.vrchat.cloud/api/1/config"))
                .setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:89.0) Gecko/20100101 Firefox/89.0")
                .timeout(Duration.ofSeconds(45))
                .build();

            while (true) {
                try {
                    HttpResponse<byte[]> response = MelonScannerApisManager.downloadRequest(httpClient, request, "VRChat API");

                    VRCAPIConfig config = gson.fromJson(new String(response.body()), VRCAPIConfig.class);

                    if (lastBVT == null) {
                        secondLastBVT = "nyan";
                        lastBVT = config.buildVersionTag;
                        lastDG = config.deploymentGroup;
                    }
                    else if (!lastBVT.equals(config.buildVersionTag)) {

                        EmbedBuilder eb = new EmbedBuilder();
                        eb.setTitle("VRCAPI Updated");
                        eb.addField("Old Build Version Tag", "[" + lastDG + "] " + lastBVT, false);
                        eb.addField("New Build Version Tag", "[" + config.deploymentGroup + "] " + config.buildVersionTag, false);
                        if (lastDG.equals(config.deploymentGroup))
                            eb.addField("WTF VRChat <:latina_pout:828090216732295228>", "Reusing Deployment Groups I see", false);
                        else if (config.buildVersionTag.equals(secondLastBVT)) {
                            eb.setTitle("VRCAPI Downgraded");
                            eb.addField("<:Neko_TeHe:865328470685909033>", "I see you fucked up VRChat and need to undo your mess.", false);
                        }
                        MessageEmbed embed = eb.build();

                        JDAManager.getJDA().getGuildById(673663870136746046L /* Modders & Chill */).getTextChannelById(829441182508515348L /* #bot-update-spam */).sendMessageEmbeds(embed).queue();
                        JDAManager.getJDA().getGuildById(876431015478951936L /* The Private Server Project */).getTextChannelById(995348312230203535L /* #official-api-updates */).sendMessageEmbeds(embed).queue();

                        secondLastBVT = lastBVT;
                        lastBVT = config.buildVersionTag;
                        lastDG = config.deploymentGroup;
                    }
                }
                catch (Exception e) {
                    ExceptionUtils.reportException("Failed to fetch VRCAPI:", e);
                }

                try {
                    Thread.sleep(60 * 1000);  // 60 seconds is maybe too slow
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
