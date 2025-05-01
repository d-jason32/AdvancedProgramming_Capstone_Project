package edu.farmingdale.advancedprogramming_capstone_project;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * AI_Helper loads configuration properties from config.properties.
 * Ensure that config.properties is placed in src/main/resources.
 */
public class AI_Helper {
    private static final Properties properties = new Properties();

    static {
        // Try loading the file using a leading slash.
        try (InputStream input = AI_Helper.class.getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Unable to find config.properties in classpath.");
            } else {
                properties.load(input);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getApiKey() {
        return properties.getProperty("API_KEY", "");
    }

}