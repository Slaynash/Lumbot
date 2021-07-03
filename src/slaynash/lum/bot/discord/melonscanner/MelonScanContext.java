package slaynash.lum.bot.discord.melonscanner;

import java.awt.Color;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class MelonScanContext {

    public final Attachment attachment;
    public final MessageReceivedEvent messageReceivedEvent;
    public final String lang;
    
    // Read pass

    public BufferedReader bufferedReader;

    public int omittedLineCount = 0;
    
    public boolean pirate = false;
    public boolean consoleCopyPaste = false;
    public boolean modifiedML = false;

    // MelonLoader infos
    public String mlName;
    public String mlVersion;
    public String mlHashCode;
    public boolean pre3 = false;
    public boolean alpha = false;

    // Game Infos
    public String game;
    public String gameBuild;

    // VRChat / emmVRC
    public String emmVRCVRChatBuild;
    public String emmVRCVersion;
    public String emmVRCHash;

    // Mod listing - temp
    public boolean preListingMods = false;
    public boolean listingMods = false;
    public int remainingModCount;
    public String tmpModName;
    public String tmpModVersion;
    public String tmpModAuthor;
    public String tmpModHash;

    // Mod listing - final
    public boolean noPlugins = false;
    public boolean noMods = false;
    public Map<String, LogsModDetails> loadedMods = new HashMap<String, LogsModDetails>();
    public List<MelonDuplicateMod> duplicatedMods = new ArrayList<MelonDuplicateMod>();

    // Missing dependencies - temp
    public String currentMissingDependenciesMods = ""; //isn't used
    public boolean readingMissingDependencies = false;

    // Missing dependencies - final
    public List<String> missingMods = new ArrayList<>();

    // Incompatibility - temp
    public String currentIncompatibleMods = "";
    public boolean readingIncompatibility = false;

    // Incompatibility - final
    public List<MelonIncompatibleMod> incompatibleMods = new ArrayList<MelonIncompatibleMod>();

    // Error handling
    public boolean hasErrors = false;
    public boolean hasNonModErrors = false;
    public boolean assemblyGenerationFailed = false;
    public List<MelonLoaderError> errors = new ArrayList<MelonLoaderError>();
    public List<String> modsThrowingErrors = new ArrayList<String>();
    public List<String> misplacedMods = new ArrayList<String>();
    public List<String> misplacedPlugins = new ArrayList<String>();

    // Thinkering pass

    public boolean isMLOutdatedVRC = false;

    public List<MelonApiMod> modDetails;

    public List<LogsModDetails> unknownMods = new ArrayList<LogsModDetails>();
    public List<String> brokenMods = new ArrayList<>();
    public List<MelonOutdatedMod> outdatedMods = new ArrayList<MelonOutdatedMod>();
    public List<MelonApiMod> corruptedMods = new ArrayList<MelonApiMod>();
    public List<String> oldMods = new ArrayList<String>();
    /*
    public List<String> universalMods = new ArrayList<String>();
    public Map<String, String> modAuthors = new HashMap<String, String>();
    */

    public EmbedBuilder embedBuilder;
    public StringBuilder reportMessage;
    public Color embedColor = Color.BLUE;

    public boolean isMLOutdated;
    public boolean unidentifiedErrors;
    
    public MelonScanContext(Attachment attachment, MessageReceivedEvent messageReceivedEvent, String lang) {
        this.attachment = attachment;
        this.messageReceivedEvent = messageReceivedEvent;
        this.lang = lang;
    }

}
