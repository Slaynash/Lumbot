package slaynash.lum.bot.discord.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.ConfigManager;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.CommandManager;

public class SetLogChannelHandlerCommand extends Command {

    @Override
    protected void onServer(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {
        if (!paramMessageReceivedEvent.getMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
            paramMessageReceivedEvent.getChannel().sendMessage("Error: You need to have the View Audit Logs permission").queue();
            return;
        }
        CommandManager.logChannels.put(paramMessageReceivedEvent.getGuild().getIdLong(), paramMessageReceivedEvent.getChannel().getId());
        paramMessageReceivedEvent.getChannel().sendMessage("Successfully set log channel").queue();
        CommandManager.saveLogChannels();
    }

    @Override
    protected boolean matchPattern(String paramString) {
        return paramString.split(" ", 2)[0].equals(ConfigManager.discordPrefix + getName());
    }

    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        return event.getMember().hasPermission(Permission.VIEW_AUDIT_LOGS);
    }

    @Override
    public String getName() {
        return "setlogchannel";
    }

    @Override
    public String getHelpDescription() {
        return "Set the log channel to the current one.";
    }

}
