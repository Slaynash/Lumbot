package slaynash.lum.bot.discord.commands;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.discord.VerifyPair;

public class VerifyCommandCommand extends Command {

    @Override
    protected void onServer(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {
        VerifyPair pair = CommandManager.verifyChannels.get(paramMessageReceivedEvent.getGuild().getIdLong());
        if (pair != null) {
            //System.out.println("Server has registered verify command with channel " + pair.channelId + " and role " + pair.roleId);
            if (paramMessageReceivedEvent.getChannel().getId().equals(pair.channelId)) {
                paramMessageReceivedEvent.getGuild().addRoleToMember(paramMessageReceivedEvent.getAuthor(), paramMessageReceivedEvent.getGuild().getRoleById(pair.roleId)).queue();
                paramMessageReceivedEvent.getChannel().sendMessage("You are now verified!").queue();
            }
        }
    }

    @Override
    protected boolean matchPattern(String paramString) {
        return paramString.equals("!verify");
    }

    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        return false;
    }

}
