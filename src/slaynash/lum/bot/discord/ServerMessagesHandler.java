package slaynash.lum.bot.discord;

import java.awt.Color;
import java.io.PrintWriter;
import java.io.StringWriter;

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
		
		new Thread(() -> {
			try {
				MelonLoaderScanner.scanLogs(event);
			}
			catch(Exception e) {
				e.printStackTrace();
				
				String error = "**An error has occured while reading logs:**\n" + getStackTrace(e);
				
				if (error.length() > 1000) {
					String[] lines = error.split("\n");
					String toSend = "";
					int i = 0;
					
					while (i < lines.length) {
						if ((toSend + lines[i] + 1).length() > 1000) {
							toSend += "...";
							break;
						}
						
						toSend += "\n" + lines[i];
					}
					
					event.getChannel().sendMessage(JDAManager.wrapMessageInEmbed(toSend, Color.RED)).queue();
				}
				else
					event.getChannel().sendMessage(JDAManager.wrapMessageInEmbed(error, Color.RED)).queue();
			}
		}).start();
  	}

	private static String getStackTrace(Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);

		e.printStackTrace(pw);

		return sw.toString();
	}
}
