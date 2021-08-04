package slaynash.lum.bot.discord.melonscanner;

public class MelonOutdatedMod {
    final String name;
    final String newName;
    final String currentVersion;
    final String latestVersion;
    final String downloadUrl;

    public MelonOutdatedMod(String name, String newName, String currentVersion, String latestVersion, String downloadUrl) {
        this.name = name;
        this.newName = newName;
        this.currentVersion = currentVersion;
        this.latestVersion = latestVersion;
        this.downloadUrl = downloadUrl;
    }
}