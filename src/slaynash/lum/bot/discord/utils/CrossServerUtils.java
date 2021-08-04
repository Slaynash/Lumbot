package slaynash.lum.bot.discord.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.dv8tion.jda.api.entities.Member;
import slaynash.lum.bot.discord.JDAManager;

public final class CrossServerUtils {

    public static Member resolveMember(String guildId, String userId) {
        return JDAManager.getJDA().getGuildById(guildId).getMemberById(userId);
    }

    public static String sanitizeInputString(String input) {
        if (input == null) input = "";

        input = input
        .replace("](", " ")
        .replaceAll("^(?!<)@", "@ ")
        .replace("*", "\\*")
        .replace("`", "\\`");

        input = Pattern.compile("(nigg(er|a)|porn|penis)", Pattern.CASE_INSENSITIVE).matcher(input).replaceAll(Matcher.quoteReplacement("{REDACTED}"));

        input = input.substring(0, Math.min(input.length(), 50)); // limit inputs to 50 chars

        return input;
    }
}
