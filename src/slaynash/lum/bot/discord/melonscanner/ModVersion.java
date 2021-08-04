package slaynash.lum.bot.discord.melonscanner;

public class ModVersion {
    public final VersionUtils.VersionData version;
    public final String hash;

    public ModVersion(String version, String hash) {
        this.version = VersionUtils.getVersion(version);
        this.hash = hash;
    }
}
