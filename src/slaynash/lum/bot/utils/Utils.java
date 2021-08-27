package slaynash.lum.bot.utils;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class Utils {
    public static String translate(String langFrom, String langTo, String text) {
        StringBuilder response = new StringBuilder();
        try {
            String urlStr = "https://script.google.com/macros/s/AKfycbyBJH20ap3UN_KUjbBjSRmEVALyvvYsyQ5bIprevMDWRrLg9GOf/exec" + //From my personal account, I get 20,000 translations / day
                "?q=" + URLEncoder.encode(text, StandardCharsets.UTF_8) +
                "&target=" + langTo +
                "&source=" + langFrom;
            URL url = new URL(urlStr);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to translate message", e);
        }
        return response.toString();
    }

    public static void replyStandard(String message, Color color, MessageReceivedEvent messageReceivedEvent) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(color)
                    .setDescription(message);
        MessageEmbed embed = embedBuilder.build();

        messageReceivedEvent.getMessage().replyEmbeds(embed);
    }
}
