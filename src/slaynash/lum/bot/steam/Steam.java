package slaynash.lum.bot.steam;

import java.nio.charset.StandardCharsets;
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
import net.dv8tion.jda.api.entities.MessageEmbed;
import slaynash.lum.bot.Main;
import slaynash.lum.bot.discord.JDAManager;

public class Steam {

    private CallbackManager callbackManager;
    private SteamClient client;
    private SteamUser user;
    private SteamApps apps;
    private boolean isLoggedOn;
    private int tickerHash;

    private int previousChangeNumber;
    private SteamAppDetails vrchatAppDetails;

    private int gameId = 438100; //VRChat

    /*
    private final static HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();
    */

    public Steam() {

        client = new SteamClient();
        user = client.getHandler(SteamUser.class);
        apps = client.getHandler(SteamApps.class);

        callbackManager = new CallbackManager(client);
        callbackManager.subscribe(ConnectedCallback.class, callback -> {
            System.out.println("[Steam] Connected to Steam, logging in...");
            user.logOnAnonymous();
        });
        callbackManager.subscribe(DisconnectedCallback.class, callback -> {
            if (Main.isShuttingDown)
                return;

            if (isLoggedOn) {
                isLoggedOn = false;
                ++tickerHash;
            }

            System.out.println("[Steam] Disconnected from Steam. Retrying in 5s...");

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
            System.out.println("[Steam] Logged in, current valve time is " + callback.getServerTime() + " UTC");

            startChangesRequesterThread();

            if (vrchatAppDetails == null)
                apps.picsGetProductInfo(gameId, null, false, false);
        });
        callbackManager.subscribe(LoggedOffCallback.class, callback -> {
            if (isLoggedOn) {
                isLoggedOn = false;
                ++tickerHash;
            }

            System.out.println("[Steam] Logged off from Steam");
        });
        callbackManager.subscribe(PICSChangesCallback.class, callback -> {
            if (previousChangeNumber == callback.getCurrentChangeNumber())
                return;

            System.out.println("[Steam] Changelist " + previousChangeNumber + " -> " + callback.getCurrentChangeNumber() + " (" + callback.getAppChanges().size() + " apps, " + callback.getPackageChanges().size() + " packages)");

            previousChangeNumber = callback.getCurrentChangeNumber();

            for (Entry<Integer, PICSChangeData> changeDataPair : callback.getAppChanges().entrySet()) {
                System.out.println("[Steam] " + changeDataPair.getKey() + ": " + changeDataPair.getValue().getId());
                if (changeDataPair.getKey() == gameId) {

                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setTitle("New Steam changelist available (#" + changeDataPair.getValue().getChangeNumber() + ")", "https://steamdb.info/app/" + gameId + "/history/?changeid=" + changeDataPair.getValue().getChangeNumber());
                    MessageEmbed embed = eb.build();

                    JDAManager.getJDA().getGuildById(673663870136746046L /* Modders & Chill */).getTextChannelById(829441182508515348L /* #bot-update-spam */).sendMessageEmbeds(embed).queue();

                    apps.picsGetProductInfo(changeDataPair.getKey(), null, false, false);
                }
            }

        });
        callbackManager.subscribe(PICSProductInfoCallback.class, callback -> {
            System.out.println("[Steam] [PICSProductInfoCallback] apps: ");
            for (Entry<Integer, PICSProductInfo> app : callback.getApps().entrySet()) {
                System.out.println("[Steam] [PICSProductInfoCallback]  - (" + app.getKey() + ") " + app.getValue().getChangeNumber());
                if (app.getKey() != gameId)
                    return;

                printKeyValue(app.getValue().getKeyValues(), 1);
                /*
                System.out.println("[Steam] [PICSProductInfoCallback]  > " + app.getValue().getHttpUri());
                System.out.println("[Steam] [PICSProductInfoCallback]  > " + bytesToHex(app.getValue().getShaHash()));
                System.out.println("[Steam] [PICSProductInfoCallback]  > " + app.getValue().getKeyValues().toString());
                */

                if (vrchatAppDetails == null) {
                    vrchatAppDetails = new SteamAppDetails(app.getValue().getKeyValues());
                    /*
                    System.out.println("Public Manifest:");
                    System.out.println(vrchatAppDetails.depots.elements);
                    System.out.println(vrchatAppDetails.depots.elements.get(gameId + 1));
                    System.out.println(vrchatAppDetails.depots.elements.get(gameId + 1).manifests);
                    System.out.println(vrchatAppDetails.depots.elements.get(gameId + 1).manifests.get("public"));
                    */


                    /*
                    System.out.println("Branches:");
                    for (Entry<String, SteamAppDetails.SteamAppBranch> branch : vrchatAppDetails.depots.branches.entrySet()) {
                        System.out.println("[" + branch.getKey() + "]");
                        System.out.println("    buildid: " + branch.getValue().buildid);
                        System.out.println("    desc: " + branch.getValue().description);
                        System.out.println("    time: " + branch.getValue().timeupdated);
                        System.out.println("    pwd: " + branch.getValue().pwdrequired);
                    }
                    */
                    return;
                }

                SteamAppDetails newAppDetails = new SteamAppDetails(app.getValue().getKeyValues());
                SteamAppDetails appChanges = SteamAppDetails.compare(vrchatAppDetails, newAppDetails);

                if (appChanges != null && appChanges.depots != null && appChanges.depots.branches != null) {

                    Map<String, SteamAppDetails.SteamAppBranch> oldBranches = vrchatAppDetails.depots.branches;
                    Map<String, SteamAppDetails.SteamAppBranch> newBranches = newAppDetails.depots.branches;
                    Map<String, SteamAppDetails.SteamAppBranch> changeBranches = appChanges.depots.branches;

                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setTitle("Depot" + (changeBranches.size() > 0 ? "s" : "") + " updated");
                    String description = "";
                    boolean isPublicBranchUpdate = false;
                    for (Entry<String, SteamAppDetails.SteamAppBranch> changedBranch : changeBranches.entrySet()) {
                        if (!oldBranches.containsKey(changedBranch.getKey())) {
                            SteamAppDetails.SteamAppBranch branchDetails = newBranches.get(changedBranch.getKey());
                            description += "[" + changedBranch.getKey() + "] Branch created (`#" + branchDetails.buildid + "`)\n";
                            if (branchDetails.description != null)
                                description += " - Description: " + branchDetails.description + "\n";
                        }
                        else if (!newBranches.containsKey(changedBranch.getKey())) {
                            //SteamAppDetails.SteamAppBranch branchDetails = oldBranches.get(changedBranch.getKey());
                            description += "[" + changedBranch.getKey() + "] Branch deleted\n";
                        }
                        else {
                            SteamAppDetails.SteamAppBranch oldBranchDetails = oldBranches.get(changedBranch.getKey());
                            SteamAppDetails.SteamAppBranch newBranchDetails = newBranches.get(changedBranch.getKey());
                            description += "[" + changedBranch.getKey() + "] Branch updated (`" + oldBranchDetails.buildid + "` -> `" + newBranchDetails.buildid + "`)\n";
                            if (newBranchDetails.description != null)
                                description += " - Description: " + newBranchDetails.description + "\n";

                            if (changedBranch.getKey().equals("public"))
                                isPublicBranchUpdate = true;
                        }
                    }
                    eb.setDescription(description);
                    MessageEmbed embed = eb.build();
                    MessageBuilder mb = new MessageBuilder();
                    if (isPublicBranchUpdate)
                        mb.setContent("@everyone");
                    mb.setEmbeds(embed);

                    JDAManager.getJDA().getGuildById(673663870136746046L /* Modders & Chill */).getTextChannelById(829441182508515348L /* #bot-update-spam */).sendMessage(mb.build()).queue();
                }

                vrchatAppDetails = newAppDetails;
            }
        });
    }

    private static void printKeyValue(KeyValue keyvalue, int depth) {
        if (keyvalue.getChildren().size() == 0)
            System.out.println("[Steam] [PICSProductInfoCallback] " + " ".repeat(depth * 4) + " " + keyvalue.getName() + ": " + keyvalue.getValue());
        else {
            System.out.println("[Steam] [PICSProductInfoCallback] " + " ".repeat(depth * 4) + " " + keyvalue.getName() + ":");
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

            System.out.println("[Steam] PICS ticker started #" + currentHash);

            while (currentHash == tickerHash) {
                apps.picsGetChangesSince(previousChangeNumber, true, true);

                try {
                    Thread.sleep(random.nextInt(3210));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

            System.out.println("[Steam] PICS ticker stopped #" + currentHash);
        }, "PICS ticker #" + currentHash);
        thread.setDaemon(true);
        thread.start();
    }

    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);

    public static String bytesToHex(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

}
