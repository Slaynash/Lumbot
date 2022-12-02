package slaynash.lum.bot.discord;

import java.util.EnumSet;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.messages.MessageRequest;
import slaynash.lum.bot.Main;

public class JDAManager {

    private static JDA jda;
    public static final long mainGuildID = 633588473433030666L;
    private static boolean init = false;
    private static final Main mainEvents = new Main();

    public static void init(String token) throws InterruptedException {
        if (!init) init = true;
        else return;
        jda = JDABuilder.createDefault(token)
                .setChunkingFilter(ChunkingFilter.NONE)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_MESSAGE_TYPING, GatewayIntent.DIRECT_MESSAGE_TYPING)
                .build();
        jda.awaitReady();
        EnumSet<Message.MentionType> deny = EnumSet.of(Message.MentionType.EVERYONE, Message.MentionType.HERE, Message.MentionType.ROLE);
        MessageRequest.setDefaultMentions(EnumSet.complementOf(deny));
    }

    public static JDA getJDA() {
        return jda;
    }

    public static void enableEvents() {
        if (jda == null) {
            return;
        }
        System.out.println("Enabling events");
        jda.getEventManager().register(mainEvents);
    }
    public static void disableEvents() {
        if (jda == null) {
            return;
        }
        System.out.println("Disabling events");
        jda.getEventManager().unregister(mainEvents);
    }
    public static boolean isEventsEnabled() {
        if (jda == null) {
            return false;
        }
        return jda.getEventManager().getRegisteredListeners().contains(mainEvents);
    }
}
