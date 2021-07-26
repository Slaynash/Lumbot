package slaynash.lum.bot.discord.commands;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.utils.CrossServerUtils;
import slaynash.lum.bot.utils.ExceptionUtils;

public class Purge extends Command {

    @Override
    protected void onServer(String paramString, MessageReceivedEvent event) {
        if (!event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.VIEW_CHANNEL))
            return;
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
            List<Message> messagelist = new ArrayList<>();
            List<Message> retrievedHistory = new ArrayList<>(); // set to replied to get the ball rolling
            if (replied != null) {
                retrievedHistory.add(replied);
                do {
                    messages = event.getChannel().getHistoryAfter(retrievedHistory.get(0), 100).complete(); //100 is max you can get
                    retrievedHistory = messages.getRetrievedHistory();
                    messagelist.addAll(retrievedHistory);
                }
                while (!retrievedHistory.get(0).getContentStripped().equals(message.getContentStripped()));

                if (message.getContentRaw().startsWith("l!purgeu")) {
                    messagelist.removeIf(m -> m.getAuthor().getIdLong() != replied.getAuthor().getIdLong());
                    if (message.getAuthor().getIdLong() != replied.getAuthor().getIdLong())
                        messagelist.add(message); // add message back to be removed
                    System.out.println("User Reply purging " + messagelist.size() + " messages");
                }
                else
                    System.out.println("Reply purging " + messagelist.size() + " messages");
            }
            // else if author ID #messages
            else if (params.length > 1 && params[1].matches("^\\d{1,3}$")) {
                int count = Integer.parseInt(params[1]);
                messagelist.add(message);
                while (count > 0) {
                    messages = event.getChannel().getHistoryBefore(messagelist.get(messagelist.size() - 1), count > 100 ? 100 : count).complete();
                    retrievedHistory = messages.getRetrievedHistory();
                    messagelist.addAll(retrievedHistory);
                    count = count - retrievedHistory.size();
                }
                System.out.println("Mass purging " + messagelist.size() + " messages");
            }
            else
                message.reply("Command is `l!purge #` or reply to the top message.").queue();

            // removing the messages
            if (messagelist.size() > 0) {
                if (messagelist.size() <= 100) {
                    event.getTextChannel().deleteMessages(messagelist).queue();
                }
                else { // greater than 100 messages
                    new Thread(() -> {
                        try {
                            int i = 0;
                            while (i < messagelist.size() - 1) {
                                event.getTextChannel().deleteMessages(messagelist.subList(i, i + 100 > messagelist.size() - 1 ? messagelist.size() - 1 : i + 100)).complete();
                                i = i + 100;
                                Thread.sleep(1111); // ratelimited once per second per Guild. I am ignoring the "per guild" part for now.
                            }
                            if (i == messagelist.size() - 1) // on the very rare chance that there is only one message left
                                messagelist.get(messagelist.size() - 1).delete().queue();
                        }
                        catch (Exception e) {
                            ExceptionUtils.reportException("An error has occurred while purging messages:", e, event.getTextChannel());
                        }
                    }).start();
                }
            }
        }
        catch (Exception e) {
            ExceptionUtils.reportException("An error has occurred while running purge:", e, event.getTextChannel());
        }
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
        return "Purge messages `l!purge #` or reply to the top message - Moderators Only";
    }

    @Override
    public String getHelpName() {
        return "l!purge";
    }
}
