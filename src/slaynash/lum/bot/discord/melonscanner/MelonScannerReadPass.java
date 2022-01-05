package slaynash.lum.bot.discord.melonscanner;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import slaynash.lum.bot.utils.ExceptionUtils;
import slaynash.lum.bot.utils.Utils;

public final class MelonScannerReadPass {

    public static boolean doPass(MelonScanContext context) throws IOException, InterruptedException, ExecutionException {
        if (context.attachment.getUrl().contains("/%")) {
            ExceptionUtils.reportException("PLEASE DO NOT USE CANARY, IT BROKY!!!!", null, null, context.messageReceivedEvent.getTextChannel());
            return false;
        }
        if (context.attachment.getSize() > 15000000)
            return false;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(context.attachment.retrieveInputStream().get()))) {
            context.bufferedReader = br;
            String line = "";
            String lastLine; ///String title, String comment, Throwable exception, TextChannel textChannel) {
            isMLOutdated = false;
            while ((lastLine = line) != null && (line = br.readLine()) != null) {

                if (shouldOmitLineCheck(line, context)) {
                    line = "";
                    continue;
                }

                if (minecraftLogLineCheck(line, context))
                    return false;

                if ((context.preListingMods || context.listingMods) && !context.pre3)
                    if (processML03ModListing(line, context))
                        continue;

                if (missingDependenciesCheck(line, context))
                    continue;

                if (context.readingMissingDependencies)
                    if (processMissingDependenciesListing(line, context))
                        continue;

                if (incompatibilityCheck(line, context))
                    continue;

                if (context.readingIncompatibility)
                    if (processIncompatibilityListing(line, context))
                        continue;

                if (line.isBlank()) continue;

                if (
                    mlVersionCheck(line, context) ||
                    gameNameCheck(line, context) ||
                    gamePathCheck(line, context) ||
                    mlHashCodeCheck(line, context) ||
                    modPre3EndmodCheck(line, lastLine, context) ||
                    gameVersionCheck(line, context) ||
                    emmVRCVersionCheck(line, context) ||
                    modPreListingCheck(line, context) ||
                    misplacedCheck(line, context) ||
                    duplicateCheck(line, context) ||
                    missingMLFileCheck(line, context) ||
                    oldModCheck(line, context)
                ) continue;

                if (compromisedMLCheck(line, context)) {
                    context.messageReceivedEvent.getMessage().delete().reason("Compromised Log file").queue();
                    return true;
                }

                if (!(unhollowerErrorCheck(line, context) || knownErrorCheck(line, context) || incompatibleAssemblyErrorCheck(line, context)))
                    unknownErrorCheck(line, context);

            }
        }
        finally {
            context.bufferedReader = null;
        }

        return true;
    }

    private static boolean shouldOmitLineCheck(String line, MelonScanContext context) {
        int linelength = line.length();
        if (linelength > 1000) {
            ++context.omittedLineCount;
            System.out.println("Omitted one line of length " + linelength);
            return true;
        }
        return false;
    }

    private static boolean minecraftLogLineCheck(String line, MelonScanContext context) {
        if (
            line.matches(".*Loading for game Minecraft.*") ||
            line.matches(".*NetQueue: Setting up.") ||
            line.matches("---- Minecraft Crash Report ----") ||
            line.contains("melon_slice") ||
            line.contains("Injecting coremod")
        ) {
            System.out.println("Minecraft Log detected");
            Utils.replyEmbed("This is not a server for Minecraft. You are in the wrong Discord server.", Color.red, context.messageReceivedEvent);
            return true;
        }
        return false;
    }

    private static void pirateCheck(String line, MelonScanContext context) {
        line = line.toLowerCase();
        if (line.contains("bloons.td.6") || line.contains("bloons.td6") || line.matches("\\[[0-9.:]+] \\[btd6e_module_helper] v[0-9.]+")) {
            System.out.println("Pirated BTD6 detected");
            context.pirate = true;
            //reportUserPiratedBTD(event);
        }
        else if (line.matches(".*\\\\boneworks.v\\d.*")) {
            System.out.println("Pirated BW detected");
            context.pirate = true;
        }
    }

    private static boolean processML03ModListing(String line, MelonScanContext context) throws IOException {
        if (line.isBlank())
            return true;

        else if (context.preListingMods && line.matches("\\[[0-9.:]+] -{30}")) return true;
        else if (context.preListingMods && (line.matches("\\[[0-9.:]+]( \\[MelonLoader])? No Plugins Loaded!") || line.matches("\\[[0-9.:]+]( \\[MelonLoader])? No Mods Loaded!"))) {
            context.remainingModCount = 0;
            context.preListingMods = false;
            context.listingMods = false;
            if (line.contains("Plugins"))
                context.noPlugins = true;
            else
                context.noMods = true;

            System.out.println("No " + (context.noPlugins ? "plugins" : "mods") + " loaded for this pass");

            return true;
        }
        else if (context.preListingMods && (line.matches("\\[[0-9.:]+]( \\[MelonLoader])? [0-9]+ Plugins? Loaded") || line.matches("\\[[0-9.:]+]( \\[MelonLoader])? [0-9]+ Mods? Loaded"))) {
            context.remainingModCount = Integer.parseInt(line.split(" ")[1]);
            context.preListingMods = false;
            context.listingMods = true;
            System.out.println(context.remainingModCount + " mods or plugins loaded on this pass");
            context.bufferedReader.readLine(); // Skip line separator

            return true;
        }
        else if (context.listingMods && context.tmpModName == null) {
            String[] split = line.split(" ", 2)[1].split(" v", 2);
            context.tmpModName = ("".equals(split[0])) ? "Broken Mod" : split[0];
            context.tmpModVersion = split.length > 1 ? split[1] : null;
            /*
            String matchedName = MelonLoaderScanner.modNameMatcher.get(context.tmpModName.trim());
            context.tmpModName = (matchedName != null) ? matchedName : context.tmpModName;
            */
            return true;
        }
        else if (line.matches("\\[[0-9.:]+]( \\[MelonLoader])? by .*")) { // Skip author
            String[] temp = line.split(" ", 3);
            if (temp.length > 2)
                context.tmpModAuthor = temp[2];
            else
                context.tmpModAuthor = "Broken Author";
            return true;
        }
        else if (line.matches("\\[[0-9.:]+]( \\[MelonLoader])? SHA256 Hash: [a-zA-Z0-9]+")) {
            context.tmpModHash = line.split(" ")[3];
            return true;
        }
        else if (line.matches("\\[[0-9.:]+]( \\[MelonLoader])? -{30}")) {

            System.out.println("Found mod " + context.tmpModName + ", version is " + context.tmpModVersion + ", and hash is " + context.tmpModHash);

            if (!"Backwards Compatibility Plugin".equalsIgnoreCase(context.tmpModName)) { //ignore BCP, it is part of ModThatIsNotMod
                if (context.loadedMods.containsKey(context.tmpModName) && context.duplicatedMods.stream().noneMatch(d -> d.hasName(context.tmpModName)))
                    context.duplicatedMods.add(new MelonDuplicateMod(context.tmpModName.trim()));
                context.loadedMods.put(context.tmpModName.trim(), new LogsModDetails(context.tmpModName, context.tmpModVersion, context.tmpModAuthor, context.tmpModHash));
                //if (tmpModAuthor != null)
                //    modAuthors.put(tmpModName.trim(), tmpModAuthor.trim());
            }

            context.tmpModName = null;
            context.tmpModVersion = null;
            context.tmpModAuthor = null;
            context.tmpModHash = null;

            --context.remainingModCount;

            if (context.remainingModCount == 0) {
                context.preListingMods = false;
                context.listingMods = false;
                System.out.println("Done scanning mods");

                return true;
            }
        }

        return false;
    }

    private static boolean missingDependenciesCheck(String line, MelonScanContext context) {
        if (line.matches("- '.*' is missing the following dependencies:")) {
            context.currentMissingDependenciesMods = line.split("'", 3)[1];
            context.readingMissingDependencies = true;
            return true;
        }
        return false;
    }

    private static boolean incompatibilityCheck(String line, MelonScanContext context) {
        if (line.matches("- '.*' is incompatible with the following Melons:")) {
            context.currentIncompatibleMods = line.split("'", 3)[1];
            context.readingIncompatibility = true;
            return true;
        }
        return false;
    }

    private static boolean oldModCheck(String line, MelonScanContext context) {
        if (line.matches("\\[[0-9.:]+] \\[ERROR] Failed to Resolve Melons for.*")) {
            if (line.contains("Could not load file or assembly '")) {
                String missingName = line.split("Could not load file or assembly '")[1].split(",")[0];
                if (!context.missingMods.contains(missingName))
                    context.missingMods.add(missingName);
                return true;
            }
            else if (line.toLowerCase().contains("exception")) {
                String erroringName = splitName(line);
                if (!context.modsThrowingErrors.contains(erroringName))
                    context.modsThrowingErrors.add(erroringName);
                return true;
            }
            else {
                String oldName = splitName(line);
                if (!context.oldMods.contains(oldName))
                    context.oldMods.add(oldName);
                return true;
            }
        }

        if (line.matches(".*BloonsTD6.*No Compatibility Layer Found!")) {
            if (!context.errors.contains(MelonLoaderError.btd6mh))
                context.errors.add(MelonLoaderError.btd6mh);
            return true;
        }

        if (line.matches("\\[[0-9.:]+] \\[ERROR] No MelonInfoAttribute Found in.*") || line.matches("\\[[0-9.:]+] \\[ERROR] Failed to Load Assembly for.*") || line.matches("\\[[0-9.:]+] \\[ERROR] Invalid Author given to MelonInfoAttribute.*")) {
            String oldName = splitName(line);
            if (oldName.equalsIgnoreCase("Facepunch Steamworks Win64"))
                context.errors.add(new MelonLoaderError("", "Please move Facepunch.Steamworks.Win64.dll into the Managed folder."));
            else if (!context.oldMods.contains(oldName))
                context.oldMods.add(oldName);
            return true;
        }
        return false;
    }

    private static String splitName(String line) {
        line = line.split(".dll", 2)[0]; //remove everything to the right
        String[] split = line.split("\\\\"); //remove everything to the left
        split = split[split.length - 1].split("\\."); //split on dots
        return String.join(" ", Arrays.copyOfRange(split, 0, split.length)); //replace dots with spaces;
    }

    private static boolean processMissingDependenciesListing(String line, MelonScanContext context) throws IOException {
        if (line.matches(" {4}- '.*'.*")) {
            String missingModName = line.split("'", 3)[1];
            if (!context.missingMods.contains(missingModName)) {
                if ("NKHook6".contains(missingModName) && !context.errors.contains(MelonLoaderError.nkh6))
                    context.errors.add(MelonLoaderError.nkh6);
                else
                    context.missingMods.add(missingModName);
            }
            return true;
        }
        else if (line.matches("- '.*' is missing the following dependencies:")) {
            context.currentMissingDependenciesMods = line.split("'", 3)[1];
            return true;
        }
        else if (line.matches("\\[[0-9.:]+] \\[Warning] Some mods are missing dependencies, which you may have to install\\.")) { //TODO check if warning is all caps
            System.out.println("Starting to list missing dependencies");
            context.readingMissingDependencies = true;
            context.bufferedReader.readLine(); // If these are optional dependencies, mark them as optional using the MelonOptionalDependencies attribute.
            context.bufferedReader.readLine(); // This warning will turn into an error and mods with missing dependencies will not be loaded in the next version of MelonLoader.
        }
        else {
            System.out.println("Done listing missing dependencies on line: " + line);
            context.readingMissingDependencies = false;
        }

        return false;
    }

    private static boolean processIncompatibilityListing(String line, MelonScanContext context) {
        if (line.matches(" {4}- '.*'.*")) {
            String incompatibleModName = line.split("'", 3)[1];
            context.incompatibleMods.add(new MelonIncompatibleMod(incompatibleModName, context.currentIncompatibleMods));
            return true;
        }
        else if (line.matches("- '.*' is incompatible with the following Melons:")) {
            context.currentIncompatibleMods = line.split("'", 3)[1];
            return true;
        }
        else {
            System.out.println("Done listing Incompatibility on line: " + line);
            context.readingIncompatibility = false;
        }

        return false;
    }

    private static boolean mlVersionCheck(String line, MelonScanContext context) {
        if (line.matches("\\[[0-9.:]+]( \\[MelonLoader])? Using v0\\..*")) {
            consoleCopypasteCheck(line, context);
            context.mlVersion = line.split("v")[1].split(" ")[0].trim();
            context.pre3 = true;
            System.out.println("ML " + context.mlVersion + " (< 0.3.0)");
            return true;
        }
        else if (line.matches("\\[[0-9.:]+]( \\[MelonLoader])? MelonLoader v0\\..*")) {
            consoleCopypasteCheck(line, context);
            context.mlVersion = line.split("v")[1].split(" ")[0].trim();
            context.alpha = line.toLowerCase().contains("alpha");
            System.out.println("ML " + context.mlVersion + " (>= 0.3.0). Alpha: " + context.alpha);
            isMLOutdated = context.mlVersion != null && !(context.mlVersion.equals(MelonScanner.latestMLVersionRelease) || context.mlVersion.equals(MelonScanner.latestMLVersionAlpha) && VersionUtils.compareVersion(MelonScanner.latestMLVersionAlpha, MelonScanner.latestMLVersionRelease) == 1/* If Alpha is more recent */);
            return true;
        }
        return false;
    }

    private static boolean gameNameCheck(String line, MelonScanContext context) {
        if (line.matches("\\[[0-9.:]+]( \\[MelonLoader])? Name: .*")) {
            context.game = line.split(":", 4)[3].trim();
            System.out.println("Game: " + context.game);
            return true;
        }
        return false;
    }

    private static boolean gamePathCheck(String line, MelonScanContext context) {
        pirateCheck(line, context);
        if (line.contains("Core::BasePath")) {
            context.corePath = line.split("=", 2)[1].trim();
            return true;
        }
        else if (line.contains("Game::BasePath")) {
            context.gamePath = line.split("=", 2)[1].trim();
            return true;
        }
        else if (line.contains("Game::DataPath")) {
            context.gamePath = line.split("=", 2)[1].trim();
            return true;
        }
        else if (line.contains("Game::ApplicationPath")) {
            context.gamePath = line.split("=", 2)[1].trim();
            return true;
        }
        return false;
    }

    private static boolean mlHashCodeCheck(String line, MelonScanContext context) {
        if (line.matches("\\[[0-9.:]+]( \\[MelonLoader])? Hash Code: .*")) {
            context.mlHashCode = line.split(":", 4)[3].trim();
            System.out.println("Hash Code: " + context.mlHashCode);
            return true;
        }
        return false;
    }

    private static boolean modPre3EndmodCheck(String line, String lastLine, MelonScanContext context) {
        if (line.matches("\\[[0-9.:]+]( \\[MelonLoader])? Game Compatibility: .*")) {
            String modnameversionauthor = lastLine.split("\\[[0-9.:]+]( \\[MelonLoader])? ", 2)[1].split("\\((http[s]?://)?[a-zA-Z0-9\\-]+\\.[a-zA-Z]{2,4}", 2)[0];
            String[] split2 = modnameversionauthor.split(" by ", 2);
            String author = split2.length > 1 ? split2[1] : null;
            String[] split3 = split2[0].split(" v", 2);
            String name = split3[0].isBlank() ? "" : split3[0];
            name = String.join("", name.split(".*[a-zA-Z0-9]\\.[a-zA-Z]{2,4}"));
            String version = split3.length > 1 ? split3[1] : null;

            context.loadedMods.put(name.trim(), new LogsModDetails(name, version, author, null));

            String compatibility = line.split("\\[[0-9.:]+]( \\[MelonLoader])? Game Compatibility: ", 2)[1];
            // TODO incompatible mod check
            //if (!compatibility.equals("Compatible") && !compatibility.equals("Universal"))
            //    context.incompatibleMods.add(name);

            System.out.println("Found mod " + name.trim() + ", version is " + version + ", compatibility is " + compatibility);
            return true;
        }
        return false;
    }

    private static boolean gameVersionCheck(String line, MelonScanContext context) {
        if (line.matches("\\[[0-9.:]+] Game Version:.*")) {
            context.gameBuild = line.split(":")[3].trim();
            System.out.println("Game version " + context.gameBuild);
            return true;
        }
        return false;
    }

    private static boolean emmVRCVersionCheck(String line, MelonScanContext context) {
        if (line.matches("\\[[0-9.:]+] \\[emmVRCLoader] VRChat build is.*")) {
            context.emmVRCVRChatBuild = line.split(":", 4)[3].trim();
            System.out.println("VRChat " + context.emmVRCVRChatBuild);
            return true;
        }
        else if (line.matches("\\[[0-9.:]+] \\[emmVRCLoader] You are running version .*")) {
            context.emmVRCVersion = line.split("version", 2)[1].trim();
            System.out.println("EmmVRC " + context.emmVRCVersion);
            return true;
        }
        return false;
    }

    public static boolean modPreListingCheck(String line, MelonScanContext context) {
        if (!context.pre3 && (line.matches("\\[[0-9.:]+] Loading.*Plugins...") || line.matches("\\[[0-9.:]+] Loading.*Mods..."))) {
            context.preListingMods = true;
            System.out.println("Starting to pre-list " + (line.contains("Plugins") ? "plugins" : "mods"));
            return true;
        }
        return false;
    }

    private static boolean misplacedCheck(String line, MelonScanContext context) {
        if (line.contains("is in the Plugins Folder:")) {
            System.out.println("Misplaced Mod in Plugins");
            String modname = line.split("Mod ")[1].split(" is")[0].trim();
            if (!context.misplacedMods.contains(modname))
                context.misplacedMods.add(modname);
            return true;
        }
        else if (line.contains("is in the Mods Folder:")) {
            System.out.println("Misplaced Plugin in Mods");
            String pluginname = line.split("Plugin ")[1].split(" is")[0].trim();
            if (!context.misplacedPlugins.contains(pluginname))
                context.misplacedPlugins.add(pluginname);
            return true;
        }
        return false;
    }

    private static boolean duplicateCheck(String line, MelonScanContext context) {
        if (line.matches("\\[[0-9.:]+] \\[ERROR] An item with the same key has already been added.*")) {
            System.out.println("Duplicate in Mods and Plugins");
            String tmpModName = line.substring(line.lastIndexOf(":") + 2);
            if (context.duplicatedMods.stream().noneMatch(d -> d.hasName(tmpModName)))
                context.duplicatedMods.add(new MelonDuplicateMod(tmpModName));
            return true;
        }
        else if (line.matches("\\[[0-9.:]+] \\[(WARNING|ERROR)] Duplicate (File|Mod|Plugin).*")) {
            System.out.println("Duplicate in Mods");
            String tmpModName = line.substring(line.lastIndexOf("\\") + 1).replace(".dll", "");
            if (context.duplicatedMods.stream().noneMatch(d -> d.hasName(tmpModName)))
                context.duplicatedMods.add(new MelonDuplicateMod(tmpModName));
            return true;
        }
        return false;
    }

    private static boolean missingMLFileCheck(String line, MelonScanContext context) {
        if (line.contains("System.IO.FileNotFoundException: Could not load file or assembly") && !line.contains("IPA/Loader")) {
            if (context.missingMods.size() == 0 && !context.readingMissingDependencies && !context.pre3)
                if (!context.errors.contains(MelonLoaderError.mlMissing))
                    context.errors.add(MelonLoaderError.mlMissing); //Mod missing ML files
            return true;
        }
        return false;
    }

    private static boolean compromisedMLCheck(String line, MelonScanContext context) {
        if (line.contains("<Transmtn.Get GET api/1/auth/user>") || line.startsWith("authcookie_")) {
            if (!context.errors.contains(MelonLoaderError.mlCompromised))
                context.errors.add(MelonLoaderError.mlCompromised);
            return true;
        }
        return false;
    }

    private static boolean isMLOutdated;

    private static boolean unhollowerErrorCheck(String line, MelonScanContext context) {
        for (MelonLoaderError knownError : MelonLoaderError.getKnownUnhollowerErrors()) {
            if (line.matches(knownError.regex)) {
                if (!context.assemblyGenerationFailed && !context.errors.contains(knownError) && !isMLOutdated)
                    context.errors.add(knownError);
                System.out.println("Found known unhollower error");
                context.hasErrors = true;
                context.assemblyGenerationFailed = true;
                return true;
            }
        }
        return false;
    }

    private static boolean knownErrorCheck(String line, MelonScanContext context) {
        for (MelonLoaderError knownError : MelonLoaderError.getKnownErrors()) {
            if (line.matches(knownError.regex)) {
                if (!context.errors.contains(knownError))
                    context.errors.add(knownError);
                System.out.println("Found known error");
                context.hasErrors = true;
                return true;
            }
        }
        Map<String, List<MelonLoaderError>> gameSpecificErrors = MelonLoaderError.getGameSpecificErrors();
        if (context.game != null && gameSpecificErrors.containsKey(context.game)) {
            for (MelonLoaderError knownGameError : gameSpecificErrors.get(context.game)) {
                if (line.matches(knownGameError.regex)) {
                    if (!context.errors.contains(knownGameError))
                        context.errors.add(knownGameError);
                    System.out.println("Found known game error");
                    context.hasErrors = true;
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean incompatibleAssemblyErrorCheck(String line, MelonScanContext context) {
        if (line.matches("\\[[0-9.:]+] \\[ERROR] System.BadImageFormatException:.*")) {
            if (!context.errors.contains(MelonLoaderError.incompatibleAssemblyError))
                context.errors.add(MelonLoaderError.incompatibleAssemblyError);
            return true;
        }
        return false;
    }

    private static void unknownErrorCheck(String line, MelonScanContext context) {
        if (line.matches("\\[[0-9.:]+]( \\[MelonLoader])? \\[[^\\[]+] \\[(Error|ERROR)].*") && !line.matches("\\[[0-9.:]+] \\[MelonLoader] \\[(Error|ERROR)].*")) {
            String mod = line.split("\\[[0-9.:]+]( \\[MelonLoader])? \\[", 2)[1].split("]", 2)[0].replace("_", " ");
            if (!context.modsThrowingErrors.contains(mod))
                context.modsThrowingErrors.add(mod);
            //System.out.println("Found mod error, caused by " + mod + ": " + line);
            context.hasErrors = true;
        }
        else if (line.matches("\\[[0-9.:]+]( \\[MelonLoader])? \\[(Error|ERROR)].*")) {
            context.hasErrors = true;
            context.hasNonModErrors = true;
            //System.out.println("Found non-mod error: " + line);
        }
        else if (line.startsWith("  at ")) {
            String[] modt = line.substring(5).split("\\.");
            if (modt.length == 0 || context.game == null)
                return;
            String mod = modt[0];
            List<MelonApiMod> mods = MelonScannerApisManager.getMods(context.game);
            if (mods != null) {
                if (mods.stream().anyMatch(m -> m.name.equalsIgnoreCase(mod))) {
                    if (!context.modsThrowingErrors.contains(mod))
                        context.modsThrowingErrors.add(mod);
                }
                else {
                    Optional<MelonApiMod> aliasedMod = mods.stream().filter(m -> m.aliases != null && Arrays.asList(m.aliases).contains(mod)).findFirst();
                    if (aliasedMod.isPresent() && !context.modsThrowingErrors.contains(aliasedMod.get().name))
                        context.modsThrowingErrors.add(aliasedMod.get().name);
                }
            }
        }
    }

    private static void consoleCopypasteCheck(String line, MelonScanContext context) {
        if (line.matches("\\[[0-9.:]+] \\[MelonLoader] .*"))
            context.consoleCopyPaste = true;
    }
}
