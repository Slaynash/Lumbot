package slaynash.lum.bot.discord.slashs;

import java.sql.ResultSet;

import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import slaynash.lum.bot.ConfigManager;
import slaynash.lum.bot.DBConnectionManagerLum;
import slaynash.lum.bot.utils.ExceptionUtils;

public class SetVRCAPI extends Slash {
    @Override
    protected CommandData globalSlashData() {
        return Commands.slash("vrcapi", "Set log channel for VRChat API changes - Admins only").setDefaultPermissions(DefaultMemberPermissions.DISABLED).setGuildOnly(true);
    }

    @Override
    public void slashRun(SlashCommandInteractionEvent event) {
        if (event.getChannelType() == ChannelType.PRIVATE) {
            event.reply("This command does not work in DMs").setEphemeral(true).queue();
        }
        else if (!ConfigManager.mainBot) {
            event.reply("This command is not available on backup bot, please wait for main bot to come back online.").setEphemeral(true).queue();
        }
        else if (!event.getGuild().getSelfMember().hasPermission(event.getGuildChannel(), net.dv8tion.jda.api.Permission.MESSAGE_SEND)) {
            event.reply("Lum does not have " + net.dv8tion.jda.api.Permission.MESSAGE_SEND.getName() + " permission").setEphemeral(true).queue();
        }
        else if (!event.getGuild().getSelfMember().hasPermission(event.getGuildChannel(), net.dv8tion.jda.api.Permission.MESSAGE_EMBED_LINKS)) {
            event.reply("Lum does not have " + net.dv8tion.jda.api.Permission.MESSAGE_EMBED_LINKS.getName() + " permission").setEphemeral(true).queue();
        }
        else try {
                String guildID = event.getGuild().getId();
                String channelID = event.getChannel().getId();
                ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT * FROM `GuildConfigurations` WHERE VRCAPI = ?", channelID);
                if (rs.next()) {
                    DBConnectionManagerLum.sendUpdate("UPDATE `GuildConfigurations` SET `VRCAPI` = NULL WHERE GuildID = ?", guildID);
                    event.reply("VRChat API monitor is now disabled in this channel").setEphemeral(true).queue();
                }
                else {
                    DBConnectionManagerLum.sendUpdate("UPDATE `GuildConfigurations` SET `VRCAPI` = ? WHERE GuildID = ?", channelID, guildID);
                    event.reply("VRChat API monitor is now enabled in this channel and reports will be sent to " + event.getChannel().getName()).setEphemeral(true).queue();
                }
                DBConnectionManagerLum.closeRequest(rs);
            }
            catch (Exception e) {
                ExceptionUtils.reportException("Failed to set meme channel", e, event.getChannel());
            }
    }
}
