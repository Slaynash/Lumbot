package slaynash.lum.bot;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.ExceptionEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdatePendingEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateOwnerEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.session.SessionRecreateEvent;
import net.dv8tion.jda.api.events.session.SessionResumeEvent;
import net.dv8tion.jda.api.events.user.UserTypingEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.gcardone.junidecode.Junidecode;
import org.jetbrains.annotations.NotNull;
import slaynash.lum.bot.api.API;
import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.discord.JDAManager;
import slaynash.lum.bot.discord.Memes;
import slaynash.lum.bot.discord.MessageProxy;
import slaynash.lum.bot.discord.Moderation;
import slaynash.lum.bot.discord.PrivateMessagesHandler;
import slaynash.lum.bot.discord.ReactionListener;
import slaynash.lum.bot.discord.ScamShield;
import slaynash.lum.bot.discord.ServerMessagesHandler;
import slaynash.lum.bot.discord.VRCApiVersionScanner;
import slaynash.lum.bot.discord.VerifyPair;
import slaynash.lum.bot.discord.commands.AddMissingRoles;
import slaynash.lum.bot.discord.melonscanner.MLHashPair;
import slaynash.lum.bot.discord.melonscanner.MelonScanner;
import slaynash.lum.bot.discord.melonscanner.MelonScannerApisManager;
import slaynash.lum.bot.discord.slashs.SlashManager;
import slaynash.lum.bot.discord.utils.CrossServerUtils;
import slaynash.lum.bot.log.LogSystem;
import slaynash.lum.bot.steam.Steam;
import slaynash.lum.bot.timers.ClearDMs;
import slaynash.lum.bot.timers.Reminders;
import slaynash.lum.bot.utils.ExceptionUtils;
import slaynash.lum.bot.utils.Utils;
import slaynash.lum.bot.uvm.UnityVersionMonitor;


public class Main extends ListenerAdapter {
    public static boolean isShuttingDown = false;

    public static void main(String[] args) throws Exception {
        System.out.println("Starting Lum...");
        System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> ExceptionUtils.reportException("Exception in thread " + thread.getName() + ":", throwable));
        LogSystem.init();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            isShuttingDown = true;

            JDA jda = JDAManager.getJDA();
            if (jda != null && jda.getSelfUser().getIdLong() == 275759980752273418L && ConfigManager.mainBot) // Lum (blue)
                jda.getTextChannelById(808076226064941086L)
                    .sendMessageEmbeds(Utils.wrapMessageInEmbed("Lum is shutting down", Color.orange))
                    .complete();

            MelonScanner.shutdown();
        }));

        ConfigManager.init();
        Localization.init();

        DBConnectionManagerLum.init();
        DBConnectionManagerShortUrls.init();

        loadLogchannelList();
        loadVerifychannelList();
        loadReactionsList();
        loadScreeningRolesList();
        loadMelonLoaderVersions();
        loadMLHashes();
        loadMLReportChannels();
        loadAPChannels();
        loadReplies();
        CrossServerUtils.loadGuildCount();

        API.start();

        MelonScanner.init();

        CommandManager.init();
        JDAManager.init(ConfigManager.discordToken);

        System.out.println("Connected to " + JDAManager.getJDA().getGuilds().size() + " Guilds!");

        SlashManager.registerCommands();

        if (JDAManager.getJDA().getSelfUser().getIdLong() == 275759980752273418L) { // Lum (blue)
            if (ConfigManager.mainBot) {
                JDAManager.getJDA().getPresence().setStatus(OnlineStatus.ONLINE);
                JDAManager.getJDA().getPresence().setActivity(Activity.watching("melons getting loaded"));
                JDAManager.enableEvents();
                UnityVersionMonitor.start();
                VRCApiVersionScanner.init();
                Timer timer = new Timer();
                timer.schedule(
                    new ClearDMs(),
                    java.util.Calendar.getInstance().getTime(),
                    1000 * 60 * 60
                );
                new Steam().start();
                Calendar time = Calendar.getInstance();
                time.set(Calendar.MILLISECOND, 0);
                time.set(Calendar.SECOND, 0);
                time.set(Calendar.MINUTE, time.get(Calendar.MINUTE) + 1);
                timer.schedule(
                    new Reminders(),
                    time.getTime(),
                    1000 * 60
                );
            }
            else
                System.out.println("Starting Lum as a backup bot, monitoring main bot...");
        }
        else
            JDAManager.enableEvents();

        HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).followRedirects(HttpClient.Redirect.ALWAYS).connectTimeout(Duration.ofSeconds(20)).build();
        HttpRequest pingCheckRequest = HttpRequest.newBuilder().GET().uri(URI.create(ConfigManager.pingURL)).setHeader("User-Agent", "LUM Bot (https://discord.gg/akFkAG2)").timeout(Duration.ofSeconds(20)).build();
        if (!ConfigManager.mainBot) {
            HttpResponse<byte[]> response = null;
            while (response == null || response.statusCode() == 200) {
                try {
                    response = MelonScannerApisManager.downloadRequest(httpClient, pingCheckRequest, "PingChecker", 2);
                    System.out.println("PingChecker: " + response.statusCode());
                }
                catch (Exception e) {
                    System.out.println("Failed to contact main bot, starting up...");
                    break;
                }

                Thread.sleep(1000 * 15);
            }
            System.out.println("PingChecker: Ping failed, starting up backup...");
            JDAManager.enableEvents();
        }
        else {
            //chunk members for mutuals after loading to prevent Lum from being unresponsive
            for (Guild guild : JDAManager.getJDA().getGuilds()) {
                if (!CommandManager.autoScreeningRoles.containsKey(guild.getIdLong())) { // already chunked in the AddMissingRoles a few lines above
                    try {
                        guild.loadMembers().get();
                    }
                    catch (Exception e) {
                        System.out.println("Failed to chunk members for guild " + guild.getName() + " (" + guild.getId() + ")");
                        Thread.sleep(1000);
                        // guild.loadMembers().get(); // try again
                    }
                    Thread.sleep(690); //rate limit is 100 chuck per minute, gave a little headroom
                }
            }
        }

        if (JDAManager.getJDA().getSelfUser().getIdLong() == 275759980752273418L) { // Lum (blue)
            Moderation.voiceStartup();
            if (ConfigManager.mainBot) {
                new AddMissingRoles().addMissing(null);
                JDAManager.getJDA()
                        .getTextChannelById(808076226064941086L)
                        .sendMessageEmbeds(Utils.wrapMessageInEmbed("Lum restarted successfully!", Color.green))
                        .queue();
            }
        }
        System.out.println("LUM Started!");
        if (!ConfigManager.mainBot) { // If not the main bot, ping the main bot to see if it is online and if not, take over
            //noinspection InfiniteLoopStatement
            while (true) {
                Thread.sleep(1000 * 15);
                if (JDAManager.getJDA().getStatus() != JDA.Status.CONNECTED) {
                    System.out.println("Not Connected to Discord...");
                    continue;
                }
                int statusCode;
                try {
                    statusCode = MelonScannerApisManager.downloadRequest(httpClient, pingCheckRequest, "PingChecker", 2).statusCode();
                }
                catch (Exception e) {
                    statusCode = 0;
                }
                if (statusCode == 200) {
                    System.out.println("PingChecker: Ping successful, shutting down...");
                    if (JDAManager.isEventsEnabled())
                        JDAManager.disableEvents();
                    JDAManager.getJDA().getGuildById(633588473433030666L).getTextChannelById(1184560349039575152L).sendMessage("Backup is shutting down").queue();
                }
                else {
                    System.out.println("PingChecker: Ping failed, starting backup...");
                    if (!JDAManager.isEventsEnabled())
                        JDAManager.enableEvents();
                    JDAManager.getJDA().getGuildById(633588473433030666L).getTextChannelById(1184560349039575152L).sendMessage("Backup can't contact Lum and starting up").queue();
                }
            }
        }
    }

    private static void loadReactionsList() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("storage/rolereactions.txt"));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ", 4);
                if (parts.length == 3 && parts[0].matches("^\\d+$") && parts[2].matches("^\\d+$")) {
                    CommandManager.reactionListeners.add(new ReactionListener(parts[0], parts[1], parts[2]));
                }
            }
            reader.close();
        }
        catch (IOException e) {
            ExceptionUtils.reportException("Failed to load RoleReactions", e);
        }
    }

    private static void loadScreeningRolesList() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("storage/rolescreening.txt"));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length == 2 && parts[0].matches("^\\d+$") && parts[1].matches("^\\d+$")) {
                    CommandManager.autoScreeningRoles.put(Long.parseLong(parts[0]), Long.parseLong(parts[1]));
                }
                else System.err.println("loadScreeningRolesList is formated badly");
            }
            reader.close();
        }
        catch (IOException e) {
            ExceptionUtils.reportException("Failed to load ScreeningRoles", e);
        }
    }

    private static void loadMLHashes() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("storage/mlhashes.txt"));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    String[] parts = line.split(" ", 3);
                    if (parts[0].equals("r"))
                        CommandManager.melonLoaderHashes.add(new MLHashPair(parts[1], parts[2]));
                    else
                        CommandManager.melonLoaderAlphaHashes.add(new MLHashPair(parts[1], parts[2]));

                }
            }
            reader.close();
        }
        catch (IOException e) {
            ExceptionUtils.reportException("Failed to load MelonLoader Hashes", e);
        }
    }

    private static void loadMLReportChannels() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("storage/mlreportchannels.txt"));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ", 4);
                if (parts.length == 2 && parts[0].matches("^\\d+$") && parts[1].matches("^\\d+$")) {
                    CommandManager.mlReportChannels.put(Long.parseLong(parts[0]), parts[1]);
                }
            }
            reader.close();
        }
        catch (IOException e) {
            ExceptionUtils.reportException("Failed to load MelonLoader Report Channels", e);
        }
    }

    private static void loadLogchannelList() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("storage/logchannels.txt"));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ", 4);
                if (parts.length == 2 && parts[0].matches("^\\d+$") && parts[1].matches("^\\d+$")) {
                    CommandManager.logChannels.put(Long.parseLong(parts[0]), parts[1]);
                }
            }
            reader.close();
        }
        catch (IOException e) {
            ExceptionUtils.reportException("Failed to load Log Channels", e);
        }
    }

    private static void loadVerifychannelList() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("storage/verifychannels.txt"));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ", 5);
                if (parts.length == 3 && parts[0].matches("^\\d+$") && parts[1].matches("^\\d+$") && parts[2].matches("^\\d+$")) {
                    CommandManager.verifyChannels.put(Long.parseLong(parts[0]), new VerifyPair(parts[1], parts[2]));
                }
            }
            reader.close();
        }
        catch (IOException e) {
            ExceptionUtils.reportException("Failed to load Verify Channels", e);
        }
    }

    private static void loadMelonLoaderVersions() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("storage/melonloaderversions.txt"));
            MelonScanner.latestMLVersionRelease = reader.readLine().trim();
            MelonScanner.latestMLVersionAlpha = reader.readLine().trim();
            reader.close();
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to load MelonLoader Versions", e);
        }
    }

    private static void loadAPChannels() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("storage/autopublishchannels.txt"));
            String line;
            while ((line = reader.readLine()) != null) {
                CommandManager.apChannels.add(Long.parseLong(line));
            }
            reader.close();
        }
        catch (IOException e) {
            ExceptionUtils.reportException("Failed to load Auto Publish Channels", e);
        }
    }

    private static void loadReplies() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("storage/replies.txt"));
            String line;
            String[] parts;
            while ((line = reader.readLine()) != null) {
                parts = line.split(",", 2);
                if (parts[0].startsWith("regex")) {
                    parts[0] = parts[0].substring(5);
                    Map<String, String> tempReplies = CommandManager.guildRegexReplies.getOrDefault(Long.parseLong(parts[0]), new HashMap<>());
                    String[] reply = parts[1].replace("&#10;", "\n").split("&#00;", 2);
                    tempReplies.put(reply[0], reply[1]);
                    CommandManager.guildRegexReplies.put(Long.parseLong(parts[0]), tempReplies);
                }
                else {
                    Map<String, String> tempReplies = CommandManager.guildReplies.getOrDefault(Long.parseLong(parts[0]), new HashMap<>());
                    String[] reply = parts[1].replace("&#10;", "\n").split("&#00;", 2);
                    tempReplies.put(reply[0], reply[1]);
                    CommandManager.guildReplies.put(Long.parseLong(parts[0]), tempReplies);
                }
            }
            reader.close();
        }
        catch (IOException e) {
            ExceptionUtils.reportException("Failed to load Replies", e);
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.isFromType(ChannelType.PRIVATE)) {
            PrivateMessagesHandler.handle(event);
        }
        else {
            ServerMessagesHandler.handle(event);
        }
    }

    @Override
    public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!MessageProxy.edits(event) && !event.isFromType(ChannelType.PRIVATE)) {
            ScamShield.checkForFishing(event);
        }
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        MessageProxy.deletes(event);
        ScamShield.checkDeleted(event);
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        // Don't react to self roles
        if (event.getMessageAuthorIdLong() == JDAManager.getJDA().getSelfUser().getIdLong())
            return;

        if (MessageProxy.reactions(event)) return;
        Memes.memeReaction(event);

        // System.out.println("[" + event.getGuild().getName() + "] [#" + event.getChannel().getName() + "] " + event.getUser().getName() + " reacted with " + event.getReactionEmote().getName() + (event.getReactionEmote().isEmote() ? "isEmote id:" + event.getReactionEmote().getId() : "") + (EmojiUtils.containsEmoji(event.getReactionEmote().getName()) ? " is Emoji" : ""));
        for (ReactionListener rl : CommandManager.reactionListeners) {
            if (event.getMessageId().equals(rl.messageId()) && (event.getEmoji().getType() == Emoji.Type.CUSTOM ? event.getEmoji().asCustom().getId().equals(rl.emoteId()) : event.getEmoji().asUnicode().getAsReactionCode().equals(rl.emoteId()))) {
                Role role = event.getGuild().getRoleById(rl.roleId());
                if (role != null) {
                    event.getGuild().addRoleToMember(event.getMember(), role).reason("User clicked role reaction").queue();
                    writeLogMessage(event.getGuild(), "Added role `" + role.getName() + "` to " + event.getUser().getAsMention());
                }
                else
                    writeLogMessage(event.getGuild(), "Role `" + rl.roleId() + "` not found");
                return;
            }
        }
    }

    private static void writeLogMessage(Guild guild, String message) {
        System.out.println("[" + guild.getName() + "] " + message);
        String channelId;
        if ((channelId = CommandManager.logChannels.get(guild.getIdLong())) != null) {
            for (TextChannel c : guild.getTextChannels()) {
                if (c.getId().equals(channelId)) {
                    c.sendMessageEmbeds(Utils.wrapMessageInEmbed(message, Color.gray)).queue();
                    break;
                }
            }
        }
    }

    @Override
    public void onMessageReactionRemove(@NotNull MessageReactionRemoveEvent event) {
        if (MessageProxy.reactions(event)) return;
        for (ReactionListener rl : CommandManager.reactionListeners) {
            if (event.getMessageId().equals(rl.messageId()) && (event.getEmoji().getType() == Emoji.Type.CUSTOM ? event.getEmoji().asCustom().getId().equals(rl.emoteId()) : event.getEmoji().asUnicode().getAsReactionCode().equals(rl.emoteId()))) {
                Role role = event.getGuild().getRoleById(rl.roleId());
                if (role != null && event.getUser() != null) {
                    event.getGuild().removeRoleFromMember(event.getUser(), role).reason("User removed role reaction").queue();
                    writeLogMessage(event.getGuild(), "Removed role `" + role.getName() + "` from " + event.getUser().getAsMention());
                }
                else
                    writeLogMessage(event.getGuild(), "Role `" + rl.roleId() + "` not found");
                return;
            }
        }
    }

    @Override
    public void onGuildMemberUpdatePending(GuildMemberUpdatePendingEvent event) {
        long targetRoleId = CommandManager.autoScreeningRoles.getOrDefault(event.getGuild().getIdLong(), 0L);
        if (targetRoleId > 0L) {
            Role role = event.getGuild().getRoleById(targetRoleId);
            if (role != null && event.getMember() != null)
                event.getGuild().addRoleToMember(event.getMember(), role).reason("User has agreed to Membership Screening requirements").queue(null, e -> { });
        }
    }

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        try {
            Moderation.voiceEvent(event);
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Exception while handling GuildVoiceUpdate event:", e);
        }
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        event.getGuild().loadMembers();
        CrossServerUtils.checkGuildCount(event);
        try {
            String thankyou = "Thank you for using Lum!\nLum has a few features that can be enabled like the Scam Shield.\nIf you would like any of these enabled, use the command `/config` or contact us in Slaynash's Workbench <https://discord.gg/akFkAG2>\nUse the command `l!help` to see the list of commands.";
            if (event.getGuild().getSystemChannel() != null && event.getGuild().getSystemChannel().canTalk()) {
                event.getGuild().getSystemChannel().sendMessage(thankyou).queue(null, m -> System.out.println("Failed to send message in System channel"));
            }
            else {
                net.dv8tion.jda.api.entities.Member owner = event.getGuild().retrieveOwner().complete();
                owner.getUser().openPrivateChannel().flatMap(channel -> channel.sendMessage(thankyou)).queue(null, m -> System.out.println("Failed to open dms with guild owner to send thank you"));
            }
        }
        catch (Exception e) {
            ExceptionUtils.reportException("An error has occurred on guild join:", e);
        }
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        System.out.println("Left " + event.getGuild().getName() + ", now connected to " + JDAManager.getJDA().getGuilds().size() + " guilds");
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        try {
            SlashManager.slashRun(event);
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Exception while handling SlashCommandInteraction event:", e);
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        try {
            SlashManager.buttonClicked(event);
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Exception while handling ButtonInteraction event:", e);
        }
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        ExceptionUtils.processExceptionQueue();
    }

    @Override
    public void onSessionRecreate(@NotNull SessionRecreateEvent event) {
        ExceptionUtils.processExceptionQueue();
    }

    @Override
    public void onSessionResume(@NotNull SessionResumeEvent event) {
        ExceptionUtils.processExceptionQueue();
    }

    @Override
    public void onException(@NotNull ExceptionEvent event) {
        try {
            ExceptionUtils.reportException(
                "Exception while handling JDA event:", event.getCause());
        }
        catch (Exception e) {
            System.err.println("[ERROR] Failed to report exception:");
            e.printStackTrace();
        }
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if (event.getUser().isBot())
            return;
        String report = CommandManager.mlReportChannels.get(event.getGuild().getIdLong());
        if (report == null) return;
        TextChannel reportchannel = event.getGuild().getTextChannelById(report);
        if (reportchannel == null) return;
        String name = Junidecode.unidecode(event.getUser().getName() + event.getUser().getGlobalName()).toLowerCase().replaceAll("[^ a-z]", "");

        boolean foundblacklist = false;
        try {
            ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT `username` FROM `blacklistusername` WHERE `username` = '" + name + "' LIMIT 1");
            foundblacklist = rs.next();
            DBConnectionManagerLum.closeRequest(rs);
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to check blacklisted username: " + name, e);
        }

        String displayName;
        if (event.getUser().getGlobalName() == null || event.getUser().getName().equals(event.getUser().getGlobalName()))
            displayName = event.getUser().getName();
        else
            displayName = event.getUser().getName() + " (" + event.getUser().getGlobalName() + ")";

        if (foundblacklist && event.getGuild().getSelfMember().hasPermission(Permission.KICK_MEMBERS)) {
            reportchannel.sendMessage(displayName + " just joined with a known scam username\nNow kicking " + event.getUser().getId()).setAllowedMentions(Collections.emptyList()).queue();
            event.getMember().kick().reason("Lum: Scammer joined").queue();
        }
        else if (CrossServerUtils.testSlurs(name) || name.contains("discord") || name.contains("developer") || name.contains("hypesquad") || name.contains("academy recruitments")) {
            reportchannel.sendMessage(displayName + " just joined with a sussy name\n" + event.getUser().getId()).setAllowedMentions(Collections.emptyList()).queue();
        }
    }

    @Override
    public void onGuildMemberUpdateNickname(GuildMemberUpdateNicknameEvent event) {
        if (event.getUser().isBot())
            return;
        String report = CommandManager.mlReportChannels.get(event.getGuild().getIdLong());
        if (report == null) return;
        TextChannel reportchannel = event.getGuild().getTextChannelById(report);
        if (reportchannel == null) return;
        if (event.getNewNickname() == null) { //removed nickname
            return;
        }
        String name = Junidecode.unidecode(event.getNewNickname()).toLowerCase().replaceAll("[^ a-z]", "");

        boolean foundblacklist = false;
        try {
            // DBConnectionManagerLum.sendUpdate("INSERT INTO `blacklistusername` (`username`) VALUES (?)", name);
            ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT `username` FROM `blacklistusername` WHERE `username` = ? LIMIT 1", name);
            foundblacklist = rs.next();
            DBConnectionManagerLum.closeRequest(rs);
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to check blacklisted username: " + name, e);
        }

        if (CrossServerUtils.testSlurs(name) || name.contains("discord") || name.contains("developer") || name.contains("hypesquad") || name.contains("academy recruitments")) {
            reportchannel.sendMessage(event.getNewNickname() + " just changed their nickname to a sussy name from " + event.getOldNickname() + "\n" + event.getUser().getId()).setAllowedMentions(Collections.emptyList()).queue();
        }
        if (!event.getGuild().getSelfMember().hasPermission(Permission.KICK_MEMBERS)) return;
        if (foundblacklist) {
            event.getMember().kick().reason("Lum: User changed nickname to known Scam").queue();
        }
    }

    @Override
    public void onUserUpdateName(UserUpdateNameEvent event) {
        if (event.getUser().isBot())
            return;
        List<Guild> mutualGuilds = new ArrayList<>(event.getUser().getMutualGuilds());
        mutualGuilds.removeIf(g -> !CommandManager.mlReportChannels.containsKey(g.getIdLong()));

        String name = Junidecode.unidecode(event.getUser().getName()).toLowerCase().replaceAll("[^ a-z]", "");
        boolean foundblacklist = false;
        try {
            ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT `username` FROM `blacklistusername` WHERE `username` = '" + name + "' LIMIT 1");
            foundblacklist = rs.next();
            DBConnectionManagerLum.closeRequest(rs);
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to check blacklisted username: " + name, e);
        }

        for (Guild guild : mutualGuilds) {
            String report = CommandManager.mlReportChannels.get(guild.getIdLong());
            TextChannel reportchannel = guild.getTextChannelById(report);
            if (reportchannel == null) return;

            if (foundblacklist && guild.getSelfMember().hasPermission(Permission.KICK_MEMBERS)) {
                reportchannel.sendMessage("Scammer started scamming " + event.getUser().getEffectiveName() + " (" + event.getUser().getId() + ")\nNow kicking!").setAllowedMentions(Collections.emptyList()).queue();
                guild.kick(event.getUser()).reason("Lum: Scammer started scamming").queue();
                return;
            }
            if (CrossServerUtils.testSlurs(name) || name.contains("discord") || name.contains("developer") || name.contains("hypesquad") || name.contains("academy recruitments")) {
                reportchannel.sendMessage(event.getNewName() + " just changed their username from " + event.getOldName() + "\n" + event.getUser().getId()).setAllowedMentions(Collections.emptyList()).queue();
            }
        }
    }

    @Override
    public void onGuildUpdateOwner(GuildUpdateOwnerEvent event) {
        if (event.getGuild().getSystemChannel().canTalk()) {
            event.getGuild().getSystemChannel().sendMessage(event.getNewOwner().getEffectiveName() + " is the new owner of " + event.getGuild().getName()).queue();
        }
    }

    @Override
    public void onUserTyping(@NotNull UserTypingEvent event) {
        MessageProxy.proxyTyping(event);
    }
}
