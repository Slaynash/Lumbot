package slaynash.lum.bot.discord.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import slaynash.lum.bot.discord.JDAManager;
import slaynash.lum.bot.utils.ExceptionUtils;

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

    public static boolean isLumDev(Member member) {
        return isLumDev(member.getId());
    }

    public static boolean isLumDev(String userId) {
        return
            "145556654241349632".equals(userId) || // Slaynash
            "240701606977470464".equals(userId); // Rakosi
    }

    private static int lastGuildCount;
    public static void loadGuildCount() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("guildcount.txt"));
            String line;
            while ((line = reader.readLine()) != null) {
                lastGuildCount = Integer.parseInt(line);
            }
            reader.close();
        }
        catch (IOException e) {
            ExceptionUtils.reportException("Failed to load Guild Count", e);
        }
    }
    private static void saveGuildCount(int count) {
        lastGuildCount = count;
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("guildcount.txt"))) {
            writer.write(count);
        }
        catch (IOException e) {
            ExceptionUtils.reportException("Failed to save Guild Count", e);
        }
    }
    public static void checkGuildCount(GuildJoinEvent event) {
        int guildSize = JDAManager.getJDA().getGuilds().size();
        System.out.println("Joined " + event.getGuild().getName() + ", connected to " + guildSize + " guilds");
        if (guildSize % 25 == 0 && guildSize != lastGuildCount) {
            JDAManager.getJDA().getGuildById(633588473433030666L).getTextChannelById(876466104036393060L).sendMessage("I joined my " + guildSize + "th guild <:Neko_cat_woah:851935805874110504>").queue();
            saveGuildCount(guildSize);
        }
    }
}
