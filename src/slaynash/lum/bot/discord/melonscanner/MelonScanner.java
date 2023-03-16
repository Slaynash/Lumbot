package slaynash.lum.bot.discord.melonscanner;

import java.awt.Color;
import java.sql.ResultSet;
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
import java.util.function.Consumer;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import slaynash.lum.bot.DBConnectionManagerLum;
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
        List<Attachment> attachments = messageReceivedEvent.getMessage().getAttachments();

        Attachment attachment = attachments.stream().filter(attach -> isValidFileFormat(attach, true)).findFirst().orElse(null);
            if (attachment == null)
                return;

        MessageCreateData message = scanMessage(messageReceivedEvent, attachment);
        if (message != null)
            messageReceivedEvent.getChannel().sendMessage(message).queue();
    }

    public static MessageCreateData scanMessage(MessageReceivedEvent messageReceivedEvent, Attachment attachment) {

        MessageCreateData messageCreateData = null;

        try {
            if (messageReceivedEvent.getMessage().getContentDisplay().startsWith("."))
                return messageCreateData;

            String lang = "en";

            if (messageReceivedEvent.getChannel().getName().toLowerCase().contains("french"))
                lang = "fr";
            else if (messageReceivedEvent.getChannel().getName().toLowerCase().contains("german"))
                lang = "de";

            long guildID = messageReceivedEvent.getGuild().getIdLong();
            if ((guildID == 1001388809184870441L/*CVRMG*/ || guildID == 663449315876012052L/*MelonLoader*/) && messageReceivedEvent.getMessage().getContentRaw().isBlank()) {
                Random random = new Random();
                if (random.nextInt(1000) == 420)
                    lang = "sga";
                if (random.nextInt(420) == 69)
                    lang = "owo";
            }

            String[] messageParts = messageReceivedEvent.getMessage().getContentRaw().split(" ");
            for (String messagePart : messageParts) {
                if (messagePart.startsWith("lang:"))
                    lang = messagePart.substring(5).toLowerCase();
            }

            if (multipleLogsCheck(messageReceivedEvent.getMessage().getAttachments(), messageReceivedEvent, lang)) // don't let people spam logs
                return messageCreateData;

            LogCounter.addMLCounter(attachment);

            MelonScanContext context = new MelonScanContext(attachment, messageReceivedEvent, lang);

            if ("message.txt".equals(attachment.getFileName()))
                context.consoleCopyPaste = true;

            if (!MelonScannerReadPass.doPass(context))
                return messageCreateData;

            postReadApiPass(context);

            if (getModsFromApi(context)) {
                cleanupDuplicateMods(context);
                processFoundMods(context);
            }

            unofficialMLCheck(context);
            prepareEmbed(context);
            fileAgeCheck(context);
            fillEmbedDescription(context);
            badModCheck(context);

            boolean issueFound;
            if  (!context.pirate) {
                issueFound  = mlOutdatedCheck(context);
                issueFound |= missingEpicCompat(context);
                issueFound |= knownErrorsCheck(context);
                issueFound |= duplicatedModsCheck(context);
                issueFound |= missingModsCheck(context);
                issueFound |= incompatibleModsCheck(context);
                //issueFound |= corruptedModsCheck(context); // Disabled for now
                issueFound |= brokenModsCheck(context);
                issueFound |= retiredModsCheck(context);
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
                    if (context.isMLOutdated)
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
                context.embedBuilder.setColor(context.embedColor);
                String description = context.embedBuilder.getDescriptionBuilder().toString();
                MessageCreateBuilder messageBuilder = new MessageCreateBuilder();
                messageBuilder.setContent(messageReceivedEvent.getAuthor().getAsMention());
                messageCreateData = messageBuilder.setEmbeds(context.embedBuilder.build()).build();
                if (context.addToChatty && !context.pirate && !(Objects.equals(context.game, "Phasmophobia") || Objects.equals(context.game, "Crab Game"))) {
                    ChattyLum.addNewHelpedRecently(messageReceivedEvent);
                }
                boolean triggered = ServerMessagesHandler.handleReplies(messageReceivedEvent, description);
                if (!triggered && context.game != null)
                    ServerMessagesHandler.handleReplies(messageReceivedEvent, context.game);
            }
        }
        catch (Exception exception) {
            ExceptionUtils.reportException(
                "Exception while reading attachment of message:",
                exception, messageReceivedEvent.getChannel().asGuildMessageChannel());
        }
        return messageCreateData;
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

    public static boolean isValidFileFormat(Attachment attachment, boolean strict) {
        if (attachment.getFileExtension() == null) return false;
        if (!attachment.getFileExtension().equalsIgnoreCase("log") && !attachment.getFileExtension().equalsIgnoreCase("txt")) return false;
        String fileName = attachment.getFileName().toLowerCase();
        if (strict && fileName.startsWith("message")) return true;
        return fileName.startsWith("latest") ||
            fileName.startsWith("melonloader") ||
            fileName.startsWith("mlinstall");
    }

    private static void postReadApiPass(MelonScanContext context) {
        Consumer<MelonScanContext> postReadApiPass = MelonScannerApisManager.getPostReadPass(context.game);
        if (postReadApiPass != null)
            postReadApiPass.accept(context);
    }

    private static boolean getModsFromApi(MelonScanContext context) {
        if (context.game != null && context.game.equalsIgnoreCase("bloonstd6-epic")) {
            context.game = "BloonsTD6";
            context.epic = true;
        }
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
            final String id = logsModDetails.id;

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
            boolean latestModBroken = false;
            for (MelonApiMod modDetail : context.modDetails) {
                if (modDetail.id != null && modDetail.id.equals(id) ||
                    modDetail.name.replaceAll("[-_ ]", "").equals(modName.replaceAll("[-_ ]", "")) ||
                    (deprecatedName = ArrayUtils.contains(modDetail.aliases, modName))
                ) {
                    System.out.println("Mod found in db: " + modDetail.name + " version " + modDetail.versions[0].version.getRaw());
                    latestModName = modDetail.name;
                    latestModVersion = modDetail.versions[0].version;
                    latestModDownloadUrl = modDetail.downloadLink;
                    latestModType = modDetail.modtype;
                    latestHasPending = modDetail.haspending;
                    latestModHash = modDetail.versions[0].hash;
                    latestModBroken = modDetail.isbroken;
                    if (latestModVersion != null && latestModHash != null && latestModVersion.getRaw().equals(logsModDetails.version) && !latestModHash.equals(logsModDetails.hash)) {
                        context.corruptedMods.add(modDetail);
                        System.out.println("Mod " + modDetail.name + " is corrupted, API hash: " + latestModHash + " vs. logs hash: " + logsModDetails.hash);
                    }
                    break;
                }
            }

            int compare = VersionUtils.compareVersion(latestModVersion, modVersion);
            if (latestModVersion == null && latestModHash == null && latestModType == null) {
                context.unknownMods.add(logsModDetails);
            }
            else if (latestHasPending && compare == 0) {
                context.hasPendingMods.add(modName);
            }
            else if (MelonScannerApisManager.brokenMods.contains(modName) || latestModBroken) {
                context.brokenMods.add(modName);
            }
            else if (MelonScannerApisManager.retiredMods.contains(modName)) {
                context.retiredMods.add(modName);
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
            if (context.mlHashCode.equals(hashes.x64()) || context.mlHashCode.equals(hashes.x86())) {
                System.out.println("ML hash found in known hashes");
                return;
            }
        }
        context.modifiedML = true;
        System.out.println("unknown hash");
        reportUserModifiedML(context.messageReceivedEvent);
    }

    private static void badModCheck(MelonScanContext context) {
        long guildid = context.messageReceivedEvent.getGuild().getIdLong();
        if (guildid != 439093693769711616L && guildid != 600298024425619456L && guildid != 663449315876012052L && guildid != 716536783621587004L && guildid != 936064484391387256L)
            return;
        if (context.badMods.isEmpty() && context.badPlugins.isEmpty())
            return;
        context.messageReceivedEvent.getMessage().delete().reason("Bad mods detected").queue();
    }

    private static void checkForPirate(MelonScanContext context) {
        if (context.gamePath == null && context.mlVersion != null && VersionUtils.compareVersion("0.5.0", context.mlVersion) <= 0) {
            context.editedLog = true; //trigger the `dont edit the log` message
            context.pirate = true;
        }
        else if (context.game == null || context.mlVersion != null && VersionUtils.compareVersion("0.5.0", context.mlVersion) > 0) {
            context.pirate = false;
        }
        else if (context.gamePath.toLowerCase().contains("steamrip")) {
            context.pirate = true;
        }
        else if (context.game.equalsIgnoreCase("BloonsTD6")) {
            if (!context.gamePath.toLowerCase().contains("steamapps\\common\\bloonstd6") && !context.gamePath.toLowerCase().contains("steamapps\\common\\bloons td 6") && !context.gamePath.toLowerCase().contains("program files\\windowsapps") && !context.epic) {
                context.pirate = true;
            }
            else if (context.gameBuild != null && context.gameBuild.startsWith("34") && VersionUtils.compareVersion("0.6.0", context.mlVersion) > 0) {
                context.embedBuilder.addField("BTD6 34", "For BTD6 version 34, Please upgrade to Alpha MelonLoader 0.6.0, you may also need to update your mods.", false);
            }
        }
        else if (context.game.equalsIgnoreCase("BONEWORKS")) {
            if (!context.gamePath.toLowerCase().contains("steamapps\\common\\boneworks\\boneworks") && !context.gamePath.toLowerCase().contains("software\\stress-level-zero-inc-boneworks")) {
                context.pirate = true;
            }
        }
        else if (context.game.equalsIgnoreCase("TheLongDark")) {
            if (context.gameBuild != null && VersionUtils.compareVersion("2.06", context.gameBuild) >= 0 && VersionUtils.compareVersion("0.6.0", context.mlVersion) > 0) {
                context.embedBuilder.addField("TLD MLALPHA", "For TLD version 2.06+, Please upgrade to Alpha MelonLoader 0.6.0, you may also need to update your mods.", false);
            }
        }
    }

    private static void prepareEmbed(MelonScanContext context) {
        String footer = "Lum Log Scanner";
        if (context.loadedMods.size() > 0) {
            footer += " | " + context.loadedMods.size() + " mods loaded";
        }
        if (context.omittedLineCount > 0) {
            footer += " | omitted " + context.omittedLineCount + " line" + (context.omittedLineCount > 1 ? "s" : "");
        }
        context.embedBuilder = new EmbedBuilder();
        context.reportMessage = new StringBuilder();
        context.embedBuilder.setTitle(Localization.get("melonscanner.logautocheckresult", context.lang))
                            .setTimestamp(Instant.now())
                            .setFooter(footer);
        checkForPirate(context);
        if (context.game != null) {
            String unityName = context.game.replace(" ", "").toLowerCase();
            if (unityName.equals("unknown"))
                return;
            try {
                ResultSet result = DBConnectionManagerLum.sendRequest("CALL `FetchIcon`(?)", unityName);
                if (result.next()) {
                    String url;
                    if (context.pirate)
                        url = result.getString("PirateURL");
                    else
                        url = result.getString("IconURL");
                    if (url == null)
                        context.messageReceivedEvent.getJDA().getTextChannelById("1001529648569659432").sendMessageEmbeds(
                                Utils.wrapMessageInEmbed("No logo found for " + unityName + "\n" + context.messageReceivedEvent.getMessage().getJumpUrl(), Color.ORANGE)).queue();
                    else if (url.length() > 1) // allows me to disable messages for edited/test games
                        context.embedBuilder.setThumbnail(url);
                }
                else {
                    context.messageReceivedEvent.getJDA().getTextChannelById("1001529648569659432").sendMessageEmbeds(
                            Utils.wrapMessageInEmbed("No thumbnail found for " + context.game + "\n" + unityName + "\n" + context.messageReceivedEvent.getMessage().getJumpUrl(), Color.RED)).queue();
                    DBConnectionManagerLum.sendUpdate("INSERT INTO `Icons` (`UnityName`) VALUES (?)", unityName);
                }
                DBConnectionManagerLum.closeRequest(result);
            }
            catch (Exception e) {
                ExceptionUtils.reportException("Failure to fetch game logo for " + context.game, e);
            }
        }
    }

    private static void fileAgeCheck(MelonScanContext context) {
        if (context.attachment.getFileName().matches("MelonLoader_\\d{2}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}\\.\\d{3}.*\\.log")) {
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

        if (context.editedLog) {
            context.embedBuilder.addField(Localization.get("melonscanner.readerror.fieldname", context.lang), Localization.get("melonscanner.readerror.field", context.lang), false);
            context.embedColor = Color.RED;
        }
    }

    private static boolean mlOutdatedCheck(MelonScanContext context) {
        context.isMLOutdated = context.mlVersion != null && (context.alpha ? (!CrossServerUtils.sanitizeInputString(context.mlVersion).equals(latestMLVersionAlpha) && VersionUtils.compareVersion(latestMLVersionAlpha, latestMLVersionRelease) == 1/* If Alpha is more recent */) : (!CrossServerUtils.sanitizeInputString(context.mlVersion).equals(latestMLVersionRelease)));
        if (context.isMLOutdated || context.modifiedML) {
            int result = VersionUtils.compareVersion(context.alpha ? latestMLVersionAlpha : latestMLVersionRelease, context.mlVersion);
            System.out.println("ML Outdated, isMLOutdated:" + context.isMLOutdated + " modifiedML:" + context.modifiedML + " Result:" + result + " Installed:" + context.mlVersion);
            switch (result) {
                case 1 -> //left more recent
                        context.embedBuilder.addField(Localization.get("melonscanner.mloutdated.fieldname", context.lang), Localization.getFormat("melonscanner.mloutdated.upbeta", context.lang, CrossServerUtils.sanitizeInputString(context.mlVersion), latestMLVersionRelease), false);
                case 0 -> { //identicals
                    if (context.alpha)
                        context.embedBuilder.addField(Localization.get("melonscanner.mloutdated.fieldname", context.lang), Localization.getFormat("melonscanner.mloutdated.upalpha", context.lang, latestMLVersionAlpha), false);
                    else
                        context.embedBuilder.addField(Localization.get("melonscanner.mloutdated.fieldname", context.lang), Localization.getFormat("melonscanner.mloutdated.reinstall", context.lang, latestMLVersionRelease), false);
                }
                case -1 -> //right more recent
                        context.embedBuilder.addField(Localization.get("melonscanner.mloutdated.fieldname", context.lang), Localization.getFormat("melonscanner.mloutdated.downgrade", context.lang, context.alpha ? latestMLVersionAlpha : latestMLVersionRelease), false);
                default -> {
                }
            }
            return true;
        }
        return false;
    }

    private static boolean knownErrorsCheck(MelonScanContext context) {
        context.errors.removeIf(e -> e == null || e.error == null || e.error.isBlank());
        if (context.errors.size() > 0) {
            StringBuilder error = new StringBuilder();
            for (int i = 0; i < context.errors.size(); ++i) {
                String errorString = context.errors.get(i).error(context.lang);
                if (errorString != null && errorString.contains("$cleanunity$")) {
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
            for (int i = 0; i < context.duplicatedMods.size() && i < (context.duplicatedMods.size() == 11 ? 11 : 10); ++i)
                error.append("- ").append(CrossServerUtils.sanitizeInputString(context.duplicatedMods.get(i) + "\n"));
            if (context.duplicatedMods.size() > 11)
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
            for (int i = 0; i < context.missingMods.size() && i < (context.missingMods.size() == 11 ? 11 : 10); ++i) {
                String missingModName = context.missingMods.get(i);
                String missingModDownloadLink = MelonScannerApisManager.getDownloadLinkForMod(context.game, missingModName);
                if (missingModDownloadLink != null)
                    error.append("- [").append(CrossServerUtils.sanitizeInputString(missingModName)).append("](").append(missingModDownloadLink).append(")\n");
                else
                    error.append("- ").append(CrossServerUtils.sanitizeInputString(missingModName)).append("\n");
            }
            if (context.missingMods.size() > 11)
                error.append(Localization.getFormat("melonscanner.missingmods.more", context.lang, context.missingMods.size() - 10));

            context.embedBuilder.addField(Localization.get("melonscanner.missingmods.fieldname", context.lang), error.substring(0, Math.min(error.toString().length(), MessageEmbed.VALUE_MAX_LENGTH)), false);
            context.embedColor = Color.ORANGE;
            return true;
        }
        return false;
    }

    private static boolean missingEpicCompat(MelonScanContext context) {
        if (context.epic && "BloonsTD6".equalsIgnoreCase(context.game) && !context.loadedMods.containsKey("BTD6 Epic Games Mod Compat")) {
            context.embedBuilder.addField("BTD6 Epic Games Mod Compat", "Please add [BTD6 Epic Games Mod Compat](https://github.com/Baydock/BTD6EpicGamesModCompat/#installation) to your Plugins folder", false);
            context.embedColor = Color.ORANGE;
            return true;
        }
        return false;
    }

    private static boolean incompatibleModsCheck(MelonScanContext context) {
        if (context.incompatibleMods.size() > 0) {
            StringBuilder error = new StringBuilder();
            for (int i = 0; i < context.incompatibleMods.size() && i < (context.incompatibleMods.size() == 11 ? 11 : 10); ++i) {
                MelonIncompatibleMod incompatibleMod = context.incompatibleMods.get(i);
                error.append("- ").append(incompatibleMod.mod).append(" is incompatible with ").append(incompatibleMod.incompatible).append("\n");
            }
            if (context.incompatibleMods.size() > 11)
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
            for (int i = 0; i < context.corruptedMods.size() && i < (context.corruptedMods.size() == 11 ? 11 : 10); ++i)
                if (context.corruptedMods.get(i).downloadLink != null)
                    error.append("- [").append(CrossServerUtils.sanitizeInputString(context.corruptedMods.get(i).name)).append("](").append(context.corruptedMods.get(i).downloadLink).append(")\n");
                else
                    error.append("- ").append(CrossServerUtils.sanitizeInputString(context.corruptedMods.get(i).name + "\n"));
            if (context.corruptedMods.size() > 11)
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
            for (int i = 0; i < context.brokenMods.size() && i < (context.brokenMods.size() == 21 ? 21 : 20); ++i)
                error.append("- ").append(CrossServerUtils.sanitizeInputString(context.brokenMods.get(i) + "\n"));
            if (context.brokenMods.size() > 21)
                error.append(Localization.getFormat("melonscanner.brokenmods.more", context.lang, context.brokenMods.size() - 20));

            context.embedBuilder.addField(Localization.get("melonscanner.brokenmods.fieldname", context.lang), error.substring(0, Math.min(error.toString().length(), MessageEmbed.VALUE_MAX_LENGTH)), false);
            context.embedColor = Color.RED;
            return true;
        }
        return false;
    }

    private static boolean retiredModsCheck(MelonScanContext context) {
        context.retiredMods.removeAll(MelonLoaderError.getModSpecificErrors().stream().map(m -> m.regex).toList());
        if (context.retiredMods.size() > 0) {
            context.retiredMods.sort(String.CASE_INSENSITIVE_ORDER);
            StringBuilder error = new StringBuilder(Localization.get("melonscanner.modretired.field", context.lang) + "\n");
            for (int i = 0; i < context.retiredMods.size() && i < (context.retiredMods.size() == 21 ? 21 : 20); ++i)
                error.append("- ").append(CrossServerUtils.sanitizeInputString(context.retiredMods.get(i) + "\n"));
            if (context.retiredMods.size() > 21)
                error.append(Localization.getFormat("melonscanner.brokenmods.more", context.lang, context.retiredMods.size() - 20));

            context.embedBuilder.addField(Localization.get("melonscanner.modretired.fieldname", context.lang), error.substring(0, Math.min(error.toString().length(), MessageEmbed.VALUE_MAX_LENGTH)), false);
            context.embedColor = Color.RED;
            return true;
        }
        return false;
    }

    private static boolean oldModsCheck(MelonScanContext context) {
        boolean reinstallML = context.errors.stream().filter(i -> i.error != null).anyMatch(e -> e.error.contains("reinstall"));
        if (context.oldMods.size() > 0 && !context.isMLOutdated || context.modifiedML || reinstallML) {
            context.oldMods.sort(String.CASE_INSENSITIVE_ORDER);
            StringBuilder error = new StringBuilder();
            boolean added = false;
            for (int i = 0; i < context.oldMods.size() && i < (context.oldMods.size() == 21 ? 21 : 20); ++i) {
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
            if (context.oldMods.size() > 21)
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
            for (int i = 0; i < context.unknownMods.size() && i < (context.unknownMods.size() == 11 ? 11 : 10); ++i) {
                LogsModDetails md = context.unknownMods.get(i);
                String unknowModOut = CrossServerUtils.sanitizeInputString(md.name);
                if (md.version != null && !md.version.isBlank())
                    unknowModOut += " " + CrossServerUtils.sanitizeInputString(md.version);
                if (md.author != null)
                    unknowModOut = Localization.getFormat("melonscanner.unknownmods.modnamewithauthor", context.lang, unknowModOut, CrossServerUtils.sanitizeInputString(md.author));
                error.append("- ").append(unknowModOut).append("\n");
            }
            if (context.unknownMods.size() > 11)
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
            for (int i = 0; i < context.misplacedMods.size() && i < (context.misplacedMods.size() == 11 ? 11 : 10); ++i) {
                String mm = context.misplacedMods.get(i);
                error.append("- ").append(CrossServerUtils.sanitizeInputString(mm)).append("\n");
            }
            if (context.misplacedMods.size() > 11)
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
            for (int i = 0; i < context.misplacedPlugins.size() && i < (context.misplacedPlugins.size() == 11 ? 11 : 10); ++i) {
                String mp = context.misplacedPlugins.get(i);
                error.append("- ").append(CrossServerUtils.sanitizeInputString(mp)).append("\n");
            }
            if (context.misplacedPlugins.size() > 11)
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
                case "ChilloutVR" -> {
                    if (context.misplacedPlugins.contains("CVRModUpdater.Loader"))
                        muMessage = "";
                    else if (context.loadedMods.containsKey("CVRModUpdater.Loader"))
                        muMessage = "";
                    else if (context.loadedMods.containsKey("UpdateChecker"))
                        muMessage = "";
                    else
                        muMessage = Localization.get("melonscanner.outdatedmods.cvrmuwarning", context.lang);
                }
                case "TheLongDark" -> {
                    if (context.misplacedPlugins.contains("AutoUpdatingPlugin"))
                        muMessage = "";
                    else
                        muMessage = Localization.get("melonscanner.outdatedmods.tldmuwarning", context.lang);
                }
                default -> muMessage = "";
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
            if (context.outdatedMods.size() >= 2)
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
            StringBuilder error = new StringBuilder(Localization.get("melonscanner.modpending.field", context.lang) + "\n");
            for (int i = 0; i < context.hasPendingMods.size() && i < (context.hasPendingMods.size() == 21 ? 21 : 20); ++i)
                error.append("- ").append(CrossServerUtils.sanitizeInputString(context.hasPendingMods.get(i))).append("\n");
            if (context.hasPendingMods.size() > 21)
                error.append(Localization.getFormat("melonscanner.modsthrowingerrors.more", context.lang, context.hasPendingMods.size() - 20));

            context.embedColor = Color.ORANGE;
            context.embedBuilder.addField(Localization.get("melonscanner.haspendingmods.fieldname", context.lang), error.substring(0, Math.min(error.toString().length(), MessageEmbed.VALUE_MAX_LENGTH)), false);
            return true;
        }
        return false;
    }

    private static boolean modsThrowingErrorsCheck(MelonScanContext context) {
        context.modsThrowingErrors.removeAll(context.brokenMods);
        if (context.modsThrowingErrors.size() > 0 && !context.isMLOutdated) {
            context.modsThrowingErrors.sort(String.CASE_INSENSITIVE_ORDER);
            StringBuilder error = new StringBuilder();
            for (int i = 0; i < context.modsThrowingErrors.size() && i < (context.modsThrowingErrors.size() == 11 ? 11 : 10); ++i)
                error.append("- ").append(CrossServerUtils.sanitizeInputString(context.modsThrowingErrors.get(i))).append("\n");
            if (context.modsThrowingErrors.size() > 11)
                error.append(Localization.getFormat("melonscanner.modsthrowingerrors.more", context.lang, context.modsThrowingErrors.size() - 10));

            context.embedBuilder.addField(Localization.get("melonscanner.modsthrowingerrors.fieldname", context.lang), error.substring(0, Math.min(error.toString().length(), MessageEmbed.VALUE_MAX_LENGTH)), false);
            context.embedColor = Color.RED;
            return true;
        }
        return false;
    }

    private static boolean minorErrorsHandling(MelonScanContext context) {
        if (context.mlHashCode != null && context.mlHashCode.equals("57545710048555350515452101521025297995653101559949575755515399509950") && (context.loadedMods.containsKey("Action Menu") || context.loadedMods.containsKey("CameraAnimation") || context.loadedMods.containsKey("FreezeFrame")))
            context.embedBuilder.addField("AM issue", "There is an issue with ML 0.5.5. Please try the latest [nightly build of ML](https://nightly.link/LavaGang/MelonLoader/workflows/build/alpha-development/MelonLoader.x64.CI.Release.zip) or maybe downgrade to 0.5.4.", false);
        if (!context.assemblyGenerationFailed && !context.isMLOutdated && context.duplicatedMods.size() == 0 && context.outdatedMods.size() == 0) {
            String error = "";
            if (context.noMods && context.missingMods.size() == 0 && context.preListingModsPlugins && !context.errors.contains(MelonLoaderError.incompatibleAssemblyError))
                error += Localization.get("melonscanner.othererrors.partiallog", context.lang) + "\n";

            if (context.noMods && context.misplacedMods.size() == 0 && !context.preListingModsPlugins && context.errors.size() == 0) {
                long guildID = context.messageReceivedEvent.getGuild().getIdLong();
                if (guildID == 439093693769711616L)
                    error += Localization.get("melonscanner.othererrors.nomodsvrcmg", context.lang) + "\n";
                else if (guildID == 322211727192358914L || guildID == 835185040752246835L) {
                    error += Localization.get("melonscanner.othererrors.nomodstld", context.lang) + "\n";
                    context.embedBuilder.setThumbnail("https://pbs.twimg.com/media/EU5rcX4WsAMcc-y?format=jpg");
                }
                else
                    error += Localization.get("melonscanner.othererrors.nomods", context.lang) + "\n";
            }
            if (context.hasNonModErrors && context.errors.size() == 0) {
                error += Localization.get("melonscanner.othererrors.unidentifiederrors", context.lang) + "\n";
                context.unidentifiedErrors = true;
            }
            if (context.mlVersion != null && VersionUtils.compareVersion(latestMLVersionRelease, context.mlVersion) == 0 && context.missingMods.contains("XUnity.AutoTranslator.Plugin.Core")) {
                error += Localization.get("Make sure that you installed all of XUnity.AutoTranslator including the UserLibs folder", context.lang) + "\n";
            }
            if (context.line.contains("Applied USER32.dll::SetTimer patch")) {
                error += Localization.get("MelonLoader most likely crashed because of Start Screen. Try adding the launch option `--melonloader.disablestartscreen` and see if that helps.", context.lang) + "\n";
            }
            if (context.line.contains("Downloading")) {
                error += Localization.get("MelonLoader gotten stuck downloading, make sure that nothing is blocking downloads.", context.lang) + "\n";
            }
            if (context.line.contains("Contacting RemoteAPI...")) {
                error += Localization.get("Unity failed to initialize graphics. Please make sure that your GPU drivers are up to date.", context.lang) + "\n";

            }

            if (context.osType != null && context.osType.matches("Wine.*") && (context.missingMods.contains("UnityEngine.UI") || context.missingMods.contains("Assembly-CSharp")))
                context.embedBuilder.addField(Localization.get("We are investigating issues with melonloader on recent versions of Wine and IL2CPP games.", context.lang), Localization.get("Try and run both of these commands```protontricks --no-runtime 305620 --force vcrun2019\nprotontricks --no-runtime 305620 --force dotnet48```then select win10 and add version to overrides.", context.lang), false);

            if (error.length() > 0) {
                context.embedBuilder.addField(Localization.get("melonscanner.othererrors.fieldname", context.lang), error, false);
                context.embedColor = Color.RED;
            }
            else if (context.mlVersion != null && (context.loadedMods.size() == 0 || context.preListingModsPlugins) && context.errors.size() == 0) {
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
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Exception while translating log:", e, event.getChannel().asGuildMessageChannel());
        }
    }
}
