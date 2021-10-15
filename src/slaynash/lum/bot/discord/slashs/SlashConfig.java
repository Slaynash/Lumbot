package slaynash.lum.bot.discord.slashs;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.Button;
import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.discord.GuildConfigurations;
import slaynash.lum.bot.discord.GuildConfigurations.ConfigurationMap;
import slaynash.lum.bot.discord.Moderation;
import slaynash.lum.bot.utils.ExceptionUtils;

public class SlashConfig {

    InteractionHook interactionhook;

    public void sendReply(SlashCommandEvent event, String guildID) {
        event.deferReply().queue(success -> interactionhook = success);

        if (!guildID.matches("^\\d{18}$")) {
            interactionhook.sendMessage("Invalid Guild ID. Please make sure that you are using the 18 digit ID.").setEphemeral(true).queue();
            return;
        }

        Guild guild = event.getJDA().getGuildById(guildID);
        if (guild == null) {
            interactionhook.sendMessage("Guild not found.").setEphemeral(true).queue();
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
                interactionhook.sendMessage("Server Config for " + guild.getName() + ": " + guildID)
                    .addActionRow(// Buttons can be in a 5x5
                        config[ConfigurationMap.SCAMSHIELD.ordinal()] ? Button.success("ss", "Scam Shield") : Button.danger("ss", "Scam Shield"),
                        config[ConfigurationMap.SSBAN.ordinal()] ? Button.danger("ssban", "Scam Shield Ban") : Button.success("ssban", "Scam Shield Kick"),
                        config[ConfigurationMap.SSCROSS.ordinal()] ? Button.success("sscross", "Scam Shield Cross " + (config[ConfigurationMap.SSBAN.ordinal()] ? "Ban" : "Kick")) : Button.danger("ssban", "Scam Shield Cross " + (config[ConfigurationMap.SSBAN.ordinal()] ? "Ban" : "Kick")))
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
            else interactionhook.sendMessage("You do not have permission to use this command.").queue();
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
                    case ("sscross") :
                        config[ConfigurationMap.SSCROSS.ordinal()] = !config[ConfigurationMap.SSCROSS.ordinal()];
                        event.editButton(config[ConfigurationMap.SSCROSS.ordinal()] ? Button.success("sscross", "Scam Shield Cross " + (config[ConfigurationMap.SSBAN.ordinal()] ? "Ban" : "Kick")) : Button.danger("ssban", "Scam Shield Cross " + (config[ConfigurationMap.SSBAN.ordinal()] ? "Ban" : "Kick"))).queue();
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
}
