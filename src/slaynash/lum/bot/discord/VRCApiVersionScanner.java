package slaynash.lum.bot.discord;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.Gson;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class VRCApiVersionScanner {
    
    private static Gson gson = new Gson();

    private final static HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private static String lastBVT, lastDG;
    
    public static void init() {
        Thread t = new Thread(() -> {

            while (true) {
                
                HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create("https://vrchat.com/api/1/config"))
                    .setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:88.0) Gecko/20100101 Firefox/88.0")
                    .build();
                
                try {
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    VRCAPIConfig config = gson.fromJson(response.body(), VRCAPIConfig.class);

                    /*
                    if (lastBVT == null) {
                        lastBVT = config.buildVersionTag;
                        lastDG = config.deploymentGroup;
                    }
                    else */if (lastBVT == null || !lastBVT.equals(config.buildVersionTag)) {

                        EmbedBuilder eb = new EmbedBuilder();
                        eb.setTitle("VRCAPI Updated");
                        eb.addField("Old Build Version Tag", "[" + lastDG + "] " + lastBVT, false);
                        eb.addField("New Build Version Tag", "[" + config.deploymentGroup + "] " + config.buildVersionTag, false);
                        MessageEmbed embed = eb.build();

                        JDAManager.getJDA().getGuildById(439093693769711616L /* VRCMG */).getTextChannelById(741433349704384662L /* Slaynash channel */).sendMessage(embed).queue();

                        lastBVT = config.buildVersionTag;
                        lastDG = config.deploymentGroup;
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

                try { Thread.sleep(60 * 1000); } catch (Exception e) {}
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
