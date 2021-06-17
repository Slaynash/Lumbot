package slaynash.lum.bot.discord.commands;

import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.components.Button;

public class Slash {
    public static void slashRun(SlashCommandEvent event){
        if (event.getName().equals("config")) {
            event.reply("Server Config")
                .addActionRow(
                    Button.danger("ss", "Scam Shield"),
                    Button.danger("dll", "DLL Remover"),
                    Button.danger("reaction", "Log Reactions"),
                    Button.danger("thanks", "Lum Thanks reply"),
                    Button.danger("partial", "Partial Log remover")).queue();
        }
    }

    public static void buttonUpdate(ButtonClickEvent event){
        //I am assuming here that if Lum sent buttons to user then they are server Owner
        if (event.getComponentId().equals("ss")) {
            ssB = !ssB;
            event.editButton(ssB ? Button.success("ss", "Scam Shield"):Button.danger("ss", "Scam Shield")).queue();
        } else if (event.getComponentId().equals("dll")) {
            dlB = !dlB;
            event.editButton(dlB ? Button.success("dll", "DLL Remover"):Button.danger("dll", "DLL Remover")).queue();
        } else if (event.getComponentId().equals("reaction")) {
            reB = !reB;
            event.editButton(reB ? Button.success("reaction", "Log Reactions"):Button.danger("reaction", "Log Reactions")).queue();
        } else if (event.getComponentId().equals("thanks")) {
            thB = !thB;
            event.editButton(thB ? Button.success("thanks", "Lum Thanks reply"):Button.danger("thanks", "Lum Thanks reply")).queue();
        } else if (event.getComponentId().equals("partial")) {
            paB = !paB;
            event.editButton(paB ? Button.success("partial", "Partial Log remover"):Button.danger("partial", "Partial Log remover")).queue();
        }
    }

}
