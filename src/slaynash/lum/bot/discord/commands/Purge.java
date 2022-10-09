package slaynash.lum.bot.discord.commands;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.utils.CrossServerUtils;
import slaynash.lum.bot.utils.ExceptionUtils;

public class Purge extends Command {

    @Override
    protected void onServer(String paramString, MessageReceivedEvent event) {
        if (!event.getGuild().getSelfMember().hasPermission(event.getChannel().asTextChannel(), Permission.VIEW_CHANNEL))
            return;
        if (!event.getGuild().getSelfMember().hasPermission(event.getChannel().asTextChannel(), Permission.MESSAGE_MANAGE)) {
            event.getMessage().reply("I need manage message permission to be able to remove messages.").delay(Duration.ofSeconds(30)).flatMap(Message::delete).queue();
            return;
        }
        new Thread(() -> {
            try {
                Message message = event.getMessage();
                if (!event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {
                    message.reply(CrossServerUtils.sanitizeInputString(event.getMember().getEffectiveName()) + ", You do not have permissions to remove messages.").delay(Duration.ofSeconds(30)).flatMap(Message::delete).queue();
                    message.delete().queue();
                    return;
                }

                String[] params = paramString.split(" ", 2);

                Message replied = message.getReferencedMessage();
                List<Message> messageList = new ArrayList<>();
                if (replied != null) {
                    messageList = event.getChannel().asTextChannel().getIterableHistory().takeUntilAsync(r -> r.equals(replied)).get();
                    messageList.add(replied); //add replied message to be removed

                    if (message.getContentRaw().startsWith(getName() + "u")) {
                        messageList.removeIf(m -> !m.getAuthor().equals(replied.getAuthor()));
                        if (!message.getAuthor().equals(replied.getAuthor()))
                            messageList.add(message); // add message back to be removed
                        System.out.println("User Reply purging " + messageList.size() + " messages");
                    }
                    else
                        System.out.println("Reply purging " + messageList.size() + " messages");
                }
                // else if author ID #messages
                else if (params.length > 1 && params[1].matches("^\\d{1,3}$")) {
                    int count = Integer.parseInt(params[1]);
                    messageList = event.getChannel().asTextChannel().getIterableHistory().takeAsync(count + 1).get();
                    System.out.println("Mass purging " + messageList.size() + " messages");
                }
                else
                    message.reply("Command is `" + getName() + " #` or reply to the top message.\n" + getName() + "u will only remove messages from the user replied to").queue();

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
                        event.getChannel().asTextChannel().deleteMessages(messageList).queue();
                    }
                    else { // greater than 100 messages
                        try {
                            int i = 0;
                            while (i < messageList.size()) {
                                if (i == messageList.size() - 1) { // on the very rare chance that there is only one message left
                                    messageList.get(i).delete().queue();
                                    return;
                                }
                                event.getChannel().asTextChannel().deleteMessages(messageList.subList(i, Math.min(i + 100, messageList.size()))).queue();
                                i = i + 100;
                            }
                        }
                        catch (Exception e) {
                            ExceptionUtils.reportException("An error has occurred while purging messages:", e, event.getChannel().asGuildMessageChannel());
                        }
                    }
                }
            }
            catch (Exception e) {
                ExceptionUtils.reportException("An error has occurred while running purge:", e, event.getChannel().asGuildMessageChannel());
            }
        }, "Purge").start();
    }

    @Override
    protected boolean matchPattern(String paramString) {
        return paramString.startsWith(getName());
    }

    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        return event.getMember().hasPermission(Permission.MESSAGE_MANAGE);
    }

    @Override
    public String getHelpDescription() {
        return "Purge messages `" + getName() + " #` or reply to the top message - Moderators Only\n\t**" + getName() + "u:** will only remove messages from the user replied to";
    }

    @Override
    public String getName() {
        return "l!purge";
    }
}
