package slaynash.lum.bot.discord.melonscanner;

import java.util.ArrayList;
import java.util.List;

public class MelonDuplicateMod {

    public final List<String> names = new ArrayList<>();
    
    public MelonDuplicateMod(String name1, String name2) {
        names.add(name1);
        if (!name1.equals(name2))
            names.add(name2);
    }

    public MelonDuplicateMod(String name1) {
        names.add(name1);
    }

    public void addName(String name) {
        if (!names.stream().anyMatch(n -> n.equals(name)))
            names.add(name);
    }

    @Override
    public String toString() {
        String r = names.get(0);
        for (int i = 1; i < names.size(); ++i)
            r += "/" + names.get(i);
        return r;
    }

    public boolean hasName(String name) {
        return names.stream().anyMatch(n -> n.equals(name));
    }

}
