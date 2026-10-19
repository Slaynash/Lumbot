package slaynash.lum.bot.discord.slashs;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.utils.ExceptionUtils;
import slaynash.lum.bot.utils.Utils;

public class KickSlash extends Slash {
    @Override
    protected CommandData globalSlashData() {
        return Commands.slash("kick", "Kick a user from this server")
            .addOption(OptionType.USER, "user", "User to kick", true)
            .addOption(OptionType.STRING, "reason", "Reason for the kick", false)
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.KICK_MEMBERS))
            .setContexts(InteractionContextType.GUILD);
    }

    @Override
    protected void slashRun(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.KICK_MEMBERS)) {
            event.reply("You need the `KICK_MEMBERS` permission to use this command.").setEphemeral(true).queue();
            return;
        }
        if (!event.getGuild().getSelfMember().hasPermission(Permission.KICK_MEMBERS)) {
            event.reply("I need the `KICK_MEMBERS` permission to kick users.").setEphemeral(true).queue();
            return;
        }

        User target = event.getOption("user").getAsUser();
        if (target.getIdLong() == event.getGuild().getSelfMember().getIdLong()) {
            event.reply("Please don't kick me, I have been a good bot.").setEphemeral(true).queue();
            return;
        }
        if (target.getIdLong() == event.getUser().getIdLong()) {
            event.reply("You can't kick yourself.").setEphemeral(true).queue();
            return;
        }

        String reason = event.getOption("reason") == null ? "" : event.getOption("reason").getAsString().trim();
        String auditReason = "Kicked by " + event.getUser().getName() + (reason.isEmpty() ? "" : " - " + reason);
        if (auditReason.length() > 512)
            auditReason = auditReason.substring(0, 512);
        String finalAuditReason = auditReason;

        event.deferReply(true).queue(hook -> event.getGuild().retrieveMember(target).queue(member -> {
            if (!event.getGuild().getSelfMember().canInteract(member)) {
                hook.editOriginal("I can't kick that user because they have a higher role than me.").queue();
                return;
            }

            member.kick().reason(finalAuditReason).queue(ignored -> {
                String message = "User " + target.getAsMention() + " (" + target.getId() + ") has been kicked!"
                    + (reason.isEmpty() ? "" : "\nReason: " + reason);
                hook.editOriginal(message).queue();

                var reportChannel = CommandManager.getModReportChannels(event, "kick");
                if (reportChannel != null && !reportChannel.equals(event.getChannel()))
                    Utils.sendMessage(message + "\nModerator: " + event.getMember().getEffectiveName(), reportChannel);
            }, error -> {
                ExceptionUtils.reportException("Failed to kick user from slash command", error);
                hook.editOriginal("The kick failed. Please check that I can kick this user and that my role is high enough.").queue();
            });
        }, error -> {
            hook.editOriginal("That user is not a member of this server and can't be kicked.").queue();
        }));
    }
}
