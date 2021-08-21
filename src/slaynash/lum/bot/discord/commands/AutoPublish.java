package slaynash.lum.bot.discord.commands;

import java.time.Duration;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.CommandManager;

public class AutoPublish extends Command {

    @Override
    protected void onServer(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {
        if (!paramMessageReceivedEvent.getTextChannel().isNews()) {
            paramMessageReceivedEvent.getMessage().reply("This is not an announcement channel.").queue();
            return;
        }
        if (!paramMessageReceivedEvent.getGuild().getSelfMember().hasPermission(paramMessageReceivedEvent.getTextChannel(), Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.MESSAGE_MANAGE, Permission.VIEW_CHANNEL)) {
            paramMessageReceivedEvent.getAuthor().openPrivateChannel().flatMap(channel -> channel.sendMessage(
                "Lum does not have the proper permissions to publish messages in " + paramMessageReceivedEvent.getTextChannel().getName() + " Please make sure that Lum has Message Read, Write, and Manage Permissions.")).queue(null, m -> System.out.println("failed to open DM"));
            return;
        }

        if (CommandManager.apChannels.contains(paramMessageReceivedEvent.getChannel().getIdLong())) {
            CommandManager.apChannels.remove(paramMessageReceivedEvent.getChannel().getIdLong());
            CommandManager.saveAPChannels();
            paramMessageReceivedEvent.getChannel().sendMessage("Successfully removed " + paramMessageReceivedEvent.getChannel().getName() + " from autopublish!").delay(Duration.ofSeconds(5)).flatMap(Message::delete).queue();
            System.out.println("Successfully removed autopublish from " + paramMessageReceivedEvent.getChannel().getName());
        }
        else {
            CommandManager.apChannels.add(paramMessageReceivedEvent.getChannel().getIdLong());
            CommandManager.saveAPChannels();
            paramMessageReceivedEvent.getChannel().sendMessage("Successfully set " + paramMessageReceivedEvent.getChannel().getName() + " to autopublish!").delay(Duration.ofSeconds(5)).flatMap(Message::delete).queue();
            System.out.println("Successfully added autopublish to " + paramMessageReceivedEvent.getChannel().getName());
        }
        paramMessageReceivedEvent.getMessage().delete().queue();
    }

    @Override
    protected boolean matchPattern(String paramString) {
        return paramString.split(" ", 2)[0].equals("l!autopublish");
    }

    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        return false;
    }

}
