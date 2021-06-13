package slaynash.lum.bot.discord;

import java.util.EnumSet;
import java.util.HashMap;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Moderation extends ListenerAdapter {

    private static final HashMap<Long, Long> noMicChannels = new HashMap<>(){{
        put(439093693769711616L /* VRCMG */     ,673955689298788376L /* no-mic */);
        put(600298024425619456L /* emmVRC */    ,682379676106096678L /* no-mic */);
    }};

    public static final HashMap<Long, Long> lockDownRoles = new HashMap<>(){{
        put(439093693769711616L /* VRCMG */     ,548534015108448256L /* Member */);
        put(600298024425619456L /* emmVRC */    ,600304115972702209L /* Members */);
        put(600298024425619456L /* MelonLoader */,663462327022256150L /* Member */);
    }};

    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent event){
        Long noMicChannel = noMicChannels.get(event.getGuild().getIdLong());
        if(noMicChannel != null)
            event.getGuild().getGuildChannelById(noMicChannel).getManager().putPermissionOverride(event.getMember(),EnumSet.of(Permission.VIEW_CHANNEL),null).queue();
    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event){
        Long noMicChannel = noMicChannels.get(event.getGuild().getIdLong());
        if(noMicChannel != null)
            event.getGuild().getGuildChannelById(noMicChannel).getManager().removePermissionOverride(event.getMember()).queue();
    }
}
