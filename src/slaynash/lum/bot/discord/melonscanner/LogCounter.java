package slaynash.lum.bot.discord.melonscanner;

import java.awt.Color;
import java.io.File;
import java.time.Instant;
import java.util.Date;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Message.Attachment;
import slaynash.lum.bot.discord.ExceptionUtils;
import slaynash.lum.bot.discord.JDAManager;

public final class LogCounter {

    private static final String workingPath = System.getProperty("user.dir");
    private static int previousLogCount = 0;

    public static void AddtoCounter(Attachment attachment) {
        try{
            String directoryName = workingPath.concat("/logs/" + Instant.now().toString().replaceAll(":", ";"));

            File directory = new File(directoryName);
            if (!directory.exists())
                directory.mkdirs(); // in case log folder is missing
            else
                System.out.println("Lum time traveled");

            attachment.downloadToFile(directoryName);
        }
        catch (Exception exception) {
            System.err.println("Exception while Saving Log");
            exception.printStackTrace();

            try {
                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setColor(Color.red);
                embedBuilder.setTitle("Exception while Saving Log");
                String exceptionString = exception.getMessage() + "\n" + ExceptionUtils.getStackTrace(exception);
                if (exceptionString.length() > 2048)
                    exceptionString = exceptionString.substring(0, 2044) + " ...";
                embedBuilder.setDescription(exceptionString);
                MessageEmbed embed = embedBuilder.build();

                JDAManager.getJDA().getGuildById(633588473433030666L).getTextChannelById(851519891965345845L).sendMessage(embed).queue();
            }
            catch (Exception e2) { e2.printStackTrace(); }
        }
    }

    public static void UpdateLogCounter() {
        try{
            Date date = new Date();
            String directoryName = workingPath.concat("/logs/");
            File directory = new File(directoryName);

            int logCount = directory.listFiles().length;
            if(logCount > 0){
                // remove files that is older then 24 hours
                for (File fileEntry : directory.listFiles()) {
                    if ((date.getTime() - fileEntry.lastModified()) > 24 * 60 * 60 * 1000) {
                        fileEntry.delete();
                    }
                }
            }
            logCount = directory.listFiles().length;

            if(logCount != previousLogCount)
                JDAManager.getJDA().getPresence().setActivity(Activity.watching(logCount + " melons squashed in 24 hours"));
            previousLogCount = logCount;
        }
        catch (Exception exception) {
            ExceptionUtils.reportException(
                "Exception while Updating Counter",
                workingPath.concat("/logs/"),
                exception);
        }
    }
}
