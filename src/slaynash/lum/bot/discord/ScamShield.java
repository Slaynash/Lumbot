package slaynash.lum.bot.discord;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.messages.MessagePoll;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import net.gcardone.junidecode.Junidecode;
import slaynash.lum.bot.DBConnectionManagerLum;
import slaynash.lum.bot.discord.melonscanner.LogCounter;
import slaynash.lum.bot.discord.utils.CrossServerUtils;
import slaynash.lum.bot.utils.ExceptionUtils;
import slaynash.lum.bot.utils.Utils;

public class ScamShield {
    public static final String LOG_IDENTIFIER = "ScamShield";
    private static final int instaKick = 7;
    private static final int instaKickDM = 4;

    private static final ConcurrentLinkedQueue<MessageReceivedEvent> allMessages = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<HandledServerMessageContext> handledMessages = new ConcurrentLinkedQueue<>();

    private static final ConcurrentHashMap<Long, ScheduledFuture<?>> ssQueuedMap = new ConcurrentHashMap<>();
    private static final Map<String, Integer> ssTerms = new HashMap<>() {{ //Keys must be all lowercase and no space, do test term with Junidecode because it can cause some weird results
            put("@everyone", 2);
            put("@here", 2);
            put("money", 1);
            put("loot", 1);
            put("csgo", 1);
            put("cs2", 1);
            put("trade", 1);
            put("knife", 1);
            put("offer", 1);
            put("btc", 1);
            put("bitcoin", 1);
            put("cryptocurrency", 1);
            put("cryptomarket", 2);
            put("$40000", 1);
            put("$100k", 1);
            put("nitro", 1);
            put("1month", 1);
            put("3month", 1);
            put("free", 1);
            put("!!!", 1);
            put("steamgift", 1);
            put("redeemasteam", 1);
            put("giftactivation", 1);
            put("booster", 1);
            put("3dartist", 1);
            put("customavatars", 1);
            put("download", 1);
            put("whatsapp", 2);
            put("telegram", 2);
            put("/t.me/", 2);
            put("askme(how)", 1);
            put("asking(how)", 1);
            put("100%", 1);
            put("yobro", 1);
            put("joinnow", 1);
            put("invite", 1);
            put("betatest", 2);
            put("underage", 1);
            put("family-world", 3);
            put("hotteen", 1);
            put("onlyfans", 1);
            put("checkmybio...", 2);
            put("checkthis", 1);
            put("linkforyou", 1);
            put("joinmyserver", 1);
            put("takeit)", 2);
            put("withdrawyourwinnings", 2);
            put("asubscription!", 2);
            put("10%ofyourprofits", 2);
            put("onlyinterestedpeople", 1);
            put("seeifitsthesameforyou", 1);
            put("itisalreadyrunningout", 2);
            put("pleasetryclaimthisquickly", 2);
            put("youhavebeenrandomlyselected", 2);
            put("gameicreatedtoday)", 2);
            put("hereisthegameitselfhttp", 2);
            put("passwordtest", 2);
            put("joinhttps//discordgg/", 2);
            put("direct-linknet/", 2);
            put("screenshareinhd", 2);
            put("friendhasgiftedyou", 2);
            put("standoutinyourfavoritediscord", 2);
            put("standoutinyourfavoritesdiscord", 2);
            put("ifinterestedsendmeadirectmessage", 2);
        }};
    private static final Map<String, Integer> ssTermsMatches = new HashMap<>() {{
            put(".*invest.*crypto.*", 1);
            put(".*left.*game.*", 2);
            put(".*free.*nitro.*(steam|epic).*", 2);
            put(".*nitro.*free.*(steam|epic).*", 2);
            put(".*(\\djpg.*){4}", 2); // weird MrBeast crypto scam
            put("@everyone(Hey,)?(join(((this|my)(friend's)?server)|now))?(https?//)?(discordgg|(discord(app|watchanimeattheoffice)?(com?|media)))(/invite)?/[\\w-_~$&+\\d]+(joinnow)?", 10);
        }};
    private static final Map<String, Integer> ssTermsPlus = new HashMap<>() {{
            put("ru/", 1);
            put("bitly", 2);
            put("cuttly", 2);
            put("hour", 1);
            put("$", 1);
            put("dollar", 1);
        }};
    private static final List<String> grabifylist = List.of(
            "bmwforum.co", // pulled from https://gist.github.com/M-rcus/9af3207273bf5d30b28c2e3892f1a412
            "catsnthing.com",
            "catsnthings.fun",
            "crabrave.pw",
            "curiouscat.club",
            "datasig.io",
            "datauth.io",
            "dateing.club",
            "discörd.com",
            "disçordapp.com",
            "fortnight.space",
            "fortnitechat.site",
            "freegiftcards.co",
            "gaming-at-my.best",
            "gamingfun.me",
            "grabify.link",
            "headshot.monster",
            "imageshare.best",
            "joinmy.site",
            "leancoding.co",
            "locations.quest",
            "lovebird.guru",
            "minecräft.com",
            "mypic.icu",
            "otherhalf.life",
            "partpicker.shop",
            "progaming.monster",
            "quickmessage.us",
            "screenshare.host",
            "screenshot.best",
            "shrekis.life",
            "sportshub.bar",
            "spottyfly.com",
            "stopify.co",
            "särahah.eu",
            "särahah.pl",
            "trulove.guru",
            "xda-developers.us",
            "yourmy.monster",
            "youshouldclick.us",
            "yoütu.be"
        );

    // must be lowercase
    private static final List<String> badGuildNames = List.of("18+", "nude", "leak", "celeb", "family");

    public static ScamResults ssValue(MessageReceivedEvent event) {
        Map<String, Integer> ssFoundTerms = new HashMap<>();
        String msg = event.getMessage().getContentStripped();
        if (event.isFromGuild()) {
            for (Member member : event.getMessage().getMentions().getMembers()) {
                msg = msg.replace("@" + member.getEffectiveName(), "");
            }
        }
        StringBuilder message = new StringBuilder(msg);
        if (event.getMessage().getPoll() != null) {
            MessagePoll poll = event.getMessage().getPoll();
            message.append(poll.getQuestion());
            poll.getAnswers().forEach(answer -> message.append(answer.getText()));
        }
        for (MessageEmbed embed : event.getMessage().getEmbeds()) {
            if (embed.getTitle() != null)
                message.append(embed.getTitle());
            if (embed.getUrl() != null)
                message.append(embed.getUrl());
        }
        final String finalMessage = Junidecode.unidecode(message.toString()).toLowerCase().replaceAll("[':,. \n\t\\p{Cf}]", "");

        long crossPost = 0;
        if (!event.isFromType(ChannelType.PRIVATE)) {
            Set<String> nameSet = new HashSet<>(); //used to filter one message per channel
            crossPost = allMessages.stream()
                .filter(m -> m.getAuthor().getIdLong() == event.getAuthor().getIdLong())
                .filter(m -> m.getChannel().getIdLong() != event.getChannel().getIdLong() /* Counts all messages in other channels  */)
                .filter(m ->
                        m.getMessage().getAttachments().isEmpty()
                        && Junidecode.unidecode(m.getMessage().getContentDisplay()).equalsIgnoreCase(Junidecode.unidecode(event.getMessage().getContentDisplay()))
                    ||
                        !event.getMessage().getAttachments().isEmpty()
                        && !m.getMessage().getAttachments().isEmpty()
                        && event.getMessage().getAttachments().get(0).getFileName().equalsIgnoreCase(m.getMessage().getAttachments().get(0).getFileName())) //count crossposted files
                .filter(e -> nameSet.add(e.getChannel().getId())) //filter one per channel
                .count();
        }

        if (crossPost > 0) {
            ssFoundTerms.put("Crossposted", (int) Math.round(Math.sqrt(crossPost) * 1.4));
        }

        int spamCount = (int) allMessages.stream()
            .filter(m -> m.getAuthor().getIdLong() == event.getAuthor().getIdLong())
            .filter(m -> m.getChannel().getIdLong() == event.getChannel().getIdLong())
            .filter(m -> Junidecode.unidecode(m.getMessage().getContentDisplay()).equalsIgnoreCase(Junidecode.unidecode(event.getMessage().getContentDisplay())))
            .count();

        if (spamCount > 0) {
            ssFoundTerms.put("Spam", 1);
        }

        if (msg.contains("](")) {
            Pattern p = Pattern.compile("\\[(https?://|)(?<shownDomain>[^]]*?\\.[a-zA-Z]{2,5})(|/.*?)]\\((https?://|)(?<hiddenDomain>.*?\\.\\w{2,5}).*?\\)");
            Matcher m = p.matcher(msg);
            while (m.find()) {
                if (!m.group("shownDomain").equalsIgnoreCase(m.group("hiddenDomain")))
                    ssFoundTerms.put("HiddenEmbed", 4);
            }
        }

        ssFoundTerms.putAll(ssTerms.entrySet().stream().filter(f -> finalMessage.contains(f.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        ssFoundTerms.putAll(ssTermsMatches.entrySet().stream().filter(f -> finalMessage.matches(f.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        if (ssFoundTerms.values().stream().reduce(0, Integer::sum) > 1) {
            ssFoundTerms.putAll(ssTermsPlus.entrySet().stream().filter(f -> finalMessage.contains(f.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        }
        if (!event.getMessage().getInvites().isEmpty()) {
            ssFoundTerms.put("Invite", 1);
            for (String invcode : event.getMessage().getInvites()) {
                try {
                    Invite inv = Invite.resolve(event.getJDA(), invcode).complete();
                    if (inv.getGuild() == null || inv.getGuild().getName() == null)
                        continue;
                    if (badGuildNames.stream().anyMatch(s -> inv.getGuild().getName().toLowerCase().contains(s))) {
                        ssFoundTerms.put("InviteBadGuild", 2);
                    }
                }
                catch (Exception ignored) { }
            }
        }
        int tempval = ssFoundTerms.values().stream().reduce(0, Integer::sum);
        if (tempval > 0 && tempval < instaKick) {
            final int domainAge = domainAgeCheck(event.getMessage().getContentStripped());
            if (domainAge > 0)
                ssFoundTerms.put("domainAge", domainAge * 3);
        }

        for (String url : Utils.extractUrls(message.toString())) {
            do {
                url = getRedirect(url);
            } while (url != null && !url.equals("GrabifyLink"));
            if (url != null && url.equals("GrabifyLink")) {
                ssFoundTerms.put("GrabifyLink", instaKick + 1);
            }
        }
        if (ssFoundTerms.values().stream().reduce(0, Integer::sum) > 1) {
            if (event.getMessage().getInvites().size() > 1)
                ssFoundTerms.put("Discord Invite", 1);
        }

        int suspiciousValue = ssFoundTerms.values().stream().reduce(0, Integer::sum);

        if (suspiciousValue > 0) {
            System.out.println("Scam Shield points for this message: " + suspiciousValue + (crossPost > 0 ? " Crossposted " : " ") + ssFoundTerms);
            System.out.println("Final message: " + finalMessage);
        }
        boolean massPing = event.getMessage().getMentions().getUsers().size() > 3; //kick mass ping selfbots
        if (!event.getMessage().getMentions().mentionsEveryone() && (event.getMessage().getContentRaw().contains("@everyone") || event.getMessage().getContentRaw().contains("@here"))) {
            massPing = true;
        }

        return new ScamResults(suspiciousValue, ssFoundTerms, massPing);
    }

    public static boolean checkForFishing(MessageUpdateEvent event) {
        return checkForFishing(new MessageReceivedEvent(event.getJDA(), event.getResponseNumber(), event.getMessage()));
    }

    public static boolean checkForFishing(MessageReceivedEvent event) {
        if (event.getMember() == null) {
            System.out.println("Member is null, skipping SS");
            return false;
        }
        if (event.getAuthor().isBot())
            return false;
        if (CrossServerUtils.checkIfStaff(event))
            return false;
        if (event.getMessage().getContentRaw().length() < 2)
            return false;

        long guildID = event.getGuild().getIdLong();
        GuildConfiguration guildconfig = DBConnectionManagerLum.getGuildConfig(guildID);
        if (guildconfig != null && !guildconfig.ScamShield()) {
            return false;
        }
        allMessages.removeIf(m -> event.getMessageIdLong() == m.getMessageIdLong()); //remove original message if edited, needs to be before ssValue

        ScamResults suspiciousResults = ssValue(event);

        allMessages.add(event);
        if (suspiciousResults.suspiciousValue == 0)
            return false;

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        allMessages.removeIf(m -> m.getMessage().getTimeCreated().toLocalDateTime().until(now, ChronoUnit.MINUTES) > 6); //remove saved messages for crosspost checks
        handledMessages.removeIf(m -> m.creationTime.until(now, ChronoUnit.MINUTES) > 3); //remove all saved messages that is older than 3 minutes
        handledMessages.removeIf(m -> event.getMessageIdLong() == m.messageReceivedEvent.getMessageIdLong()); //remove original message if edited

        suspiciousResults.calulatedValue = suspiciousResults.suspiciousValue;
        if (suspiciousResults.calulatedValue < 3 && suspiciousResults.calulatedValue > 0)
            suspiciousResults.calulatedValue--;
        else if (suspiciousResults.calulatedValue >= 3 && suspiciousResults.calulatedValue < instaKick) //if one message gets instaKick+ then it is an instant kick on first message
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
        else if (suspiciousResults.totalSuspicionCount > 6) {
            System.out.println("suspiciousResults.sameauthormessages.size: " + suspiciousResults.sameauthormessages.size());
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
            event.getJDA().getTextChannelById(896839871543525417L).sendMessage("DM from " + event.getAuthor().getEffectiveName() + " " + event.getAuthor().getId() + " gotten " + suspiciousResults.suspiciousValue + " sus points\nMutual Servers: "
                + CrossServerUtils.getMutualGuilds(event.getAuthor()).stream().map(Guild::getName).toList() + "\n" + suspiciousResults.ssFoundTerms + "\n\n" + message).queue();
        if (suspiciousResults.suspiciousValue >= instaKickDM)
            return handleCrossBan(event, suspiciousResults);
        else
            return false;
    }

    private static boolean handleCrossBan(MessageReceivedEvent event, ScamResults suspiciousResults) {
        System.out.println(CrossServerUtils.getMutualGuilds(event.getAuthor()).stream().map(Guild::getName).collect(Collectors.toList()));
        if (event.getAuthor().getIdLong() == 761335833307119658L) return false; //please don't cross ban my alt account, it is annoying to rejoin after every test
        List<Guild> mutualGuilds = new ArrayList<>(CrossServerUtils.getMutualGuilds(event.getAuthor()));
        mutualGuilds.removeIf(g -> {
            if (suspiciousResults.sameauthormessages != null && g == event.getGuild())
                return false;  // Do not remove current guild regardless of cross ban setting
            GuildConfiguration guildconfig = DBConnectionManagerLum.getGuildConfig(g.getIdLong());
            if (guildconfig != null) {
                if (event.getChannelType() == ChannelType.PRIVATE)
                    return !guildconfig.ScamShieldDm();
                else
                    return !guildconfig.ScamShieldCross();
            }
            return true;
        });
        boolean status = false;
        for (Guild guild : mutualGuilds) {
            if (handleBan(event, guild.getIdLong(), suspiciousResults))
                status = true;
        }
        if (status) {
            String id;
            if (event.getChannelType() == ChannelType.PRIVATE) id = "Private";
            else id = event.getGuild().getId();

            LogCounter.addSSCounter(event.getAuthor().getId(), event.getMessage().getContentRaw(), id); // add to status counter
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
        }
    }

    public static boolean handleBan(MessageReceivedEvent event, long guildID, ScamResults suspiciousResults) {
        boolean status = false;
        try {
            Guild guild = event.getJDA().getGuildById(guildID);
            assert guild != null;
            Member member = guild.getMember(event.getAuthor());
            if (member == null) {
                System.out.println(event.getAuthor().getEffectiveName() + " is no longer in " + guild.getName());
                return false;
            }
            String sourceName;
            boolean cross;
            boolean dm = false;
            if (event.getChannelType() == ChannelType.PRIVATE) {
                sourceName = "DMs";
                cross = true;
                dm = true;
            }
            else {
                sourceName = event.getGuild().getName();
                cross = !sourceName.equals(guild.getName());
            }
            String usernameWithTag = event.getAuthor().getEffectiveName();
            String userId = event.getAuthor().getAsMention();
            MessageChannelUnion reportChannel = CommandManager.getModReportChannels(guild, "scam");
            System.out.println("Report channel: " + guildID + " " + reportChannel);
            boolean ssBan;
            GuildConfiguration guildconfig = DBConnectionManagerLum.getGuildConfig(guildID);
            if (guildconfig != null) {
                ssBan = guildconfig.ScamShieldBan();
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

            if (suspiciousResults.sameauthormessages != null && !suspiciousResults.sameauthormessages.isEmpty())
                suspiciousResults.sameauthormessages.stream().filter(m -> m.messageReceivedEvent.getGuild().getSelfMember().hasPermission(m.messageReceivedEvent.getChannel().asGuildMessageChannel(), Permission.MESSAGE_MANAGE)).forEach(m -> m.messageReceivedEvent.getMessage().delete().reason("Lum's Scam Shield").queue());

            if (!guild.getSelfMember().canInteract(member)) {
                embedBuilder.setDescription("Unable to " + (ssBan ? "Ban" : "Kick") + " user **" + usernameWithTag + "** (*" + userId + "*) because they are a higher role than my role");
                if (!dm && guild.equals(event.getGuild()) && event.getGuild().getSelfMember().hasPermission(event.getChannel().asGuildMessageChannel(), Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS))
                    event.getChannel().asGuildMessageChannel().sendMessageEmbeds(embedBuilder.build()).queue();
                else if (guild.getOwner() != null)
                    guild.getOwner().getUser().openPrivateChannel().flatMap(channel -> channel.sendMessageEmbeds(embedBuilder.build())).queue(null, m -> System.out.println("Failed to open dms with guild owner to send SS is higher role then Mine."));
            }
            else if (guild.getSelfMember().hasPermission(Permission.BAN_MEMBERS)) {
                event.getAuthor().openPrivateChannel().flatMap(channel -> channel.sendMessage("You have been automatically been " + (ssBan ? "Banned" : "Kicked") + " from " + guild.getName() +
                    " by Scam Shield. We highly recommend that you change your password immediately.")).queue(null, m -> System.out.println("Failed to open dms with scammer"));
                if (ssBan)
                    member.ban(1, TimeUnit.DAYS).reason("Banned by Lum's Scam Shield").queue();
                else
                    member.ban(1, TimeUnit.DAYS).reason("Kicked by Lum's Scam Shield").queue(s -> guild.unban(event.getAuthor()).reason("Kicked by Lum's Scam Shield").queue());
                embedBuilder.setDescription("User **" + usernameWithTag + "** (*" + userId + "*) was " + (cross ? "cross " : "") + (ssBan ? "Banned" : "Kicked") + " by the Scam Shield");
                status = true;
            }
            else if (guild.getSelfMember().hasPermission(Permission.KICK_MEMBERS)) {
                event.getAuthor().openPrivateChannel().flatMap(channel -> channel.sendMessage("You have been automatically been Kicked from " + guild.getName() +
                    " by Scam Shield. We highly recommend that you change your password immediately.")).queue(null, m -> System.out.println("Failed to open dms with scammer"));
                member.kick().reason("Kicked by Lum's Scam Shield").queue();
                embedBuilder.setDescription("User **" + usernameWithTag + "** (*" + userId + "*) was Kicked by the Scam Shield");

                if (guild.getSelfMember().hasPermission(Permission.MESSAGE_MANAGE) && !dm) { // there are no messages in guild if scam came from dm
                    List<Message> messagelist = new ArrayList<>();
                    suspiciousResults.sameauthormessages.forEach(m -> {
                        if (m.messageReceivedEvent.getGuild().getSelfMember().hasPermission(m.messageReceivedEvent.getChannel().asGuildMessageChannel(), Permission.VIEW_CHANNEL, Permission.MESSAGE_MANAGE)) {
                            messagelist.add(m.messageReceivedEvent.getMessage());
                        }
                        else if (!m.messageReceivedEvent.getGuild().getSelfMember().hasPermission(m.messageReceivedEvent.getChannel().asGuildMessageChannel(), Permission.MESSAGE_MANAGE)) {
                            System.out.println("Lum does not have MESSAGE_MANAGE perm in " + m.messageReceivedEvent.getChannel().getName());
                            String temp = "";
                            if (!embedBuilder.getDescriptionBuilder().toString().isBlank())
                                temp = embedBuilder.getDescriptionBuilder() + "\n";
                            embedBuilder.setDescription(temp + "Lum failed to remove messages from **" + usernameWithTag + "** (*" + userId + "*) because I don't have manage message perms for the channel " + m.messageReceivedEvent.getChannel().getName());
                        }
                        else {
                            System.out.println("Lum does not have VIEW_CHANNEL perm in " + m.messageReceivedEvent.getChannel().getName());
                            String temp = "";
                            if (!embedBuilder.getDescriptionBuilder().toString().isBlank())
                                temp = embedBuilder.getDescriptionBuilder() + "\n";
                            embedBuilder.setDescription(temp + "Lum failed to remove messages from **" + usernameWithTag + "** (*" + userId + "*) because I don't have view channel perms.");
                        }
                    });
                    System.out.println("Removing " + messagelist.size() + " messages");
                    messagelist.forEach(m -> m.delete().queue(/*success*/ null, /*failure*/ f -> System.out.println("Message failed to be deleted, most likely removed")));
                }
                else if (!dm) {
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
                if (dm) {
                    sb = new StringBuilder(usernameWithTag + " " + userId + " DMed me a likely scam\n");
                    sb.append(event.getMessage().getContentRaw());
                    sb.append("\n").append(suspiciousResults.ssFoundTerms);
                }
                else {
                    sb = new StringBuilder(usernameWithTag + " " + userId + " was " + (ssBan ? "Banned" : "Kicked") + " from " + sourceName);
                    suspiciousResults.sameauthormessages.forEach(a -> sb
                        .append("\n")
                        .append(Junidecode.unidecode(a.messageReceivedEvent.getMessage().getContentRaw()))
                        .append("\n")
                        .append(!a.messageReceivedEvent.getMessage().getAttachments().isEmpty() ? a.messageReceivedEvent.getMessage().getAttachments().get(0).getUrl() : "")
                        .append("\n")
                        .append(a.suspiciousResults.suspiciousValue)
                        .append(" point")
                        .append(a.suspiciousResults.suspiciousValue > 1 ? "s in " : " in ")
                        .append(a.messageReceivedEvent.getChannel().getName())
                        .append(" for ")
                        .append(a.suspiciousResults.ssFoundTerms)
                        .append("\n"));
                }
                if (guild.getSelfMember().hasPermission(reportChannel.asGuildMessageChannel(), Permission.MESSAGE_EMBED_LINKS))
                    ssQueuedMap.put(guildID, reportChannel.sendMessageEmbeds(embedBuilder.build()).addFiles(FileUpload.fromData(sb.toString().getBytes(), usernameWithTag + ".txt")).queueAfter(4, TimeUnit.SECONDS));
                else if (guild.getSelfMember().hasPermission(reportChannel.asGuildMessageChannel(), Permission.MESSAGE_SEND))
                    ssQueuedMap.put(guildID, reportChannel.sendMessage(embedBuilder.getDescriptionBuilder().toString()).addFiles(FileUpload.fromData(sb.toString().getBytes(), usernameWithTag + ".txt")).queueAfter(4, TimeUnit.SECONDS));
            }
            else if (!dm && event.getGuild() == guild) {
                embedBuilder.getDescriptionBuilder().append("\nTo admins: Use the command `/log` to set the report channel.");
                if (guild.getSelfMember().hasPermission(event.getChannel().asGuildMessageChannel(), Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_SEND))
                    event.getChannel().asGuildMessageChannel().sendMessageEmbeds(embedBuilder.build()).queue();
                else if (guild.getSelfMember().hasPermission(event.getChannel().asGuildMessageChannel(), Permission.MESSAGE_SEND))
                    event.getChannel().asGuildMessageChannel().sendMessage(embedBuilder.getDescriptionBuilder().toString()).queue();
            }
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed handleBan in SS", e);
        }
        return status;
    }

    private static final List<String> whitelist = List.of(
        "youtu.be",
        "discord.gg",
        ".jp",
        ".edu",
        "telegra.ph",
        "robydrinks.be",
        "bxlblog.be",
        "meap.gg",
        "bit.ly",
        "github.io",
        "twitch.tv",
        "aka.ms",
        "com.tw",
        "u.to",
        "gg.gg",
        "fairyte.ch",
        "rb.gy",
        "surl.li"
    );

    private static int domainAgeCheck(String message) {
        int age = 14; //in days, less then but not equal will count
        int count = 0;
        try {
            List<String> urls = Utils.extractUrls(message);

            for (String url : urls) {
                url = url.replace("<@", " ").replace("\\", "/");
                if (URI.create(url).getHost() == null)
                    continue;
                String domain = URI.create(url).getHost().replaceAll("[^a-zA-Z\\d-._~/?#:%]", "");
                while (domain.split("\\.").length > 2)
                    domain = domain.split("\\.", 2)[1]; //remove all subdomains
                String tld = domain.split("\\.", 2)[1];
                if (Character.isDigit(tld.charAt(0)))
                    continue;
                String domainLower = domain.toLowerCase();
                if (whitelist.stream().anyMatch(domainLower::contains))
                    continue;
                Process p = Runtime.getRuntime().exec("whois " + domain);
                StringBuilder output = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                String whois = output.toString().replace("+0000", "Z");
                if (whois.startsWith("No match for domain"))
                    continue;
                whois = whois.replaceFirst("(?m)Last update of WHOIS.*$", ""); //for no matches eg. twitch.tv
                ArrayList<ZonedDateTime> list = new ArrayList<>();
                Matcher matcher = Pattern.compile(" [\\d-]+T[\\d-:.]+(Z|\\+[\\d:]+)").matcher(whois);
                while (matcher.find()) {
                    DateTimeFormatter f = DateTimeFormatter.ISO_DATE_TIME;
                    ZonedDateTime parsedDate = ZonedDateTime.parse(matcher.group().strip(), f);
                    list.add(parsedDate);
                }
                Matcher matcher2 = Pattern.compile(" \\d{2}-[\\da-zA-Z]{3}-\\d{4}").matcher(whois.replace(".", "-"));
                while (matcher2.find()) {
                    DateTimeFormatter f = DateTimeFormatter.ofPattern("dd-[MM][MMM]-yyyy"); //EU standard date example is uillinois.edu
                    LocalDate date = LocalDate.parse(matcher2.group().strip(), f);
                    ZonedDateTime parsedDate = date.atStartOfDay(ZoneId.systemDefault());
                    list.add(parsedDate);
                }
                Matcher matcher3 = Pattern.compile(" \\d{4}-?\\d{2}-?\\d{2} \\d{2}:\\d{2}:\\d{2}").matcher(whois);
                while (matcher3.find()) {
                    DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss").withZone(ZoneId.systemDefault()); //example is chng.it and pimaker.at
                    ZonedDateTime parsedDate = ZonedDateTime.parse(matcher3.group().strip().replace("-", ""), f);
                    list.add(parsedDate);
                }
                if (list.isEmpty())
                    throw new Exception("Can not find any dates for " + domain + "\n" + whois);
                ZonedDateTime mindate = Collections.min(list);
                if (mindate.isAfter(ZonedDateTime.now().minusDays(age)))
                    count++;
            }
        }
        catch (Exception e) {
            JDAManager.getJDA().getTextChannelById(927044970278453300L).sendMessageEmbeds(Utils.wrapMessageInEmbed("Failed to check domain age\n" + e.getMessage(), Color.RED)).queue();
            e.printStackTrace();
        }
        return count;
    }

    public static void checkDeleted(MessageDeleteEvent event) { //doesn't do BulkDelete
        long mID = event.getMessageIdLong();
        handledMessages.forEach(m -> {
            if (m.messageReceivedEvent.getMessage().getIdLong() == mID) {
                m.suspiciousResults.ssFoundTerms.put("Deleted Message", -1);
                m.suspiciousResults.suspiciousValue = m.suspiciousResults.ssFoundTerms.values().stream().reduce(0, Integer::sum);
            }
        });
    }

    public static class ScamResults {
        public ScamResults(int suspiciousValue, Map<String, Integer> ssFoundTerms, boolean massPing) {
            this.ssFoundTerms = ssFoundTerms;
            this.suspiciousValue = suspiciousValue;
            this.massPing = massPing;
        }
        public int suspiciousValue;
        public int calulatedValue;
        public int totalSuspicionCount;
        public final Map<String, Integer> ssFoundTerms;
        public final boolean massPing;
        public List<HandledServerMessageContext> sameauthormessages;
    }

    private static String getRedirect(String url) {
        try {
            if (grabifylist.stream().anyMatch(url.toLowerCase()::contains)) {
                return "GrabifyLink";
            }
            HttpURLConnection con = (HttpURLConnection) (new URL(url).openConnection());
            con.setInstanceFollowRedirects(false);
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            return con.getHeaderField("Location"); // null if no redirect
        }
        catch (Exception e) {
            return null;
        }
    }
}
