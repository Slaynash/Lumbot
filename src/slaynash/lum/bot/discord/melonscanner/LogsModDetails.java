package slaynash.lum.bot.discord.melonscanner;

public class LogsModDetails {
    public String name;
    public String version;
    public String author;
    public String hash;
    public String assembly;

    public String id;

    public LogsModDetails(String hash, String assembly) {
        this.hash = hash;
        this.assembly = assembly;
    }

    public LogsModDetails(String name, String version, String author, String hash) {
        this.name = name;
        this.version = version;
        this.author = author;
        this.hash = hash;
    }

    public LogsModDetails(String name, String version, String author, String hash, String assembly) {
        this.name = name;
        this.version = version;
        this.author = author;
        this.hash = hash;
        this.assembly = assembly;
    }
}
