package slaynash.lum.bot.discord.melonscanner;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;

import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message.Attachment;
import slaynash.lum.bot.discord.JDAManager;
import slaynash.lum.bot.utils.ExceptionUtils;

public final class LogCounter {

    private static final String workingPath = System.getProperty("user.dir");
    private static int previousLogCount = 0;
    private static int previousSSCount = 0;

    public static void AddtoCounter(Attachment attachment) {
        try{
            String directoryPath = workingPath.concat("/logs/");

            // File directory = new File(directoryPath);
            // if (!directory.exists())
            //     directory.mkdirs(); // in case log folder is missing

            attachment.downloadToFile(directoryPath + Instant.now().toString().replaceAll(":", "_") + attachment.getFileName())
                .thenAccept(file -> System.out.println("Saved attachment to " + file.getName()));
        }
        catch (Exception exception) {
            ExceptionUtils.reportException(
                "Exception while Saving Log",
                exception.getMessage(),
                exception);
        }
    }

    public static void AddSSCounter(String bannedUser, String message, String guildID) {
        try{
            String directoryPath = workingPath.concat("/SSlogs/");

            // File directory = new File(directoryPath);
            // if (!directory.exists())
            //     directory.mkdirs(); // in case log folder is missing

            Files.writeString(Path.of(directoryPath, bannedUser + guildID + ".txt"), message);
        }
        catch (Exception exception) {
            ExceptionUtils.reportException(
                "Exception while Saving Scam Shield Log",
                exception.getMessage(),
                exception);
        }
    }

    public static void UpdateCounter() {
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
                workingPath,
                exception);
        }
    }
}
