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
    public int retryCount = 0;
    public String readLine = "";
    public String line = "";
    public String lastLine = "";
    public String secondlastLine = "";

    public boolean pirate = false;
    public boolean editedLog = false;
    public boolean consoleCopyPaste = false;
    public boolean modifiedML = false;
    public boolean addToChatty = false;

    // MelonLoader infos
    public String mlName;
    public String mlVersion;
    public String mlHashCode;
    public boolean pre3 = false;
    public boolean alpha = false;
    public boolean mono = false;
    public boolean il2Cpp = false;
    public boolean isMLOutdated = false;
    public boolean unidentifiedErrors = false;

    // Path infos
    public String corePath;
    public String gamePath;
    public String gameDataPath;
    public String gameAppPath;

    // Game Infos
    public String game;
    public String gameBuild;

    // VRChat / emmVRC
    public String emmVRCVRChatBuild;
    public String emmVRCVersion;
    public int vrcmuMods = -1;

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
    public Map<String, LogsModDetails> loadedMods = new HashMap<>();
    public List<MelonDuplicateMod> duplicatedMods = new ArrayList<>();

    // Missing dependencies - temp
    public String currentMissingDependenciesMods = ""; //isn't used
    public boolean readingMissingDependencies = false;

    // Missing dependencies - final
    public final List<String> missingMods = new ArrayList<>();

    // Incompatibility - temp
    public String currentIncompatibleMods = "";
    public boolean readingIncompatibility = false;

    // Incompatibility - final
    public final List<MelonIncompatibleMod> incompatibleMods = new ArrayList<>();

    // Error handling
    public boolean hasErrors = false;
    public boolean hasNonModErrors = false;
    public boolean assemblyGenerationFailed = false;
    public final List<MelonLoaderError> errors = new ArrayList<>();
    public final List<String> modsThrowingErrors = new ArrayList<>();
    public final List<String> misplacedMods = new ArrayList<>();
    public final List<String> misplacedPlugins = new ArrayList<>();

    // Thinkering pass
    public boolean isMLOutdatedVRC = false;
    public boolean vrcmuModsMismatch = false;
    public boolean missingErrorHeader = false;
    public List<MelonApiMod> modDetails;
    public final List<LogsModDetails> unknownMods = new ArrayList<>();
    public final List<String> brokenMods = new ArrayList<>();
    public final List<MelonOutdatedMod> outdatedMods = new ArrayList<>();
    public final List<MelonOutdatedMod> outdatedPlugins = new ArrayList<>();
    public final List<MelonOutdatedMod> newerMods = new ArrayList<>();
    public final List<MelonApiMod> corruptedMods = new ArrayList<>();
    public final List<String> oldMods = new ArrayList<>();
    public final List<String> hasPendingMods = new ArrayList<>();
    /*
    public List<String> universalMods = new ArrayList<>();
    public Map<String, String> modAuthors = new HashMap<>();
    */

    public EmbedBuilder embedBuilder;
    public StringBuilder reportMessage;
    public Color embedColor = Color.BLUE;

    public MelonScanContext(Attachment attachment, MessageReceivedEvent messageReceivedEvent, String lang) {
        this.attachment = attachment;
        this.messageReceivedEvent = messageReceivedEvent;
        this.lang = lang;
    }

}
