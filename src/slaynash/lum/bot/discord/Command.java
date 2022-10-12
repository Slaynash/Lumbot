package slaynash.lum.bot.discord;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public abstract class Command {
    protected Command instance = this;

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected abstract boolean matchPattern(String paramString);

    protected void onClient(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {
    }

    protected void onServer(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {
    }

    public String getName() {
        return null;
    }

    public String getHelpDescription() {
        return null;
    }

    public boolean includeInHelp(MessageReceivedEvent event) {
        return true;
    }

    public boolean allowBots() {
        return false;
    }
}
