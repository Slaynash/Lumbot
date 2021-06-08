package slaynash.lum.bot.discord.melonscanner;

public class MelonApiMod {
    public String name;
    public ModVersion[] versions;
    public String downloadLink;
    public String[] aliases;

    public MelonApiMod(String name, ModVersion[] versions, String downloadLink, String[] aliases) {
        this.name = name;
        this.versions = versions;
        this.downloadLink = downloadLink;
        this.aliases = aliases;
    }
    
    public MelonApiMod(String name, String version, String downloadLink, String[] aliases) {
        this(name, new ModVersion[] {new ModVersion(version, null)}, downloadLink, aliases);
    }
    
    public MelonApiMod(String name, String version, String downloadLink) {
        this(name, version, downloadLink, new String[0]);
    }
    
    public String getName() {
        return name;
    }
}
