package edu.farmingdale.advancedprogramming_capstone_project;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * SessionManager handles live class sessions by generating a unique session code.
 */
public class SessionManager {
    private static String sessionCode;
    private static Set<String> activeUsers = new HashSet<>();

    /**
     * Creates a new session by generating a unique 6-character code.
     * @return The session code.
     */
    public static String createSession() {
        sessionCode = UUID.randomUUID().toString().substring(0, 6);
        return sessionCode;
    }

    /**
     * Adds a user to the session if the code matches.
     * @param code The session code.
     * @param username The user's name.
     * @return True if added, false otherwise.
     */
    public static boolean joinSession(String code, String username) {
        if (code.equals(sessionCode)) {
            activeUsers.add(username);
            return true;
        }
        return false;
    }

    /**
     * Ends the current session.
     */
    public static void endSession() {
        sessionCode = null;
        activeUsers.clear();
    }
}
