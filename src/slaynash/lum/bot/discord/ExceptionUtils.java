package slaynash.lum.bot.discord;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class ExceptionUtils {
    
    /**
    Returns a String representation of the exception's stack trace
    @param exception The target exception
    */
    public static String getStackTrace(Exception exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        exception.printStackTrace(pw);

        return sw.toString();
    }
    
}
