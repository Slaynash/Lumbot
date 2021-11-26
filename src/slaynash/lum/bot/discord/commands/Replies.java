package slaynash.lum.bot.discord.commands;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.utils.Utils;

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
                Utils.replyEmbed(sb.toString(), null, event);
            }
            else {
                Utils.replyEmbed("There are no replies in this guild", null, event);
            }
        }
        else if (parts[0].startsWith(getName() + "r")) {
            parts = parts[1].split("\n", 2);
            String pattern = parts[0].toLowerCase();
            try {
                Pattern.compile(pattern);
            }
            catch (Exception e) {
                Utils.replyEmbed("Invalid Regex! Please use a site like regexr.com to test regex", Color.RED, event);
                return;
            }
            if (parts.length == 1) {
                if (regexReplies.remove(pattern) != null)
                    Utils.replyEmbed("Removed the regex `" + parts[0] + "`", Color.GREEN, event);
                else {
                    Utils.replyEmbed("Please do `" + getName() + "r <regex>newline<message>`", Color.RED, event);
                    return;
                }
            }
            else {
                if (regexReplies.put(pattern, parts[1].trim()) != null) {
                    Utils.replyEmbed("Updated the regex reply `" + parts[0] + "`", Color.GREEN, event);
                }
                else
                    Utils.replyEmbed("Created the regex reply `" + parts[0] + "`", Color.GREEN, event);
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
                    Utils.replyEmbed("Removed the reply `" + parts[0] + "`", Color.GREEN, event);
                else {
                    Utils.replyEmbed("Please do `" + getName() + " <trigger>newline<message>`", Color.RED, event);
                    return;
                }
            }
            else {
                if (replies.put(parts[0].toLowerCase(), parts[1].trim()) != null) {
                    Utils.replyEmbed("Updated the reply `" + parts[0] + "`", Color.GREEN, event);
                }
                else
                    Utils.replyEmbed("Created the reply `" + parts[0] + "`", Color.GREEN, event);
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
