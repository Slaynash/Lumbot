package slaynash.lum.bot.discord.slashs;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class Slash {
    public static void slashRun(SlashCommandEvent event) {
        if (event.getChannelType() != ChannelType.PRIVATE && !event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.VIEW_CHANNEL))
            return;
        if (event.getName().equals("config")) { // Guild command
            String guildID = event.getGuild().getId();
            new SlashConfig().sendReply(event, guildID);
        }
        else if (event.getName().equals("configs")) { //Global/DM command
            String guildID = event.getOptionsByName("guild").get(0).getAsString();
            new SlashConfig().sendReply(event, guildID);
        }
        else if (event.getName().equals("exo")) {
            new UnivUCBLLIFExoGenerator().onCommand(event);
        }
    }

    public static void buttonClick(ButtonClickEvent event) {
        if (event.getChannelType() != ChannelType.PRIVATE && !event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.VIEW_CHANNEL))
            return;
        String message = event.getMessage().getContentRaw();
        if (message.startsWith("Server Config"))
            new SlashConfig().buttonClick(event);
        else
            event.reply("Unknown button click").queue();
    }
}
