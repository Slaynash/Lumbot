package slaynash.lum.bot.discord.commands;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.gcardone.junidecode.Junidecode;
import slaynash.lum.bot.discord.Command;

public class DumpID extends Command {
    @Override
    protected void onServer(String paramString, MessageReceivedEvent event) {
        if (!includeInHelp(event))
            return;
        String[] parts = paramString.split(" ", 2);
        if (parts.length < 2) {
            event.getMessage().reply("Usage: " + getName() + " <Regex>").queue();
            return;
        }
        String regex = Junidecode.unidecode(parts[1]).toLowerCase();
        try {
            Pattern.compile(regex);
        } catch (PatternSyntaxException exception) {
            event.getMessage().reply("Invalid Regex, please check your regex and try again").queue();
            return;
        }
        List<Member> members = new ArrayList<>();
        event.getGuild().loadMembers(m -> {
            if (m.getNickname() != null && Junidecode.unidecode(m.getNickname()).toLowerCase().matches(regex)) {
                members.add(m);
                return;
            }
            if (Junidecode.unidecode(m.getUser().getName()).toLowerCase().matches(regex))
                members.add(m);
        });
        if (members.size() == 0) {
            event.getMessage().reply("No users found.").queue();
            return;
        }
        members.sort(Comparator.comparing(m -> m.getUser().getId()));
        StringBuilder sb = new StringBuilder();
        for (Member m : members) {
            sb.append(m.getUser().getId()).append(" ").append(m.getUser().getName()).append(m.getNickname() != null ? " nickname: " + m.getNickname() : "").append("\n");
        }
        event.getMessage().reply(sb.toString().getBytes(), event.getGuild().getName() + " " + regex + ".txt").queue();
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
        return "Dump all user IDs in the server that match a regex";
    }

    @Override
    public String getName() {
        return "l!dumpid";
    }
}
