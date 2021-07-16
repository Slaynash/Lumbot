package slaynash.lum.bot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import slaynash.lum.bot.utils.ExceptionUtils;

public final class Localization {

    public static Map<String, Map<String, String>> localizations = new HashMap<>();

    public static boolean init() {
        Gson gson = new Gson();

        try (Stream<String> lines = Files.lines(new File("localization.json").toPath())) {
            String data = lines.collect(Collectors.joining("\n"));

            localizations.putAll(gson.fromJson(data, new TypeToken<HashMap<String, HashMap<String, String>>>() {}.getType()));
        }
        catch (IOException exception) {
            ExceptionUtils.reportException("Failed to load translations", exception);
            return false;
        }

        return true;
    }

    public static boolean reload() {
        synchronized (localizations) {
            Map<String, Map<String, String>> localizationsBackup = new HashMap<>(localizations);
            localizations.clear();
            if (!init()) {
                localizations.putAll(localizationsBackup);
                return false;
            }
        }

        return true;
    }

    public static String get(String key, String lang) {

        if ("sga".equals(lang))
            return toStandardGalacticAlphabet(get(key, "en"));

        String locale;

        synchronized (localizations) {
            Map<String, String> langMap = localizations.get(lang);
            if (langMap == null) {
                if (lang.equals("en"))
                    return key;
                return get(key, "en");
            }

            locale = langMap.get(key);
            if (locale == null) {
                if (lang.equals("en"))
                    return key;
                return get(key, "en");
            }
        }

        return locale;
    }

    public static String getFormat(String key, String lang, Object... args) {
        String ret = get(key, lang);
        if (!ret.equals(key))
            ret = String.format(ret, args);
        return ret;
    }

    final static String[] STANDARD_GALACTIC_ALPHABET = {
        "ᔑ", "ʖ", "ᓵ", "↸", "ᒷ", "⎓",
        "⊣", "⍑", "╎", "⋮", "ꖌ", "ꖎ",
        "ᒲ", "リ", "𝙹", "!¡", "ᑑ", "∷",
        "ᓭ", "ℸ", "⚍", "⍊", "∴" , "̇/", "||", "⨅"
    };
    public static String toStandardGalacticAlphabet(String original) {
        StringBuilder ret = new StringBuilder(original.length());

        original.toLowerCase().chars().forEach(c -> {
            if (c <= 'z' && c >= 'a')
                ret.append(STANDARD_GALACTIC_ALPHABET[c - 'a']);
            else
                ret.append((char)c);
        });

        return ret.toString();
    }

}
