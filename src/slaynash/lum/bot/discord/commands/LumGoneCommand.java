package slaynash.lum.bot.discord.commands;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.JDAManager;
import slaynash.lum.bot.discord.utils.CrossServerUtils;

public class LumGoneCommand extends Command {

    @Override
    protected void onServer(String str, MessageReceivedEvent event) {
        if (!includeInHelp(event))
            return;

        event.getChannel().sendMessage("Ok I'm leaving...").queue();
        JDAManager.getJDA().getPresence().setStatus(OnlineStatus.INVISIBLE);
        try {
            Thread.sleep(10000);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        JDAManager.getJDA().getPresence().setStatus(OnlineStatus.ONLINE);
    }

    @Override
    protected boolean matchPattern(String str) {
        return str.contains("lum stop");
    }

    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        return CrossServerUtils.isLumDev(event.getMember());
    }
}
