package edu.farmingdale.advancedprogramming_capstone_project;

/**
 * TokenManager is a simple singleton to store the ACS access token.
 */
public class TokenManager {
    private static String token;

    public static void setToken(String tokenValue) {
        token = tokenValue;
    }

    public static String getToken() {
        return token;
    }
}


