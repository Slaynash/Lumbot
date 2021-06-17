package slaynash.lum.bot.discord.commands;

import java.awt.Color;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.discord.JDAManager;

public class HelpCommand extends Command {
    
    @Override
    protected boolean matchPattern(String pattern) {
        return pattern.split(" ", 2)[0].equals("l!help");
    }
    
    @Override
    protected void onServer(String command, MessageReceivedEvent event) {
        String[] split = command.split(" ", 2);
        String path = split.length > 1 ? command.split(" ", 2)[1].trim() : "";
        
        String helpMessage = "**__Help " + path + ":__**\n\n";
        
        boolean empty = true;
        for (Command cmd : CommandManager.getCommands()) {
            if (/*cmd.getHelpPath() != null && cmd.getHelpPath().equals(path) &&*/ cmd.includeInHelp(event)) {
                empty = false;
                helpMessage = helpMessage + "**" + cmd.getHelpName() + "**: " + cmd.getHelpDescription() + "\n";
            }
        }
        if (!empty) {
            event.getChannel().sendMessageEmbeds(JDAManager.wrapMessageInEmbed(helpMessage, Color.CYAN)).queue();
        }
        else
        {
            helpMessage = helpMessage + "**Subhelp directory not found**";
            event.getChannel().sendMessageEmbeds(JDAManager.wrapMessageInEmbed(helpMessage, Color.RED)).queue();
        }
    }
    
    @Override
    public String getHelpDescription() {
        return "Show a description of all commands";
    }
    
    @Override
    public String getHelpName() {
        return "l!help";
    }
}
