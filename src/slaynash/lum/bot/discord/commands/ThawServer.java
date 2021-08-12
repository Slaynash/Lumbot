package slaynash.lum.bot.discord.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.discord.GuildConfigurations;
import slaynash.lum.bot.discord.Moderation;
import slaynash.lum.bot.discord.ServerMessagesHandler;

public class ThawServer extends Command {

    @Override
    protected void onServer(String paramString, MessageReceivedEvent event) {
        if (!includeInHelp(event))
            return;

        Long lockedDownRole = GuildConfigurations.lockDownRoles.get(event.getGuild().getIdLong());

        event.getGuild().getRoleById(lockedDownRole).getManager().givePermissions(Permission.MESSAGE_WRITE).complete();
        String reportChannel = CommandManager.mlReportChannels.get(event.getGuild().getIdLong());
        if (reportChannel != null)
            event.getGuild().getTextChannelById(reportChannel).sendMessage("User " + event.getAuthor().getIdLong() + " has thawed this server from frozen state.").queue();
        else
            event.getChannel().sendMessage("User " + event.getAuthor().getIdLong() + " has thawed this server from frozen state.").queue();
    }

    @Override
    protected boolean matchPattern(String paramString) {
        return paramString.startsWith("l!thaw");
    }

    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        return GuildConfigurations.lockDownRoles.get(event.getGuild().getIdLong()) != null && (Moderation.getAdmins(event.getGuild()).contains(event.getAuthor().getIdLong()) || ServerMessagesHandler.checkIfStaff(event));
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
