package slaynash.lum.bot.discord.melonscanner;

import java.awt.Color;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import slaynash.lum.bot.discord.ExceptionUtils;
import slaynash.lum.bot.discord.JDAManager;
import slaynash.lum.bot.discord.melonscanner.apis.AudicaModDetails;
import slaynash.lum.bot.discord.melonscanner.apis.BTD6Gurrenm4ModDetails;
import slaynash.lum.bot.discord.melonscanner.apis.BTD6InfernoModDetails;
import slaynash.lum.bot.discord.melonscanner.apis.TheLongDarkModDetails;
import slaynash.lum.bot.discord.melonscanner.apis.VRCModDetails;

public class MelonScannerApisManager {

    private static List<MelonScannerApi<?>> apis = new ArrayList<>();
    
    private final static HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .followRedirects(Redirect.ALWAYS)
            .build();

    private static Gson gson = new Gson();
    
    private static Thread fetchThread;
    private static Map<String, List<MelonApiMod>> games = new ConcurrentHashMap<>();

    static {
        apis.add(new MelonScannerApi<ArrayList<VRCModDetails>>(
            "VRChat",
            "https://api.vrcmg.com/v0/mods.json",
            false, // Check using hashes?
            new TypeToken<ArrayList<VRCModDetails>>() {}.getType(),
            (apiData, mods) -> {
                for (VRCModDetails processingmods : apiData) {
                    VRCModDetails.VRCModVersionDetails vrcmoddetails = processingmods.versions[0];

                    String[] aliases = new String[processingmods.aliases.length - 1];
                    for (int i = 0; i < aliases.length; ++i)
                        aliases[i] = processingmods.aliases[i];

                    mods.add(new MelonApiMod(vrcmoddetails.name, vrcmoddetails.modversion, vrcmoddetails.downloadlink, aliases));
                }

                return true;
            }
        ));

        apis.add(new MelonScannerApi<HashMap<String, BTD6InfernoModDetails>>(
            "BloonsTD6",
            "https://raw.githubusercontent.com/Inferno-Dev-Team/Inferno-Omnia/main/version.json",
            false, // Check using hashes?
            new TypeToken<HashMap<String, BTD6InfernoModDetails>>() {}.getType(),
            (apiData, mods) -> {
                for (Entry<String, BTD6InfernoModDetails> mod : apiData.entrySet())
                    mods.add(new MelonApiMod(mod.getKey(), mod.getValue().version, null));
                return true;
            }
        ));

        apis.add(new MelonScannerApi<HashMap<String, BTD6Gurrenm4ModDetails>>(
            "BloonsTD6",
            "https://raw.githubusercontent.com/gurrenm3/MelonLoader-BTD-Mods/main/mods.json",
            false, // Check using hashes?
            new TypeToken<HashMap<String, BTD6Gurrenm4ModDetails>>() {}.getType(),
            (apiData, mods) -> {
                for (Entry<String, BTD6Gurrenm4ModDetails> mod : apiData.entrySet())
                    mods.add(new MelonApiMod(mod.getKey(), mod.getValue().version, null));
                return true;
            }
        ));

        apis.add(new MelonScannerApi<HashMap<String, AudicaModDetails>>(
            "Audica",
            "https://raw.githubusercontent.com/Ahriana/AudicaModsDirectory/main/api.json",
            false, // Check using hashes?
            new TypeToken<HashMap<String, AudicaModDetails>>() {}.getType(),
            (apiData, mods) -> {
                for (Entry<String, AudicaModDetails> mod : apiData.entrySet())
                    mods.add(new MelonApiMod(mod.getKey(), mod.getValue().version, mod.getValue().download[0].browser_download_url));
                return true;
            }
        ));

        apis.add(new MelonScannerApi<HashMap<String, TheLongDarkModDetails>>(
            "TheLongDark",
            "https://tld.xpazeapps.com/api.json",
            false, // Check using hashes?
            new TypeToken<HashMap<String, TheLongDarkModDetails>>() {}.getType(),
            (apiData, mods) -> {
                for (Entry<String, TheLongDarkModDetails> mod : apiData.entrySet())
                    mods.add(new MelonApiMod(mod.getKey(), mod.getValue().version, mod.getValue().download.browser_download_url, mod.getValue().aliases));
                return true;
            }
        ));
    }
    
    public static void startFetchingThread() {
        fetchThread = new Thread(() -> {
            while (true) {

                // We use a temp Map to avoid clearing the common one
                Map<String, List<MelonApiMod>> gamesTemp = new HashMap<>();

                for (MelonScannerApi<?> api : apis) {

                    HttpRequest request = HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create(api.endpoint))
                        .setHeader("User-Agent", "LUM Bot")
                        .timeout(Duration.ofSeconds(20))
                        .build();

                    try {
                        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                        if (response.statusCode() < 200 || response.statusCode() >= 400)
                            throw new Exception("Failed to fetch remote API data (server returned code " + response.statusCode() + ")");
                        
                        String apiDataRaw = response.body();

                        List<MelonApiMod> apiMods = new ArrayList<>();
                        if (api.parse(apiDataRaw, apiMods))
                            api.cachedMods = apiMods;
                        else
                            apiMods = api.cachedMods;

                        List<MelonApiMod> currentMods = gamesTemp.get(api.game);
                        if (currentMods == null || currentMods.isEmpty())
                            games.put(api.game, currentMods = new ArrayList<MelonApiMod>(apiMods));
                        else {
                            for (MelonApiMod newMod : apiMods) {

                                MelonApiMod currentMod = null;
                                for (MelonApiMod mod : currentMods) {
                                    if (mod.name.equals(newMod.name)) {
                                        currentMod = mod;
                                        break;
                                    }
                                }
                                
                                if (currentMod == null)
                                    currentMods.add(newMod);
                                else {
                                    // TODO compare using aliases too
                                    if (VersionUtils.CompareVersion(newMod.versions[0].version, currentMod.versions[0].version) > 0) {
                                        // TODO merge rather than replace
                                        currentMods.remove(currentMod);
                                        currentMods.add(newMod);
                                    }
                                }
                                
                            }
                        }

                        /*
                        for (MelonApiMod replacingMod : currentMods) {
                            for (String replacedModName : replacingMod.replacingMods) {
                                for (MelonApiMod replacedMod : currentMods) {
                                    if (replacedMod.name.equals(replacedModName)) {
                                        replacedMod.replacedBy = replacingMod.name;
                                        break;
                                    }
                                }
                            }
                        }
                        */
                        Thread.sleep(1000); // sleep for a sec so all requests don't come at the same time.
                    }
                    catch (HttpTimeoutException exception) {
                        System.err.println("Fetching " + api.endpoint + " timedout:");

                        try {
                            EmbedBuilder embedBuilder = new EmbedBuilder();
                            embedBuilder.setColor(Color.orange);
                            embedBuilder.setTitle("MelonScanner API Timed Out for " + api.endpoint);
                            MessageEmbed embed = embedBuilder.build();
                            JDAManager.getJDA().getGuildById(633588473433030666L).getTextChannelById(851519891965345845L).sendMessage(embed).queue();
                        }
                        catch (Exception e2) { e2.printStackTrace(); }
                    }
                    catch (Exception exception) {
                        System.err.println("Exception while fetching " + api.endpoint + ":");
                        exception.printStackTrace();

                        try {
                            EmbedBuilder embedBuilder = new EmbedBuilder();
                            embedBuilder.setColor(Color.red);
                            embedBuilder.setTitle("MelonScanner API Exception for " + api.endpoint);
                            String exceptionString = exception.getMessage() + "\n" + ExceptionUtils.getStackTrace(exception);
                            if (exceptionString.length() > 2048)
                                exceptionString = exceptionString.substring(0, 2044) + " ...";
                            embedBuilder.setDescription(exceptionString);
                            MessageEmbed embed = embedBuilder.build();

                            JDAManager.getJDA().getGuildById(633588473433030666L).getTextChannelById(851519891965345845L).sendMessage(embed).queue();
                        }
                        catch (Exception e2) { e2.printStackTrace(); }
                    }
                }

                for (Entry<String, List<MelonApiMod>> entry : gamesTemp.entrySet())
                    games.put(entry.getKey(), entry.getValue());

                    try {
                        Thread.sleep(6 * 60 * 1000); // 10 times / hour (every 6 minutes)
                    } catch (InterruptedException e) { e.printStackTrace(); }

            }
        }, "MelonScannerApisManagerThread");
        fetchThread.setDaemon(true);
        fetchThread.start();
    }

    
    // Additional classes

    private static class MelonScannerApi<T> {

        public final String game;
        public final String endpoint;
        public final boolean compareUsingHashes;
        public final Type type;
        public final IMelonApiParser<T> parser;

        public List<MelonApiMod> cachedMods = new ArrayList<>();

        public MelonScannerApi(String game, String endpoint, boolean compareUsingHashes, Type type, IMelonApiParser<T> parser) {
            this.game = game;
            this.endpoint = endpoint;
            this.compareUsingHashes = compareUsingHashes;
            this.type = type;
            this.parser = parser;
        }

        public boolean parse(String apiDataRaw, List<MelonApiMod> mods) {
            T apiData = gson.fromJson(apiDataRaw, type);
            return parser.parse(apiData, mods);
        }
    }

    private static interface IMelonApiParser<T> {
        public boolean parse(T apiData, List<MelonApiMod> mods);
    }

    public static List<MelonApiMod> getMods(String game) {
        if (game == null)
            return null;
        List<MelonApiMod> list = games.get(game);
        return list == null ? null : new ArrayList<MelonApiMod>(games.get(game));
    }

    public static boolean compareUsingHash(String game) {
        MelonScannerApi<?> api = apis.stream().filter(api_ -> api_.game.equals(game)).findFirst().orElse(null);
        return api == null ? false : api.compareUsingHashes;
    }

    public static String getDownloadLinkForMod(String game, String missingModName) {
        if (game == null)
            return null;
        
        List<MelonApiMod> mods = games.get(game);
        if (mods == null)
            return null;

        MelonApiMod mod = mods.stream().filter(modtmp -> modtmp.name.equals(missingModName)).findFirst().orElse(null);
        return mod != null ? mod.downloadLink : null;
    }
}
