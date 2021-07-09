package slaynash.lum.bot.discord;

import java.util.List;

import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.utils.TimeManager;

public class PrivateMessagesHandler {
    public static void handle(MessageReceivedEvent event) {
        System.out.printf("[%s] [PM] %s: %s\n", new Object[] { TimeManager.getTimeForLog(), event.getAuthor().getName(),
                event.getMessage().getContentRaw().replace("\n", "\n\t\t") });
        List<Attachment> attachments = event.getMessage().getAttachments();
        if (attachments.size() > 0)
            System.out.println("[" + TimeManager.getTimeForLog() + "] " + attachments.size() + " Files");
        for (Attachment a : attachments)
            System.out.println("[" + TimeManager.getTimeForLog() + "] - " + a.getUrl());

        if (event.getAuthor().getIdLong() != JDAManager.getJDA().getSelfUser().getIdLong())
            event.getChannel()
                    .sendMessage("I'm sorry, but I don't handle direct messages. Please use me in a server I'm in!")
                    .queue();

        // CommandManager.runAsClient(event);
    }
}
