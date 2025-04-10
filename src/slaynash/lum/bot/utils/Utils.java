package slaynash.lum.bot.utils;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.GroupChannel;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.JDAManager;

public class Utils {
    public static String translate(String langFrom, String langTo, String text, int maxLength) {
        String translation = translate(langFrom, langTo, text);
        if (translation.length() > maxLength) {
            translation = translation.substring(0, maxLength);
        }
        return translation;
    }
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
    public static MessageEmbed wrapMessageInEmbed(String message) {
        return wrapMessageInEmbed(message, null, null);
    }
    public static MessageEmbed wrapMessageInEmbed(String message, Color color) {
        return wrapMessageInEmbed(message, color, null);
    }
    public static MessageEmbed wrapMessageInEmbed(String message, Color color, String imageURL) {
        EmbedBuilder eb = new EmbedBuilder();
        if (color != null)
            eb.setColor(color);
        if (message.length() > MessageEmbed.DESCRIPTION_MAX_LENGTH)
            eb.setDescription(message.substring(0, MessageEmbed.DESCRIPTION_MAX_LENGTH - 4) + " ...");
        else
            eb.setDescription(message);
        if (imageURL != null)
            eb.setImage(imageURL);
        return eb.build();
    }

    public static void replyEmbed(String message, Color color, MessageReceivedEvent event) {
        replyEmbed(message, color, null, event);
    }
    public static void replyEmbed(String message, Color color, String imageURL, MessageReceivedEvent event) {
        MessageEmbed embed = wrapMessageInEmbed(message, color, imageURL);

        if (!event.getGuild().getSelfMember().hasPermission(event.getChannel().asGuildMessageChannel(), Permission.MESSAGE_SEND))
            return;
        if (event.getChannelType() == ChannelType.PRIVATE || event.getGuild().getSelfMember().hasPermission(event.getChannel().asGuildMessageChannel(), Permission.MESSAGE_EMBED_LINKS))
            event.getMessage().replyEmbeds(embed).queue();
        else {
            event.getMessage().reply(embed.getDescription()).queue();
        }
    }
    public static void replyEmbed(MessageEmbed embed, MessageReceivedEvent event) {
        if (!event.getGuild().getSelfMember().hasPermission(event.getChannel().asGuildMessageChannel(), Permission.MESSAGE_SEND))
            return;
        if (event.getChannelType() == ChannelType.PRIVATE || event.getGuild().getSelfMember().hasPermission(event.getChannel().asGuildMessageChannel(), Permission.MESSAGE_EMBED_LINKS))
            event.getMessage().replyEmbeds(embed).queue();
        else {
            event.getMessage().reply(embed.getDescription()).queue();
        }
    }

    public static void sendEmbed(String message, Color color, MessageReceivedEvent event) {
        MessageEmbed embed = wrapMessageInEmbed(message, color);

        if (!event.getGuild().getSelfMember().hasPermission(event.getChannel().asGuildMessageChannel(), Permission.MESSAGE_SEND))
            return;
        if (event.getChannelType() == ChannelType.PRIVATE || event.getGuild().getSelfMember().hasPermission(event.getChannel().asGuildMessageChannel(), Permission.MESSAGE_EMBED_LINKS))
            event.getChannel().sendMessageEmbeds(embed).queue();
        else {
            event.getChannel().sendMessage(embed.getDescription()).queue();
        }
    }
    public static void sendEmbed(MessageEmbed embed, MessageReceivedEvent event) {
        if (!event.getGuild().getSelfMember().hasPermission(event.getChannel().asGuildMessageChannel(), Permission.MESSAGE_SEND))
            return;
        if (event.getChannelType() == ChannelType.PRIVATE || event.getGuild().getSelfMember().hasPermission(event.getChannel().asGuildMessageChannel(), Permission.MESSAGE_EMBED_LINKS))
            event.getChannel().sendMessageEmbeds(embed).queue();
        else {
            event.getChannel().sendMessage(embed.getDescription()).queue();
        }
    }
    public static void sendEmbed(MessageEmbed embed, MessageChannelUnion channel) {
        if (channel == null)
            return;
        switch (channel.getType()) {
            case TEXT -> {
                if (((GuildChannel) channel).getGuild().getSelfMember().hasPermission((GuildChannel) channel, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)) {
                    TextChannel textChannel = (TextChannel) channel;
                    textChannel.sendMessageEmbeds(embed).queue();
                }
            }
            case NEWS -> {
                if (((GuildChannel) channel).getGuild().getSelfMember().hasPermission((GuildChannel) channel, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)) {
                    NewsChannel newsChannel = (NewsChannel) channel;
                    newsChannel.sendMessageEmbeds(embed).queue();
                }
            }
            case GUILD_PRIVATE_THREAD, GUILD_PUBLIC_THREAD, GUILD_NEWS_THREAD -> {
                if (((GuildChannel) channel).getGuild().getSelfMember().hasPermission((GuildChannel) channel, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)) {
                    ThreadChannel threadChannel = (ThreadChannel) channel;
                    threadChannel.sendMessageEmbeds(embed).queue();
                }
            }
            case PRIVATE -> {
                PrivateChannel privateChannel = (PrivateChannel) channel;
                privateChannel.sendMessageEmbeds(embed).queue();
            }
            case GROUP -> {
                GroupChannel groupChannel = (GroupChannel) channel;
                groupChannel.sendMessageEmbeds(embed).queue();
            }
            default -> {
            }
        }
    }
    public static void sendMessage(String message, MessageChannelUnion channel) {
        if (channel == null)
            return;
        switch (channel.getType()) {
            case TEXT -> {
                if (((GuildChannel) channel).getGuild().getSelfMember().hasPermission((GuildChannel) channel, Permission.MESSAGE_SEND)) {
                    TextChannel textChannel = (TextChannel) channel;
                    textChannel.sendMessage(message).queue();
                }
            }
            case NEWS -> {
                if (((GuildChannel) channel).getGuild().getSelfMember().hasPermission((GuildChannel) channel, Permission.MESSAGE_SEND)) {
                    NewsChannel newsChannel = (NewsChannel) channel;
                    newsChannel.sendMessage(message).queue();
                }
            }
            case GUILD_PRIVATE_THREAD, GUILD_PUBLIC_THREAD, GUILD_NEWS_THREAD -> {
                if (((GuildChannel) channel).getGuild().getSelfMember().hasPermission((GuildChannel) channel, Permission.MESSAGE_SEND)) {
                    ThreadChannel threadChannel = (ThreadChannel) channel;
                    threadChannel.sendMessage(message).queue();
                }
            }
            case PRIVATE -> {
                PrivateChannel privateChannel = (PrivateChannel) channel;
                privateChannel.sendMessage(message).queue();
            }
            case GROUP -> {
                GroupChannel groupChannel = (GroupChannel) channel;
                groupChannel.sendMessage(message).queue();
            }
            default -> {
            }
        }
    }

    /*
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
    public static List<String> extractUrls(String text) {
        //https://stackoverflow.com/questions/5713558/detect-and-extract-url-from-a-string
        List<String> containedUrls = new ArrayList<>();
        String urlRegex = "(https?:((//)|(\\\\))+[\\w:#@%/;$()~_?+-=\\\\.&]*)";
        Pattern pattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE);
        Matcher urlMatcher = pattern.matcher(text);

        while (urlMatcher.find()) {
            containedUrls.add(text.substring(urlMatcher.start(0),
                    urlMatcher.end(0)));
        }

        return containedUrls;
    }

    public void checkIconURL(String iconURL, String unityName) {
        if (iconURL == null) {
            System.out.println("Icon URL is null");
            return;
        }
        if (iconURL.contains("discordapp"))  //TODO: check Discord media for 404
            return;
        new Thread(() -> {
            try {
                HttpURLConnection huc = (HttpURLConnection) new URL(iconURL).openConnection();
                huc.setRequestMethod("HEAD");
                int responseCode = huc.getResponseCode();
                if (responseCode / 100 == 4) { //4xx
                    JDAManager.getJDA().getTextChannelById("1001529648569659432").sendMessageEmbeds(
                                Utils.wrapMessageInEmbed("logo for " + unityName + " has " + responseCode + "\n" + iconURL, Color.decode("6696969"))).queue();
                }
            }
            catch (Exception e) {
                ExceptionUtils.reportException("checkIconURL Failed", e);
            }
        }).start();
    }
}
