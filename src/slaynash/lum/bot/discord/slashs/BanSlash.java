package slaynash.lum.bot.discord.slashs;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import slaynash.lum.bot.DBConnectionManagerLum;
import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.utils.ExceptionUtils;
import slaynash.lum.bot.utils.Utils;

public class BanSlash extends Slash {
    private static final Pattern DURATION_PATTERN = Pattern.compile("^(\\d+)([mhd])$", Pattern.CASE_INSENSITIVE);
    private static final long MAX_EXPIRY_MILLIS = Instant.parse("9999-12-31T23:59:59Z").toEpochMilli();

    @Override
    protected CommandData globalSlashData() {
        return Commands.slash("ban", "Ban a user, optionally for a limited time")
            .addOption(OptionType.USER, "user", "User to ban", true)
            .addOption(OptionType.STRING, "reason", "Reason for the ban", false)
            .addOption(OptionType.STRING, "duration", "Temporary ban duration (for example 30m, 12h, 7d)", false)
            .addOption(OptionType.INTEGER, "purge_days", "Delete this many days of the user's messages (0-7)", false)
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS))
            .setContexts(InteractionContextType.GUILD);
    }

    @Override
    protected void slashRun(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.BAN_MEMBERS)) {
            event.reply("You need the `BAN_MEMBERS` permission to use this command.").setEphemeral(true).queue();
            return;
        }
        if (!event.getGuild().getSelfMember().hasPermission(Permission.BAN_MEMBERS)) {
            event.reply("I need the `BAN_MEMBERS` permission to ban users.").setEphemeral(true).queue();
            return;
        }

        User target = event.getOption("user").getAsUser();
        if (target.getIdLong() == event.getGuild().getSelfMember().getIdLong()) {
            event.reply("I can't ban myself.").setEphemeral(true).queue();
            return;
        }
        if (target.getIdLong() == event.getUser().getIdLong()) {
            event.reply("You can't ban yourself.").setEphemeral(true).queue();
            return;
        }

        Member targetMember = event.getGuild().getMember(target);
        if (targetMember != null && !event.getGuild().getSelfMember().canInteract(targetMember)) {
            event.reply("I can't ban that user because they have a higher role than me.").setEphemeral(true).queue();
            return;
        }

        int purgeDays = event.getOption("purge_days") == null ? 0 : event.getOption("purge_days").getAsInt();
        if (purgeDays < 0 || purgeDays > 7) {
            event.reply("Purge days must be between 0 and 7.").setEphemeral(true).queue();
            return;
        }

        String duration = event.getOption("duration") == null ? null : event.getOption("duration").getAsString();
        long expiresAt = 0;
        if (duration != null) {
            try {
                expiresAt = parseExpiry(duration);
            }
            catch (IllegalArgumentException e) {
                event.reply("Invalid duration. Use a positive number followed by `m`, `h`, or `d`, such as `30m`, `12h`, or `7d`.").setEphemeral(true).queue();
                return;
            }
        }

        String reason = event.getOption("reason") == null ? "" : event.getOption("reason").getAsString().trim();
        String auditReason = "Banned by " + event.getUser().getName() + (reason.isEmpty() ? "" : " - " + reason);
        if (auditReason.length() > 512)
            auditReason = auditReason.substring(0, 512);
        String finalAuditReason = auditReason;
        long finalExpiresAt = expiresAt;
        event.deferReply(true).queue(hook -> event.getGuild().ban(target, purgeDays, TimeUnit.DAYS)
            .reason(finalAuditReason)
            .queue(ignored -> {
                try {
                    if (finalExpiresAt == 0)
                        DBConnectionManagerLum.sendUpdate("DELETE FROM `TemporaryBans` WHERE `GuildID` = ? AND `UserID` = ?", event.getGuild().getIdLong(), target.getIdLong());
                    else
                        DBConnectionManagerLum.sendUpdate(
                            "INSERT INTO `TemporaryBans` (`GuildID`, `UserID`, `ExpiresAt`) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `ExpiresAt` = VALUES(`ExpiresAt`)",
                            event.getGuild().getIdLong(), target.getIdLong(), new Date(finalExpiresAt));
                }
                catch (SQLException e) {
                    ExceptionUtils.reportException("Failed to save temporary ban expiry", e);
                    if (finalExpiresAt != 0) {
                        event.getGuild().unban(target).reason("Unable to schedule temporary ban expiry").queue(
                            unbanned -> hook.editOriginal("The ban expiry could not be saved, so I removed the ban instead.").queue(),
                            unbanError -> {
                                ExceptionUtils.reportException("Failed to remove ban after expiry could not be saved", unbanError);
                                hook.editOriginal("The ban was applied, but its expiry could not be saved and automatic unban is not guaranteed.").queue();
                            });
                    }
                    else {
                        hook.editOriginal("The user was banned, but I couldn't update the scheduled-ban record.").queue();
                    }
                    return;
                }

                String message = "User " + target.getAsMention() + " (" + target.getId() + ") has been banned!"
                    + (reason.isEmpty() ? "" : "\nReason: " + reason)
                    + (duration == null ? "" : "\nDuration: " + duration)
                    + "\nPurged message history: " + purgeDays + " day(s).";
                hook.editOriginal(message).queue();

                var reportChannel = CommandManager.getModReportChannels(event, "ban");
                if (reportChannel != null && !reportChannel.equals(event.getChannel()))
                    Utils.sendMessage(message + "\nModerator: " + event.getMember().getEffectiveName(), reportChannel);
            }, error -> {
                ExceptionUtils.reportException("Failed to ban user from slash command", error);
                hook.editOriginal("The ban failed. Please check that I can ban this user and that my role is high enough.").queue();
            }));
    }

    private static long parseExpiry(String duration) {
        Matcher matcher = DURATION_PATTERN.matcher(duration.trim().toLowerCase(Locale.ROOT));
        if (!matcher.matches())
            throw new IllegalArgumentException("Invalid duration format");

        long amount;
        try {
            amount = Long.parseLong(matcher.group(1));
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("Duration is too large", e);
        }
        if (amount <= 0)
            throw new IllegalArgumentException("Duration must be positive");

        long secondsPerUnit = switch (matcher.group(2).toLowerCase(Locale.ROOT)) {
            case "m" -> 60;
            case "h" -> 60 * 60;
            case "d" -> 24 * 60 * 60;
            default -> throw new IllegalArgumentException("Unsupported duration unit");
        };
        try {
            long durationMillis = Math.multiplyExact(Math.multiplyExact(amount, secondsPerUnit), 1000);
            long expiresAt = Math.addExact(System.currentTimeMillis(), durationMillis);
            if (expiresAt > MAX_EXPIRY_MILLIS)
                throw new IllegalArgumentException("Duration exceeds the supported date range");
            return expiresAt;
        }
        catch (ArithmeticException e) {
            throw new IllegalArgumentException("Duration is too large", e);
        }
    }
}
