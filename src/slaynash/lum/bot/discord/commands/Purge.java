package slaynash.lum.bot.discord.commands;

import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.ExceptionUtils;
import slaynash.lum.bot.discord.ServerMessagesHandler;

public class Purge extends Command {

    @Override
    protected void onServer(String paramString, MessageReceivedEvent event) {
        try{
            if (!event.getAuthor().getId().equals(event.getGuild().getOwnerId()) && !ServerMessagesHandler.checkIfStaff(event)){
                event.getMessage().reply("You do not have perms to use this command.").queue();
                return;
            }

            String[] params = paramString.split(" ", 2);

            Message replied = event.getMessage().getReferencedMessage();
            if(replied != null){
                MessageHistory messages = event.getChannel().getHistoryAfter(replied, 100).complete(); //100 is max you can get
                List<Message> messagelist = new ArrayList<>(messages.getRetrievedHistory());
                messagelist.add(replied);
                event.getTextChannel().deleteMessages(messagelist).queue();
            }
            else if( params.length>1 && params[1].matches("^\\d{1,3}$") ){
                int count = Integer.parseInt(params[1]);
                MessageHistory messages = event.getChannel().getHistoryBefore(event.getMessage(), count).complete();
                List<Message> messagelist = new ArrayList<>(messages.getRetrievedHistory());
                messagelist.add(event.getMessage());
                event.getTextChannel().deleteMessages(messagelist).queue();
            }
            else 
                event.getMessage().reply("Command is `l!purge #` or reply to the top message.").queue();
        } catch(Exception e) {
            ExceptionUtils.reportException("An error has occured while running purge:", e, event.getTextChannel());
        }
    }

    @Override
    protected boolean matchPattern(String paramString) {
        return paramString.startsWith("l!purge");
    }

    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        return (event.getAuthor().getId().equals(event.getGuild().getOwnerId()) || !ServerMessagesHandler.checkIfStaff(event));
    }

    @Override
    public String getHelpDescription() {
        return "Purge messages `l!purge #` or reply to the top message - Staff Only";
    }

    @Override
    public String getHelpName() {
        return "l!purge";
    }
}
