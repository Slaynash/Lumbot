package slaynash.lum.bot.discord.slashs;

import java.util.List;
import java.util.Map;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public abstract class Slash {
    protected Slash instance = this;

    protected CommandData globalSlashData() {
        return null;
    }
    protected Map<Long, CommandData> guildSlashData() {
        return null;
    }
    protected List<String> buttonList() {
        return null;
    }

    protected void slashRun(SlashCommandInteractionEvent event) {
    }
    protected void buttonClick(ButtonInteractionEvent event) {
    }
}
