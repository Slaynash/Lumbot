package slaynash.lum.bot.discord.melonscanner;

public class MelonApiMod {
    public final String id;
    public final String name;
    public final ModVersion[] versions;
    public final String downloadLink;
    public final String[] aliases;
    public final String modtype;
    public final boolean haspending;
    public final boolean isbroken;

    public MelonApiMod(String id, String name, ModVersion[] versions, String downloadLink, String[] aliases, String modtype, boolean haspending, boolean isbroken) {
        this.id = id;
        this.name = name;
        this.versions = versions;
        this.downloadLink = downloadLink;
        this.aliases = aliases;
        this.modtype = modtype;
        this.haspending = haspending;
        this.isbroken = isbroken;
    }

    public MelonApiMod(String id, String name, String version, String downloadLink, String[] aliases, String hash, String modtype, boolean haspending, boolean isbroken) {
        this(id, name, new ModVersion[] {new ModVersion(version, hash)}, downloadLink, aliases, modtype, haspending, isbroken);
    }

    public MelonApiMod(String id, String name, String version, String downloadLink, String[] aliases) {
        this(id, name, new ModVersion[] {new ModVersion(version, null)}, downloadLink, aliases, "", false, false);
    }

    public String getName() {
        return name;
    }
}
