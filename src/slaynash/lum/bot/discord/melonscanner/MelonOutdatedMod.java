package slaynash.lum.bot.discord.melonscanner;

import com.github.zafarkhaja.semver.Version;

public class MelonOutdatedMod {
    final String name;
    final String newName;
    final Version currentVersion;
    final Version latestVersion;
    final String downloadUrl;

    public MelonOutdatedMod(String name, String newName, Version currentVersion, Version latestVersion, String downloadUrl) {
        this.name = name;
        this.newName = newName;
        this.currentVersion = currentVersion;
        this.latestVersion = latestVersion;
        this.downloadUrl = downloadUrl;
    }
}