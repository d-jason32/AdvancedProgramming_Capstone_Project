package edu.farmingdale.advancedprogramming_capstone_project;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * GeminiService interacts with the Gemini API to generate a summary.
 * Handles basic JSON construction and error checking.
 */
public class GeminiService {

    // --- Configuration ---
    // TODO: Replace "gemini-1.5-flash-latest" with your desired valid model name
    private static final String MODEL_NAME = "gemini-1.5-flash-latest";
    private static final String API_KEY = AI_Helper.getApiKey();
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL_NAME + ":generateContent?key=" + API_KEY;
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1) // Or HTTP_2 if preferred/supported
            .build();
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    /**
     * Sends a prompt to the Gemini API asynchronously and returns a CompletableFuture with the summary.
     * Includes basic JSON escaping and HTTP status code checking.
     *
     * @param prompt The text to summarize.
     * @return A CompletableFuture containing the summary response as a string, or an error message.
     */
    public static CompletableFuture<String> getSummaryAsync(String prompt) {
        if (API_KEY == null || API_KEY.trim().isEmpty()) {
            System.err.println("ERROR: Gemini API Key is missing or empty.");
            return CompletableFuture.completedFuture("Error: Gemini API Key not configured.");
        }

        try {
            ObjectNode payloadNode = jsonMapper.createObjectNode();
            payloadNode.putArray("contents")
                    .addObject()
                    .putArray("parts")
                    .addObject()
                    .put("text", prompt);
            String jsonPayload = jsonMapper.writeValueAsString(payloadNode);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        int statusCode = response.statusCode();
                        String responseBody = response.body();

                        if (statusCode >= 200 && statusCode < 300) {
                            return parseSummaryFromResponse(responseBody);
                        } else {
                            System.err.printf("ERROR: Gemini API call failed with status %d. Response: %s%n", statusCode, responseBody);
                            return String.format("Error: Gemini API failed (Status: %d). %s",
                                    statusCode, parseErrorFromResponse(responseBody));
                        }
                    })
                    .exceptionally(e -> {
                        System.err.println("ERROR: Exception calling Gemini API: " + e.getMessage());
                        e.printStackTrace();
                        return "Error calling Gemini API: " + e.getMessage();
                    });

        } catch (Exception e) {
            // Catch potential errors during request building (e.g., invalid URI)
            System.err.println("ERROR: Failed to build Gemini request: " + e.getMessage());
            e.printStackTrace();
            return CompletableFuture.completedFuture("Error: Failed to build request for Gemini.");
        }
    }

    /**
     * @param responseBody from the Gemini API
     * @return raw response text if parsing fails
     */
    private static String parseSummaryFromResponse(String responseBody) {
        try {
            ObjectNode root = (ObjectNode) jsonMapper.readTree(responseBody);
            return root.path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text")
                    .asText("");
        } catch (Exception e) {
            System.err.println("Error parsing Gemini success response: " + e.getMessage());
            return responseBody;
        }
    }

    /**
     * @param responseBody from the Gemini API
     * @return raw response text if parsing fails
     */
    private static String parseErrorFromResponse(String responseBody) {
        try {
            ObjectNode root = (ObjectNode) jsonMapper.readTree(responseBody);
            return root.path("error")
                    .path("message")
                    .asText(responseBody);
        } catch (Exception e) {
            System.err.println("Error parsing Gemini error response: " + e.getMessage());
            return responseBody;
        }
    }
}