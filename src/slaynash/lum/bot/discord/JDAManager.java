package slaynash.lum.bot.discord;

import java.awt.Color;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.exceptions.RateLimitedException;

public class JDAManager {

	private static JDA jda;
	private static boolean init = false;

	protected static void init(String token) throws LoginException, IllegalArgumentException, InterruptedException, RateLimitedException {
		if(!init) init = true; else return;
		jda = JDABuilder.createDefault(token)
				.addEventListeners(new Main())
				.build();
		jda.awaitReady();
	}
	
	public static MessageEmbed wrapMessageInEmbed(String message, Color color) {
		EmbedBuilder eb = new EmbedBuilder();
		eb.setColor(color);
		eb.setDescription(message);
		return eb.build();
	}
	
	protected static JDA getJDA() {
		return jda;
	}

}
