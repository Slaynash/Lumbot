package slaynash.lum.bot.steam;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

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
import slaynash.lum.bot.Main;
import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.discord.JDAManager;
import slaynash.lum.bot.discord.ServerChannel;
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
    public static final Map<Integer, List<ServerChannel>> reportChannels = new HashMap<>();
    private static List<ServerChannel> channels2Remove;

    public Steam() {

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
            if (callback.getResult() != EResult.OK) {
                if (callback.getResult() == EResult.ServiceUnavailable) {
                    ExceptionUtils.reportException("Steam Service unavailable. Retrying in 5min...");
                    try {
                        Thread.sleep(5 * 60 * 1000);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    client.connect();
                    return;
                }
                else {
                    ExceptionUtils.reportException("Failed to login to Steam: " + callback.getResult());
                    client.disconnect();
                    return;
                }
            }

            isLoggedOn = true;
            System.out.println("Logged in, current valve time is " + callback.getServerTime() + " UTC");

            startChangesRequesterThread();

            for (Integer gameID : reportChannels.keySet()) {
                if (gameDetails.get(gameID) == null)
                    apps.picsGetProductInfo(gameID, null, false, false);
            }
        });
        callbackManager.subscribe(LoggedOffCallback.class, callback -> {
            if (isLoggedOn) {
                isLoggedOn = false;
                ++tickerHash;
            }

            System.out.println("Logged off from Steam");
        });
        callbackManager.subscribe(PICSChangesCallback.class, callback -> {
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
                System.out.println("" + gameID + ": " + changeDataPair.getValue().getId());
                if (reportChannels.containsKey(gameID)) {
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setTitle("New Steam changelist from " + gameID + " (#" + changeDataPair.getValue().getChangeNumber() + ")", "https://steamdb.info/app/" + gameID + "/history/?changeid=" + changeDataPair.getValue().getChangeNumber());

                    channels2Remove = new ArrayList<>();
                    List<ServerChannel> rchannels = reportChannels.get(gameID);
                    for (ServerChannel sc : rchannels) {
                        if (testChannel(sc))
                            continue;
                        Guild guild = JDAManager.getJDA().getGuildById(sc.serverID);
                        TextChannel channel = guild.getTextChannelById(sc.channelId);
                        if (channel.canTalk())
                            channel.sendMessageEmbeds(eb.build()).queue();
                        else
                            System.out.println("Lum can't talk in " + guild.getName() + " " + channel.getName());
                    }
                    removeChannels(gameID);
                    apps.picsGetProductInfo(gameID, null, false, false);
                }
            }
        });
        callbackManager.subscribe(PICSProductInfoCallback.class, callback -> {
            System.out.println("[PICSProductInfoCallback] apps: ");
            for (Entry<Integer, PICSProductInfo> app : callback.getApps().entrySet()) {
                System.out.println("[PICSProductInfoCallback]  - (" + app.getKey() + ") " + app.getValue().getChangeNumber());
                if (!reportChannels.containsKey(app.getKey()))
                    return;

                printKeyValue(app.getValue().getKeyValues(), 1);
                List<ServerChannel> rchannels = reportChannels.get(app.getKey());
                SteamAppDetails gameDetail = gameDetails.get(app.getKey());

                if (gameDetail == null) { //for startup and first time added
                    gameDetail = new SteamAppDetails(app.getValue().getKeyValues());
                    gameDetails.put(app.getKey(), gameDetail);
                    return;
                }

                SteamAppDetails newAppDetails = new SteamAppDetails(app.getValue().getKeyValues());
                SteamAppDetails appChanges = SteamAppDetails.compare(gameDetail, newAppDetails);

                if (appChanges != null && appChanges.depots != null && appChanges.depots.branches != null) {

                    Map<String, SteamAppDetails.SteamAppBranch> oldBranches = gameDetail.depots.branches;
                    Map<String, SteamAppDetails.SteamAppBranch> newBranches = newAppDetails.depots.branches;
                    Map<String, SteamAppDetails.SteamAppBranch> changeBranches = appChanges.depots.branches;

                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setTitle(gameDetail.common.name + " Depot" + (changeBranches.size() > 1 ? "s" : "") + " changed");
                    StringBuilder description = new StringBuilder();
                    boolean isPublicBranchUpdate = false;
                    for (Entry<String, SteamAppDetails.SteamAppBranch> changedBranch : changeBranches.entrySet()) {
                        if (!oldBranches.containsKey(changedBranch.getKey())) {
                            SteamAppDetails.SteamAppBranch branchDetails = newBranches.get(changedBranch.getKey());
                            description.append("[").append(changedBranch.getKey()).append("] Branch created (`#").append(branchDetails.buildid).append("`)\n");
                            if (branchDetails.description != null && !branchDetails.description.isBlank())
                                description.append(" - Description: ").append(branchDetails.description).append("\n");
                            if (branchDetails.pwdrequired == null || !branchDetails.pwdrequired)
                                description.append(" - This is a public branch").append("\n");
                        }
                        else if (!newBranches.containsKey(changedBranch.getKey())) {
                            description.append("[").append(changedBranch.getKey()).append("] Branch deleted\n");
                            if (changedBranch.getValue() == null || !changedBranch.getValue().pwdrequired)
                                description.append(" - This was a public branch").append("\n");
                        }
                        else {
                            SteamAppDetails.SteamAppBranch oldBranchDetails = oldBranches.get(changedBranch.getKey());
                            SteamAppDetails.SteamAppBranch newBranchDetails = newBranches.get(changedBranch.getKey());
                            description.append("[").append(changedBranch.getKey()).append("] Branch ").append(oldBranchDetails.buildid < newBranchDetails.buildid ? "updated" : "downgraded").append(" (`").append(oldBranchDetails.buildid).append("` -> `").append(newBranchDetails.buildid).append("`)\n");
                            if (newBranchDetails.description != null && !newBranchDetails.description.isBlank()) // I don't think this is ever null but nice to have
                                description.append(" - Description: ").append(newBranchDetails.description).append("\n");
                            if (newBranchDetails.pwdrequired == null || !newBranchDetails.pwdrequired)
                                description.append(" - This is a public branch").append("\n");
                            if (changedBranch.getKey().equals("public"))
                                isPublicBranchUpdate = true;
                        }
                    }
                    eb.setDescription(description.toString());
                    MessageBuilder mb = new MessageBuilder();
                    mb.setEmbeds(eb.build());

                    for (ServerChannel sc : rchannels) {
                        if (isPublicBranchUpdate && sc.serverID.equals("673663870136746046"))
                            mb.setContent("@everyone");
                        TextChannel channel = JDAManager.getJDA().getGuildById(sc.serverID).getTextChannelById(sc.channelId);
                        if (channel.canTalk())
                            channel.sendMessage(mb.build()).allowedMentions(Collections.singletonList(MentionType.EVERYONE)).queue();
                        mb.setContent("");
                    }
                }
                if (newAppDetails != null)
                    gameDetails.put(app.getKey(), newAppDetails);
            }
        });
    }

    private static boolean testChannel(ServerChannel sc) {
        if (JDAManager.getJDA() == null)
            return true;
        if (!JDAManager.getJDA().getStatus().equals(JDA.Status.CONNECTED))
            return true;
        Guild guild = JDAManager.getJDA().getGuildById(sc.serverID);
        if (guild == null) {
            System.out.println("Steam can not find Guild " + sc.serverID);
            channels2Remove.add(sc);
            return true;
        }
        TextChannel channel = guild.getTextChannelById(sc.channelId);
        if (channel == null) {
            System.out.println("Steam can not find Channel " + sc.channelId + " from guild " + sc.serverID);
            channels2Remove.add(sc);
            return true;
        }
        return false;
    }
    private static void removeChannels(Integer gameID) {
        List<ServerChannel> rchannels = reportChannels.get(gameID);
        if (rchannels.removeAll(channels2Remove)) {
            if (rchannels.size() > 0)
                reportChannels.put(gameID, rchannels);
            else
                reportChannels.remove(gameID);
            CommandManager.saveSteamWatch();
        }
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
        if (!gameDetails.containsKey(gameID))
            apps.picsGetProductInfo(gameID, null, false, false);
    }

    private void startChangesRequesterThread() {
        int currentHash = tickerHash;

        Thread thread = new Thread(() -> {
            Random random = new Random();

            System.out.println("PICS ticker started #" + currentHash);

            while (currentHash == tickerHash) {
                apps.picsGetChangesSince(previousChangeNumber, true, true);

                try {
                    Thread.sleep(random.nextInt(3210));
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

}
