package slaynash.lum.bot.discord.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.CommandManager;

public class SetMLReportChannelCommand extends Command {

    @Override
    protected void onServer(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {
        if (!paramMessageReceivedEvent.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            paramMessageReceivedEvent.getChannel().sendMessage("Error: You need to have the Administrator permission").queue();
            return;
        }

        if (paramMessageReceivedEvent.getChannel().getId().equals(CommandManager.mlReportChannels.get(paramMessageReceivedEvent.getGuild().getIdLong()))) {
            CommandManager.mlReportChannels.remove(paramMessageReceivedEvent.getGuild().getIdLong());
            CommandManager.saveMLReportChannels();
            paramMessageReceivedEvent.getChannel().sendMessage("Successfully unset ML report channel").queue();
        }
        else {
            CommandManager.mlReportChannels.put(paramMessageReceivedEvent.getGuild().getIdLong(), paramMessageReceivedEvent.getChannel().getId());
            CommandManager.saveMLReportChannels();
            paramMessageReceivedEvent.getChannel().sendMessage("Successfully set ML report channel").queue();
        }
    }

    @Override
    protected boolean matchPattern(String paramString) {
        return paramString.split(" ", 2)[0].equals("setmlreportchannel");
    }

    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        return false;
    }

}
