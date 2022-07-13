package slaynash.lum.bot.discord.slashs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import slaynash.lum.bot.discord.JDAManager;
import slaynash.lum.bot.utils.ExceptionUtils;

public class SlashManager {
    private static final List<Slash> slashes = new ArrayList<>();

    public static void slashRun(SlashCommandEvent event) {
        System.out.println("Slash " + event.getName() + (event.getSubcommandName() == null ? "" : " " + event.getSubcommandName()) + " options:" + event.getOptions() + " in " + (event.getGuild() == null ? "DM " + event.getUser().getId() : event.getGuild().getName()));
        if (event.getChannelType() == ChannelType.TEXT && !event.getTextChannel().canTalk())
            return;  //Lum can't talk in this channel
        for (Slash slash : slashes) {
            CommandData global = slash.globalSlashData();
            if (global != null && event.getName().equals(global.getName())) {
                slash.slashRun(event);
                return;
            }
            if (slash.guildSlashData() != null) {
                CommandData guild = slash.guildSlashData().get(event.getGuild().getIdLong());
                if (guild != null && event.getName().equals(guild.getName())) {
                    slash.slashRun(event);
                    return;
                }
            }
        }
        event.reply("Unknown command").queue();
    }

    public static void buttonClicked(ButtonClickEvent event) {
        if (event.getChannelType() != ChannelType.PRIVATE && !event.getTextChannel().canTalk())
            return; //Lum can't talk in this channel

        for (Slash slash : slashes) {
            if (slash.buttonList() != null && slash.buttonList().contains(event.getComponentId())) {
                slash.buttonClick(event);
                return;
            }
        }

        event.reply("Unknown button click").queue();
    }

    protected static void registerSlash(Slash command) {
        synchronized (slashes) {
            slashes.add(command);
        }
    }

    public static void registerCommands() {
        JDA jda = JDAManager.getJDA();

        registerSlash(new SlashConfig());
        registerSlash(new SteamWatcher());
        registerSlash(new Replies());
        registerSlash(new UnivUCBLLIFExoGenerator());

        try {
            List<CommandData> globalSlashes = new ArrayList<>();
            Map<Long, List<CommandData>> guildSlashes = new HashMap<>();

            for (Slash slash : slashes) {
                CommandData globalSlash = slash.globalSlashData();
                if (globalSlash != null)
                    globalSlashes.add(globalSlash);
                Map<Long, CommandData> guildSlash = slash.guildSlashData();
                if (guildSlash != null) {
                    for (Entry<Long, CommandData> guild : guildSlash.entrySet()) {
                        List<CommandData> guildCommands = guildSlashes.get(guild.getKey());
                        if (guildCommands == null)
                            guildCommands = new ArrayList<>();
                        guildCommands.add(guild.getValue());
                        guildSlashes.put(guild.getKey(), guildCommands);
                    }
                }
            }

            //Removes any old commands and registers the new ones
            jda.updateCommands().addCommands(globalSlashes).queue();
            for (Guild guild : jda.getGuilds()) {
                List<CommandData> gslash = guildSlashes.get(guild.getIdLong());
                if (gslash == null)
                    guild.updateCommands().addCommands().queue(null, e -> { });
                else
                    guild.updateCommands().addCommands(gslash).queue(null, e -> { });
            }
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Error registering command", e);
        }
    }
}
