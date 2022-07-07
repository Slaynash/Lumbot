package slaynash.lum.bot;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import slaynash.lum.bot.utils.ExceptionUtils;

public final class ConfigManager {
    private ConfigManager() {
    }

    private static final String SETTINGS_FILE = ".settings.conf";
    private static boolean initialized = false;
    private static final Properties properties = new Properties();

    public static String discordToken;

    public static String dbAddress;
    public static String dbPort;
    public static String dbDatabaseLum;
    public static String dbDatabaseShortURL;
    public static String dbLogin;
    public static String dbPassword;

    public static String discordPrefix;
    public static String vrcmgBlacklist;

    public static String curseforgeApiKey;

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

            dbAddress = properties.getProperty("DB_ADDRESS");
            dbPort = properties.getProperty("DB_PORT");
            dbDatabaseLum = properties.getProperty("DB_DATABASELUM");
            dbDatabaseShortURL = properties.getProperty("DB_DATABASESHORTURL");
            dbLogin = properties.getProperty("DB_LOGIN");
            dbPassword = properties.getProperty("DB_PASSWORD");

            discordPrefix = properties.getProperty("DISCORD_PREFIX");
            vrcmgBlacklist = properties.getProperty("VRCMG_BLACKLIST");

            curseforgeApiKey = properties.getProperty("CURSEFORGE_API_KEY");

        } catch (IOException e) {
            ExceptionUtils.reportException("Failed to load config", e);
        }
    }
}
