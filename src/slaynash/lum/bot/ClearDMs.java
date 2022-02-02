package slaynash.lum.bot;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.TimerTask;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import slaynash.lum.bot.discord.JDAManager;
import slaynash.lum.bot.utils.ExceptionUtils;

public class ClearDMs extends TimerTask {
    public void run() {
        JDA jda = JDAManager.getJDA();
        List<TextChannel> channels = jda.getGuildById(633588473433030666L).getCategoryById(924780998124798022L).getTextChannels();
        channels.forEach(c -> {
            try {
                List<Message> mess = c.getIterableHistory().takeAsync(1).get();
                if (mess.size() == 0) return;
                OffsetDateTime time = mess.get(0).getTimeCreated();
                if (time.isBefore(OffsetDateTime.now().minusDays(7)))
                    c.delete().queue();
            }
            catch (Exception e) {
                ExceptionUtils.reportException("Issue with ClearDMs", e);
            }
        });
    }
}
