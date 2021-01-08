package slaynash.lum.bot.discord;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public abstract class Command
{
  protected Command instance = this;
  
  protected abstract boolean matchPattern(String paramString);
  
  protected abstract void onClient(String paramString, MessageReceivedEvent paramMessageReceivedEvent);
  
  protected abstract void onServer(String paramString, MessageReceivedEvent paramMessageReceivedEvent);
  
  protected abstract void onLUM(String paramString);
  
  protected abstract String getHelpPath();
  
  protected abstract String getHelpName();
  
  protected abstract String getHelpDescription();
}
