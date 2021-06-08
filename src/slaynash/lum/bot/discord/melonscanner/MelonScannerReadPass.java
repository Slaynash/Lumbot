package slaynash.lum.bot.discord.melonscanner;

import java.awt.Color;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;

public final class MelonScannerReadPass {

    public static boolean DoPass(MelonScanContext context) throws IOException, InterruptedException, ExecutionException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(context.attachment.retrieveInputStream().get()))) {
            context.bufferedReader = br;
            String line = "";
            String lastLine = "";
            while ((lastLine = line) != null && (line = br.readLine()) != null) {

                if (shouldOmitLineCheck(line, context)) {
                    line = "";
                    continue;
                }

                if (minecraftLogLineCheck(line, context))
                    return false;

                pirateCheck(line, context);

                if ((context.preListingMods || context.listingMods) && !context.pre3)
                    if (processML03ModListing(line, context))
                        continue;

                if (missingDependenciesCheck(line, context))
                    continue;

                if (context.readingMissingDependencies)
                    if (processMissingDependenciesListing(line, context))
                        continue;

                
                if (line.isBlank()) continue;

                if ((
                    mlVersionCheck(line, context) ||
                    gameNameCheck(line, context) ||
                    mlHashCodeCheck(line, context) ||
                    modPre3EndmodCheck(line, lastLine, context) ||
                    gameVersionCheck(line, context) ||
                    emmVRCVersionCheck(line, context) ||
                    modPreListingCheck(line, context) ||
                    duplicateCheck(line, context) ||
                    missingMLFileCheck(line, context)
                )) continue;


                if (!(unhollowerErrorCheck(line, context) || knownErrorCheck(line, context) || incompatibleAssemblyErrorCheck(line, context)))
                    unknownErrorCheck(line, context);

            }
        }
        catch (IOException | InterruptedException | ExecutionException exception) {
            throw exception;
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
            line.matches(".*NetQueue: Setting up.") ||
            line.matches("---- Minecraft Crash Report ----") ||
            line.matches(".*melon_slice.*") ||
            line.matches(".*Injecting coremod.*")
        ) {
            System.out.println("Minecraft Log detected");
            MelonScanner.replyStandard("This is not a server for Minecraft. You are in the wrong Discord server.", Color.red, context.messageReceivedEvent);
            return true;
        }
        return false;
    }

    private static void pirateCheck(String line, MelonScanContext context) {
        if (line.matches(".*Bloons.TD.6.*")) {
            System.out.println("Pirated BTD6 detected");
            context.pirate = true;
            //reportUserPiratedBTD(event);
        }
    }

    private static boolean processML03ModListing(String line, MelonScanContext context) throws IOException {
        if (line.isBlank())
            return true;
        
        else if (context.preListingMods && line.matches("\\[[0-9.:]+\\] ------------------------------"));
        else if (context.preListingMods && (line.matches("\\[[0-9.:]+\\]( \\[MelonLoader\\]){0,1} No Plugins Loaded!") || line.matches("\\[[0-9.:]+\\]( \\[MelonLoader\\]){0,1} No Mods Loaded!"))) {
            context.remainingModCount = 0;
            context.preListingMods = false;
            context.listingMods = false;
            if (line.contains("Plugins"))
                context.noPlugins = true;
            else
                context.noMods = true;
            
            System.out.println("No mod/plugins loaded for this pass");
            
            return true;
        }
        else if (context.preListingMods && (line.matches("\\[[0-9.:]+\\]( \\[MelonLoader\\]){0,1} [0-9]+ Plugins? Loaded") || line.matches("\\[[0-9.:]+\\]( \\[MelonLoader\\]){0,1} [0-9]+ Mods? Loaded"))) {
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
        else if (line.matches("\\[[0-9.:]+\\]( \\[MelonLoader\\]){0,1} by .*")) { // Skip author
            context.tmpModAuthor = line.split(" ")[2];
            return true;
        }
        else if (line.matches("\\[[0-9.:]+\\]( \\[MelonLoader\\]){0,1} SHA256 Hash: [a-zA-Z0-9]+")) {
            context.tmpModHash = line.split(" ")[3];
            return true;
        }
        // TODO handle incompatible mods
        else if (line.matches("\\[[0-9.:]+\\]( \\[MelonLoader\\]){0,1} ------------------------------")) {
            
            System.out.println("Found mod " + context.tmpModName + ", version is " + context.tmpModVersion + ", and hash is " + context.tmpModHash);
            
            if (context.loadedMods.containsKey(context.tmpModName) && !context.duplicatedMods.stream().anyMatch(d -> d.hasName(context.tmpModName)))
                context.duplicatedMods.add(new MelonDuplicateMod(context.tmpModName.trim()));
                
            context.loadedMods.put(context.tmpModName.trim(), new LogsModDetails(context.tmpModName, context.tmpModVersion, context.tmpModAuthor, context.tmpModHash));
            //if (tmpModAuthor != null)
            //    modAuthors.put(tmpModName.trim(), tmpModAuthor.trim());
            
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

    private static boolean processMissingDependenciesListing(String line, MelonScanContext context) throws IOException {
        if (line.matches("    - '.*'.*")) {
            String missingModName = line.split("'", 3)[1];
            if (!context.missingMods.contains(missingModName)) {
                if ("NKHook6".contains(missingModName) && !context.errors.contains(MelonLoaderError.nkh6))
                    context.errors.add(MelonLoaderError.nkh6);
                else 
                    context.missingMods.add(missingModName);
            }
            return true;
        }
        else if (line.matches("\\[[0-9.:]+\\] \\[Warning\\] Some mods are missing dependencies, which you may have to install\\.")) {
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


    private static boolean mlVersionCheck(String line, MelonScanContext context) {
        if (line.matches("\\[[0-9.:]+\\]( \\[MelonLoader\\]){0,1} Using v0\\..*")) {
            consoleCopypasteCheck(line, context);
            context.mlVersion = line.split("v")[1].split(" ")[0].trim();
            context.pre3 = true;
            System.out.println("ML " + context.mlVersion + " (< 0.3.0)");
            return true;
        }
        else if (line.matches("\\[[0-9.:]+\\]( \\[MelonLoader\\]){0,1} MelonLoader v0\\..*")) {
            consoleCopypasteCheck(line, context);
            context.mlVersion = line.split("v")[1].split(" ")[0].trim();
            context.alpha = line.toLowerCase().contains("alpha");
            System.out.println("ML " + context.mlVersion + " (>= 0.3.0). Alpha: " + context.alpha);
            return true;
        }
        return false;
    }

    private static boolean gameNameCheck(String line, MelonScanContext context) {
        if (line.matches("\\[[0-9.:]+\\]( \\[MelonLoader\\]){0,1} Name: .*")) {
            context.game = line.split(":", 4)[3].trim();
            System.out.println("Game: " + context.game);
            return true;
        }
        return false;
    }

    private static boolean mlHashCodeCheck(String line, MelonScanContext context) {
        if (line.matches("\\[[0-9.:]+\\]( \\[MelonLoader\\]){0,1} Hash Code: .*")) {
            context.mlHashCode = line.split(":", 4)[3].trim();
            System.out.println("Hash Code: " + context.mlHashCode);
            return true;
        }
        return false;
    }

    private static boolean modPre3EndmodCheck(String line, String lastLine, MelonScanContext context) {
        if (line.matches("\\[[0-9.:]+\\]( \\[MelonLoader\\]){0,1} Game Compatibility: .*")) {
            String modnameversionauthor = lastLine.split("\\[[0-9.:]+\\]( \\[MelonLoader\\]){0,1} ", 2)[1].split("\\((http[s]{0,1}:\\/\\/){0,1}[a-zA-Z0-9\\-]+\\.[a-zA-Z]{2,4}", 2)[0];
            String[] split2 = modnameversionauthor.split(" by ", 2);
            String author = split2.length > 1 ? split2[1] : null;
            String[] split3 = split2[0].split(" v", 2);
            String name = split3[0].isBlank() ? "" : split3[0];
            name = String.join("", name.split(".*[a-zA-Z0-9]\\.[a-zA-Z]{2,4}"));
            String version = split3.length > 1 ? split3[1] : null;

            context.loadedMods.put(name.trim(), new LogsModDetails(name, version, author, null));
            
            String compatibility = line.split("\\[[0-9.:]+\\]( \\[MelonLoader\\]){0,1} Game Compatibility: ", 2)[1];
            // TODO incompatible mod check
            //if (!compatibility.equals("Compatible") && !compatibility.equals("Universal"))
            //    context.incompatibleMods.add(name);
            
            System.out.println("Found mod " + name.trim() + ", version is " + version + ", compatibility is " + compatibility);
            return true;
        }
        return false;
    }

    private static boolean gameVersionCheck(String line, MelonScanContext context) {
        if (line.matches("\\[[0-9.:]+\\] Game Version:.*")) {
            context.gameBuild = line.split(":")[3].trim();
            System.out.println("Game version " + context.gameBuild);
            return true;
        }
        return false;
    }

    private static boolean emmVRCVersionCheck(String line, MelonScanContext context) {
        if (line.matches("\\[[0-9.:]+\\] \\[emmVRCLoader\\] VRChat build is.*")) {
            context.emmVRCVRChatBuild = line.split(":", 4)[3].trim();
            System.out.println("VRChat " + context.emmVRCVRChatBuild);
            return true;
        }
        else if (line.matches("\\[[0-9.:]+\\] \\[emmVRCLoader\\] You are running version .*")) {
            context.emmVRCVersion = line.split("version", 2)[1].trim();
            System.out.println("EmmVRC " + context.emmVRCVersion);
            return true;
        }
        return false;
    }

    public static boolean modPreListingCheck(String line, MelonScanContext context) {
        if (!context.pre3 && (line.matches("\\[[0-9.:]+\\] Loading Plugins...") || line.matches("\\[[0-9.:]+\\] Loading Mods..."))) {
            context.preListingMods = true;
            System.out.println("Starting to pre-list mods/plugins");
            return true;
        }
        return false;
    }

    private static boolean duplicateCheck(String line, MelonScanContext context) {
        if (line.matches("\\[[0-9.:]+\\] \\[ERROR\\] An item with the same key has already been added.*")) {
            System.out.println("Duplicate in Mods and Plugins");
            if (!context.duplicatedMods.stream().anyMatch(d -> d.hasName(context.tmpModName)))
                context.duplicatedMods.add(new MelonDuplicateMod(line.substring(line.lastIndexOf(":") + 2)));
            return true;
        }
        else if (line.matches("\\[[0-9.:]+\\] \\[WARNING\\] Duplicate File.*")) {
            System.out.println("Duplicate in Mods");
            if (!context.duplicatedMods.stream().anyMatch(d -> d.hasName(context.tmpModName)))
                context.duplicatedMods.add(new MelonDuplicateMod(line.substring(line.lastIndexOf("\\") + 1).split("[.]", 2)[0]));
            return true;
        }
        return false;
    }

    private static boolean missingMLFileCheck(String line, MelonScanContext context) {
        if (line.contains("System.IO.FileNotFoundException: Could not load file or assembly")) {
            if(context.missingMods.size() == 0 && !context.readingMissingDependencies && !context.pre3)
                if (!context.errors.contains(MelonLoaderError.mlMissing))
                    context.errors.add(MelonLoaderError.mlMissing); //Mod missing ML files
            return true;
        }
        return false;
    }


    private static boolean unhollowerErrorCheck(String line, MelonScanContext context) {
        for (MelonLoaderError knownError : MelonLoaderError.knownUnhollowerErrors) {
            if (line.matches(knownError.regex)) {
                if (!context.assemblyGenerationFailed && !context.errors.contains(knownError))
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
        for (MelonLoaderError knownError : MelonLoaderError.knownErrors) {
            if (line.matches(knownError.regex)) {
                if (!context.errors.contains(knownError))
                    context.errors.add(knownError);
                System.out.println("Found known error");
                context.hasErrors = true;
                return true;
            }
        }
        return false;
    }

    private static boolean incompatibleAssemblyErrorCheck(String line, MelonScanContext context) {
        if (line.matches("\\[[0-9.:]+\\] \\[ERROR\\] System.BadImageFormatException:.*")) {
            if (!context.errors.contains(MelonLoaderError.incompatibleAssemblyError))
                context.errors.add(MelonLoaderError.incompatibleAssemblyError);
            return true;
        }
        return false;
    }

    private static void unknownErrorCheck(String line, MelonScanContext context) {
        if (line.matches("\\[[0-9.:]+\\]( \\[MelonLoader\\]){0,1} \\[[^\\[]+\\] \\[(Error|ERROR)\\].*") && !line.matches("\\[[0-9.:]+\\] \\[MelonLoader\\] \\[(Error|ERROR)\\].*")) {
            String mod = line.split("\\[[0-9.:]+\\]( \\[MelonLoader\\]){0,1} \\[", 2)[1].split("\\]", 2)[0];
            if (!context.modsThrowingErrors.contains(mod))
                context.modsThrowingErrors.add(mod);
            System.out.println("Found mod error, caused by " + mod + ": " + line);
            context.hasErrors = true;
        }
        else if (line.matches("\\[[0-9.:]+\\]( \\[MelonLoader\\]){0,1} \\[(Error|ERROR)\\].*")) {
            context.hasErrors = true;
            context.hasNonModErrors = true;
            System.out.println("Found non-mod error: " + line);
        }
    }



    private static void consoleCopypasteCheck(String line, MelonScanContext context) {
        if (line.matches("\\[[0-9.:]+\\] \\[MelonLoader\\] .*"))
            context.consoleCopyPaste = true;
    }
    
}
