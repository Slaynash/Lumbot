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

        Map<String, String> regexReplies = CommandManager.guildRegexReplies.getOrDefault(event.getGuild().getIdLong(), new HashMap<>());
        Map<String, String> replies = CommandManager.guildReplies.getOrDefault(event.getGuild().getIdLong(), new HashMap<>());
        String[] parts = paramString.split(" ", 2);

        if (parts.length == 1) {
            if (replies.size() > 0 || regexReplies.size() > 0) {
                StringBuilder sb = new StringBuilder("Current replies in this guild:\n");
                replies.forEach((k, v) -> sb.append("`".concat(k).concat("` -> `").concat(v).concat("`\n")));
                regexReplies.forEach((k, v) -> sb.append("`".concat(k).concat("` -> `").concat(v).concat("`\n")));
                event.getMessage().reply(sb.toString()).queue();
            }
            else {
                event.getMessage().reply("There are no replies in this guild").queue();
            }
        }
        else if (parts[0].startsWith(getName() + "r")) {
            parts = parts[1].split("\n", 2);
            if (parts.length == 1) {
                if (regexReplies.remove(parts[0].toLowerCase()) != null)
                    event.getMessage().reply("Removed the regex `" + parts[0] + "`").queue();
                else {
                    event.getMessage().reply("Please do `" + getName() + "r <regex>newline<message>`").queue();
                    return;
                }
            }
            else {
                if (regexReplies.put(parts[0].toLowerCase(), parts[1].trim()) != null) {
                    event.getMessage().reply("Updated the regex reply `" + parts[0] + "`").queue();
                }
                else
                    event.getMessage().reply("Created the regex reply `" + parts[0] + "`").queue();
            }
            if (regexReplies.size() == 0)
                CommandManager.guildRegexReplies.remove(event.getGuild().getIdLong());
            else
                CommandManager.guildRegexReplies.put(event.getGuild().getIdLong(), regexReplies);
            CommandManager.saveReplies();
        }
        else {
            parts = parts[1].split("\n", 2);
            if (parts.length == 1) {
                if (replies.remove(parts[0].toLowerCase()) != null)
                    event.getMessage().reply("Removed the reply `" + parts[0] + "`").queue();
                else {
                    event.getMessage().reply("Please do `" + getName() + " <trigger>newline<message>`").queue();
                    return;
                }
            }
            else {
                if (replies.put(parts[0].toLowerCase(), parts[1].trim()) != null) {
                    event.getMessage().reply("Updated the reply `" + parts[0] + "`").queue();
                }
                else
                    event.getMessage().reply("Created the reply `" + parts[0] + "`").queue();
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
        return paramString.startsWith(getName());
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
    public String getName() {
        return "l!replies";
    }
}
