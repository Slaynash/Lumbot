package slaynash.lum.bot.discord;

import java.awt.Color;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class HelpCommand
  extends Command
{
  @Override
protected boolean matchPattern(String pattern)
  {
    return pattern.startsWith("l!help");
  }
  
  @Override
protected void onClient(String command, MessageReceivedEvent event) {}
  
  @Override
protected void onServer(String command, MessageReceivedEvent event)
  {
    String[] split = command.split(" ", 2);
    String path = split.length > 1 ? command.split(" ", 2)[1].trim() : "";
    String helpMessage = "**__Help " + path + ":__**\n\n";
    boolean empty = true;
    for (Command cmd : CommandManager.getCommands()) {
      if ((cmd.getHelpPath() != null) && (cmd.getHelpPath().equals(path)))
      {
        empty = false;
        helpMessage = helpMessage + "**" + cmd.getHelpName() + "**: " + cmd.getHelpDescription() + "\n";
      }
    }
    if (!empty)
    {
      event.getChannel().sendMessage(JDAManager.wrapMessageInEmbed(helpMessage, Color.BLUE)).queue();
    }
    else
    {
      helpMessage = helpMessage + "**Subhelp directory not found**";
      event.getChannel().sendMessage(JDAManager.wrapMessageInEmbed(helpMessage, Color.RED)).queue();
    }
  }
  
  @Override
protected void onLUM(String command) {}
  
  public static void registerCommand(Command command) {}
  
  @Override
protected String getHelpPath()
  {
    return "";
  }
  
  @Override
protected String getHelpDescription()
  {
    return "Show a description of all commands";
  }
  
  @Override
protected String getHelpName()
  {
    return "l!help";
  }
}
