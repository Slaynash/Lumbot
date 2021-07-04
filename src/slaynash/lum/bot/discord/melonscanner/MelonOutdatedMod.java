package slaynash.lum.bot.discord.melonscanner;

public class MelonOutdatedMod {
    String name;
    String newName;
    String currentVersion;
    String latestVersion;
    String downloadUrl;

    public MelonOutdatedMod(String name, String newName, String currentVersion, String latestVersion, String downloadUrl) {
        this.name = name;
        this.newName = newName;
        this.currentVersion = currentVersion;
        this.latestVersion = latestVersion;
        this.downloadUrl = downloadUrl;
    }
}