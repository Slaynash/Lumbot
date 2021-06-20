package slaynash.lum.bot.discord;

import java.awt.Color;
import java.io.PrintWriter;
import java.io.StringWriter;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;

public final class ExceptionUtils {
    
    /**
    Returns a String representation of the exception's stack trace
    @param exception The target exception
    */
    public static String getStackTrace(Exception exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        exception.printStackTrace(pw);

        return sw.toString();
    }
    
    public static void reportException(String title, String comment, Exception exception, TextChannel textChannel) {
        System.err.println(title);
        exception.printStackTrace();

        try {
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setColor(Color.red);
            embedBuilder.setTitle(title);
            String exceptionString = comment + "\n" + ExceptionUtils.getStackTrace(exception);
            if (exceptionString.length() > 2048)
                exceptionString = exceptionString.substring(0, 2044) + " ...";
            embedBuilder.setDescription(exceptionString);
            MessageEmbed embed = embedBuilder.build();

            JDAManager.getJDA().getGuildById(633588473433030666L).getTextChannelById(851519891965345845L).sendMessageEmbeds(embed).queue();

            if(textChannel != null){
                EmbedBuilder sorryEmbedBuilder = new EmbedBuilder();
                sorryEmbedBuilder.setColor(Color.red);
                sorryEmbedBuilder.setTitle(title);
                sorryEmbedBuilder.setDescription("Lum has encounter an error and has notified the devs.");
                MessageEmbed sorryEmbed = sorryEmbedBuilder.build();
                textChannel.sendMessageEmbeds(sorryEmbed).queue();;
            }
        }
        catch (Exception e2) { e2.printStackTrace(); }
    }

    public static void reportException(String title, Exception exception) {
        reportException(title, null, exception, null);
    }
    public static void reportException(String title, String comment, Exception exception) {
        reportException(title, comment, exception, null);
    }
    public static void reportException(String title, Exception exception, TextChannel textChannel) {
        reportException(title, null, exception, textChannel);
    }
}
