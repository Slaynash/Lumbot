package slaynash.lum.bot.discord;

import java.awt.Color;

import java.io.PrintWriter;
import java.io.StringWriter;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

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
    
    public static void reportException(String title, String comment, Exception exception) {
        System.err.println(title);
        exception.printStackTrace();

        try {
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setColor(Color.red);
            embedBuilder.setTitle(title);
            String exceptionString = ExceptionUtils.getStackTrace(exception);
            if (exceptionString.length() > 2048)
                exceptionString = exceptionString.substring(0, 2044) + " ...";
            embedBuilder.setDescription(exceptionString);
            MessageEmbed embed = embedBuilder.build();

            JDAManager.getJDA().getGuildById(633588473433030666L).getTextChannelById(851519891965345845L).sendMessage(embed).queue();
        }
        catch (Exception e2) { e2.printStackTrace(); }
    }
    
}
