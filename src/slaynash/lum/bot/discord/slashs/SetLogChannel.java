package slaynash.lum.bot.discord.slashs;

import java.sql.ResultSet;

import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import slaynash.lum.bot.ConfigManager;
import slaynash.lum.bot.DBConnectionManagerLum;
import slaynash.lum.bot.utils.ExceptionUtils;

public class SetLogChannel extends Slash {
    final OptionData optionType = new OptionData(OptionType.STRING, "type", "Log Type, Don't set this to print current log settings", false).addChoices(
            // value is the SQL column name
            new Command.Choice("MelonLogs", "melon"),
            new Command.Choice("ScamShield", "scam"),
            // new Command.Choice("Message", "message"),
            new Command.Choice("Kick", "kick"),
            new Command.Choice("Ban", "ban"),
            new Command.Choice("Join/Leave", "joins"),
            new Command.Choice("Replies", "reply"),
            new Command.Choice("Roles from reaction listener", "role"),
            new Command.Choice("Users", "users")
            );
    @Override
    protected CommandData globalSlashData() {
        return Commands.slash("log", "Set channel to place moderation logs - Admins only").setDefaultPermissions(DefaultMemberPermissions.DISABLED).setGuildOnly(true)
            .addOptions(optionType);
    }

    @Override
    public void slashRun(SlashCommandInteractionEvent event) {
        if (event.getChannelType() == ChannelType.PRIVATE) {
            event.reply("This command does not work in DMs").setEphemeral(true).queue();
        }
        else if (!ConfigManager.mainBot) {
            event.reply("This command is not available on backup bot, please wait for main bot to come back online.").setEphemeral(true).queue();
        }
        else if (!event.getGuildChannel().canTalk()) {
            event.reply("Lum can not talk in this channel").setEphemeral(true).queue();
        }
        else if (!event.getGuild().getSelfMember().hasPermission(event.getGuildChannel(), net.dv8tion.jda.api.Permission.MESSAGE_EMBED_LINKS)) {
            event.reply("Lum does not have " + net.dv8tion.jda.api.Permission.MESSAGE_EMBED_LINKS.getName() + " permission").setEphemeral(true).queue();
        }
        else try {
                String guildID = event.getGuild().getId();
                String channelID = event.getChannel().getId();
                ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT * FROM `LogChannel` WHERE GuildID = ?", guildID);
                if (rs.next()) {
                    if (event.getOptions().isEmpty()) {
                        StringBuilder logs = new StringBuilder("Moderation logs are set in this channel for:\n");
                        for (Choice types : optionType.getChoices()) {
                            long channel = rs.getLong(types.getAsString());
                            logs.append("- ").append(types.getName()).append(" - ").append(channel == 0 ? "Not Set" : "<#" + channel + '>').append("\n");
                        }
                        event.reply(logs.toString()).queue();
                    }
                    else {
                        String type = event.getOptions().get(0).getAsString();
                        long current = rs.getLong(type);
                        boolean isCurrent = current == event.getChannel().getIdLong();
                        DBConnectionManagerLum.sendUpdate("UPDATE `LogChannel` SET `" + type + "`=?,`user`=? WHERE GuildID = ?", isCurrent ? null : channelID, event.getUser().getIdLong(), guildID);
                        event.reply("Moderation logs is now " + (isCurrent ? "disabled" : "enabled") + " in this channel for " + type).queue();
                    }
                }
                else {
                    if (event.getOptions().isEmpty()) {
                        event.reply("There is no log channels set for this server. Rerun this command this command with an option to set ").queue();
                        return;
                    }
                    else {
                        String type = event.getOptions().get(0).getAsString();
                        DBConnectionManagerLum.sendUpdate("INSERT INTO `LogChannel` (`GuildID`, `" + type + "`, `user`) VALUES (?, ?, ?)", guildID, channelID, event.getUser().getIdLong());
                        event.reply("Moderation logs is now enabled in this channel for " + type).queue();
                    }
                }
                DBConnectionManagerLum.closeRequest(rs);
            }
            catch (Exception e) {
                ExceptionUtils.reportException("Failed to set log channel", e, event.getChannel());
            }
    }
}
