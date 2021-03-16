package slaynash.lum.bot.discord;

import net.dv8tion.jda.api.entities.Member;

public final class CrossServerUtils {
    
    public static Member resolveMember(String guildId, String userId) {
        return JDAManager.getJDA().getGuildById(guildId).getMemberById(userId);
    }

}
