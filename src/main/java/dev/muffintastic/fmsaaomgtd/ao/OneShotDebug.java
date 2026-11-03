package dev.muffintastic.fmsaaomgtd.ao;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class OneShotDebug {
    // Compile-time toggle
    public static final boolean DEBUG = false;

    private static final Map<String, AtomicBoolean> ONCE = new ConcurrentHashMap<>();

    private OneShotDebug() {}

    public static void print(final String site, final String message) {
        if (DEBUG && ONCE.putIfAbsent(site, new AtomicBoolean(true)) == null) {
            System.out.println("[fmsaaomgtd:debug] " + site + ": " + message);
        }
    }
}
