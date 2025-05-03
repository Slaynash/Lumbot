package slaynash.lum.bot.discord;

import java.awt.Color;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.gcardone.junidecode.Junidecode;
import slaynash.lum.bot.DBConnectionManagerLum;
import slaynash.lum.bot.discord.utils.CrossServerUtils;
import slaynash.lum.bot.utils.ExceptionUtils;
import slaynash.lum.bot.utils.Utils;

public class Members {

    public static void logMemberJoin(GuildMemberJoinEvent event) {
        MessageChannelUnion report = CommandManager.getModReportChannels(event.getGuild(), "joins");
        if (report != null) {
            String displayName;
            if (event.getUser().getGlobalName() == null || event.getUser().getName().equals(event.getUser().getGlobalName()))
                displayName = event.getUser().getName();
            else
                displayName = event.getUser().getName() + " (" + event.getUser().getGlobalName() + ")";

            // Check if the user was in guild before
            boolean isRejoining = false;
            long time_left = 0;
            long time_joined = event.getMember().hasTimeJoined() ? event.getMember().getTimeJoined().toEpochSecond() : Instant.now().getEpochSecond();
            try {
                String sql = "SELECT * FROM Users WHERE user_id = ? AND guild_id = ?";
                ResultSet rs = DBConnectionManagerLum.sendRequest(sql, event.getUser().getId(), event.getGuild().getId());
                isRejoining = rs.next();
                if (isRejoining) {
                    time_left = rs.getLong("time_left");
                }
                DBConnectionManagerLum.closeRequest(rs);
            }
            catch (Exception e) {
                ExceptionUtils.reportException("Failed to check if user is rejoining", e);
            }

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("User Join");
            embed.setColor(Color.green);
            embed.addField("User", event.getUser().getAsMention() + "\n" + displayName, false);
            embed.addField("Account created", "<t:" + event.getUser().getTimeCreated().toEpochSecond() + ":f>", false);
            embed.setThumbnail(event.getUser().getEffectiveAvatarUrl());
            embed.setTimestamp(Instant.now());
            String name = Junidecode.unidecode(event.getUser().getName() + event.getUser().getGlobalName()).toLowerCase().replaceAll("[^ a-z]", "");
            if (CrossServerUtils.testSlurs(name) || name.contains("discord") || name.contains("developer") || name.contains("hypesquad") || name.contains("academy recruitments")) {
                embed.addField("", "Sussy Username", false);
            }

            if (isRejoining) {
                embed.setTitle("User Rejoining");
                embed.setColor(Color.decode("42069"));
                if (time_left > 0) {
                    embed.addField("Last seen", Utils.secToTime(time_joined - time_left) + " ago\n<t:" + time_left + ":f>", false);
                }
                else {
                    embed.addField("Last seen", "Unknown", false);
                }
            }

            Utils.sendEmbed(embed.build(), report);
        }
        // Update user in the database
        try {
            long time_joined = event.getMember().hasTimeJoined() ? event.getMember().getTimeJoined().toEpochSecond() : Instant.now().getEpochSecond();
            String sql = "INSERT INTO Users (user_id, guild_id, time_joined) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `time_joined`=?;";
            DBConnectionManagerLum.sendUpdate(sql, event.getUser().getId(), event.getGuild().getId(), time_joined, time_joined);
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to log user join", e);
        }
    }

    public static void logMemberLeave(GuildMemberRemoveEvent event) {
        MessageChannelUnion report = CommandManager.getModReportChannels(event.getGuild(), "joins");
        if (report != null) {
            long time_joined = 0;
            // get time_joined
            try {
                String sql = "SELECT * FROM Users WHERE user_id = ? AND guild_id = ?";
                ResultSet rs = DBConnectionManagerLum.sendRequest(sql, event.getUser().getId(), event.getGuild().getId());
                if (rs.next()) {
                    time_joined = rs.getLong("time_joined");
                }
                DBConnectionManagerLum.closeRequest(rs);
            }
            catch (Exception e) {
                ExceptionUtils.reportException("Failed to check if user is rejoining", e);
            }

            String displayName;
            if (event.getUser().getGlobalName() == null || event.getUser().getName().equals(event.getUser().getGlobalName()))
                displayName = event.getUser().getName();
            else
                displayName = event.getUser().getName() + " (" + event.getUser().getGlobalName() + ")";

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("User Left");
            embed.setColor(Color.red);
            embed.addField("User", event.getUser().getAsMention() + "\n" + displayName, false);
            if (time_joined > 0) {
                embed.addField("Stay duration", Utils.secToTime(Instant.now().getEpochSecond() - time_joined), false);
            }
            embed.setThumbnail(event.getUser().getEffectiveAvatarUrl());
            embed.setTimestamp(Instant.now());
            Utils.sendEmbed(embed.build(), report);
        }
        // Update user from the database
        try {
            String sql = "UPDATE Users SET time_left = ? WHERE user_id = ? AND guild_id = ?";
            DBConnectionManagerLum.sendUpdate(sql, Instant.now().getEpochSecond(), event.getUser().getId(), event.getGuild().getId());
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to log user leave", e);
        }
    }

    public static void logUpdateNickname(GuildMemberUpdateNicknameEvent event) {
        MessageChannelUnion report = CommandManager.getModReportChannels(event.getGuild(), "users");
        if (report == null) return;

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("User Nickname Change");
        embed.setColor(Color.yellow);
        embed.addField("User", event.getUser().getAsMention(), false);
        embed.addField("Old Nickname", event.getOldNickname() == null ? "None" : event.getOldNickname(), false);
        embed.addField("New Nickname", event.getNewNickname() == null ? "None" : event.getNewNickname(), false);
        embed.setThumbnail(event.getUser().getEffectiveAvatarUrl());
        embed.setTimestamp(Instant.now());
        String name = Junidecode.unidecode(event.getNewNickname()).toLowerCase().replaceAll("[^ a-z]", "");
        if (CrossServerUtils.testSlurs(name) || name.contains("discord") || name.contains("developer") || name.contains("hypesquad") || name.contains("academy recruitments")) {
            embed.addField("", "Sussy Username", false);
        }
        Utils.sendEmbed(embed.build(), report);
    }

    public static void logUsernameChange(UserUpdateNameEvent event) {
        List<Guild> mutualGuilds = new ArrayList<>(event.getUser().getMutualGuilds());

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("User Name Change");
        embed.setColor(Color.yellow);
        embed.addField("User", event.getUser().getAsMention(), false);
        embed.addField("Before", event.getOldName(), false);
        embed.addField("After", event.getNewName(), false);
        embed.setThumbnail(event.getUser().getEffectiveAvatarUrl());
        embed.setTimestamp(Instant.now());
        String name = Junidecode.unidecode(event.getUser().getName()).toLowerCase().replaceAll("[^ a-z]", "");
        if (CrossServerUtils.testSlurs(name) || name.contains("discord") || name.contains("developer") || name.contains("hypesquad") || name.contains("academy recruitments")) {
            embed.addField("", "Sussy Username", false);
        }

        for (Guild guild : mutualGuilds) {
            MessageChannelUnion report = CommandManager.getModReportChannels(guild, "users");
            if (report == null) return;
            Utils.sendEmbed(embed.build(), report);
        }
    }

    // load all users when Lum joins a server
    public static void loadAllUsers(Guild guild) {
        guild.loadMembers().onSuccess(members -> {
            for (Member member : members) {
                String sql = "INSERT INTO Users (user_id, guild_id, time_joined) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `time_joined`=?;";
                String time_joined = member.hasTimeJoined() ? String.valueOf(member.getTimeJoined().toEpochSecond()) : "NULL";
                try {
                    DBConnectionManagerLum.sendUpdate(sql, member.getId(), guild.getId(), time_joined, time_joined);
                }
                catch (Exception e) {
                    ExceptionUtils.reportException("Failed to load all users", e);
                }
            }
        }).onError(error -> {
            ExceptionUtils.reportException("Failed to load all users", error);
        });
    }
}
