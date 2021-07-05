package slaynash.lum.bot.utils;

import java.awt.Color;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.Queue;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA.Status;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import slaynash.lum.bot.discord.JDAManager;

public final class ExceptionUtils {

    private static class QueuedException {
        public String title;
        public String comment;
        public Throwable exception;
        public TextChannel textChannel;

        public QueuedException(String title, String comment, Throwable exception, TextChannel textChannel) {
            this.title = title;
            this.comment = comment;
            this.exception = exception;
            this.textChannel = textChannel;
        }
    }

    private static final Queue<QueuedException> queuedExceptions = new LinkedList<>();

    public static void processExceptionQueue() {
        while (queuedExceptions.peek() != null) {
            QueuedException exception = queuedExceptions.remove();
            reportDiscord(exception.title, exception.comment, exception.exception, exception.textChannel);
        }
    }

    /**
    Returns a String representation of the exception's stack trace.
    @param exception The target exception
    */
    public static String getStackTrace(Throwable exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        exception.printStackTrace(pw);

        return sw.toString();
    }

    public static void reportException(String title) {
        reportException(title, null, null, null);
    }

    public static void reportException(String title, String comment) {
        reportException(title, comment, null, null);
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

    public static void reportException(String title, String comment, Throwable exception, TextChannel textChannel) {
        if (comment != null)
            System.err.println(title + ": " + comment + ":");
        else
            System.err.println(title + ":");

        if (exception != null)
            exception.printStackTrace();

        if (JDAManager.getJDA() == null || JDAManager.getJDA().getStatus() != Status.CONNECTED)
            queuedExceptions.add(new QueuedException(title, comment, exception, textChannel));
        else
            reportDiscord(title, comment, exception, textChannel);
    }

    private static void reportDiscord(String title, String comment, Throwable exception, TextChannel textChannel) {
        String exceptionString = "";
        try {
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setColor(Color.red);
            if (textChannel == null) {
                embedBuilder.setTitle(title);
            }
            else {
                String channelName = textChannel.getGuild().getName() + " #" + textChannel.getName() + " > " + textChannel.getId();
                String channelLink = "https://canary.discord.com/channels/" + textChannel.getGuild().getId() + "/" + textChannel.getId() + "/" + textChannel.getLatestMessageId();
                embedBuilder.setTitle(title + " In " + channelName, channelLink);
            }
            if (comment != null) {
                exceptionString = exceptionString + comment + "\n";
            }
            if (exception != null) {
                exceptionString = exceptionString + ExceptionUtils.getStackTrace(exception);
                if (exceptionString.length() > MessageEmbed.TEXT_MAX_LENGTH)
                    exceptionString = exceptionString.substring(0, MessageEmbed.TEXT_MAX_LENGTH - 4) + " ...";
            }
            MessageEmbed embed = embedBuilder.setDescription(exceptionString).build();
            if (!embed.isEmpty())
                JDAManager.getJDA().getGuildById(633588473433030666L).getTextChannelById(851519891965345845L).sendMessageEmbeds(embed).queue();

            if (textChannel != null) {
                EmbedBuilder sorryEmbedBuilder = new EmbedBuilder();
                sorryEmbedBuilder.setColor(Color.red);
                sorryEmbedBuilder.setTitle(title);
                sorryEmbedBuilder.setDescription("Lum has encountered an error and has notified the devs.");
                MessageEmbed sorryEmbed = sorryEmbedBuilder.build();
                if (!sorryEmbed.isEmpty())
                    textChannel.sendMessageEmbeds(sorryEmbed).queue();
            }
        }
        catch (Exception e2) {
            e2.printStackTrace();
        }
    }
}
