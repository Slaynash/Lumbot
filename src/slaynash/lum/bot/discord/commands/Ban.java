package slaynash.lum.bot.discord.commands;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.ConfigManager;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.CommandManager;

public class Ban extends Command {
    //TODO DM banned user and better perms, maybe ban via regex
    @Override
    protected void onServer(String paramString, MessageReceivedEvent event) {
        if (!includeInHelp(event))
            return;

        int delDays = 0;
        Member banMember;
        Message replied = event.getMessage().getReferencedMessage();
        String reason = "";
        if (replied != null) {
            String[] parts = paramString.split(" ", 3);
            if (parts.length > 1) {
                if (parts[1].matches("^\\d{1,2}$")) { //l!ban 9 reason
                    delDays = Math.min(Integer.parseInt(parts[1]), 7);
                    if (parts.length > 2)
                        reason = parts[2];
                }
                else { //l!ban reason
                    if (parts.length == 2)
                        reason = parts[1];
                    else
                        reason = parts[1] + " " + parts[2];
                }
            }
            banMember = replied.getMember();
        }
        else {
            String[] parts = paramString.split(" ", 4);
            if (parts.length < 2) {
                event.getMessage().reply("Usage: reply to user or " + ConfigManager.discordPrefix + getName() + " <UserID> (purge days) (reason)").queue();
                return;
            }
            if (parts.length == 3) {
                if (parts[2].matches("^\\d{1,2}$")) { //l!ban ID 9 reason
                    delDays = Math.min(Integer.parseInt(parts[2]), 7);
                }
                else {
                    reason = parts[2];
                }
            }
            else if (parts.length > 3) {
                if (parts[2].matches("^\\d{1,2}$")) { //l!ban ID 9 reason
                    delDays = Math.min(Integer.parseInt(parts[2]), 7);
                    reason = parts[3];
                }
                else { //l!ban ID reason
                    reason = parts[2] + " " + parts[3];
                }
            }
            try {
                banMember = event.getGuild().getMemberById(parts[1]);
            }
            catch (Exception e) {
                event.getMessage().reply("Invalid snowflake, User was not found!").queue();
                return;
            }
        }

        if (banMember == null) {
            event.getMessage().reply("User was not found!").queue();
            return;
        }

        if (banMember.equals(event.getGuild().getSelfMember())) {
            event.getMessage().reply("You don't really want to ban me... Right? <a:kanna_cry:851143700297941042>").queue();
            return;
        }

        if (banMember.equals(event.getMember())) {
            event.getMessage().reply("As much as want to ban you for everything you have done to me, I unfortunately can't ban you <:NotHuTao:828069127478181888>").queue();
            return;
        }

        if (!event.getGuild().getSelfMember().canInteract(banMember)) {
            event.getMessage().reply("Can not ban " + banMember.getUser().getAsMention() + "(" + banMember.getId() + ") because they are a higher role than my role").setAllowedMentions(Collections.emptyList()).queue();
            return;
        }

        if (reason.isBlank())
            banMember.ban(delDays, TimeUnit.DAYS).reason("Banned by " + event.getAuthor().getName()).queue();
        else
            banMember.ban(delDays, TimeUnit.DAYS).reason(event.getAuthor().getName() + " - " + reason).queue(); //reason limit is 512 chars

        String reportChannel = CommandManager.mlReportChannels.get(event.getGuild().getIdLong());
        if (reportChannel != null && !reportChannel.equals(event.getChannel().asGuildMessageChannel().getId()))
            event.getGuild().getTextChannelById(reportChannel).sendMessage("User " + banMember.getUser().getAsMention() + "(" + banMember.getId() + ") has been banned by " + event.getMember().getEffectiveName() + "!\n" + reason).setAllowedMentions(Collections.emptyList()).queue();
        event.getChannel().sendMessage("User " + banMember.getUser().getAsMention() + "(" + banMember.getId() + ") has been banned!\n" + reason).queue();
    }

    @Override
    protected boolean matchPattern(String paramString) {
        return paramString.startsWith(ConfigManager.discordPrefix + getName());
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
        return "ban";
    }
}
