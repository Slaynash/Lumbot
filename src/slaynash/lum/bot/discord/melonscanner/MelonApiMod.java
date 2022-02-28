package slaynash.lum.bot.discord.melonscanner;

public class MelonApiMod {
    public final String name;
    public final ModVersion[] versions;
    public final String downloadLink;
    public final String[] aliases;
    public final String modtype;
    public final boolean haspending;

    public MelonApiMod(String name, ModVersion[] versions, String downloadLink, String[] aliases, String modtype, String haspending) {
        this.name = name;
        this.versions = versions;
        this.downloadLink = downloadLink;
        this.aliases = aliases;
        this.modtype = modtype;
        this.haspending = Boolean.parseBoolean(haspending);
    }

    public MelonApiMod(String name, String version, String downloadLink, String[] aliases, String hash, String modtype, String haspending) {
        this(name, new ModVersion[] {new ModVersion(version, hash)}, downloadLink, aliases, modtype, haspending);
    }

    public MelonApiMod(String name, String version, String downloadLink, String[] aliases) {
        this(name, new ModVersion[] {new ModVersion(version, null)}, downloadLink, aliases, "", "False");
    }

    public String getName() {
        return name;
    }
}
