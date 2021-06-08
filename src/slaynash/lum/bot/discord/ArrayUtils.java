package slaynash.lum.bot.discord;

public final class ArrayUtils {

    public static boolean contains(String[] aliases, String name) {
        if (aliases == null)
            return false;

        for (String alias : aliases)
            if (alias.equals(name))
                return true;
        
        return false;
    }
    
}
