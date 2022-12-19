package slaynash.lum.bot.timers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.TimerTask;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import slaynash.lum.bot.discord.JDAManager;

public class ClearDMs extends TimerTask {
    public void run() {
        Guild mainGuild = JDAManager.getJDA().getGuildById(JDAManager.mainGuildID);
        if (mainGuild == null)
            return;
        List<TextChannel> channels = mainGuild.getCategoryById(924780998124798022L).getTextChannels();
        channels.forEach(c -> c.retrieveMessageById(c.getLatestMessageId()).queue(m -> {
            if (m.getTimeCreated().isBefore(OffsetDateTime.now().minusDays(7))) {
                c.delete().queue();
            }
        }, e -> System.out.println("Failed to retrieve message from channel " + c.getName())));
    }
}
