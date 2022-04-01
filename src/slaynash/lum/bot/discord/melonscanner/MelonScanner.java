package slaynash.lum.bot.discord.melonscanner;

import java.awt.Color;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.Localization;
import slaynash.lum.bot.UrlShortener;
import slaynash.lum.bot.discord.ChattyLum;
import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.discord.ServerMessagesHandler;
import slaynash.lum.bot.discord.utils.CrossServerUtils;
import slaynash.lum.bot.utils.ArrayUtils;
import slaynash.lum.bot.utils.ExceptionUtils;
import slaynash.lum.bot.utils.Utils;

public final class MelonScanner {
    public static final String LOG_IDENTIFIER = "MelonScanner";

    public static String latestMLVersionRelease = "0.4.1";
    public static String latestMLVersionAlpha = "0.3.0";

    private static final Color melonPink = new Color(255, 59, 106);

    private static final ScheduledExecutorService sheduledExecutor = Executors.newSingleThreadScheduledExecutor();

    public static void init() {
        MelonLoaderError.init();
        MelonScannerApisManager.startFetchingThread();

        sheduledExecutor.scheduleWithFixedDelay(LogCounter::updateCounter, 30, 10, TimeUnit.SECONDS);
    }

    public static void shutdown() {
        sheduledExecutor.shutdown();
    }


    public static void scanMessage(MessageReceivedEvent messageReceivedEvent) {

        try {
            if (messageReceivedEvent.getMessage().getContentDisplay().startsWith("."))
                return;

            String lang = "en";

            if (messageReceivedEvent.getChannel().getName().toLowerCase().contains("french"))
                lang = "fr";
            else if (messageReceivedEvent.getChannel().getName().toLowerCase().contains("german"))
                lang = "de";

            long guildID = messageReceivedEvent.getGuild().getIdLong();
            if ((guildID == 600298024425619456L/*emmVRC*/ || guildID == 439093693769711616L/*VRCMG*/ || guildID == 663449315876012052L/*MelonLoader*/) && messageReceivedEvent.getMessage().getContentRaw().isBlank()) {
                Random random = new Random();
                if (random.nextInt(1000) == 420)
                    lang = "sga";
            }

            String[] messageParts = messageReceivedEvent.getMessage().getContentRaw().split(" ");
            for (String messagePart : messageParts) {
                if (messagePart.startsWith("lang:"))
                    lang = messagePart.substring(5);
            }

            List<Attachment> attachments = messageReceivedEvent.getMessage().getAttachments();

            if (multipleLogsCheck(attachments, messageReceivedEvent, lang)) // This should not happen, but just in case
                return;

            Attachment attachment = attachments.stream().filter(attach -> isValidFileFormat(attach, true)).findFirst().orElse(null);
            if (attachment == null)
                return;

            MelonScanContext context = new MelonScanContext(attachment, messageReceivedEvent, lang);

            if ("message.txt".equals(attachment.getFileName()))
                context.consoleCopyPaste = true;

            if (oldEmmVRCLogCheck(context))
                return;

            if (!MelonScannerReadPass.doPass(context))
                return;

            vrcHashCheck(context);

            if (getModsFromApi(context)) {
                cleanupDuplicateMods(context);
                processFoundMods(context);
            }

            unofficialMLCheck(context);
            prepareEmbed(context);
            fileAgeCheck(context);
            fillEmbedDescription(context);


            boolean issueFound;
            if  (!context.pirate) {
                issueFound  = mlOutdatedCheck(context);
                issueFound |= vrchatVersionCheck(context);
                issueFound |= knownErrorsCheck(context);
                issueFound |= duplicatedModsCheck(context);
                issueFound |= missingModsCheck(context);
                issueFound |= incompatibleModsCheck(context);
                issueFound |= corruptedModsCheck(context);
                issueFound |= brokenModsCheck(context);
                issueFound |= oldModsCheck(context);
                issueFound |= misplacedModsCheck(context);
                issueFound |= misplacedPluginsCheck(context);
                issueFound |= modsHasPendingCheck(context);
                issueFound |= outdatedPluginCheck(context);
                issueFound |= outdatedModsCheck(context);
                issueFound |= newerModsCheck(context);
                issueFound |= unknownModsCheck(context);
                issueFound |= modsThrowingErrorsCheck(context);
                issueFound |= minorErrorsHandling(context);

                if (issueFound) {
                    if (context.isMLOutdatedVRC || context.isMLOutdated)
                        context.embedColor = melonPink;

                    if (!context.unidentifiedErrors)
                        context.addToChatty = true;
                }
                else if (context.mlVersion != null || context.modifiedML) {
                    if (context.hasErrors) {
                        context.embedBuilder.addField(Localization.get("melonscanner.unidentifiederrors.fieldname", lang), Localization.get("melonscanner.unidentifiederrors.field", lang), false);
                        context.embedColor = Color.RED;
                    }
                    else {
                        context.embedBuilder.addField(Localization.get("melonscanner.noissue.fieldname", lang), Localization.get("melonscanner.noissue.field", lang), false);
                        context.embedColor = Color.LIGHT_GRAY;
                        context.addToChatty = true;
                    }
                }
            }
            else {
                context.embedBuilder.addField(Localization.get("melonscanner.pirate.fieldname", lang), Localization.get("melonscanner.pirate.field", lang), false);
                context.embedColor = Color.RED;
            }

            if (context.embedBuilder.getFields().size() > 0) {
                LogCounter.addMLCounter(attachment);
                context.embedBuilder.setColor(context.embedColor);
                String description = context.embedBuilder.getDescriptionBuilder().toString();
                MessageBuilder messageBuilder = new MessageBuilder();
                messageBuilder.append(messageReceivedEvent.getAuthor().getAsMention());
                messageReceivedEvent.getChannel().sendMessage(messageBuilder.setEmbeds(context.embedBuilder.build()).build()).queue();
                if (context.addToChatty && !context.pirate && !(Objects.equals(context.game, "Phasmophobia") || Objects.equals(context.game, "Crab Game"))) {
                    ChattyLum.addNewHelpedRecently(messageReceivedEvent);
                }
                ServerMessagesHandler.handleReplies(messageReceivedEvent, description);
            }
        }
        catch (Exception exception) {
            ExceptionUtils.reportException(
                "Exception while reading attachment of message:",
                exception, messageReceivedEvent.getTextChannel());
        }
    }


    // Message sanity check

    private static boolean multipleLogsCheck(List<Attachment> attachments, MessageReceivedEvent messageReceivedEvent, String lang) {
        boolean hasAlreadyFound = false;
        for (Attachment attachment : attachments)
            if (isValidFileFormat(attachment, true)) {
                if (hasAlreadyFound) {
                    Utils.replyEmbed(Localization.get("melonscanner.onelogatatime", lang), Color.red, messageReceivedEvent);
                    return true;
                }
                hasAlreadyFound = true;
            }
        return false;
    }

    private static boolean oldEmmVRCLogCheck(MelonScanContext context) {
        if (context.attachment.getFileName().startsWith("emmVRC")) {
            Utils.replyEmbed(Localization.get("melonscanner.vrchat.oldemmvrc", context.lang), Color.orange, context.messageReceivedEvent);
            return true;
        }
        return false;
    }

    public static boolean isValidFileFormat(Attachment attachment, boolean strict) {
        if (attachment.getFileName() == null) return false;
        String fileName = attachment.getFileName().toLowerCase();
        if (strict && fileName.startsWith("message")) return true;
        return fileName.startsWith("latest") ||
            fileName.startsWith("melonloader") ||
            fileName.startsWith("mlinstall");
    }


    // Logs thinkering
    private static void vrcHashCheck(MelonScanContext context) {
        if ("VRChat".equals(context.game)) {
            context.isMLOutdatedVRC = true;
            if (context.mlHashCode != null) {
                System.out.println("game is vrc, checking hash");

                boolean hasVRChat1043ReadyML = false;
                for (MLHashPair mlHashes : context.alpha ? CommandManager.melonLoaderAlphaHashes : CommandManager.melonLoaderHashes) {
                    if (mlHashes.x64.equals(CommandManager.melonLoaderVRCHash))
                        hasVRChat1043ReadyML = true;

                    if (mlHashes.x64.equals(context.mlHashCode)) {
                        System.out.println("matching hash found");
                        if (hasVRChat1043ReadyML)
                            context.isMLOutdatedVRC = false;
                        break;
                    }
                }
            }
        }
    }

    private static boolean getModsFromApi(MelonScanContext context) {
        return (context.modDetails = MelonScannerApisManager.getMods(context.game)) != null;
    }

    private static void cleanupDuplicateMods(MelonScanContext context) {
        Map<String, LogsModDetails> loadedMods = new HashMap<>();
        List<MelonDuplicateMod> duplicatedMods = new ArrayList<>(context.duplicatedMods);

        for (LogsModDetails loadedMod : context.loadedMods.values()) {
            String modName = loadedMod.name;
            MelonApiMod apiMod = context.modDetails.stream().filter(m -> m.name.equals(loadedMod.name) || ArrayUtils.contains(m.aliases, loadedMod.name)).findFirst().orElse(null);
            /*
            if (!apiMod.name.equals(context.tmpModName) && ArrayUtils.contains(apiMod.aliases, context.tmpModName)) {
                context.duplicatedMods.add(context.tmpModName.trim());
                break;
            }
            */
            if (apiMod != null)
                modName = apiMod.name;

            LogsModDetails prevLoadedMod = loadedMods.get(modName);
            if (prevLoadedMod != null) {
                final String modNameFinal = modName;
                MelonDuplicateMod duplicateMod = duplicatedMods.stream().filter(dm -> dm.hasName(modNameFinal)).findFirst().orElse(null);
                if (duplicateMod != null)
                    duplicateMod.addName(loadedMod.name);
                else
                    duplicatedMods.add(new MelonDuplicateMod(prevLoadedMod.name, loadedMod.name));
            }
            else
                loadedMods.put(modName, loadedMod);
        }

        context.loadedMods = loadedMods;
        context.duplicatedMods = duplicatedMods;
    }

    private static void processFoundMods(MelonScanContext context) {
        for (Entry<String, LogsModDetails> entry : context.loadedMods.entrySet()) {
            final String modName = entry.getKey();
            final LogsModDetails logsModDetails = entry.getValue();
            final VersionUtils.VersionData modVersion = logsModDetails.version != null ? VersionUtils.getVersion(logsModDetails.version) : null;

            for (MelonLoaderError modSpecificError : MelonLoaderError.getModSpecificErrors()) {
                if (modSpecificError.regex.equals(modName)) {
                    context.errors.add(modSpecificError);
                    break;
                }
            }

            if (modVersion == null) {
                context.unknownMods.add(logsModDetails);
                continue;
            }

            String latestModName = null;
            VersionUtils.VersionData latestModVersion = null;
            String latestModHash = null;
            String latestModType = null;
            String latestModDownloadUrl = null;
            boolean latestHasPending = false;
            boolean deprecatedName = false;
            for (MelonApiMod modDetail : context.modDetails) {
                if (modDetail.name.replaceAll("[-_ ]", "").equals(modName.replaceAll("[-_ ]", "")) || (deprecatedName = ArrayUtils.contains(modDetail.aliases, modName))) {
                    System.out.println("Mod found in db: " + modDetail.name + " version " + modDetail.versions[0].version.getRaw());
                    latestModName = modDetail.name;
                    latestModVersion = modDetail.versions[0].version;
                    latestModDownloadUrl = modDetail.downloadLink;
                    latestModType = modDetail.modtype;
                    latestHasPending = modDetail.haspending;
                    latestModHash = modDetail.versions[0].hash;
                    if (latestModVersion != null && latestModHash != null && latestModVersion.getRaw().equals(logsModDetails.version) && !latestModHash.equals(logsModDetails.hash))
                        context.corruptedMods.add(modDetail);
                    break;
                }
            }

            int compare = VersionUtils.compareVersion(latestModVersion, modVersion);
            if (latestModVersion == null && latestModHash == null && latestModType == null) {
                context.unknownMods.add(logsModDetails);
            }
            else if(latestHasPending && compare == 0) {
                context.hasPendingMods.add(modName);
            }
            else if (CommandManager.brokenMods.contains(modName)) {
                context.brokenMods.add(modName);
            }
            else if (deprecatedName || compare > 0) {
                if (latestModType != null && latestModType.equalsIgnoreCase("plugin"))
                    context.outdatedPlugins.add(new MelonOutdatedMod(modName, latestModName, modVersion.getRaw(), latestModVersion.getRaw(), latestModDownloadUrl));
                else
                    context.outdatedMods.add(new MelonOutdatedMod(modName, latestModName, modVersion.getRaw(), latestModVersion.getRaw(), latestModDownloadUrl));
                context.modsThrowingErrors.remove(modName);
            }
            else if (!latestHasPending && compare < 0 && !latestModVersion.getRaw().isBlank()) {
                context.newerMods.add(new MelonOutdatedMod(modName, latestModName, modVersion.getRaw(), latestModVersion.getRaw(), latestModDownloadUrl));
            }
        }
    }

    private static void unofficialMLCheck(MelonScanContext context) {
        if (context.mlHashCode == null)
            return;

        if (CrossServerUtils.checkIfStaff(context.messageReceivedEvent))
            return;

        for (MLHashPair hashes : context.alpha ? CommandManager.melonLoaderAlphaHashes : CommandManager.melonLoaderHashes) {
            if (context.mlHashCode.equals(hashes.x64) || context.mlHashCode.equals(hashes.x86)) {
                System.out.println("hash found in known hashes: ");
                return;
            }
        }
        context.modifiedML = true;
        System.out.println("unknown hash");
        reportUserModifiedML(context.messageReceivedEvent);
    }

    private static void checkForPirate(MelonScanContext context) {
        if (context.gamePath == null && context.mlVersion != null && VersionUtils.compareVersion("0.5.0", context.mlVersion) <= 0) {
            context.editedLog = true; //trigger the `dont edit the log` message
        }
        else if (context.game == null || context.mlVersion != null && VersionUtils.compareVersion("0.5.0", context.mlVersion) > 0) {
            return;
        }
        else if (context.game.equalsIgnoreCase("BloonsTD6")) {
            if (!context.gamePath.contains("steamapps\\common\\BloonsTD6") && !context.gamePath.contains("Program Files\\WindowsApps")) {
                context.pirate = true;
            }
        }
        else if (context.game.equalsIgnoreCase("BONEWORKS")) {
            if (!context.gamePath.contains("steamapps\\common\\BONEWORKS\\BONEWORKS") && !context.gamePath.contains("Software\\stress-level-zero-inc-boneworks")) {
                context.pirate = true;
            }
        }
    }

    private static void prepareEmbed(MelonScanContext context) {
        checkForPirate(context);
        String footer = "Lum Log Scanner";
        if (context.loadedMods.size() > 0) {
            footer += " | " + context.loadedMods.size() + " mods loaded";
        }
        context.embedBuilder = new EmbedBuilder();
        context.reportMessage = new StringBuilder();
        context.embedBuilder.setTitle(Localization.get("melonscanner.logautocheckresult", context.lang))
                            .setTimestamp(Instant.now())
                            .setFooter(footer);

        if (context.game != null) {
            switch (context.game) {
                case "Albion Online Client":
                    context.embedBuilder.setThumbnail("https://assets.albiononline.com/assets/images/header/logo.png");
                    break;
                case "Among Us":
                    context.embedBuilder.setThumbnail("https://i.imgur.com/cGdWOch.png");
                    break;
                case "ArizonaSunshine":
                    context.embedBuilder.setThumbnail("https://arizona-sunshine.com/wp-content/uploads/2020/12/ArizonaSunshine_Logo_White.png");
                    break;
                case "Audica":
                    context.embedBuilder.setThumbnail("https://i.imgur.com/CHa4yW0.png");
                    break;
                case "BattleBit":
                    context.embedBuilder.setThumbnail("https://static.wikia.nocookie.net/battlebit_gamepedia_en/images/b/b3/Bbpng.png");
                    break;
                case "BloonsTD6":
                    if (context.pirate)
                        context.embedBuilder.setThumbnail("https://i.redd.it/76et0pfu87e31.png"); //sad monkey
                    else
                        context.embedBuilder.setThumbnail("https://i.imgur.com/BSXtkvW.png");
                    break;
                case "BONEWORKS":
                    if (context.pirate)
                        context.embedBuilder.setThumbnail("https://i.redd.it/o248hiyef2v61.png");
                    else
                        context.embedBuilder.setThumbnail("https://puu.sh/HAj1G/87f77fddf2.png");
                    break;
                case "Car Mechanic Simulator 2021":
                    context.embedBuilder.setThumbnail("https://cdn.akamai.steamstatic.com/steamcommunity/public/images/clans/29076066/86ec92aabfad0878253d3e47ef4133e3e3228471.png");
                    break;
                case "Cloudpunk ":
                    context.embedBuilder.setThumbnail("https://i0.wp.com/gameoctane.com/wp-content/uploads/2021/03/CPlogo-1.png");
                    break;
                case "Crab Game":
                    context.embedBuilder.setThumbnail("https://cdn.akamai.steamstatic.com/steam/apps/1782210/header.jpg");
                    break;
                case "Demeo":
                    context.embedBuilder.setThumbnail("https://cdn.discordapp.com/attachments/760342261967487068/863610041335676938/demeo.png");
                    break;
                case "DEVOUR":
                    context.embedBuilder.setThumbnail("https://cdn.shopify.com/s/files/1/0596/5506/1664/files/solid_logo_07bfc008-42ec-4f9c-b64a-7d4dbb9073f8.png");
                    break;
                case "Eco":
                    context.embedBuilder.setThumbnail("https://wiki.play.eco/logo.png");
                    break;
                case "Eleven":
                    context.embedBuilder.setThumbnail("https://habigames.ru/images/logo/054/logo_eleven-table-tennis-vr.png");
                    break;
                case "Eliatopia":
                    context.embedBuilder.setThumbnail("https://images.crazygames.com/eliatopia/20210722142022/eliatopia-cover");
                    break;
                case "Enter the Gungeon":
                    context.embedBuilder.setThumbnail("https://static.wikia.nocookie.net/vsbattles/images/1/12/Enter-the-gungeon-logo.png");
                    break;
                case "EscapeFromTarkov":
                    context.embedBuilder.setThumbnail("https://cdn.discordapp.com/attachments/760342261967487068/875097509918302279/EscapeFromTarkov-lg.png");
                    break;
                case "Eye of the Temple":
                    context.embedBuilder.setThumbnail("https://portforward.com/eye-of-the-temple/eye-of-the-temple-logo-large.webp");
                    break;
                case "FallGuys_client":
                    context.embedBuilder.setThumbnail("https://seeklogo.com/images/F/fall-guys-logo-0809CCA0D8-seeklogo.com.png");
                    break;
                case "Feign":
                    context.embedBuilder.setThumbnail("https://cdn.cloudflare.steamstatic.com/steam/apps/1436990/header.jpg");
                    break;
                case "Firewatch":
                    context.embedBuilder.setThumbnail("https://w7.pngwing.com/pngs/347/28/png-transparent-firewatch-video-game-gog-com-adventure-game-pc-game-slipknot-logo-game-text-orange.png");
                    break;
                case "Genshin Impact":
                    context.embedBuilder.setThumbnail("https://gmedia.playstation.com/is/image/SIEPDC/genshin-impact-logo-01-en-3sep21?$native--t$");
                    break;
                case "GodsOfGravity":
                    context.embedBuilder.setThumbnail("https://cdn.sidequestvr.com/file/140588/cover-art-landscape-calltoaction.png");
                    break;
                case "Ghost Hunters Corp":
                    context.embedBuilder.setThumbnail("https://cdn.cloudflare.steamstatic.com/steam/apps/1618540/header.jpg");
                    break;
                case "gunfirereborn":
                case "Gunfire Reborn":
                    context.embedBuilder.setThumbnail("https://cdn.discordapp.com/attachments/760342261967487068/868613876600680558/Gunfire_Reborn_Logo.png");
                    break;
                case "guigubahuang":
                    context.embedBuilder.setThumbnail("https://cdn.discordapp.com/attachments/760342261967487068/837379147617140786/guigubahuang.png");
                    break;
                case "Hired Ops":
                    context.embedBuilder.setThumbnail("https://www.hiredops.com/themes/hiredops/images/logo.png");
                    break;
                case "Hot Dogs Horseshoes and Hand Grenades":
                    context.embedBuilder.setThumbnail("https://cdn.discordapp.com/attachments/760342261967487069/871468287144390698/Hot_Dogs_Horseshoes_and_Hand_Grenades.png");
                    break;
                case "House Party":
                    context.embedBuilder.setThumbnail("https://pbs.twimg.com/profile_images/1186376117357305856/ZlgsB9ag_400x400.jpg");
                    break;
                case "IEYTD":
                    context.embedBuilder.setThumbnail("https://cdn.schellgames.com/images/Media-Kits/i-expect-you-to-die/IEYTD_Logo_Square.png");
                    break;
                case "Job Simulator":
                    context.embedBuilder.setThumbnail("https://i.imgur.com/0kmohjK.png");
                    break;
                case "Karlson":
                    context.embedBuilder.setThumbnail("https://i.redd.it/ldxbkjuziad51.png");
                    break;
                case "Kill the King":
                    context.embedBuilder.setThumbnail("https://kill-the-king.com/imgs/KTK_Logos/logo_KTK-hamburger.png");
                    break;
                case "maj":
                    context.embedBuilder.setThumbnail("https://mahjongsoul.yo-star.com/img/Mahjong_logo.d393a548.png");
                    break;
                case "Muse Dash":
                    context.embedBuilder.setThumbnail("https://cdn.discordapp.com/attachments/760342261967487068/863664777918545940/musedash.png");
                    break;
                case "My Friendly Neighborhood":
                    context.embedBuilder.setThumbnail("https://cdn.cloudflare.steamstatic.com/steam/apps/1574260/header.jpg");
                    break;
                case "Night in the Woods":
                    context.embedBuilder.setThumbnail("https://upload.wikimedia.org/wikipedia/commons/e/e5/Night_in_the_Woods_logo_black.svg");
                    break;
                case "Phasmophobia":
                    context.embedBuilder.setThumbnail("https://cdn.discordapp.com/attachments/760342261967487069/866926713761431562/Phasmophobia_Logo.png");
                    break;
                case "Pipe":
                    context.embedBuilder.setThumbnail("http://bmxstreets.com/wp-content/uploads/2017/12/pipe_black_1080-cropped-300x169.png");
                    break;
                case "Pistol Whip":
                    context.embedBuilder.setThumbnail("https://i.imgur.com/MeMcntj.png");
                    break;
                case "Rec Room":
                    context.embedBuilder.setThumbnail("https://images.squarespace-cdn.com/content/v1/582e7271bebafbd72792bd97/1571873579729-YGLX8EAT3EP2LIFJ7EZQ/RecRoomLogo_Shadow.png");
                    break;
                case "raot":
                    context.embedBuilder.setThumbnail("https://i.gjcdn.net/data/games/9/147/613147/media/game-description/game_logo_text-5uyytkca.png");
                    break;
                case "Reventure":
                    context.embedBuilder.setThumbnail("https://i2.wp.com/maroonersrock.com/wp-content/uploads/2019/08/logo.png");
                    break;
                case "Run": //what a crappy unity name, its bro falls
                    context.embedBuilder.setThumbnail("https://pbs.twimg.com/profile_images/1410686748494024704/5BD8YvYN_400x400.jpg");
                    context.embedBuilder.addField("Dude, this game sucks", "Don't promote knockoffs, uninstall it", false);
                    break;
                case "Sable":
                    context.embedBuilder.setThumbnail("https://upload.wikimedia.org/wikipedia/en/4/4c/Sable_cover_art.jpg");
                    break;
                case "SCPSL":
                    context.embedBuilder.setThumbnail("https://scpslgame.com/wp-content/uploads/revslider/slider-1/main_logo_large_glow.png");
                    break;
                case "Secret Neighbor":
                    context.embedBuilder.setThumbnail("https://image.api.playstation.com/vulcan/ap/rnd/202103/1711/dC3wH7bU7ghkcQeVszD18hFL.png");
                    break;
                case "Shredders":
                    context.embedBuilder.setThumbnail("https://www.windowscentral.com/sites/wpcentral.com/files/styles/large/public/field/image/2022/03/shredders-hero-image-01.jpg");
                    break;
                case "Stick Fight: The Game":
                    context.embedBuilder.setThumbnail("https://toppng.com//public/uploads/preview/color-stick-fight-the-game-115629098126na96uprb2.png");
                    break;
                case "Stumble Guys":
                    context.embedBuilder.setThumbnail("https://cdn.now.gg/apps-content/com.kitkagames.fallbuddies/logo/stumble-guys-multiplayer-royale.png");
                    context.embedBuilder.addField("Dude, this game sucks", "Don't promote knockoffs, uninstall it", false);
                    break;
                case "Superliminal":
                    context.embedBuilder.setThumbnail("https://cdn.discordapp.com/attachments/760342261967487068/875463746833772554/Superliminal.png");
                    break;
                case "Super Auto Pets":
                    context.embedBuilder.setThumbnail("https://cdn.cloudflare.steamstatic.com/steam/apps/1714040/capsule_616x353.jpg");
                    break;
                case "SynthRiders":
                    context.embedBuilder.setThumbnail("https://synthridersvr.com/wp-content/uploads/2020/08/Synth-Riders-logo.png");
                    break;
                case "TheForest":
                    context.embedBuilder.setThumbnail("https://www.logolynx.com/images/logolynx/f6/f6b42ce3f8dc299e74bd3c77bc0ec54a.png");
                    break;
                case "TheLongDark":
                    context.embedBuilder.setThumbnail("https://puu.sh/HAj1H/e2f9018e69.png");
                    break;
                case "The Sandbox Maker":
                    context.embedBuilder.setThumbnail("https://www.sandbox.game/img/13_Create/GameMaker/game-maker-iso.png");
                    break;
                case "The Golf Club 2019 ProTee": //has anticheats
                    context.embedBuilder.setThumbnail("https://www.thegolfclub2019.com/wp-content/uploads/2018/05/TheGolfClub2019_Overview_logo_update.png");
                    break;
                case "Townscaper":
                    context.embedBuilder.setThumbnail("https://cdn1.epicgames.com/salesEvent/salesEvent/EGS_Townscaper_OskarStlberg_S3_2560x1440-558540cb0b62cb5520f1f9e5b4c7a204");
                    break;
                case "ULTRAKILL":
                    context.embedBuilder.setThumbnail("https://cdn2.steamgriddb.com/file/sgdb-cdn/logo/a295a55e7c3b2af6f363711442900b96.png");
                    break;
                case "UNO":
                    context.embedBuilder.setThumbnail("https://cdn.discordapp.com/attachments/934636524954329183/934640238960189440/UMG.png");
                    break;
                case "VRChat":
                    context.embedBuilder.setThumbnail("https://puu.sh/HAiW4/bb2a98afdc.png");
                    break;
                case "VSeeFace":
                    context.embedBuilder.setThumbnail("https://www.vseeface.icu/assets/img/VSF.png");
                    break;
                case "Wasteland 3":
                    context.embedBuilder.setThumbnail("https://www.nicepng.com/png/full/300-3005551_wasteland-3-logo-png-image-grandmaster-flash-vs.png");
                    break;
                case "Wobbly Life":
                    context.embedBuilder.setThumbnail("https://cdn.discordapp.com/attachments/760342261967487069/887755823038754886/Wobbly_Life.png");
                    break;
                case "Zenith":
                    context.embedBuilder.setThumbnail("https://image.api.playstation.com/vulcan/img/rnd/202112/2106/3FzveWLc7O0W5x89U63y9WNj.png");
                    break;
                case "zombieland_vr_headshot_fever":
                    context.embedBuilder.setThumbnail("https://www.hgunified.com/wp-content/uploads/2021/03/ZOMBIELAND.png");
                    break;
                case "デュエプレ":
                    context.embedBuilder.setThumbnail("https://dmps.takaratomy.co.jp/common/img/logo.png");
                    break;
                default:
            }
        }
    }

    private static void fileAgeCheck(MelonScanContext context) {
        if (context.attachment.getFileName().matches("MelonLoader_[0-9]{2}-[0-9]{2}-[0-9]{2}_[0-9]{2}-[0-9]{2}-[0-9]{2}\\.[0-9]{3}.*\\.log")) {
            String fileDateString = context.attachment.getFileName().split("_", 3)[1];
            long ageDays = 0;
            try {
                LocalDate fileDate = LocalDate.parse("20" + fileDateString);
                LocalDate now = LocalDate.now();
                ageDays = ChronoUnit.DAYS.between(fileDate, now);
            }
            catch (Exception e) {
                ExceptionUtils.reportException("Exception while reading log age", "File age: " + fileDateString, e);
            }

            if (ageDays > 1) {
                if (context.pre3)
                    context.reportMessage.append(Localization.getFormat("melonscanner.reportmessage.logdayolds", context.lang, ageDays)).append("\n");
                else
                    context.reportMessage.append(Localization.getFormat("melonscanner.reportmessage.logdayoldsreupload", context.lang, ageDays)).append("\n");
            }
        }
    }

    private static void fillEmbedDescription(MelonScanContext context) {
        if (context.consoleCopyPaste)
            context.reportMessage.append("*").append(Localization.get("melonscanner.reportmessage.copy", context.lang)).append("*\n");

        if (context.game != null && context.mlVersion != null && !latestMLVersionAlpha.equals(latestMLVersionRelease) && context.mlVersion.equals(latestMLVersionAlpha))
            context.reportMessage.append("*").append(Localization.get("melonscanner.reportmessage.alpha", context.lang)).append("*\n");

        if (context.game != null && context.modDetails == null)
            context.reportMessage.append("*").append(Localization.getFormat("melonscanner.reportmessage.notsupported", context.lang, context.game)).append("*\n");

        context.embedBuilder.setDescription(context.reportMessage);

        if (context.remainingModCount != 0 || context.editedLog) {
            context.embedBuilder.addField(Localization.get("melonscanner.readerror.fieldname", context.lang), Localization.get("melonscanner.readerror.field", context.lang), false);
            context.embedColor = Color.RED;
        }
    }

    private static boolean mlOutdatedCheck(MelonScanContext context) {
        context.isMLOutdated = context.mlVersion != null && !(CrossServerUtils.sanitizeInputString(context.mlVersion).equals(latestMLVersionRelease) || CrossServerUtils.sanitizeInputString(context.mlVersion).equals(latestMLVersionAlpha) && VersionUtils.compareVersion(latestMLVersionAlpha, latestMLVersionRelease) == 1/* If Alpha is more recent */);

        if (context.isMLOutdated) {
            int result = VersionUtils.compareVersion(latestMLVersionRelease, context.mlVersion);
            switch (result) {
                case 1: //left more recent
                    context.embedBuilder.addField(Localization.get("melonscanner.mloutdated.fieldname", context.lang), Localization.getFormat("melonscanner.mloutdated.upbeta", context.lang, CrossServerUtils.sanitizeInputString(context.mlVersion), latestMLVersionRelease), false);
                    break;
                case 0: //identicals
                    if (context.alpha)
                        context.embedBuilder.addField(Localization.get("melonscanner.mloutdated.fieldname", context.lang), Localization.getFormat("melonscanner.mloutdated.upalpha", context.lang, latestMLVersionAlpha, CommandManager.melonLoaderVRCMinDate), false);
                    else
                        context.embedBuilder.addField(Localization.get("melonscanner.mloutdated.fieldname", context.lang), Localization.getFormat("melonscanner.mloutdated.reinstall", context.lang, latestMLVersionRelease), false);
                    break;
                case -1: //right more recent
                    context.embedBuilder.addField(Localization.get("melonscanner.mloutdated.fieldname", context.lang), Localization.getFormat("melonscanner.mloutdated.downgrade", context.lang, context.alpha ? latestMLVersionAlpha : latestMLVersionRelease), false);
                    break;
                default:
            }
            return true;
        }
        return false;
    }

    private static boolean vrchatVersionCheck(MelonScanContext context) {
        if (context.gameBuild != null && "VRChat".equals(context.game)) {
            String[] tempString = context.gameBuild.split("-", 3);
            context.gameBuild = tempString.length > 1 ? tempString[1] : "0"; //VRChat build number
            int compare = VersionUtils.compareVersion(context.gameBuild, CommandManager.vrchatBuild);
            if (compare == -1) {
                context.embedBuilder.addField("VRChat:", Localization.getFormat("melonscanner.vrcversioncheck.outdated", context.lang, CrossServerUtils.sanitizeInputString(context.gameBuild), CommandManager.vrchatBuild), false);
                context.embedColor = Color.ORANGE;
                return true;
            }
            else if (compare == 1) {
                context.embedBuilder.addField("VRChat:", Localization.getFormat("melonscanner.vrcversioncheck.overdated", context.lang, CrossServerUtils.sanitizeInputString(context.gameBuild), CommandManager.vrchatBuild), false);
                context.embedColor = Color.ORANGE;
                return true;
            }
        }
        //for pre0.4.0 VRChat version checking with emmVRC
        else if (context.emmVRCVRChatBuild != null) {
            int compare = VersionUtils.compareVersion(context.emmVRCVRChatBuild, CommandManager.vrchatBuild);
            if (compare == -1) {
                context.embedBuilder.addField("VRChat:", Localization.getFormat("melonscanner.vrcversioncheck.outdated", context.lang, CrossServerUtils.sanitizeInputString(context.emmVRCVRChatBuild), CommandManager.vrchatBuild), false);
                context.embedColor = Color.ORANGE;
                return true;
            }
            else if (compare == 1) {
                context.embedBuilder.addField("VRChat:", Localization.getFormat("melonscanner.vrcversioncheck.overdated", context.lang, CrossServerUtils.sanitizeInputString(context.emmVRCVRChatBuild), CommandManager.vrchatBuild), false);
                context.embedColor = Color.ORANGE;
                return true;
            }
        }
        return false;
    }

    private static boolean knownErrorsCheck(MelonScanContext context) {
        if (context.errors.size() > 0) {
            StringBuilder error = new StringBuilder();
            for (int i = 0; i < context.errors.size(); ++i) {
                String errorString = context.errors.get(i).error(context.lang);
                if (errorString.contains("$cleanunity$")) {
                    errorString = errorString.replace("$cleanunity$", "");
                    context.embedBuilder.setImage("https://cdn.discordapp.com/attachments/600661924010786816/956088486220431400/unknown.png");
                }
                error.append("- ").append(errorString).append("\n");
            }
            context.embedBuilder.addField(Localization.get("melonscanner.knownerrors.fieldname", context.lang), error.substring(0, Math.min(error.toString().length(), MessageEmbed.VALUE_MAX_LENGTH)), false);
            context.embedColor = Color.RED;
            return true;
        }
        return false;
    }

    private static boolean duplicatedModsCheck(MelonScanContext context) {
        if (context.duplicatedMods.size() > 0) {
            StringBuilder error = new StringBuilder();
            for (int i = 0; i < context.duplicatedMods.size() && i < 10; ++i)
                error.append("- ").append(CrossServerUtils.sanitizeInputString(context.duplicatedMods.get(i) + "\n"));
            if (context.duplicatedMods.size() > 10)
                error.append(Localization.getFormat("melonscanner.duplicatemods.more", context.lang, context.duplicatedMods.size() - 10));

            error.append(Localization.get("melonscanner.duplicatemods.warning", context.lang));
            context.embedBuilder.addField(Localization.get("melonscanner.duplicatemods.fieldname", context.lang), error.substring(0, Math.min(error.toString().length(), MessageEmbed.VALUE_MAX_LENGTH)), false);
            context.embedColor = Color.RED;
            return true;
        }
        return false;
    }

    private static boolean missingModsCheck(MelonScanContext context) {
        if (context.missingMods.size() > 0) {
            context.missingMods.sort(String.CASE_INSENSITIVE_ORDER);
            StringBuilder error = new StringBuilder();
            for (int i = 0; i < context.missingMods.size() && i < 10; ++i) {
                String missingModName = context.missingMods.get(i);
                String missingModDownloadLink = MelonScannerApisManager.getDownloadLinkForMod(context.game, missingModName);
                if (missingModDownloadLink != null)
                    error.append("- [").append(CrossServerUtils.sanitizeInputString(missingModName)).append("](").append(missingModDownloadLink).append(")\n");
                else
                    error.append("- ").append(CrossServerUtils.sanitizeInputString(missingModName)).append("\n");
            }
            if (context.missingMods.size() > 10)
                error.append(Localization.getFormat("melonscanner.missingmods.more", context.lang, context.missingMods.size() - 10));

            context.embedBuilder.addField(Localization.get("melonscanner.missingmods.fieldname", context.lang), error.substring(0, Math.min(error.toString().length(), MessageEmbed.VALUE_MAX_LENGTH)), false);
            context.embedColor = Color.ORANGE;
            return true;
        }
        return false;
    }

    private static boolean incompatibleModsCheck(MelonScanContext context) {
        if (context.incompatibleMods.size() > 0) {
            StringBuilder error = new StringBuilder();
            for (int i = 0; i < context.incompatibleMods.size() && i < 10; ++i) {
                MelonIncompatibleMod incompatibleMod = context.incompatibleMods.get(i);
                error.append("- ").append(incompatibleMod.mod).append(" is incompatible with ").append(incompatibleMod.incompatible).append("\n");
            }
            if (context.incompatibleMods.size() > 10)
                error.append(Localization.getFormat("melonscanner.incompatibleMods.more", context.lang, context.incompatibleMods.size() - 10));

            context.embedBuilder.addField(Localization.get("melonscanner.incompatibleMods.fieldname", context.lang), error.substring(0, Math.min(error.toString().length(), MessageEmbed.VALUE_MAX_LENGTH)), false);
            context.embedColor = Color.ORANGE;
            return true;
        }
        return false;
    }

    private static boolean corruptedModsCheck(MelonScanContext context) {
        context.corruptedMods.removeIf(m -> context.brokenMods.contains(m.name));
        if (context.corruptedMods.size() > 0) {
            StringBuilder error = new StringBuilder(Localization.get("melonscanner.corruptedmods.warning", context.lang) + "\n");
            for (int i = 0; i < context.corruptedMods.size() && i < 10; ++i)
                if (context.corruptedMods.get(i).downloadLink != null)
                    error.append("- [").append(CrossServerUtils.sanitizeInputString(context.corruptedMods.get(i).name)).append("](").append(context.corruptedMods.get(i).downloadLink).append(")\n");
                else
                    error.append("- ").append(CrossServerUtils.sanitizeInputString(context.corruptedMods.get(i).name + "\n"));
            if (context.corruptedMods.size() > 10)
                error.append(Localization.getFormat("melonscanner.corruptedmods.more", context.lang, context.corruptedMods.size() - 10));

            context.embedBuilder.addField(Localization.get("melonscanner.corruptedmods.fieldname", context.lang), error.substring(0, Math.min(error.toString().length(), MessageEmbed.VALUE_MAX_LENGTH)), false);
            context.embedColor = Color.RED;
            return true;
        }
        return false;
    }

    private static boolean brokenModsCheck(MelonScanContext context) {
        context.brokenMods.removeAll(MelonLoaderError.getModSpecificErrors().stream().map(m -> m.regex).toList());
        if (context.brokenMods.size() > 0) {
            context.brokenMods.sort(String.CASE_INSENSITIVE_ORDER);
            StringBuilder error = new StringBuilder();
            for (int i = 0; i < context.brokenMods.size() && i < 20; ++i)
                error.append("- ").append(CrossServerUtils.sanitizeInputString(context.brokenMods.get(i) + "\n"));
            if (context.brokenMods.size() > 20)
                error.append(Localization.getFormat("melonscanner.brokenmods.more", context.lang, context.brokenMods.size() - 20));

            context.embedBuilder.addField(Localization.get("melonscanner.brokenmods.fieldname", context.lang), error.substring(0, Math.min(error.toString().length(), MessageEmbed.VALUE_MAX_LENGTH)), false);
            context.embedColor = Color.RED;
            return true;
        }
        return false;
    }

    private static boolean oldModsCheck(MelonScanContext context) {
        if (context.oldMods.size() > 0 && !(context.isMLOutdatedVRC || context.isMLOutdated) || context.modifiedML) {
            context.oldMods.sort(String.CASE_INSENSITIVE_ORDER);
            StringBuilder error = new StringBuilder();
            boolean added = false;
            for (int i = 0; i < context.oldMods.size() && i < 20; ++i) {
                boolean found = false;
                String modName = context.oldMods.get(i);
                if (context.modDetails != null) { //null check for games without an API
                    for (MelonApiMod modDetail : context.modDetails) {
                        if (modDetail.name.equals(modName)) { //I not sure if all all APIs contain the recent name in aliases like for TLD so I just do a check
                            if (modDetail.modtype != null && modDetail.modtype.equalsIgnoreCase("plugin"))
                                context.outdatedPlugins.add(new MelonOutdatedMod(modDetail.name, modName, "?", modDetail.versions[0].version.getRaw(), modDetail.downloadLink));
                            else
                                context.outdatedMods.add(new MelonOutdatedMod(modDetail.name, modName, "?", modDetail.versions[0].version.getRaw(), modDetail.downloadLink));
                            found = true;
                            break;
                        }
                        if (modDetail.aliases != null) { //null check for mods without an aliases
                            for (String alias : modDetail.aliases) {
                                if (alias.equals(modName)) {
                                    if (modDetail.modtype != null && modDetail.modtype.equalsIgnoreCase("plugin"))
                                        context.outdatedPlugins.add(new MelonOutdatedMod(modDetail.name, modName, "?", modDetail.versions[0].version.getRaw(), modDetail.downloadLink));
                                    else
                                        context.outdatedMods.add(new MelonOutdatedMod(modDetail.name, modName, "?", modDetail.versions[0].version.getRaw(), modDetail.downloadLink));
                                    found = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (!found) {
                    error.append("- ").append(CrossServerUtils.sanitizeInputString(context.oldMods.get(i) + "\n"));
                    added = true;
                }
            }
            if (context.oldMods.size() > 20)
                error.append(Localization.getFormat("melonscanner.oldmods.more", context.lang, context.oldMods.size() - 20));
            if (added) {
                context.embedBuilder.addField(Localization.get("melonscanner.oldmods.fieldname", context.lang), error.substring(0, Math.min(error.toString().length(), MessageEmbed.VALUE_MAX_LENGTH)), false);
                context.embedColor = Color.RED;
            }
            return true;
        }
        return false;
    }

    private static boolean unknownModsCheck(MelonScanContext context) {
        if (context.unknownMods.size() > 0) {
            StringBuilder error = new StringBuilder();
            for (int i = 0; i < context.unknownMods.size() && i < 10; ++i) {
                LogsModDetails md = context.unknownMods.get(i);
                String unknowModOut = CrossServerUtils.sanitizeInputString(md.name);
                if (md.version != null && !md.version.isBlank())
                    unknowModOut += " " + CrossServerUtils.sanitizeInputString(md.version);
                if (md.author != null)
                    unknowModOut = Localization.getFormat("melonscanner.unknownmods.modnamewithauthor", context.lang, unknowModOut, CrossServerUtils.sanitizeInputString(md.author));
                error.append("- ").append(unknowModOut).append("\n");
            }
            if (context.unknownMods.size() > 10)
                error.append(Localization.getFormat("melonscanner.unknownmods.more", context.lang, context.unknownMods.size() - 10));

            context.embedBuilder.addField(Localization.get("melonscanner.unknownmods.fieldname", context.lang), error.substring(0, Math.min(error.toString().length(), MessageEmbed.VALUE_MAX_LENGTH)), false);
            if (context.embedColor.equals(Color.BLUE))
                context.embedColor = Color.RED;
            return true;
        }
        return false;
    }

    private static boolean misplacedModsCheck(MelonScanContext context) {
        if (context.misplacedMods.size() > 0) {
            context.misplacedMods.sort(String.CASE_INSENSITIVE_ORDER);
            StringBuilder error = new StringBuilder(Localization.get("melonscanner.misplacedmods.warning", context.lang) + "\n");
            for (int i = 0; i < context.misplacedMods.size() && i < 10; ++i) {
                String mm = context.misplacedMods.get(i);
                error.append("- ").append(CrossServerUtils.sanitizeInputString(mm)).append("\n");
            }
            if (context.misplacedMods.size() > 10)
                error.append(Localization.getFormat("melonscanner.misplacedmods.more", context.lang, context.misplacedMods.size() - 10));

            context.embedBuilder.addField(Localization.get("melonscanner.misplacedmods.fieldname", context.lang), error.substring(0, Math.min(error.toString().length(), MessageEmbed.VALUE_MAX_LENGTH)), false);
            if (context.embedColor.equals(Color.BLUE))
                context.embedColor = Color.RED;
            return true;
        }
        return false;
    }

    private static boolean misplacedPluginsCheck(MelonScanContext context) {
        if (context.misplacedPlugins.size() > 0) {
            context.misplacedPlugins.sort(String.CASE_INSENSITIVE_ORDER);
            StringBuilder error = new StringBuilder(Localization.get("melonscanner.misplacedplugins.warning", context.lang) + "\n");
            for (int i = 0; i < context.misplacedPlugins.size() && i < 10; ++i) {
                String mp = context.misplacedPlugins.get(i);
                error.append("- ").append(CrossServerUtils.sanitizeInputString(mp)).append("\n");
            }
            if (context.misplacedPlugins.size() > 10)
                error.append(Localization.getFormat("melonscanner.misplacedplugins.more", context.lang, context.misplacedPlugins.size() - 10));

            context.embedBuilder.addField(Localization.get("melonscanner.misplacedplugins.fieldname", context.lang), error.substring(0, Math.min(error.toString().length(), MessageEmbed.VALUE_MAX_LENGTH)), false);
            if (context.embedColor.equals(Color.BLUE))
                context.embedColor = Color.RED;
            return true;
        }
        return false;
    }

    private static boolean outdatedModsCheck(MelonScanContext context) {
        if (context.outdatedMods.size() > 0) {
            String muMessage;
            switch (context.game) {
                case "VRChat":
                    if (context.misplacedPlugins.contains("VRCModUpdater.Loader"))
                        muMessage = "";
                    else if (context.loadedMods.containsKey("VRCModUpdater.Loader"))
                        muMessage = "";
                    else if (context.loadedMods.containsKey("UpdateChecker"))
                        muMessage = "";
                    else
                        muMessage = Localization.get("melonscanner.outdatedmods.vrcmuwarning", context.lang);
                    break;
                case "TheLongDark":
                    if (context.misplacedPlugins.contains("AutoUpdatingPlugin"))
                        muMessage = "";
                    else
                        muMessage = Localization.get("melonscanner.outdatedmods.tldmuwarning", context.lang);
                    break;
                default:
                    muMessage = "";
                    break;
            }

            StringBuilder error = new StringBuilder();
            String nextModLine = computeOutdatedModLine(context.outdatedMods.get(0));
            for (int i = 0; i < context.outdatedMods.size() && i < 20; ++i) {
                error.append(nextModLine);

                if (i + 1 < context.outdatedMods.size())
                    nextModLine = computeOutdatedModLine(context.outdatedMods.get(i + 1));
                else
                    break; // no next outdated Mod

                if (error.length() + nextModLine.length() + muMessage.length() + 18 > MessageEmbed.VALUE_MAX_LENGTH) {
                    error.append(Localization.getFormat("melonscanner.outdatedmods.more", context.lang, context.outdatedMods.size() - i)).append("\n"); //length is about 17 char
                    break;
                }
            }
            if (context.outdatedMods.size() >= 3)
                error.insert(0, muMessage + "\n");

            context.embedBuilder.addField(Localization.get("melonscanner.outdatedmods.fieldname", context.lang), error.substring(0, Math.min(error.toString().length(), MessageEmbed.VALUE_MAX_LENGTH)), false);
            context.embedColor = Color.ORANGE;
            return true;
        }
        return false;
    }

    private static boolean outdatedPluginCheck(MelonScanContext context) {
        if (context.outdatedPlugins.size() > 0) {

            StringBuilder error = new StringBuilder();
            String nextModLine = computeOutdatedModLine(context.outdatedPlugins.get(0));
            for (int i = 0; i < context.outdatedPlugins.size() && i < 20; ++i) {
                error.append(nextModLine);

                if (i + 1 < context.outdatedPlugins.size())
                    nextModLine = computeOutdatedModLine(context.outdatedPlugins.get(i + 1));
                else
                    break; // no next outdated Mod

                if (error.length() + nextModLine.length() + 18 > MessageEmbed.VALUE_MAX_LENGTH) {
                    error.append(Localization.getFormat("melonscanner.outdatedmods.more", context.lang, context.outdatedPlugins.size() - i)).append("\n"); //length is about 17 char
                    break;
                }
            }

            context.embedBuilder.addField(Localization.get("melonscanner.outdatedplugins.fieldname", context.lang), error.substring(0, Math.min(error.toString().length(), MessageEmbed.VALUE_MAX_LENGTH)), false);
            context.embedColor = Color.ORANGE;
            return true;
        }
        return false;
    }

    private static boolean newerModsCheck(MelonScanContext context) {
        if (context.newerMods.size() > 0) {

            StringBuilder error = new StringBuilder();
            String nextModLine = computeOutdatedModLine(context.newerMods.get(0));
            for (int i = 0; i < context.newerMods.size() && i < 10; ++i) {
                error.append(nextModLine);

                if (i + 1 < context.newerMods.size())
                    nextModLine = computeOutdatedModLine(context.newerMods.get(i + 1));
                else
                    break; // no next outdated Mod

                if (error.length() + nextModLine.length() + 18 > MessageEmbed.VALUE_MAX_LENGTH) {
                    error.append(Localization.getFormat("melonscanner.outdatedmods.more", context.lang, context.newerMods.size() - i)).append("\n"); //length is about 17 char
                    break;
                }
            }

            context.embedBuilder.addField(Localization.get("melonscanner.newermods.fieldname", context.lang), error.substring(0, Math.min(error.toString().length(), MessageEmbed.VALUE_MAX_LENGTH)), false);
            context.embedColor = Color.ORANGE;
            return true;
        }
        return false;
    }

    private static String computeOutdatedModLine(MelonOutdatedMod outdatedMod) {
        String namePart = outdatedMod.downloadUrl == null ? outdatedMod.name : ("[" + outdatedMod.name + "](" + UrlShortener.getShortenedUrl(outdatedMod.downloadUrl) + ")");
        String line = "- " + namePart + ": `" + CrossServerUtils.sanitizeInputString(outdatedMod.currentVersion) + "` -> `" + outdatedMod.latestVersion + "`";
        if (!outdatedMod.name.equals(outdatedMod.newName))
            line += " (" + outdatedMod.newName + ")";
        line += "\n";
        return line;
    }

    private static boolean modsHasPendingCheck(MelonScanContext context) {
        if (context.hasPendingMods.size() > 0) {
            context.hasPendingMods.sort(String.CASE_INSENSITIVE_ORDER);
            StringBuilder error = new StringBuilder("The following mods has an update waiting for review. Please wait until the review process is finished.\n");
            for (int i = 0; i < context.hasPendingMods.size() && i < 20; ++i)
                error.append("- ").append(CrossServerUtils.sanitizeInputString(context.hasPendingMods.get(i))).append("\n");
            if (context.hasPendingMods.size() > 20)
                error.append(Localization.getFormat("melonscanner.modsthrowingerrors.more", context.lang, context.hasPendingMods.size() - 20));

            context.embedColor = Color.ORANGE;
            context.embedBuilder.addField(Localization.get("melonscanner.haspendingmods.fieldname", context.lang), error.substring(0, Math.min(error.toString().length(), MessageEmbed.VALUE_MAX_LENGTH)), false);
            return true;
        }
        return false;
    }

    private static boolean modsThrowingErrorsCheck(MelonScanContext context) {
        context.modsThrowingErrors.removeAll(context.brokenMods);
        if (context.modsThrowingErrors.size() > 0 && !context.isMLOutdated && !context.isMLOutdatedVRC) {
            context.modsThrowingErrors.sort(String.CASE_INSENSITIVE_ORDER);
            StringBuilder error = new StringBuilder();
            for (int i = 0; i < context.modsThrowingErrors.size() && i < 10; ++i)
                error.append("- ").append(CrossServerUtils.sanitizeInputString(context.modsThrowingErrors.get(i))).append("\n");
            if (context.modsThrowingErrors.size() > 10)
                error.append(Localization.getFormat("melonscanner.modsthrowingerrors.more", context.lang, context.modsThrowingErrors.size() - 10));

            context.embedBuilder.addField(Localization.get("melonscanner.modsthrowingerrors.fieldname", context.lang), error.substring(0, Math.min(error.toString().length(), MessageEmbed.VALUE_MAX_LENGTH)), false);
            context.embedColor = Color.RED;
            return true;
        }
        return false;
    }

    private static boolean minorErrorsHandling(MelonScanContext context) {
        if (!context.assemblyGenerationFailed && !context.isMLOutdated && !context.isMLOutdatedVRC && context.duplicatedMods.size() == 0 && context.outdatedMods.size() == 0) {
            String error = "";
            if (context.noMods && context.missingMods.size() == 0 && context.preListingMods && !context.errors.contains(MelonLoaderError.incompatibleAssemblyError))
                error += Localization.get("melonscanner.othererrors.partiallog", context.lang) + "\n";

            if (context.noMods && context.misplacedMods.size() == 0 && !context.preListingMods && context.errors.size() == 0) {
                long guildID = context.messageReceivedEvent.getGuild().getIdLong();
                if (guildID == 600298024425619456L)
                    error += Localization.get("melonscanner.othererrors.nomodsemmvrc", context.lang) + "\n";
                else if (guildID == 439093693769711616L)
                    error += Localization.get("melonscanner.othererrors.nomodsvrcmg", context.lang) + "\n";
                else if (guildID == 322211727192358914L || guildID == 835185040752246835L) {
                    error += Localization.get("melonscanner.othererrors.nomodstld", context.lang) + "\n";
                    context.embedBuilder.setThumbnail("https://pbs.twimg.com/media/EU5rcX4WsAMcc-y?format=jpg");
                }
                else
                    error += Localization.get("melonscanner.othererrors.nomods", context.lang) + "\n";
            }
            if (context.loadedMods.containsKey("BTKCompanionLoader") && context.loadedMods.containsKey("BTKSANameplateMod")) {
                error += "Remove BTKSANameplateMod as it is included with BTKCompanionLoader\n";
            }
            if (context.hasNonModErrors && context.errors.size() == 0) {
                error += Localization.get("melonscanner.othererrors.unidentifiederrors", context.lang) + "\n";
                context.unidentifiedErrors = true;
            }
            if (context.mlVersion != null && VersionUtils.compareVersion(latestMLVersionRelease, context.mlVersion) == 0 && context.missingMods.contains("XUnity.AutoTranslator.Plugin.Core")) {
                error += "Make sure that you installed all of XUnity.AutoTranslator including the UserLibs folder\n";
            }
            if (context.line.contains("Applied USER32.dll::SetTimer patch")) {
                error += "MelonLoader most likely crashed because of Start Screen. Try adding the launch option `--melonloader.disablestartscreen` and see if that helps.";
            }
            if (context.line.contains("Downloading")) {
                error += "MelonLoader Gotten stuck downloading, make sure that nothing is blocking downloads.";
            }
            if (context.line.contains("Contacting RemoteAPI...")) {
                error += "Unity failed to initialize graphics. Please make sure that your GPU drivers are up to date.";
            }
            if (context.mlVersion != null && context.messageReceivedEvent.getGuild().getIdLong() == 819950183784644618L /* ReMod */ && !context.loadedMods.containsKey("ReMod") && !(context.preListingMods || context.listingMods)) {
                context.embedBuilder.addField("You don't have ReMod", "ReMod is missing from your Mods folder. Please download it from <#841105987004006401> and put it into your Mods folder.", false);
                if (context.embedColor == Color.BLUE)
                    context.embedColor = Color.ORANGE;
            }

            if (error.length() > 0) {
                context.embedBuilder.addField(Localization.get("melonscanner.othererrors.fieldname", context.lang), error, false);
                context.embedColor = Color.RED;
            }
            else if (context.mlVersion != null && (context.loadedMods.size() == 0 || context.preListingMods) && context.errors.size() == 0) {
                context.embedBuilder.addField(Localization.get("melonscanner.partiallog.fieldname", context.lang), Localization.get("melonscanner.partiallog.field", context.lang), false);
                context.embedColor = Color.ORANGE;
            }
            else
                return false;

            return true;
        }
        return false;
    }

    // Utils
    private static void reportUserModifiedML(MessageReceivedEvent event) {
        String reportChannel = CommandManager.mlReportChannels.get(event.getGuild().getIdLong()); // https://discord.com/channels/663449315876012052/663461849102286849/801676270974795787
        if (reportChannel != null) {
            event.getGuild().getTextChannelById(reportChannel).sendMessageEmbeds(
                Utils.wrapMessageInEmbed(
                        "User " + event.getMember().getAsMention() + " is using an unofficial MelonLoader.\nMessage: <https://discord.com/channels/" + event.getGuild().getId() + "/" + event.getChannel().getId() + "/" + event.getMessageId() + ">",
                            Color.orange)).queue();
        }
    }

    public static void translateLog(MessageReceivedEvent event) {
        try {
            System.out.println("Translating Log Results Checks in " + event.getGuild().getName());
            String message = event.getMessage().getContentStripped().toLowerCase();
            Message referenced = event.getMessage().getReferencedMessage();
            if (referenced == null || referenced.isEdited() || !message.startsWith("tr:"))
                return;
            MessageEmbed embed = referenced.getEmbeds().get(0);
            if (embed == null || embed.getFooter() == null || !"Lum Log Scanner".equals(embed.getFooter().getText()))
                return;
            String lan = message.substring(3).trim();
            System.out.println("Passed validations, translating to " + lan);
            EmbedBuilder editEmbed = new EmbedBuilder(embed);
            editEmbed.setTitle(Utils.translate("en", lan, embed.getTitle(), MessageEmbed.TITLE_MAX_LENGTH));
            editEmbed.setDescription(Utils.translate("en", lan, embed.getDescription(), MessageEmbed.DESCRIPTION_MAX_LENGTH));
            editEmbed.clearFields();
            for (Field field : embed.getFields()) {
                String name = Utils.translate("en", lan, field.getName(), MessageEmbed.TITLE_MAX_LENGTH);
                String value = Utils.translate("en", lan, field.getValue(), MessageEmbed.VALUE_MAX_LENGTH);
                editEmbed.addField(name, value, field.isInline());
            }
            System.out.println("Done Translating Log Results");
            referenced.editMessageEmbeds(editEmbed.build()).queue();
        } catch (Exception e) {
            ExceptionUtils.reportException("Exception while translating log:", e, event.getTextChannel());
        }
    }
}
