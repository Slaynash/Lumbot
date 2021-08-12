package slaynash.lum.bot.discord.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.CommandManager;

public class Unban extends Command {
    @Override
    protected void onServer(String paramString, MessageReceivedEvent event) {
        if (!includeInHelp(event))
            return;

        User unbanUser;
        String[] parts = paramString.split(" ", 2);
        if (parts.length < 2 || !parts[1].matches("^\\d{18}$")) {
            event.getMessage().reply("Usage: l!unban <UserID>").queue();
            return;
        }
        unbanUser = event.getJDA().getUserById(parts[1]);
        if (unbanUser == null) {
            event.getMessage().reply("User was not found!").queue();
            return;
        }

        event.getGuild().unban(unbanUser).queue();

        String reportChannel = CommandManager.mlReportChannels.get(event.getGuild().getIdLong());
        if (reportChannel != null)
            event.getGuild().getTextChannelById(reportChannel).sendMessage("User " + unbanUser.getAsMention() + "(" + unbanUser.getId() + ") has been unbanned by " + event.getMember().getEffectiveName() + "!").queue();
        else
            event.getChannel().sendMessage("User " + unbanUser.getAsMention() + "(" + unbanUser.getId() + ") has been unbanned!").queue();
    }

    @Override
    protected boolean matchPattern(String paramString) {
        return paramString.startsWith("l!unban");
    }

    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        return event.getGuild().getSelfMember().hasPermission(Permission.BAN_MEMBERS) && event.getMember().hasPermission(Permission.BAN_MEMBERS);
    }

    @Override
    public String getHelpDescription() {
        return "Unbans a member with UserID- Staff only";
    }

    @Override
    public String getHelpName() {
        return "l!unban";
    }
}
