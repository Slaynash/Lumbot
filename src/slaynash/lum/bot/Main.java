package slaynash.lum.bot;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.security.auth.login.LoginException;

import org.jetbrains.annotations.NotNull;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.ExceptionEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateBoostTimeEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdatePendingEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateMaxMembersEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import slaynash.lum.bot.api.API;
import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.discord.GuildConfigurations;
import slaynash.lum.bot.discord.JDAManager;
import slaynash.lum.bot.discord.Moderation;
import slaynash.lum.bot.discord.PrivateMessagesHandler;
import slaynash.lum.bot.discord.ReactionListener;
import slaynash.lum.bot.discord.ServerChannel;
import slaynash.lum.bot.discord.ServerMessagesHandler;
import slaynash.lum.bot.discord.VRCApiVersionScanner;
import slaynash.lum.bot.discord.VerifyPair;
import slaynash.lum.bot.discord.melonscanner.MLHashPair;
import slaynash.lum.bot.discord.melonscanner.MelonScanner;
import slaynash.lum.bot.discord.slashs.Slash;
import slaynash.lum.bot.discord.utils.CrossServerUtils;
import slaynash.lum.bot.log.LogSystem;
import slaynash.lum.bot.steam.Steam;
import slaynash.lum.bot.utils.ExceptionUtils;
import slaynash.lum.bot.utils.Utils;


public class Main extends ListenerAdapter {
    public static boolean isShuttingDown = false;

    public static void main(String[] args) throws LoginException, IllegalArgumentException, InterruptedException {
        System.out.println("Starting Lum...");
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> ExceptionUtils.reportException("Exception in thread " + thread.getName() + ":", throwable));
        LogSystem.init();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            isShuttingDown = true;

            JDA jda = JDAManager.getJDA();
            if (jda != null && jda.getSelfUser().getIdLong() == 275759980752273418L) // Lum (blue)
            jda
                .getGuildById(633588473433030666L)
                .getTextChannelById(808076226064941086L)
                .sendMessageEmbeds(Utils.wrapMessageInEmbed("Lum is shutting down", Color.orange))
                .complete();

            MelonScanner.shutdown();
        }));

        ConfigManager.init();
        Localization.init();

        DBConnectionManagerShortUrls.init();

        loadSteamWatch();
        new Steam().start();

        loadLogchannelList();
        loadVerifychannelList();
        loadReactionsList();
        loadScreeningRolesList();
        loadMelonLoaderVersions();
        loadMLHashes();
        loadMLVRCHash();
        loadMLReportChannels();
        loadVRCBuild();
        loadGuildConfigs();
        loadAPChannels();
        loadReplies();
        CrossServerUtils.loadGuildCount();

        API.start();

        MelonScanner.init();

        CommandManager.init();
        JDAManager.init(ConfigManager.discordToken);

        JDAManager.getJDA().getPresence().setStatus(OnlineStatus.ONLINE);
        JDAManager.getJDA().getPresence().setActivity(Activity.watching("melons getting loaded"));
        System.out.println("Connected to " + JDAManager.getJDA().getGuilds().size() + " Guilds!");

        if (JDAManager.getJDA().getSelfUser().getIdLong() == 275759980752273418L) // Lum (blue)
            JDAManager.getJDA()
                .getGuildById(633588473433030666L)
                .getTextChannelById(808076226064941086L)
                .sendMessageEmbeds(Utils.wrapMessageInEmbed("Lum restarted successfully !", Color.green))
                .queue();

        try {
            OptionData optionUCBLLIF = new OptionData(OptionType.STRING, "type", "Type d'exercice", true).addChoices(
                new Command.Choice("Conversions binaire", "binconv"),
                new Command.Choice("Boucles", "loops"),
                new Command.Choice("Master Theorem", "mthm"),
                new Command.Choice("Tas", "heap"),
                new Command.Choice("AVL", "avl"),
                new Command.Choice("Table de vérité", "bintable"));
            JDAManager.getJDA().getGuildById(624635229222600717L).upsertCommand("exo", "Génère ou affiche le corrigé d'un exercice")
                .addSubcommands(
                    new SubcommandData("create", "Génère un exercice")
                        .addOptions(optionUCBLLIF)
                    .addOption(OptionType.STRING, "ticket", "Ticket d'identification de l'exercice (optionnel)", false))
                .addSubcommands(new SubcommandData("solve", "Affiche le corrigé d'un exercice")
                    .addOptions(optionUCBLLIF)
                    .addOption(OptionType.STRING, "ticket", "Ticket d'identification de l'exercice", true))
                .setDefaultEnabled(true)
                .queue();
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to upsert UCBL guild commands", e);
        }

        VRCApiVersionScanner.init();
        UnityVersionMonitor.start();

        //registerCommands();
        Moderation.voiceStartup();

        System.out.println("LUM Started!");
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
                if (!line.equals("")) {
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

    private static void loadMLVRCHash() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("storage/mlvrchash.txt"));
            CommandManager.melonLoaderVRCHash = reader.readLine();
            CommandManager.melonLoaderVRCMinDate = reader.readLine();
            reader.close();
        }
        catch (IOException e) {
            ExceptionUtils.reportException("Failed to load VRChat Hash", e);
        }
    }

    private static void loadVRCBuild() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("storage/vrchatbuild.txt"));
            String line = reader.readLine();
            if (line != null)
                CommandManager.vrchatBuild = line.trim();
            else
                CommandManager.vrchatBuild = "1";
            reader.close();
        }
        catch (IOException e) {
            ExceptionUtils.reportException("Failed to load VRChat Build infos", e);
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

    private static void loadGuildConfigs() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("storage/guildconfigurations.txt"));
            String line;
            HashMap<Long, boolean[]> tempMap = new HashMap<>();
            while ((line = reader.readLine()) != null) {
                String[] broke = line.split(" ");
                boolean[] tempBooleans = new boolean[GuildConfigurations.ConfigurationMap.values().length];
                for (int i = 1; i < broke.length; i++)
                    tempBooleans[i - 1] = Boolean.parseBoolean(broke[i]);
                tempMap.put(Long.parseLong(broke[0]), tempBooleans);
            }
            reader.close();
            GuildConfigurations.configurations = tempMap;
        }
        catch (IOException e) {
            ExceptionUtils.reportException("Failed to load Guild Configs", e);
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

    private static void loadSteamWatch() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("storage/steamwatch.txt"));
            String line;
            String[] parts;
            while ((line = reader.readLine()) != null) {
                parts = line.split(",");
                Integer gameID = Integer.parseInt(parts[0]);
                List<ServerChannel> rc = Steam.reportChannels.getOrDefault(gameID, new ArrayList<>());
                rc.add(new ServerChannel(parts[1], parts[2]));
                Steam.reportChannels.put(gameID, rc);
            }
            reader.close();
        }
        catch (IOException e) {
            ExceptionUtils.reportException("Failed to load Steam watch configs", e);
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
    public void onGuildMemberUpdateBoostTime(@NotNull GuildMemberUpdateBoostTimeEvent event) {
        String guildName = event.getGuild().getName();
        String memberName = event.getMember().getEffectiveName();
        OffsetDateTime newBoost = event.getNewTimeBoosted();
        OffsetDateTime oldBoost = event.getOldTimeBoosted();
        event.getJDA().getGuildById(633588473433030666L).getTextChannelById(924927981581905922L).sendMessage(memberName + " boosted " + guildName + " new boost time: " + newBoost + " old boost time: " + oldBoost).queue();
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        if (event.isFromType(ChannelType.PRIVATE)) {
            PrivateMessagesHandler.handle(event);
        }
        else {
            ServerMessagesHandler.handle(event);
        }
    }

    @Override
    public void onGuildMessageReactionAdd(@NotNull GuildMessageReactionAddEvent event) {
        // Don't react to self roles
        if (event.getUser().getIdLong() == JDAManager.getJDA().getSelfUser().getIdLong())
            return;

        // System.out.println("[" + event.getGuild().getName() + "] [#" + event.getChannel().getName() + "] " + event.getUser().getName() + " reacted with " + event.getReactionEmote().getName() + (event.getReactionEmote().isEmote() ? "isEmote id:" + event.getReactionEmote().getId() : "") + (EmojiUtils.containsEmoji(event.getReactionEmote().getName()) ? " is Emoji" : ""));
        for (ReactionListener rl : CommandManager.reactionListeners) {
            if (event.getMessageId().equals(rl.messageId) && (event.getReactionEmote().isEmote() ? event.getReactionEmote().getEmote().getId().equals(rl.emoteId) : event.getReactionEmote().getName().equals(rl.emoteId))) {
                Role role = event.getGuild().getRoleById(rl.roleId);
                if (role != null) {
                    event.getGuild().addRoleToMember(event.getMember(), role).reason("User clicked role reaction").queue();
                    writeLogMessage(event.getGuild(), "Added role `" + role.getName() + "` to " + event.getUser().getAsMention());
                }
                return;
            }
        }
    }

    private static void writeLogMessage(Guild guild, String message) {
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
    public void onGuildMessageReactionRemove(@NotNull GuildMessageReactionRemoveEvent event) {
        for (ReactionListener rl : CommandManager.reactionListeners) {
            if (event.getMessageId().equals(rl.messageId) && (event.getReactionEmote().isEmote() ? event.getReactionEmote().getEmote().getId().equals(rl.emoteId) : event.getReactionEmote().getName().equals(rl.emoteId))) {
                Role role = event.getGuild().getRoleById(rl.roleId);
                if (role != null && event.getUser() != null) {
                    event.getGuild().removeRoleFromMember(event.getUserId(), role).reason("User removed role reaction").queue();
                    writeLogMessage(event.getGuild(), "Removed role `" + role.getName() + "` from " + event.getUser().getAsMention());
                }
                return;
            }
        }
    }

    @Override
    public void onGuildMemberUpdatePending(GuildMemberUpdatePendingEvent event) {
        long targetRoleId = CommandManager.autoScreeningRoles.getOrDefault(event.getGuild().getIdLong(), 0L);
        if (targetRoleId > 0L) {
            Role role = event.getGuild().getRoleById(targetRoleId);
            if (role != null)
                event.getGuild().addRoleToMember(event.getMember(), role).reason("User has agreed to Membership Screening requirements").queue();
        }
    }

    @Override
    public void onGuildVoiceJoin(@NotNull GuildVoiceJoinEvent event) {
        Moderation.voiceJoin(event);
    }

    @Override
    public void onGuildVoiceLeave(@NotNull GuildVoiceLeaveEvent event) {
        Moderation.voiceLeave(event);
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        CrossServerUtils.checkGuildCount(event);
        try {
            String thankyou = "Thank you for using Lum!\nLum has a few features that can be enabled like the Scam Shield.\nIf you would like any of these enabled, use the command `/config` or contact us in Slaynash's Workbench <https://discord.gg/akFkAG2>";
            if (event.getGuild().getSystemChannel() != null && event.getGuild().getSystemChannel().canTalk()) {
                event.getGuild().getSystemChannel().sendMessage(thankyou).queue(null, m -> System.out.println("Failed to send message in System channel"));
            }
            else {
                event.getGuild().getOwner().getUser().openPrivateChannel().flatMap(channel -> channel.sendMessage(thankyou)).queue(null, m -> System.out.println("Failed to open dms with guild owner to send thank you"));
            }
            event.getGuild().upsertCommand("config", "send server config buttons for this guild").setDefaultEnabled(false)
                .queueAfter(10, TimeUnit.SECONDS, g -> event.getGuild().updateCommandPrivilegesById(g.getId(), Moderation.getAdminsPrivileges(event.getGuild())).queue(null, e -> ExceptionUtils.reportException("An error has occurred on guild join:", e))); // register Guild command for newly joined server
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
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        Slash.slashRun(event);
    }

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        Slash.buttonClick(event);
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        ExceptionUtils.processExceptionQueue();
    }
    /*
    private static void registerCommands() {
        Guild loopGuild = null;
        try {
            JDAManager.getJDA().setRequiredScopes("applications.commands"); // incase we use getInviteUrl(permissions)
            // JDAManager.getJDA().updateCommands().addCommands(new CommandData("configs", "send server config buttons").addOption(OptionType.STRING, "guild", "Enter Guild ID", true)).queue(); // Global/DM command
            for (Guild tempGuild : JDAManager.getJDA().getGuilds()) {
                loopGuild = tempGuild;
                try {
                    List<Command> commands = loopGuild.retrieveCommands().complete();
                    if (commands.stream().anyMatch(c -> c.getName().equals("config")))
                        continue;
                    long cmdID = loopGuild.upsertCommand("config", "send server config buttons for this guild").setDefaultEnabled(false).complete().getIdLong(); // Guild command
                    loopGuild.updateCommandPrivilegesById(cmdID, Moderation.getAdminsPrivileges(loopGuild)).queue();
                }
                catch (Exception e) {
                    System.err.println("Failed to register slash command for: " + loopGuild.getName());
                }
            }
        }
        catch (Exception e) {
            ExceptionUtils.reportException(
                "Error registering command for " + loopGuild.getName(), e);
        }
    }
    */
    @Override
    public void onException(@NotNull ExceptionEvent event) {
        try {
            ExceptionUtils.reportException(
                "Exception while handling JDA event:", event.getCause());
        }
        catch (Exception e) {
            System.err.println("Failed to report exception:");
            e.printStackTrace();
        }
    }

    @Override
    public void onGuildUpdateMaxMembers(GuildUpdateMaxMembersEvent event) {
        if (event.getGuild().getSystemChannel().canTalk()) {
            if (event.getNewMaxMembers() > event.getOldMaxMembers()) {
                event.getGuild().getSystemChannel().sendMessage("Yay, the max guild member has been increased to " + event.getNewMaxMembers() + " from " + event.getOldMaxMembers()).queue();
            }
            else {
                event.getGuild().getSystemChannel().sendMessage("The max guild member has been lowered to " + event.getNewMaxMembers() + " from " + event.getOldMaxMembers()).queue();
            }
        }
    }
}
