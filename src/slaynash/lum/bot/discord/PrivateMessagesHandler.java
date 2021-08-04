package slaynash.lum.bot.discord;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import slaynash.lum.bot.utils.TimeManager;

public class PrivateMessagesHandler {
    private static final List<Long> dmSS = new ArrayList<>();
    public static void handle(MessageReceivedEvent event) {

        if (event.getAuthor().getIdLong() != JDAManager.getJDA().getSelfUser().getIdLong()) {
            System.out.printf("[%s] [DM] %s%s%s: %s\n", TimeManager.getTimeForLog(),
                    event.getAuthor().getAsTag(),
                    event.getMessage().isEdited() ? " *edited*" : "",
                    event.getMessage().getType().isSystem() ? " *system*" : "",
                    event.getMessage().getContentRaw().replace("\n", "\n\t\t"));
            List<Attachment> attachments = event.getMessage().getAttachments();
            if (attachments.size() > 0) {
                System.out.println("[" + TimeManager.getTimeForLog() + "] " + attachments.size() + " Files");
                for (Attachment a : attachments)
                    System.out.println("[" + TimeManager.getTimeForLog() + "] - " + a.getUrl());
            }
            if (ScamShield.ssValue(event) > 3 && !dmSS.contains(event.getAuthor().getIdLong())) {
                System.out.println("I was DM'd Scam, sending FitnessGram™");
                dmSS.add(event.getAuthor().getIdLong());
                event.getChannel().sendMessage("The FitnessGram™ Pacer Test is a multistage aerobic capacity test that progressively gets more difficult as it continues.").delay(Duration.ofSeconds(3))
                    .flatMap(m -> m.getChannel().sendMessage("The 20 meter pacer test will begin in 30 seconds.").delay(Duration.ofSeconds(3))
                    .flatMap(m2 -> m2.getChannel().sendMessage("Line up at the start.").delay(Duration.ofSeconds(3))
                    .flatMap(m3 -> m3.getChannel().sendMessage("The running speed starts slowly, but gets faster each minute after you hear this signal.").delay(Duration.ofSeconds(3))
                    .flatMap(m4 -> m4.getChannel().sendMessage("Ding!").delay(Duration.ofSeconds(3))
                    .flatMap(m5 -> m5.getChannel().sendMessage("A single lap should be completed each time you hear this sound.").delay(Duration.ofSeconds(3))
                    .flatMap(m6 -> m6.getChannel().sendMessage("Ding!").delay(Duration.ofSeconds(3))
                    .flatMap(m7 -> m7.getChannel().sendMessage("Remember to run in a straight line, and run as long as possible.").delay(Duration.ofSeconds(3))
                    .flatMap(m8 -> m8.getChannel().sendMessage("The second time you fail to complete a lap before the sound, your test is over.").delay(Duration.ofSeconds(3))
                    .flatMap(m9 -> m9.getChannel().sendMessage("The test will begin on the word start.").delay(Duration.ofSeconds(3))
                    .flatMap(m10 -> m10.getChannel().sendMessage("On your mark, get ready, Ding!"))))))))))).queue();
            }
            else {
                event.getChannel()
                    .sendMessage("I'm sorry, but I don't handle direct messages. Please use me in a server I'm in!")
                    .queue();
            }
        }
        // CommandManager.runAsClient(event);
    }

    public static void handle(MessageUpdateEvent event) {
        handle(new MessageReceivedEvent(event.getJDA(), event.getResponseNumber(), event.getMessage()));
    }
}
