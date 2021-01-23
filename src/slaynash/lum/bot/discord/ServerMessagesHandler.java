package slaynash.lum.bot.discord;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class ServerMessagesHandler {
	public static void handle(MessageReceivedEvent event) {
		System.out.printf("[%s] [%s][%s] %s: %s\n",
				TimeManager.getTimeForLog(),
				event.getGuild().getName(),
				event.getTextChannel().getName(),
				event.getAuthor().getName(),
				event.getMessage().getContentRaw() );
    
    CommandManager.runAsServer(event);
    
    MelonLoaderScanner.scanLogs(event);
  }
}
