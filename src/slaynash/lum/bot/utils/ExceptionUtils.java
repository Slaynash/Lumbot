package slaynash.lum.bot.utils;

import java.awt.Color;
import java.io.PrintWriter;
import java.io.StringWriter;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import slaynash.lum.bot.discord.JDAManager;

public final class ExceptionUtils {
    
    /**
    Returns a String representation of the exception's stack trace
    @param exception The target exception
    */
    public static String getStackTrace(Throwable exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        exception.printStackTrace(pw);

        return sw.toString();
    }
    
    public static void reportException(String title, String comment, Throwable exception, TextChannel textChannel) {
        System.err.println(title);
        String exceptionString = "";

        try {
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setColor(Color.red);
            if(textChannel == null){
                embedBuilder.setTitle(title);
            } else{
                String channelName = textChannel.getGuild().getName() + " #" + textChannel.getName() + " > " + textChannel.getId();
                String channelLink = "https://canary.discord.com/channels/" + textChannel.getGuild().getId() + "/" + textChannel.getId() + "/" + textChannel.getLatestMessageId();
                embedBuilder.setTitle(title + " In " + channelName, channelLink);
            }
            if (comment != null) {
                exceptionString.concat(comment + "\n");
            }
            if(exception != null){
                exception.printStackTrace();
                exceptionString.concat(ExceptionUtils.getStackTrace(exception));
                if (exceptionString.length() > MessageEmbed.TEXT_MAX_LENGTH)
                    exceptionString = exceptionString.substring(0, MessageEmbed.TEXT_MAX_LENGTH - 4) + " ...";
                embedBuilder.setDescription(exceptionString);
            }
            MessageEmbed embed = embedBuilder.build();
            if(!embed.isEmpty())
                JDAManager.getJDA().getGuildById(633588473433030666L).getTextChannelById(851519891965345845L).sendMessageEmbeds(embed).queue();

            if(textChannel != null){
                EmbedBuilder sorryEmbedBuilder = new EmbedBuilder();
                sorryEmbedBuilder.setColor(Color.red);
                sorryEmbedBuilder.setTitle(title);
                sorryEmbedBuilder.setDescription("Lum has encountered an error and has notified the devs.");
                MessageEmbed sorryEmbed = sorryEmbedBuilder.build();
                if (!sorryEmbed.isEmpty())
                    textChannel.sendMessageEmbeds(sorryEmbed).queue();
            }
        }
        catch (Exception e2) { e2.printStackTrace(); }
    }

    public static void reportException(String title, Throwable exception) {
        reportException(title, null, exception, null);
    }
    public static void reportException(String title, String comment, Throwable exception) {
        reportException(title, comment, exception, null);
    }
    public static void reportException(String title, Throwable exception, TextChannel textChannel) {
        reportException(title, null, exception, textChannel);
    }

    public static void reportException(String title, String comment) {
        reportException(title, comment, null, null);
    }

    public static void reportException(String title) {
        reportException(title, null, null, null);
    }
}
