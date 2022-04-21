package slaynash.lum.bot.discord;

import java.util.EnumSet;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.AllowedMentions;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import slaynash.lum.bot.Main;

public class JDAManager {

    private static JDA jda;
    private static boolean init = false;

    public static void init(String token) throws LoginException, IllegalArgumentException, InterruptedException {
        if (!init) init = true;
        else return;
        jda = JDABuilder.createDefault(token)
                .addEventListeners(new Main())
                .setChunkingFilter(ChunkingFilter.NONE)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGE_REACTIONS)
                .build();
        jda.awaitReady();
        EnumSet<Message.MentionType> deny = EnumSet.of(Message.MentionType.EVERYONE, Message.MentionType.HERE, Message.MentionType.ROLE);
        AllowedMentions.setDefaultMentions(EnumSet.complementOf(deny));
    }

    public static JDA getJDA() {
        return jda;
    }

}
