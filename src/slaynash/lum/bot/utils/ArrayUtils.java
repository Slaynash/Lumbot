package slaynash.lum.bot.utils;

public final class ArrayUtils {

    public static <T> boolean contains(T[] aliases, T value) {
        if (aliases == null)
            return false;

        for (T alias : aliases)
            if ((alias == null && value == null) || alias.equals(value))
                return true;
        
        return false;
    }

    public static boolean contains(long[] aliases, long value) {
        if (aliases == null)
            return false;

        for (long alias : aliases)
            if (alias == value)
                return true;
        
        return false;
    }
    
}
