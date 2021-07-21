package slaynash.lum.bot.discord;

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
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.melonscanner.LogCounter;

public class ScamShield {

    private static final Queue<HandledServerMessageContext> handledMessages = new LinkedList<>();

    private static ScheduledFuture<?> ssQueued;

    public static boolean checkForFishing(MessageReceivedEvent event) {

        Long guildID = event.getGuild().getIdLong();

        if (ServerMessagesHandler.checkIfStaff(event))
            return false;

        int suspiciousValue = 0;
        // I found a simple referral and you can loot skins there\nhttp://csgocyber.ru/simlpebonus\nIf it's not difficult you can then throw me a trade and I'll give you the money
        //@everyone Hello I am leaving CS:GO and giving away my skins to people who send trade offers. For first people I will give away my 3 knifes. Don't be greedy and take few skins :  https://streancommunuty.ru/tradoffer/new/?partner=1284276379&token=iMDdLkoe
        String message = event.getMessage().getContentDisplay().toLowerCase();
        if (event.getMessage().getEmbeds().size() > 0) {
            MessageEmbed embed = event.getMessage().getEmbeds().get(0);
            message = message + " " + embed.getTitle() + " " + embed.getDescription();
        }

        boolean newAccount = event.getAuthor().getTimeCreated().isAfter(OffsetDateTime.now().minusDays(7));

        suspiciousValue += newAccount ? 1 : 0; //add sus points if account is less then 7 days old
        suspiciousValue += message.contains("@everyone") ? 2 : 0;
        suspiciousValue += message.contains("money") ? 1 : 0;
        suspiciousValue += message.contains("loot") ? 2 : 0;
        suspiciousValue += message.replace(":", "").replace(" ", "").contains("csgo") ? 2 : 0; //CS:GO that ignores colon and spaces
        suspiciousValue += message.contains("trade") ? 2 : 0;
        suspiciousValue += message.contains("skin") ? 1 : 0;
        suspiciousValue += message.contains("knife") ? 2 : 0;
        suspiciousValue += message.contains("offer") ? 1 : 0;
        suspiciousValue += message.contains("btc") ? 1 : 0;
        suspiciousValue += message.contains("free") ? 1 : 0;
        suspiciousValue += message.contains("case") ? 1 : 0;
        suspiciousValue += message.contains("!!!") ? 1 : 0;
        suspiciousValue += message.contains("code:") ? 2 : 0;
        suspiciousValue += message.contains("booster") ? 2 : 0;
        suspiciousValue += message.contains("dollar") ? 1 : 0;
        suspiciousValue += message.contains("download") ? 1 : 0;
        suspiciousValue += message.contains("100%") ? 1 : 0;
        suspiciousValue += message.contains("bro") ? 1 : 0;
        suspiciousValue += message.contains("check this") ? 1 : 0;
        suspiciousValue += message.contains("made a game") ? 2 : 0;
        if (suspiciousValue > 1) {
            suspiciousValue += message.contains("http") ? 1 : 0;
            suspiciousValue += message.contains(".ru/") ? 1 : 0;
            suspiciousValue += message.contains("bit.ly") ? 2 : 0;
            suspiciousValue += message.contains("cutt.ly") ? 2 : 0;
            suspiciousValue += message.contains("mega.nz") ? 2 : 0;
            suspiciousValue += message.contains("hour") ? 1 : 0;
            suspiciousValue += message.contains("steampowered.com") ? -1 : 0;
            suspiciousValue += message.contains("steamcommunity.com") ? -1 : 0;
            suspiciousValue += message.contains("$") ? 1 : 0;
        }

        if (suspiciousValue > 0) {
            System.out.print("Scam Shield points for this message: " + suspiciousValue);
            if (newAccount)
                System.out.println(" New account");
            else
                System.out.println();
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        while (handledMessages.peek() != null && handledMessages.peek().creationTime.until(now, ChronoUnit.SECONDS) > 60 * 3)
            handledMessages.remove(); //remove all saved messages that is older then 3 minutes

        if (suspiciousValue < 3)
            suspiciousValue = 0;
        if (suspiciousValue > 3 && suspiciousValue < 6) //if one message gets 6+ then it is a instant kick on first message
            suspiciousValue = 3;
        if (suspiciousValue > 0)
            handledMessages.add(new HandledServerMessageContext(event, suspiciousValue, guildID)); // saves a copy of message and point, should avoid false-positives, force 2 messages

        List<HandledServerMessageContext> sameauthormessages = handledMessages.stream()
            .filter(m -> m.messageReceivedEvent.getMember().getIdLong() == event.getMember().getIdLong() && m.guildId == guildID)
            .collect(Collectors.toList());

        int suspiciousCount = (int) sameauthormessages.stream().map(m -> m.suspiciousValue).reduce(0, Integer::sum); //this adds all points that one user collected

        if (suspiciousCount > 4 && !event.getMember().hasPermission(Permission.ADMINISTRATOR, Permission.MESSAGE_MANAGE)) {
            String usernameWithTag = event.getAuthor().getAsTag();
            String userId = event.getAuthor().getId();
            String reportChannelID = CommandManager.logChannels.get(guildID);
            boolean ssBan;
            if (GuildConfigurations.configurations.get(guildID) != null) {
                ssBan = GuildConfigurations.configurations.get(guildID)[GuildConfigurations.ConfigurationMap.SSBAN.ordinal()];
            }
            else {
                ssBan = false;
            }
            EmbedBuilder embedBuilder = new EmbedBuilder()
                .setAuthor(ssBan ? "Ban" : "Kick" + " Report", null, "https://cdn.discordapp.com/avatars/275759980752273418/05d2f38ca37928426f7c49b191b8b552.webp")
                .setTimestamp(Instant.now())
                .setFooter("Received " + suspiciousCount + " naughty points.");

            if (!event.getGuild().getSelfMember().canInteract(event.getMember()))
                return false;

            if (ssBan && event.getGuild().getSelfMember().hasPermission(Permission.BAN_MEMBERS)) {
                try {
                    event.getAuthor().openPrivateChannel().flatMap(channel -> channel.sendMessage("You have been automatically been Banned from " + event.getGuild().getName() +
                    " for phishing. We highly recommend that you change your password immediately.")).complete();
                }
                catch (Exception e) {
                    System.out.println("Failed to open dms with scammer");
                }
                event.getMember().ban(1, "Banned by Lum's Scam Shield").complete();
                embedBuilder.setDescription("User **" + usernameWithTag + "** (*" + userId + "*) was Banned by the Scam Shield");
                LogCounter.addSSCounter(userId, message, event.getGuild().getId()); // add to status counter
            }
            else if (!ssBan && event.getGuild().getSelfMember().hasPermission(Permission.KICK_MEMBERS)) {
                try {
                    event.getAuthor().openPrivateChannel().flatMap(channel -> channel.sendMessage("You have been automatically been Kicked from " + event.getGuild().getName() +
                    " for phishing. We highly recommend that you change your password immediately.")).complete();
                }
                catch (Exception e) {
                    System.out.println("Failed to open dms with scammer");
                }
                event.getMember().kick("Kicked by Lum's Scam Shield").complete();
                embedBuilder.setDescription("User **" + usernameWithTag + "** (*" + userId + "*) was Kicked by the Scam Shield");
                LogCounter.addSSCounter(userId, message, event.getGuild().getId()); // add to status counter
            }
            else
                embedBuilder.setDescription("Lum failed to " + (ssBan ? "Ban" : "Kick") + " **" + usernameWithTag + "** (*" + userId + "*) for scam because I don't have " + (ssBan ? "Ban" : "Kick") + " perms");

            if (!ssBan && event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) {
                List<Message> messagelist = new ArrayList<>();
                sameauthormessages.forEach(m -> {
                    if (m.messageReceivedEvent.getGuild().getSelfMember().hasPermission(m.messageReceivedEvent.getTextChannel(), Permission.VIEW_CHANNEL)) {
                        messagelist.add(m.messageReceivedEvent.getMessage());
                    }
                    else {
                        System.out.println("Lum does not have VIEW_CHANNEL perm in " + m.messageReceivedEvent.getTextChannel().getName());
                        String temp = "";
                        if (!embedBuilder.getDescriptionBuilder().toString().isBlank())
                            temp = embedBuilder.getDescriptionBuilder().toString() + "\n";
                        embedBuilder.setDescription(temp + "Lum failed to remove messages from **" + usernameWithTag + "** (*" + userId + "*) because I don't have view channel perms.");
                    }
                });
                if (messagelist.size() == 1)
                    event.getMessage().delete().queue();
                else if (messagelist.size() > 1)
                    event.getTextChannel().deleteMessages(messagelist).queue();
            }
            else if (!ssBan) {
                System.out.println("Lum does not have MESSAGE_MANAGE perm");
                String temp = "";
                if (!embedBuilder.getDescriptionBuilder().toString().isBlank())
                    temp = embedBuilder.getDescriptionBuilder().toString() + "\n";
                embedBuilder.setDescription(temp + "Lum failed to remove messages from **" + usernameWithTag + "** (*" + userId + "*) because I don't have manage message perms.");
            }

            if (reportChannelID != null) {
                TextChannel reportChannel = event.getGuild().getTextChannelById(reportChannelID);
                if (ssQueued != null)
                    ssQueued.cancel(/*mayInterruptIfRunning*/ true);
                if (event.getGuild().getSelfMember().hasPermission(reportChannel, Permission.MESSAGE_EMBED_LINKS))
                    ssQueued = reportChannel.sendMessageEmbeds(embedBuilder.build()).queueAfter(10, TimeUnit.SECONDS);
                else
                    ssQueued = reportChannel.sendMessage(embedBuilder.getDescriptionBuilder().toString()).queueAfter(10, TimeUnit.SECONDS);
            }
            else {
                embedBuilder.getDescriptionBuilder().append("\nTo admins: Use the command `l!setlogchannel` to set the report channel.");
                if (event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_EMBED_LINKS))
                    event.getTextChannel().sendMessageEmbeds(embedBuilder.build()).queue();
                else
                    event.getTextChannel().sendMessage(embedBuilder.getDescriptionBuilder().toString()).queue();
            }
            return true;
        }
        return false;
    }
}
