package slaynash.lum.bot.discord.melonscanner;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;

import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message.Attachment;
import slaynash.lum.bot.discord.ExceptionUtils;
import slaynash.lum.bot.discord.JDAManager;

public final class LogCounter {

    private static final String workingPath = System.getProperty("user.dir");
    private static int previousLogCount = 0;
    private static int previousSSCount = 0;

    public static void AddtoCounter(Attachment attachment) {
        try{
            String directoryPath = workingPath.concat("/logs/" + Instant.now().toString().replaceAll(":", ";"));

            File directory = new File(directoryPath);
            if (!directory.exists())
                directory.mkdirs(); // in case log folder is missing
            else{
                System.out.println("Lum time traveled");
                JDAManager.getJDA().getGuildById(633588473433030666L).getTextChannelById(851519891965345845L).sendMessage("Lum time traveled").queue(); // I am currious if this happens
                return;
            }

            attachment.downloadToFile(directoryPath);
        }
        catch (Exception exception) {
            ExceptionUtils.reportException(
                "Exception while Saving Log",
                exception.getMessage(),
                exception);
        }
    }

    public static void AddSSCounter(String bannedUser, String message) {
        try{
            String directoryPath = workingPath.concat("/SSlogs/" + bannedUser);

            File directory = new File(directoryPath);
            if (!directory.exists())
                directory.mkdirs(); // in case log folder is missing
            else{
                System.out.println("Lum banned someone twice");
                JDAManager.getJDA().getGuildById(633588473433030666L).getTextChannelById(851519891965345845L).sendMessage("Lum banned someone twice").queue(); // I am currious if this happens
                return;
            }

            Files.writeString(Path.of(directoryPath), message);
        }
        catch (Exception exception) {
            ExceptionUtils.reportException(
                "Exception while Saving Scam Shield Log",
                exception.getMessage(),
                exception);
        }
    }

    public static void UpdateLogCounter() {
        try{
            Date date = new Date();
            String directoryPath = workingPath.concat("/logs/");
            File directory = new File(directoryPath);

            int logCount = directory.listFiles().length;
            if(logCount > 0){
                // remove folders that is older then 24 hours
                for (File fileEntry : directory.listFiles()) {
                    if ((date.getTime() - fileEntry.lastModified()) > 24 * 60 * 60 * 1000) {
                        fileEntry.delete();
                    }
                }
            }
            logCount = directory.listFiles().length;

            directoryPath = workingPath.concat("/SSlogs/");
            directory = new File(directoryPath);
            int sslogCount = directory.listFiles().length;
            if(sslogCount > 0){
                // remove folders that is older then 24 hours
                for (File fileEntry : directory.listFiles()) {
                    if ((date.getTime() - fileEntry.lastModified()) > 24 * 60 * 60 * 1000) {
                        fileEntry.delete();
                    }
                }
            }
            sslogCount = directory.listFiles().length;

            if(logCount != previousLogCount || sslogCount != previousSSCount)
                JDAManager.getJDA().getPresence().setActivity(Activity.watching(logCount + " melons squashed and removed " + sslogCount + " scammers in 24 hours"));
            previousLogCount = logCount;
            previousSSCount = sslogCount;
        }
        catch (Exception exception) {
            ExceptionUtils.reportException(
                "Exception while Updating Counter",
                workingPath.concat("/logs/"),
                exception);
        }
    }
}
