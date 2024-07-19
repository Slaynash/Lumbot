package slaynash.lum.bot.discord;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import slaynash.lum.bot.DBConnectionManagerLum;
import slaynash.lum.bot.discord.melonscanner.MelonScannerApisManager;
import slaynash.lum.bot.utils.ExceptionUtils;

public class VRCApiVersionScanner {

    private static final Gson gson = new Gson();

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private static String secondLastBVT, lastBVT, lastDG;

    public static void init() {
        Thread t = new Thread(() -> {

            HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://api.vrchat.cloud/api/1/config"))
                .setHeader("User-Agent", "LUM Bot (https://discord.gg/akFkAG2)")
                .timeout(Duration.ofSeconds(30))
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

                        secondLastBVT = lastBVT;
                        lastBVT = config.buildVersionTag;
                        lastDG = config.deploymentGroup;

                        if (!JDAManager.isEventsEnabled())
                            continue;

                        List<ServerChannel> channels = new ArrayList<>();
                        try {
                            ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT * FROM `GuildConfigurations` WHERE VRCAPI IS NOT NULL");
                            while (rs.next()) {
                                channels.add(new ServerChannel(rs.getString("GuildID"), rs.getString("VRCAPI")));
                            }
                            DBConnectionManagerLum.closeRequest(rs);
                        }
                        catch (SQLException e) {
                            ExceptionUtils.reportException("Failed to fetch VRCAPI channels", e);
                            continue;
                        }

                        for (ServerChannel channel : channels) {
                            Guild guild = JDAManager.getJDA().getGuildById(channel.serverID());
                            if (guild == null)
                                continue;
                            MessageChannel tc = (MessageChannel) JDAManager.getJDA().getGuildChannelById(channel.channelId());
                            if (tc == null)
                                continue;
                            if (tc.canTalk()) {
                                if (guild.getSelfMember().hasPermission((GuildChannel) tc, Permission.MESSAGE_EMBED_LINKS))
                                    tc.sendMessageEmbeds(embed).queue();
                                else
                                    tc.sendMessage("Gibme embed perms").queue();
                            }
                            else
                                ExceptionUtils.reportException("Can not post VRCAPI in " + guild.getName());
                        }
                    }
                }
                catch (ConnectException e) {
                    System.out.println("VRCAPI: " + e.getMessage());
                }
                catch (Exception e) {
                    ExceptionUtils.reportException("Failed to fetch VRCAPI:", e);
                }

                try {
                    Thread.sleep(45 * 1000);
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
