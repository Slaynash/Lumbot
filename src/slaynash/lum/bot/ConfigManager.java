package slaynash.lum.bot;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public final class ConfigManager {
    private ConfigManager() {}

    private static final String SETTINGS_FILE = ".settings.conf";
    private static boolean initialized = false;
	private static Properties properties = new Properties();

    public static String discordToken;

    public static String dbAddress;
	public static String dbPort;
	public static String dbDatabase;
	public static String dbLogin;
	public static String dbPassword;

    public static void init() {
        if(initialized)
			return;
		initialized = true;
		
		File settingsFile = new File(SETTINGS_FILE);
		
		if(!settingsFile.exists()) {
			System.err.println(SETTINGS_FILE + " missing");
			System.exit(-1);
		}

        try (FileInputStream fis = new FileInputStream(SETTINGS_FILE)) {
			
			properties.load(fis);

            discordToken = properties.getProperty("DISCORD_TOKEN");

            dbAddress = properties.getProperty("DB_ADDRESS");
			dbPort = properties.getProperty("DB_PORT");
			dbDatabase = properties.getProperty("DB_DATABASE");
			dbLogin = properties.getProperty("DB_LOGIN");
			dbPassword = properties.getProperty("DB_PASSWORD");

        } catch (IOException e) {
			e.printStackTrace();
		}
    }
}
