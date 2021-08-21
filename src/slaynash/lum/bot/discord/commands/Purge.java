package slaynash.lum.bot.discord.commands;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.utils.CrossServerUtils;
import slaynash.lum.bot.utils.ExceptionUtils;

public class Purge extends Command {

    @Override
    protected void onServer(String paramString, MessageReceivedEvent event) {
        if (!event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.VIEW_CHANNEL))
            return;
        Thread thread = new Thread(() -> {
            try {
                Message message = event.getMessage();
                if (!event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {
                    message.reply(CrossServerUtils.sanitizeInputString(event.getMember().getEffectiveName()) + ", You do not have permissions to remove messages.").delay(Duration.ofSeconds(30)).flatMap(Message::delete).queue();
                    message.delete().queue();
                    return;
                }

                String[] params = paramString.split(" ", 2);

                Message replied = message.getReferencedMessage();
                MessageHistory messages;
                List<Message> messageList = new ArrayList<>();
                List<Message> retrievedHistory = new ArrayList<>(); // set to replied to get the ball rolling
                if (replied != null) {
                    messageList.add(replied); //add replied message to be removed
                    retrievedHistory.add(replied); //add replied message to start looping, this will not be added to be removed
                    do {
                        messages = event.getChannel().getHistoryAfter(retrievedHistory.get(0), 100).complete(); //100 is max you can get
                        retrievedHistory = messages.getRetrievedHistory();
                        messageList.addAll(retrievedHistory);
                    }
                    while (!retrievedHistory.get(0).getContentStripped().equals(message.getContentStripped()));

                    if (message.getContentRaw().startsWith("l!purgeu")) {
                        messageList.removeIf(m -> m.getAuthor().getIdLong() != replied.getAuthor().getIdLong());
                        if (message.getAuthor().getIdLong() != replied.getAuthor().getIdLong())
                            messageList.add(message); // add message back to be removed
                        System.out.println("User Reply purging " + messageList.size() + " messages");
                    }
                    else
                        System.out.println("Reply purging " + messageList.size() + " messages");
                }
                // else if author ID #messages
                else if (params.length > 1 && params[1].matches("^\\d{1,3}$")) {
                    int count = Integer.parseInt(params[1]);
                    messageList.add(message);
                    while (count > 0) {
                        messages = event.getChannel().getHistoryBefore(messageList.get(messageList.size() - 1), Math.min(count, 100)).complete();
                        retrievedHistory = messages.getRetrievedHistory();
                        messageList.addAll(retrievedHistory);
                        count = count - retrievedHistory.size();
                    }
                    System.out.println("Mass purging " + messageList.size() + " messages");
                }
                else
                    message.reply("Command is `l!purge #` or reply to the top message.").queue();

                //remove if unknown message ie message already removed
                messageList.removeIf(m -> m.getType() == MessageType.UNKNOWN);
                List<Message> oldMessages = new ArrayList<>();
                boolean oldMessage = false;
                for (Message mes : messageList) {
                    if (mes.getTimeCreated().isBefore(OffsetDateTime.now().minusWeeks(2))) {
                        if (!oldMessage) {
                            event.getMessage().reply("Purge contains old messages, I need to remove one at a time and this can take a while. Please be patent.").delay(Duration.ofSeconds(5)).flatMap(Message::delete).queue();
                            oldMessage = true;
                        }
                        oldMessages.add(mes); //manually remove messages older than 2 weeks old
                        mes.delete().queue();
                    }
                }
                messageList.removeAll(oldMessages);

                // removing the messages
                if (messageList.size() > 0) {
                    if (messageList.size() == 1) {
                        messageList.get(0).delete().queue();
                    }
                    else if (messageList.size() <= 100) {
                        event.getTextChannel().deleteMessages(messageList).queue();
                    }
                    else { // greater than 100 messages
                        try {
                            int i = 0;
                            while (i < messageList.size() - 1) {
                                event.getTextChannel().deleteMessages(messageList.subList(i, Math.min(i + 100, messageList.size() - 1))).complete();
                                i = i + 100;
                                Thread.sleep(1111); // ratelimited once per second per Guild. I am ignoring the "per guild" part for now.
                            }
                            if (i == messageList.size() - 1) // on the very rare chance that there is only one message left
                                messageList.get(messageList.size() - 1).delete().queue();
                        }
                        catch (Exception e) {
                            ExceptionUtils.reportException("An error has occurred while purging messages:", e, event.getTextChannel());
                        }
                    }
                }
            }
            catch (Exception e) {
                ExceptionUtils.reportException("An error has occurred while running purge:", e, event.getTextChannel());
            }
        });
        new Thread(() -> {
            thread.start();
            try {
                Thread.sleep(20 * 60 * 1000);
            }
            catch (InterruptedException ignored) { }
            if (thread.isAlive()) {
                thread.interrupt(); //stop purge if taking too long because .complete gotten stuck
                System.out.println("Stopping purge because it took too long");
                event.getTextChannel().sendMessage("Stopping purge, It took way too long.").queue();
            }
        }).start();
    }

    @Override
    protected boolean matchPattern(String paramString) {
        return paramString.startsWith("l!purge");
    }

    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        return (event.getMember().hasPermission(Permission.MESSAGE_MANAGE));
    }

    @Override
    public String getHelpDescription() {
        return "Purge messages `l!purge #` or reply to the top message - Moderators Only\n\tpurgeu will only remove messages from the user replied to";
    }

    @Override
    public String getHelpName() {
        return "l!purge";
    }
}
