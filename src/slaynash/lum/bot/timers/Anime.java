package slaynash.lum.bot.timers;

import java.awt.Color;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;
import net.dv8tion.jda.api.EmbedBuilder;
import slaynash.lum.bot.ConfigManager;
import slaynash.lum.bot.discord.JDAManager;
import slaynash.lum.bot.utils.ExceptionUtils;

public class Anime extends TimerTask {
    public void run() {
        try {
            List<AnimeEntry> animes = checkSubs();
            Instant startOfDay = Instant.now().truncatedTo(ChronoUnit.DAYS);

            StringBuilder sb = new StringBuilder();
            Color color = Color.GREEN;
            boolean upcomming = false;

            for (AnimeEntry anime : animes) {
                String url = "https://animeschedule.net/anime/" + anime.route;
                Instant date = anime.getEpisodeDateInstant();
                String time = " at <t:" + date.getEpochSecond() + ":t>";
                String episode = "Episode " + (anime.subtractedEpisodeNumber > 0 ? anime.subtractedEpisodeNumber + " - " : "") + anime.episodeNumber;
                if (anime.delayedText != null && !anime.delayedText.isEmpty())
                    time = " **" + anime.delayedText + "**";
                String title = anime.title;
                if (anime.english != null && !anime.english.isEmpty())
                    title = anime.english;
                if (anime.getMediaTypeName().equals("Movie"))
                    episode = "Movie";
                else if (anime.episodeNumber == anime.episodes && anime.episodes > 1) {
                    episode = episode + "F";
                    color = new Color(0, 111, 0); // Dark green for final episodes
                }
                if (anime.status.equals("Upcoming")) {
                    time = time + "\\*";
                    color = new Color(111, 255, 22); // Neon Green for upcoming anime
                    upcomming = true;
                }

                if (date.isAfter(startOfDay) && date.isBefore(startOfDay.plus(1, ChronoUnit.DAYS))) {
                    sb.append("* [").append(title).append("](").append(url).append(") ").append(episode).append(time).append("\n");
                }
            }

            if (upcomming)
                sb.append("\n\\* = Upcoming Anime, time may change");

            if (sb.isEmpty()) {
                sb.append("No anime today");
                color = Color.YELLOW;
            }

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("Anime Schedule <t:" + startOfDay.getEpochSecond() + ":D>");
            embed.setDescription(sb.toString().strip());
            embed.setColor(color);
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

    private static List<AnimeEntry> checkSubs() {
        List<AnimeEntry> animeEntries = new ArrayList<>();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://animeschedule.net/api/v3/timetables/sub"))
                .header("User-Agent", "LUM Bot " + ConfigManager.commitHash)
                .header("Authorization", "Bearer " + ConfigManager.animescheduleApiKey)
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new Exception("Failed to fetch anime entries: " + response.statusCode() + " - " + response.body());
            }

            animeEntries = new Gson().fromJson(response.body(), new TypeToken<List<AnimeEntry>>(){}.getType());

            List<AnimeEntry> rawAnimeEntries = checkRawUpcoming();
            for (AnimeEntry entry : rawAnimeEntries) {
                // add entry to animeEntries if route does not exist in animeEntries
                boolean exists = false;
                for (AnimeEntry existingEntry : animeEntries) {
                    if (existingEntry.route.equals(entry.route)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    animeEntries.add(entry);
                }
            }

            animeEntries.removeIf(entry -> entry.donghua);
            animeEntries.sort(Comparator.comparing(AnimeEntry::getEpisodeDateInstant));
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Exception while handling Anime:", e);
        }
        return animeEntries;
    }

    private static List<AnimeEntry> checkRawUpcoming() {
        List<AnimeEntry> animeEntries = new ArrayList<>();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://animeschedule.net/api/v3/timetables/raw"))
                .header("User-Agent", "LUM Bot " + ConfigManager.commitHash)
                .header("Authorization", "Bearer " + ConfigManager.animescheduleApiKey)
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new Exception("Failed to fetch raw anime entries: " + response.statusCode() + " - " + response.body());
            }

            List<AnimeEntry> rawAnimeEntries = new Gson().fromJson(response.body(), new TypeToken<List<AnimeEntry>>(){}.getType());

            for (AnimeEntry entry : rawAnimeEntries) {
                if (entry.status.equals("Upcoming")) {
                    animeEntries.add(entry);
                }
            }

        }
        catch (Exception e) {
            ExceptionUtils.reportException("Exception while handling Anime:", e);
        }
        return animeEntries;
    }

    public static class AnimeEntry {
        public AnimeEntry(String title,
                          String english,
                          String route,
                          String delayedText,
                          String delayedFrom,
                          String delayedUntil,
                          String status,
                          String episodeDate,
                          int episodeNumber,
                          int subtractedEpisodeNumber,
                          int episodes,
                          boolean donghua,
                          String airType,
                          JsonArray mediaTypes,
                          String airingStatus
        )
        {
            this.title = title;
            this.english = english;
            this.route = route;
            this.delayedText = delayedText;
            this.delayedFrom = delayedFrom;
            this.delayedUntil = delayedUntil;
            this.status = status;
            this.episodeDate = episodeDate;
            this.episodeNumber = episodeNumber;
            this.subtractedEpisodeNumber = subtractedEpisodeNumber;
            this.episodes = episodes;
            this.donghua = donghua;
            this.airType = airType;
            this.mediaTypes = mediaTypes;
            this.airingStatus = airingStatus;
        }
        public final String title;
        public final String english;
        public final String route;
        public final String delayedText;
        public final String delayedFrom;
        public final String delayedUntil;
        public final String status;
        public final String episodeDate;
        public final int episodeNumber;
        public final int subtractedEpisodeNumber;
        public final int episodes;
        public final boolean donghua;
        public final String airType;
        public final JsonArray mediaTypes;
        public final String airingStatus;

        public Instant getDelayedFromInstant() {
            return Instant.parse(delayedFrom);
        }

        public Instant getDelayedUntilInstant() {
            return Instant.parse(delayedUntil);
        }

        public Instant getEpisodeDateInstant() {
            return Instant.parse(episodeDate);
        }

        public String getMediaTypeName() {
            if (!mediaTypes.isEmpty()) {
                return mediaTypes.get(0).getAsJsonObject().get("name").getAsString();
            }
            return null;
        }
    }
}
