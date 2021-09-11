package slaynash.lum.bot.discord.commands;

import java.util.Objects;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.discord.GuildConfigurations;
import slaynash.lum.bot.discord.Moderation;
import slaynash.lum.bot.discord.ServerMessagesHandler;

public class LockDown extends Command {

    @Override
    protected void onServer(String paramString, MessageReceivedEvent event) {
        if (!includeInHelp(event))
            return;

        String reportChannel = CommandManager.mlReportChannels.get(event.getGuild().getIdLong());
        Role lockDownRole = event.getGuild().getRoleById(GuildConfigurations.lockDownRoles.get(event.getGuild().getIdLong()));
        boolean lockDownState = lockDownRole.hasPermission(Permission.MESSAGE_WRITE);

        if (lockDownState)
            lockDownRole.getManager().revokePermissions(Permission.MESSAGE_WRITE).complete();
        else
            lockDownRole.getManager().givePermissions(Permission.MESSAGE_WRITE).complete();

        if (!Objects.equals(reportChannel, event.getTextChannel().getId()))
            event.getGuild().getTextChannelById(reportChannel).sendMessage("User " + event.getAuthor().getAsTag() + " has " + (lockDownState ? "locked down" : "unlocked") + " this server in " + event.getChannel().getName()).queue();
        event.getChannel().sendMessage("User " + event.getAuthor().getAsTag() + " has " + (lockDownState ? "locked down" : "unlocked") + " this server.").queue();
    }

    @Override
    protected boolean matchPattern(String paramString) {
        return paramString.startsWith(getName());
    }

    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        return GuildConfigurations.lockDownRoles.get(event.getGuild().getIdLong()) != null && (Moderation.getAdmins(event.getGuild()).contains(event.getAuthor().getIdLong()) || ServerMessagesHandler.checkIfStaff(event));
    }

    @Override
    public String getHelpDescription() {
        return "Toggles Member's sending messages permission in all channels for emergencies - Staff only";
    }

    @Override
    public String getName() {
        return "l!lockdown";
    }
}
