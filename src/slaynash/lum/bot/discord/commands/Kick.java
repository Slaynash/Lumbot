package slaynash.lum.bot.discord.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.CommandManager;

public class Kick extends Command {
    @Override
    protected void onServer(String paramString, MessageReceivedEvent event) {
        if (!includeInHelp(event))
            return;

        Member kickMember;
        Message replied = event.getMessage().getReferencedMessage();
        if (replied != null) {
            kickMember = replied.getMember();
        }
        else {
            String[] parts = paramString.split(" ", 2);
            if (parts.length < 2 || !parts[1].matches("^\\d{18}$")) {
                event.getMessage().reply("Usage: reply to user or " + getName() + " <UserID>").queue();
                return;
            }
            kickMember = event.getGuild().getMemberById(parts[1]);
        }

        if (kickMember == null) {
            event.getMessage().reply("User was not found!").queue();
            return;
        }

        kickMember.kick().reason("Kicked by " + event.getMember().getEffectiveName()).queue();

        String reportChannel = CommandManager.mlReportChannels.get(event.getGuild().getIdLong());
        if (reportChannel != null && !reportChannel.equals(event.getTextChannel().getId()))
            event.getGuild().getTextChannelById(reportChannel).sendMessage("User " + kickMember.getUser().getAsMention() + "(" + kickMember.getId() + ") has been kicked by " + event.getMember().getEffectiveName() + "!").allowedMentions(null).queue();
        event.getChannel().sendMessage("User " + kickMember.getUser().getAsMention() + "(" + kickMember.getId() + ") has been kicked!").queue();
    }

    @Override
    protected boolean matchPattern(String paramString) {
        return paramString.startsWith(getName());
    }

    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        return event.getGuild().getSelfMember().hasPermission(Permission.KICK_MEMBERS) && event.getMember().hasPermission(Permission.KICK_MEMBERS);
    }

    @Override
    public String getHelpDescription() {
        return "Kicks a member. Reply or UserID - Staff only";
    }

    @Override
    public String getName() {
        return "l!kick";
    }
}
