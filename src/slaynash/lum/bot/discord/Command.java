package slaynash.lum.bot.discord;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public abstract class Command {
	protected Command instance = this;
	
	protected abstract boolean matchPattern(String paramString);
	
	protected void onClient(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {}
	
	protected void onServer(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {}
	
	public String getHelpPath() {
		return null;
	}
	
	public String getHelpName() {
		return null;
	}
	
	public String getHelpDescription() {
		return null;
	}
	
	public boolean includeInHelp(MessageReceivedEvent event) {
		return true;
	}
	
}
