package slaynash.lum.bot.discord.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.GuildConfigurations;
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

    /**
     * Check if sender is part of Guild Staff/Trusted.
     * @param event MessageReceivedEvent
     * @return true if sender really was Guild Staff/Trusted
     */
    public static boolean checkIfStaff(MessageReceivedEvent event) {
        if (event.getMember() == null) //https://discord.com/channels/633588473433030666/851519891965345845/883320272982278174
            return false;
        if (event.getMember().hasPermission(Permission.ADMINISTRATOR) || event.getMember().hasPermission(Permission.MESSAGE_MANAGE))
            return true;
        for (Entry<Long, long[]> whitelistedRolesServer : GuildConfigurations.whitelistedRolesServers.entrySet()) {
            Guild targetGuild;
            Member serverMember;
            if ((targetGuild = event.getJDA().getGuildById(whitelistedRolesServer.getKey())) != null &&
                (serverMember = targetGuild.getMember(event.getAuthor())) != null) {
                List<Role> roles = serverMember.getRoles();
                for (Role role : roles) {
                    long roleId = role.getIdLong();
                    for (long whitelistedRoleId : whitelistedRolesServer.getValue()) {
                        if (whitelistedRoleId == roleId) {
                            return true; // The sender is whitelisted
                        }
                    }
                }
            }
        }
        return false;
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
            reader = new BufferedReader(new FileReader("storage/guildcount.txt"));
            lastGuildCount = Integer.parseInt(reader.readLine());
            reader.close();
        }
        catch (IOException e) {
            ExceptionUtils.reportException("Failed to load Guild Count", e);
        }
    }
    private static void saveGuildCount(int count) {
        lastGuildCount = count;
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("storage/guildcount.txt"))) {
            writer.write(String.valueOf(count));
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
