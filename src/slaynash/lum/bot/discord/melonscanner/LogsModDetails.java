package slaynash.lum.bot.discord.melonscanner;

public class LogsModDetails {
    public final String name;
    public String version;
    public final String author;
    public final String hash;

    public String id;

    public LogsModDetails(String name, String version, String author, String hash) {
        this.name = name;
        this.version = version;
        this.author = author;
        this.hash = hash;
    }
}
