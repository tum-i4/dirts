package edu.tum.sse.dirts.util;

import java.util.logging.Level;

/**
 * Central loggin provider
 */
public class Log {

    private static Level logLevel = Level.WARNING;

    public static void setLogLevel(Level logLevel) {
        Log.logLevel = logLevel;
    }

    public static void log(Level level, String msg) {
        if (level.intValue() >= logLevel.intValue()) {
            System.out.println("[" + level + "] " + msg);
        }
    }

    public static void log(Level level, String alternativePrefix, String msg) {
        if (level.intValue() >= logLevel.intValue()) {
            System.out.println("[" + alternativePrefix + "]" + " " + msg);
        }
    }

    public static void errLog(Level level, String msg) {
        if (level.intValue() >= logLevel.intValue()) {
            System.err.println("[" + level + "] " + msg);
        }
    }
}
