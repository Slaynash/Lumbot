package slaynash.lum.bot.uvm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class UnityUtils {

    public static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static final String downloadPath = "/mnt/hdd3t/unity_versions";

    public static String getMonoManagedSubpath(String version) {
        String monoManagedSubpath = "win64_player_nondevelopment_mono/Data";

        if (version.startsWith("3.")) {
            monoManagedSubpath = "windows64standaloneplayer";
        }
        else if (version.startsWith("4.")) {
            if (version.startsWith("4.5") ||
                version.startsWith("4.6") ||
                version.startsWith("4.7"))
            {
                monoManagedSubpath = "win64_nondevelopment/Data";
            }
            else {
                monoManagedSubpath = "windows64standaloneplayer";
            }
        }
        else if (version.startsWith("5.")) {
            if (version.startsWith("5.3")) {
                monoManagedSubpath = "win64_nondevelopment_mono/Data";
            }
            else {
                monoManagedSubpath = "win64_nondevelopment/Data";
            }
        }
        else if (version.startsWith("20")) {
            if (UnityVersion.compare(version, "2021.2.0") < 0)
                monoManagedSubpath = "win64_nondevelopment_mono/Data";
        }

        return monoManagedSubpath + "/Managed";
    }

}