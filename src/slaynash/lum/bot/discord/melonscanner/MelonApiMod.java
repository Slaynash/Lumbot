package slaynash.lum.bot.discord.melonscanner;

import com.github.zafarkhaja.semver.Version;

public record MelonApiMod(String id, String name, ModVersion[] versions, String downloadLink, String[] aliases,
                          String modtype, boolean haspending, boolean isbroken)
{

    public MelonApiMod(String id, String name, Version version, String downloadLink, String[] aliases, String hash, String modtype, boolean haspending, boolean isbroken) {
        this(id, name, new ModVersion[]{new ModVersion(version, hash)}, downloadLink, aliases, modtype, haspending, isbroken);
    }

    public MelonApiMod(String id, String name, Version version, String downloadLink, String[] aliases) {
        this(id, name, new ModVersion[] {new ModVersion(version, null)}, downloadLink, aliases, "", false, false);
    }
}
