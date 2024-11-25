package slaynash.lum.bot.discord;

import java.util.EnumSet;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.messages.MessageRequest;
import slaynash.lum.bot.ConfigManager;
import slaynash.lum.bot.Main;

public class JDAManager {

    private static JDA jda;
    public static final long mainGuildID = 633588473433030666L;
    private static boolean init = false;
    private static final Main mainEvents = new Main();

    public static void init(String token) throws InterruptedException {
        if (!init) init = true;
        else return;
        while (true) {
            try {
                jda = JDABuilder.createDefault(token)
                    .setChunkingFilter(ChunkingFilter.NONE)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_MESSAGE_TYPING, GatewayIntent.DIRECT_MESSAGE_TYPING, GatewayIntent.GUILD_EXPRESSIONS)
                    .setMaxReconnectDelay(60)
                    // .setEventPassthrough(true)
                    .build();
                break;
            }
            catch (Exception e) {
                System.out.println("Error while building JDA: " + e.getMessage());
                Thread.sleep(30 * 1000); //Wait 30 seconds before retrying
            }
        }
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

    public static boolean isProductionBot() {
        if (jda == null)
            return false;
        return jda.getSelfUser().getIdLong() == 275759980752273418L;
    }

    public static boolean isDevBot() {
        if (jda == null)
            return false;
        return jda.getSelfUser().getIdLong() == 773707709064151051L;
    }

    public static boolean isMainBot() {
        return isProductionBot() && ConfigManager.mainBot;
    }

    public static boolean isBackupBot() {
        // Note: The dev bot is always assumed to be non-backup
        return isProductionBot() && !ConfigManager.mainBot;
    }
}
