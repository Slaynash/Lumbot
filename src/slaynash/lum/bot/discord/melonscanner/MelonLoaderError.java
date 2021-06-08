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

public class MelonLoaderError {
    
    public static final List<MelonLoaderError> knownUnhollowerErrors = new ArrayList<>();
    public static final List<MelonLoaderError> knownErrors = new ArrayList<>();
    public static final Map<String, List<MelonLoaderError>> gameSpecificErrors = new HashMap<>();
    public static final List<MelonLoaderError> modSpecificErrors = new ArrayList<>();
    
    public static final MelonLoaderError nkh6 = new MelonLoaderError("", "A mod is missing NKHook6. NKHook6 is broken and it is recommended to remove the mod that depends on it.");
    public static final MelonLoaderError mlMissing = new MelonLoaderError("", "A mod is missing a MelonLoader file. Add to your Virus scanner exeption list and reinstall MelonLoader.");
    
    public static final MelonLoaderError incompatibleAssemblyError = new MelonLoaderError(
            "\\[[0-9.:]+\\] \\[ERROR\\] System.BadImageFormatException:.*",
            "You have an invalid or incompatible assembly in your `Mods` or `Plugins` folder.");


    public String regex;
    public String error;
    
    public MelonLoaderError(String regex, String error) {
        this.regex = regex;
        this.error = error;
    }
    

    public static void init() {

        Gson gson = new Gson();

        try (Stream<String> lines = Files.lines(new File("melonscannererrors.json").toPath())) {
            String data = lines.collect(Collectors.joining("\n"));

            HashMap<String, JsonElement> filedata = gson.fromJson(data, new TypeToken<HashMap<String, JsonElement>>() {}.getType());
            knownUnhollowerErrors.addAll(gson.fromJson(filedata.get("unhollowerErrors"), new TypeToken<ArrayList<MelonLoaderError>>() {}.getType()));
            knownErrors.addAll(gson.fromJson(filedata.get("knownErrors"), new TypeToken<ArrayList<MelonLoaderError>>() {}.getType()));
            gameSpecificErrors.putAll(gson.fromJson(filedata.get("gameSpecificErrors"), new TypeToken<HashMap<String, List<MelonLoaderError>>>() {}.getType()));

            HashMap<String, String> modSpecificErrorsTemp = gson.fromJson(filedata.get("modSpecificErrors"), new TypeToken<HashMap<String, String>>() {}.getType());
            for (Entry<String, String> modSpecificError : modSpecificErrorsTemp.entrySet())
                modSpecificErrors.add(new MelonLoaderError(modSpecificError.getKey(), modSpecificError.getValue()));
        }
        catch (IOException exception) {
            exception.printStackTrace();
        }
    }

}
