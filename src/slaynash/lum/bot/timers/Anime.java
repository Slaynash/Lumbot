package slaynash.lum.bot.timers;

import java.awt.Color;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.EmbedBuilder;
import slaynash.lum.bot.ConfigManager;
import slaynash.lum.bot.discord.JDAManager;
import slaynash.lum.bot.utils.ExceptionUtils;

public class Anime extends TimerTask {
    public void run() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://animeschedule.net/api/v3/timetables/sub"))
                .header("User-Agent", "LUM Bot " + ConfigManager.commitHash)
                .header("Authorization", "Bearer " + ConfigManager.animescheduleApiKey)
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            JsonArray json = JsonParser.parseString(response.body()).getAsJsonArray();
            Instant startOfDay = Instant.now().truncatedTo(ChronoUnit.DAYS);

            StringBuilder sb = new StringBuilder();

            for (JsonElement element : json) {
                JsonObject anime = element.getAsJsonObject();
                String episode = "Episode " + anime.get("episodeNumber").getAsString();
                String episodeDate = anime.get("episodeDate").getAsString();
                Instant instant = Instant.parse(episodeDate);
                String title = anime.get("title").getAsString();
                String url = "https://animeschedule.net/anime/" + anime.get("route").getAsString();
                String time = " at <t:" + instant.getEpochSecond() + ":t>\n";
                if (anime.has("delayedText"))
                    time = " **" + anime.get("delayedText").getAsString() + "**\n";
                if (anime.has("english"))
                    title = anime.get("english").getAsString();
                if (anime.has("episodes") && anime.get("episodes").equals(anime.get("episodeNumber")))
                    episode = episode + "F";
                if (anime.get("mediaTypes").getAsJsonArray().get(0).getAsJsonObject().get("name").getAsString().equals("Movie"))
                    episode = "Movie";

                if (instant.isAfter(startOfDay) && instant.isBefore(startOfDay.plus(1, ChronoUnit.DAYS))) {
                    sb.append("* [").append(title).append("](").append(url).append(") ").append(episode).append(time);
                }
            }

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("Anime Schedule <t:" + startOfDay.getEpochSecond() + ":D>");
            embed.setDescription(sb.toString().strip());
            embed.setColor(Color.GREEN);
            JDAManager.getJDA().getGuildById(627168678471008269L).getTextChannelById(628799325232693248L).sendMessageEmbeds(embed.build()).queue();
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Exception while handling Anime:", e);
        }
    }

    public static void start() {
        Timer timer = new Timer();
        timer.schedule(
            new Anime(),
            Date.from(Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS)),
            1000 * 60 * 60 * 24
        );
    }
}
