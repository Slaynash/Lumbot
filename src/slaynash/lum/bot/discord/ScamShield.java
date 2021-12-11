package slaynash.lum.bot.discord;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.gcardone.junidecode.Junidecode;
import slaynash.lum.bot.discord.melonscanner.LogCounter;
import slaynash.lum.bot.discord.utils.CrossServerUtils;
import slaynash.lum.bot.utils.ExceptionUtils;
import slaynash.lum.bot.utils.Utils;

public class ScamShield {
    public static final String LOG_IDENTIFIER = "ScamShield";

    private static final Queue<MessageReceivedEvent> allMessages = new LinkedList<>();
    private static final Queue<HandledServerMessageContext> handledMessages = new LinkedList<>();

    private static final Map<Long, ScheduledFuture<?>> ssQueuedMap = new HashMap<>();
    private static final Map<String, Integer> ssTerms = new HashMap<>() {{ //Keys must be all lowercase and no space
            put("@everyone", 2);
            put("money", 1);
            put("loot", 1);
            put("csgo", 2);
            put("trade", 2);
            put("skin", 1);
            put("knife", 1);
            put("offer", 1);
            put("btc", 1);
            put("bitcoin", 1);
            put("nitro", 1);
            put("1month", 1);
            put("3month", 1);
            put("free", 1);
            put("case", 1);
            put("!!!", 1);
            put("booster", 1);
            put("dollar", 1);
            put("download", 1);
            put("100%", 1);
            put("bro", 1);
            put("nigger", 1);
            put("checkthis", 1);
            put("linkforyou", 1);
            put("screenshareinhd", 2);
            put("friendhasgiftedyou", 2);
            put("standoutinyourfavoritediscord", 2);
            put("standoutinyourfavoritesdiscord", 2);
        }};
    private static final Map<String, Integer> ssTermsMatches = new HashMap<>() {{
            put(".*made.*game.*", 1);
            put(".*left.*game.*", 2);
            put(".*nitro.*free.*steam.*", 2);
        }};
    private static final Map<String, Integer> ssTermsPlus = new HashMap<>() {{
            put("http", 2);
            put(".ru/", 1);
            put("bit.ly", 2);
            put("cutt.ly", 2);
            put("mega.nz", 2);
            put("hour", 1);
            put("$", 1);
            put("discord", 1);
        }};

    public static int ssValue(MessageReceivedEvent event) {
        // I found a simple referral and you can loot skins there\nhttp://csgocyber.ru/simlpebonus\nIf it's not difficult you can then throw me a trade and I'll give you the money
        //@everyone Hello I am leaving CS:GO and giving away my skins to people who send trade offers. For first people I will give away my 3 knifes. Don't be greedy and take few skins :  https://streancommunuty.ru/tradoffer/new/?partner=1284276379&token=iMDdLkoe

        if (checkCrasher(event))
            // return 69;
            event.getJDA().getGuildById(633588473433030666L /* Slaynash's Workbench */).getTextChannelById(919084218724790292L).sendMessage(event.getAuthor().getAsTag() + " just posted a crasher video in " + event.getChannel().getName());
        Map<String, Integer> ssFoundTerms = new HashMap<>();
        boolean newAccount = event.getAuthor().getTimeCreated().isAfter(OffsetDateTime.now().minusDays(7));
        String message = Junidecode.unidecode(event.getMessage().getContentStripped());
        if (event.getMessage().getEmbeds().size() > 0) {
            MessageEmbed embed = event.getMessage().getEmbeds().get(0);
            message = message + embed.getTitle() + embed.getDescription();
        }
        message = message.toLowerCase().replace(":", "").replace(" ", "");
        final String finalMessage = message;

        long crossPost = 0;
        if (!event.isFromType(ChannelType.PRIVATE)) {
            Set<String> nameSet = new HashSet<>(); //used to filter one message per channel
            crossPost = allMessages.stream()
                .filter(m -> m.getMember().getIdLong() == event.getMember().getIdLong())
                .filter(m -> m.getGuild().getIdLong() == event.getGuild().getIdLong())
                .filter(m -> m.getChannel().getIdLong() != event.getChannel().getIdLong() /* Counts all messages in other channels  */)
                .filter(m -> (
                    (
                        m.getMessage().getAttachments().size() == 0
                        && m.getMessage().getContentDisplay().equalsIgnoreCase(event.getMessage().getContentDisplay())
                        && ssTerms.keySet().stream().anyMatch(s -> m.getMessage().getContentDisplay().toLowerCase().contains(s)))
                    || (
                        event.getMessage().getAttachments().size() > 0
                        && m.getMessage().getAttachments().size() > 0
                        && event.getMessage().getAttachments().get(0).getFileName().equalsIgnoreCase(m.getMessage().getAttachments().get(0).getFileName())))) //count crossposted files
                .filter(e -> nameSet.add(e.getChannel().getId())) //filter one per channel
                .count();
        }

        int suspiciousValue = newAccount ? 1 : 0; //add sus points if account is less than 7 days old
        suspiciousValue += (int) (crossPost);

        ssFoundTerms.putAll(ssTerms.entrySet().stream().filter(f -> finalMessage.contains(f.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        ssFoundTerms.putAll(ssTermsMatches.entrySet().stream().filter(f -> finalMessage.matches(f.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        if ((suspiciousValue + ssFoundTerms.values().stream().reduce(0, Integer::sum)) > 1) {
            ssFoundTerms.putAll(ssTermsPlus.entrySet().stream().filter(f -> finalMessage.contains(f.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        }

        suspiciousValue += ssFoundTerms.values().stream().reduce(0, Integer::sum);

        if (suspiciousValue > 0) {
            System.out.println("Scam Shield points for this message: " + suspiciousValue + (newAccount ? " New Account" : "") + (crossPost > 0 ? " Crossposted " : " ") + ssFoundTerms.keySet());
        }

        if (event.getMessage().getMentions(MentionType.USER).size() > 3) //kick mass ping selfbots
            suspiciousValue = 69420;

        return suspiciousValue;
    }

    public static boolean checkForFishing(MessageReceivedEvent event) {
        if (event.getMember() == null)
            return false;
        if (CrossServerUtils.checkIfStaff(event))
            return false;

        long guildID = event.getGuild().getIdLong();
        int suspiciousValue = ssValue(event);
        boolean massping = false;

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        allMessages.removeIf(m -> m.getMessage().getTimeCreated().toLocalDateTime().until(now, ChronoUnit.MINUTES) > 3); //remove saved messages for crosspost checks
        handledMessages.removeIf(m -> m.creationTime.until(now, ChronoUnit.MINUTES) > 3); //remove all saved messages that is older than 3 minutes
        handledMessages.removeIf(m -> event.getMessageIdLong() == m.messageReceivedEvent.getMessageIdLong()); //remove original message if edited
        allMessages.add(event);

        if (suspiciousValue == 69420) {
            massping = true;
            suspiciousValue = 0;
        }

        if (suspiciousValue < 3)
            suspiciousValue = 0;
        if (suspiciousValue > 3 && suspiciousValue < 6) //if one message gets 6+ then it is an instant kick on first message
            suspiciousValue = 3;
        if (suspiciousValue > 0)
            handledMessages.add(new HandledServerMessageContext(event, suspiciousValue, guildID)); // saves a copy of message and point, should avoid false-positives, force 2 messages
        else
            return false;

        List<HandledServerMessageContext> sameauthormessages = handledMessages.stream()
            .filter(m -> m.messageReceivedEvent.getMember().getIdLong() == event.getMember().getIdLong() && m.guildId == guildID)
            .collect(Collectors.toList());

        int suspiciousCount = sameauthormessages.stream().map(m -> m.suspiciousValue).reduce(0, Integer::sum); //this adds all points that one user collected

        if (massping && !event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {
            handleMassPings(event, sameauthormessages, suspiciousCount);
            return true;
        }
        else if (suspiciousCount > 4 && !event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {
            handleCrossBan(event, sameauthormessages, suspiciousCount);
            return true;
        }

        return false;
    }

    public static boolean checkForFishingPrivate(MessageReceivedEvent event) {
        String message = event.getMessage().getContentDisplay().toLowerCase();

        if (CrossServerUtils.checkIfStaff(event))
            return false;

        int suspiciousValue = ssValue(event);
        if (suspiciousValue > 0)
            event.getJDA().getGuildById(633588473433030666L).getTextChannelById(896839871543525417L).sendMessage("DM from " + event.getAuthor().getAsTag() + " " + event.getAuthor().getId() + " gotten " + suspiciousValue + " sus points\nMutual Servers: "
                + event.getAuthor().getMutualGuilds().stream().map(Guild::getName).collect(Collectors.toList()) + "\n\n" + message).queue();
        if (suspiciousValue <= 3)
            return false;

        return handleCrossBan(event, null, suspiciousValue);
    }

    private static boolean handleCrossBan(MessageReceivedEvent event, List<HandledServerMessageContext> sameauthormessages, int suspiciousCount) {
        System.out.println(event.getAuthor().getMutualGuilds().stream().map(Guild::getName).collect(Collectors.toList()));
        List<Guild> mutualGuilds = new ArrayList<>(event.getAuthor().getMutualGuilds());
        mutualGuilds.removeIf(g -> {
            if (sameauthormessages != null && g == event.getGuild())
                return false;
            if (GuildConfigurations.configurations.get(g.getIdLong()) != null)
                return !GuildConfigurations.configurations.get(g.getIdLong())[GuildConfigurations.ConfigurationMap.SSCROSS.ordinal()];
            else
                return true;
        });
        boolean status = false;
        for (Guild guild : mutualGuilds) {
            if (handleBan(event, guild.getIdLong(), suspiciousCount, sameauthormessages == null || guild.getIdLong() != event.getGuild().getIdLong(), sameauthormessages))
                status = true;
        }
        if (status) {
            if (event.getChannelType() == ChannelType.PRIVATE)
                LogCounter.addSSCounter(event.getAuthor().getId(), event.getMessage().getContentRaw(), "Private"); // add to status counter
            else
                LogCounter.addSSCounter(event.getAuthor().getId(), event.getMessage().getContentRaw(), event.getGuild().getId()); // add to status counter
        }
        return status;
    }

    private static void handleMassPings(MessageReceivedEvent event, List<HandledServerMessageContext> sameauthormessages, int suspiciousCount) {
        if (suspiciousCount == 69420) {
            event.getMessage().reply(event.getAuthor().getName() + " Please do not mass ping users or you will be removed from this server!").queue();
        }
        else {
            handleBan(event, event.getGuild().getIdLong(), sameauthormessages.stream().map(m -> m.messageReceivedEvent.getMessage().getMentions(MentionType.USER).size()).reduce(0, Integer::sum), false, sameauthormessages);
            event.getTextChannel().sendMessage("Sorry all for the ghost ping! The user causing it has been removed from this server.").queue();
        }
    }

    public static boolean handleBan(MessageReceivedEvent event, Long guildID, int suspiciousCount, Boolean cross, List<HandledServerMessageContext> sameauthormessages) {
        boolean status = false;
        try {
            Guild guild = event.getJDA().getGuildById(guildID);
            Member member = guild.getMember(event.getAuthor());
            String sourceName;
            if (event.getChannelType() == ChannelType.PRIVATE)
                sourceName = "DMs";
            else
                sourceName = event.getGuild().getName();
            String usernameWithTag = event.getAuthor().getAsTag();
            String userId = event.getAuthor().getId();
            TextChannel reportChannel = guild.getTextChannelById(CommandManager.mlReportChannels.getOrDefault(guildID, "0"));
            boolean ssBan;
            if (GuildConfigurations.configurations.get(guildID) != null) {
                ssBan = GuildConfigurations.configurations.get(guildID)[GuildConfigurations.ConfigurationMap.SSBAN.ordinal()];
            }
            else {
                ssBan = false;
            }
            ScheduledFuture<?> ssQueued = ssQueuedMap.getOrDefault(guildID, null);
            if (ssQueued != null && !ssQueued.isDone()) // used to prevent multiple bans/kicks if they sent scam many times within the same second, cooldown for four seconds
                    return false;
            System.out.println("Now " + (ssBan ? "Banning " : "Kicking ") + usernameWithTag + " from " + guild.getName());
            EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTimestamp(Instant.now())
                .setFooter("Received " + suspiciousCount + " naughty points.");
            if (cross)
                embedBuilder.setAuthor("Cross " + (ssBan ? "Ban" : "Kick") + " from " + sourceName, null, "https://cdn.discordapp.com/avatars/275759980752273418/05d2f38ca37928426f7c49b191b8b552.webp");
            else
                embedBuilder.setAuthor(ssBan ? "Ban" : "Kick" + " Report", null, "https://cdn.discordapp.com/avatars/275759980752273418/05d2f38ca37928426f7c49b191b8b552.webp");

            if (member != null && !guild.getSelfMember().canInteract(member) && sameauthormessages != null) { //This may fail from DMs b/c of getTextChannel
                embedBuilder.setDescription("Unable to " + (ssBan ? "Ban" : "Kick") + " user **" + usernameWithTag + "** (*" + userId + "*) because they are a higher role than my role");
                if (guild.equals(event.getGuild()) && event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS))
                    event.getTextChannel().sendMessageEmbeds(embedBuilder.build()).queue();
                else
                    guild.getOwner().getUser().openPrivateChannel().flatMap(channel -> channel.sendMessage("Unable to " + (ssBan ? "Ban" : "Kick") + " user **" + usernameWithTag + "** (*" + userId + "*) because they are a higher role than my role")).queue(null, m -> System.out.println("Failed to open dms with guild owner to send SS is higher role then Mine."));
            }
            else if (guild.getSelfMember().hasPermission(Permission.BAN_MEMBERS)) {
                event.getAuthor().openPrivateChannel().flatMap(channel -> channel.sendMessage("You have been automatically been " + (ssBan ? "Banned" : "Kicked") + " from " + guild.getName() +
                    " by Scam Shield. We highly recommend that you change your password immediately.")).queue(null, m -> System.out.println("Failed to open dms with scammer"));
                if (ssBan)
                    member.ban(1).reason("Banned by Lum's Scam Shield").queue();
                else
                    member.ban(1).reason("Kicked by Lum's Scam Shield").queue((s) -> guild.unban(event.getAuthor()).reason("Kicked by Lum's Scam Shield").queue());
                embedBuilder.setDescription("User **" + usernameWithTag + "** (*" + userId + "*) was " + (cross ? "cross " : "") + (ssBan ? "Banned" : "Kicked") + " by the Scam Shield");
                status = true;
            }
            else if (guild.getSelfMember().hasPermission(Permission.KICK_MEMBERS)) {
                event.getAuthor().openPrivateChannel().flatMap(channel -> channel.sendMessage("You have been automatically been Kicked from " + guild.getName() +
                    " by Scam Shield. We highly recommend that you change your password immediately.")).queue(null, m -> System.out.println("Failed to open dms with scammer"));
                member.kick().reason("Kicked by Lum's Scam Shield").queue();
                embedBuilder.setDescription("User **" + usernameWithTag + "** (*" + userId + "*) was Kicked by the Scam Shield");

                if (guild.getSelfMember().hasPermission(Permission.MESSAGE_MANAGE) && sameauthormessages != null) {
                    List<Message> messagelist = new ArrayList<>();
                    sameauthormessages.forEach(m -> {
                        if (m.messageReceivedEvent.getGuild().getSelfMember().hasPermission(m.messageReceivedEvent.getTextChannel(), Permission.VIEW_CHANNEL)) {
                            messagelist.add(m.messageReceivedEvent.getMessage());
                        }
                        else {
                            System.out.println("Lum does not have VIEW_CHANNEL perm in " + m.messageReceivedEvent.getTextChannel().getName());
                            String temp = "";
                            if (!embedBuilder.getDescriptionBuilder().toString().isBlank())
                                temp = embedBuilder.getDescriptionBuilder() + "\n";
                            embedBuilder.setDescription(temp + "Lum failed to remove messages from **" + usernameWithTag + "** (*" + userId + "*) because I don't have view channel perms.");
                        }
                    });
                    System.out.println("Removing " + messagelist.size() + " messages");
                    if (messagelist.size() > 0)
                        messagelist.forEach(m -> m.delete().queue(/*success*/ null, /*failure*/ (f) -> System.out.println("Message failed to be deleted, most likely removed")));
                }
                else if (sameauthormessages != null) {
                    System.out.println("Lum does not have MESSAGE_MANAGE perm");
                    String temp = "";
                    if (!embedBuilder.getDescriptionBuilder().toString().isBlank())
                        temp = embedBuilder.getDescriptionBuilder() + "\n";
                    embedBuilder.setDescription(temp + "Lum failed to remove messages from **" + usernameWithTag + "** (*" + userId + "*) because I don't have manage message perms.");
                }
                status = true;
            }
            else
                embedBuilder.setDescription("Lum failed to " + (ssBan ? "Banned" : "Kicked") + " **" + usernameWithTag + "** (*" + userId + "*) for scam because I don't have " + (ssBan ? "Banned" : "Kicked") + " perms");

            if (reportChannel != null) {
                StringBuilder sb;
                if (sameauthormessages == null) { //came from DMs
                    sb = new StringBuilder(usernameWithTag + " " + userId + " DMed me a likely scam" + (event.getAuthor().getTimeCreated().isAfter(OffsetDateTime.now().minusDays(7)) ? " Additional point added for young account\n" : "\n"));
                    sb.append(event.getMessage().getContentRaw());
                }
                else {
                    sb = new StringBuilder(usernameWithTag + " " + userId + " was " + (ssBan ? "Banned" : "Kicked") + " from " + sourceName + (event.getAuthor().getTimeCreated().isAfter(OffsetDateTime.now().minusDays(7)) ? " Additional point added for young account\n" : "\n"));
                    sameauthormessages.forEach(a -> sb.append("\n").append(a.messageReceivedEvent.getMessage().getContentRaw()).append("\n\n").append(a.suspiciousValue).append(" point").append(a.suspiciousValue > 1 ? "s in " : " in ").append(a.messageReceivedEvent.getChannel().getName()).append("\n"));
                }
                if (guild.getSelfMember().hasPermission(reportChannel, Permission.MESSAGE_EMBED_LINKS))
                    ssQueuedMap.put(guildID, reportChannel.sendMessageEmbeds(embedBuilder.build()).addFile(sb.toString().getBytes(), usernameWithTag + ".txt").queueAfter(4, TimeUnit.SECONDS));
                else
                    ssQueuedMap.put(guildID, reportChannel.sendMessage(embedBuilder.getDescriptionBuilder().toString()).addFile(sb.toString().getBytes(), usernameWithTag + ".txt").queueAfter(4, TimeUnit.SECONDS));
            }
            else if (sameauthormessages != null && event.getGuild() == guild) {
                embedBuilder.getDescriptionBuilder().append("\nTo admins: Use the command `l!setmlreportchannel` to set the report channel.");
                if (guild.getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_EMBED_LINKS))
                    event.getTextChannel().sendMessageEmbeds(embedBuilder.build()).queue();
                else
                    event.getTextChannel().sendMessage(embedBuilder.getDescriptionBuilder().toString()).queue();
            }
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed handleBan in SS", e);
        }
        return status;
    }

    public static boolean checkCrasher(MessageReceivedEvent event) {
        List<Attachment> attachments = event.getMessage().getAttachments();
        for (Attachment attachment : attachments) {
            if (attachment.isVideo()) {
                if (checkForCrasher(attachment))
                    return true;
            }
        }
        for (String url : Utils.extractUrls(event.getMessage().getContentRaw())) {
            if (url.matches(".*(mp4|mkv|wmv|m4v|mov|avi|flv|webm)")) {
                if (checkForCrasher(url))
                    return true;
            }
        }
        return false;
    }
    public static boolean checkForCrasher(Attachment attachment) {
        try {
            File file = attachment.downloadToFile("~/images/testvid/" + attachment.getFileName()).get();
            Process p = Runtime.getRuntime().exec("ffprobe -v error -show_entries frame=width -select_streams v -of csv=p=0 -skip_frame nokey ~/images/testvid/" + attachment.getFileName() + " | uniq");
            p.waitFor();
            file.delete();
            BufferedReader buf = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = "";
            String previousRes = "";
            while ((line = buf.readLine()) != null) {
                if (line.matches(".*[A-Za-z].*"))
                    continue;
                if (previousRes.isBlank())
                    previousRes = line;
                else if (!Objects.equals(line, previousRes))
                    return true;
            }
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed checkForCrasher Attachment", e);
        }
        return false;
    }
    public static boolean checkForCrasher(String url) {
        try {
            String[] parts = url.split(".");
            String fileName = parts[parts.length - 1] + "." + parts[parts.length];
            InputStream in = new URL(url).openStream();
            Files.copy(in, Paths.get("~/images/testvid/" + fileName), StandardCopyOption.REPLACE_EXISTING);
            Process p = Runtime.getRuntime().exec("ffprobe -v error -show_entries frame=width -select_streams v -of csv=p=0 -skip_frame nokey ~/images/testvid/" + fileName + " | uniq");
            p.waitFor();
            new File("~/images/testvid/" + fileName).delete();
            BufferedReader buf = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = "";
            String previousRes = "";
            while ((line = buf.readLine()) != null) {
                if (line.matches(".*[A-Za-z].*"))
                    continue;
                if (previousRes.isBlank())
                    previousRes = line;
                else if (!Objects.equals(line, previousRes))
                    return true;
            }
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed checkForCrasher URL", e);
        }
        return false;
    }
}
