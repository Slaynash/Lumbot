package slaynash.lum.bot.log;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.utils.TimeManager;

public class LumLogOutput extends PrintStream {
    private final String name;

    private static final Map<Class<?>, String> knownLoggingElements = new HashMap<>();

    public LumLogOutput(PrintStream defaultOut, String streamName) {
        super(defaultOut);
        this.name = streamName;
    }

    @Override
    public void println(String obj) {
        String loggingElement = null;
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            Class<?> elementClass;
            try {
                elementClass = Class.forName(element.getClassName());
            }
            catch (Exception e) {
                break;
            }

            loggingElement = knownLoggingElements.get(elementClass);
            if (loggingElement != null)
                break;

            for (Field field : elementClass.getFields()) {
                if (field.getName().equals("LOG_IDENTIFIER") && Modifier.isStatic(field.getModifiers()) && Modifier.isPublic(field.getModifiers())) {
                    try {
                        loggingElement = (String) field.get(null);
                        knownLoggingElements.put(elementClass, loggingElement);
                        break;
                    }
                    catch (Exception ignored) { }
                }
            }
            if (loggingElement != null)
                break;

            if (elementClass.getSuperclass() != null && elementClass.getSuperclass() == Command.class) {
                knownLoggingElements.put(elementClass, loggingElement = elementClass.getName());
                break;
            }
        }
        
        String[] lines = obj.split("\n");
        for (String line : lines) {
            if (loggingElement != null)
                super.println("[" + TimeManager.getTimeForLog() + "] [" + name + "] [" + loggingElement + "] " + line);
            else
                super.println("[" + TimeManager.getTimeForLog() + "] [" + name + "] " + line);
        }
    }
}
