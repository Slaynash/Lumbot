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

public final class Localization {

    public static Map<String, Map<String, String>> localizations;
    
    public static void init() {
        Gson gson = new Gson();

        try (Stream<String> lines = Files.lines(new File("localization.json").toPath())) {
            String data = lines.collect(Collectors.joining("\n"));

            localizations = gson.fromJson(data, new TypeToken<HashMap<String, HashMap<String, String>>>() {}.getType());
        }
        catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public static String get(String key, String lang) {
        Map<String, String> langMap = localizations.get(lang);
        if (langMap == null) {
            if (lang.equals("en"))
                return key;
            return get(key, "en");
        }
        
        String locale = langMap.get(key);
        if (locale == null) {
            if (lang.equals("en"))
                return key;
            return get(key, "en");
        }

        return locale;
    }

    public static String getFormat(String key, String lang, Object... args) {
        String ret = get(key, lang);
        if (!ret.equals(key))
            ret = String.format(ret, args);
        return ret;
    }

}
