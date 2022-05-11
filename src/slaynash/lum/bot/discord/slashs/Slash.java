package slaynash.lum.bot.discord.slashs;

import java.util.List;
import java.util.Map;

import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
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

    protected void slashRun(SlashCommandEvent event) {
    }
    protected void buttonClick(ButtonClickEvent event) {
    }
}
