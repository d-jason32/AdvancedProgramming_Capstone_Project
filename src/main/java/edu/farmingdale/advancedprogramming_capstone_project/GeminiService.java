package edu.farmingdale.advancedprogramming_capstone_project;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

/**
 * GeminiService interacts with the Gemini API to generate a lesson summary.
 * The API key is loaded from config.properties via AI_Helper.
 */
public class GeminiService {
    // Build the Gemini API URL using the API key.
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + AI_Helper.getApiKey();
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Sends a prompt to the Gemini API asynchronously and returns a CompletableFuture with the summary.
     *
     * @param prompt The text to summarize.
     * @return A CompletableFuture containing the summary response as a string.
     */
    public static CompletableFuture<String> getSummaryAsync(String prompt) {
        String jsonPayload = "{ \"contents\": [{ \"parts\":[{\"text\": \"" + prompt + "\"}] }] }";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .exceptionally(e -> {
                    e.printStackTrace();
                    return "Error calling Gemini API: " + e.getMessage();
                });
    }
}