package slaynash.lum.bot.discord.commands;

import java.util.HashMap;
import java.util.Map;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.CommandManager;

public class Replies extends Command {
    @Override
    protected void onServer(String paramString, MessageReceivedEvent event) {
        if (!includeInHelp(event))
            return;

        Map<String, String> replies = CommandManager.guildReplies.get(event.getGuild().getIdLong());
        String[] parts = paramString.split(" ", 2);

        if (replies == null)
            replies = new HashMap<>();

        if (parts.length == 1) {
            StringBuilder sb = new StringBuilder("Current replies in this guild:\n");
            replies.forEach((k, v) -> sb.append(k.concat(" -> ").concat(v).concat("\n")));
            event.getMessage().reply(sb.toString()).queue();
        }
        else {
            parts = parts[1].split(",");
            if (parts.length == 1) {
                if (replies.remove(parts[0].toLowerCase()) != null)
                    event.getMessage().reply("Removed the reply " + parts[0]).queue();
                else {
                    event.getMessage().reply("Please do `l!replies <trigger>,<message>`").queue();
                    return;
                }
            }
            else {
                if (replies.put(parts[0].toLowerCase(), parts[1].trim()) != null) {
                    event.getMessage().reply("Updated the reply " + parts[0]).queue();
                }
                else
                    event.getMessage().reply("Created the reply " + parts[0]).queue();
            }
            if (replies.size() == 0)
                CommandManager.guildReplies.remove(event.getGuild().getIdLong());
            else
                CommandManager.guildReplies.put(event.getGuild().getIdLong(), replies);
            CommandManager.saveReplies();
        }
    }

    @Override
    protected boolean matchPattern(String paramString) {
        return paramString.startsWith("l!replies");
    }

    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        return event.getMember().hasPermission(Permission.MESSAGE_MANAGE);
    }

    @Override
    public String getHelpDescription() {
        return "Sets auto message replies";
    }

    @Override
    public String getHelpName() {
        return "l!replies";
    }
}
