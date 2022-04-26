package slaynash.lum.bot.discord.melonscanner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import slaynash.lum.bot.Localization;
import slaynash.lum.bot.utils.ExceptionUtils;
import slaynash.lum.bot.utils.Utils;

public class MelonLoaderError {

    private static final List<MelonLoaderError> knownUnhollowerErrors = new ArrayList<>();
    private static final List<MelonLoaderError> knownErrors = new ArrayList<>();
    private static final Map<String, List<MelonLoaderError>> gameSpecificErrors = new HashMap<>();
    private static final List<MelonLoaderError> modSpecificErrors = new ArrayList<>();

    public static final MelonLoaderError nkh6 = new MelonLoaderError("A mod is missing NKHook6. NKHook6 is broken and it is recommended to remove the mod that depends on it.");
    public static final MelonLoaderError btd6mh = new MelonLoaderError("A mod is missing BTD Mod Helper. Please unzip [this zip](https://github.com/gurrenm3/BTD-Mod-Helper/releases/latest/) into your Mods folder.");
    public static final MelonLoaderError mlMissing = new MelonLoaderError("A mod is missing a MelonLoader file. Add to your Virus scanner exception list and reinstall MelonLoader.");
    public static final MelonLoaderError mlCompromised = new MelonLoaderError("MelonLoader is in a compromised state and is displaying sensitive information. Please reinstall MelonLoader.");

    public static final MelonLoaderError incompatibleAssemblyError = new MelonLoaderError(
            "\\[[0-9.:]+\\] \\[ERROR\\] System.BadImageFormatException:.*",
            "You have an invalid or incompatible assembly in your `Mods` or `Plugins` folder.");


    public final String nextLineRegex;
    public final String regex;
    public final String error;

    public MelonLoaderError(String error) {
        this.error = error;
        this.regex = null;
        this.nextLineRegex = null;
    }
    public MelonLoaderError(String regex, String error) {
        this.regex = regex;
        this.error = error;
        this.nextLineRegex = null;
    }
    public MelonLoaderError(String regex, String error, String nextLineRegex) {
        this.regex = regex;
        this.error = error;
        this.nextLineRegex = nextLineRegex;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof MelonLoaderError me)) {
            return false;
        }
        return this.error.equals(me.error);
    }

    public static boolean init() {

        Gson gson = new Gson();

        try (Stream<String> lines = Files.lines(new File("melonscannererrors.json").toPath())) {
            String data = lines.collect(Collectors.joining("\n"))
                .replace("$MLRelease$", MelonScanner.latestMLVersionRelease)
                .replace("$MLBeta$", MelonScanner.latestMLVersionAlpha);

            HashMap<String, JsonElement> filedata = gson.fromJson(data, new TypeToken<HashMap<String, JsonElement>>() {}.getType());
            synchronized (knownUnhollowerErrors) {
                knownUnhollowerErrors.clear();
                knownUnhollowerErrors.addAll(gson.fromJson(filedata.get("unhollowerErrors"), new TypeToken<ArrayList<MelonLoaderError>>() {}.getType()));
            }
            synchronized (knownErrors) {
                knownErrors.clear();
                knownErrors.addAll(gson.fromJson(filedata.get("knownErrors"), new TypeToken<ArrayList<MelonLoaderError>>() {}.getType()));
            }
            synchronized (gameSpecificErrors) {
                gameSpecificErrors.clear();
                gameSpecificErrors.putAll(gson.fromJson(filedata.get("gameSpecificErrors"), new TypeToken<HashMap<String, List<MelonLoaderError>>>() {}.getType()));
            }
            synchronized (modSpecificErrors) {
                modSpecificErrors.clear();
                HashMap<String, String> modSpecificErrorsTemp = gson.fromJson(filedata.get("modSpecificErrors"), new TypeToken<HashMap<String, String>>() {}.getType());
                for (Entry<String, String> modSpecificError : modSpecificErrorsTemp.entrySet())
                    modSpecificErrors.add(new MelonLoaderError(modSpecificError.getKey(), modSpecificError.getValue()));
            }
            System.out.println("Done loading Errors.");
        }
        catch (IOException exception) {
            ExceptionUtils.reportException("Failed to load MelonLoader Errors", exception);
            return false;
        }

        return true;
    }

    public static boolean reload() {
        return init();
    }


    public static List<MelonLoaderError> getKnownUnhollowerErrors() {
        synchronized (knownUnhollowerErrors) {
            return new ArrayList<>(knownUnhollowerErrors);
        }
    }

    public static List<MelonLoaderError> getKnownErrors() {
        synchronized (knownErrors) {
            return new ArrayList<>(knownErrors);
        }
    }

    public static Map<String, List<MelonLoaderError>> getGameSpecificErrors() {
        synchronized (gameSpecificErrors) {
            return new HashMap<>(gameSpecificErrors);
        }
    }

    public static List<MelonLoaderError> getModSpecificErrors() {
        synchronized (modSpecificErrors) {
            return new ArrayList<>(modSpecificErrors);
        }
    }

    public String error(String lang) {
        switch (lang) {
            case "fr":
                return Utils.translate("en", "fr", error);
            case "de":
                return Utils.translate("en", "de", error);
            case "sga":
                return Localization.toStandardGalacticAlphabet(error);
            default:
        }
        return error;
    }
}
