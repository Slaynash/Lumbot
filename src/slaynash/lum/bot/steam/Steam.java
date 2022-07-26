package slaynash.lum.bot.steam;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import in.dragonbra.javasteam.enums.EResult;
import in.dragonbra.javasteam.steam.handlers.steamapps.PICSChangeData;
import in.dragonbra.javasteam.steam.handlers.steamapps.PICSProductInfo;
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
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.entities.TextChannel;
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

    private static final Map<Integer, SteamAppDetails> gameDetails = new HashMap<>();
    private static final CopyOnWriteArrayList<Integer> intGameIDs = new CopyOnWriteArrayList<>();

    public Steam() {

        try {
            ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT `value` FROM `Config` WHERE `setting` = 'LastSteamChange' LIMIT 1");
            rs.next();
            previousChangeNumber = rs.getInt("value");
            DBConnectionManagerLum.closeRequest(rs);
        }
        catch (SQLException e) {
            ExceptionUtils.reportException("Failed to initialize last steam depos change#", e);
        }

        client = new SteamClient();
        user = client.getHandler(SteamUser.class);
        apps = client.getHandler(SteamApps.class);

        callbackManager = new CallbackManager(client);
        callbackManager.subscribe(ConnectedCallback.class, callback -> {
            System.out.println("Connected to Steam, logging in...");
            user.logOnAnonymous();
        });
        callbackManager.subscribe(DisconnectedCallback.class, callback -> {
            if (Main.isShuttingDown)
                return;

            if (isLoggedOn) {
                isLoggedOn = false;
                ++tickerHash;
            }

            System.out.println("Disconnected from Steam. Retrying in 5s...");

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

            try { //initialize all depos
                ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT DISTINCT `GameID` FROM `SteamWatch` WHERE 1");
                while (rs.next()) {
                    Integer gameID = rs.getInt("GameID");
                    if (gameDetails.get(gameID) == null)
                        apps.picsGetProductInfo(gameID, null, false, false);
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
        });
        callbackManager.subscribe(PICSChangesCallback.class, callback -> {
            for (Integer intGameID : intGameIDs) {
                apps.picsGetProductInfo(intGameID, null, false, false);
                intGameIDs.remove(intGameID);
            }
            if (previousChangeNumber == callback.getCurrentChangeNumber())
                return;

            System.out.println("Changelist " + previousChangeNumber + " -> " + callback.getCurrentChangeNumber() + " (" + callback.getAppChanges().size() + " apps, " + callback.getPackageChanges().size() + " packages)");

            previousChangeNumber = callback.getCurrentChangeNumber();

            for (Entry<Integer, PICSChangeData> changeDataPair : callback.getAppChanges().entrySet()) {
                Integer gameID = changeDataPair.getKey();
                List<SteamChannel> channels = new ArrayList<>();
                System.out.println("" + gameID + ": " + changeDataPair.getValue().getId());
                try {
                    ResultSet rs = DBConnectionManagerLum.sendRequest("CALL `GetSteamWatch`(?,?)", previousChangeNumber, gameID);
                    while (rs.next()) {
                        channels.add(new SteamChannel(rs.getString("GameID"), rs.getString("ServerID"), rs.getString("ChannelID"), rs.getString("publicMention"), rs.getString("betaMention"), rs.getString("otherMention")));
                    }
                    DBConnectionManagerLum.closeRequest(rs);
                }
                catch (SQLException e) {
                    ExceptionUtils.reportException("Failed to initialize all steam depos", e);
                }
                if (channels.size() != 0) {
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setTitle("New Steam changelist from " + getGameName(gameID) + " (#" + changeDataPair.getValue().getChangeNumber() + ")", "https://steamdb.info/app/" + gameID + "/history/?changeid=" + changeDataPair.getValue().getChangeNumber());

                    for (SteamChannel sc : channels) {
                        if (testChannel(sc))
                            continue;
                        Guild guild = JDAManager.getJDA().getGuildById(sc.guildID);
                        TextChannel channel = guild.getTextChannelById(sc.channelId);
                        if (channel.canTalk())
                            channel.sendMessageEmbeds(eb.build()).queue(s -> {
                                if (channel.isNews())
                                    s.crosspost().queue();
                            });
                        else
                            System.out.println("Lum can't talk in " + guild.getName() + " " + channel.getName());
                    }
                    apps.picsGetProductInfo(gameID, null, false, false);
                }
            }
        });
        callbackManager.subscribe(PICSProductInfoCallback.class, callback -> {
            System.out.println("[PICSProductInfoCallback] apps: ");
            for (Entry<Integer, PICSProductInfo> app : callback.getApps().entrySet()) {
                System.out.println("[PICSProductInfoCallback]  - (" + app.getKey() + ") " + app.getValue().getChangeNumber());
                List<SteamChannel> channels = new ArrayList<>();
                try {
                    ResultSet rs = DBConnectionManagerLum.sendRequest("CALL `GetSteamWatch`(?,?)", previousChangeNumber, app.getKey());
                    while (rs.next()) {
                        channels.add(new SteamChannel(rs.getString("GameID"), rs.getString("ServerID"), rs.getString("ChannelID"), rs.getString("publicMention"), rs.getString("betaMention"), rs.getString("otherMention")));
                    }
                    DBConnectionManagerLum.closeRequest(rs);
                }
                catch (SQLException e) {
                    ExceptionUtils.reportException("Failed to initialize all steam depos", e);
                }

                printKeyValue(app.getValue().getKeyValues(), 1);
                SteamAppDetails gameDetail = gameDetails.get(app.getKey());

                if (gameDetail == null) { //for startup and first time added
                    gameDetail = new SteamAppDetails(app.getValue().getKeyValues());
                    gameDetails.put(app.getKey(), gameDetail);
                    return;
                }

                if (channels.size() == 0)
                    return;

                SteamAppDetails newAppDetails = new SteamAppDetails(app.getValue().getKeyValues());
                SteamAppDetails appChanges = SteamAppDetails.compare(gameDetail, newAppDetails);

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
                            description.append("[").append(changedBranch.getKey()).append("] Branch created (`#").append(branchDetails.buildid).append("`)\n");
                            if (branchDetails.description != null && !branchDetails.description.isBlank())
                                description.append(" - Description: ").append(branchDetails.description).append("\n");
                            if (branchDetails.pwdrequired == null || !branchDetails.pwdrequired) {
                                description.append(" - This is a public branch").append("\n");
                                isBetaBranchUpdate = true;
                            }
                            if (changedBranch.getKey().equals("public")) {
                                isPublicBranchUpdate = true;
                                isBetaBranchUpdate = false;
                            }
                        }
                        else if (!newBranches.containsKey(changedBranch.getKey())) {
                            description.append("[").append(changedBranch.getKey()).append("] Branch deleted\n");
                            if (changedBranch.getValue().pwdrequired == null || !changedBranch.getValue().pwdrequired)
                                description.append(" - This was a public branch").append("\n");
                        }
                        else {
                            SteamAppDetails.SteamAppBranch oldBranchDetails = oldBranches.get(changedBranch.getKey());
                            SteamAppDetails.SteamAppBranch newBranchDetails = newBranches.get(changedBranch.getKey());
                            description.append("[").append(changedBranch.getKey()).append("] Branch ").append(oldBranchDetails.buildid < newBranchDetails.buildid ? "updated" : "downgraded").append(" (`").append(oldBranchDetails.buildid).append("` -> `").append(newBranchDetails.buildid).append("`)\n");
                            if (newBranchDetails.description != null && !newBranchDetails.description.isBlank()) // I don't think this is ever null but nice to have
                                description.append(" - Description: ").append(newBranchDetails.description).append("\n");
                            if (newBranchDetails.pwdrequired == null || !newBranchDetails.pwdrequired) {
                                description.append(" - This is a public branch").append("\n");
                                isBetaBranchUpdate = true;
                            }
                            if (changedBranch.getKey().equals("public")) {
                                isPublicBranchUpdate = true;
                                isBetaBranchUpdate = false;
                            }
                        }
                    }
                    eb.setDescription(new String(description.toString().getBytes(), StandardCharsets.UTF_8));
                    MessageBuilder mb = new MessageBuilder();
                    mb.setEmbeds(eb.build());

                    for (SteamChannel sc : channels) {
                        if (isPublicBranchUpdate && sc.publicMessage != null)
                            mb.setContent(sc.publicMessage);
                        if (isBetaBranchUpdate && sc.betaMessage != null)
                            mb.setContent(sc.betaMessage);
                        if (!isPublicBranchUpdate && !isBetaBranchUpdate && sc.otherMessage != null) {
                            mb.setContent(sc.otherMessage);
                        }

                        TextChannel channel = JDAManager.getJDA().getGuildById(sc.guildID).getTextChannelById(sc.channelId);
                        if (channel.canTalk())
                            channel.sendMessage(mb.build()).allowedMentions(Arrays.asList(MentionType.values())).queue(s -> {
                                if (channel.isNews())
                                    s.crosspost().queue();
                            });
                        mb.setContent("");
                    }
                }
                if (gameDetail != null && newAppDetails != null && gameDetail.common != null && newAppDetails.common != null) {

                    SteamAppDetailsCommon oldCommon = gameDetail.common;
                    SteamAppDetailsCommon newCommon = newAppDetails.common;

                    if (oldCommon.review_percentage != null && newCommon.review_percentage != null) {
                        EmbedBuilder eb = new EmbedBuilder();
                        eb.setTitle(gameDetail.common.name + " Review Percentage " + (Integer.parseInt(oldCommon.review_percentage) > Integer.parseInt(newCommon.review_percentage) ? "decreased" : "increased"));
                        eb.setDescription(oldCommon.review_percentage + " -> " + newCommon.review_percentage);
                        MessageBuilder mb = new MessageBuilder();
                        mb.setEmbeds(eb.build());

                        for (SteamChannel sc : channels) {
                            TextChannel channel = JDAManager.getJDA().getGuildById(sc.guildID).getTextChannelById(sc.channelId);
                            if (channel.canTalk())
                                channel.sendMessage(mb.build()).allowedMentions(Arrays.asList(MentionType.values())).queue(s -> {
                                    if (channel.isNews())
                                        s.crosspost().queue();
                                });
                            mb.setContent("");
                        }
                    }
                }
                if (newAppDetails != null)
                    gameDetails.put(app.getKey(), newAppDetails);
            }
        });
    }

    private static boolean testChannel(SteamChannel sc) {
        if (JDAManager.getJDA() == null)
            return true;
        if (!JDAManager.getJDA().getStatus().equals(JDA.Status.CONNECTED))
            return true;
        Guild guild = JDAManager.getJDA().getGuildById(sc.guildID);
        if (guild == null) {
            System.out.println("Steam can not find Guild " + sc.guildID);
            try {
                DBConnectionManagerLum.sendUpdate("DELETE FROM `SteamWatch` WHERE `ServerID` = ?", sc.guildID);
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
            return true;
        }
        TextChannel channel = guild.getTextChannelById(sc.channelId);
        if (channel == null) {
            System.out.println("Steam can not find Channel " + sc.channelId + " from guild " + sc.guildID);
            try {
                DBConnectionManagerLum.sendUpdate("DELETE FROM `SteamWatch` WHERE `ChannelID` = ?", sc.channelId);
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    private static void printKeyValue(KeyValue keyvalue, int depth) {
        if (keyvalue.getChildren().size() == 0)
            System.out.println("[PICSProductInfoCallback] " + " ".repeat(depth * 4) + " " + keyvalue.getName() + ": " + keyvalue.getValue());
        else {
            System.out.println("[PICSProductInfoCallback] " + " ".repeat(depth * 4) + " " + keyvalue.getName() + ":");
            for (KeyValue child : keyvalue.getChildren())
                printKeyValue(child, depth + 1);
        }

    }

    public void start() {
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
        if (!gameDetails.containsKey(gameID))
            intGameIDs.add(gameID);
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
        if (gameDetails.containsKey(gameID)) {
            SteamAppDetails appDetails = gameDetails.get(gameID);
            if (appDetails.common != null && appDetails.common.name != null)
                return new String(appDetails.common.name.getBytes(), StandardCharsets.UTF_8);
            else
                return gameID.toString();
        }
        intDetails(gameID);
        long timestamp = System.currentTimeMillis();
        while (!gameDetails.containsKey(gameID) && System.currentTimeMillis() - timestamp < 6900) {
            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (!gameDetails.containsKey(gameID)) //timed out
            return gameID.toString();
        SteamAppDetails appDetails = gameDetails.get(gameID);
        if (appDetails.common != null && appDetails.common.name != null)
            return new String(appDetails.common.name.getBytes(), StandardCharsets.UTF_8);
        return gameID.toString();
    }
}
