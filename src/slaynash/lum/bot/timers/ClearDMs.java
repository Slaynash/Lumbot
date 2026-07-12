package slaynash.lum.bot.timers;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import slaynash.lum.bot.DBConnectionManagerLum;
import slaynash.lum.bot.Main;
import slaynash.lum.bot.discord.JDAManager;
import slaynash.lum.bot.utils.ExceptionUtils;

public class ClearDMs extends TimerTask {
    public void run() {
        Guild mainGuild = JDAManager.getJDA().getGuildById(JDAManager.mainGuildID);
        if (mainGuild == null)
            return;
        List<TextChannel> channels = mainGuild.getCategoryById(924780998124798022L).getTextChannels();
        channels.forEach(c -> c.retrieveMessageById(c.getLatestMessageId()).queue(m -> {
            if (m.getTimeCreated().isBefore(OffsetDateTime.now().minusDays(14))) {
                c.getIterableHistory().forEachAsync(message -> {
                    try {
                        DBConnectionManagerLum.sendUpdate("DELETE FROM `MessagePairs` WHERE `DevMessage` = ?", message.getId());
                    }
                    catch (SQLException e) {
                        ExceptionUtils.reportException("failed to delete message pair for message " + message.getId(), e);
                    }
                    return true;
                }).thenRun(() -> c.delete().queue());
            }
        }, e -> System.out.println("Failed to retrieve message from channel " + c.getName())));
    }

    public static void start() {
        Main.SCHEDULER.scheduleAtFixedRate(
            new ClearDMs(),
            1, 60, TimeUnit.MINUTES);
    }
}
