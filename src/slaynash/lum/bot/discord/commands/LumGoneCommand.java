package slaynash.lum.bot.discord.commands;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.JDAManager;
import slaynash.lum.bot.discord.utils.CrossServerUtils;

public class LumGoneCommand extends Command {

    @Override
    protected void onServer(String str, MessageReceivedEvent event) {
        if (JDAManager.getJDA().getPresence().getStatus().equals(OnlineStatus.INVISIBLE) && str.toLowerCase().contains("lum") && str.toLowerCase().contains("come back")) {
            event.getChannel().sendMessage("Nope, I am not here <:Neko_pout:865328471102324778>").queue();
            return;
        }

        if (!(CrossServerUtils.isLumDev(event.getMember()) || event.getAuthor().getIdLong() == 209206057594126336L))
            return;

        event.getChannel().sendMessage("Ok I'm leaving...").queue();
        JDAManager.getJDA().getPresence().setStatus(OnlineStatus.INVISIBLE);
        try {
            Thread.sleep(15000);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        JDAManager.getJDA().getPresence().setStatus(OnlineStatus.ONLINE);
    }

    @Override
    protected boolean matchPattern(String str) {
        return str.toLowerCase().contains("lum stop");
    }

    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        return false;
    }
}
