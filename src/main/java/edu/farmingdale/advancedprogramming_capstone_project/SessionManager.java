package edu.farmingdale.advancedprogramming_capstone_project;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * SessionManager manages call sessions by generating a unique session code
 * and storing active sessions in a shared map.
 */
public class SessionManager {
    private static final Map<String, String> activeSessions = new HashMap<>();

    /**
     * Creates a new session by generating a unique session code.
     * @return The generated session code.
     */
    public static String createSession() {
        String sessionCode = UUID.randomUUID().toString().substring(0, 6);
        activeSessions.put(sessionCode, "Active");
        return sessionCode;
    }

    /**
     * Checks if a session exists for the given session code.
     * @param sessionCode The session code.
     * @return True if the session exists, false otherwise.
     */
    public static boolean joinSession(String sessionCode) {
        return activeSessions.containsKey(sessionCode);
    }
}
