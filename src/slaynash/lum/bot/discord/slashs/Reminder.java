package slaynash.lum.bot.discord.slashs;

import java.awt.Color;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import slaynash.lum.bot.ConfigManager;
import slaynash.lum.bot.DBConnectionManagerLum;
import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.utils.ExceptionUtils;
import slaynash.lum.bot.utils.Utils;


public class Reminder extends Slash {
    @Override
    protected CommandData globalSlashData() {
        return Commands.slash("reminder", "Set yourself a reminder. No options will show your current reminders")
                .addOption(OptionType.INTEGER,  "minutes", "How many minutes?", false)
                .addOption(OptionType.INTEGER,  "hours", "How many Hours?", false)
                .addOption(OptionType.INTEGER,  "days", "How many Days?", false)
                .addOption(OptionType.STRING,  "message", "Enter Message to send on trigger", false)
                .addOption(OptionType.STRING,  "color", "Hex color of embed", false)
            .setDefaultPermissions(DefaultMemberPermissions.ENABLED);
    }

    @Override
    public void slashRun(SlashCommandInteractionEvent event) {
        if (!ConfigManager.mainBot) {
            event.reply("Lum is running on Backup mode. Replies is in readonly mode and maybe a bit outdated.").setEphemeral(true).queue();
            return;
        }

        // Get the options
        long userid = event.getUser().getIdLong();
        int minutes = event.getOption("minutes") == null ? 0 : event.getOption("minutes").getAsInt();
        int hours = event.getOption("hours") == null ? 0 : event.getOption("hours").getAsInt();
        int days = event.getOption("days") == null ? 0 : event.getOption("days").getAsInt();
        String message = event.getOption("message") == null ? null : event.getOption("message").getAsString().replace("\\n", "\n");

        long time = ((long) minutes * 60 * 1000) + ((long) hours * 60 * 60 * 1000) + ((long) days * 24 * 60 * 60 * 1000);
        long timestamp = System.currentTimeMillis() + time;

        long guildid;
        long channelid;
        if (event.getChannelType() == ChannelType.PRIVATE) {
            guildid = 0;
            channelid = 0;
        }
        else {
            guildid = event.getGuild().getIdLong();
            channelid = event.getChannel().getIdLong();
        }

        if (time == 0) {
            try {
                ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT `TSend`, CURRENT_TIMESTAMP, `Message` FROM `Reminders` WHERE `UserID` = " + userid + " AND `ServerID` = " + guildid);
                boolean hasReminder = false;
                StringBuilder sb = new StringBuilder();
                while (rs.next()) {
                    hasReminder = true;
                    long diff = rs.getTimestamp("TSend").getTime() - rs.getTimestamp("CURRENT_TIMESTAMP").getTime();
                    sb.append(rs.getString("Message")).append("\n").append("Time Remaining: ");
                    if (TimeUnit.MILLISECONDS.toDays(diff) > 0)
                        sb.append(TimeUnit.MILLISECONDS.toDays(diff)).append(" days ");
                    if (TimeUnit.MILLISECONDS.toHours(diff) > 0)
                        sb.append(TimeUnit.MILLISECONDS.toHours(diff) % 24).append(" hours ");
                    if (TimeUnit.MILLISECONDS.toMinutes(diff) > 0)
                        sb.append(TimeUnit.MILLISECONDS.toMinutes(diff) % 60).append(" minutes ");
                    sb.append("\n\n");
                }
                DBConnectionManagerLum.closeRequest(rs);
                if (hasReminder) {
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setTitle("Your reminders");
                    eb.setDescription(sb.toString());
                    eb.setColor(Color.GREEN);
                    event.replyEmbeds(eb.build()).queue();
                }
                else {
                    event.replyEmbeds(Utils.wrapMessageInEmbed("You don't have any reminder")).queue();
                }
            }
            catch (SQLException e) {
                ExceptionUtils.reportException("Failed to get user reminders", e);
            }
            return;
        }

        // Set color
        Color color;
        if (event.getOption("color") != null) {
            String colort = event.getOption("color").getAsString();
            if (colort.startsWith("#")) {
                colort = colort.substring(1);
            }
            for (char c:colort.toCharArray()) {
                if (!('0' <= c && c <= '9' || 'a' <= c && c <= 'f' || 'A' <= c && c <= 'F')) {
                    event.replyEmbeds(Utils.wrapMessageInEmbed("Bad hex color !\nExemple (pure green): 00ff00", Color.RED)).queue();
                    return;
                }
            }
            color = CommandManager.hex2Rgb(colort);
        }
        else {
            color = new Color(-13223617);
        }

        // Create the reminder
        if (timestamp / 1000 > Integer.MAX_VALUE) {
            event.replyEmbeds(Utils.wrapMessageInEmbed("Reminder too far in the future! [Can't be past 2038](https://en.wikipedia.org/wiki/Year_2038_problem)", Color.RED)).queue();
            return;
        }
        java.sql.Timestamp sqlTimestamp = new java.sql.Timestamp(timestamp);
        try {
            DBConnectionManagerLum.sendUpdate("INSERT INTO Reminders (UserID, ServerID, ChannelID, Message, TSend, Color) VALUES (?, ?, ?, ?, ?, ?)", userid, guildid, channelid, message, sqlTimestamp, color.getRGB());
        }
        catch (SQLException e) {
            ExceptionUtils.reportException("Error saving Reminder", e);
            event.reply("Error saving Reminder, Sent a message to devs").queue();
        }
        event.replyEmbeds(new EmbedBuilder().setColor(color).setTitle("Reminder set").setDescription(message).setTimestamp(sqlTimestamp.toInstant()).build()).queue();
    }
}
