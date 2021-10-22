package slaynash.lum.bot.discord.commands;

import java.awt.Color;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.utils.Utils;

public class HelpCommand extends Command {

    @Override
    protected boolean matchPattern(String pattern) {
        return pattern.split(" ", 2)[0].equals(getName());
    }

    @Override
    protected void onServer(String command, MessageReceivedEvent event) {

        StringBuilder helpMessage = new StringBuilder("**__Help :__**\n\n");

        boolean empty = true;
        for (Command cmd : CommandManager.getCommands()) {
            if (cmd.includeInHelp(event)) {
                empty = false;
                helpMessage.append("**").append(cmd.getName()).append("**: ").append(cmd.getHelpDescription()).append("\n");
            }
        }
        if (!empty) {
            event.getChannel().sendMessageEmbeds(Utils.wrapMessageInEmbed(helpMessage.toString(), Color.CYAN)).queue();
        }
        else {
            helpMessage.append("**No commands found**");
            event.getChannel().sendMessageEmbeds(Utils.wrapMessageInEmbed(helpMessage.toString(), Color.RED)).queue();
        }
    }

    @Override
    public String getHelpDescription() {
        return "Show a description of all commands";
    }

    @Override
    public String getName() {
        return "l!help";
    }

    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        return false;
    }
}
