package slaynash.lum.bot.discord.melonscanner;

import java.io.FileInputStream;
import java.io.IOException;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

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

    private static List<MelonScannerApi> apis = new ArrayList<>();

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .followRedirects(Redirect.ALWAYS)
            .build();

    private static Gson gson = new Gson();
    private static Globals server_globals;

    private static Thread fetchThread;
    private static Map<String, List<MelonApiMod>> games = new ConcurrentHashMap<>();

    static {
        apis.add(new MelonScannerApi("VRChat", "vrcmg", "https://api.vrcmg.com/v0/mods.json", true /* Check using hashes? */));
        apis.add(new MelonScannerApi("BloonsTD6", "btd6_inferno", "https://raw.githubusercontent.com/Inferno-Dev-Team/Inferno-Omnia/main/version.json", false /* Check using hashes? */));
        apis.add(new MelonScannerApi("BloonsTD6", "btd6_gurrenm4", "https://raw.githubusercontent.com/gurrenm3/MelonLoader-BTD-Mods/main/mods.json", false /* Check using hashes? */));
        apis.add(new MelonScannerApi("Audica", "audica_ahriana", "https://raw.githubusercontent.com/Ahriana/AudicaModsDirectory/main/api.json", false /* Check using hashes? */));
        apis.add(new MelonScannerApi("TheLongDark", "tld", "https://tld.xpazeapps.com/api.json", false /* Check using hashes? */));
        // apis.add(new MelonScannerApi("Domeo", "domeo", "", false /* Check using hashes? */));
    }

    public static void startFetchingThread() {
        fetchThread = new Thread(() -> {
            while (true) {

                // We use a temp Map to avoid clearing the common one
                Map<String, List<MelonApiMod>> gamesTemp = new HashMap<>();

                for (MelonScannerApi api : apis) {

                    HttpRequest request = HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create(api.endpoint))
                        .setHeader("User-Agent", "LUM Bot")
                        .timeout(Duration.ofSeconds(30))
                        .build();

                    try {

                        // API request

                        HttpResponse<String> response = downloadRequest(request, api.name);

                        String apiDataRaw = response.body();
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

                        FileInputStream fis = new FileInputStream("apiscripts/" + api.name + ".lua"); // TODO compile and cache
                        LuaValue modsLuaRaw = server_globals.load(fis, api.name + ".lua", "t", user_globals).call();
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

                                LuaTable mod = null;
                                try {
                                    mod = v.checktable();
                                }
                                catch (LuaError e) {
                                    System.err.println("Invalid value for key " + k + "\n" + ExceptionUtils.getStackTrace(e));
                                    continue;
                                }

                                String name = mod.get("name").checkjstring();
                                System.out.println("[API] Processing mod " + name);
                                String approvalStatus = "0";
                                if (mod.get("approvalStatus") != null && !mod.get("approvalStatus").isnil())
                                    approvalStatus = mod.get("approvalStatus").checkjstring();
                                String version = mod.get("version").checkjstring();
                                String downloadLink = mod.get("downloadLink") == LuaValue.NIL ? null : mod.get("downloadLink").checkjstring();
                                String hash = mod.get("hash") == LuaValue.NIL ? null : mod.get("hash").checkjstring();
                                String[] aliases = null;
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

                                if (approvalStatus != null && Integer.parseInt(approvalStatus) == 2)
                                    CommandManager.brokenMods.add(name);
                                apiMods.add(new MelonApiMod(name, version, downloadLink, aliases, hash));
                            }

                            api.cachedMods = apiMods;
                        }

                        // Update stored api datas

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
                                    if (VersionUtils.compareVersion(newMod.versions[0].version, currentMod.versions[0].version) > 0) {
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
                        ExceptionUtils.reportException("MelonScanner API Timed Out for " + api.endpoint);
                    }
                    catch (IOException exception) {
                        if (exception.getMessage().contains("GOAWAY")) {
                            ExceptionUtils.reportException(api.name + " is a meanie and told me to go away <a:kanna_cry:851143700297941042>");
                        }
                        else
                            ExceptionUtils.reportException("MelonScanner API Connection Error for " + api.endpoint, exception);
                    }
                    catch (Exception exception) {
                        ExceptionUtils.reportException("MelonScanner API Exception for " + api.endpoint, exception);
                    }
                }

                for (Entry<String, List<MelonApiMod>> entry : gamesTemp.entrySet())
                    games.put(entry.getKey(), entry.getValue());

                try {
                    Thread.sleep(6 * 60 * 1000); // 10 times / hour (every 6 minutes)
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }

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
        public final boolean compareUsingHashes;

        public List<MelonApiMod> cachedMods = new ArrayList<>();

        public MelonScannerApi(String game, String name, String endpoint, boolean compareUsingHashes) {
            this.game = game;
            this.name = name;
            this.endpoint = endpoint;
            this.compareUsingHashes = compareUsingHashes;
        }
    }

    public static List<MelonApiMod> getMods(String game) {
        if (game == null)
            return null;
        List<MelonApiMod> list = games.get(game);
        return list == null ? null : new ArrayList<MelonApiMod>(games.get(game));
    }

    public static boolean compareUsingHash(String game) {
        MelonScannerApi api = apis.stream().filter(api_ -> api_.game.equals(game)).findFirst().orElse(null);
        return api == null ? false : api.compareUsingHashes;
    }

    public static String getDownloadLinkForMod(String game, String missingModName) {
        if (game == null)
            return null;

        List<MelonApiMod> mods = games.get(game);
        if (mods == null)
            return null;

        MelonApiMod mod = mods.stream().filter(modtmp -> modtmp.name.equals(missingModName)).findFirst().orElse(null);

        if (mod == null) {
            mod = mods.stream().filter(modtmp -> Arrays.stream(modtmp.aliases).anyMatch(missingModName::equals)).findFirst().orElse(null);
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

    public static HttpResponse<String> downloadRequest(HttpRequest request, String source) throws Exception {
        return downloadRequest(httpClient, request, source);
    }
    public static HttpResponse<String> downloadRequest(HttpClient httpClient, HttpRequest request, String source) throws Exception {
        HttpResponse<String> response = null;
        Exception exception = null;
        int attempts = 4;
        for (int i = 0; i < attempts; i++) {
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() < 200 || response.statusCode() >= 400) {
                    // System.out.println("Lum gotten status code: " + response.statusCode() + " from " + source + " and is retrying");
                    throw new Exception("Lum gotten status code: " + response.statusCode() + " from " + source);
                }
                if (response.body() == null || response.body().isBlank()) {
                    // System.out.println(source + " provided empty response");
                    throw new Exception("Lum gotten an empty responce: " + response.statusCode() + " from " + source);
                }
            }
            catch (Exception e) {
                exception = e;
                System.out.println("Caught Exception " + e.getClass().getName() + " from " + source);
                Thread.sleep(1000 * 30); // Sleep for half a minute
                continue;
            }
            return response; //only returns if the above continue did not run
        }
        throw new Exception(exception);
    }
}
