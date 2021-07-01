package slaynash.lum.bot.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class TimeManager {
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    
    public static String getTimeForLog() {
        return dtf.format(LocalDateTime.now());
    }
}
