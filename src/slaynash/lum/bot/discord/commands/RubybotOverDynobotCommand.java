package slaynash.lum.bot.discord.commands;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;

public class RubybotOverDynobotCommand extends Command {

    @Override
    protected void onServer(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {
        paramMessageReceivedEvent.getMessage().reply("<:SmugSip:743484784415866950>").queue();
    }

    @Override
    protected boolean matchPattern(String paramString) {
        return paramString.trim().toLowerCase().replace(" ", "").replace(">", "<").contains("rubybot<lum");
    }

    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        return false;
    }

    @Override
    public boolean allowBots() {
        return true;
    }

}
