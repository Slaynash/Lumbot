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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.UrlShortener;
import slaynash.lum.bot.discord.ArrayUtils;
import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.discord.ExceptionUtils;
import slaynash.lum.bot.discord.JDAManager;
import slaynash.lum.bot.discord.MLHashPair;
import slaynash.lum.bot.discord.ServerMessagesHandler;

public final class MelonScanner {

    public static String latestMLVersionRelease = "0.2.7.4";
    public static String latestMLVersionBeta = "0.3.0";
    
    private static Color melonPink = new Color(255, 59, 106); 
    
    private static ScheduledExecutorService sheduledExecutor = Executors.newSingleThreadScheduledExecutor();

    public static void init() {
        MelonLoaderError.init();
        MelonScannerApisManager.startFetchingThread();

        sheduledExecutor.scheduleWithFixedDelay(() -> {
            LogCounter.UpdateLogCounter();
        }, 30, 30, TimeUnit.SECONDS);
    }

    public static void shutdown() {
        sheduledExecutor.shutdown();
    }


    public static void scanMessage(MessageReceivedEvent messageReceivedEvent) {
        
        try {

            List<Attachment> attachments = messageReceivedEvent.getMessage().getAttachments();

            if (multipleLogsCheck(attachments, messageReceivedEvent)) // This should not happen, but just in case
                return;

            Attachment attachment = attachments.stream().filter(a -> isValidFileFormat(a)).findFirst().orElse(null);
            if (attachment == null)
                return;
            
            MelonScanContext context = new MelonScanContext(attachment, messageReceivedEvent);
            if (oldEmmVRCLogCheck(context))
                return;

            if (!MelonScannerReadPass.DoPass(context))
                return;

            VRCHashCheck(context);

            if (GetModsFromApi(context)) {
                CleanupDuplicateMods(context);
                ProcessFoundMods(context);
            }

            unofficialMLCheck(context);
            prepareEmbed(context);
            fileAgeCheck(context);
            fillEmbedDescription(context);


            boolean issueFound = false;
            issueFound |= mlOutdatedCheck(context);
            issueFound |= vrchatVersionCheck(context);
            issueFound |= knownErrorsCheck(context);
            issueFound |= duplicatedModsCheck(context);
            issueFound |= missingModsCheck(context);
            //issueFound |= incompatibleModsCheck(context);
            issueFound |= brokenModsCheck(context);
            issueFound |= unknownModsCheck(context);
            issueFound |= outdatedModsCheck(context);
            issueFound |= misplacedModsCheck(context);
            issueFound |= misplacedPluginsCheck(context);
            issueFound |= modsThrowingErrorsCheck(context);
            issueFound |= minorErrorsHandling(context);

            if (issueFound) {
                if (context.isMLOutdatedVRC || context.isMLOutdated)
                    context.embedColor = melonPink;

                if(!context.unidentifiedErrors)
                    ServerMessagesHandler.addNewHelpedRecently(messageReceivedEvent);
            }
            else if (context.mlVersion != null) {
                if (context.hasErrors) {
                    context.embedBuilder.addField("There are unidentified errors", "- please wait while someone checks them" , false);
                    context.embedColor = Color.RED;
                }
                else {
                    context.embedBuilder.addField("No issues found", "- If you had issues please say so and wait for someone to check the log" , false);
                    context.embedColor = Color.LIGHT_GRAY;
                    ServerMessagesHandler.addNewHelpedRecently(messageReceivedEvent);
                }
            }
            
            if (issueFound || context.mlVersion != null) {
                LogCounter.AddtoCounter(attachment);
                context.embedBuilder.setColor(context.embedColor);
                MessageBuilder messageBuilder = new MessageBuilder();
                messageBuilder.append("<@" + context.messageReceivedEvent.getAuthor().getId() + ">");
                messageReceivedEvent.getChannel().sendMessage(messageBuilder.setEmbed(context.embedBuilder.build()).build()).queue();
            }
        }
        catch (Exception exception) {
            String channelName = messageReceivedEvent.getGuild().getName() + " #" + messageReceivedEvent.getChannel().getName() + " > " + messageReceivedEvent.getMessageId();
            String channelLink = "https://canary.discord.com/channels/" + messageReceivedEvent.getGuild().getId() + "/" + messageReceivedEvent.getChannel().getId() + "/" + messageReceivedEvent.getMessageId();
            ExceptionUtils.reportException(
                "Exception while reading attachment of message " + channelLink + ":",
                "In [" + channelName + "](" + channelLink + "):",
                exception);
        }
    }


    // Message sanity check

    private static boolean multipleLogsCheck(List<Attachment> attachments, MessageReceivedEvent messageReceivedEvent) {
        boolean hasAlreadyFound = false;
        for (Attachment attachment : attachments)
            if (isValidFileFormat(attachment)) {
                if (hasAlreadyFound) {
                    replyStandard("Please send one log at a time.", Color.red, messageReceivedEvent);
                    return true;
                }
                hasAlreadyFound = true;
            }
        return false;
    }

    private static boolean oldEmmVRCLogCheck(MelonScanContext context) {
        if (context.attachment.getFileName().startsWith("emmVRC")) {
            replyStandard("This is an old log from emmVRC and should be deleted. Please upload your MelonLoader/Latest.log or update MelonLoader to 0.3.0 if missing.", Color.orange, context.messageReceivedEvent);
            return true;
        }
        return false;
    }

    private static boolean isValidFileFormat(Attachment attachment) {
        return attachment.getFileExtension() != null && (
            attachment.getFileExtension().toLowerCase().equals("log") ||
            attachment.getFileExtension().toLowerCase().equals("txt"));
    }


    // Logs thinkering

    private static void VRCHashCheck(MelonScanContext context) {
        if ("VRChat".equals(context.game)) {
            context.isMLOutdatedVRC = true;
            if (context.mlHashCode != null && "VRChat".equals(context.game)) {
                System.out.println("game is vrc, checking hash");
                
                boolean hasVRChat1043ReadyML = false;
                //boolean hasNotBrokenDeobfMap = false;
                for (MLHashPair mlHashes : (context.alpha ? CommandManager.melonLoaderAlphaHashes : CommandManager.melonLoaderHashes)) {
                    System.out.println("x86: " + mlHashes.x86 + ", x64: " + mlHashes.x64);
                    //if (mlHashes.x64.equals("25881"))
                    //    hasNotBrokenDeobfMap = true;
                    if (mlHashes.x64.equals(CommandManager.melonLoaderVRCHash))
                        hasVRChat1043ReadyML = true;
                    
                    if (mlHashes.x64.equals(context.mlHashCode)) {
                        System.out.println("matching hash found");
                        if (hasVRChat1043ReadyML)
                            context.isMLOutdatedVRC = false;
                        //if (!hasNotBrokenDeobfMap)
                        //    context.isMLOutdatedVRCBrokenDeobfMap = true;
                        break;
                    }
                }
            }
        }
    }

    private static boolean GetModsFromApi(MelonScanContext context) {
        return (context.modDetails = MelonScannerApisManager.getMods(context.game)) != null;
    }

    private static void CleanupDuplicateMods(MelonScanContext context) {
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

    private static void ProcessFoundMods(MelonScanContext context) {
        for (Entry<String, LogsModDetails> entry : context.loadedMods.entrySet()) {
            final String modName = entry.getKey();
            final LogsModDetails logsModDetails = entry.getValue();
            final VersionUtils.VersionData modVersion = logsModDetails.version != null ? VersionUtils.GetVersion(logsModDetails.version) : null;
            //String modHash = logsModDetails.hash;
                
            for (MelonLoaderError modSpecificError : MelonLoaderError.modSpecificErrors) {
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
            String latestModDownloadUrl = null;
            boolean deprecatedName = false;
            for (MelonApiMod modDetail : context.modDetails) {
                if (modDetail.name.equals(modName) || (deprecatedName = ArrayUtils.contains(modDetail.aliases, modName))) {
                    /*
                    if (checkUsingHash) {
                        // TODO Hash comparison
                    }
                    else*/ {
                        System.out.println("Mod found in db: " + modDetail.name + " version " + modDetail.versions[0].version.getRaw());
                        latestModName = modDetail.name;
                        latestModVersion = modDetail.versions[0].version;
                        latestModDownloadUrl = modDetail.downloadLink;
                        break;
                    }
                }
            }

            if (latestModVersion == null && latestModHash == null) {
                context.unknownMods.add(logsModDetails);
            }
            else if (CommandManager.brokenVrchatMods.contains(modName)) {
                context.brokenMods.add(modName);
            }
            else if (deprecatedName || /*checkUsingHash ? (modHash != null && !modHash.equals(latestModHash)) :*/ VersionUtils.CompareVersion(latestModVersion, modVersion) > 0) {
                context.outdatedMods.add(new MelonOutdatedMod(modName, latestModName, modVersion.getRaw(), latestModVersion.getRaw(), latestModDownloadUrl));
                context.modsThrowingErrors.remove(modName);
            }
        }
    }

    private static void unofficialMLCheck(MelonScanContext context) {
        if (context.mlHashCode == null)
            return;
        
        boolean found = false;
        for (MLHashPair hashes : (context.alpha ? CommandManager.melonLoaderAlphaHashes : CommandManager.melonLoaderHashes)) {
            if (context.mlHashCode.equals(hashes.x64) || context.mlHashCode.equals(hashes.x86)) {
                System.out.println("hash found in known hashes: " + found);
                return;
            }
        }
        System.out.println("unknown hash");
        reportUserModifiedML(context.messageReceivedEvent);
    }

    private static void prepareEmbed(MelonScanContext context) {
        context.embedBuilder = new EmbedBuilder();
        context.reportMessage = new StringBuilder();
        context.embedBuilder.setTitle("Log Autocheck Result:");
        context.embedBuilder.setTimestamp(Instant.now());
        context.embedBuilder.setFooter("Lum Log Scanner");
        
        if (context.game != null) {
            switch (context.game) {
                case "Among Us":
                    context.embedBuilder.setThumbnail("https://i.imgur.com/cGdWOch.png");
                    break;
                case "Audica":
                    context.embedBuilder.setThumbnail("https://i.imgur.com/CHa4yW0.png");
                    break;
                case "BloonsTD6":
                    if(context.pirate)
                        context.embedBuilder.setThumbnail("https://i.redd.it/76et0pfu87e31.png"); //sad monkey
                    else
                        context.embedBuilder.setThumbnail("https://i.imgur.com/BSXtkvW.png");
                    break;
                case "BONEWORKS":
                    context.embedBuilder.setThumbnail("https://puu.sh/HAj1G/87f77fddf2.png");
                    break;
                case "guigubahuang":
                    context.embedBuilder.setThumbnail("https://cdn.discordapp.com/attachments/760342261967487068/837379147617140786/guigubahuang.png");
                    break;
                case "Job Simulator":
                    context.embedBuilder.setThumbnail("https://i.imgur.com/0kmohjK.png");
                    break;
                case "Pistol Whip":
                    context.embedBuilder.setThumbnail("https://i.imgur.com/MeMcntj.png");
                    break;
                case "TheLongDark":
                    context.embedBuilder.setThumbnail("https://puu.sh/HAj1H/e2f9018e69.png");
                    break;
                case "VRChat":
                    context.embedBuilder.setThumbnail("https://puu.sh/HAiW4/bb2a98afdc.png");
                    break;
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
            catch (Exception e) { e.printStackTrace(); }
            
            if (ageDays > 1) {
                if (context.pre3)
                    context.reportMessage.append("This log file is " + ageDays + " days old\n");
                else
                    context.reportMessage.append("This log file is " + ageDays + " days old. Reupload your log from MelonLoader/Latest.log\n");
            }
        }
    }

    private static void fillEmbedDescription(MelonScanContext context) {
        //if (omittedLines > 0)
        //    message += "*Omitted " + omittedLines + " lines of length > 1000.*\n";
                
        if (context.consoleCopyPaste)
            context.reportMessage.append("*You sent a copy of the console logs. Please type `!logs` to know where to find the complete game logs.*\n");

        if (context.game != null && context.mlVersion != null && !latestMLVersionBeta.equals(latestMLVersionRelease) && context.mlVersion.equals(latestMLVersionBeta))
            context.reportMessage.append("*You are running an alpha version of MelonLoader.*\n");

        /* TODO No hash error
        if (context.game != null && context.checkUsingHash && !context.hasMLHashes)
            context.reportMessage.append("*Your MelonLoader doesn't provide mod hashes (requires >0.3.0). Mod versions will not be verified.*\n");
        else*/ if (context.game != null && context.modDetails == null)
            context.reportMessage.append("*" + context.game + " isn't officially supported by the autochecker. Mod versions will not be verified.*\n");

        context.embedBuilder.setDescription(context.reportMessage);

        if (context.remainingModCount != 0) {
            context.embedBuilder.addField("Log Format:", "Error reading Log. Please do not edit the log and reupload the original log.", false);
            context.embedColor = Color.RED;
        }
    }

    private static boolean mlOutdatedCheck(MelonScanContext context) {
        // TODO use VersionUtils compare
        context.isMLOutdated = context.mlVersion != null && !(context.mlVersion.equals(latestMLVersionRelease) || context.mlVersion.equals(latestMLVersionBeta));

        if (context.isMLOutdatedVRC) {
            if (context.pre3)
                context.embedBuilder.addField("Warning:", "Please update MelonLoader using the [official installer](https://github.com/LavaGang/MelonLoader.Installer/releases/latest/download/MelonLoader.Installer.exe) then check `Show ALPHA Pre-Releases`\nVRChat modding requires MelonLoader " + latestMLVersionBeta + " released after **" + CommandManager.melonLoaderVRCMinDate + ".**", false);
            else
                context.embedBuilder.addField("Warning:", "Please reinstall MelonLoader using the [official installer](https://github.com/LavaGang/MelonLoader.Installer/releases/latest/download/MelonLoader.Installer.exe)\nVRChat modding requires MelonLoader " + latestMLVersionBeta + " released after **" + CommandManager.melonLoaderVRCMinDate + ".**", false);
            return true;
        }
        else if (context.isMLOutdated) {
            context.embedBuilder.addField("Warning:", "Please update MelonLoader using the [official installer](https://github.com/LavaGang/MelonLoader.Installer/releases/latest/download/MelonLoader.Installer.exe)\nThe installed MelonLoader is outdated: " + sanitizeInputString(context.mlVersion) + " -> " + latestMLVersionRelease + ".", false);
            return true;
        }
        return false;
    }

    private static boolean vrchatVersionCheck(MelonScanContext context) {
        if (context.gameBuild != null) {
            if("VRChat".equals(context.game)) {
                context.gameBuild = context.gameBuild.split("-", 2)[1].substring(0, 4); //VRChat build number
                if(!context.gameBuild.equals(CommandManager.vrchatBuild)) {
                    context.embedBuilder.addField("VRChat:", "You are running an outdated version of VRChat: `" + sanitizeInputString(context.gameBuild) + "` -> `" + CommandManager.vrchatBuild + "`", false);
                    context.embedColor = Color.ORANGE;
                    return true;
                }
            }
        }
        //for pre0.4.0 VRChat version checking
        else if (context.emmVRCVRChatBuild != null && !context.emmVRCVRChatBuild.equals(CommandManager.vrchatBuild)) {
            context.embedBuilder.addField("VRChat:", "You are running an outdated version of VRChat: `" + sanitizeInputString(context.emmVRCVRChatBuild) + "` -> `" + CommandManager.vrchatBuild + "`", false);
            context.embedColor = Color.ORANGE;
            return true;
        }
        return false;
    }

    private static boolean knownErrorsCheck(MelonScanContext context) {
        if (context.errors.size() > 0) {
            String error = "";
            for (int i = 0; i < context.errors.size(); ++i) {
                error += "- " + context.errors.get(i).error + "\n";
            }
            context.embedBuilder.addField("Known Errors:", error, false);
            context.embedColor = Color.RED;
            return true;
        }
        return false;
    }

    private static boolean duplicatedModsCheck(MelonScanContext context) {
        if (context.duplicatedMods.size() > 0) {
            String error = "";
            for (int i = 0; i < context.duplicatedMods.size() && i < 10; ++i)
                error += "- " + sanitizeInputString(context.duplicatedMods.get(i) + "\n");
            if (context.duplicatedMods.size() > 10)
                error += "- and " + (context.duplicatedMods.size() - 10) + " more...";
           
            error += "- Duplicate Mods are known to crash MelonLoader. Make sure there is only one copy in both Mods and Plugins folder.";
            context.embedBuilder.addField("Duplicate Mods:", error , false);
            context.embedColor = Color.RED;
            return true;
        }
        return false;
    }

    private static boolean missingModsCheck(MelonScanContext context) {
        if (context.missingMods.size() > 0) {
            String error = "";
            for (int i = 0; i < context.missingMods.size() && i < 10; ++i) {
                String missingModName = context.missingMods.get(i);
                String missingModDownloadLink = MelonScannerApisManager.getDownloadLinkForMod(context.game, missingModName);
                if (missingModDownloadLink != null)
                    error += "- [" + sanitizeInputString(missingModName) + "]("+ missingModDownloadLink +")\n";
                else
                    error += "- " + sanitizeInputString(missingModName) + "\n";
            }
            if (context.missingMods.size() > 10)
                error += "- and " + (context.missingMods.size() - 10) + " more...";
            
            context.embedBuilder.addField("Missing Dependencies:", error , false);
            context.embedColor = Color.ORANGE;
            return true;
        }
        return false;
    }

    /*
    private static boolean incompatibleModsCheck(MelonScanContext context) {
        if (context.incompatibleMods.size() > 0) {
            String error = "";
            for (int i = 0; i < context.incompatibleMods.size() && i < 10; ++i)
                error += "- " + sanitizeInputString(context.incompatibleMods.get(i) + "\n");
            if (context.incompatibleMods.size() > 10)
                error += "- and " + (context.incompatibleMods.size() - 10) + " more...";
            
            context.embedBuilder.addField("Incompatible Mods:", error , false);
            context.embedColor = Color.RED;
            return true;
        }
        return false;
    }
    */

    private static boolean brokenModsCheck(MelonScanContext context) {
        if (context.brokenMods.size() > 0) {
            String error = "";
            for (int i = 0; i < context.brokenMods.size() && i < 20; ++i)
                error += "- " + sanitizeInputString(context.brokenMods.get(i) + "\n");
            if (context.brokenMods.size() > 20)
                error += "- and " + (context.brokenMods.size() - 20) + " more...";
            
            context.embedBuilder.addField("Currently Broken Mods:", error , false);
            context.embedColor = Color.RED;
            return true;
        }
        return false;
    }

    private static boolean unknownModsCheck(MelonScanContext context) {
        if (context.unknownMods.size() > 0) {
            String error = "";
            for (int i = 0; i < context.unknownMods.size() && i < 10; ++i) {
                LogsModDetails md = context.unknownMods.get(i);
                error += "- " + sanitizeInputString(md.name) + ((md.version.equals("") || md.version.length() > 20) ? "" : (" " + sanitizeInputString(md.version))) + (md.author != null ? (" **by** " + sanitizeInputString(md.author) + "\n") : "\n");
            }
            if (context.unknownMods.size() > 10)
                error += "- and " + (context.unknownMods.size() - 10) + " more...";
            
            context.embedBuilder.addField("Unverified/Unknown Mods:", error , false);
            if (context.embedColor.equals(Color.BLUE))
                context.embedColor = Color.RED;
            return true;
        }
        return false;
    }

    private static boolean misplacedModsCheck(MelonScanContext context) {
        if (context.misplacedMods.size() > 0) {
            String error = "Please move the following mods out of your Plugins and into the Mods folder.\n";
            for (int i = 0; i < context.misplacedMods.size() && i < 10; ++i) {
                String mm = context.misplacedMods.get(i);
                error += "- " + sanitizeInputString(mm) + "\n";
            }
            if (context.misplacedMods.size() > 10)
                error += "- and " + (context.misplacedMods.size() - 10) + " more...";
            
            context.embedBuilder.addField("Misplaced Mods:", error , false);
            if (context.embedColor.equals(Color.BLUE))
                context.embedColor = Color.RED;
            return true;
        }
        return false;
    }

    private static boolean misplacedPluginsCheck(MelonScanContext context) {
        if (context.misplacedPlugins.size() > 0) {
            String error = "Please move the following plugins out of your Mods and into the Plugins folder.\n";
            for (int i = 0; i < context.misplacedPlugins.size() && i < 10; ++i) {
                String mp = context.misplacedPlugins.get(i);
                error += "- " + sanitizeInputString(mp) + "\n";
            }
            if (context.misplacedPlugins.size() > 10)
                error += "- and " + (context.misplacedPlugins.size() - 10) + " more...";
            
            context.embedBuilder.addField("Misplaced Plugins:", error , false);
            if (context.embedColor.equals(Color.BLUE))
                context.embedColor = Color.RED;
            return true;
        }
        return false;
    }

    private static boolean outdatedModsCheck(MelonScanContext context) {
        if (context.outdatedMods.size() > 0) {
            String vrcmuMessage = "VRChat".equals(context.game) ? "- Consider getting [VRCModUpdater](https://s.slaynash.fr/VRCMULatest) and moving it to the **Plugins** folder" : "";
            
            String error = "";
            String nextModLine = computeOutdatedModLine(context.outdatedMods.get(0), context);
            for (int i = 0; i < context.outdatedMods.size() && i < 20; ++i) {
                error += nextModLine;
                
                if (i + 1 < context.outdatedMods.size())
                    nextModLine = computeOutdatedModLine(context.outdatedMods.get(i + 1), context);
                else 
                    break; // no next outdated Mod
                
                if (error.length() + nextModLine.length() + vrcmuMessage.length() + 18 > 1024){
                    error += "- and " + (context.outdatedMods.size() - i) + " more...\n"; //length is about 17 char
                    break;
                }
            }
            if (context.outdatedMods.size() >= 3)
                error += vrcmuMessage;
            
            context.embedBuilder.addField("Outdated Mods:", error, false);
            context.embedColor = Color.ORANGE;
            return true;
        }
        return false;
    }

    private static String computeOutdatedModLine(MelonOutdatedMod outdatedMod, MelonScanContext context) {
        String namePart = outdatedMod.downloadUrl == null ? outdatedMod.name : ("[" + outdatedMod.name + "](" + UrlShortener.GetShortenedUrl(outdatedMod.downloadUrl) + ")");
        String line = "- " + namePart + ": `" + sanitizeInputString(outdatedMod.currentVersion) + "` -> `" + outdatedMod.latestVersion + "`";
        if (!outdatedMod.name.equals(outdatedMod.newName))
            line += " (" + outdatedMod.newName + ")";
        line += "\n";
        return line;
    }

    private static boolean modsThrowingErrorsCheck(MelonScanContext context) {
        if (context.modsThrowingErrors.size() > 0 && !context.isMLOutdated && !context.isMLOutdatedVRC) {
            String error = "";
            for (int i = 0; i < context.modsThrowingErrors.size() && i < 10; ++i)
                error += "- " + sanitizeInputString(context.modsThrowingErrors.get(i)) + "\n";
            if (context.modsThrowingErrors.size() > 10)
                error += "- and " + (context.modsThrowingErrors.size() - 10) + " more...";
            
            context.embedBuilder.addField("Mods With Errors:", error , false);
            context.embedColor = Color.RED;
            return true;
        }
        return false;
    }

    private static boolean minorErrorsHandling(MelonScanContext context) {
        if (!context.assemblyGenerationFailed && !context.isMLOutdated && !context.isMLOutdatedVRC && context.duplicatedMods.size() == 0) {
            String error = "";
            if (context.noMods && context.missingMods.size() == 0 && context.preListingMods && !context.errors.contains(MelonLoaderError.incompatibleAssemblyError))
                error += "- You have a partial log. Either MelonLoader crashed or you entered select mode in MelonLoader console and need to push any key.\n";
                
            if (context.noMods && context.missingMods.size() == 0 && !context.preListingMods && !context.errors.contains(MelonLoaderError.incompatibleAssemblyError))
                error += "- You have no mods installed in your Mods folder\n";
            
            if (context.hasNonModErrors && context.errors.size() == 0) {
                error += "- There are some unidentified errors. Please wait for a moderator or a helper to manually check the file.\n";
                context.unidentifiedErrors = true;
            }
            
            if (error.length() > 0) {
                context.embedBuilder.addField("Other Errors:", error , false);
                context.embedColor = Color.RED;
            }
            else if (context.mlVersion != null && context.loadedMods.size() == 0) {
                context.embedBuilder.addField("Partial Log:", "- MelonLoader either crashed or paused by the console being clicked on.\nPlease push any key on console to continue, reinstall MelonLoader, or verify integrity of your game." , false);
                context.embedColor = Color.ORANGE;
            }
            else
                return false;
            
            return true;
        }
        return false;
    }


    // Utils

    static void replyStandard(String message, Color color, MessageReceivedEvent messageReceivedEvent) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(color);
        embedBuilder.setDescription(message);
        MessageEmbed embed = embedBuilder.build();

        messageReceivedEvent.getMessage().reply(embed);
    }

    private static void reportUserModifiedML(MessageReceivedEvent event) {
        String reportChannel = CommandManager.mlReportChannels.get(event.getGuild().getIdLong()); // https://discord.com/channels/663449315876012052/663461849102286849/801676270974795787
        if (reportChannel != null) {
            event.getGuild().getTextChannelById(reportChannel).sendMessage(
                    JDAManager.wrapMessageInEmbed(
                            "User <@" + event.getMember().getId() + "> is using an unofficial MelonLoader.\nMessage: <https://discord.com/channels/" + event.getGuild().getId() + "/" + event.getChannel().getId() + "/" + event.getMessageId() + ">",
                            Color.orange)).queue();
        }
    }
    
    private static String sanitizeInputString(String input) {
        input = input
        .replace("](", " ")
        .replace("@", "@ ")
        .replace("*", "\\*")
        .replace("`", "\\`");
        
        input = Pattern.compile("(nigg(er|a)|porn)", Pattern.CASE_INSENSITIVE).matcher(input).replaceAll(Matcher.quoteReplacement("{REDACTED}"));
        
        input = input.substring(0, input.length() > 50 ? 50 : input.length()); // limit inputs to 50 chars
        
        return input;
    }

}
