package slaynash.lum.bot.discord.slashs;

import java.sql.ResultSet;
import java.util.List;

import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import slaynash.lum.bot.ConfigManager;
import slaynash.lum.bot.DBConnectionManagerLum;
import slaynash.lum.bot.utils.ExceptionUtils;

public class SetMemes extends Slash {
    @Override
    protected CommandData globalSlashData() {
        return Commands.slash("meme", "Auto moderate a meme channel - Admins only").addOption(OptionType.CHANNEL, "report", "Optional Channel for message logs", false).setDefaultPermissions(DefaultMemberPermissions.DISABLED);
    }

    @Override
    public void slashRun(SlashCommandInteractionEvent event) {
        List<OptionMapping> reportChannel = event.getOptionsByName("report");
        if (event.getChannelType() == ChannelType.PRIVATE) {
            event.reply("This command does not work in DMs").setEphemeral(true).queue();
        }
        else if (!ConfigManager.mainBot) {
            event.reply("This command is not available on backup bot, please wait for main bot to come back online.").setEphemeral(true).queue();
        }
        else if (!reportChannel.isEmpty() && reportChannel.get(0).getAsLong() == event.getChannel().getIdLong()) {
            event.reply("You can't set the report channel to the same channel as the meme channel").setEphemeral(true).queue();
        }
        else try {
                String guildID = event.getGuild().getId();
                String channelID = event.getChannel().getId();
                ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT `ReportChannel` FROM `Memes` WHERE MemeChannel = ?", channelID);
                if (rs.next()) {
                    DBConnectionManagerLum.sendUpdate("DELETE FROM `Memes` WHERE MemeChannel = ?", channelID);
                }
                else {
                    DBConnectionManagerLum.sendUpdate("INSERT INTO `Memes` (`GuildID`, `user`, `MemeChannel`, `ReportChannel`) VALUES (?, ?, ?, ?)", guildID, event.getUser().getIdLong(), channelID, reportChannel.isEmpty() ? null : reportChannel.get(0).getAsChannel().getId());
                }
                DBConnectionManagerLum.closeRequest(rs);
                if (reportChannel.isEmpty()) {
                    event.reply("Meme moderation is now enabled in this channel").setEphemeral(true).queue();
                }
                else {
                    event.reply("Meme moderation is now enabled in this channel and reports will be sent to " + reportChannel.get(0).getAsChannel().getAsMention()).setEphemeral(true).queue();
                }
            }
            catch (Exception e) {
                ExceptionUtils.reportException("Failed to set meme channel", e, event.getChannel());
            }
    }
}
