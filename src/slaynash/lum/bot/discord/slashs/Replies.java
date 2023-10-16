package slaynash.lum.bot.discord.slashs;

import java.awt.Color;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.code.regexp.Matcher;
import com.google.code.regexp.Pattern;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.FileUpload;
import slaynash.lum.bot.ConfigManager;
import slaynash.lum.bot.DBConnectionManagerLum;
import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.utils.ExceptionUtils;

//TODO add more replacements like username, server age
//TODO figure out a way to remove a option, you can't send an empty option

public class Replies extends Slash {
    @Override
    protected CommandData globalSlashData() {
        return Commands.slash("reply", "Custom Replies")
            .addSubcommands(new SubcommandData("list", "List all current replies"))
            .addSubcommands(new SubcommandData("add", "Add or Update reply")
                .addOption(OptionType.INTEGER, "ukey", "Reply Key used to update existing Reply", false)
                .addOption(OptionType.STRING,  "message", "Enter Message to send on trigger", false)
                .addOption(OptionType.STRING,  "regex", "Use regex matching (regex needs to match all of user's message)", false)
                .addOption(OptionType.STRING,  "contains", "Term that message needs to contain", false)
                .addOption(OptionType.STRING,  "equals", "Term that message needs to equal", false)
                .addOption(OptionType.USER,    "user", "Trigger if someone sends a message", false)
                .addOption(OptionType.BOOLEAN, "delete", "Should User's message be deleted?", false)
                .addOption(OptionType.BOOLEAN, "kick", "Should the User be kicked?", false)
                .addOption(OptionType.BOOLEAN, "ban", "Should the User be banned?", false)
                .addOption(OptionType.BOOLEAN, "bot", "Allow replying to other bots", false)
                .addOption(OptionType.BOOLEAN, "edit", "Allow replying to when member edits their message", false)
                .addOption(OptionType.CHANNEL, "channel", "Allow reply in only a single channel", false)
                .addOption(OptionType.ROLE,    "ignorerole", "Prevent triggering if user has role", false)
                .addOption(OptionType.BOOLEAN, "report", "Report if this reply was triggered", false)
            //  .addOption(OptionType.INTEGER, "repeat", "Trigger if repeated command", false) //todo Maybe later
            )
            .addSubcommands(new SubcommandData("delete", "Remove a reply")
                .addOption(OptionType.INTEGER, "ukey", "Reply Key used to delete existing Reply", true))
            .setDefaultPermissions(DefaultMemberPermissions.DISABLED)
            .setGuildOnly(true);
    }

    @Override
    public void slashRun(SlashCommandInteractionEvent event) {
        if (!ConfigManager.mainBot) {
            event.reply("Lum is running on Backup mode. Replies is in readonly mode and maybe a bit outdated.").setEphemeral(true).queue();
            return;
        }
        System.out.println("Options size: " + event.getOptions().size());
        if (event.getChannelType() == ChannelType.PRIVATE) {
            event.reply("Replies currently does not work in DMs").queue();
            return;
        }
        if (event.getSubcommandName() == null) { // Don't think this is needed but won't hurt to have
            event.reply("Please specify a subcommand").queue();
            return;
        }
        long muserid = event.getUser().getIdLong();
        long guildid = event.getGuild().getIdLong();
        String regex = event.getOption("regex") == null ? null : event.getOption("regex").getAsString().replace("\\n", "\n").toLowerCase();
        String contains = event.getOption("contains") == null ? null : event.getOption("contains").getAsString().replace("\\n", "\n").toLowerCase();
        String equals = event.getOption("equals") == null ? null : event.getOption("equals").getAsString().replace("\\n", "\n").toLowerCase();
        String message = event.getOption("message") == null ? null : event.getOption("message").getAsString().replace("\\n", "\n");
        String user = event.getOption("user") == null ? null : event.getOption("user").getAsUser().getId();
        String channel = event.getOption("channel") == null ? null : event.getOption("channel").getAsChannel().getId();
        String ignorerole = event.getOption("ignorerole") == null ? null : event.getOption("ignorerole").getAsRole().getId();

        boolean delete = false;
        if (event.getOption("delete") != null && event.getOption("delete").getAsBoolean()) {
            delete = true;
            if (!event.getGuild().getSelfMember().hasPermission(event.getChannel().asGuildMessageChannel(), Permission.MESSAGE_MANAGE)) {
                event.reply("I don't have the `MESSAGE_MANAGE` permission in this channel. I won't be able to delete the triggered message.").queue();
                return;
            }
        }
        boolean kick = false;
        if (event.getOption("kick") != null && event.getOption("kick").getAsBoolean()) {
            kick = true;
            if (!event.getGuild().getSelfMember().hasPermission(event.getChannel().asGuildMessageChannel(), Permission.KICK_MEMBERS)) {
                event.reply("I don't have the `KICK_MEMBERS` permission in this channel. I won't be able to kick the user.").queue();
                return;
            }
        }
        boolean ban = false;
        if (event.getOption("ban") != null && event.getOption("ban").getAsBoolean()) {
            ban = true;
            if (!event.getGuild().getSelfMember().hasPermission(event.getChannel().asGuildMessageChannel(), Permission.BAN_MEMBERS)) {
                event.reply("I don't have the `BAN_MEMBERS` permission in this channel. I won't be able to ban the user.").queue();
                return;
            }
        }
        boolean bot = event.getOption("bot") != null && event.getOption("bot").getAsBoolean();
        boolean edit = event.getOption("edit") != null && event.getOption("edit").getAsBoolean();
        boolean report = event.getOption("report") != null && event.getOption("report").getAsBoolean();
        if (report) {
            TextChannel reportChannel = event.getGuild().getTextChannelById(CommandManager.mlReportChannels.getOrDefault(event.getGuild().getIdLong(), "0"));
            if (reportChannel == null) {
                event.reply("I can't find the report channel. Please set it with `l!setmlreportchannel` in the log channel").queue();
            }
            if (!event.getGuild().getSelfMember().hasPermission(reportChannel, Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND)) {
                event.reply("I can not send reports to the report channel as I do not have permission to view or send messages in that channel.").queue();
            }
        }

        if (event.getChannel().asTextChannel() != null && event.getChannel().asTextChannel().getParentCategoryIdLong() == 924780998124798022L) guildid = 0;

        if (event.getSubcommandName().equals("list")) {
            int c = 0;
            StringBuilder sb = new StringBuilder();
            try {
                String sql = "SELECT * FROM `Replies` WHERE `guildID` = '" + event.getGuild().getId() + "' ORDER BY `ukey` ASC";
                ResultSet rs = DBConnectionManagerLum.sendRequest(sql);
                sb.append("Current replies in this guild:\n");

                while (rs.next()) {
                    sb.append("Reply ukey: ").append(rs.getInt("ukey"));
                    sb.append(rs.getBoolean("bdelete") ? "\tdelete " : "");
                    sb.append(rs.getBoolean("bkick") ? "\tkick " : "");
                    sb.append(rs.getBoolean("bban") ? "\tban" : "");
                    sb.append(rs.getBoolean("bbot") ? "\tbot" : "");
                    sb.append(rs.getBoolean("bedit") ? "\tedit" : "");
                    sb.append(rs.getBoolean("breport") ? "\treport" : "");
                    sb.append("\n");
                    if (rs.getString("regex") != null) {
                        sb.append("\tRegex: ").append(rs.getString("regex").replace("\n", "\n\t\t")).append("\n");
                    }
                    if (rs.getString("contains") != null) {
                        sb.append("\tContains: ").append(rs.getString("contains").replace("\n", "\n\t\t")).append("\n");
                    }
                    if (rs.getString("equals") != null) {
                        sb.append("\tEquals: ").append(rs.getString("equals").replace("\n", "\n\t\t")).append("\n");
                    }
                    if (rs.getString("user") != null) {
                        sb.append("\tUser: ").append(event.getJDA().getUserById(rs.getString("user")).getEffectiveName()).append("\n");
                    }
                    if (rs.getString("channel") != null) {
                        sb.append("\tChannel: ").append(event.getJDA().getTextChannelById(rs.getString("channel")).getName()).append("\n");
                    }
                    if (rs.getString("ignorerole") != null) {
                        sb.append("\tIgnored Role: ").append(event.getJDA().getRoleById(rs.getString("ignorerole")).getName()).append("\n");
                    }
                    if (rs.getString("message") != null) {
                        sb.append("\tMessage: ").append(rs.getString("message").replace("\n", "\n\t\t")).append("\n");
                    }
                    c++;
                }
                DBConnectionManagerLum.closeRequest(rs);
            }
            catch (SQLException e) {
                ExceptionUtils.reportException("Failed to get reply to List", e, event.getChannel());
                return;
            }

            if (c == 0) {
                event.reply("No replies in this guild").queue();
            }
            else {
                if (event.getGuild().getSelfMember().hasPermission(event.getChannel().asGuildMessageChannel(), Permission.MESSAGE_ATTACH_FILES))
                    event.replyFiles(FileUpload.fromData(sb.toString().getBytes(), event.getGuild().getName() + " replies.txt")).queue();
                else
                    event.reply("I cant send logs into this channel. Please give me MESSAGE_ATTACH_FILES perms and try again.").queue();
            }
        }
        else if (event.getSubcommandName().equals("add")) {
            if (event.getOption("ukey") == null && regex == null && contains == null && equals == null || event.getOption("ukey") != null && event.getOptions().size() == 1) {
                event.reply("Please set at least one option").queue();
                return;
            }
            if (regex != null) {
                try {
                    Pattern.compile(regex);
                }
                catch (Exception e) {
                    event.replyEmbeds(new EmbedBuilder().setTitle("Invalid Regex!").setDescription("Please use a site like [regex101](https://regex101.com/) to test regex").setColor(Color.RED).build()).queue();
                    return;
                }
            }
            if (message != null) {
                if (message.length() > Message.MAX_CONTENT_LENGTH) {
                    event.reply("Message is too long. Please use a shorter message.").queue();
                    return;
                }
                Pattern p = Pattern.compile("<a?:\\w+:(?<id>\\d+)>", Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(message);
                while (m.find()) {
                    RichCustomEmoji emote = event.getJDA().getEmojiById(m.group("id"));
                    try {
                        if (!emote.canInteract(emote.getGuild().getSelfMember())) {
                            event.reply("Lum can not use that emote.").queue();
                            return;
                        }
                    }
                    catch (Exception e) {
                        event.reply("Lum can not use that emote as I also need to be in that emote's server.").queue();
                        return;
                    }
                }
            }

            if (event.getOption("ukey") == null) {
                if (event.getOption("regex") == null && event.getOption("contains") == null && event.getOption("equals") == null)
                    event.getChannel().asGuildMessageChannel().sendMessage("This will trigger on every message, I hope you know what you are going").queue();
                try {
                    DBConnectionManagerLum.sendUpdate("INSERT INTO `Replies` (`guildID`, `regex`, `contains`, `equals`, `message`, `user`, `channel`, `ignorerole`, `bdelete`, `bkick`, `bban`, `bbot`, `bedit`, `breport`, `lastedited`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", guildid, regex, contains, equals, message, user, channel, ignorerole, delete, kick, ban, bot, edit, report, muserid);

                    ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT `ukey` FROM `Replies` WHERE `guildID` = ? ORDER BY `ukey` DESC LIMIT 1", guildid);
                    rs.next();
                    int ukey = rs.getInt("ukey");

                    event.reply("Added reply, ukey: " + ukey).queue();

                    DBConnectionManagerLum.closeRequest(rs);
                }
                catch (SQLException e) {
                    ExceptionUtils.reportException("Failed to Add reply", e, event.getChannel());
                }
            }
            else {
                long ukey = event.getOption("ukey").getAsLong();
                try {
                    ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT `Replies`.`guildID` FROM `Replies` WHERE `Replies`.`ukey` = ? LIMIT 1", ukey);
                    if (!rs.next() || rs.getLong("guildID") != guildid) {
                        event.reply("Invalid ukey or reply not found!").setEphemeral(true).queue();
                        DBConnectionManagerLum.closeRequest(rs);
                        return;
                    }
                    DBConnectionManagerLum.closeRequest(rs);
                }
                catch (SQLException e) {
                    ExceptionUtils.reportException("Failed to check for reply", e, event.getChannel());
                    return;
                }

                try { // not sure how to combine them so updating one at a time
                    DBConnectionManagerLum.sendUpdate("UPDATE `Replies` Set lastedited = ? WHERE `Replies`.`ukey` = ?", muserid, ukey);
                    if (message != null)
                        DBConnectionManagerLum.sendUpdate("UPDATE `Replies` Set message = ? WHERE `Replies`.`ukey` = ?", message, ukey);
                    if (regex != null)
                        DBConnectionManagerLum.sendUpdate("UPDATE `Replies` Set regex = ? WHERE `Replies`.`ukey` = ?", regex, ukey);
                    if (contains != null)
                        DBConnectionManagerLum.sendUpdate("UPDATE `Replies` Set contains = ? WHERE `Replies`.`ukey` = ?", contains, ukey);
                    if (equals != null)
                        DBConnectionManagerLum.sendUpdate("UPDATE `Replies` Set equals = ? WHERE `Replies`.`ukey` = ?", equals, ukey);
                    if (user != null)
                        DBConnectionManagerLum.sendUpdate("UPDATE `Replies` Set user = ? WHERE `Replies`.`ukey` = ?", user, ukey);
                    if (channel != null)
                        DBConnectionManagerLum.sendUpdate("UPDATE `Replies` Set channel = ? WHERE `Replies`.`ukey` = ?", channel, ukey);
                    if (ignorerole != null)
                        DBConnectionManagerLum.sendUpdate("UPDATE `Replies` Set ignorerole = ? WHERE `Replies`.`ukey` = ?", ignorerole, ukey);
                    if (event.getOption("delete") != null)
                        DBConnectionManagerLum.sendUpdate("UPDATE `Replies` Set bdelete = ? WHERE `Replies`.`ukey` = ?", delete, ukey);
                    if (event.getOption("kick") != null)
                        DBConnectionManagerLum.sendUpdate("UPDATE `Replies` Set bkick = ? WHERE `Replies`.`ukey` = ?", kick, ukey);
                    if (event.getOption("ban") != null)
                        DBConnectionManagerLum.sendUpdate("UPDATE `Replies` Set bban = ? WHERE `Replies`.`ukey` = ?", ban, ukey);
                    if (event.getOption("bot") != null)
                        DBConnectionManagerLum.sendUpdate("UPDATE `Replies` Set bbot = ? WHERE `Replies`.`ukey` = ?", bot, ukey);
                    if (event.getOption("edit") != null)
                        DBConnectionManagerLum.sendUpdate("UPDATE `Replies` Set bedit = ? WHERE `Replies`.`ukey` = ?", edit, ukey);
                    if (event.getOption("report") != null)
                        DBConnectionManagerLum.sendUpdate("UPDATE `Replies` Set breport = ? WHERE `Replies`.`ukey` = ?", report, ukey);
                    event.reply("Reply " + ukey + " updated!").queue();
                }
                catch (SQLException e) {
                    ExceptionUtils.reportException("Failed to Update reply", e, event.getChannel());
                }
            }
        }
        else if (event.getSubcommandName().equals("delete")) {
            int deleted;
            long ukey = event.getOption("ukey").getAsLong();
            try {
                String update = "DELETE FROM `Replies` WHERE `guildID` = '" + event.getGuild().getId() + "' AND `ukey` = '" + ukey + "'";
                deleted = DBConnectionManagerLum.sendUpdate(update);
            }
            catch (SQLException e) {
                ExceptionUtils.reportException("Failed to Delete reply", e, event.getChannel());
                return;
            }
            if (deleted == 0) {
                event.reply("No reply found with that trigger").queue();
            }
            else {
                event.reply("Deleted reply " + ukey).queue();
            }

        }
        else {
            event.reply("Unknown subcommand").queue();
        }
    }
}
