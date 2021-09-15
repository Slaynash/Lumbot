package slaynash.lum.bot.steam;

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
import net.dv8tion.jda.api.MessageBuilder;
import slaynash.lum.bot.Main;
import slaynash.lum.bot.discord.JDAManager;
import slaynash.lum.bot.discord.ServerChannel;

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

    public Steam() {

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
                System.err.println("[Steam] Failed to login: " + callback.getResult());
                client.disconnect();
                return;
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

            for (Entry<Integer, PICSChangeData> changeDataPair : callback.getAppChanges().entrySet()) {
                Integer gameID = changeDataPair.getKey();
                System.out.println("" + gameID + ": " + changeDataPair.getValue().getId());
                if (reportChannels.containsKey(gameID)) {
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setTitle("New Steam changelist from " + gameID + " (#" + changeDataPair.getValue().getChangeNumber() + ")", "https://steamdb.info/app/" + gameID + "/history/?changeid=" + changeDataPair.getValue().getChangeNumber());

                    List<ServerChannel> rchannels = reportChannels.get(gameID);
                    for (ServerChannel sc : rchannels) {
                        JDAManager.getJDA().getGuildById(sc.serverID).getTextChannelById(sc.channelId).sendMessageEmbeds(eb.build()).queue();
                    }

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

                if (gameDetail == null) {
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
                    eb.setTitle(gameDetail.common.name + " Depot" + (changeBranches.size() > 0 ? "s" : "") + " updated");
                    StringBuilder description = new StringBuilder();
                    boolean isPublicBranchUpdate = false;
                    for (Entry<String, SteamAppDetails.SteamAppBranch> changedBranch : changeBranches.entrySet()) {
                        if (!oldBranches.containsKey(changedBranch.getKey())) {
                            SteamAppDetails.SteamAppBranch branchDetails = newBranches.get(changedBranch.getKey());
                            description.append("[").append(changedBranch.getKey()).append("] Branch created (`#").append(branchDetails.buildid).append("`)\n");
                            if (branchDetails.description != null)
                                description.append(" - Description: ").append(branchDetails.description).append("\n");
                        }
                        else if (!newBranches.containsKey(changedBranch.getKey())) {
                            description.append("[").append(changedBranch.getKey()).append("] Branch deleted\n");
                        }
                        else {
                            SteamAppDetails.SteamAppBranch oldBranchDetails = oldBranches.get(changedBranch.getKey());
                            SteamAppDetails.SteamAppBranch newBranchDetails = newBranches.get(changedBranch.getKey());
                            description.append("[").append(changedBranch.getKey()).append("] Branch ").append(oldBranchDetails.buildid < newBranchDetails.buildid ? "updated" : "downgraded").append(" (`").append(oldBranchDetails.buildid).append("` -> `").append(newBranchDetails.buildid).append("`)\n");
                            if (newBranchDetails.description != null)
                                description.append(" - Description: ").append(newBranchDetails.description).append("\n");

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
                        JDAManager.getJDA().getGuildById(sc.serverID).getTextChannelById(sc.channelId).sendMessage(mb.build()).queue();
                        mb.setContent("");
                    }
                }
                gameDetails.put(app.getKey(), newAppDetails);
            }
        });
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
