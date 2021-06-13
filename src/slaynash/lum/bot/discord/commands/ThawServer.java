package slaynash.lum.bot.discord.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.Moderation;
import slaynash.lum.bot.discord.ServerMessagesHandler;

public class ThawServer extends Command {
    
    @Override
    protected void onServer(String paramString, MessageReceivedEvent event) {
        if (!ServerMessagesHandler.checkIfStaff(event))
            return;
        
        event.getGuild().getRoleById(Moderation.lockDownRoles.get(event.getGuild().getIdLong())).getManager().givePermissions(Permission.MESSAGE_WRITE).complete();
    }

    @Override
    protected boolean matchPattern(String paramString) {
        return paramString.startsWith("l!thaw");
    }
    
    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        return Moderation.lockDownRoles.get(event.getGuild().getIdLong()) != null;
    }
    
    @Override
    public String getHelpDescription() {
        return "Allow Members to send messages again after lockdown - Staff Only";
    }
    
    @Override
    public String getHelpName() {
        return "l!thaw";
    }
}
