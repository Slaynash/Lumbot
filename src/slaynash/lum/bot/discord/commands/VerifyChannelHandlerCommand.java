package slaynash.lum.bot.discord.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.ConfigManager;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.discord.VerifyPair;

public class VerifyChannelHandlerCommand extends Command {

    @Override
    protected void onServer(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {
        if (!paramMessageReceivedEvent.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            paramMessageReceivedEvent.getChannel().sendMessage("Error: You need to have the Administrator permission").queue();
            return;
        }

        if (CommandManager.verifyChannels.containsKey(paramMessageReceivedEvent.getGuild().getIdLong())) {
            CommandManager.verifyChannels.remove(paramMessageReceivedEvent.getGuild().getIdLong());
            paramMessageReceivedEvent.getChannel().sendMessage("Successfully removed verify handler").queue();
        }
        else {
            String[] parts = paramString.split(" ", 3);
            if (parts.length != 2) {
                paramMessageReceivedEvent.getChannel().sendMessage("Usage: " + ConfigManager.discordPrefix + getName() + " `roleid`").queue();
            }
            try {
                Long.parseLong(parts[1]);
            }
            catch (Exception e) {
                paramMessageReceivedEvent.getChannel().sendMessage("Error: Invalid role id. usage: " + ConfigManager.discordPrefix + getName() + " `roleid`").queue();
                return;
            }

            CommandManager.verifyChannels.put(paramMessageReceivedEvent.getGuild().getIdLong(), new VerifyPair(paramMessageReceivedEvent.getChannel().getId(), parts[1]));
            paramMessageReceivedEvent.getChannel().sendMessage("Successfully set verify handler").queue();
        }
        CommandManager.saveVerifyChannels();
    }

    @Override
    protected boolean matchPattern(String paramString) {
        return paramString.split(" ", 2)[0].equals(ConfigManager.discordPrefix + getName());
    }

    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        return event.getMember().hasPermission(Permission.ADMINISTRATOR);
    }

    @Override
    public String getName() {
        return "setverifychannel";
    }

    @Override
    public String getHelpDescription() {
        return "Set the verify channel to the current one.";
    }

}
