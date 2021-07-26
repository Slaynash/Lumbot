package slaynash.lum.bot.discord.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.discord.GuildConfigurations;
import slaynash.lum.bot.discord.Moderation;
import slaynash.lum.bot.discord.GuildConfigurations.ConfigurationMap;
import slaynash.lum.bot.utils.ExceptionUtils;

public class Slash {
    public static void slashRun(SlashCommandEvent event) {
        if (event.getName().equals("config")) { // Guild command
            String guildID = event.getGuild().getId();
            sendReply(event, guildID);
        }
        else if (event.getName().equals("configs")) { //Global/DM command
            String guildID = event.getOptionsByName("guild").get(0).getAsString();
            sendReply(event, guildID);
        }
    }

    public static void buttonUpdate(ButtonClickEvent event) {
        if (event.getGuild().getSelfMember().hasPermission(Permission.VIEW_CHANNEL))
            return;
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
                    case ("ss") :
                        config[ConfigurationMap.SCAMSHIELD.ordinal()] = !config[ConfigurationMap.SCAMSHIELD.ordinal()];
                        event.editButton(config[ConfigurationMap.SCAMSHIELD.ordinal()] ? Button.success("ss", "Scam Shield") : Button.danger("ss", "Scam Shield")).queue();
                        break;
                    case ("dll") :
                        config[ConfigurationMap.DLLREMOVER.ordinal()] = !config[ConfigurationMap.DLLREMOVER.ordinal()];
                        event.editButton(config[ConfigurationMap.DLLREMOVER.ordinal()] ? Button.success("dll", "DLL Remover") : Button.danger("dll", "DLL Remover")).queue();
                        break;
                    case ("reaction") :
                        config[ConfigurationMap.LOGREACTION.ordinal()] = !config[ConfigurationMap.LOGREACTION.ordinal()];
                        event.editButton(config[ConfigurationMap.LOGREACTION.ordinal()] ? Button.success("reaction", "Log Reactions") : Button.danger("reaction", "Log Reactions")).queue();
                        break;
                    case ("thanks") :
                        config[ConfigurationMap.LUMREPLIES.ordinal()] = !config[ConfigurationMap.LUMREPLIES.ordinal()];
                        event.editButton(config[ConfigurationMap.LUMREPLIES.ordinal()] ? Button.success("thanks", "Chatty Lum") : Button.danger("thanks", "Chatty Lum")).queue();
                        break;
                    case ("dad") :
                        config[ConfigurationMap.DADJOKES.ordinal()] = !config[ConfigurationMap.DADJOKES.ordinal()];
                        event.editButton(config[ConfigurationMap.DADJOKES.ordinal()] ? Button.success("dad", "Dad Jokes") : Button.danger("dad", "Dad Jokes")).queue();
                        break;
                    case ("partial") :
                        config[ConfigurationMap.PARTIALLOGREMOVER.ordinal()] = !config[ConfigurationMap.PARTIALLOGREMOVER.ordinal()];
                        event.editButton(config[ConfigurationMap.PARTIALLOGREMOVER.ordinal()] ? Button.success("partial", "Partial Log remover") : Button.danger("partial", "Partial Log remover")).queue();
                        break;
                    case ("general") :
                        config[ConfigurationMap.GENERALLOGREMOVER.ordinal()] = !config[ConfigurationMap.GENERALLOGREMOVER.ordinal()];
                        event.editButton(config[ConfigurationMap.GENERALLOGREMOVER.ordinal()] ? Button.success("general", "General Log remover") : Button.danger("general", "General Log remover")).queue();
                        break;
                    case ("log") :
                        config[ConfigurationMap.LOGSCAN.ordinal()] = !config[ConfigurationMap.LOGSCAN.ordinal()];
                        event.editButton(config[ConfigurationMap.LOGSCAN.ordinal()] ? Button.success("log", "MelonLoader Log scanner") : Button.danger("log", "MelonLoader Log scanner")).queue();
                        break;
                    case ("mlr") :
                        config[ConfigurationMap.MLREPLIES.ordinal()] = !config[ConfigurationMap.MLREPLIES.ordinal()];
                        event.editButton(config[ConfigurationMap.MLREPLIES.ordinal()] ? Button.success("mlr", "MelonLoader AutoReplies") : Button.danger("mlr", "MelonLoader AutoReplies")).queue();
                        break;
                    case ("ssban") :
                        config[ConfigurationMap.SSBAN.ordinal()] = !config[ConfigurationMap.SSBAN.ordinal()];
                        event.editButton(config[ConfigurationMap.SSBAN.ordinal()] ? Button.danger("ssban", "Scam Shield Ban") : Button.success("ssban", "Scam Shield Kick")).queue();
                        break;
                    case ("delete") :
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

    private static void sendReply(SlashCommandEvent event, String guildID) {
        if (event.getGuild().getSelfMember().hasPermission(Permission.VIEW_CHANNEL))
            return;
        try {
            Guild guild = event.getJDA().getGuildById(guildID);
            boolean[] config = GuildConfigurations.configurations.get(Long.valueOf(guildID));
            if (config == null) {
                config = new boolean[GuildConfigurations.ConfigurationMap.values().length];
                config[GuildConfigurations.ConfigurationMap.LOGSCAN.ordinal()] = true;
                GuildConfigurations.configurations.put(Long.valueOf(guildID), config);
                CommandManager.saveGuildConfigs();
            }
            if (guild != null) {
                if (Moderation.getAdmins(guild).contains(event.getUser().getIdLong())) {
                    System.out.println("sent config for " + guild.getName());
                    event.reply("Server Config for " + guild.getName() + ": " + guildID)
                        .addActionRow(// Buttons can be in a 5x5
                            config[ConfigurationMap.SCAMSHIELD.ordinal()] ? Button.success("ss", "Scam Shield") : Button.danger("ss", "Scam Shield"),
                            config[ConfigurationMap.SSBAN.ordinal()] ? Button.danger("ssban", "Scam Shield Ban") : Button.success("ssban", "Scam Shield Kick"),
                            config[ConfigurationMap.LUMREPLIES.ordinal()] ? Button.success("thanks", "Chatty Lum") : Button.danger("thanks", "Chatty Lum"),
                            config[ConfigurationMap.DADJOKES.ordinal()] ? Button.success("dad", "Dad Jokes") : Button.danger("dad", "Dad Jokes"))
                        .addActionRow(
                            config[ConfigurationMap.DLLREMOVER.ordinal()] ? Button.success("dll", "DLL Remover") : Button.danger("dll", "DLL Remover"),
                            config[ConfigurationMap.PARTIALLOGREMOVER.ordinal()] ? Button.success("partial", "Partial Log remover") : Button.danger("partial", "Partial Log remover"),
                            config[ConfigurationMap.GENERALLOGREMOVER.ordinal()] ? Button.success("general", "General Log remover") : Button.danger("general", "General Log remover"))
                        .addActionRow(
                            config[ConfigurationMap.LOGSCAN.ordinal()] ? Button.success("log", "MelonLoader Log scanner") : Button.danger("log", "MelonLoader Log scanner"),
                            config[ConfigurationMap.MLREPLIES.ordinal()] ? Button.success("mlr", "MelonLoader AutoReplies") : Button.danger("mlr", "MelonLoader AutoReplies"),
                            config[ConfigurationMap.LOGREACTION.ordinal()] ? Button.success("reaction", "Log Reactions") : Button.danger("reaction", "Log Reactions"))
                        .addActionRow(
                            Button.danger("delete", "Delete this message")).queue();
                }
                else event.reply("You do not have permission to use this command.");
            }
            else event.reply("Guild not found.");
        }
        catch (Exception e) {
            ExceptionUtils.reportException("An error has occurred while sending Slash Reply:", e);
        }
    }
}
