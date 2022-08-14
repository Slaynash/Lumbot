package slaynash.lum.bot.discord;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Random;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.melonscanner.MelonScannerApisManager;
import slaynash.lum.bot.utils.ExceptionUtils;

public class LumJokes {
    public static final String LOG_IDENTIFIER = "LumJokes";
    private static final Random random = new Random();

    public static boolean sendJoke(MessageReceivedEvent event) {
        String message = event.getMessage().getContentStripped().toLowerCase();
        boolean hasLum = message.matches(".*\\blum\\b.*");

        if (!hasLum || !message.contains("joke")) {
            return false;
        }

        System.out.println("Requested a joke");
        new Thread(() -> {
            String joke = "";
            String punchLine = "";
            HttpResponse<byte[]> response = null;
            String site = "blank";

            int type;
            if (message.contains("dad"))
                type = 0;
            else if (LocalDate.now().getMonthValue() == 10 || message.contains("spook")) { //halloween
                type = 2;
            }
            else if (LocalDate.now().getMonthValue() == 12) { //christmas
                type = 3;
            }
            else if (random.nextInt(420) == 69) {
                type = 69;
            }
            else {
                type =  random.nextInt(2);
            }

            switch (type) {
                case 0:
                    try {
                        site = "DADJOKE";
                        response = MelonScannerApisManager.downloadRequest(dadJokeRequest, "DADJOKE");
                        joke = new String(response.body());
                    }
                    catch (Exception e) {
                        ExceptionUtils.reportException("An error has occurred while while getting Dad joke:", e, event.getTextChannel());
                    }
                    break;
                case 1:
                    try {
                        site = "JokeAPI";
                        response = MelonScannerApisManager.downloadRequest(jokeAPIRequest, "JokeAPI");
                        JsonObject parsed = JsonParser.parseString(new String(response.body())).getAsJsonObject();
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
                    break;
                case 2:
                    try {
                        site = "SpookyJokeAPI";
                        response = MelonScannerApisManager.downloadRequest(spookyjokeAPIRequest, "SpookyJokeAPI");
                        JsonObject parsed = JsonParser.parseString(new String(response.body())).getAsJsonObject();
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
                        ExceptionUtils.reportException("An error has occurred while while getting Spooky JokeAPI:", e, event.getTextChannel());
                    }
                    break;
                case 3:
                    try {
                        site = "ChristmasJokeAPI";
                        response = MelonScannerApisManager.downloadRequest(christmasjokeAPIRequest, "ChristmasJokeAPI");
                        JsonObject parsed = JsonParser.parseString(new String(response.body())).getAsJsonObject();
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
                        ExceptionUtils.reportException("An error has occurred while while getting Christmas JokeAPI:", e, event.getTextChannel());
                    }
                    break;
                case 69:
                    site = "Dad_jokes.mp4";
                    joke = "https://cdn.discordapp.com/attachments/509655431787053057/963596823858135100/Dad_jokes.mp4";
                    break;
                default:
                    site = "fucky wucky";
                    joke = "<@240701606977470464> OOPSIE WOOPSIE!! Uwu I make a fucky wucky!!";
            }
            if (!joke.isEmpty()) {
                if (punchLine.isEmpty()) {
                    event.getChannel().sendMessage(joke).queue();
                    System.out.println(joke);
                }
                else {
                    System.out.println(joke + "\n" + punchLine);
                    try {
                        Message sentJoke = event.getChannel().sendMessage(joke).complete();
                        event.getTextChannel().sendTyping().queue(); //sends typing for 10 seconds
                        Thread.sleep(10 * 1000);
                        sentJoke.editMessage(joke + "\n\n||" + punchLine + "||").queue();
                    }
                    catch (InterruptedException e) {
                        ExceptionUtils.reportException("An error has occurred sending JokeAPI:", e, event.getTextChannel());
                    }
                }
            }
            else
                ExceptionUtils.reportException("Joke is empty :( in " + event.getGuild().getName() + " from " + site);
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

    private static final HttpRequest spookyjokeAPIRequest = HttpRequest.newBuilder()
        .GET()
        .uri(URI.create("https://v2.jokeapi.dev/joke/Spooky?blacklistFlags=nsfw,religious,political,racist,sexist"))
        .setHeader("User-Agent", "LUM Bot (https://discord.gg/akFkAG2)")
        .setHeader("Accept", "text/json") //may not be needed
        .timeout(Duration.ofSeconds(20))
        .build();
    private static final HttpRequest christmasjokeAPIRequest = HttpRequest.newBuilder()
        .GET()
        .uri(URI.create("https://v2.jokeapi.dev/joke/Christmas?blacklistFlags=nsfw,religious,political,racist,sexist"))
        .setHeader("User-Agent", "LUM Bot (https://discord.gg/akFkAG2)")
        .setHeader("Accept", "text/json") //may not be needed
        .timeout(Duration.ofSeconds(20))
        .build();
}
