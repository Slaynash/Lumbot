package slaynash.lum.bot.steam;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.gson.Gson;
import in.dragonbra.javasteam.enums.EResult;
import in.dragonbra.javasteam.steam.handlers.steamapps.PICSChangeData;
import in.dragonbra.javasteam.steam.handlers.steamapps.PICSProductInfo;
import in.dragonbra.javasteam.steam.handlers.steamapps.PICSRequest;
import in.dragonbra.javasteam.steam.handlers.steamapps.SteamApps;
import in.dragonbra.javasteam.steam.handlers.steamapps.callback.PICSChangesCallback;
import in.dragonbra.javasteam.steam.handlers.steamapps.callback.PICSProductInfoCallback;
import in.dragonbra.javasteam.steam.handlers.steamuser.SteamUser;
import in.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOffCallback;
import in.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback;
import in.dragonbra.javasteam.steam.steamclient.SteamClient;
import in.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager;
import in.dragonbra.javasteam.steam.steamclient.callbacks.ConnectedCallback;
import in.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback;
import in.dragonbra.javasteam.types.KeyValue;
import in.dragonbra.javasteam.util.NetHookNetworkListener;
import in.dragonbra.javasteam.util.log.LogListener;
import in.dragonbra.javasteam.util.log.LogManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import slaynash.lum.bot.DBConnectionManagerLum;
import slaynash.lum.bot.Main;
import slaynash.lum.bot.discord.JDAManager;
import slaynash.lum.bot.steam.SteamAppDetails.SteamAppDetailsCommon;
import slaynash.lum.bot.utils.ExceptionUtils;

public class Steam {
    public static final String LOG_IDENTIFIER = "Steam";

    private final CallbackManager callbackManager;
    private final SteamClient client;
    private final SteamUser user;
    private final SteamApps apps;
    private boolean isLoggedOn;
    private int tickerHash;

    private int previousChangeNumber;

    private static final CopyOnWriteArrayList<Integer> intGameIDs = new CopyOnWriteArrayList<>();

    public Steam() {

        LogManager.addListener(new MyListener());
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("storage/previousSteamChange.txt"));
            previousChangeNumber = Integer.parseInt(reader.readLine());
            reader.close();
        }
        catch (IOException e) {
            ExceptionUtils.reportException("Failed to load previousSteamChange", e);
        }

        client = new SteamClient();
        user = client.getHandler(SteamUser.class);
        apps = client.getHandler(SteamApps.class);

        client.setDebugNetworkListener(new NetHookNetworkListener("SteamLogs"));
        callbackManager = new CallbackManager(client);
        callbackManager.subscribe(ConnectedCallback.class, callback -> {
            System.out.println("Connected to Steam, logging in...");
            user.logOnAnonymous();
        });
        callbackManager.subscribe(DisconnectedCallback.class, callback -> {
            System.out.println("Disconnected from Steam. Retrying in 5s...");
            if (Main.isShuttingDown)
                return;

            if (isLoggedOn) {
                isLoggedOn = false;
                ++tickerHash;
            }

            try {
                Thread.sleep(5 * 1000);
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            client.connect();
        });
        callbackManager.subscribe(LoggedOnCallback.class, callback -> {
            EResult result = callback.getResult();
            if (result != EResult.OK) {
                if (result == EResult.ServiceUnavailable || result == EResult.Timeout || result == EResult.TryAnotherCM) {
                    ExceptionUtils.reportException("Steam: " + result + " Retrying in a min...");
                    client.disconnect();
                    try {
                        Thread.sleep(60 * 1000);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    client.connect();
                }
                else {
                    ExceptionUtils.reportException("Failed to login to Steam: " + result);
                    client.disconnect();
                }
                return;
            }

            isLoggedOn = true;
            System.out.println("Logged in, current valve time is " + callback.getServerTime() + " UTC");

            try { //initialize all missing depos
                ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT DISTINCT s.`GameID` FROM `SteamWatch` s WHERE s.`GameID` NOT IN (SELECT `GameID` FROM `SteamApp`)");
                while (rs.next()) {
                    int gameID = rs.getInt("GameID");
                    apps.picsGetProductInfo(new PICSRequest(gameID), null);
                }
                DBConnectionManagerLum.closeRequest(rs);
            }
            catch (SQLException e) {
                ExceptionUtils.reportException("Failed to initialize all steam depos", e);
            }
            startChangesRequesterThread();
        });
        callbackManager.subscribe(LoggedOffCallback.class, callback -> {
            if (isLoggedOn) {
                isLoggedOn = false;
                ++tickerHash;
            }

            System.out.println("Logged off from Steam");
            if (callback.getResult() == EResult.TryAnotherCM || callback.getResult() == EResult.ServiceUnavailable)
                client.connect();
        });
        callbackManager.subscribe(PICSChangesCallback.class, callback -> {
            for (Integer intGameID : intGameIDs) {
                apps.picsGetProductInfo(new PICSRequest(intGameID), null);
                intGameIDs.remove(intGameID);
            }
            if (previousChangeNumber == callback.getCurrentChangeNumber())
                return;

            System.out.println("Changelist " + previousChangeNumber + " -> " + callback.getCurrentChangeNumber() + " (" + callback.getAppChanges().size() + " apps, " + callback.getPackageChanges().size() + " packages)");

            previousChangeNumber = callback.getCurrentChangeNumber();

            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("storage/previousSteamChange.txt"))) {
                writer.write(String.valueOf(previousChangeNumber));
            }
            catch (IOException e) {
                ExceptionUtils.reportException("Failed to save previousSteamChange", e);
            }

            for (Entry<Integer, PICSChangeData> changeDataPair : callback.getAppChanges().entrySet()) {
                Integer gameID = changeDataPair.getKey();
                List<SteamChannel> channels = new ArrayList<>();
                System.out.println(gameID + ": " + changeDataPair.getValue().getId());
                try {
                    ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT * FROM `SteamWatch` WHERE `SteamWatch`.GameID = ?", gameID);
                    while (rs.next()) {
                        channels.add(new SteamChannel(rs.getString("GameID"), rs.getString("ServerID"), rs.getString("ChannelID"), rs.getString("publicMention"), rs.getString("betaMention"), rs.getString("otherMention")));
                    }
                    DBConnectionManagerLum.closeRequest(rs);
                }
                catch (SQLException e) {
                    ExceptionUtils.reportException("Failed to fetch SteamWatch in Changes", e);
                    continue;
                }
                if (!JDAManager.isEventsEnabled()) {
                    System.out.println("Steam sees Events disabled");
                    continue;
                }
                if (!channels.isEmpty()) {
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setTitle("New Steam changelist from " + getGameName(gameID) + " (#" + changeDataPair.getValue().getChangeNumber() + ")", "https://steamdb.info/app/" + gameID + "/history/?changeid=" + changeDataPair.getValue().getChangeNumber());

                    for (SteamChannel sc : channels) {
                        if (testChannel(sc))
                            continue;
                        Guild guild = JDAManager.getJDA().getGuildById(sc.guildID());
                        if (guild == null) { // kinda useless since we already tested it in testChannel but whatever
                            System.out.println("Steam can not find Guild " + sc.guildID());
                            continue;
                        }
                        MessageChannel channel = (MessageChannel) guild.getGuildChannelById(sc.channelId());
                        if (channel == null) { // kinda useless but whatever
                            System.out.println("Steam can not find Channel " + sc.channelId() + " from guild " + sc.guildID());
                            continue;
                        }
                        if (channel.canTalk())
                            if (!guild.getSelfMember().hasPermission((GuildChannel) channel, Permission.MESSAGE_EMBED_LINKS)) {
                                channel.sendMessage("I need the `Embed Links` permission to send this message").queue();
                            }
                            else channel.sendMessageEmbeds(eb.build()).setAllowedMentions(Arrays.asList(MentionType.values())).queue(s -> {
                                if (channel.getType() == ChannelType.NEWS)
                                    s.crosspost().queue();
                            });
                        else
                            System.out.println("Lum can't talk in " + guild.getName() + " " + channel.getName());
                    }
                    apps.picsGetProductInfo(new PICSRequest(gameID), null);
                }
            }
        });
        callbackManager.subscribe(PICSProductInfoCallback.class, callback -> {
            System.out.println("[PICSProductInfoCallback] apps: ");
            for (Entry<Integer, PICSProductInfo> app : callback.getApps().entrySet()) {
                System.out.println("[PICSProductInfoCallback]  - (" + app.getKey() + ") " + app.getValue().getChangeNumber());
                List<SteamChannel> channels = new ArrayList<>();
                try {
                    ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT * FROM `SteamWatch` WHERE `SteamWatch`.GameID = ?", app.getKey());
                    while (rs.next()) {
                        channels.add(new SteamChannel(rs.getString("GameID"), rs.getString("ServerID"), rs.getString("ChannelID"), rs.getString("publicMention"), rs.getString("betaMention"), rs.getString("otherMention")));
                    }
                    DBConnectionManagerLum.closeRequest(rs);
                }
                catch (SQLException e) {
                    ExceptionUtils.reportException("Failed to fetch SteamWatch in Info", e);
                    continue;
                }

                printKeyValue(app.getValue().getKeyValues(), 1);
                SteamAppDetails gameDetail = getGameDetails(app.getKey());


                if (gameDetail == null) { //for startup and first time added
                    System.out.println("First time added " + app.getKey() + " " + app.getValue().getChangeNumber());
                    gameDetail = new SteamAppDetails(app.getValue().getKeyValues());
                    setGameDetails(app.getKey(), gameDetail);
                    return;
                }

                if (channels.isEmpty()) {
                    System.out.println("No channels for " + app.getKey());
                    return;
                }

                if (!JDAManager.isEventsEnabled())
                    return;

                SteamAppDetails newAppDetails = new SteamAppDetails(app.getValue().getKeyValues());
                SteamAppDetails appChanges = SteamAppDetails.compare(gameDetail, newAppDetails);

                if (appChanges == null)
                    System.out.println("No changes for " + app.getKey());

                if (appChanges != null && appChanges.depots != null && appChanges.depots.branches != null) {

                    Map<String, SteamAppDetails.SteamAppBranch> oldBranches = gameDetail.depots.branches;
                    Map<String, SteamAppDetails.SteamAppBranch> newBranches = newAppDetails.depots.branches;
                    Map<String, SteamAppDetails.SteamAppBranch> changeBranches = appChanges.depots.branches;

                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setTitle(new String(gameDetail.common.name.getBytes(), StandardCharsets.UTF_8) + " Depot" + (changeBranches.size() > 1 ? "s" : "") + " changed");
                    StringBuilder description = new StringBuilder();
                    boolean isPublicBranchUpdate = false;
                    boolean isBetaBranchUpdate = false;
                    for (Entry<String, SteamAppDetails.SteamAppBranch> changedBranch : changeBranches.entrySet()) {
                        if (!oldBranches.containsKey(changedBranch.getKey())) {
                            SteamAppDetails.SteamAppBranch branchDetails = newBranches.get(changedBranch.getKey());
                            description.append("[`").append(changedBranch.getKey()).append("`] Branch created (`#").append(branchDetails.buildid).append("`)\n");
                            if (branchDetails.description != null && !branchDetails.description.isBlank())
                                description.append("- Description: ").append(branchDetails.description).append("\n");
                            if (branchDetails.pwdrequired == null || !branchDetails.pwdrequired) {
                                description.append("- This is a public branch").append("\n");
                                isBetaBranchUpdate = true;
                            }
                            if (changedBranch.getKey().equals("public")) {
                                isPublicBranchUpdate = true;
                                isBetaBranchUpdate = false;
                            }
                        }
                        else if (!newBranches.containsKey(changedBranch.getKey())) {
                            description.append("[`").append(changedBranch.getKey()).append("`] Branch deleted\n");
                            if (changedBranch.getValue().pwdrequired == null || !changedBranch.getValue().pwdrequired)
                                description.append("- This was a public branch").append("\n");
                        }
                        else {
                            SteamAppDetails.SteamAppBranch oldBranchDetails = oldBranches.get(changedBranch.getKey());
                            SteamAppDetails.SteamAppBranch newBranchDetails = newBranches.get(changedBranch.getKey());
                            String grade = oldBranchDetails.buildid < newBranchDetails.buildid ? "upgraded" : "downgraded";
                            if (oldBranchDetails.buildid == newBranchDetails.buildid) grade = "updated";
                            description.append("[`").append(changedBranch.getKey()).append("`] Branch ").append(grade).append(" (`").append(oldBranchDetails.buildid).append("` -> `").append(newBranchDetails.buildid).append("`)\n");
                            if (newBranchDetails.description != null && !newBranchDetails.description.isBlank()) // I don't think this is ever null but nice to have
                                description.append("- Description: ").append(newBranchDetails.description).append("\n");
                            if (newBranchDetails.pwdrequired == null || !newBranchDetails.pwdrequired) {
                                description.append("- This is a public branch\n");
                                isBetaBranchUpdate = true;
                            }
                            if (changedBranch.getKey().equals("public")) {
                                isPublicBranchUpdate = true;
                                isBetaBranchUpdate = false;
                            }
                        }
                    }
                    if (description.length() > 4096)
                        eb.setDescription(new String(description.substring(0, 4093).concat("...").getBytes(), StandardCharsets.UTF_8));
                    else
                        eb.setDescription(new String(description.toString().getBytes(), StandardCharsets.UTF_8));
                    MessageCreateBuilder mb = new MessageCreateBuilder();
                    mb.setEmbeds(eb.build());

                    for (SteamChannel sc : channels) {
                        if (testChannel(sc))
                            continue;

                        mb.setContent("");
                        if (isPublicBranchUpdate && sc.publicMessage() != null)
                            mb.setContent(sc.publicMessage());
                        if (isBetaBranchUpdate && sc.betaMessage() != null)
                            mb.setContent(sc.betaMessage());
                        if (!isPublicBranchUpdate && !isBetaBranchUpdate && sc.otherMessage() != null) {
                            mb.setContent(sc.otherMessage());
                        }

                        MessageChannel channel;
                        try {
                            channel = (MessageChannel) JDAManager.getJDA().getGuildById(sc.guildID()).getGuildChannelById(sc.channelId());
                        }
                        catch (Exception e) {
                            ExceptionUtils.reportException("Failed to get guild " + sc.guildID()  + " for Info", e);
                            continue;
                        }
                        if (channel != null && channel.canTalk() && JDAManager.getJDA().getGuildById(sc.guildID()).getSelfMember().hasPermission((GuildChannel) channel, Permission.MESSAGE_EMBED_LINKS))
                            channel.sendMessage(mb.build()).setAllowedMentions(Arrays.asList(MentionType.values())).queue(s -> {
                                if (channel.getType() == ChannelType.NEWS)
                                    s.crosspost().queue();
                            });
                    }
                }
                if (gameDetail.common != null && newAppDetails.common != null) {

                    SteamAppDetailsCommon oldCommon = gameDetail.common;
                    SteamAppDetailsCommon newCommon = newAppDetails.common;
                    EmbedBuilder eb = new EmbedBuilder();
                    if (oldCommon.review_percentage != null && newCommon.review_percentage != null && !oldCommon.review_percentage.equals(newCommon.review_percentage)) {
                        eb.setTitle(gameDetail.common.name + " Review Percentage " + (Integer.parseInt(oldCommon.review_percentage) > Integer.parseInt(newCommon.review_percentage) ? "decreased" : "increased"));
                        eb.setDescription(oldCommon.review_percentage + " -> " + newCommon.review_percentage);
                    }
                    if (oldCommon.store_tags != null && newCommon.store_tags != null && !oldCommon.store_tags.equals(newCommon.store_tags)) {
                        if (new HashSet<>(oldCommon.store_tags).containsAll(newCommon.store_tags) && new HashSet<>(newCommon.store_tags).containsAll(oldCommon.store_tags)) {
                            eb.setTitle(gameDetail.common.name + " Store Tags updated");
                            eb.setDescription("tags only switched places");
                        }
                        else if (oldCommon.store_tags.size() > newCommon.store_tags.size()) {
                            eb.setTitle(gameDetail.common.name + " Store Tags removed");
                            oldCommon.store_tags.removeAll(newCommon.store_tags);
                            eb.setDescription(String.join("\n", oldCommon.store_tags));
                        }
                        else if (oldCommon.store_tags.size() < newCommon.store_tags.size()) {
                            eb.setTitle(gameDetail.common.name + " Store Tags added");
                            newCommon.store_tags.removeAll(oldCommon.store_tags);
                            eb.setDescription(String.join("\n", newCommon.store_tags));
                        }
                        else {
                            ExceptionUtils.reportException("");
                        }
                    }
                    if (!eb.isEmpty()) {
                        MessageCreateBuilder mb = new MessageCreateBuilder();
                        mb.setEmbeds(eb.build());

                        for (SteamChannel sc : channels) {
                            if (testChannel(sc))
                                continue;
                            MessageChannel channel;
                            try {
                                channel = (MessageChannel) JDAManager.getJDA().getGuildById(sc.guildID()).getGuildChannelById(sc.channelId());
                            }
                            catch (Exception e) {
                                ExceptionUtils.reportException("Failed to get guild for reviews", e);
                                continue;
                            }
                            if (channel == null) continue;
                            if (channel.canTalk())
                                channel.sendMessage(mb.build()).setAllowedMentions(Arrays.asList(MentionType.values())).queue(s -> {
                                    if (channel.getType() == ChannelType.NEWS)
                                        s.crosspost().queue();
                                });
                            mb.setContent("");
                        }
                    }
                }
                setGameDetails(app.getKey(), newAppDetails);
            }
        });
    }

    // *
    // * @param sc
    // * @return true if channel is invalid
    private static boolean testChannel(SteamChannel sc) {
        if (JDAManager.getJDA() == null) {
            System.out.println("Steam can not find JDA");
            return true;
        }
        if (!JDAManager.getJDA().getStatus().equals(JDA.Status.CONNECTED)) {
            System.out.println("Steam finds JDA is disconnected");
            return true;
        }
        Guild guild = JDAManager.getJDA().getGuildById(sc.guildID());
        if (guild == null) {
            System.out.println("Steam can not find Guild " + sc.guildID());
            // try {
            //     DBConnectionManagerLum.sendUpdate("DELETE FROM `SteamWatch` WHERE `ServerID` = ?", sc.guildID());
            // }
            // catch (SQLException e) {
            //     ExceptionUtils.reportException("Failed to remove missing Guild: " + sc.guildID());
            // }
            return true;
        }
        MessageChannel channel = (MessageChannel) guild.getGuildChannelById(sc.channelId());
        if (channel == null) {
            System.out.println("Steam can not find Channel " + sc.channelId() + " from guild " + sc.guildID());
            // try {
            //     DBConnectionManagerLum.sendUpdate("DELETE FROM `SteamWatch` WHERE `ChannelID` = ?", sc.channelId());
            // }
            // catch (SQLException e) {
            //     ExceptionUtils.reportException("Failed to remove missing Channel: " + sc.channelId());
            // }
            return true;
        }
        return false;
    }

    private static void printKeyValue(KeyValue keyvalue, int depth) {
        if (keyvalue.getChildren().isEmpty())
            System.out.println("[PICSProductInfoCallback] " + " ".repeat(depth * 4) + " " + keyvalue.getName() + ": " + keyvalue.getValue());
        else {
            System.out.println("[PICSProductInfoCallback] " + " ".repeat(depth * 4) + " " + keyvalue.getName() + ":");
            for (KeyValue child : keyvalue.getChildren())
                printKeyValue(child, depth + 1);
        }

    }

    public void start() {
        System.out.println("Starting Steam...");
        client.connect();

        Thread thread = new Thread(() -> {
            while (!Main.isShuttingDown) {
                callbackManager.runWaitCallbacks(5 * 1000);
            }
        }, "Steam Thread");
        thread.setDaemon(true);
        thread.start();
    }

    public void intDetails(Integer gameID) {
        // For some reason, SteamKit ignores picsGetProductInfo if it is called outside of a callback
        try {
            ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT `TS` FROM `SteamApp` WHERE `GameID` = ?", gameID);
            if (!rs.next()) {
                System.out.println("Init SteamApp for " + gameID);
                // apps.picsGetProductInfo(gameID, null, false, false);
                intGameIDs.add(gameID);
            }
            DBConnectionManagerLum.closeRequest(rs);
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to int Steam Details", e);
        }
    }

    private void startChangesRequesterThread() {
        int currentHash = tickerHash;

        Thread thread = new Thread(() -> {
            Random random = new Random();

            System.out.println("PICS ticker started #" + currentHash);

            while (currentHash == tickerHash) {
                apps.picsGetChangesSince(previousChangeNumber, true, true);

                try {
                    Thread.sleep(random.nextInt(3210) + 1000);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

            System.out.println("PICS ticker stopped #" + currentHash);
        }, "PICS ticker #" + currentHash);
        thread.setDaemon(true);
        thread.start();
    }

    // Steam Id to Game name
    public String getGameName(Integer gameID) {
        if (gameID == null)
            return "null";
        SteamAppDetails gameDetail = getGameDetails(gameID);
        if (gameDetail != null) {
            if (gameDetail.common != null && gameDetail.common.name != null)
                return gameDetail.common.name;
            else
                return gameID.toString();
        }
        intDetails(gameID);
        long timestamp = System.currentTimeMillis();
        while ((gameDetail = getGameDetails(gameID)) == null && System.currentTimeMillis() - timestamp < 6900) {
            try {
                Thread.sleep(420);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
                return "error";
            }
        }
        if (gameDetail == null) //timed out
            return gameID.toString();
        if (gameDetail.common != null && gameDetail.common.name != null)
            return gameDetail.common.name;
        return gameID.toString();
    }
    private SteamAppDetails getGameDetails(Integer gameID) {
        if (gameID == null)
            return null;
        SteamAppDetails appDetails = null;
        try {
            ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT * FROM `SteamApp` WHERE `GameID` = ?", gameID);
            if (rs.next()) {
                appDetails = new Gson().fromJson(rs.getString("Depot"), SteamAppDetails.class);
            }
            else
                System.out.println("SteamApp not found for " + gameID);
            DBConnectionManagerLum.closeRequest(rs);
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to get Game Details", e);
        }
        return appDetails;
    }
    private boolean setGameDetails(Integer gameID, SteamAppDetails appDetails) {
        if (gameID == null)
            return false;
        if (appDetails == null)
            return false;
        try {
            String json = new Gson().toJson(appDetails);
            ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT `TS` FROM `SteamApp` WHERE `GameID` = ?", gameID);
            if (rs.next())
                DBConnectionManagerLum.sendUpdate("UPDATE `SteamApp` SET `Depot` = ? WHERE `SteamApp`.`GameID` = ?", json, gameID);
            else
                DBConnectionManagerLum.sendUpdate("INSERT INTO `SteamApp` (`GameID`, `Depot`) VALUES (?, ?)", gameID, json);
            DBConnectionManagerLum.closeRequest(rs);
            return true;
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to set Game Details", e);
            return false;
        }
    }

    static class MyListener implements LogListener {

        // this function will be called when internal steamkit components write to the debuglog
        @Override
        public void onLog(Class<?> clazz, String message, Throwable throwable) {
            // for this example, we'll print the output to the console
            System.out.println("[" + LOG_IDENTIFIER + "] " + clazz.getSimpleName() + ": " + message);
        }

        @Override
        public void onError(Class<?> clazz, String message, Throwable throwable) {
            // for this example, we'll print errors the output to the console
            System.err.println("[" + LOG_IDENTIFIER + "] " + clazz.getSimpleName() + ": " + message);
        }
    }
}
