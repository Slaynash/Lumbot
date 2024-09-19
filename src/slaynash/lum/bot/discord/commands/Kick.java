package slaynash.lum.bot.discord.commands;

import java.util.Collections;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.ConfigManager;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.CommandManager;

public class Kick extends Command {
    @Override
    protected void onServer(String paramString, MessageReceivedEvent event) {
        if (!includeInHelp(event))
            return;

        Member kickMember;
        Message replied = event.getMessage().getReferencedMessage();
        String reason = "";
        if (replied != null) { //l!kick reason
            kickMember = replied.getMember();
            String[] parts = paramString.split(" ", 2);
            if (parts.length > 1)
                reason = parts[1];
        }
        else {
            String[] parts = paramString.split(" ", 3); //l!kick UserID reason
            if (parts.length < 2) {
                event.getMessage().reply("Usage: reply to user or " + ConfigManager.discordPrefix + getName() + " <UserID> (reason)").queue();
                return;
            }
            if (parts.length > 2) {
                reason = parts[2];
            }
            try {
                kickMember = event.getGuild().getMemberById(parts[1]);
            }
            catch (Exception e) {
                event.getMessage().reply("Invalid snowflake, User was not found!").queue();
                return;
            }
        }

        if (kickMember == null) {
            event.getMessage().reply("User was not found!").queue();
            return;
        }

        if (kickMember.equals(event.getGuild().getSelfMember())) {
            event.getMessage().reply("Please don't kick me, I have been a good bot").queue();
            return;
        }

        if (kickMember.equals(event.getMember())) {
            event.getMessage().reply("https://tenor.com/view/leave-go-away-just-leave-you-are-annoying-leave-server-gif-17802417").queue();
            return;
        }

        if (!event.getGuild().getSelfMember().canInteract(kickMember)) {
            event.getMessage().reply("Can not kick " + kickMember.getUser().getAsMention() + "(" + kickMember.getId() + ") because they are a higher role than my role").setAllowedMentions(Collections.emptyList()).queue();
            return;
        }

        if (reason.isBlank())
            kickMember.kick().reason("Kicked by " + event.getMember().getEffectiveName()).queue();
        else
            kickMember.kick().reason(event.getAuthor().getName() + " - " + reason).queue();

        String reportChannel = CommandManager.mlReportChannels.get(event.getGuild().getIdLong());
        if (reportChannel != null && !reportChannel.equals(event.getChannel().asGuildMessageChannel().getId()))
            event.getGuild().getTextChannelById(reportChannel).sendMessage("User " + kickMember.getUser().getAsMention() + "(" + kickMember.getId() + ") has been kicked by " + event.getMember().getEffectiveName() + "!\n" + reason).setAllowedMentions(Collections.emptyList()).queue();
        event.getChannel().sendMessage("User " + kickMember.getUser().getAsMention() + "(" + kickMember.getId() + ") has been kicked!\n" + reason).queue();
    }

    @Override
    protected boolean matchPattern(String paramString) {
        return paramString.startsWith(ConfigManager.discordPrefix + getName());
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
        return "kick";
    }
}
