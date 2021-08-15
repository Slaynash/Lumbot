package slaynash.lum.bot.discord.commands;

import java.awt.Color;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.discord.JDAManager;

public class MLSetMinForVRC extends Command {

    @Override
    protected void onServer(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {
        if (!includeInHelp(paramMessageReceivedEvent))
            return;

        String[] parts = paramString.split(" ", 3);

        if (parts.length != 3) {
            paramMessageReceivedEvent.getChannel().sendMessageEmbeds(JDAManager.wrapMessageInEmbed("Usage: l!setvrcmlversion <ml hash> <ml release time>\nExample: l!setvrcmlversion 25881 feb. 6, 2021 at 10.01pm CET", Color.RED)).queue();
            return;
        }

        String hash = parts[1];
        System.out.println("hash: " + paramString);

        if (!hash.matches("^[0-9]{5,}$"))
            paramMessageReceivedEvent.getChannel().sendMessageEmbeds(JDAManager.wrapMessageInEmbed("Usage: l!setvrcmlversion <ml hash> <ml release time>\nExample: l!setvrcmlversion 25881 feb. 6, 2021 at 10.01pm CET", Color.RED)).queue();

        CommandManager.melonLoaderVRCHash = hash;
        CommandManager.melonLoaderVRCMinDate = parts[2];
        CommandManager.saveMLVRCHash();
        paramMessageReceivedEvent.getChannel().sendMessage("Successfully updated minimal MelonLoader version for VRChat").queue();
    }

    @Override
    protected boolean matchPattern(String paramString) {
        return paramString.split(" ", 2)[0].equals("l!setvrcmlversion");
    }

    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        return event.getGuild().getIdLong() == 673663870136746046L || (// Modders & Chill
            event.getGuild().getIdLong() == 439093693769711616L && // VRChat Modding Group
            event.getChannel().getIdLong() == 729855750561595405L); // #staff-bot-commands
    }

    @Override
    public String getHelpDescription() {
        return "Set the minimal required version of MelonLoader for VRChat";
    }

    @Override
    public String getHelpName() {
        return "l!setvrcmlversion";
    }
}
