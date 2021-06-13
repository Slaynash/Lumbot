package slaynash.lum.bot.discord;

import java.util.EnumSet;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;

public class Moderation {
    public static void voiceJoin(GuildVoiceJoinEvent event){
        Long guildID = (event.getGuild().getIdLong());
        if(GuildConfigurations.noMicChannels.containsKey(guildID)){
            // event.getJDA().getGuildById(760342261967487066L).getTextChannelById(853694950037389343L).sendMessage("<@" + event.getMember().getIdLong() +"> joined a Voice channel in " + guildID).queue();
            event.getGuild().getGuildChannelById(GuildConfigurations.noMicChannels.get(guildID)).getManager().putPermissionOverride(event.getMember(),EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_READ, Permission.MESSAGE_WRITE), null).queue();
        }
    }

    public static void voiceLeave(GuildVoiceLeaveEvent event){
        Long guildID = (event.getGuild().getIdLong());
        if(GuildConfigurations.noMicChannels.containsKey(guildID)){
            // event.getJDA().getGuildById(760342261967487066L).getTextChannelById(853694950037389343L).sendMessage("<@" + event.getMember().getIdLong() +"> left a Voice channel in " + guildID).queue();
            event.getGuild().getGuildChannelById(GuildConfigurations.noMicChannels.get(guildID)).getManager().removePermissionOverride(event.getMember()).queue();
        }
    }
}
