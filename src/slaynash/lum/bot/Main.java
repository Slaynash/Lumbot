package slaynash.lum.bot;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.ExceptionEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
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
import org.jetbrains.annotations.NotNull;
import slaynash.lum.bot.api.API;
import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.discord.JDAManager;
import slaynash.lum.bot.discord.Members;
import slaynash.lum.bot.discord.Memes;
import slaynash.lum.bot.discord.MessageProxy;
import slaynash.lum.bot.discord.Moderation;
import slaynash.lum.bot.discord.PrivateMessagesHandler;
import slaynash.lum.bot.discord.ReactionListener;
import slaynash.lum.bot.discord.ScamShield;
import slaynash.lum.bot.discord.ServerMessagesHandler;
import slaynash.lum.bot.discord.VRCApiVersionScanner;
import slaynash.lum.bot.discord.VerifyPair;
import slaynash.lum.bot.discord.melonscanner.FetchMelonLoaderVersions;
import slaynash.lum.bot.discord.melonscanner.MelonScanner;
import slaynash.lum.bot.discord.slashs.SlashManager;
import slaynash.lum.bot.discord.utils.CrossServerUtils;
import slaynash.lum.bot.log.LogSystem;
import slaynash.lum.bot.steam.Steam;
import slaynash.lum.bot.timers.Anime;
import slaynash.lum.bot.timers.ClearDMs;
import slaynash.lum.bot.timers.Reminders;
import slaynash.lum.bot.utils.ExceptionUtils;
import slaynash.lum.bot.utils.Utils;
import slaynash.lum.bot.uvm.UnityVersionMonitor;


public class Main extends ListenerAdapter {
    public static boolean isShuttingDown = false;

    public static void main(String[] args) throws Exception {
        System.out.println("Starting Lum...");
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> ExceptionUtils.reportException("Exception in thread " + thread.getName() + ":", throwable));
        LogSystem.init();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            isShuttingDown = true;

            JDA jda = JDAManager.getJDA();
            if (jda != null && JDAManager.isMainBot()) // Lum (blue)
                jda.getTextChannelById(808076226064941086L)
                    .sendMessageEmbeds(Utils.wrapMessageInEmbed("Lum is shutting down", Color.orange))
                    .complete();

            MelonScanner.shutdown();
        }));

        ConfigManager.init();
        Localization.init();

        DBConnectionManagerLum.init();
        DBConnectionManagerShortUrls.init();

        loadVerifychannelList();
        loadReactionsList();
        loadScreeningRolesList();
        loadAPChannels();
        CrossServerUtils.loadGuildCount();

        API.start();

        MelonScanner.init();

        CommandManager.init();
        JDAManager.init(ConfigManager.discordToken);

        System.out.println("Connected to " + JDAManager.getJDA().getGuilds().size() + " Guilds!");

        SlashManager.registerCommands();

        if (JDAManager.isProductionBot()) { // Lum (blue)
            if (ConfigManager.mainBot) {
                JDAManager.getJDA().getPresence().setStatus(OnlineStatus.ONLINE);
                JDAManager.getJDA().getPresence().setActivity(Activity.watching("melons getting loaded"));
                JDAManager.enableEvents();
                UnityVersionMonitor.start();
                VRCApiVersionScanner.init();
                FetchMelonLoaderVersions.start();
                ClearDMs.start();
                Steam.start();
                Reminders.start();
                Anime.start();
            }
            else
                System.out.println("Starting Lum as a backup bot, monitoring main bot...");
        }
        else
            JDAManager.enableEvents();

        if (JDAManager.isProductionBot()) { // Lum (blue)
            Moderation.voiceStartup();
            if (ConfigManager.mainBot) {
                JDAManager.getJDA()
                        .getTextChannelById(808076226064941086L)
                        .sendMessageEmbeds(Utils.wrapMessageInEmbed("Lum restarted successfully!", Color.green))
                        .queue();
            }
        }
        System.out.println("LUM Started!");

        HttpRequest pingCheckRequest = HttpRequest.newBuilder().GET().uri(URI.create(ConfigManager.pingURL)).setHeader("User-Agent", "LUM Bot (https://discord.gg/akFkAG2)").timeout(Duration.ofSeconds(20)).build();

        if (!ConfigManager.mainBot) { // If not the main bot, ping the main bot to see if it is online and if not, take over
            boolean mainBotOnline = true;
            //noinspection InfiniteLoopStatement
            while (true) {
                Thread.sleep(1000 * (mainBotOnline ? 15 : 1));
                if (JDAManager.getJDA().getStatus() != JDA.Status.CONNECTED) {
                    System.out.println("Not Connected to Discord...");
                    continue;
                }
                int statusCode;
                try {
                    statusCode = Utils.downloadRequest(pingCheckRequest, "PingChecker").statusCode();
                }
                catch (Exception e) {
                    statusCode = 0;
                }
                if (statusCode == 200) {
                    System.out.println("PingChecker: Ping successful to main bot, everything is fine");
                    if (JDAManager.isEventsEnabled())
                        JDAManager.disableEvents();
                    if (!mainBotOnline) {
                        JDAManager.getJDA().getGuildById(633588473433030666L).getTextChannelById(1184560349039575152L).sendMessage("Backup is shutting down").queue();
                        mainBotOnline = true;
                    }
                }
                else {
                    //check if internet is available
                    try {
                        URL url = new URL("http://www.google.com");
                        URLConnection connection = url.openConnection();
                        connection.connect();
                        System.out.println("Internet is connected");
                    }
                    catch (Exception e) {
                        System.out.println("Internet is not connected");
                        continue;
                    }

                    if (mainBotOnline) {
                        System.out.println("PingChecker: Ping failed, starting backup...");
                        JDAManager.getJDA().getGuildById(633588473433030666L).getTextChannelById(1184560349039575152L).sendMessage("Backup can't contact Lum and starting up").queue();
                        mainBotOnline = false;
                    }
                    if (!JDAManager.isEventsEnabled())
                        JDAManager.enableEvents();
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

        //TODO: send log message
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        MessageProxy.deletes(event);
        ScamShield.checkDeleted(event);

        //TODO: send log message
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
        Utils.sendEmbed(Utils.wrapMessageInEmbed(message, Color.gray), CommandManager.getModReportChannels(guild, "role"));
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
        Members.loadAllUsers(event.getGuild());
        CrossServerUtils.checkGuildCount(event);
        try {
            String thankyou = "Thank you for using Lum!\nLum has a few features that can be enabled like the Scam Shield.\nIf you would like any of these enabled, use the command `/config` or contact us in Slaynash's Workbench <https://discord.gg/akFkAG2>\nUse the command `" + ConfigManager.discordPrefix + "help` to see the list of commands.";
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
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        Members.logMemberJoin(event);
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        Members.logMemberLeave(event);
    }

    @Override
    public void onGuildMemberUpdateNickname(@NotNull GuildMemberUpdateNicknameEvent event) {
        Members.logUpdateNickname(event);
    }

    @Override
    public void onUserUpdateName(@NotNull UserUpdateNameEvent event) {
        Members.logUsernameChange(event);
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
