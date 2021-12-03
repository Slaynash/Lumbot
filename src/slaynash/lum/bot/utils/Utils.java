package slaynash.lum.bot.utils;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class Utils {
    public static String translate(String langFrom, String langTo, String text) {
        if (langFrom == null || langTo == null || text == null)
            return "";
        if (langFrom.isBlank() || langTo.isBlank() || text.isBlank())
            return "";
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
                response.append(inputLine).append("\n");
            }
            in.close();
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to translate message", e);
        }
        return response.toString().trim().replace("] (", "](").replace(" /", "/").replace("/ ", "/").replace(" .", ".");
    }

    public static MessageEmbed wrapMessageInEmbed(String message, Color color) {
        EmbedBuilder eb = new EmbedBuilder();
        if (color != null)
            eb.setColor(color);
        if (message.length() > MessageEmbed.DESCRIPTION_MAX_LENGTH)
            eb.setDescription(message.substring(0, MessageEmbed.DESCRIPTION_MAX_LENGTH - 4) + " ...");
        else
            eb.setDescription(message);
        return eb.build();
    }

    public static void replyEmbed(String message, Color color, MessageReceivedEvent event) {
        MessageEmbed embed = wrapMessageInEmbed(message, color);

        if (!event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_WRITE))
            return;
        if (event.getChannelType() == ChannelType.PRIVATE || event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_EMBED_LINKS))
            event.getMessage().replyEmbeds(embed).queue();
        else {
            event.getMessage().reply(embed.getDescription()).queue();
        }
    }
    public static void replyEmbed(MessageEmbed embed, MessageReceivedEvent event) {
        if (!event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_WRITE))
            return;
        if (event.getChannelType() == ChannelType.PRIVATE || event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_EMBED_LINKS))
            event.getMessage().replyEmbeds(embed).queue();
        else {
            event.getMessage().reply(embed.getDescription()).queue();
        }
    }

    public static void sendEmbed(String message, Color color, MessageReceivedEvent event) {
        MessageEmbed embed = wrapMessageInEmbed(message, color);

        if (!event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_WRITE))
            return;
        if (event.getChannelType() == ChannelType.PRIVATE || event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_EMBED_LINKS))
            event.getChannel().sendMessageEmbeds(embed).queue();
        else {
            event.getChannel().sendMessage(embed.getDescription()).queue();
        }
    }
    public static void sendEmbed(MessageEmbed embed, MessageReceivedEvent event) {
        if (!event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_WRITE))
            return;
        if (event.getChannelType() == ChannelType.PRIVATE || event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_EMBED_LINKS))
            event.getChannel().sendMessageEmbeds(embed).queue();
        else {
            event.getChannel().sendMessage(embed.getDescription()).queue();
        }
    }

    /**
    * Calculates the similarity (a number within 0 and 1) between two strings.
    * from https://stackoverflow.com/a/16018452
    */
    public static double similarity(String s1, String s2) {
        String longer = s1, shorter = s2;
        if (s1.length() < s2.length()) { // longer should always have greater length
            longer = s2;
            shorter = s1;
        }
        int longerLength = longer.length();
        if (longerLength == 0) {
            return 1.0; /* both strings are zero length */
        }
        return (longerLength - editDistance(longer, shorter)) / (double) longerLength;
    }

    // Example implementation of the Levenshtein Edit Distance
    // See http://rosettacode.org/wiki/Levenshtein_distance#Java
    public static int editDistance(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();

        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0)
                  costs[j] = j;
                else {
                    if (j > 0) {
                        int newValue = costs[j - 1];
                        if (s1.charAt(i - 1) != s2.charAt(j - 1))
                            newValue = Math.min(Math.min(newValue, lastValue),
                                costs[j]) + 1;
                        costs[j - 1] = lastValue;
                        lastValue = newValue;
                    }
                }
            }
            if (i > 0)
            costs[s2.length()] = lastValue;
        }
        return costs[s2.length()];
    }
}
