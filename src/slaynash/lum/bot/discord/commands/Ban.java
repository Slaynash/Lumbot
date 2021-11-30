package slaynash.lum.bot.discord.commands;

import java.util.Collections;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.CommandManager;

public class Ban extends Command {
    //TODO DM banned user and add custom reason and better perms
    @Override
    protected void onServer(String paramString, MessageReceivedEvent event) {
        if (!includeInHelp(event))
            return;

        int delDays = 0;
        Member banMember;
        Message replied = event.getMessage().getReferencedMessage();
        if (replied != null) {
            String[] parts = paramString.split(" ", 2);
            if (parts.length > 1 && parts[1].matches("^\\d{1,2}$"))
                delDays = Math.min(Integer.parseInt(parts[1]), 7);
            banMember = replied.getMember();
        }
        else {
            String[] parts = paramString.split(" ", 3);
            if (parts.length < 2 || !parts[1].matches("^\\d{18}$")) {
                event.getMessage().reply("Usage: reply to user or " + getName() + " <UserID> (purge days)").queue();
                return;
            }
            if (parts.length == 3 && parts[2].matches("^\\d{1,2}$")) {
                delDays = Math.min(Integer.parseInt(parts[2]), 7);
            }
            banMember = event.getGuild().getMemberById(parts[1]);
        }

        if (banMember == null) {
            event.getMessage().reply("User was not found!").queue();
            return;
        }

        banMember.ban(delDays).reason("Banned by " + event.getMember().getEffectiveName()).queue();

        String reportChannel = CommandManager.mlReportChannels.get(event.getGuild().getIdLong());
        if (reportChannel != null && !reportChannel.equals(event.getTextChannel().getId()))
            event.getGuild().getTextChannelById(reportChannel).sendMessage("User " + banMember.getUser().getAsMention() + "(" + banMember.getId() + ") has been banned by " + event.getMember().getEffectiveName() + "!").allowedMentions(Collections.emptyList()).queue();
        event.getChannel().sendMessage("User " + banMember.getUser().getAsMention() + "(" + banMember.getId() + ") has been banned!").queue();
    }

    @Override
    protected boolean matchPattern(String paramString) {
        return paramString.startsWith(getName());
    }

    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        return event.getGuild().getSelfMember().hasPermission(Permission.BAN_MEMBERS) && event.getMember().hasPermission(Permission.BAN_MEMBERS);
    }

    @Override
    public String getHelpDescription() {
        return "Bans a member. Reply or UserID with optional purge days - Staff only";
    }

    @Override
    public String getName() {
        return "l!ban";
    }
}
