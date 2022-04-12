package slaynash.lum.bot.discord.melonscanner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

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
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;

import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.utils.ExceptionUtils;

public class MelonScannerApisManager {
    public static final String LOG_IDENTIFIER = "ML:API";

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

    static {
        apis.add(new MelonScannerApi("VRChat", "vrcmg", "https://api.vrcmg.com/v0/mods.json", true));
        apis.add(new MelonScannerApi("UNO", "uno", "https://mintlily.lgbt/img/rak/uno.json", true));
        apis.add(new MelonScannerApi("BloonsTD6", "btd6_inferno", "http://1330studios.com/btd6_info.json"));
        // apis.add(new MelonScannerApi("BloonsTD6", "btd6_gurrenm4", "https://raw.githubusercontent.com/gurrenm3/MelonLoader-BTD-Mods/main/mods.json"));
        apis.add(new MelonScannerApi("Audica", "audica_ahriana", "https://raw.githubusercontent.com/Ahriana/AudicaModsDirectory/main/api.json"));
        apis.add(new MelonScannerApi("TheLongDark", "tld", "https://tld.xpazeapps.com/api.json"));
        apis.add(new ThunderstoreApi("BONEWORKS", "boneworks"));
        // apis.add(new MelonScannerApi("Domeo", "domeo", ""));
        apis.add(new MelonScannerApi("MuseDash", "musedash", "https://mdmc.moe/api/v5/mods"));
    }

    public static void startFetchingThread() {
        Thread fetchThread = new Thread(() -> {
            while (true) {

                // We use a temp Map to avoid clearing the common one
                //Map<String, List<MelonApiMod>> gamesTemp = new HashMap<>();

                for (MelonScannerApi api : apis) {

                    System.out.println("Fetching " + api.name);

                    HttpRequest.Builder builder = HttpRequest.newBuilder()
                            .GET()
                            .uri(URI.create(api.endpoint))
                            .setHeader("User-Agent", "LUM Bot")
                            .setHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                            .setHeader("Pragma", "no-cache")
                            .setHeader("Expires", "-1")
                            .timeout(Duration.ofSeconds(45));


                    if (api.isGZip)
                        builder.header("Accept-Encoding", "gzip");

                    HttpRequest request = builder.build();

                    try {

                        // API request

                        HttpResponse<byte[]> response = downloadRequest(request, api.name);
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
                                //ExceptionUtils.reportException("VRChat deobf map check failed", "Failed to decompress current deobfuscation map", e);
                                return;
                            }

                            responseBody = decompressedStream.toByteArray();
                        }


                        String apiDataRaw = new String(responseBody);
                        //System.out.println("API retured body: " + apiDataRaw);
                        //System.out.println("API URI: " + response.uri());

                        JsonElement data = gson.fromJson(apiDataRaw, JsonElement.class);

                        // Script pass

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

                        user_globals.set("base64toLowerHexString", new Base64toLowerHexString());

                        user_globals.set("data", CoerceJavaToLua.coerce(data));

                        FileInputStream fis = new FileInputStream("apiscripts/" + api.getScriptName() + ".lua"); // TODO compile and cache
                        LuaValue modsLuaRaw = server_globals.load(fis, api.getScriptName() + ".lua", "t", user_globals).call();
                        fis.close();

                        // Parse data returned by script

                        List<MelonApiMod> apiMods = new ArrayList<>();

                        if (modsLuaRaw == LuaValue.FALSE)
                            apiMods = api.cachedMods;
                        else {
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

                                String name = mod.get("name").checkjstring();
                                System.out.println("Processing mod " + name);
                                String approvalStatus = "0";
                                if (mod.get("approvalStatus") != null && !mod.get("approvalStatus").isnil())
                                    approvalStatus = mod.get("approvalStatus").checkjstring();
                                String version = mod.get("version").checkjstring();
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

                                if (approvalStatus != null && Integer.parseInt(approvalStatus) == 2) {
                                    if (!CommandManager.brokenMods.contains(name))
                                        CommandManager.brokenMods.add(name);
                                }
                                else
                                    CommandManager.brokenMods.remove(name);
                                apiMods.add(new MelonApiMod(name, version, downloadLink, aliases, hash, modtype, haspending, isbroken));
                            }
                            if (api.name.equals("vrcmg")) {
                                //apiMods.add(new MelonApiMod("ReMod", null, null, null, null, "Mod", false)); // ReMod uses Lum embed for outdated version
                                apiMods.add(new MelonApiMod("WholesomeLoader", null, null, null, null, "Mod", false, false));
                            }

                            api.cachedMods = apiMods;
                        }

                        // Update stored api datas

                        List<MelonApiMod> currentMods = games.get(api.game);
                        if (currentMods == null || currentMods.isEmpty()) //If we are starting up
                            games.put(api.game, new ArrayList<>(apiMods)); //just add all mods in
                        else { //check for updates
                            for (MelonApiMod newMod : apiMods) {

                                MelonApiMod currentMod = null;
                                for (MelonApiMod mod : currentMods) {
                                    if (mod.name.equalsIgnoreCase(newMod.name)) {
                                        currentMod = mod;
                                        break;
                                    }
                                }

                                if (currentMod == null)
                                    currentMods.add(newMod);
                                else {
                                    // TODO compare using aliases too
                                    if (VersionUtils.compareVersion(newMod.versions[0].version, currentMod.versions[0].version) != 0) {
                                        // TODO merge rather than replace
                                        currentMods.remove(currentMod);
                                        currentMods.add(newMod);
                                    }
                                }
                            }
                            games.put(api.game, currentMods);
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

                        if (doneFirstInit)
                            Thread.sleep(6 * 60 * 1000 / apis.size()); // stager sleep so all requests don't come at the same time.
                        else
                            Thread.sleep(5 * 1000);
                    }
                    catch (HttpTimeoutException exception) {
                        ExceptionUtils.reportException("MelonScanner API Timed Out for " + api.name + ", " + api.endpoint);
                    }
                    catch (IOException exception) {
                        if (exception.getMessage().contains("GOAWAY")) {
                            ExceptionUtils.reportException(api.name + " is a meanie and told me to go away <a:kanna_cry:851143700297941042>");
                        }
                        else
                            ExceptionUtils.reportException("MelonScanner API Connection Error for " + api.name + ", " + api.endpoint, exception.getMessage());
                    }
                    catch (Exception exception) {
                        ExceptionUtils.reportException("MelonScanner API Exception for " + api.name + ", " + api.endpoint, exception);
                    }

                }

                doneFirstInit = true;

            }
        }, "MelonScannerApisManagerThread");
        fetchThread.setDaemon(true);
        fetchThread.start();
    }


    // Additional classes

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
        public final boolean compareUsingHashes;

        public List<MelonApiMod> cachedMods = new ArrayList<>();

        public MelonScannerApi(String game, String name, String endpoint) {
            this.game = game;
            this.name = name;
            this.endpoint = endpoint;
            this.compareUsingHashes = false;
        }
        public MelonScannerApi(String game, String name, String endpoint, boolean compareUsingHashes) {
            this.game = game;
            this.name = name;
            this.endpoint = endpoint;
            this.compareUsingHashes = compareUsingHashes;
        }

        public String getScriptName() {
            return name;
        }
    }

    private static class ThunderstoreApi extends MelonScannerApi {
        public ThunderstoreApi(String game, String tsName) {
            super(game, "thunderstore:" + tsName, "https://" + tsName + ".thunderstore.io/api/v1/package/");
        }

        @Override
        public String getScriptName() {
            return "thunderstore";
        }
    }

    public static List<MelonApiMod> getMods(String game) {
        if (game == null)
            return null;
        List<MelonApiMod> list = games.get(game);
        return list == null ? null : new ArrayList<>(games.get(game));
    }

    public static boolean compareUsingHash(String game) {
        MelonScannerApi api = apis.stream().filter(api_ -> api_.game.equals(game)).findFirst().orElse(null);
        return api != null && api.compareUsingHashes;
    }

    public static String getDownloadLinkForMod(String game, String missingModName) {
        if (game == null)
            return null;

        List<MelonApiMod> mods = games.get(game);
        if (mods == null)
            return null;

        MelonApiMod mod = mods.stream().filter(modtmp -> modtmp.name.equals(missingModName)).findFirst().orElse(null);

        if (mod == null) {
            mod = mods.stream().filter(modtmp -> modtmp.aliases != null).filter(modtmp -> Arrays.asList(modtmp.aliases).contains(missingModName)).findFirst().orElse(null);
        }

        return mod != null ? mod.downloadLink : null;
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
        HttpResponse<byte[]> response;
        Exception exception = null;
        int attempts = 3;
        for (int i = 0; i < attempts; i++) {
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

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
                Thread.sleep(1000 * 30); // Sleep for half a minute
                continue;
            }
            return response;
        }
        throw new Exception(exception);
    }
}
