package li.cil.oc.util;

import java.util.Collections;
import java.util.Set;

public final class SideTracker {
    private static final Set<Thread> serverThreads = Collections.newSetFromMap(new java.util.WeakHashMap<Thread, Boolean>());

    public static void addServerThread() {
        serverThreads.add(Thread.currentThread());
    }

    public static boolean isServer() {
        // Simple check - if we're on a server thread, we're server-side
        return serverThreads.contains(Thread.currentThread()) || Thread.currentThread().getName().contains("Server");
    }

    public static boolean isClient() {
        return !isServer();
    }
}
