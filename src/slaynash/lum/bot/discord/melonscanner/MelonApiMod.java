package slaynash.lum.bot.discord.melonscanner;

public class MelonApiMod {
    public String name;
    public ModVersion[] versions;
    public String downloadLink;
    public String[] aliases;
    public String hash;

    public MelonApiMod(String name, ModVersion[] versions, String downloadLink, String[] aliases, String hash) {
        this.name = name;
        this.versions = versions;
        this.downloadLink = downloadLink;
        this.aliases = aliases;
        this.hash = hash;
    }
    
    public MelonApiMod(String name, String version, String downloadLink, String[] aliases, String hash) {
        this(name, new ModVersion[] {new ModVersion(version, null)}, downloadLink, aliases, hash);
    }

    public MelonApiMod(String name, String version, String downloadLink, String[] aliases) {
        this(name, new ModVersion[] {new ModVersion(version, null)}, downloadLink, aliases, null);
    }
    
    public MelonApiMod(String name, String version, String downloadLink) {
        this(name, version, downloadLink, new String[0], null);
    }

    public String getName() {
        return name;
    }
}
