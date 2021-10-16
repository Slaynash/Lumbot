package slaynash.lum.bot.discord;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
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
import slaynash.lum.bot.discord.melonscanner.LogCounter;

public class ScamShield {
    public static final String LOG_IDENTIFIER = "ScamShield";

    private static final Queue<MessageReceivedEvent> allMessages = new LinkedList<>();
    private static final Queue<HandledServerMessageContext> handledMessages = new LinkedList<>();

    private static ScheduledFuture<?> ssQueued;

    public static int ssValue(MessageReceivedEvent event) {
        // I found a simple referral and you can loot skins there\nhttp://csgocyber.ru/simlpebonus\nIf it's not difficult you can then throw me a trade and I'll give you the money
        //@everyone Hello I am leaving CS:GO and giving away my skins to people who send trade offers. For first people I will give away my 3 knifes. Don't be greedy and take few skins :  https://streancommunuty.ru/tradoffer/new/?partner=1284276379&token=iMDdLkoe

        String message = Normalizer.normalize(event.getMessage().getContentStripped(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        boolean newAccount = event.getAuthor().getTimeCreated().isAfter(OffsetDateTime.now().minusDays(7));
        if (event.getMessage().getEmbeds().size() > 0) {
            MessageEmbed embed = event.getMessage().getEmbeds().get(0);
            message = message + embed.getTitle() + embed.getDescription();
        }
        message = message.toLowerCase().replace(":", "").replace(" ", "");

        long crossPost = 0;
        if (!event.isFromType(ChannelType.PRIVATE)) {
            crossPost = allMessages.stream()
                .filter(m -> m.getMember().getIdLong() == event.getMember().getIdLong() && m.getGuild().getIdLong() == event.getGuild().getIdLong() && m.getMessage().getAttachments().size() == 0
                    && m.getMessage().getContentDisplay().equalsIgnoreCase(event.getMessage().getContentDisplay()) && m.getChannel().getIdLong() != event.getChannel().getIdLong() /* Counts all messages in other channels  */)
                .count();
        }

        int suspiciousValue = (int) crossPost;
        suspiciousValue += newAccount ? 1 : 0; //add sus points if account is less than 7 days old
        suspiciousValue += message.contains("@everyone") ? 2 : 0; //all spaces are removed
        suspiciousValue += message.contains("money") ? 1 : 0;
        suspiciousValue += message.contains("loot") ? 2 : 0;
        suspiciousValue += message.contains("csgo") ? 2 : 0;
        suspiciousValue += message.contains("trade") ? 2 : 0;
        suspiciousValue += message.contains("skin") ? 1 : 0;
        suspiciousValue += message.contains("knife") ? 1 : 0;
        suspiciousValue += message.contains("offer") ? 1 : 0;
        suspiciousValue += message.contains("btc") ? 1 : 0;
        suspiciousValue += message.contains("bitcoin") ? 1 : 0;
        suspiciousValue += message.contains("nitro") ? 1 : 0;
        suspiciousValue += message.contains("1months") ? 1 : 0;
        suspiciousValue += message.contains("3months") ? 1 : 0;
        suspiciousValue += message.contains("free") ? 1 : 0;
        suspiciousValue += message.contains("case") ? 1 : 0;
        suspiciousValue += message.contains("!!!") ? 1 : 0;
        suspiciousValue += message.contains("booster") ? 1 : 0;
        suspiciousValue += message.contains("dollar") ? 1 : 0;
        suspiciousValue += message.contains("download") ? 1 : 0;
        suspiciousValue += message.contains("100%") ? 1 : 0;
        suspiciousValue += message.contains("bro") ? 1 : 0;
        suspiciousValue += message.contains("checkthis") ? 1 : 0;
        suspiciousValue += message.contains("friendhasgiftedyou") ? 2 : 0;
        suspiciousValue += message.matches(".*made.*game.*") ? 1 : 0;
        suspiciousValue += message.matches(".*left.*game.*") ? 2 : 0;
        if (suspiciousValue > 1) {
            suspiciousValue += message.contains("http") ? 1 : 0;
            suspiciousValue += message.contains(".ru/") ? 1 : 0;
            suspiciousValue += message.contains("bit.ly") ? 2 : 0;
            suspiciousValue += message.contains("cutt.ly") ? 2 : 0;
            suspiciousValue += message.contains("mega.nz") ? 2 : 0;
            suspiciousValue += message.contains("hour") ? 1 : 0;
            suspiciousValue += message.contains("$") ? 1 : 0;
        }

        if (suspiciousValue > 0) {
            System.out.println("Scam Shield points for this message: " + suspiciousValue + (newAccount ? " New Account" : "") + (crossPost > 0 ? " Crossposted" : ""));
        }

        if (event.getMessage().getMentions(MentionType.USER).size() > 3) //kick mass ping selfbots
            suspiciousValue = 69420;

        return suspiciousValue;
    }

    public static boolean checkForFishing(MessageReceivedEvent event) {
        if (event.getMember() == null)
            return false;
        if (event.getMessage().getType().isSystem())
            return false;
        if (ServerMessagesHandler.checkIfStaff(event))
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

        if (event.getMessage().getType().isSystem())
            return false;
        if (ServerMessagesHandler.checkIfStaff(event))
            return false;

        int suspiciousValue = ssValue(event);
        if (suspiciousValue > 0)
            event.getJDA().getGuildById(633588473433030666L).getTextChannelById(896839871543525417L).sendMessage("DM from " + event.getAuthor().getAsMention() + " " + event.getAuthor().getId() + " gotten " + suspiciousValue + " sus points\n\n" + message).queue();
        if (suspiciousValue <= 3)
            return false;

        //return handleCrossBan(event, null, suspiciousValue);
        return false; //remove line when enabling above
    }

    private static boolean handleCrossBan(MessageReceivedEvent event, List<HandledServerMessageContext> sameauthormessages, int suspiciousCount) {
        List<Guild> mutualGuilds = new ArrayList<>(event.getAuthor().getMutualGuilds());
        mutualGuilds.removeIf(g -> {
            if (g == event.getGuild())
                return false;
            if (GuildConfigurations.configurations.get(g.getIdLong()) != null)
                return !GuildConfigurations.configurations.get(g.getIdLong())[GuildConfigurations.ConfigurationMap.SSCROSS.ordinal()];
            else
                return true;
        });
        boolean status = false;
        for (Guild guild : mutualGuilds) {
            if (handleBan(event, guild.getIdLong(), suspiciousCount, true, sameauthormessages))
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
        Guild guild = event.getJDA().getGuildById(guildID);
        Member member = guild.getMember(event.getAuthor());
        String usernameWithTag = event.getAuthor().getAsTag();
        String userId = event.getAuthor().getId();
        String reportChannelID = CommandManager.mlReportChannels.get(guildID);
        boolean ssBan;
        if (GuildConfigurations.configurations.get(guildID) != null) {
            ssBan = GuildConfigurations.configurations.get(guildID)[GuildConfigurations.ConfigurationMap.SSBAN.ordinal()];
        }
        else {
            ssBan = false;
        }
        System.out.println("Now " + (ssBan ? "Banning " : "Kicking ") + usernameWithTag + " from " + guild.getName());
        EmbedBuilder embedBuilder = new EmbedBuilder()
            .setAuthor(ssBan ? "Ban" : "Kick" + " Report", null, "https://cdn.discordapp.com/avatars/275759980752273418/05d2f38ca37928426f7c49b191b8b552.webp")
            .setTimestamp(Instant.now())
            .setFooter("Received " + suspiciousCount + " naughty points.");

        if (!guild.getSelfMember().canInteract(member)) {
            embedBuilder.setDescription("Unable to " + (ssBan ? "Ban" : "Kick") + " user **" + usernameWithTag + "** (*" + userId + "*) because they are a higher role than my role");
            if (guild.getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_EMBED_LINKS))
                event.getTextChannel().sendMessageEmbeds(embedBuilder.build()).queue();
            else
                event.getTextChannel().sendMessage(embedBuilder.getDescriptionBuilder().toString()).queue();
        }

        if (guild.getSelfMember().hasPermission(Permission.BAN_MEMBERS)) {
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

        if (reportChannelID != null) {
            TextChannel reportChannel = guild.getTextChannelById(reportChannelID);
            StringBuilder sb = new StringBuilder(usernameWithTag + " " + userId + " was " + (ssBan ? "Banned" : "Kicked") + " from " + guild.getName() + (event.getAuthor().getTimeCreated().isAfter(OffsetDateTime.now().minusDays(7)) ? " Additional point added for young account\n" : "\n"));
            sameauthormessages.forEach(a -> sb.append("\n").append(a.messageReceivedEvent.getMessage().getContentRaw()).append("\n\n").append(a.suspiciousValue).append(" point").append(a.suspiciousValue > 1 ? "s in " : " in ").append(a.messageReceivedEvent.getChannel().getName()).append("\n"));
            if (ssQueued != null)
                ssQueued.cancel(/*mayInterruptIfRunning*/ true);
            if (guild.getSelfMember().hasPermission(reportChannel, Permission.MESSAGE_EMBED_LINKS))
                ssQueued = reportChannel.sendMessageEmbeds(embedBuilder.build()).addFile(sb.toString().getBytes(), usernameWithTag + ".txt").queueAfter(10, TimeUnit.SECONDS);
            else
                ssQueued = reportChannel.sendMessage(embedBuilder.getDescriptionBuilder().toString()).addFile(sb.toString().getBytes(), usernameWithTag + ".txt").queueAfter(10, TimeUnit.SECONDS);
        }
        else {
            embedBuilder.getDescriptionBuilder().append("\nTo admins: Use the command `l!setmlreportchannel` to set the report channel.");
            if (guild.getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_EMBED_LINKS))
                event.getTextChannel().sendMessageEmbeds(embedBuilder.build()).queue();
            else
                event.getTextChannel().sendMessage(embedBuilder.getDescriptionBuilder().toString()).queue();
        }
        return status;
    }
}
