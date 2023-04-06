package slaynash.lum.bot.utils;

import java.awt.Color;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.Queue;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA.Status;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import slaynash.lum.bot.ConfigManager;
import slaynash.lum.bot.discord.JDAManager;

public final class ExceptionUtils {

    private record QueuedException(String title, String comment, Throwable exception, MessageChannelUnion channel) {
    }

    private static final Queue<QueuedException> queuedExceptions = new LinkedList<>();

    public static void processExceptionQueue() {
        while (queuedExceptions.peek() != null) {
            QueuedException exception = queuedExceptions.remove();
            reportDiscord(exception.title, exception.comment, exception.exception, exception.channel);
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
    public static void reportException(String title, Throwable exception, MessageChannelUnion channel) {
        reportException(title, null, exception, channel);
    }

    public static void reportException(String title, String comment, Throwable exception, MessageChannelUnion channel) {
        if (comment != null)
            System.err.println(title + ": " + comment + ":");
        else
            System.err.println(title + ":");

        if (exception != null)
            exception.printStackTrace();

        if (JDAManager.getJDA() == null || JDAManager.getJDA().getStatus() != Status.CONNECTED)
            queuedExceptions.add(new QueuedException(title, comment, exception, channel));
        else {
            processExceptionQueue();
            reportDiscord(title, comment, exception, channel);
        }
    }

    private static void reportDiscord(String title, String comment, Throwable exception, MessageChannelUnion channel) {

        if (JDAManager.getJDA().getSelfUser().getIdLong() != 275759980752273418L || !JDAManager.isEventsEnabled())
            return;

        if (!ConfigManager.mainBot)
            title = "BACKUP: " + title;

        String exceptionString = "";
        try {
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setColor(Color.red);
            if (channel == null) {
                embedBuilder.setTitle(title);
            }
            else if (channel.getType() == ChannelType.PRIVATE) {
                String channelName = "PRIVATE #" + channel.asPrivateChannel().getUser().getId();
                embedBuilder.setTitle(title + " In " + channelName);
            }
            else {
                String channelName = channel.asGuildMessageChannel().getGuild().getName() + " #" + channel.asGuildMessageChannel().getName() + " > " + channel.getId();
                embedBuilder.setTitle(title + " In " + channelName, channel.asGuildMessageChannel().getJumpUrl());
            }
            if (comment != null) {
                if (comment.length() > 1000) // allows space for exception
                    comment = comment.substring(0, 1000);
                exceptionString = exceptionString + comment + "\n";
            }
            if (exception != null) {
                exceptionString = exceptionString + ExceptionUtils.getStackTrace(exception);
                if (exceptionString.length() > MessageEmbed.DESCRIPTION_MAX_LENGTH)
                    exceptionString = exceptionString.substring(0, MessageEmbed.DESCRIPTION_MAX_LENGTH - 4) + " ...";
            }
            MessageEmbed embed = embedBuilder.setDescription(exceptionString).build();
            if (!embed.isEmpty()) {
                if (exceptionString.contains("gotten status code") || exceptionString.contains("request timed out") || exceptionString.contains("connect timed out") || exceptionString.contains("Not a JSON Object: []"))
                    JDAManager.getJDA().getGuildById(JDAManager.mainGuildID).getTextChannelById(912757433913454612L).sendMessageEmbeds(embed).queue();
                else
                    JDAManager.getJDA().getGuildById(JDAManager.mainGuildID).getTextChannelById(851519891965345845L).sendMessageEmbeds(embed).queue();
            }

            if (channel != null && channel.getType() == ChannelType.TEXT) {
                TextChannel textChannel = channel.asTextChannel();
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
