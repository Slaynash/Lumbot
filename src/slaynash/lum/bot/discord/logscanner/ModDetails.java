package slaynash.lum.bot.discord.logscanner;

public class ModDetails {
    public String name;
    public ModVersion[] versions;
    public String downloadLink;

    public ModDetails(String name, ModVersion[] versions, String downloadLink) {
        this.name = name;
        this.versions = versions;
        this.downloadLink = downloadLink;
    }
    
    public ModDetails(String name, String version, String downloadLink) {
        this.name = name;
        this.versions = new ModVersion[] {new ModVersion(version, null)};
        this.downloadLink = downloadLink;
    }
    
    public String getName() {
        return name;
    }
}
