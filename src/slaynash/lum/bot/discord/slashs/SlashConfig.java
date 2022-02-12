package slaynash.lum.bot.discord.slashs;

import java.time.Duration;
import java.util.Arrays;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.discord.GuildConfigurations;
import slaynash.lum.bot.discord.GuildConfigurations.ConfigurationMap;
import slaynash.lum.bot.discord.Moderation;
import slaynash.lum.bot.utils.ExceptionUtils;

public class SlashConfig {

    public void sendReply(SlashCommandEvent event, String guildID) {
        Guild guild = event.getJDA().getGuildById(guildID);
        if (guild == null) {
            event.reply("Guild was not found.").queue();
            return;
        }
        try {
            boolean[] config = GuildConfigurations.configurations.get(Long.valueOf(guildID));
            if (config == null) {
                config = new boolean[GuildConfigurations.ConfigurationMap.values().length];
                config[GuildConfigurations.ConfigurationMap.LOGSCAN.ordinal()] = true;
                GuildConfigurations.configurations.put(Long.valueOf(guildID), config);
                CommandManager.saveGuildConfigs();
            }
            if (Moderation.getAdmins(guild).contains(event.getUser().getIdLong())) {
                System.out.println("Sent config for " + guild.getName());
                System.out.println(Arrays.toString(config));
                event.reply("Server Config for " + guild.getName() + ": " + guildID)
                    .addActionRow(// Buttons can be in a 5x5
                        config[ConfigurationMap.SCAMSHIELD.ordinal()] ? Button.success("ss", "Scam Shield") : Button.danger("ss", "Scam Shield"),
                        config[ConfigurationMap.SSBAN.ordinal()] ? Button.danger("ssban", "Scam Shield Ban") : Button.success("ssban", "Scam Shield Kick"),
                        config[ConfigurationMap.SSCROSS.ordinal()] ? Button.success("sscross", "Scam Shield Cross " + (config[ConfigurationMap.SSBAN.ordinal()] ? "Ban" : "Kick")) : Button.danger("sscross", "Scam Shield Cross " + (config[ConfigurationMap.SSBAN.ordinal()] ? "Ban" : "Kick")))
                    .addActionRow(
                        config[ConfigurationMap.DLLREMOVER.ordinal()] ? Button.success("dll", "DLL Remover") : Button.danger("dll", "DLL Remover"),
                        config[ConfigurationMap.PARTIALLOGREMOVER.ordinal()] ? Button.success("partial", "Partial Log remover") : Button.danger("partial", "Partial Log remover"),
                        config[ConfigurationMap.GENERALLOGREMOVER.ordinal()] ? Button.success("general", "General Log remover") : Button.danger("general", "General Log remover"))
                    .addActionRow(
                        config[ConfigurationMap.LOGSCAN.ordinal()] ? Button.success("log", "MelonLoader Log scanner") : Button.danger("log", "MelonLoader Log scanner"),
                        config[ConfigurationMap.MLREPLIES.ordinal()] ? Button.success("mlr", "MelonLoader AutoReplies") : Button.danger("mlr", "MelonLoader AutoReplies"),
                        config[ConfigurationMap.LOGREACTION.ordinal()] ? Button.success("reaction", "Log Reactions") : Button.danger("reaction", "Log Reactions"))
                    .addActionRow(
                        config[ConfigurationMap.LUMREPLIES.ordinal()] ? Button.success("thanks", "Chatty Lum") : Button.danger("thanks", "Chatty Lum"),
                        config[ConfigurationMap.DADJOKES.ordinal()] ? Button.success("dad", "Dad Jokes") : Button.danger("dad", "Dad Jokes"))
                    .addActionRow(
                        Button.danger("delete", "Delete this message")).queue();
            }
            else event.reply("You do not have permission to use this command.").queue();
        }
        catch (Exception e) {
            ExceptionUtils.reportException("An error has occurred while sending Slash Reply:", e);
        }
    }

    public void buttonClick(ButtonClickEvent event) {
        try {
            String[] message = event.getMessage().getContentRaw().split(": ");
            if (message.length < 2) {
                event.deferEdit().queue();
                return;
            }
            Long guildID = Long.valueOf(message[message.length - 1]);
            Guild guild = event.getJDA().getGuildById(guildID);
            boolean[] config = GuildConfigurations.configurations.get(guildID);
            if (Moderation.getAdmins(guild).contains(event.getUser().getIdLong())) {
                switch (event.getComponentId()) {
                    case "ss" :
                        config[ConfigurationMap.SCAMSHIELD.ordinal()] = !config[ConfigurationMap.SCAMSHIELD.ordinal()];
                        event.editButton(config[ConfigurationMap.SCAMSHIELD.ordinal()] ? Button.success("ss", "Scam Shield") : Button.danger("ss", "Scam Shield")).queue();
                        checkBanPerm(event, guild, config[ConfigurationMap.SSBAN.ordinal()]);
                        break;
                    case "dll" :
                        config[ConfigurationMap.DLLREMOVER.ordinal()] = !config[ConfigurationMap.DLLREMOVER.ordinal()];
                        event.editButton(config[ConfigurationMap.DLLREMOVER.ordinal()] ? Button.success("dll", "DLL Remover") : Button.danger("dll", "DLL Remover")).queue();
                        checkDllRemovePerm(event, guild);
                        break;
                    case "reaction" :
                        config[ConfigurationMap.LOGREACTION.ordinal()] = !config[ConfigurationMap.LOGREACTION.ordinal()];
                        event.editButton(config[ConfigurationMap.LOGREACTION.ordinal()] ? Button.success("reaction", "Log Reactions") : Button.danger("reaction", "Log Reactions")).queue();
                        break;
                    case "thanks" :
                        config[ConfigurationMap.LUMREPLIES.ordinal()] = !config[ConfigurationMap.LUMREPLIES.ordinal()];
                        event.editButton(config[ConfigurationMap.LUMREPLIES.ordinal()] ? Button.success("thanks", "Chatty Lum") : Button.danger("thanks", "Chatty Lum")).queue();
                        break;
                    case "dad" :
                        config[ConfigurationMap.DADJOKES.ordinal()] = !config[ConfigurationMap.DADJOKES.ordinal()];
                        event.editButton(config[ConfigurationMap.DADJOKES.ordinal()] ? Button.success("dad", "Dad Jokes") : Button.danger("dad", "Dad Jokes")).queue();
                        break;
                    case "partial" :
                        config[ConfigurationMap.PARTIALLOGREMOVER.ordinal()] = !config[ConfigurationMap.PARTIALLOGREMOVER.ordinal()];
                        event.editButton(config[ConfigurationMap.PARTIALLOGREMOVER.ordinal()] ? Button.success("partial", "Partial Log remover") : Button.danger("partial", "Partial Log remover")).queue();
                        break;
                    case "general" :
                        config[ConfigurationMap.GENERALLOGREMOVER.ordinal()] = !config[ConfigurationMap.GENERALLOGREMOVER.ordinal()];
                        event.editButton(config[ConfigurationMap.GENERALLOGREMOVER.ordinal()] ? Button.success("general", "General Log remover") : Button.danger("general", "General Log remover")).queue();
                        break;
                    case "log" :
                        config[ConfigurationMap.LOGSCAN.ordinal()] = !config[ConfigurationMap.LOGSCAN.ordinal()];
                        event.editButton(config[ConfigurationMap.LOGSCAN.ordinal()] ? Button.success("log", "MelonLoader Log scanner") : Button.danger("log", "MelonLoader Log scanner")).queue();
                        break;
                    case "mlr" :
                        config[ConfigurationMap.MLREPLIES.ordinal()] = !config[ConfigurationMap.MLREPLIES.ordinal()];
                        event.editButton(config[ConfigurationMap.MLREPLIES.ordinal()] ? Button.success("mlr", "MelonLoader AutoReplies") : Button.danger("mlr", "MelonLoader AutoReplies")).queue();
                        break;
                    case "ssban" :
                        config[ConfigurationMap.SSBAN.ordinal()] = !config[ConfigurationMap.SSBAN.ordinal()];
                        event.editButton(config[ConfigurationMap.SSBAN.ordinal()] ? Button.danger("ssban", "Scam Shield Ban") : Button.success("ssban", "Scam Shield Kick")).queue();
                        checkBanPerm(event, guild, config[ConfigurationMap.SSBAN.ordinal()]);
                        break;
                    case "sscross" :
                        config[ConfigurationMap.SSCROSS.ordinal()] = !config[ConfigurationMap.SSCROSS.ordinal()];
                        event.editButton(config[ConfigurationMap.SSCROSS.ordinal()] ? Button.success("sscross", "Scam Shield Cross " + (config[ConfigurationMap.SSBAN.ordinal()] ? "Ban" : "Kick")) : Button.danger("sscross", "Scam Shield Cross " + (config[ConfigurationMap.SSBAN.ordinal()] ? "Ban" : "Kick"))).queue();
                        break;
                    case "delete" :
                        event.getMessage().delete().queue();
                        break;
                    default :
                }
                GuildConfigurations.configurations.put(guildID, config); // update Values
                CommandManager.saveGuildConfigs(); // backup values
            }
            else {
                event.deferEdit().queue();
            }
        }
        catch (Exception e) {
            ExceptionUtils.reportException("An error has occurred while updating buttons:", event.getChannel().getName(), e);
        }
    }

    private void checkBanPerm(ButtonClickEvent event, Guild guild, boolean ban) {
        String message = "";
        if (ban) {
            if (!guild.getSelfMember().hasPermission(Permission.BAN_MEMBERS)) {
                if (guild.getSelfMember().hasPermission(Permission.KICK_MEMBERS))
                    message = "I don't have ban permission but I will kick scammers instead.";
                else
                    message = "I don't have ban permission so I can't remove scammers should they appear.";
            }
        }
        else {
            if (!guild.getSelfMember().hasPermission(Permission.KICK_MEMBERS))
                message = "I don't have kick permission so I can't remove scammers should they appear.";
            if (!guild.getSelfMember().hasPermission(Permission.MESSAGE_MANAGE) && !guild.getSelfMember().hasPermission(Permission.BAN_MEMBERS))
                message = "I can't clean up messages after I removed the scammer. I need either Manage Messages or Ban permission.";
        }
        if (message.isEmpty())
            return;
        final String finalMessage = message;
        if (event.getChannelType() == ChannelType.PRIVATE || !event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_WRITE)) {
            event.getUser().openPrivateChannel().flatMap(channel -> channel.sendMessage(finalMessage)).delay(Duration.ofSeconds(60)).flatMap(Message::delete).queue();
        }
        else {
            event.getTextChannel().sendMessage(finalMessage).delay(Duration.ofSeconds(30)).flatMap(Message::delete).queue();
        }
    }
    private void checkDllRemovePerm(ButtonClickEvent event, Guild guild) {
        if (!guild.getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) {
            final String message = "I don't have manage message permission so I can't remove dll and zip files.";
            if (event.getChannelType() == ChannelType.PRIVATE || !event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_WRITE)) {
                event.getUser().openPrivateChannel().flatMap(channel -> channel.sendMessage(message)).delay(Duration.ofSeconds(60)).flatMap(Message::delete).queue();
            }
            else {
                event.getTextChannel().sendMessage(message).delay(Duration.ofSeconds(30)).flatMap(Message::delete).queue();
            }
        }
    }
}
