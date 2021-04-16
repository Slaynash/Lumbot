package slaynash.lum.bot.discord;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.UrlShortener;
import slaynash.lum.bot.discord.logscanner.AudicaModDetails;
import slaynash.lum.bot.discord.logscanner.BTD6ModDetails;
import slaynash.lum.bot.discord.logscanner.ModDetails;
import slaynash.lum.bot.discord.logscanner.TheLongDarkModDetails;
import slaynash.lum.bot.discord.logscanner.VRCModDetails;
import slaynash.lum.bot.discord.logscanner.VRCModVersionDetails;
import slaynash.lum.bot.discord.logscanner.VersionUtils;

public class MelonLoaderScanner {
    
    //default values, to be replaced by command *to be added*
    public static String latestMLVersionRelease = "0.2.7.4";
    public static String latestMLVersionBeta = "0.3.0";
    public static String uixURL = "";
    
    private static List<MelonLoaderError> knownErrors = new ArrayList<MelonLoaderError>() {{
        add(new MelonLoaderError(
                "\\[[0-9.:]+\\] \\[emmVRCLoader\\] You have emmVRC's Stealth Mode enabled..*",
                "You have emmVRC's Stealth Mode enabled. To access the functions menu, press the \"Report World\" button. Most visual functions of emmVRC have been disabled."));
        // add(new MelonLoaderError(
        //         "\\[[0-9.:]+\\] \\[ERROR\\] System.BadImageFormatException:.*",
        //         "You have an invalid or incompatible assembly in your `Mods` or `Plugins` folder."));
        add(new MelonLoaderError(
                 "\\[[0-9.:]+\\] ClickFix.*",
                 "ClickFix was added into VRChat and no longer needed. Please delete it from your Mods folder."));
        add(new MelonLoaderError(
                "\\[[0-9.:]+\\] \\[.*\\] \\[Error\\] System\\.IO\\.FileNotFoundException\\: Could not load file or assembly.*",
                "One or more mod is missing a library / required mod, or a file is corrupted."));
        //This should hopefully be fixed in 0.3.1
        add(new MelonLoaderError(
                "\\[[0-9.:]+\\] \\[INTERNAL FAILURE\\] Failed to Read Unity Version from File Info or globalgamemanagers\\!",
                "MelonLoader failed to read your Unity version and game name. Try re-installing MelonLoader or delete UnityCrashHandler64.exe."));
        /*
        add(new MelonLoaderError(
                "\\[[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}\\] \\[emmVRCLoader\\] \\[ERROR\\] System\\.Reflection\\.TargetInvocationException: Exception has been thrown by the target of an invocation\\. ---> System\\.TypeLoadException: Could not load type of field 'emmVRC\\.Hacks\\.FBTSaving\\+<>c__DisplayClass5_0:steam'.*",
                "emmVRC currently has some incompatibilities with the Oculus build as of the latest VRChat update. For now, the Steam build is recommended."));
        */
        add(new MelonLoaderError(
                "\\[[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}\\] \\[OculusPlayspaceMover\\] \\[ERROR\\] OVRCameraRig not found, this mod only work in Oculus for now\\!",
                "OculusPlayspaceMover does not work in SteamVR. It is recommended to use OVR Advanced Settings for a playspace mover <https://youtu.be/E4ZByfPWTuM>"));
        //This should hopefully be fixed in 0.3.1
        add(new MelonLoaderError(
                "\\[[0-9.:]+\\] \\[ERROR\\] Settings load failed: System\\.InvalidOperationException: The document has errors:.*",
                "Mod config has been corrupted. Please delete UserData/MelonPreferences.cfg"));
        /*
        add(new MelonLoaderError(
                ".*Harmony\\.HarmonyInstance\\..*",
                "You seems to have a 0Harmony.dll file in your `Mods` or `Plugins` folder. This breaks mods and plugins, since Harmony is embed into MelonLoader"));
        */
        //emmVRC error
        add(new MelonLoaderError(
                "\\[[0-9.:]+\\] \\[emmVRCLoader\\] \\[ERROR\\] System.NullReferenceException: Object reference not set to an instance of an object\\r\\n\\r\\n  at System.Net.AutoWebProxyScriptEngine.*",
                "Please open Window's \"Change Proxy Settings\" and disable all three toggles."));
        add(new MelonLoaderError(
                "\\[[0-9.:]+\\] \\[emmVRCLoader\\] \\[ERROR\\] System.Net.WebException.*",
                "Please open Window's \"Change Proxy Settings\" and disable all three toggles. It also could a firewall blocking the connection."));
        add(new MelonLoaderError(
                "\\[[0-9.:]+\\] \\[emmVRCLoader\\] \\[ERROR\\] Your instance history file is invalid. It will be wiped.",
                "emmVRC instance history became corrupted and has been reset."));
    }};
    
    private static List<MelonLoaderError> knownUnhollowerErrors = new ArrayList<MelonLoaderError>() {{
        add(new MelonLoaderError(
                ".*System\\.IO\\.FileNotFoundException\\: .* ['|\"]System\\.IO\\.Compression.*", 
                "You are actually missing the required .NET Framework for MelonLoader.\nPlease make sure to install it using the following link: <https://dotnet.microsoft.com/download/dotnet-framework/net48>"));
        add(new MelonLoaderError(
                "System.UnauthorizedAccessException:.*",
                "The access to a file has been denied. Please make sure the game is closed when installing MelonLoader, or try restarting your computer. If this doesn't work, try running the MelonLoader Installer with administrator privileges"));
        add(new MelonLoaderError(
                "System.IO.IOException: The process cannot access the file.*",
                "The access to a file has been denied. Please make sure the game is closed when installing MelonLoader, or try restarting your computer. If this doesn't work, try running the MelonLoader Installer with administrator privileges"));
        add(new MelonLoaderError(
                "SHA512 Hash from Temp File does not match Repo Hash!",
                "Installer failed to download MelonLoader successfully. Try again later or redownload the official MelonLoader installer."));
        add(new MelonLoaderError(
                "\\[[0-9.:]+\\]    at MelonLoader\\.AssemblyGenerator\\.LocalConfig\\.Save\\(String path\\)",
                "The access to a file has been denied. Please try starting the game with administrator privileges, or try restarting your computer (failed to save AssemblyGenerator/config.cfg)"));
        add(new MelonLoaderError(
                "\\[[0-9.:]+\\]    at MelonLoader\\.AssemblyGenerator\\.Main\\.SetupDirectory\\(String path\\)",
                "The access to a file has been denied. Please try starting the game with administrator privileges, or try restarting your computer (failed to setup directories)"));
        add(new MelonLoaderError(
                "\\[[0-9.:]+\\] Il2CppDumper.exe does not exist!",
                "MelonLoader assembly generation failed. Please delete the `MelonLoader` folder and `version.dll` file from your game folder, and install MelonLoader again (failed to download Il2CppDumper)"));
        add(new MelonLoaderError(
                "\\[[0-9.:]+\\] \\[INTERNAL FAILURE\\] Failed to Execute Assembly Generator!",
                "The assembly generation failed. This is most likely caused by your anti-virus. Add an exception or disable it, then try again."));
        add(new MelonLoaderError(
                "\\[[0-9.:]+\\] \\[ERROR\\] Unhandled Exception: System\\.IO\\.IOException: There is not enough space on the disk\\.",
                "The storage is full. Please make some space and try your game again."));
        add(new MelonLoaderError(
                "\\[[0-9.:]+\\] \\[ERROR\\] Unhandled Exception: System\\.Collections\\.Generic\\.KeyNotFoundException: The given key was not present in the dictionary\\.",
                "The assembly generation failed. This is most likely caused by your anti-virus. Add an exception or disable it, then try again."));
        add(new MelonLoaderError(
                "\\[[0-9.:]+\\] ERROR: Can't use auto mode to process file, try manual mode.",
                "Il2CppDumper generation failed. Please verify the integrity of your game or reinstall MelonLoader."));
        add(new MelonLoaderError(
                "\\[[0-9.:]+\\] \\[INTERNAL FAILURE\\] Failed to Find Mono Directory!",
                "Missing Mono Directory. Please verify the integrity of your game or reinstall MelonLoader."));
        add(new MelonLoaderError(
                "\\[[0-9.:]+\\] \\[INTERNAL FAILURE\\] MelonLoader.dll Does Not Exist!",
                "Missing MelonLoader/MelonLoader.dll. Please do not move it or whitelist it in your virus scanner."));
        add(new MelonLoaderError(
                ".*Phasmophobia.*",
                "We do not support the use of MelonLoader on Phasmophobia,\n nor does Phasmophobia support MelonLoader.\nPlease remove MelonLoader, Mods, Plugins, UserData, NOTICE.txt, and version.dll."));
        add(new MelonLoaderError(
                ".*Unexpected escape character.*",
                "Please use forward slashes (`/`) in MelonPreferences.cfg"));
        add(new MelonLoaderError(
                ".*No MelonInfoAttribute Found.*",
                "An old invalid mod attempted to load."));
        add(new MelonLoaderError(
                ".*Invalid Version given to MelonInfoAttribute.*",
                "An invalid/broken mod attempted to load."));
        add(new MelonLoaderError(
                ".*Il2CppDumper\\.BinaryStream\\.ReadClassArray\\[T\\]\\(Int64.*",
                "Il2CppDumper encountered an error. On MelonLoader 0.3.0 ALPHA, go to the `MelonLoader` folder of your game, open `LaunchOptions.ini`, and set the following values:\n```ini\n[AssemblyGenerator]\nForceUnityDependencies=false\nForceUnityDependencies_Version=0.0.0.0\nForceIl2CppDumper=true\nForceIl2CppDumper_Version=6.5.3\nForceIl2CppAssemblyUnhollower=false\nForceIl2CppAssemblyUnhollower_Version=0.0.0.0\nForceCpp2IL_Version=6.5.3\n```"));
    }};
    
    private static MelonLoaderError incompatibleAssemblyError = new MelonLoaderError(
            "\\[[0-9.:]+\\] \\[ERROR\\] System.BadImageFormatException:.*",
            "You have an invalid or incompatible assembly in your `Mods` or `Plugins` folder.");
    
    
    private static Gson gson = new Gson();
    public static Map<String, List<ModDetails>> mods = new HashMap<>();
    private static Map<String, Boolean> checkUsingHashes = new HashMap<String, Boolean>() {{
        put("VRChat", false);
        put("BloonsTD6", false);
        put("Audica", false);
        put("TheLongDark", false);
        
        put("BONEWORKS", true);
    }};
    
    private final static HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();
    
    private static Map<String, String> modNameMatcher = new HashMap<String, String>() {{
        // MelonInfo name -> Submitted name
        put("Advanced Invites", "AdvancedInvites");
        put("Advanced Safety", "AdvancedSafety");
        put("Core Limiter", "CoreLimiter");
        put("DiscordRichPresence-ML", "VRCDiscordRichPresence-ML");
        put("Game Priority Changer", "GamePriority");
        put("Input System", "InputSystem");
        put("MControl", "MControl (Music Playback Controls)");
        put("MultiplayerDynamicBones", "Multiplayer Dynamic Bones");
        put("MuteBlinkBeGone", "Mute Blink Be Gone");
        put("NearClippingPlaneAdjuster.dll", "NearClipPlaneAdj");
        put("No Steam. At all.", "NoSteamAtAll");
        put("Particle and DynBone limiter settings UI", "ParticleAndBoneLimiterSettings");
        put("Player Rotater", "Player Rotater (Desktop Only)");
        put("Player Volume Control", "PlayerVolumeControl");
        put("Rank Volume Control", "RankVolumeControl");
        put("Runtime Graphics Settings", "RuntimeGraphicsSettings");
        put("ThumbParams", "VRCThumbParams");
        put("Toggle Mic Icon", "ToggleMicIcon");
        put("TogglePostProcessing", "Toggle Post Processing");
        put("UI Expansion Kit", "UIExpansionKit");
        put("VRC Video Library", "VRCVideoLibrary");
        
        // backward compatibility
        put("BTKSANameplateFix", "BTKSANameplateMod");
    }};
    
    public static void Init() {
        
        Thread t = new Thread(() -> {
            System.out.println("MelonLoaderScannerThread start");
            while (true) {
                
                // VRChat
                
                HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create("https://api.vrcmg.com/v0/mods.json"))
                    .setHeader("User-Agent", "LUM Bot")
                    .build();
                
                try {
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    
                    CommandManager.brokenVrchatMods.clear();
                    
                    synchronized (mods) {
                        List<VRCModDetails> vrcmods = gson.fromJson(response.body(), new TypeToken<ArrayList<VRCModDetails>>() {}.getType());
                        
                        List<ModDetails> modsprocessed = new ArrayList<>();
                        for (VRCModDetails processingmods : vrcmods) {
                            VRCModVersionDetails vrcmoddetails = processingmods.versions[0];
                            modsprocessed.add(new ModDetails(vrcmoddetails.name, vrcmoddetails.modversion, vrcmoddetails.downloadlink));
                            
                            // Add to broken mod list if broken
                            if (vrcmoddetails.approvalstatus == 2)
                                CommandManager.brokenVrchatMods.add(vrcmoddetails.name);
                            if ("UI Expansion Kit".equals(vrcmoddetails.name))
                                uixURL = vrcmoddetails.downloadlink;
                        }
                        
                        mods.put("VRChat", modsprocessed);
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                
                // BTD6
                
                request = HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create("https://raw.githubusercontent.com/Inferno-Dev-Team/Inferno-Omnia/main/version.json"))
                        .setHeader("User-Agent", "LUM Bot")
                        .build();
                    
                try {
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    
                    synchronized (mods) {
                        Map<String, BTD6ModDetails> processingmods = gson.fromJson(response.body(), new TypeToken<HashMap<String, BTD6ModDetails>>() {}.getType());
                        
                        List<ModDetails> modsprocessed = new ArrayList<>();
                        for (Entry<String, BTD6ModDetails> mod : processingmods.entrySet()) {
                            modsprocessed.add(new ModDetails(mod.getKey(), mod.getValue().version, null));
                        }
                        
                        mods.put("BloonsTD6", modsprocessed);
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                
                // Audica
                
                request = HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create("https://raw.githubusercontent.com/Ahriana/AudicaModsDirectory/main/api.json"))
                        .setHeader("User-Agent", "LUM Bot")
                        .build();
                    
                try {
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    
                    synchronized (mods) {
                        Map<String, AudicaModDetails> processingmods = gson.fromJson(response.body(), new TypeToken<HashMap<String, AudicaModDetails>>() {}.getType());
                        
                        List<ModDetails> modsprocessed = new ArrayList<>();
                        for (Entry<String, AudicaModDetails> mod : processingmods.entrySet()) {
                            modsprocessed.add(new ModDetails(mod.getKey(), mod.getValue().version, mod.getValue().download[0].browser_download_url));
                        }
                        
                        mods.put("Audica", modsprocessed);
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }

                // The Long Dark

                request = HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create("https://tld.xpazeapps.com/api.json"))
                        .setHeader("User-Agent", "LUM Bot")
                        .build();
                    
                try {
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    
                    synchronized (mods) {
                        Map<String, TheLongDarkModDetails> processingmods = gson.fromJson(response.body(), new TypeToken<HashMap<String, TheLongDarkModDetails>>() {}.getType());
                        
                        List<ModDetails> modsprocessed = new ArrayList<>();
                        for (Entry<String, TheLongDarkModDetails> mod : processingmods.entrySet()) {
                            modsprocessed.add(new ModDetails(mod.getKey(), mod.getValue().version, mod.getValue().download.browser_download_url));
                        }
                        
                        mods.put("TheLongDark", modsprocessed);
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                
                // BONEWORKS
                
                // TODO
                
                // Sleep
                
                try {
                    Thread.sleep(6 * 60 * 1000); // 10 times / hour (every 6 minutes)
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            
        }, "MelonLoaderScannerThread");
        t.setDaemon(true);
        t.start();
    }

    public static void scanLogs(MessageReceivedEvent event) {
        List<Attachment> attachments = event.getMessage().getAttachments();
        
        List<MelonLoaderError> errors = new ArrayList<MelonLoaderError>();
        String mlVersion = null;
        boolean hasErrors = false, hasNonModErrors = false;
        boolean assemblyGenerationFailed = false;
        String game = null;
        String mlHashCode = null;
        
        boolean preListingMods = false;
        boolean listingMods = false;
        boolean readingMissingDependencies = false;
        Map<String, LogsModDetails> loadedMods = new HashMap<String, LogsModDetails>();
        
        List<String> duplicatedMods = new ArrayList<String>();
        List<String> unknownMods = new ArrayList<String>();
        List<String> universalMods = new ArrayList<String>();
        List<String> incompatibleMods = new ArrayList<String>();
        List<MelonOutdatedMod> outdatedMods = new ArrayList<MelonOutdatedMod>();
        Map<String, String> modAuthors = new HashMap<String, String>();
        List<String> missingMods = new ArrayList<>();
        List<String> brokenMods = new ArrayList<>();
        
        List<String> modsThrowingErrors = new ArrayList<String>();
        
        String emmVRCVersion = null;
        String emmVRCVRChatBuild = null;
        
        boolean isMLOutdatedVRC = false;
        boolean isMLOutdatedVRCBrokenDeobfMap = false;
        
        boolean noMods = false, noPlugins = false;
        boolean consoleCopyPaste = false;
        boolean pre3 = false;
        boolean alpha = false;
        Color messageColor = Color.BLUE;
        Color melonPink = new Color(255,59,106); 
        
        int remainingModCount = 0;
        
        int ommitedLines = 0;
        
        String currentMissingDependenciesMods = "";
        
        String tmpModName = null, tmpModVersion = null, tmpModHash = null;
        
        for (int i = 0; i < attachments.size(); ++i) {
            Attachment attachment = attachments.get(i);
            
            if (attachment.getFileExtension() != null && (attachment.getFileExtension().toLowerCase().equals("log") || attachment.getFileExtension().toLowerCase().equals("txt"))) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(attachment.retrieveInputStream().get()))) {
                    
                    System.out.println("Reading file " + attachment.getFileName());
                    //String lastModName = null;
                    String line = "";
                    String lastLine = null;
                    while ((lastLine = line) != null && (line = br.readLine()) != null) {
                        
                        
                        //System.out.println("reading line");
                        int linelength = line.length();
                        //System.out.println("length: " + linelength);
                        
                        if (linelength > 1000) {
                            ++ommitedLines;
                            line = "";
                            System.out.println("Ommited one line of length " + linelength);
                            continue;
                        }
                        
                        // Mod listing
                        
                        if (preListingMods || listingMods) {
                            if (!pre3) {
                                if (line.isBlank())
                                    continue;
                                
                                else if (preListingMods && line.matches("\\[[0-9.:]+\\] ------------------------------"));
                                else if (preListingMods && (line.matches("\\[[0-9.:]+\\]( \\[MelonLoader\\]){0,1} No Plugins Loaded!") || line.matches("\\[[0-9.:]+\\]( \\[MelonLoader\\]){0,1} No Mods Loaded!"))) {
                                    remainingModCount = 0;
                                    preListingMods = false;
                                    listingMods = false;
                                    if (line.contains("Plugins"))
                                        noPlugins = true;
                                    else
                                        noMods = true;
                                    
                                    System.out.println("No mod/plugins loaded for this pass");
                                    
                                    continue;
                                }
                                else if (preListingMods && (line.matches("\\[[0-9.:]+\\]( \\[MelonLoader\\]){0,1} [0-9]+ Plugins? Loaded") || line.matches("\\[[0-9.:]+\\]( \\[MelonLoader\\]){0,1} [0-9]+ Mods? Loaded"))) {
                                    remainingModCount = Integer.parseInt(line.split(" ")[1]);
                                    preListingMods = false;
                                    listingMods = true;
                                    System.out.println(remainingModCount + " mods or plugins loaded on this pass");
                                    br.readLine(); // Skip line separator
                                    
                                    continue;
                                }
                                else if (listingMods && tmpModName == null) {
                                    String[] split = line.split(" ", 2)[1].split(" v", 2);
                                    tmpModName = ("".equals(split[0])) ? "Broken Mod" : split[0];
                                    tmpModVersion = split.length > 1 ? split[1] : null;
                                    continue;
                                }
                                else if (line.matches("\\[[0-9.:]+\\]( \\[MelonLoader\\]){0,1} by .*")) { // Skip author
                                    continue;
                                }
                                else if (line.matches("\\[[0-9.:]+\\]( \\[MelonLoader\\]){0,1} SHA256 Hash: [a-zA-Z0-9]+")) {
                                    tmpModHash = line.split(" ")[3];
                                    continue;
                                }
                                else if (line.matches("\\[[0-9.:]+\\]( \\[MelonLoader\\]){0,1} ------------------------------")) {
                                    
                                    System.out.println("Found mod " + tmpModName + ", version is " + tmpModVersion + ", and hash is " + tmpModHash);
                                    
                                    if (loadedMods.containsKey(tmpModName) && !duplicatedMods.contains(tmpModName))
                                        duplicatedMods.add(tmpModName.trim());
                                    loadedMods.put(tmpModName.trim(), new LogsModDetails(tmpModVersion, tmpModHash));
                                    //if (tmpModAuthor != null)
                                    //    modAuthors.put(tmpModName.trim(), tmpModAuthor.trim());
                                    
                                    tmpModName = null;
                                    tmpModVersion = null;
                                    tmpModHash = null;
                                    
                                    --remainingModCount;
                                    
                                    if (remainingModCount == 0) {
                                        preListingMods = false;
                                        listingMods = false;
                                        System.out.println("Done scanning mods");
                                        
                                        continue;
                                    }
                                }
                            }
                        }
                        
                        // Missing dependencies listing
                        
                        if (line.matches("- '.*' is missing the following dependencies:")) {
                            currentMissingDependenciesMods = line.split("'", 3)[1];
                            readingMissingDependencies = true;
                            continue;
                        }
                        
                        if (readingMissingDependencies) {
                            if (line.matches("    - '.*'.*")) {
                                String missingModName = line.split("'", 3)[1];
                                if (!missingMods.contains(missingModName))
                                    missingMods.add(missingModName);
                                
                                continue;
                            }
                            else {
                                System.out.println("Done listing missing dependencies on line: " + line);
                                readingMissingDependencies = false;
                            }
                        }
                        
                        if (line.isEmpty());
                        else if (line.matches("\\[[0-9.:]+\\]( \\[MelonLoader\\]){0,1} Using v0\\..*")) {
                            if (line.matches("\\[[0-9.:]+\\] \\[MelonLoader\\] .*"))
                                consoleCopyPaste = true;
                            mlVersion = line.split("v")[1].split(" ")[0].trim();
                            pre3 = true;
                            if ("VRChat".equals(game))
                                isMLOutdatedVRC = true;
                            System.out.println("ML " + mlVersion + " (< 0.3.0)");
                        }
                        else if (line.matches("\\[[0-9.:]+\\]( \\[MelonLoader\\]){0,1} MelonLoader v0\\..*")) {
                            if (line.matches("\\[[0-9.:]+\\] \\[MelonLoader\\] .*"))
                                consoleCopyPaste = true;
                            mlVersion = line.split("v")[1].split(" ")[0].trim();
                            alpha = line.toLowerCase().contains("alpha");
                            System.out.println("ML " + mlVersion + " (>= 0.3.0). Alpha: " + alpha);
                        }
                        else if (line.matches("\\[[0-9.:]+\\]( \\[MelonLoader\\]){0,1} Name: .*")) {
                            game = line.split(":", 4)[3].trim();
                            System.out.println("Game: " + game);
                            
                            if (mlHashCode != null && "VRChat".equals(game)) {
                                System.out.println("game is vrc, checking hash");
                                isMLOutdatedVRC = true;
                                
                                boolean hasVRChat1043ReadyML = false;
                                boolean hasNotBrokenDeobfMap = false;
                                for (MLHashPair mlHashes : (alpha ? CommandManager.melonLoaderAlphaHashes : CommandManager.melonLoaderHashes)) {
                                    System.out.println("x86: " + mlHashes.x86 + ", x64: " + mlHashes.x64);
                                    if (mlHashes.x64.equals("25881"))
                                        hasNotBrokenDeobfMap = true;
                                    if (mlHashes.x64.equals(CommandManager.melonLoaderVRCHash))
                                        hasVRChat1043ReadyML = true;
                                    
                                    if (mlHashes.x64.equals(mlHashCode)) {
                                        System.out.println("matching hash found");
                                        if (hasVRChat1043ReadyML)
                                            isMLOutdatedVRC = false;
                                        if (!hasNotBrokenDeobfMap)
                                            isMLOutdatedVRCBrokenDeobfMap = true;
                                        break;
                                    }
                                }
                            }
                        }
                        else if (line.matches("\\[[0-9.:]+\\]( \\[MelonLoader\\]){0,1} Hash Code: .*")) {
                            mlHashCode = line.split(":", 4)[3].trim();
                            System.out.println("Hash Code: " + mlHashCode);
                        }
                        else if (line.matches("\\[[0-9.:]+\\]( \\[MelonLoader\\]){0,1} Game Compatibility: .*")) {
                            if (lastLine.isBlank())
                                continue;
                            
                            String modnameversionauthor = lastLine.split("\\[[0-9.:]+\\]( \\[MelonLoader\\]){0,1} ", 2)[1].split("\\((http[s]{0,1}:\\/\\/){0,1}[a-zA-Z0-9\\-]+\\.[a-zA-Z]{2,4}", 2)[0];
                            String[] split2 = modnameversionauthor.split(" by ", 2);
                            String author = split2.length > 1 ? split2[1] : null;
                            String[] split3 = split2[0].split(" v", 2);
                            String name = split3[0].isBlank() ? "" : split3[0];
                            name = String.join("", name.split(".*[a-zA-Z0-9]\\.[a-zA-Z]{2,4}"));
                            String version = split3.length > 1 ? split3[1] : null;
                            
                            if (loadedMods.containsKey(name) && !duplicatedMods.contains(name))
                                duplicatedMods.add(name.trim());
                            loadedMods.put(name.trim(), new LogsModDetails(version, null));
                            if (author != null)
                                modAuthors.put(name.trim(), author.trim());
                            
                            String compatibility = line.split("\\[[0-9.:]+\\]( \\[MelonLoader\\]){0,1} Game Compatibility: ", 2)[1];
                            if (compatibility.equals("Universal"))
                                universalMods.add(name);
                            else if (compatibility.equals("Compatible")) {}
                            else
                                incompatibleMods.add(name);
                            
                            System.out.println("Found mod " + name.trim() + ", version is " + version + ", compatibility is " + compatibility);
                        }
                        // VRChat / EmmVRC Specifics
                        else if (line.matches("\\[[0-9.:]+\\] \\[emmVRCLoader\\] VRChat build is.*")) {
                            emmVRCVRChatBuild = line.split(":", 4)[3].trim();
                            System.out.println("VRChat " + emmVRCVRChatBuild);
                        }
                        // VRChat / EmmVRC Specifics
                        else if (line.matches("\\[[0-9.:]+\\] \\[emmVRCLoader\\] You are running version .*")) {
                            emmVRCVersion = line.split("version", 2)[1].trim();
                            System.out.println("EmmVRC " + emmVRCVersion);
                        }
                        else if (!pre3 && (line.matches("\\[[0-9.:]+\\] Loading Plugins...") || line.matches("\\[[0-9.:]+\\] Loading Mods..."))) {
                            preListingMods = true;
                            System.out.println("Starting to pre-list mods/plugins");
                        }
                        else if (line.matches("\\[[0-9.:]+\\] \\[ERROR\\] An item with the same key has already been added.*")) {
                            System.out.println("Duplicate in Mods and Plugins");
                            duplicatedMods.add(line.substring(line.lastIndexOf(":")+2));
                        }
                        else if (line.matches("\\[[0-9.:]+\\] \\[Warning\\] Some mods are missing dependencies, which you may have to install\\.")) {
                            System.out.println("Starting to list missing dependencies");
                            readingMissingDependencies = true;
                            br.readLine(); // If these are optional dependencies, mark them as optional using the MelonOptionalDependencies attribute.
                            line = br.readLine(); // This warning will turn into an error and mods with missing dependencies will not be loaded in the next version of MelonLoader.
                        }
                        
                        else {
                            boolean found = false;
                            for (MelonLoaderError knownError : knownUnhollowerErrors) {
                                if (line.matches(knownError.regex)) {
                                    if (isMLOutdatedVRCBrokenDeobfMap) {
                                        if (!assemblyGenerationFailed)
                                            errors.add(new MelonLoaderError("", "The assembly generation failed. You will need to reinstall MelonLoader for it to work"));
                                    }
                                    else {
                                        if (!errors.contains(knownError))
                                            errors.add(knownError);
                                    }
                                    System.out.println("Found known unhollower error");
                                    hasErrors = true;
                                    assemblyGenerationFailed = true;
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                for (MelonLoaderError knownError : knownErrors) {
                                    if (line.matches(knownError.regex)) {
                                        if (!errors.contains(knownError))
                                            errors.add(knownError);
                                        System.out.println("Found known error");
                                        hasErrors = true;
                                        found = true;
                                        break;
                                    }
                                }
                            }
                            if (!found) {
                                if (line.matches("\\[[0-9.:]+\\] \\[ERROR\\] System.BadImageFormatException:.*")) {
                                    if (!errors.contains(incompatibleAssemblyError))
                                        errors.add(incompatibleAssemblyError);
                                }
                                else if (line.matches("\\[[0-9.:]+\\]( \\[MelonLoader\\]){0,1} \\[[^\\[]+\\] \\[(Error|ERROR)\\].*") && !line.matches("\\[[0-9.:]+\\] \\[MelonLoader\\] \\[(Error|ERROR)\\].*")) {
                                    String mod = line.split("\\[[0-9.:]+\\]( \\[MelonLoader\\]){0,1} \\[", 2)[1].split("\\]", 2)[0];
                                    if (!modsThrowingErrors.contains(mod))
                                        modsThrowingErrors.add(mod);
                                    System.out.println("Found mod error, caused by " + mod + ": " + line);
                                    hasErrors = true;
                                }
                                else if (line.matches("\\[[0-9.:]+\\]( \\[MelonLoader\\]){0,1} \\[(Error|ERROR)\\].*")) {
                                    hasErrors = true;
                                    hasNonModErrors = true;
                                    System.out.println("Found non-mod error: " + line);
                                }
                            }
                        }
                    }
                    
                } catch (InterruptedException | ExecutionException | IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Done reading file");
        }
        
        boolean isMLOutdated = mlVersion != null && !(mlVersion.equals(latestMLVersionRelease) || mlVersion.equals(latestMLVersionBeta));
        
        List<ModDetails> modDetails = null;
        
        if (game != null)
            modDetails = mods.get(game);
        
        boolean checkUsingHash = false;
        boolean hasMLHashes = false;
        
        if (modDetails != null) {
            
            checkUsingHash = checkUsingHashes.get(game);
            
            hasMLHashes = loadedMods.values().size() > 0 ? loadedMods.values().toArray(new LogsModDetails[0])[0].hash != null : checkUsingHash;
            
            if (checkUsingHash ? hasMLHashes : true) {
                for (Entry<String, LogsModDetails> entry : loadedMods.entrySet()) {
                    String modName = entry.getKey();
                    LogsModDetails logsModDetails = entry.getValue();
                    VersionUtils.VersionData modVersion = logsModDetails.version != null ? VersionUtils.GetVersion(logsModDetails.version) : null;
                    String modHash = logsModDetails.hash;
                    
                    if (modVersion == null) {
                        unknownMods.add(modName);
                        continue;
                    }
                    
                    String matchedModName = modNameMatcher.get(entry.getKey().trim());
                    if (matchedModName != null) {
                        modAuthors.put(matchedModName, modAuthors.get(modName));
                    }
                    
                    VersionUtils.VersionData latestModVersion = null;
                    String latestModHash = null;
                    String latestModDownloadUrl = null;
                    for (ModDetails modDetail : modDetails) {
                        if (modDetail.name.replace(" ", "").equals(modName.replace(" ", ""))) {
                            if (checkUsingHash) {
                                // TODO
                            }
                            else {
                                System.out.println("Mod found in db: " + modDetail.name + " version " + modDetail.versions[0].version.getRaw());
                                latestModVersion = modDetail.versions[0].version;
                                latestModDownloadUrl = modDetail.downloadLink;
                                break;
                            }
                        }
                    }

                    //System.out.println("modVersion: " + modVersion);
                    //System.out.println("latestModVersion: " + latestModVersion);
                    
                    if (latestModVersion == null && latestModHash == null) {
                        unknownMods.add(modName);
                    }
                    else if (CommandManager.brokenVrchatMods.contains(modName)) {
                        brokenMods.add(modName);
                    }
                    else if (!checkUsingHash ? VersionUtils.CompareVersion(latestModVersion, modVersion) > 0 : (modHash != null && !modHash.equals(latestModHash))) {
                        outdatedMods.add(new MelonOutdatedMod(modName, modVersion.getRaw(), latestModVersion.getRaw(), latestModDownloadUrl));
                        modsThrowingErrors.remove(modName);
                    }
                    /* TODO
                    else if (modName.equals("emmVRC")) {
                        if (emmVRCVersion == null) {
                            
                        }
                        else {
                            
                        }
                    }
                    */
                }
            }
        }
        
        if (mlHashCode != null) {
            boolean found = false;
            for (MLHashPair hashes : (alpha ? CommandManager.melonLoaderAlphaHashes : CommandManager.melonLoaderHashes)) {
                if (mlHashCode.equals(hashes.x64) || mlHashCode.equals(hashes.x86)) {
                    found = true;
                    break;
                }
            }
            System.out.println("hash found in known hashes: " + found);
            
            if (!found) {
                reportUserModifiedML(event);
            }
        }
        
        String message = "";
        EmbedBuilder eb = new EmbedBuilder();
        MessageBuilder mb = new MessageBuilder();
        eb.setTitle("Log Autocheck Result:");
        eb.setTimestamp(Instant.now());
        eb.setFooter("Lum Log Scanner");
        mb.append("<@" + event.getAuthor().getId() + ">");
        
        for (int i = 0; i < attachments.size(); ++i) {
            Attachment attachment = attachments.get(i);
            if (attachment.getFileName().matches("MelonLoader_[0-9]{2}-[0-9]{2}-[0-9]{2}_[0-9]{2}-[0-9]{2}-[0-9]{2}\\.[0-9]{3}.*\\.log")) {
                String fileDateString = attachment.getFileName().split("_", 3)[1];
                long ageDays = 0;
                try {
                    LocalDate fileDate = LocalDate.parse("20" + fileDateString);
                    LocalDate now = LocalDate.now();
                    ageDays = ChronoUnit.DAYS.between(fileDate, now);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                
                if (ageDays > 1) {
                    if (pre3)
                        message += "This log file is " + ageDays + " days old\n";
                    else
                        message += "This log file is " + ageDays + " days old. Reupload your log from MelonLoader/Latest.log\n";
                }
            }
        }
        
        if (ommitedLines > 0)
            message += "*Ommited " + ommitedLines + " lines of length > 1000.*\n";
        
        if (consoleCopyPaste)
            message += "*You sent a copy of the console logs. Please type `!logs` to know where to find the complete game logs.*\n";
        
        if (game != null && mlVersion != null && !latestMLVersionBeta.equals(latestMLVersionRelease) && mlVersion.equals(latestMLVersionBeta))
            message += "*You are running an alpha version of MelonLoader.*\n";
        
        if (game != null && checkUsingHash && !hasMLHashes)
            message += "*Your MelonLoader doesn't provide mod hashes (requires >0.3.0). Mod versions will not be verified.*\n";
        else if (game != null && modDetails == null)
            message += "*" + game + " isn't officially supported by the autochecker. Mod versions will not be verified.*\n";
        
        eb.setDescription(message);
        
        if (errors.size() > 0 || isMLOutdated || isMLOutdatedVRC || duplicatedMods.size() != 0 || unknownMods.size() != 0 || outdatedMods.size() != 0 || brokenMods.size() != 0 || incompatibleMods.size() != 0 || modsThrowingErrors.size() != 0 || missingMods.size() != 0 || (mlVersion != null && loadedMods.size() == 0) || (noPlugins && noMods)) {
            
            if (isMLOutdatedVRC) {
                if (pre3)
                    eb.addField("Warning:", "Please update MelonLoader using the [official installer](https://github.com/LavaGang/MelonLoader.Installer/releases/latest/download/MelonLoader.Installer.exe) and check `Show ALPHA Pre-Releases`\nVRChat modding requires MelonLoader " + latestMLVersionBeta + " released after **" + CommandManager.melonLoaderVRCMinDate + ".**", false);
                else
                    eb.addField("Warning:", "Please reinstall MelonLoader using the [official installer](https://github.com/LavaGang/MelonLoader.Installer/releases/latest/download/MelonLoader.Installer.exe)\nVRChat modding requires MelonLoader " + latestMLVersionBeta + " released after **" + CommandManager.melonLoaderVRCMinDate + ".**", false);
            }
            else if (isMLOutdated)
                eb.addField("Warning:", "Please update MelonLoader using the [official installer](https://github.com/LavaGang/MelonLoader.Installer/releases/latest/download/MelonLoader.Installer.exe)\nThe installed MelonLoader is outdated: " + sanitizeInputString(mlVersion) + " -> " + latestMLVersionRelease + ".", false);
            
            
            if (emmVRCVRChatBuild != null && !emmVRCVRChatBuild.equals(CommandManager.vrchatBuild)) {
                eb.addField("VRChat:", "You are running an outdated version of VRChat: `" + sanitizeInputString(emmVRCVRChatBuild) + "` -> `" + CommandManager.vrchatBuild + "`", false);
                messageColor = Color.YELLOW;
            }
            
            if (duplicatedMods.size() > 0) {
                String error = "";
                for (int i = 0; i < duplicatedMods.size() && i < 10; ++i)
                    error += "- " + sanitizeInputString(duplicatedMods.get(i) + "\n");
                if (duplicatedMods.size() > 10)
                    error += "- and " + (duplicatedMods.size() - 10) + " more...";
               
                eb.addField("Duplicate mods:", error , false);
                messageColor = Color.RED;
            }
            
            if (missingMods.size() > 0) {
                String error = "";
                for (int i = 0; i < missingMods.size() && i < 10; ++i)
                    if ("UIExpansionKit".equals(sanitizeInputString(missingMods.get(i))))
                        error += "- [UI Expansion Kit]("+ uixURL +")\n";
                    else
                        error += "- " + sanitizeInputString(missingMods.get(i) + "\n");
                if (missingMods.size() > 10)
                    error += "- and " + (missingMods.size() - 10) + " more...";
                
                eb.addField("Missing dependencies:", error , false);
                messageColor = Color.YELLOW;
            }
            
            if (incompatibleMods.size() > 0) {
                String error = "";
                for (int i = 0; i < incompatibleMods.size() && i < 10; ++i)
                    error += "- " + sanitizeInputString(incompatibleMods.get(i) + "\n");
                if (incompatibleMods.size() > 10)
                    error += "- and " + (incompatibleMods.size() - 10) + " more...";
                
                eb.addField("Incompatible mods:", error , false);
                messageColor = Color.RED;
            }
            
            if (brokenMods.size() > 0) {
                String error = "";
                for (int i = 0; i < brokenMods.size() && i < 20; ++i)
                    error += "- " + sanitizeInputString(brokenMods.get(i) + "\n");
                if (brokenMods.size() > 20)
                    error += "- and " + (brokenMods.size() - 20) + " more...";
                
                eb.addField("Broken mods:", error , false);
                messageColor = Color.RED;
            }
            
            if (unknownMods.size() > 0) {
                String error = "";
                for (int i = 0; i < unknownMods.size() && i < 10; ++i) {
                    String s = unknownMods.get(i);
                    error += "- " + sanitizeInputString(s) + (modAuthors.containsKey(s) ? (" **by** " + sanitizeInputString(modAuthors.get(s)) + "\n") : "\n");
                }
                if (unknownMods.size() > 10)
                    error += "- and " + (unknownMods.size() - 10) + " more...";
                
                eb.addField("Unverified/Unknown mods:", error , false);
                messageColor = Color.RED;
            }
            
            //
            if (outdatedMods.size() > 0) {
                String vrcmuMessage = "VRChat".equals(game) ? "- Consider getting [VRCModUpdater](https://s.slaynash.fr/VRCMULatest) and moving it to the **Plugins** folder" : "";
                String error = "";
                boolean vrcmuAdded = false;
                for (int i = 0; i < outdatedMods.size() && i < 20; ++i) {
                    MelonOutdatedMod m = outdatedMods.get(i);
                    String namePart = m.downloadUrl == null ? m.name : ("[" + m.name + "](" + UrlShortener.GetShortenedUrl(m.downloadUrl) + ")");
                    error += "- " + namePart + ": `" + sanitizeInputString(m.currentVersion) + "` -> `" + m.latestVersion + "`\n";
                    if (i != outdatedMods.size() - 1 && error.length() + vrcmuMessage.length() + 20 > 1024)
                    {
                        error += "- and " + (outdatedMods.size() - i) + " more...\n";
                        error += vrcmuMessage;
                        vrcmuAdded = true;
                        break;
                    }
                }
                if (!vrcmuAdded && outdatedMods.size() > 3)
                    error += vrcmuMessage;
                
                eb.addField("Outdated mods:", error, false);
                messageColor = Color.YELLOW;
            }
            
            if (errors.size() > 0) {
                String error = "";
                for (int i = 0; i < errors.size(); ++i) {
                    error += "- " + sanitizeInputString(errors.get(i).error) + "\n";
                }
                eb.addField("Known errors:", error , false);
                messageColor = Color.RED;
            }
            
            if (modsThrowingErrors.size() > 0 && !isMLOutdated && !isMLOutdatedVRC) {
                String error = "";
                for (int i = 0; i < modsThrowingErrors.size() && i < 10; ++i)
                    error += "- " + sanitizeInputString(modsThrowingErrors.get(i)) + "\n";
                if (modsThrowingErrors.size() > 10)
                    error += "- and " + (modsThrowingErrors.size() - 10) + " more...";
                
                eb.addField("Mods With Errors:", error , false);
                messageColor = Color.RED;
            }
            
            if (!assemblyGenerationFailed && !isMLOutdated && !isMLOutdatedVRC) {
                String error = "";
                if (loadedMods.size() == 0 && missingMods.size() == 0 && preListingMods && !errors.contains(incompatibleAssemblyError))
                    error += "- You have a partial log. Either MelonLoader crashed or you entered select mode in MelonLoader console and need to push any key.\n";
                    
                if (loadedMods.size() == 0 && missingMods.size() == 0 && !preListingMods && !errors.contains(incompatibleAssemblyError) || (noPlugins && noMods))
                    error += " - You have no mods installed in your Mods and Plugins folder\n";
                
                if (hasNonModErrors)
                    error += " - There are some unidentified errors. Please wait for a moderator or a helper to manually check the file.\n";
                
                if(error.length()>0) {
                    eb.addField("Other Errors:", error , false);
                    messageColor = Color.RED;
                }
            }
            
            if (isMLOutdatedVRC || isMLOutdated)
                messageColor = melonPink;
            
            eb.setColor(messageColor);
            mb.setEmbed(eb.build());
            event.getChannel().sendMessage(mb.build()).queue();
        }
        else if (mlVersion != null) {
            if (hasErrors) {
                eb.addField("There are unidentified errors", "- please wait while someone checks them" , false);
                messageColor = Color.RED;
            }
            else {
                eb.addField("No issues found", "- If you had issues please say so and wait for someone to check the log" , false);
                messageColor = Color.LIGHT_GRAY;
            }
            
            eb.setColor(messageColor);
            mb.setEmbed(eb.build());
            event.getChannel().sendMessage(mb.build()).queue();
        }
        //else non-log messages
    }
    
    private static void reportUserModifiedML(MessageReceivedEvent event) {
        String reportChannel = CommandManager.mlReportChannels.get(event.getGuild().getIdLong()); // https://discord.com/channels/663449315876012052/663461849102286849/801676270974795787
        if (reportChannel != null) {
            event.getGuild().getTextChannelById(reportChannel).sendMessage(
                    JDAManager.wrapMessageInEmbed(
                            "User <@" + event.getMember().getId() + "> is using a modified MelonLoader.\nMessage: <https://discord.com/channels/" + event.getGuild().getId() + "/" + event.getChannel().getId() + "/" + event.getMessageId() + ">",
                            Color.RED)).queue();
        }
    }
    
    private static String sanitizeInputString(String input) {
        return input
                .replace("@", "@ ")
                .replace("*", "\\*")
                .replace("`", "\\`")
                .replace("nigger", "[CENSORED]")
                .replace("nigga", "[CENSORED]");
    }
    
    private static class MelonLoaderError {
        String regex;
        String error;
        
        public MelonLoaderError(String regex, String error) {
            this.regex = regex;
            this.error = error;
        }
    }
    
    public static class MelonOutdatedMod {
        String name;
        String currentVersion;
        String latestVersion;
        String downloadUrl;
        
        public MelonOutdatedMod(String name, String currentVersion, String latestVersion, String downloadUrl) {
            this.name = name;
            this.currentVersion = currentVersion;
            this.latestVersion = latestVersion;
            this.downloadUrl = downloadUrl;
        }
    }
}
