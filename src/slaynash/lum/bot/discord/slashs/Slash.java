package slaynash.lum.bot.discord.slashs;

import java.util.List;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

public class Slash {
    public static void slashRun(SlashCommandEvent event) {
        if (event.getChannelType() != ChannelType.PRIVATE && !event.getTextChannel().canTalk())
            return;  //Lum can't talk in this channel
        if (event.getName().equals("config")) { // Guild settings command
            String guildID;
            List<OptionMapping> guildOption = event.getOptionsByName("guild");
            if (event.getChannelType() == ChannelType.PRIVATE) {
                if (guildOption.size() == 0) {
                    event.reply("You must specify a guild ID in DMs").queue();
                    return;
                }
                else
                    guildID = guildOption.get(0).getAsString();
            }
            else {
                if (guildOption.size() == 0)
                    guildID = event.getGuild().getId();
                else
                    guildID = guildOption.get(0).getAsString();
            }
            new SlashConfig().sendReply(event, guildID);
        }
        else if (event.getName().equals("exo")) {
            new UnivUCBLLIFExoGenerator().onCommand(event);
        }
        else
            event.reply("Unknown command").queue();
    }

    public static void buttonClick(ButtonClickEvent event) {
        if (event.getChannelType() != ChannelType.PRIVATE && !event.getTextChannel().canTalk())
            return; //Lum can't talk in this channel
        String message = event.getMessage().getContentRaw();
        if (message.startsWith("Server Config"))
            new SlashConfig().buttonClick(event);
        else
            event.reply("Unknown button click").queue();
    }
}
