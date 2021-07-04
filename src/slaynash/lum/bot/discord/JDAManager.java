package slaynash.lum.bot.discord;

import java.awt.Color;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import slaynash.lum.bot.Main;

public class JDAManager {

    private static JDA jda;
    private static boolean init = false;

    public static void init(String token) throws LoginException, IllegalArgumentException, InterruptedException, RateLimitedException {
        if (!init) init = true;
        else return;
        jda = JDABuilder.createDefault(token)
                .addEventListeners(new Main())
                .setChunkingFilter(ChunkingFilter.ALL)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .build();
        jda.awaitReady();
    }

    public static MessageEmbed wrapMessageInEmbed(String message, Color color) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(color);
        if (message.length() > MessageEmbed.TEXT_MAX_LENGTH)
            eb.setDescription(message.substring(0, MessageEmbed.TEXT_MAX_LENGTH - 4) + " ...");
        else
            eb.setDescription(message);
        return eb.build();
    }

    public static JDA getJDA() {
        return jda;
    }

}
