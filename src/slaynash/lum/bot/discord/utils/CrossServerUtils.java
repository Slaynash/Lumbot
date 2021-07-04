package slaynash.lum.bot.discord.utils;

import net.dv8tion.jda.api.entities.Member;
import slaynash.lum.bot.discord.JDAManager;

public final class CrossServerUtils {

    public static Member resolveMember(String guildId, String userId) {
        return JDAManager.getJDA().getGuildById(guildId).getMemberById(userId);
    }

}
