package slaynash.lum.bot.discord.melonscanner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

import com.github.zafarkhaja.semver.Version;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.dv8tion.jda.api.EmbedBuilder;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.Bit32Lib;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;
import slaynash.lum.bot.ConfigManager;
import slaynash.lum.bot.DBConnectionManagerLum;
import slaynash.lum.bot.discord.JDAManager;
import slaynash.lum.bot.utils.ExceptionUtils;

public class MelonScannerApisManager {
    public static final String LOG_IDENTIFIER = "ML:API";

    public static final List<String> brokenMods = new ArrayList<>();
    public static final List<String> retiredMods = new ArrayList<>();
    public static final List<String> badMod = new ArrayList<>();
    public static final List<String> badAuthor = new ArrayList<>();
    public static final List<String> badPlugin = new ArrayList<>();

    private static final List<MelonScannerApi> apis = new ArrayList<>();

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .followRedirects(Redirect.ALWAYS)
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_NONE))
            .connectTimeout(Duration.ofSeconds(45))
            .build();

    private static final Gson gson = new Gson();
    private static Globals server_globals;

    private static final Map<String, List<MelonApiMod>> games = new ConcurrentHashMap<>();

    private static boolean doneFirstInit = false;

    private static final List<Entry<MelonScannerApi, Instant>> erroringAPIs = new ArrayList<>();

    static {
        apis.add(new MelonScannerApi("Audica", "audica_ahriana", "https://raw.githubusercontent.com/Ahriana/AudicaModsDirectory/main/api.json"));
        apis.add(new MelonScannerApi("BloonsTD6", "btd6_inferno", "http://1330studios.com/btd6_info.json"));
        apis.add(new ThunderstoreApi("BONELAB", "bonelab"));
        apis.add(new ThunderstoreApi("BONEWORKS", "boneworks"));
        apis.add(new MelonScannerApi("ChilloutVR", "vrcmg", "https://api.cvrmg.com/v1/mods"));
        apis.add(new CurseforgeApi("Demeo", 78135));
        apis.add(new ThunderstoreApi("Hard Bullet", "hard-bullet"));
        // apis.add(new MelonScannerApi("Inside the Backrooms", "audica_ahriana", "https://raw.githubusercontent.com/spicebag/InsideTheBackroomsModDirectory/main/main/api/api.json"));
        // apis.add(new ThunderstoreApi("Lethal Company", "lethal-company"));
        // apis.add(new MelonScannerApi("MuseDash", "musedash", "https://mdmc.moe/api/v5/mods"));
        apis.add(new MelonScannerApi("MuseDash", "musedashgh", "https://raw.githubusercontent.com/MDModsDev/ModLinks/main/ModLinks.json"));
        apis.add(new ThunderstoreApi("RUMBLE", "rumble"));
        apis.add(new MelonScannerApi("TheLongDark", "tld", "https://tldmods.com/api.json"));
    }

    public static void startFetchingThread() {
        Thread fetchThread = new Thread(() -> {
            while (true) {

                // We use a temp Map to avoid clearing the common one
                //Map<String, List<MelonApiMod>> gamesTemp = new HashMap<>();

                for (MelonScannerApi api : apis) {

                    System.out.println("Fetching " + api.game + " : " + api.name);

                    //timestamp
                    Instant start = Instant.now();

                    HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .GET()
                        .setHeader("User-Agent", "LUM Bot " + ConfigManager.commitHash)
                        .setHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                        .setHeader("Pragma", "no-cache")
                        .setHeader("Expires", "-1")
                        .timeout(Duration.ofSeconds(45));


                    if (api.isGZip)
                        builder.header("Accept-Encoding", "gzip");

                    for (String header : api.customHeaders) {
                        builder.setHeader(header.split(":")[0], header.split(":")[1]);
                    }

                    String constructedURI = api.endpoint;

                    try {

                        // Script setup

                        if (server_globals == null) {
                            server_globals = new Globals();
                            server_globals.load(new JseBaseLib());
                            server_globals.load(new PackageLib());
                            server_globals.load(new JseMathLib());

                            server_globals.set("base64toLowerHexString", new Base64toLowerHexString());

                            LoadState.install(server_globals);
                            LuaC.install(server_globals);
                        }

                        Globals user_globals = new Globals();
                        user_globals.load(new JseBaseLib());
                        user_globals.load(new PackageLib());
                        user_globals.load(new Bit32Lib());
                        user_globals.load(new TableLib());
                        user_globals.load(new JseMathLib());
                        user_globals.load(new StringLib());

                        user_globals.set("base64toLowerHexString", new Base64toLowerHexString());

                        // API request

                        int pagingOffset = 0;
                        boolean doneFetching = false;

                        List<MelonApiMod> apiMods = new ArrayList<>();

                        while (!doneFetching) {


                            if (api.maxPagination > 0)
                                constructedURI = api.endpoint
                                    .replace("{count}", String.valueOf(api.maxPagination))
                                    .replace("{offset}", String.valueOf(pagingOffset));

                            builder.uri(URI.create(constructedURI));

                            HttpRequest request = builder.build();

                            HttpResponse<byte[]> response;
                            try {
                                response = downloadRequest(httpClient, request, api.game + " : " + api.name, 8);
                            }
                            catch (ConnectException e) {
                                System.out.println("MLAPI: " + e.getMessage());
                                break;
                            }
                            byte[] responseBody = response.body();
                            if (api.isGZip) {
                                ByteArrayOutputStream decompressedStream = new ByteArrayOutputStream();
                                try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(responseBody))) {
                                    int len;
                                    while ((len = gis.read(responseBody)) > 0)
                                        decompressedStream.write(responseBody, 0, len);
                                }
                                catch (Exception e) {
                                    ExceptionUtils.reportException("[API] Failed to decompress GZip response", e);
                                    break;
                                }

                                responseBody = decompressedStream.toByteArray();
                            }
                            String responseString = new String(responseBody);
                            if (responseString.charAt(0) == '<') {
                                ExceptionUtils.reportException("Received HTML for " + api.name);
                                break;
                            }
                            else if (responseString.length() <= 4) {
                                ExceptionUtils.reportException("Received empty JSON for " + api.name);
                                break;
                            }

                            JsonElement data = gson.fromJson(responseString, JsonElement.class);

                            // Script pass

                            user_globals.set("data", CoerceJavaToLua.coerce(data));

                            FileInputStream fis = new FileInputStream("apiscripts/" + api.getScriptName() + ".lua"); // TODO compile and cache
                            LuaValue modsLuaRaw = server_globals.load(fis, api.getScriptName() + ".lua", "t", user_globals).call();
                            fis.close();

                            // Parse data returned by script

                            if (modsLuaRaw == LuaValue.FALSE || modsLuaRaw == LuaValue.NIL) {
                                if (apiMods.isEmpty())
                                    apiMods = api.cachedMods;
                                ExceptionUtils.reportException("MelonScanner API Script returned FALSE or NIL for " + api.game + " : " + api.name, constructedURI);
                                doneFetching = true;
                            }
                            else {
                                int modsCount = 0;
                                LuaTable mods = modsLuaRaw.checktable();

                                LuaValue k = LuaValue.NIL;
                                Varargs n;
                                while (!(k = (n = mods.next(k)).arg1()).isnil()) {
                                    LuaValue v = n.arg(2);
                                    try {
                                        k.checkint();
                                    }
                                    catch (LuaError e) {
                                        System.err.println("Returned table contains an invalid entry: " + n + "\n" + ExceptionUtils.getStackTrace(e));
                                        continue;
                                    }

                                    LuaTable mod;
                                    try {
                                        mod = v.checktable();
                                    }
                                    catch (LuaError e) {
                                        System.err.println("Invalid value for key " + k + "\n" + ExceptionUtils.getStackTrace(e));
                                        continue;
                                    }

                                    String name = sanitizeName(mod.get("name").checkjstring());
                                    // System.out.println("Processing mod " + name);
                                    String approvalStatus = "0";
                                    if (mod.get("approvalStatus") != null && !mod.get("approvalStatus").isnil())
                                        approvalStatus = mod.get("approvalStatus").checkjstring();
                                    String id = mod.get("id") == LuaValue.NIL ? null : mod.get("id").checkjstring();
                                    Version version = Version.tryParse(mod.get("version").checkjstring(), false).orElse(null);
                                    String downloadLink = mod.get("downloadLink") == LuaValue.NIL ? null : mod.get("downloadLink").checkjstring();
                                    String modtype = mod.get("modtype") == LuaValue.NIL ? null : mod.get("modtype").checkjstring();
                                    boolean haspending = mod.get("haspending") != LuaValue.NIL && mod.get("haspending").checkboolean();
                                    String hash = mod.get("hash") == LuaValue.NIL ? null : mod.get("hash").checkjstring();
                                    String[] aliases = null;
                                    boolean isbroken = mod.get("isbroken") != LuaValue.NIL && mod.get("isbroken").checkboolean();
                                    LuaValue aliasesRaw = mod.get("aliases");
                                    if (aliasesRaw != LuaValue.NIL) {
                                        LuaTable aliasesTable = aliasesRaw.checktable();
                                        aliases = new String[aliasesTable.length()];
                                        int iAlias = 0;
                                        LuaValue k2 = LuaValue.NIL;
                                        Varargs n2;
                                        while (!(k2 = (n2 = aliasesTable.next(k2)).arg1()).isnil()) {
                                            aliases[iAlias++] = n2.arg(2).checkjstring();
                                        }
                                    }
                                    if (mod.get("is_deprecated") != LuaValue.NIL && mod.get("is_deprecated").checkboolean())
                                        approvalStatus = "3";

                                    if (downloadLink != null && downloadLink.equalsIgnoreCase("https://github.com/GrahamKracker/BTD6EpicGamesModCompat/releases/download/1.2.1/BTD6EpicGamesModCompat.dll")) {
                                        version = Version.parse("1.2.1");
                                    }

                                    if (isbroken || approvalStatus != null && Integer.parseInt(approvalStatus) == 2) {
                                        if (!brokenMods.contains(name))
                                            brokenMods.add(name);
                                    }
                                    else
                                        brokenMods.remove(name);

                                    if (approvalStatus != null && Integer.parseInt(approvalStatus) >= 3) {
                                        if (!retiredMods.contains(name))
                                            retiredMods.add(name);
                                    }
                                    else
                                        retiredMods.remove(name);
                                    apiMods.add(new MelonApiMod(id, name, version, downloadLink, aliases, hash, modtype, haspending, isbroken));
                                    ++modsCount;
                                }

                                if (api.maxPagination <= 0 || modsCount < api.maxPagination) {
                                    doneFetching = true;
                                }
                            }

                            if (erroringAPIs.stream().anyMatch(entry -> entry.getKey() == api)) {
                                EmbedBuilder eb = new EmbedBuilder();
                                eb.setTitle("MelonScanner API for " + api.game + " : " + api.name + " is no longer erroring");
                                eb.setDescription("It has been erroring for " + Duration.between(erroringAPIs.stream().filter(entry -> entry.getKey() == api).findFirst().orElseThrow().getValue(), Instant.now())
                                        .truncatedTo(ChronoUnit.SECONDS).toString().substring(2).replaceAll("(\\d[HMS])(?!$)", "$1 ")
                                        + (ConfigManager.mainBot ? "" : "\n\nFrom backup bot"));

                                if (JDAManager.isEventsEnabled())
                                    JDAManager.getJDA().getTextChannelById("912757433913454612").sendMessageEmbeds(eb.build()).queue();
                                erroringAPIs.removeIf(entry -> entry.getKey() == api);
                            }

                            if (!doneFetching)
                                Thread.sleep(2 * 1000); // Sleep 2 seconds to avoid API spam
                            else if (!apiMods.isEmpty()) {
                                api.cachedMods = apiMods;
                            }
                        }

                        // Update stored api datas

                        List<MelonApiMod> currentMods = games.get(api.game);
                        if (currentMods == null || currentMods.isEmpty()) //If we are starting up
                            games.put(api.game, new ArrayList<>(apiMods)); //just add all mods in
                        else { //check for updates
                            for (MelonApiMod newMod : apiMods) {

                                MelonApiMod currentMod = null;
                                for (MelonApiMod mod : currentMods) {
                                    // TODO compare using aliases too
                                    if (mod.name.replaceAll("[-_ ]", "").equalsIgnoreCase(newMod.name.replaceAll("[-_ ]", ""))) {
                                        currentMod = mod;
                                        break;
                                    }
                                }

                                if (currentMod == null)
                                    currentMods.add(newMod);
                                else {
                                    if (currentMod.versions == null || currentMod.versions[0].version() == null || newMod.versions != null && newMod.versions[0].version().isHigherThanOrEquivalentTo(currentMod.versions[0].version())) {
                                        currentMods.remove(currentMod);
                                        currentMods.add(newMod);
                                    }
                                }
                            }
                            synchronized (games) {
                                games.put(api.game, currentMods);
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

                        ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT * FROM `Mods` WHERE Game IS NULL OR Game = ?", api.game);
                        while (rs.next()) {
                            MelonApiMod mod = new MelonApiMod(rs.getString("ID"), rs.getString("Name"), Version.parse(rs.getString("Version")), rs.getString("DownloadLink"), rs.getString("Aliases") == null ? null : rs.getString("Aliases").split(","), rs.getString("Hash"), rs.getString("Type"), rs.getBoolean("HasPending"), rs.getBoolean("IsBroken"));
                            List<MelonApiMod> mods = games.get(api.game);
                            if (mods == null)
                                continue;
                            if (mod.versions[0].version() == null)
                                continue;
                            mods.removeIf(modtmp ->
                                modtmp.name.replaceAll("[-_ ]", "").equals(mod.name.replaceAll("[-_ ]", ""))
                                && (modtmp.versions[0] == null || mod.versions[0].version().isHigherThan(modtmp.versions[0].version())));
                            mods.add(mod);
                            synchronized (games) {
                                games.put(api.game, mods);
                            }
                        }
                        DBConnectionManagerLum.closeRequest(rs);

                        System.out.println("Done fetching " + api.game + " : " + api.name + " in " + Duration.between(start, Instant.now()).toMillis() + "ms");

                        if (doneFirstInit)
                            Thread.sleep(6 * 60 * 1000 / apis.size()); // stager sleep so all requests don't come at the same time.
                        else
                            Thread.sleep(100);
                    }
                    catch (HttpTimeoutException exception) {
                        ExceptionUtils.reportException("MelonScanner API Timed Out for " + api.game + " : " + api.name + ", " + constructedURI);
                    }
                    catch (IOException exception) {
                        if (exception.getMessage().contains("GOAWAY")) {
                            ExceptionUtils.reportException(api.game + " : " + api.name + " is a meanie and told me to go away <a:kanna_cry:851143700297941042>");
                        }
                        else
                            ExceptionUtils.reportException("MelonScanner API Connection Error for " + api.game + " : " + api.name + ", " + constructedURI, exception.getMessage());
                    }
                    catch (Exception exception) {
                        if (erroringAPIs.stream().noneMatch(entry -> entry.getKey() == api)) {
                            ExceptionUtils.reportException("MelonScanner API Exception for " + api.game + " : " + api.name + ", " + constructedURI, exception);
                        }
                        erroringAPIs.add(Map.entry(api, Instant.now()));
                    }

                }
                if (!doneFirstInit)
                    System.out.println("Done inital fetching");
                doneFirstInit = true;
            }
        }, "MelonScannerApisManagerThread");
        fetchThread.setDaemon(true);
        fetchThread.start();
    }


    // Additional classes

    private static String sanitizeName(String name) {
        if (name == null)
            return null;

        //              API name        |   Log name
        if (name.equals("JeviLibBL")) name = "JeviLib";
        if (name.equals("Fusion")) name = "LabFusion";
        return name;
    }

    private static class Base64toLowerHexString extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue arg) {
            return LuaValue.valueOf(bytesToHex(Base64.getDecoder().decode(arg.checkjstring())).toLowerCase());
        }
    }

    private static class MelonScannerApi {

        public final String game;
        public final String name;
        public final String endpoint;

        public final boolean isGZip = false;
        public int maxPagination;
        public final List<String> customHeaders = new ArrayList<>();

        public List<MelonApiMod> cachedMods = new ArrayList<>();

        public MelonScannerApi(String game, String name, String endpoint) {
            this.game = game;
            this.name = name;
            this.endpoint = endpoint;
            this.maxPagination = -1;
        }

        public String getScriptName() {
            return name;
        }
    }

    private static class ThunderstoreApi extends MelonScannerApi {
        public ThunderstoreApi(String game, String tsName) {
            super(game, "thunderstore:" + tsName, "https://thunderstore.io/c/" + tsName + "/api/v1/package/");
        }

        @Override
        public String getScriptName() {
            return "thunderstore";
        }
    }

    private static class CurseforgeApi extends MelonScannerApi {
        public CurseforgeApi(String game, int cfId) {
            super(game, "curseforge:" + cfId + " (" + game + ")", "https://api.curseforge.com/v1/mods/search?gameId=" + cfId + "&pageSize={count}&index={offset}");
            maxPagination = 50;
            customHeaders.add("X-Api-Key: " + ConfigManager.curseforgeApiKey);
        }

        @Override
        public String getScriptName() {
            return "curseforge";
        }
    }

    public static List<MelonApiMod> getMods(String game) {
        if (game == null)
            return null;
        List<MelonApiMod> list = games.get(game);
        return list == null ? null : new ArrayList<>(games.get(game));
    }

    public static boolean getMods(MelonScanContext context) {
        if (context.game == null)
            return false;
        List<MelonApiMod> list = getMods(context.game);
        context.modApiFound = list != null;

        if (list == null)
            list = new ArrayList<>();

        context.modDetails = list;
        return context.modApiFound;
    }

    public static String getDownloadLinkForMod(String game, String missingModName) {
        if (game == null)
            return null;

        synchronized (games) {
            List<MelonApiMod> mods = games.get(game);
            if (mods == null)
                return null;

            MelonApiMod mod = mods.stream().filter(modtmp -> modtmp.name.equals(missingModName)).findFirst().orElse(null);

            if (mod == null) {
                mod = mods.stream().filter(modtmp -> modtmp.aliases != null).filter(modtmp -> Arrays.asList(modtmp.aliases).contains(missingModName)).findFirst().orElse(null);
            }

            return mod != null ? mod.downloadLink : null;
        }
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

    public static HttpResponse<byte[]> downloadRequest(HttpRequest request, String source) throws Exception {
        return downloadRequest(httpClient, request, source);
    }
    public static HttpResponse<byte[]> downloadRequest(HttpClient httpClient, HttpRequest request, String source) throws Exception {
        return downloadRequest(httpClient, request, source, 3);
    }
    public static HttpResponse<byte[]> downloadRequest(HttpClient httpClient, HttpRequest request, String source, int attempts) throws Exception {
        HttpResponse<byte[]> response;
        Exception exception = null;
        for (int i = 0; i < attempts; i++) {
            try {
                response = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray()).get(6, java.util.concurrent.TimeUnit.MINUTES);

                if (response.statusCode() < 200 || response.statusCode() >= 400) {
                    System.out.println("Lum gotten status code: " + response.statusCode() + " from " + source + " and is retrying");
                    throw new Exception("Lum gotten status code: " + response.statusCode() + " from " + source);
                }
                if (response.body() == null || response.body().length == 0) {
                    System.out.println(source + " provided empty response");
                    throw new Exception("Lum gotten an empty response: " + response.statusCode() + " from " + source);
                }
            }
            catch (Exception e) {
                exception = e;
                System.out.println("Lum got " + e.getLocalizedMessage() + " from " + source + " and is retrying");
                Thread.sleep(1000 * 30); // Sleep for half a minute
                continue;
            }
            return response;
        }
        throw new Exception(exception);
    }
}
