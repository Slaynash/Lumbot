package slaynash.lum.bot.discord;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.gcardone.junidecode.Junidecode;
import slaynash.lum.bot.discord.melonscanner.LogCounter;
import slaynash.lum.bot.discord.utils.CrossServerUtils;
import slaynash.lum.bot.utils.ExceptionUtils;
import slaynash.lum.bot.utils.Utils;
import slaynash.lum.bot.utils.Whois;

public class ScamShield {
    public static final String LOG_IDENTIFIER = "ScamShield";

    private static final ConcurrentLinkedQueue<MessageReceivedEvent> allMessages = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<HandledServerMessageContext> handledMessages = new ConcurrentLinkedQueue<>();

    private static final ConcurrentHashMap<Long, ScheduledFuture<?>> ssQueuedMap = new ConcurrentHashMap<>();
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
            put(".*nitro.*free.*(steam|epic).*", 2);
        }};
    private static final Map<String, Integer> ssTermsPlus = new HashMap<>() {{
            put("http", 1);
            put(".ru/", 1);
            put("bit.ly", 2);
            put("cutt.ly", 2);
            put("mega.nz", 2);
            put("hour", 1);
            put("$", 1);
            put("discord", 1);
        }};

    public static ScamResults ssValue(MessageReceivedEvent event) {
        // I found a simple referral and you can loot skins there\nhttp://csgocyber.ru/simlpebonus\nIf it's not difficult you can then throw me a trade and I'll give you the money
        //@everyone Hello I am leaving CS:GO and giving away my skins to people who send trade offers. For first people I will give away my 3 knifes. Don't be greedy and take few skins :  https://streancommunuty.ru/tradoffer/new/?partner=1284276379&token=iMDdLkoe

        Map<String, Integer> ssFoundTerms = new HashMap<>();
        if (event.getAuthor().getTimeCreated().isAfter(OffsetDateTime.now().minusDays(7))) //add sus points if account is less than 7 days old
            ssFoundTerms.put("newAccount", 1);
        StringBuilder message = new StringBuilder(Junidecode.unidecode(event.getMessage().getContentStripped()));
        for (MessageEmbed embed : event.getMessage().getEmbeds()) {
            message.append(embed.getTitle()).append(embed.getDescription());
        }
        message = new StringBuilder(message.toString().toLowerCase().replaceAll("[':,. \n\t]", ""));

        long crossPost = 0;
        if (!event.isFromType(ChannelType.PRIVATE)) {
            Set<String> nameSet = new HashSet<>(); //used to filter one message per channel
            crossPost = allMessages.stream()
                .filter(m -> m.getAuthor().getIdLong() == event.getAuthor().getIdLong())
                .filter(m -> m.getChannel().getIdLong() != event.getChannel().getIdLong() /* Counts all messages in other channels  */)
                .filter(m ->
                        m.getMessage().getAttachments().size() == 0
                        && m.getMessage().getContentDisplay().equalsIgnoreCase(event.getMessage().getContentDisplay())
                        && ssTerms.keySet().stream().anyMatch(s -> m.getMessage().getContentDisplay().toLowerCase().contains(s))
                    ||
                        event.getMessage().getAttachments().size() > 0
                        && m.getMessage().getAttachments().size() > 0
                        && event.getMessage().getAttachments().get(0).getFileName().equalsIgnoreCase(m.getMessage().getAttachments().get(0).getFileName())) //count crossposted files
                .filter(e -> nameSet.add(e.getChannel().getId())) //filter one per channel
                .count();
        }

        if (crossPost > 0) {
            ssFoundTerms.put("Crossposted", (int) crossPost);
        }

        final String finalMessage = message.toString();
        ssFoundTerms.putAll(ssTerms.entrySet().stream().filter(f -> finalMessage.contains(f.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        ssFoundTerms.putAll(ssTermsMatches.entrySet().stream().filter(f -> finalMessage.matches(f.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        final int domainAge = domainAgeCheck(event.getMessage().getContentStripped());
        if (domainAge > 0) {
            ssFoundTerms.put("domainAge", domainAge * 2);
        }

        if (ssFoundTerms.values().stream().reduce(0, Integer::sum) > 1) {
            ssFoundTerms.putAll(ssTermsPlus.entrySet().stream().filter(f -> finalMessage.contains(f.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        }

        int suspiciousValue = ssFoundTerms.values().stream().reduce(0, Integer::sum);

        if (suspiciousValue > 0) {
            System.out.println("Scam Shield points for this message: " + suspiciousValue + (crossPost > 0 ? " Crossposted " : " ") + ssFoundTerms);
        }
        boolean massPing = event.getMessage().getMentions(MentionType.USER).size() > 3; //kick mass ping selfbots

        return new ScamResults(suspiciousValue, ssFoundTerms, massPing);
    }

    public static boolean checkForFishing(MessageReceivedEvent event) {
        if (event.getMember() == null) {
            System.out.println("Member is null, skipping SS");
            return false;
        }
        if (CrossServerUtils.checkIfStaff(event))
            return false;

        long guildID = event.getGuild().getIdLong();
        ScamResults suspiciousResults = ssValue(event);

        if (suspiciousResults.suspiciousValue == 0)
            return false;

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        allMessages.removeIf(m -> m.getMessage().getTimeCreated().toLocalDateTime().until(now, ChronoUnit.MINUTES) > 3); //remove saved messages for crosspost checks
        handledMessages.removeIf(m -> m.creationTime.until(now, ChronoUnit.MINUTES) > 3); //remove all saved messages that is older than 3 minutes
        handledMessages.removeIf(m -> event.getMessageIdLong() == m.messageReceivedEvent.getMessageIdLong()); //remove original message if edited
        allMessages.add(event);

        suspiciousResults.calulatedValue = suspiciousResults.suspiciousValue;
        if (suspiciousResults.calulatedValue < 3)
            suspiciousResults.calulatedValue = 0;
        if (suspiciousResults.calulatedValue > 3 && suspiciousResults.calulatedValue <= 7) //if one message gets 8+ then it is an instant kick on first message
            suspiciousResults.calulatedValue = 3;
        handledMessages.add(new HandledServerMessageContext(event, suspiciousResults, guildID)); // saves a copy of message and point, should avoid false-positives, force 2 messages

        suspiciousResults.sameauthormessages = handledMessages.stream()
            .filter(m -> m.messageReceivedEvent.getMember().getIdLong() == event.getMember().getIdLong() && m.guildId == guildID)
            .collect(Collectors.toList());

        suspiciousResults.totalSuspicionCount = suspiciousResults.sameauthormessages.stream().map(m -> m.suspiciousResults.calulatedValue).reduce(0, Integer::sum); //this adds all points that one user collected

        if (suspiciousResults.massPing) {
            handleMassPings(event, suspiciousResults);
            return true;
        }
        else if (suspiciousResults.totalSuspicionCount > 5) {
            handleCrossBan(event, suspiciousResults);
            return true;
        }

        return false;
    }

    public static boolean checkForFishingPrivate(MessageReceivedEvent event) {
        String message = event.getMessage().getContentDisplay().toLowerCase();

        if (CrossServerUtils.checkIfStaff(event))
            return false;

        ScamResults suspiciousResults = ssValue(event);
        suspiciousResults.totalSuspicionCount = suspiciousResults.calulatedValue = suspiciousResults.suspiciousValue;
        if (suspiciousResults.suspiciousValue > 0)
            event.getJDA().getGuildById(633588473433030666L).getTextChannelById(896839871543525417L).sendMessage("DM from " + event.getAuthor().getAsTag() + " " + event.getAuthor().getId() + " gotten " + suspiciousResults.suspiciousValue + " sus points\nMutual Servers: "
                + event.getAuthor().getMutualGuilds().stream().map(Guild::getName).collect(Collectors.toList()) + "\n\n" + message).queue();
        if (suspiciousResults.suspiciousValue <= 3)
            return false;

        return handleCrossBan(event, suspiciousResults);
    }

    private static boolean handleCrossBan(MessageReceivedEvent event, ScamResults suspiciousResults) {
        System.out.println(event.getAuthor().getMutualGuilds().stream().map(Guild::getName).collect(Collectors.toList()));
        List<Guild> mutualGuilds = new ArrayList<>(event.getAuthor().getMutualGuilds());
        mutualGuilds.removeIf(g -> {
            if (suspiciousResults.sameauthormessages != null && g == event.getGuild())
                return false;
            if (GuildConfigurations.configurations.get(g.getIdLong()) != null)
                return !GuildConfigurations.configurations.get(g.getIdLong())[GuildConfigurations.ConfigurationMap.SSCROSS.ordinal()];
            else
                return true;
        });
        boolean status = false;
        for (Guild guild : mutualGuilds) {
            if (handleBan(event, guild.getIdLong(), suspiciousResults))
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

    private static void handleMassPings(MessageReceivedEvent event, ScamResults suspiciousResults) {
        int massPingCount = (int) handledMessages.stream()
            .filter(e -> e.suspiciousResults.massPing)
            .filter(e -> e.messageReceivedEvent.getGuild().getIdLong() == event.getGuild().getIdLong())
            .filter(e -> e.messageReceivedEvent.getAuthor().getIdLong() == event.getAuthor().getIdLong()).count();
        if (massPingCount <= 1 && suspiciousResults.massPing) {
            event.getMessage().reply(event.getAuthor().getName() + " Please do not mass ping users or you will be removed from this server!").queue();
        }
        else if (suspiciousResults.massPing) {
            handleBan(event, event.getGuild().getIdLong(), suspiciousResults);
            event.getTextChannel().sendMessage("Sorry all for the ghost ping! The user causing it has been removed from this server.").queue();
        }
    }

    public static boolean handleBan(MessageReceivedEvent event, long guildID, ScamResults suspiciousResults) {
        boolean status = false;
        try {
            Guild guild = event.getJDA().getGuildById(guildID);
            Member member = guild.getMember(event.getAuthor());
            String sourceName;
            boolean cross;
            if (event.getChannelType() == ChannelType.PRIVATE) {
                sourceName = "DMs";
                cross = true;
            }
            else {
                sourceName = event.getGuild().getName();
                cross = !sourceName.equals(guild.getName());
            }
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
                .setFooter("Received " + suspiciousResults.totalSuspicionCount + " naughty points.");
            if (cross)
                embedBuilder.setAuthor("Cross " + (ssBan ? "Ban" : "Kick") + " from " + sourceName, null, "https://cdn.discordapp.com/avatars/275759980752273418/05d2f38ca37928426f7c49b191b8b552.webp");
            else
                embedBuilder.setAuthor(ssBan ? "Ban" : "Kick" + " Report", null, "https://cdn.discordapp.com/avatars/275759980752273418/05d2f38ca37928426f7c49b191b8b552.webp");

            if (member != null && !guild.getSelfMember().canInteract(member) && suspiciousResults.sameauthormessages != null) { //This may fail from DMs b/c of getTextChannel
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
                    member.ban(1).reason("Kicked by Lum's Scam Shield").queue(s -> guild.unban(event.getAuthor()).reason("Kicked by Lum's Scam Shield").queue());
                embedBuilder.setDescription("User **" + usernameWithTag + "** (*" + userId + "*) was " + (cross ? "cross " : "") + (ssBan ? "Banned" : "Kicked") + " by the Scam Shield");
                status = true;
            }
            else if (guild.getSelfMember().hasPermission(Permission.KICK_MEMBERS)) {
                event.getAuthor().openPrivateChannel().flatMap(channel -> channel.sendMessage("You have been automatically been Kicked from " + guild.getName() +
                    " by Scam Shield. We highly recommend that you change your password immediately.")).queue(null, m -> System.out.println("Failed to open dms with scammer"));
                member.kick().reason("Kicked by Lum's Scam Shield").queue();
                embedBuilder.setDescription("User **" + usernameWithTag + "** (*" + userId + "*) was Kicked by the Scam Shield");

                if (guild.getSelfMember().hasPermission(Permission.MESSAGE_MANAGE) && suspiciousResults.sameauthormessages != null) {
                    List<Message> messagelist = new ArrayList<>();
                    suspiciousResults.sameauthormessages.forEach(m -> {
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
                        messagelist.forEach(m -> m.delete().queue(/*success*/ null, /*failure*/ f -> System.out.println("Message failed to be deleted, most likely removed")));
                }
                else if (suspiciousResults.sameauthormessages != null) {
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
                if (suspiciousResults.sameauthormessages == null) { //came from DMs
                    sb = new StringBuilder(usernameWithTag + " " + userId + " DMed me a likely scam\n");
                    sb.append(event.getMessage().getContentRaw());
                    sb.append("\n").append(suspiciousResults.ssFoundTerms);
                }
                else {
                    sb = new StringBuilder(usernameWithTag + " " + userId + " was " + (ssBan ? "Banned" : "Kicked") + " from " + sourceName);
                    suspiciousResults.sameauthormessages.forEach(a -> sb.append("\n").append(a.messageReceivedEvent.getMessage().getContentRaw()).append("\n\n").append(a.suspiciousResults.suspiciousValue).append(" point").append(a.suspiciousResults.suspiciousValue > 1 ? "s in " : " in ").append(a.messageReceivedEvent.getChannel().getName()).append(" for ").append(a.suspiciousResults.ssFoundTerms).append("\n"));
                }
                if (guild.getSelfMember().hasPermission(reportChannel, Permission.MESSAGE_EMBED_LINKS))
                    ssQueuedMap.put(guildID, reportChannel.sendMessageEmbeds(embedBuilder.build()).addFile(sb.toString().getBytes(), usernameWithTag + ".txt").queueAfter(4, TimeUnit.SECONDS));
                else
                    ssQueuedMap.put(guildID, reportChannel.sendMessage(embedBuilder.getDescriptionBuilder().toString()).addFile(sb.toString().getBytes(), usernameWithTag + ".txt").queueAfter(4, TimeUnit.SECONDS));
            }
            else if (suspiciousResults.sameauthormessages != null && event.getGuild() == guild) {
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

    private static int domainAgeCheck(String message) {
        int count = 0;
        try {
            List<String> urls = Utils.extractUrls(message);

            for (String url : urls) {
                String domain = URI.create(url).getHost();
                while (domain.split("\\.").length > 2)
                    domain = domain.split("\\.", 2)[1]; //remove all subdomains

                String whois = Whois.whois(domain);
                Matcher matcher = Pattern.compile(" [0-9T\\-:\\.]+Z").matcher(whois);
                ArrayList<ZonedDateTime> list = new ArrayList<>();
                DateTimeFormatter f = DateTimeFormatter.ISO_DATE_TIME;
                while (matcher.find()) {
                    ZonedDateTime parsedDate = ZonedDateTime.parse(matcher.group().strip(), f);
                    list.add(parsedDate);
                }
                ZonedDateTime mindate = Collections.min(list);
                if (mindate.isAfter(ZonedDateTime.now().minusDays(7)))
                    count++;
            }
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to check domain age", e);
        }
        return count;
    }

    public static class ScamResults {
        public ScamResults(int suspiciousValue, Map<String, Integer> ssFoundTerms, boolean massPing) {
            this.ssFoundTerms = ssFoundTerms;
            this.suspiciousValue = suspiciousValue;
            this.massPing = massPing;
        }
        public final int suspiciousValue;
        public int calulatedValue;
        public int totalSuspicionCount;
        public final Map<String, Integer> ssFoundTerms;
        public final boolean massPing;
        public List<HandledServerMessageContext> sameauthormessages;
    }
}
