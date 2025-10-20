package slaynash.lum.bot.discord.melonscanner;

import java.awt.Color;
import java.io.BufferedReader;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.zafarkhaja.semver.Version;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.DBConnectionManagerLum;

public class MelonScanContext {
    public Version latestMLVersion = null;
    public Version latestMLNightly = null;
    public Version overrideMLVersion = null;

    public final Attachment attachment;
    public final MessageReceivedEvent messageReceivedEvent;
    public final String lang;

    // Read pass
    public BufferedReader bufferedReader;
    public int omittedLineCount = 0;
    public int retryCount = 0;
    public String nextLine = "";
    public String readLine = "";
    public String line = "";
    public String lastLine = "";
    public String secondlastLine = "";
    public int linesToSkip = 0;
    public int lineCount = 0;

    public boolean pirate = false;
    public boolean editedLog = false;
    public boolean consoleCopyPaste = false;
    public boolean modifiedML = false;
    public boolean addToChatty = false;
    public boolean mlCrashed = false;

    // MelonLoader infos
    public String mlName;
    public Version mlVersion;
    public String osType;
    public String mlHashCode;
    public String arch;
    public boolean pre3 = false;
    public boolean nightly = false;
    public boolean mono = false;
    public boolean il2Cpp = false;
    public boolean android = false;
    public boolean isMLOutdated = false;
    public boolean unidentifiedErrors = false;

    // Path infos
    public String corePath;
    public String gamePath;
    public String gameDataPath;
    public String gameAppPath;

    // Game Infos
    public String game;
    public Version gameBuild;
    public String gameBuildRaw;
    public boolean epic = false;

    // Mod listing - temp
    public boolean listingPlugins = false;
    public boolean preListingModsPlugins = false;
    public boolean listingModsPlugins = false;
    public int remainingModCount;
    public String tmpModName;
    public Version tmpModVersion;
    public String tmpModAuthor;
    public String tmpModHash;
    public String tmpModAssembly;

    // Mod listing - final
    public boolean noPlugins = false;
    public boolean noMods = false;
    public Map<String, LogsModDetails> loadedMods = new HashMap<>();
    public List<MelonDuplicateMod> duplicatedMods = new ArrayList<>();
    public final List<LogsModDetails> modAssemblies = new ArrayList<>();

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
    public List<MelonApiMod> modDetails;
    public boolean modApiFound = false;
    public final List<LogsModDetails> unknownMods = new ArrayList<>();
    public final List<String> brokenMods = new ArrayList<>();
    public final List<String> retiredMods = new ArrayList<>();
    public final List<MelonOutdatedMod> outdatedMods = new ArrayList<>();
    public final List<MelonOutdatedMod> outdatedPlugins = new ArrayList<>();
    public final List<MelonOutdatedMod> newerMods = new ArrayList<>();
    public final List<MelonApiMod> corruptedMods = new ArrayList<>();
    public final List<String> oldMods = new ArrayList<>();
    public final List<String> hasPendingMods = new ArrayList<>();
    public final List<String> badMods = new ArrayList<>();
    public final List<String> badPlugins = new ArrayList<>();

    public final EmbedBuilder embedBuilder = new EmbedBuilder();
    public final StringBuilder reportMessage = new StringBuilder();
    public Color embedColor = Color.BLUE;

    public MelonScanContext(Attachment attachment, MessageReceivedEvent messageReceivedEvent, String lang) {
        this.attachment = attachment;
        this.messageReceivedEvent = messageReceivedEvent;
        this.lang = lang;

        ResultSet rs = null;
        try {
            rs = DBConnectionManagerLum.sendRequest("SELECT * FROM `MLhash` WHERE Nightly = 0 AND Android = 0 ORDER BY `TS` DESC LIMIT 1");
            if (rs.next())
                this.latestMLVersion = Version.parse(rs.getString("Version"));
            else
                this.latestMLVersion = Version.of(6, 9, 0);
            DBConnectionManagerLum.closeRequest(rs);
            rs = DBConnectionManagerLum.sendRequest("SELECT * FROM `MLhash` WHERE Android = 0 ORDER BY `TS` DESC LIMIT 1");
            if (rs.next())
                this.latestMLNightly = Version.parse(rs.getString("Version"));
            else
                this.latestMLNightly = Version.of(6, 9, 0);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            DBConnectionManagerLum.closeRequest(rs);
        }
    }

}
