package slaynash.lum.bot;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import slaynash.lum.bot.utils.ExceptionUtils;

public final class ConfigManager {
    private ConfigManager() {
    }

    private static final String SETTINGS_FILE = ".settings.conf";
    private static boolean initialized = false;
    private static final Properties properties = new Properties();

    public static String discordToken;
    public static boolean mainBot;
    public static String pingURL;

    public static String dbAddress;
    public static String dbPort;
    public static String dbDatabaseLum;
    public static String dbDatabaseShortURL;
    public static String dbLogin;
    public static String dbPassword;

    public static String discordPrefix;
    public static String cvrmgBlacklist;

    public static String curseforgeApiKey;
    public static String gitHubApiKey;
    public static String animescheduleApiKey;

    public static String commitHash;

    public static void init() {
        if (initialized)
            return;
        initialized = true;

        File settingsFile = new File(SETTINGS_FILE);

        if (!settingsFile.exists()) {
            System.err.println(SETTINGS_FILE + " missing");
            System.exit(-1);
        }

        try (FileInputStream fis = new FileInputStream(SETTINGS_FILE)) {

            properties.load(fis);

            discordToken = properties.getProperty("DISCORD_TOKEN");
            mainBot = Boolean.parseBoolean(properties.getProperty("MAIN_BOT"));
            pingURL = properties.getProperty("PING_URL");

            dbAddress = properties.getProperty("DB_ADDRESS");
            dbPort = properties.getProperty("DB_PORT");
            dbDatabaseLum = properties.getProperty("DB_DATABASELUM");
            dbDatabaseShortURL = properties.getProperty("DB_DATABASESHORTURL");
            dbLogin = properties.getProperty("DB_LOGIN");
            dbPassword = properties.getProperty("DB_PASSWORD");

            discordPrefix = properties.getProperty("DISCORD_PREFIX");

            curseforgeApiKey = properties.getProperty("CURSEFORGE_API_KEY");
            gitHubApiKey = properties.getProperty("GitHub_API_KEY");
            animescheduleApiKey = properties.getProperty("ANIMESCHEDULE_API_KEY");

        }
        catch (IOException e) {
            ExceptionUtils.reportException("Failed to load config", e);
        }

        commitHash = "unknown";
        try {
            InputStream gitstream = Main.class.getResourceAsStream("/git.properties");
            if (gitstream != null) {
                java.util.Properties properties = new java.util.Properties();
                properties.load(gitstream);
                System.out.println("Lum hash: " + properties.getProperty("git.commit.id.abbrev"));
                System.out.println("Lum build time: " + properties.getProperty("git.build.time"));
                commitHash = properties.getProperty("git.commit.id.abbrev");
            }
            else
                System.err.println("Failed to load git.properties");
        }
        catch (IOException e) {
            ExceptionUtils.reportException("Failed to load git commit hash", e);
        }
    }
}
