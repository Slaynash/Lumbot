package slaynash.lum.bot.discord;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.melonscanner.MelonScannerApisManager;
import slaynash.lum.bot.utils.ExceptionUtils;

public class LumJokes {
    public static boolean sendJoke(MessageReceivedEvent event) {
        String message = event.getMessage().getContentStripped().toLowerCase();
        boolean hasLum = message.matches(".*\\blum\\b.*");

        if (!hasLum || message.startsWith(".") || !message.contains("joke")) {
            return false;
        }

        System.out.println("Requested a joke");
        new Thread(() -> {
            String joke = "";
            String punchLine = "";
            boolean dad = message.contains("dad");
            HttpResponse<String> response;
            if (dad) {
                try {
                    response = MelonScannerApisManager.downloadRequest(dadJokeRequest, "DADJOKE");
                    joke = response.body();
                }
                catch (Exception e) {
                    ExceptionUtils.reportException("An error has occurred while while getting Dad joke:", e, event.getTextChannel());
                }
            }
            else {
                try {
                    response = MelonScannerApisManager.downloadRequest(jokeAPIRequest, "JokeAPI");
                    JsonParser parser = new JsonParser();
                    JsonObject parsed = parser.parse(response.body()).getAsJsonObject();
                    if ("single".equals(parsed.get("type").getAsString())) {
                        joke = parsed.get("joke").getAsString();
                    }
                    else if ("twopart".equals(parsed.get("type").getAsString())) {
                        joke = parsed.get("setup").getAsString();
                        punchLine = parsed.get("delivery").getAsString();
                    }
                    else
                        throw new Exception("json was not as expected from JokeAPI");
                }
                catch (Exception e) {
                    ExceptionUtils.reportException("An error has occurred while while getting JokeAPI:", e, event.getTextChannel());
                }
            }
            if (!joke.isEmpty()) {
                if (punchLine.isEmpty()) {
                    event.getChannel().sendMessage(joke).queue();
                }
                else {
                    event.getChannel().sendMessage(joke + "\n\n||" + punchLine + "||").queue();;
                }
            }
            else
                ExceptionUtils.reportException(dad ? "Dad " : "" + "Joke is empty :( in " + event.getGuild().getName());
        }).start();
        return true;
    }

    private static final HttpRequest dadJokeRequest = HttpRequest.newBuilder()
        .GET()
        .uri(URI.create("https://icanhazdadjoke.com/"))
        .setHeader("User-Agent", "LUM Bot (https://discord.gg/akFkAG2)")
        .setHeader("Accept", "text/plain")
        .timeout(Duration.ofSeconds(20))
        .build();

    private static final HttpRequest jokeAPIRequest = HttpRequest.newBuilder()
        .GET()
        .uri(URI.create("https://v2.jokeapi.dev/joke/Programming,Miscellaneous,Pun"/*,Dark,Spooky,Christmas*/ + "?blacklistFlags=nsfw,religious,political,racist,sexist"/*,explicit*/))
        .setHeader("User-Agent", "LUM Bot (https://discord.gg/akFkAG2)")
        .setHeader("Accept", "text/json") //may not be needed
        .timeout(Duration.ofSeconds(20))
        .build();
}
