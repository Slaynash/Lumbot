package slaynash.lum.bot.discord.melonscanner;

import com.github.zafarkhaja.semver.Version;

public class LogsModDetails {
    public String name;
    public Version version;
    public String author;
    public String hash;
    public String assembly;

    public String id;

    public LogsModDetails(String hash, String assembly) {
        this.hash = hash;
        this.assembly = assembly;
    }

    public LogsModDetails(String name, Version version, String author) {
        this.name = name;
        this.version = version;
        this.author = author;
    }

    public LogsModDetails(String name, Version version, String author, String hash, String assembly) {
        this.name = name;
        this.version = version;
        this.author = author;
        this.hash = hash;
        this.assembly = assembly;
    }
}
