package slaynash.lum.bot.discord.melonscanner;

import java.io.File;
import java.time.Instant;
import java.util.Date;

import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message.Attachment;
import slaynash.lum.bot.discord.JDAManager;

public final class LogCounter {

    private static final String workingPath = System.getProperty("user.dir");

    public static void AddtoCounter(Attachment attachment) {
        String directoryName = workingPath.concat("\\logs\\" + Instant.now().toString().replaceAll(":", ";"));

        File directory = new File(directoryName);
        if (!directory.exists())
            directory.mkdirs(); // in case log folder is missing
        else
            System.out.println("Lum time traveled");

        attachment.downloadToFile(directoryName);
    }

    public static void UpdateLogCounter() {
        int logCount = 0;

        Date date = new Date();
        String directoryName = workingPath.concat("\\logs");
        File directory = new File(directoryName);

        // remove files that is older then 24 hours
        for (File fileEntry : directory.listFiles()) {
            if ((date.getTime() - fileEntry.lastModified()) > 24 * 60 * 60 * 1000) {
                fileEntry.delete();
            }
        }

        logCount = directory.listFiles().length;

        JDAManager.getJDA().getPresence().setActivity(Activity.watching("for logs, " + logCount + " read in the past 24 hours"));
    }
}
