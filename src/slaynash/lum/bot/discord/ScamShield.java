package slaynash.lum.bot.discord;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.Instant;
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
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.melonscanner.LogCounter;
import slaynash.lum.bot.utils.ExceptionUtils;

public class ScamShield {

    private static final Queue<HandledServerMessageContext> handledMessages = new LinkedList<>();

    private static ScheduledFuture<?> ssQueued;
    
    public static boolean checkForFishing(MessageReceivedEvent event) {

        Long GuildID = event.getGuild().getIdLong();

        if(ServerMessagesHandler.checkIfStaff(event))
            return false;

        // I found a simple referral and you can loot skins there\nhttp://csgocyber.ru/simlpebonus\nIf it's not difficult you can then throw me a trade and I'll give you the money
        //@everyone Hello I am leaving CS:GO and giving away my skins to people who send trade offers. For first people I will give away my 3 knifes. Don't be greedy and take few skins :  https://streancommunuty.ru/tradoffer/new/?partner=1284276379&token=iMDdLkoe
        String message = event.getMessage().getContentRaw().toLowerCase();
        int suspiciousValue = 0;
        suspiciousValue += event.getAuthor().getTimeCreated().isAfter(OffsetDateTime.now().minusDays(7)) ? 1 : 0; //add sus points if account is less then 7 days old
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
        suspiciousValue += message.contains("!!!!") ? 1 : 0;
        suspiciousValue += message.contains("code:") ? 2 : 0;
        suspiciousValue += message.contains("booster") ? 2 : 0;
        suspiciousValue += message.contains("dollar") ? 1 : 0;
        suspiciousValue += message.contains("download") ? 1 : 0;
        suspiciousValue += message.contains("100%") ? 1 : 0;
        suspiciousValue += message.contains("made a game") ? 2 : 0;
        if (suspiciousValue > 0){
            suspiciousValue += message.contains("http") ? 1 : 0;
            suspiciousValue += message.contains(".ru/") ? 1 : 0;
            suspiciousValue += message.contains("bit.ly") ? 2 : 0;
            suspiciousValue += message.contains("cutt.ly") ? 2 : 0;
            suspiciousValue += message.contains("mega.nz") ? 2 : 0;
            suspiciousValue += message.contains("hour") ? 1 : 0;
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        while (handledMessages.peek() != null && handledMessages.peek().creationTime.until(now, ChronoUnit.SECONDS) > 60)
            handledMessages.remove(); //remove all saved messages that is older then 60 seconds

        if (suspiciousValue < 2)
            suspiciousValue = 0;
        if (suspiciousValue > 3 && suspiciousValue < 7) //if one message gets 7+ then it is a instant ban on first message
            suspiciousValue = 3;
        if (suspiciousValue > 0)
            handledMessages.add(new HandledServerMessageContext(event, suspiciousValue, GuildID)); // saves a copy of message and point, should avoid false-positives, force 2 messages

        List<HandledServerMessageContext> sameauthormessages = handledMessages.stream()
            .filter(m -> m.messageReceivedEvent.getMember().getIdLong() == event.getMember().getIdLong() && m.guildId == GuildID)
            .collect(Collectors.toList());

        int suspiciousCount = (int)sameauthormessages.stream().map(m -> m.suspiciousValue).reduce(0, Integer::sum); //this adds all points that one user collected

        if (suspiciousCount > 4) {
            String usernameWithTag = event.getAuthor().getAsTag();
            String userId = event.getAuthor().getId();
            String reportChannel = CommandManager.mlReportChannels.get(GuildID);
            EmbedBuilder embedBuilder = new EmbedBuilder()
                .setAuthor("Ban Report", null, "https://cdn.discordapp.com/avatars/275759980752273418/05d2f38ca37928426f7c49b191b8b552.webp")
                .setTimestamp(Instant.now())
                .setFooter("Received " + suspiciousCount + " naughty points.");

            if(event.getGuild().getSelfMember().hasPermission(Permission.BAN_MEMBERS)){
                event.getMember().ban(1, "Banned by Lum's Scam Shield").complete();
                embedBuilder.setDescription("User **" + usernameWithTag + "** (*" + userId + "*) was Banned by the Scam Shield");
                LogCounter.AddSSCounter(userId, message, event.getGuild().getId()); // add to status counter
            }
            else {
                if(event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)){
                    List<Message> messagelist = new ArrayList<>();
                    sameauthormessages.forEach(m -> messagelist.add(m.messageReceivedEvent.getMessage()));
                    if(messagelist.size() == 1)
                        event.getMessage().delete().queue();
                    else if (messagelist.size() > 1)
                        event.getTextChannel().deleteMessages(messagelist).queue();
                    embedBuilder.setDescription("Lum failed to ban **" + usernameWithTag + "** (*" + userId + "*) for scam because I don't have ban perms but did remove messages.");
                }
                else
                    embedBuilder.setDescription("Lum failed to ban **" + usernameWithTag + "** (*" + userId + "*) because I don't have ban perms.");
            }

            MessageEmbed builtEmbed = embedBuilder.build();
            if(builtEmbed.isEmpty()){
                ExceptionUtils.reportException("Scam Shield report Embed is empty", "Guild: " + GuildID);
                return true;
            }
            if (reportChannel != null){
                if (ssQueued != null)
                    ssQueued.cancel(/*mayInterruptIfRunning*/ true);
                ssQueued = event.getGuild().getTextChannelById(reportChannel).sendMessageEmbeds(builtEmbed).queueAfter(10, TimeUnit.SECONDS);
            }
            else
                event.getTextChannel().sendMessageEmbeds(builtEmbed).queue();

            return true;
        }
        return false;
    }

}
