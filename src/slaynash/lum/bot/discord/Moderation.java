package slaynash.lum.bot.discord;

import java.util.EnumSet;
import java.util.HashMap;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;

public class Moderation {

    private static final HashMap<Long, Long> noMicChannels = new HashMap<>(){{
        put(439093693769711616L /* VRCMG */     ,673955689298788376L /* no-mic */);
        put(600298024425619456L /* emmVRC */    ,682379676106096678L /* no-mic */);
        put(663449315876012052L /* MelonLoader */,701494833495408640L /* voice-chat */);
    }};

    public static final HashMap<Long, Long> lockDownRoles = new HashMap<>(){{
        put(439093693769711616L /* VRCMG */     ,548534015108448256L /* Member */);
        put(600298024425619456L /* emmVRC */    ,600304115972702209L /* Members */);
        put(663449315876012052L /* MelonLoader */,663462327022256150L /* Member */);
    }};

    public static void voiceJoin(GuildVoiceJoinEvent event){
        Long guildID = (event.getGuild().getIdLong());
        if(noMicChannels.containsKey(guildID)){
            event.getJDA().getGuildById(760342261967487066L).getTextChannelById(853694950037389343L).sendMessage("<@" + event.getMember().getIdLong() +"> joined a Voice channel in " + guildID).queue();
            event.getGuild().getGuildChannelById(noMicChannels.get(guildID)).getManager().putPermissionOverride(event.getMember(),EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_READ, Permission.MESSAGE_WRITE), null).queue();
        }
    }

    public static void voiceLeave(GuildVoiceLeaveEvent event){
        Long guildID = (event.getGuild().getIdLong());
        if(noMicChannels.containsKey(guildID)){
            event.getJDA().getGuildById(760342261967487066L).getTextChannelById(853694950037389343L).sendMessage("<@" + event.getMember().getIdLong() +"> left a Voice channel in " + guildID).queue();
            event.getGuild().getGuildChannelById(noMicChannels.get(guildID)).getManager().removePermissionOverride(event.getMember()).queue();
        }
    }
}
