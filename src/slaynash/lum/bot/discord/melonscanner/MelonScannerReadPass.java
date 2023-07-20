package slaynash.lum.bot.discord.melonscanner;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import com.google.code.regexp.Matcher;
import com.google.code.regexp.Pattern;
import net.dv8tion.jda.api.Permission;
import slaynash.lum.bot.utils.Utils;

public final class MelonScannerReadPass {

    private static final int omitLineCount = 1200;

    public static boolean doPass(MelonScanContext context) throws IOException, InterruptedException, ExecutionException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(context.attachment.getProxy().download().get()))) {
            context.bufferedReader = br;
            context.nextLine = "";
            context.line = "";
            while (context.nextLine != null) {
                context.readLine = br.readLine();
                if (context.readLine != null && context.readLine.isBlank())
                    continue;

                if (++context.lineCount > 20000) {
                    context.embedBuilder.addField("Log too long", "Some parts of the log was not scanned.", false);
                    return true;
                }

                context.secondlastLine = context.lastLine;
                context.lastLine = context.line;
                context.line = context.nextLine;
                context.nextLine = context.readLine;
                if (context.linesToSkip > 0) {
                    context.linesToSkip--;
                    continue;
                }
                if (shouldOmitLineCheck(context))
                    continue;
                if (context.line.isBlank())
                    continue;

                if (minecraftLogLineCheck(context))
                    return false;
                if (compromisedMLCheck(context))
                    return true;

                if (missingDependenciesCheck(context))
                    continue;

                if (context.readingMissingDependencies)
                    if (processMissingDependenciesListing(context))
                        continue;

                if (incompatibilityCheck(context))
                    continue;

                if (context.readingIncompatibility)
                    if (processIncompatibilityListing(context))
                        continue;

                if ((context.preListingModsPlugins || context.listingModsPlugins) && !context.pre3)
                    if (processML03ModListing(context))
                        continue;

                if (
                    mlVersionCheck(context) ||
                    gameNameCheck(context) ||
                    gameTypeCheck(context) ||
                    gamePathCheck(context) ||
                    mlHashCodeCheck(context) ||
                    modPre3EndmodCheck(context) ||
                    gameVersionCheck(context) ||
                    modPreListingCheck(context) ||
                    misplacedCheck(context) ||
                    duplicateCheck(context) ||
                    oldModCheck(context) ||
                    missingMLFileCheck(context)
                ) continue;

                if (!(unhollowerErrorCheck(context) || knownErrorCheck(context) || incompatibleAssemblyErrorCheck(context)))
                    unknownErrorCheck(context);

            }
        }
        catch (Exception e) {
            if (context.retryCount >= 3) {
                throw e;
            }
            context.retryCount++;
            Thread.sleep(5000);
            doPass(context);
        }
        finally {
            context.bufferedReader = null;
        }

        return true;
    }

    private static boolean shouldOmitLineCheck(MelonScanContext context) {
        int linelength = context.line.length();
        if (linelength > omitLineCount) {
            ++context.omittedLineCount;
            System.out.println("Omitted one line of length " + linelength);
            return true;
        }
        return false;
    }

    private static boolean minecraftLogLineCheck(MelonScanContext context) {
        String line = context.line;
        if (
            line.matches(".*Loading for game Minecraft.*") ||
            line.matches(".*NetQueue: Setting up.") ||
            line.matches("---- Minecraft Crash Report ----") ||
            line.contains("melon_slice") ||
            line.contains("Injecting coremod")
        ) {
            System.out.println("Minecraft Log detected");
            if (context.messageReceivedEvent.getGuild().getIdLong() == 663449315876012052L) {
                Utils.replyEmbed("This is not a server for Minecraft. You are in the wrong Discord server.", Color.red, context.messageReceivedEvent);
                return true;
            }
        }
        return false;
    }

    private static void pirateCheck(MelonScanContext context) {
        String line = context.line;
        line = line.toLowerCase();
        if (line.contains("bloons.td.6") || line.contains("bloons.td6") || line.matches("\\[[\\d.:]+] \\[btd6e_module_helper] v[\\d.]+")) {
            System.out.println("Pirated BTD6 detected");
            context.pirate = true;
        }
        else if (line.matches(".*\\\\boneworks\\.v\\d.*")) {
            System.out.println("Pirated BW detected");
            context.pirate = true;
        }
        else if (line.matches(".*\\\\bonelab\\.v\\d.*")) {
            System.out.println("Pirated BL detected");
            context.pirate = true;
        }
    }

    private static boolean processML03ModListing(MelonScanContext context) {
        String line = context.line;
        if (line.isBlank())
            return true;

        else if (context.preListingModsPlugins && line.matches("\\[[\\d.:]+] -{30,}")) {
            return true;
        }
        else if (line.matches("\\[[\\d.:]+] \\[.*")) {
            return false; //some mod is printing in the middle of the mod listing
        }
        else if (context.nextLine != null && line.matches("\\[[\\d.:]+] -{30,}") && context.nextLine.matches("\\[[\\d.:]+] -{30,}")) { // If some idiot removes a mod but keeps the separator
            if (context.mlVersion != null && VersionUtils.compareVersion("0.5.5", context.mlVersion) > 0) {
                System.out.println("editedLog");
                context.editedLog = true;
            }
            return true;
        }
        else if (line.contains("Failed to load Melon '")) {
            if (line.contains("Melon is a Plugin")) {
                String pluginname = line.split("'")[1];
                if (!context.misplacedPlugins.contains(pluginname))
                    context.misplacedPlugins.add(pluginname);
            }
            else if (line.contains("Melon is a Mod")) {
                String modname = line.split("'")[1];
                if (!context.misplacedMods.contains(modname))
                    context.misplacedMods.add(modname);
            }
            return true;
        }
        else if (line.matches("\\[[\\d.:]+] Melon Assembly loaded: .*")) {
            context.preListingModsPlugins = false;
            context.listingModsPlugins = true;

            String assembly = null;
            String hash = null;

            String[] temp = context.line.substring(0, context.line.length() - 1).split("\\\\");
            assembly = temp[temp.length - 1];
            Matcher hashRegex = Pattern.compile(".*Hash: '(.*)'").matcher(context.nextLine);
            if (hashRegex.matches()) {
                hash = hashRegex.group(1);
            }

            if (assembly != null && hash != null) {
                context.modAssemblies.add(new LogsModDetails(hash, assembly));
            }

            context.linesToSkip++;
            return true;
        }
        else if (line.matches("\\[[\\d.:]+]( \\[MelonLoader])? (No|0) (Mod|Plugin)s? [lL]oaded.?")) {
            context.remainingModCount = 0;
            context.preListingModsPlugins = false;
            context.listingModsPlugins = false;
            if (line.contains("Plugins"))
                context.noPlugins = true;
            else
                context.noMods = true;

            System.out.println("No " + (context.noPlugins ? "plugins" : "mods") + " loaded for this pass");

            return true;
        }
        else if (line.matches("\\[[\\d.:]+]( \\[MelonLoader])? \\d+ (Mod|Plugin)s? [lL]oaded.?")) {
            String[] split = line.split(" ");
            if (split.length < 2) return true;
            context.remainingModCount = Integer.parseInt(split[1]);
            context.listingPlugins = line.contains("Plugin");
            context.preListingModsPlugins = false;
            context.listingModsPlugins = context.mlVersion != null && VersionUtils.compareVersion("0.5.5", context.mlVersion) > 0;
            if (context.listingModsPlugins && context.remainingModCount <= 0) {
                context.editedLog = true;
            }
            System.out.println(context.remainingModCount + " " + (context.listingPlugins ? "plugins" : "mods") + " loaded on this pass");
            context.linesToSkip += 1; // Skip line separator
            return true;
        }
        else if (context.listingModsPlugins && context.tmpModName == null && !line.matches("\\[[\\d.:]+] -{30,}")) {
            String[] split = line.split(" ", 2);
            if (split.length < 2) return true;
            split = split[1].split(" v", 2);
            context.tmpModName = ("".equals(split[0])) ? "Broken Mod" : split[0].trim();
            context.tmpModVersion = split.length > 1 ? split[1] : null;
            return true;
        }
        else if (line.matches("\\[[\\d.:]+]( \\[MelonLoader])? by .*")) {
            String[] split = line.split(" ", 3);
            if (split.length > 2)
                context.tmpModAuthor = split[2];
            else
                context.tmpModAuthor = "Broken Author";
            if (context.tmpModName == null) {
                context.editedLog = true;
            }
            return true;
        }
        else if (line.matches("\\[[\\d.:]+] SHA256 Hash: [a-zA-Z\\d]+")) {
            String[] split = line.split(" ");
            if (split.length < 4) return true;
            context.tmpModHash = split[3];
            return true;
        }
        else if (line.matches("\\[[\\d.:]+] Assembly: .*\\.dll")) {
            String[] split = line.split(" ");
            if (split.length < 3) return true;
            context.tmpModAssembly = split[2];
            return true;
        }
        else if (line.matches("\\[[\\d.:]+]( \\[MelonLoader])? -{30,}")) {

            if (context.tmpModName == null) {
                System.out.println("Mod/Plugin Name: null");
                return true;
            }

            System.out.println("Found mod " + context.tmpModName + ", version is " + context.tmpModVersion + ", and hash is " + context.tmpModHash);

            context.modAssemblies.stream().filter(m -> m.assembly.equalsIgnoreCase(context.tmpModAssembly)).findFirst().ifPresent(m -> context.tmpModHash = m.hash);

            if (!"Backwards Compatibility Plugin".equalsIgnoreCase(context.tmpModName)) { //ignore BCP, it is part of ModThatIsNotMod
                if (context.loadedMods.containsKey(context.tmpModName) && context.duplicatedMods.stream().noneMatch(d -> d.hasName(context.tmpModName)))
                    context.duplicatedMods.add(new MelonDuplicateMod(context.tmpModName.trim()));
                context.loadedMods.put(context.tmpModName.trim(), new LogsModDetails(context.tmpModName, context.tmpModVersion, context.tmpModAuthor, context.tmpModHash));
            }

            if (context.listingPlugins) {
                if (context.tmpModName != null && MelonScannerApisManager.badPlugin.stream().anyMatch(context.tmpModName::equalsIgnoreCase))
                    context.badPlugins.add(context.tmpModName);
                if (context.tmpModName != null && context.tmpModAuthor != null && MelonScannerApisManager.badAuthor.stream().anyMatch(context.tmpModAuthor.toLowerCase()::contains))
                    context.badPlugins.add(context.tmpModName);
            }
            else {
                if (context.tmpModName != null && MelonScannerApisManager.badMod.stream().anyMatch(context.tmpModName::equalsIgnoreCase))
                    context.badMods.add(context.tmpModName);
                if (context.tmpModName != null && context.tmpModAuthor != null && MelonScannerApisManager.badAuthor.stream().anyMatch(context.tmpModAuthor.toLowerCase()::contains))
                    context.badMods.add(context.tmpModName);
            }

            context.tmpModName = null;
            context.tmpModVersion = null;
            context.tmpModAuthor = null;
            context.tmpModHash = null;
            context.tmpModAssembly = null;

            if (context.mlVersion != null && VersionUtils.compareVersion("0.5.5", context.mlVersion) > 0 && --context.remainingModCount == 0) {
                context.preListingModsPlugins = false;
                context.listingModsPlugins = false;
                System.out.println("Done scanning mods");
            }
            return true;
        }

        return false;
    }

    private static boolean missingDependenciesCheck(MelonScanContext context) {
        if (context.line.matches("- '.*' is missing the following dependencies:")) {
            String[] split = context.line.split("'", 3);
            if (split.length < 2) return true;
            context.currentMissingDependenciesMods = split[1];
            context.readingMissingDependencies = true;
            return true;
        }
        return false;
    }

    private static boolean incompatibilityCheck(MelonScanContext context) {
        if (context.line.matches("- '.*' is incompatible with the following Melons:")) {
            String[] split = context.line.split("'", 3);
            if (split.length < 2) return true;
            context.currentIncompatibleMods = split[1];
            context.readingIncompatibility = true;
            return true;
        }
        return false;
    }

    private static boolean oldModCheck(MelonScanContext context) {
        String line = context.line;
        if (line.matches("\\[[\\d.:]+] \\[ERROR] Failed to Resolve Melons for.*")) {
            if (line.contains("Could not load file or assembly '")) {
                String[] split = line.split("Could not load file or assembly '");
                if (split.length < 2) return true;
                String missingName = split[1].split(",")[0];
                if (!context.missingMods.contains(missingName))
                    context.missingMods.add(missingName);
                return true;
            }
            else if (line.toLowerCase().contains("exception") && !line.toLowerCase().contains("typeloadexception")) {
                String erroringName = splitName(line).replace("_", " ");
                //TODO check if broken
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

        if ("BloonsTD6".equalsIgnoreCase(context.game) && !context.loadedMods.containsKey("BloonsTD6 Mod Helper") && line.matches(".*No Compatibility Layer Found!")) {
            if (!context.errors.contains(MelonLoaderError.btd6mh))
                context.errors.add(MelonLoaderError.btd6mh);
            return true;
        }

        if (line.matches("\\[[\\d.:]+] \\[ERROR] No MelonInfoAttribute Found in.*") || line.matches("\\[[\\d.:]+] \\[ERROR] Failed to Load Assembly for.*") || line.matches("\\[[\\d.:]+] \\[ERROR] Invalid Author given to MelonInfoAttribute.*") || line.matches("\\[[\\d.:]+] \\[WARNING] No Compatibility Layer for.*")) {
            String oldName = splitName(line);
            if (oldName.equalsIgnoreCase("Facepunch Steamworks Win64"))
                context.errors.add(new MelonLoaderError("Please move Facepunch.Steamworks.Win64.dll into the Managed folder."));
            else if (!context.oldMods.contains(oldName))
                context.oldMods.add(oldName);
            return true;
        }
        if (line.matches("\\[[\\d.:]+] \\[ERROR] Incompatible Platform Domain for Mod: .*\\\\(?<modname>\\w+).dll")) {
            String modname = splitName(line);
            context.errors.add(new MelonLoaderError(modname + " is incompatible with this game type. Are sure sure this is a mod for this game?"));
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

    private static boolean processMissingDependenciesListing(MelonScanContext context) {
        String line = context.line;
        if (line.matches(" {4}- '.*'.*")) {
            String[] split = line.split("'", 3);
            if (split.length < 2) return true;
            String missingModName = split[1];
            if (!context.missingMods.contains(missingModName)) {
                if ("NKHook6".contains(missingModName) && !context.errors.contains(MelonLoaderError.nkh6))
                    context.errors.add(MelonLoaderError.nkh6);
                else
                    context.missingMods.add(missingModName);
            }
            return true;
        }
        else if (line.matches("- '.*' is missing the following dependencies:")) {
            String[] split = line.split("'", 3);
            if (split.length < 2) return true;
            context.currentMissingDependenciesMods = split[1];
            return true;
        }
        else if (line.matches("(?i)\\[[\\d.:]+]( \\[Warning]|) Some (mods|Melons) are missing dependencies, which you may have to install\\.")) {
            System.out.println("Starting to list missing dependencies");
            context.readingMissingDependencies = true;
            context.linesToSkip += 2;
            // If these are optional dependencies, mark them as optional using the MelonOptionalDependencies attribute.
            // This warning will turn into an error and mods with missing dependencies will not be loaded in the next version of MelonLoader. TODO This will break Lum when ML does it
            return true;
        }
        else {
            System.out.println("Done listing missing dependencies on line: " + line);
            context.readingMissingDependencies = false;
        }

        return false;
    }

    private static boolean processIncompatibilityListing(MelonScanContext context) {
        String line = context.line;
        if (line.matches(" {4}- '.*'.*")) {
            String[] split = line.split("'", 3);
            if (split.length < 2) return true;
            String incompatibleModName = split[1];
            context.incompatibleMods.add(new MelonIncompatibleMod(incompatibleModName, context.currentIncompatibleMods));
            return true;
        }
        else if (line.matches("- '.*' is incompatible with the following Melons:")) {
            String[] split = line.split("'", 3);
            if (split.length < 2) return true;
            context.currentIncompatibleMods = split[1];
            return true;
        }
        else {
            System.out.println("Done listing Incompatibility on line: " + line);
            context.readingIncompatibility = false;
        }

        return false;
    }

    private static boolean mlVersionCheck(MelonScanContext context) {
        String line = context.line;
        if (line.matches("\\[[\\d.:]+]( \\[MelonLoader])? Using v0\\..*")) {
            consoleCopypasteCheck(context);
            String[] split = line.split("v");
            if (split.length < 2) return true;
            context.mlVersion = split[1].split(" ")[0].trim();
            context.pre3 = true;
            System.out.println("ML " + context.mlVersion + " (< 0.3.0)");
            return true;
        }
        else if (line.matches("\\[[\\d.:]+]( \\[MelonLoader])? MelonLoader v0\\..*")) {
            consoleCopypasteCheck(context);
            String[] split = line.split("v");
            if (split.length < 2) return true;
            context.mlVersion = split[1].split(" ")[0].trim();
            context.alpha = line.toLowerCase().contains("alpha");
            System.out.println("ML " + context.mlVersion + " (>= 0.3.0). Alpha: " + context.alpha);
            return true;
        }
        return false;
    }

    private static boolean gameNameCheck(MelonScanContext context) {
        if (context.line.matches("\\[[\\d.:]+]( \\[MelonLoader])?( Game)? Name: .*")) {
            String[] split = context.line.split(":", 4);
            if (split.length < 4) return true;
            context.game = split[3].trim();
            System.out.println("Game: " + context.game);
            return true;
        }
        return false;
    }

    private static boolean gameTypeCheck(MelonScanContext context) {
        if (context.line.matches("\\[[\\d.:]+] Game Type: .*")) {
            String[] split = context.line.split(":", 4);
            if (split.length < 4) return true;
            String type = split[3].trim();
            if (type.equalsIgnoreCase("Il2Cpp")) {
                context.il2Cpp = true;
                return true;
            }
            else if (type.toLowerCase().contains("mono")) {
                context.mono = true;
                return true;
            }
        }
        return false;
    }

    private static boolean gamePathCheck(MelonScanContext context) {
        String line = context.line;
        pirateCheck(context);
        String[] split = line.split("=", 2);
        if (split.length < 2) return false;
        if (line.contains("Core::BasePath")) {
            context.corePath = split[1].trim();
            if (context.corePath.contains("'")) {
                context.errors.add(new MelonLoaderError("Your path contains `'` and is know to break MelonLoader. Please move your game to a different directory without `'` in it."));
            }
            return true;
        }
        else if (line.contains("Game::BasePath")) {
            context.gamePath = split[1].trim();
            return true;
        }
        else if (line.contains("Game::DataPath")) {
            context.gamePath = split[1].trim();
            return true;
        }
        else if (line.contains("Game::ApplicationPath")) {
            context.gamePath = split[1].trim();
            return true;
        }
        return false;
    }

    private static boolean mlHashCodeCheck(MelonScanContext context) {
        String[] split = context.line.split(":", 4);
        if (split.length < 4) return false;
        if (context.line.matches("\\[[\\d.:]+]( \\[MelonLoader])? Hash Code: .*")) {
            context.mlHashCode = split[3].trim().toUpperCase();
            System.out.println("Hash Code: " + context.mlHashCode);
            return true;
        }
        if (context.line.matches("\\[[\\d.:]+] OS: .*")) {
            context.osType = split[3].trim();
            System.out.println("OS: " + context.osType);
            return true;
        }
        return false;
    }

    private static boolean modPre3EndmodCheck(MelonScanContext context) {
        if (context.line.matches("\\[[\\d.:]+]( \\[MelonLoader])? Game Compatibility: .*")) {
            String[] split = context.lastLine.split("\\[[\\d.:]+]( \\[MelonLoader])? ", 2);
            if (split.length < 2) return true;
            String modnameversionauthor = split[1].split("\\((https?://)?[a-zA-Z\\d\\-]+\\.[a-zA-Z]{2,4}", 2)[0];
            String[] split2 = modnameversionauthor.split(" by ", 2);
            String author = split2.length > 1 ? split2[1] : null;
            String[] split3 = split2[0].split(" v", 2);
            String name = split3[0].isBlank() ? "" : split3[0].trim();
            name = String.join("", name.split(".*[a-zA-Z\\d]\\.[a-zA-Z]{2,4}"));
            String version = split3.length > 1 ? split3[1] : null;

            context.loadedMods.put(name, new LogsModDetails(name, version, author, null));

            split = context.line.split("\\[[\\d.:]+]( \\[MelonLoader])? Game Compatibility: ", 2);
            if (split.length < 2) return true;
            String compatibility = split[1];
            // TODO incompatible mod check
            //if (!compatibility.equals("Compatible") && !compatibility.equals("Universal"))
            //    context.incompatibleMods.add(name);

            System.out.println("Found mod " + name + ", version is " + version + ", compatibility is " + compatibility);
            return true;
        }
        return false;
    }

    private static boolean gameVersionCheck(MelonScanContext context) {
        if (context.line.matches("\\[[\\d.:]+] Game Version:.*")) {
            String[] split = context.line.split(":");
            if (split.length == 3) {
                context.errors.add(new MelonLoaderError("Your Game Version is blank. Please verify that both " + context.game + " and MelonLoader are installed properly."));
                return true;
            }
            else if (split.length < 3) return true;
            context.gameBuild = split[3].trim();
            System.out.println("Game version " + context.gameBuild);
            return true;
        }
        return false;
    }

    public static boolean modPreListingCheck(MelonScanContext context) {
        if (!context.pre3 && (context.line.matches("\\[[\\d.:]+] Loading.*(Mod|Plugin)s...") || context.line.matches("\\[[\\d.:]+] Loading.*(Mod|Plugin)s from .*"))) {
            context.preListingModsPlugins = true;
            System.out.println("Starting to pre-list " + (context.line.contains("Plugins") ? "plugins" : "mods"));
            return true;
        }
        return false;
    }

    private static boolean misplacedCheck(MelonScanContext context) {
        String line = context.line;
        if (line.contains("is in the Plugins Folder:")) {
            System.out.println("Misplaced Mod in Plugins");
            String[] split = line.split("Mod ");
            if (split.length < 2) return true;
            String modname = split[1].split(" is")[0].trim();
            if (!context.misplacedMods.contains(modname))
                context.misplacedMods.add(modname);
            return true;
        }
        else if (line.contains("is in the Mods Folder:")) {
            System.out.println("Misplaced Plugin in Mods");
            String[] split = line.split("Plugin ");
            if (split.length < 2) return true;
            String pluginname = split[1].split(" is")[0].trim();
            if (!context.misplacedPlugins.contains(pluginname))
                context.misplacedPlugins.add(pluginname);
            return true;
        }
        return false;
    }

    private static boolean duplicateCheck(MelonScanContext context) {
        String line = context.line;
        if (line.matches("\\[[\\d.:]+] \\[ERROR] An item with the same key has already been added.*")) {
            System.out.println("Duplicate in Mods and Plugins");
            String tmpModName = line.substring(line.lastIndexOf(":") + 2);
            if (context.duplicatedMods.stream().noneMatch(d -> d.hasName(tmpModName)))
                context.duplicatedMods.add(new MelonDuplicateMod(tmpModName));
            return true;
        }
        else if (line.matches("\\[[\\d.:]+] \\[(WARNING|ERROR)] Duplicate (File|Mod|Plugin).*")) {
            System.out.println("Duplicate in Mods");
            String tmpModName = line.substring(line.lastIndexOf("\\") + 1).replace(".dll", "");
            if (context.duplicatedMods.stream().noneMatch(d -> d.hasName(tmpModName)))
                context.duplicatedMods.add(new MelonDuplicateMod(tmpModName));
            return true;
        }
        return false;
    }

    private static boolean missingMLFileCheck(MelonScanContext context) {
        if (!context.preListingModsPlugins && !context.listingModsPlugins)
            return false;
        if (context.line.contains("System.IO.FileNotFoundException: Could not load file or assembly") && !context.line.contains("IPA/Loader")) {
            if (context.missingMods.size() == 0 && !context.readingMissingDependencies && !context.pre3)
                if (!context.errors.contains(MelonLoaderError.mlMissing))
                    context.errors.add(MelonLoaderError.mlMissing); //Mod missing ML files
            return true;
        }
        return false;
    }

    private static boolean compromisedMLCheck(MelonScanContext context) {
        if (context.line.contains("<Transmtn.Get GET api") || context.line.startsWith("authcookie_")) {
            System.out.println("COMPROMISED ML");
            if (!context.errors.contains(MelonLoaderError.mlCompromised))
                context.errors.add(MelonLoaderError.mlCompromised);
            if (context.messageReceivedEvent.getGuild() != null && context.messageReceivedEvent.getGuild().getSelfMember().hasPermission(context.messageReceivedEvent.getChannel().asGuildMessageChannel(), Permission.MESSAGE_MANAGE))
                context.messageReceivedEvent.getMessage().delete().reason("Compromised Log file").queue();
            else
                context.messageReceivedEvent.getChannel().asGuildMessageChannel().sendMessage("This is a compromised MelonLoader log. I can not remove that log, please delete that log for your own safety").queue();
            return true;
        }
        return false;
    }

    private static boolean unhollowerErrorCheck(MelonScanContext context) {
        for (MelonLoaderError knownError : MelonLoaderError.getKnownUnhollowerErrors()) {
            if (knownError.nextLineRegex != null && context.nextLine != null && !context.nextLine.matches(knownError.nextLineRegex)) {
                continue;
            }
            if (knownError.previousLineRegex != null && context.lastLine != null && !context.lastLine.matches(knownError.previousLineRegex)) {
                continue;
            }
            String errorMess = knownError.error;
            Matcher m = Pattern.compile(knownError.regex).matcher(context.line);
            if (m.matches()) {
                if (m.namedGroups().size() > 0) {
                    for (Entry<String, String> entry : m.namedGroups().get(0).entrySet())
                        errorMess = errorMess.replace(entry.getKey(), entry.getValue());
                }
                MelonLoaderError newerror = new MelonLoaderError(knownError.regex, errorMess);
                if (context.errors != null && !context.assemblyGenerationFailed && !context.errors.contains(newerror)) {
                    System.out.println("Found known unhollower error " + knownError.error);
                    context.errors.add(newerror);
                    context.hasErrors = true;
                    context.assemblyGenerationFailed = true;
                }
                return true;
            }
        }
        return false;
    }

    private static boolean knownErrorCheck(MelonScanContext context) {
        for (MelonLoaderError knownError : MelonLoaderError.getKnownErrors()) {
            if (knownError.nextLineRegex != null && context.nextLine != null && !context.nextLine.matches(knownError.nextLineRegex)) {
                continue;
            }
            if (knownError.previousLineRegex != null && context.lastLine != null && !context.lastLine.matches(knownError.previousLineRegex)) {
                continue;
            }
            String errorMess = knownError.error;
            Matcher m = Pattern.compile(knownError.regex).matcher(context.line);
            if (m.matches()) {
                if (m.namedGroups().size() > 0) {
                    for (Entry<String, String> entry : m.namedGroups().get(0).entrySet())
                        errorMess = errorMess.replace(entry.getKey(), entry.getValue());
                }
                MelonLoaderError newerror = new MelonLoaderError(knownError.regex, errorMess);
                if (context.errors != null && !context.errors.contains(newerror)) {
                    System.out.println("Found known error " + knownError.error);
                    context.errors.add(newerror);
                    context.hasErrors = true;
                }
                return true;
            }
        }
        Map<String, List<MelonLoaderError>> gameSpecificErrors = MelonLoaderError.getGameSpecificErrors();
        if (context.game != null && gameSpecificErrors.containsKey(context.game)) {
            for (MelonLoaderError knownGameError : gameSpecificErrors.get(context.game)) {
                if (knownGameError.nextLineRegex != null && context.nextLine != null && !context.nextLine.matches(knownGameError.nextLineRegex)) {
                    continue;
                }
                if (knownGameError.previousLineRegex != null && context.lastLine != null && !context.lastLine.matches(knownGameError.previousLineRegex)) {
                    continue;
                }
                String errorMess = knownGameError.error;
                Matcher m = Pattern.compile(knownGameError.regex).matcher(context.line);
                if (m.matches()) {
                    if (m.namedGroups().size() > 0) {
                        for (Entry<String, String> entry : m.namedGroups().get(0).entrySet())
                            errorMess = errorMess.replace(entry.getKey(), entry.getValue());
                    }
                    MelonLoaderError newerror = new MelonLoaderError(knownGameError.regex, errorMess);
                    if (context.errors != null && !context.errors.contains(newerror)) {
                        System.out.println("Found known game error " + knownGameError.error);
                        context.errors.add(newerror);
                        context.hasErrors = true;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean incompatibleAssemblyErrorCheck(MelonScanContext context) {
        if (context.line.matches("\\[[\\d.:]+] \\[ERROR] System.BadImageFormatException:.*")) {
            if (!context.errors.contains(MelonLoaderError.incompatibleAssemblyError))
                context.errors.add(MelonLoaderError.incompatibleAssemblyError);
            return true;
        }
        return false;
    }

    private static void unknownErrorCheck(MelonScanContext context) {
        String line = context.line;
        if (line.matches("\\[[\\d.:]+]( \\[MelonLoader])? \\[[^\\[]+] \\[(Error|ERROR)].*") && !line.matches("\\[[\\d.:]+] \\[(MelonLoader|Il2CppAssemblyGenerator)] \\[(Error|ERROR)].*")) {
            String[] split = line.split("\\[[\\d.:]+]( \\[MelonLoader])? \\[", 2);
            if (split.length < 2) return;
            String mod = split[1].split("]", 2)[0].replace("_", " ");
            if (!context.modsThrowingErrors.contains(mod))
                context.modsThrowingErrors.add(mod);
            System.out.println("Found mod error, caused by " + mod + ": " + line);
            context.hasErrors = true;
        }
        else if (line.matches("\\[[\\d.:]+]( \\[MelonLoader])? \\[(Error|ERROR)].*")) {
            System.out.println("Found non-mod error: " + line);
            context.hasErrors = true;
            context.hasNonModErrors = true;
        }
        else if (line.startsWith("  at ")) {
            String[] modt = line.substring(5).split("\\.");
            if (modt.length == 0 || context.game == null)
                return;
            String mod = modt[0];
            if (context.loadedMods.get(mod) == null)
                return; // not a loaded mod
            List<MelonApiMod> mods = MelonScannerApisManager.getMods(context.game);
            if (mods != null) {
                if (mods.stream().anyMatch(m -> m.name.equalsIgnoreCase(mod))) {
                    if (!context.modsThrowingErrors.contains(mod.replace("_", " ")))
                        context.modsThrowingErrors.add(mod.replace("_", " "));
                }
                else {
                    Optional<MelonApiMod> aliasedMod = mods.stream().filter(m -> m.aliases != null && Arrays.asList(m.aliases).contains(mod)).findFirst();
                    if (aliasedMod.isPresent() && !context.modsThrowingErrors.contains(aliasedMod.get().name))
                        context.modsThrowingErrors.add(aliasedMod.get().name);
                }
            }
        }
        else if (line.toLowerCase().contains("[error]")) {
            System.out.println("Found random error: " + line);
            context.hasErrors = true;
            context.hasNonModErrors = true;
        }
    }

    private static void consoleCopypasteCheck(MelonScanContext context) {
        if (context.line.matches("\\[[\\d.:]+] \\[MelonLoader] .*"))
            context.consoleCopyPaste = true;
    }
}
