package slaynash.lum.bot.uvm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.NotImplementedException;

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

    public static String getUnityPath(String version, boolean is64bit, boolean isIl2Cpp, boolean dev) {
        String archName = is64bit ? "64" : "32";
        String runtimeName = isIl2Cpp ? "il2cpp" : "mono";
        String devName = dev ? "development" : "nondevelopment";

        if (!version.startsWith("20") || version.startsWith("2017.1."))
            throw new NotImplementedException("Function not implemented for unity versions other than 2017.2.0");

        if (version.startsWith("20")) {
            if (UnityVersion.compare(version, "2021.2.0") < 0)
                return "win" + archName + "_" + devName + "_" + runtimeName + "/";
        }

        return "win" + archName + "_player_" + devName + "_" + runtimeName + "/";
    }

    public static String getPdbName(String version, boolean is64bit, boolean isIl2Cpp, boolean dev) {
        String archName = is64bit ? "64" : "32";
        String archName2 = is64bit ? "64" : "86";
        String runtimeName = isIl2Cpp ? "il2cpp" : "mono";
        String devName = dev ? "development_" : "";

        if (!version.startsWith("20") || version.startsWith("2017.1."))
            throw new NotImplementedException("Function not implemented for unity versions other than 2017.2.0");

        if (version.startsWith("20")) {
            if (UnityVersion.compare(version, "2021.2.0") < 0)
                return "UnityPlayer_Win" + archName + "_" + devName + runtimeName + "_x" + archName2 + ".pdb";
            else if (UnityVersion.compare(version, "2022.2.0") < 0)
                return "UnityPlayer_Win" + archName + "_player_" + devName + runtimeName + "_x" + archName2 + "_s.pdb";
        }

        return "UnityPlayer_Win" + archName + "_player_" + devName + runtimeName + "_x" + archName2 + ".pdb";
    }

}