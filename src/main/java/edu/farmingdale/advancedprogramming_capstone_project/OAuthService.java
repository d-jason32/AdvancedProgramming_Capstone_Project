package edu.farmingdale.advancedprogramming_capstone_project;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.aad.msal4j.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import javafx.application.HostServices;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static edu.farmingdale.advancedprogramming_capstone_project.LoginController.dotenv;

public class OAuthService {
    /**
     * Handles Google OAuth 2.0 authentication flow for an application.
     * It manages the process of constructing authentication URLs, opening a browser
     * for user login, handling the callback from the Google OAuth server, and
     * getting access tokens and user information.
     * 1. It creates the authorization URL
     * 2. It opens the browser for their Google account login
     * 3. Starts a callback server so it can handle their response
     */
    public static class GoogleAuthHandler {
        private static final String CLIENT_ID = dotenv.get("GOOGLE_CLIENT_ID");
        private static final String CLIENT_SECRET = dotenv.get("GOOGLE_CLIENT_SECRET");
        private static final String REDIRECT_URI = "http://localhost:8080/auth/google/callback";
        private static final String SCOPE = "email profile";
        private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
        private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

        private final Runnable onSuccess;
        private final Consumer<String> onError;
        private final HostServices hostServices;

        /**
         * Initializes a GoogleAuthHandler instance
         * @param onSuccess Runnable to execute on successful authentication
         * @param onError Consumer<String> to handle error messages
         * @param hostServices HostServices for browser operations
         */
        public GoogleAuthHandler(Runnable onSuccess, Consumer<String> onError, HostServices hostServices) {
            this.onSuccess = onSuccess;
            this.onError = onError;
            this.hostServices = hostServices;
        }

        /**
         * Starts the Google OAuth 2.0 authentication process
         * 1. Builds the authorization URL
         * 2. Opens the browser for user login
         * 3. Starts a callback server to handle their response
         */
        public void startAuthentication() {
            try {
                String authUrl = buildAuthUrl();
                openBrowser(authUrl);
                startCallbackServer();
            } catch (Exception e) {
                onError.accept("Google login error: " + e.getMessage());
            }
        }

        /**
         * Constructs the Google OAuth 2.0 authorization URL with required parameters:
         * - client_id: Application's client ID
         * - redirect_uri: Callback URL
         * - response_type: Set to 'code' for authorization code flow
         * - scope: Requested permissions (email and profile)
         * - access_type: offline for refresh tokens
         * - prompt: select_account to have account selection
         * @return authorization URL as a String
         */
        @NotNull
        private String buildAuthUrl() throws UnsupportedEncodingException {
            return "https://accounts.google.com/o/oauth2/v2/auth?" +
                    "client_id=" + CLIENT_ID + "&" +
                    "redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) + "&" +
                    "response_type=code&" +
                    "scope=" + URLEncoder.encode(SCOPE, StandardCharsets.UTF_8) + "&" +
                    "access_type=offline&" +
                    "prompt=select_account";
        }

        /** Opens Browser via Host Services
         * @param url String
         */
        private void openBrowser(String url) {
            hostServices.showDocument(url);
        }


        /**
         * Calls Server Starter
         */
        private void startCallbackServer() {
            new Thread(() -> {
                try {
                    HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
                    server.createContext("/auth/google/callback", exchange -> {
                        try {
                            handleCallback(exchange);
                        } finally {
                            exchange.close();
                            server.stop(0);
                        }
                    });
                    server.start();
                } catch (IOException e) {
                    onError.accept("Failed to start callback server: " + e.getMessage());
                }
            }).start();
        }

        /**
         * Handles the callback from the Google OAuth server
         * 1. Parses the authorization code from query parameters
         * 2. Exchanges code for access token
         * 3. Fetches user info using a access token
         * 4. Triggers success/error callbacks
         * @param exchange HTTP exchange containing callback data
         */
        private void handleCallback(@NotNull HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseQuery(query);
            String code = params.get("code");

            if (code == null) {
                sendErrorResponse(exchange, "No authorization code received");
                onError.accept("Google login failed: No authorization code received");
                return;
            }

            try {
                String tokenResponse = exchangeCodeForTokens(code);
                JsonObject tokenJson = JsonParser.parseString(tokenResponse).getAsJsonObject();
                String accessToken = tokenJson.get("access_token").getAsString();

                String userInfo = getUserInfo(accessToken);
                JsonObject userJson = JsonParser.parseString(userInfo).getAsJsonObject();

                //Extract Email From OAuth for DB connection
                String email = userJson.get("email").getAsString();
                System.out.println("User's email: " + email); // Or store it somewhere

                sendSuccessResponse(exchange);
                onSuccess.run();

            } catch (Exception e) {
                sendErrorResponse(exchange, "Authentication failed");
                onError.accept("Google login failed: " + e.getMessage());
            }
        }

        /**
         * Exchanges an authorization code for access tokens by making a POST request to Google's token endpoint.
         * @param code The authorization code received from Google OAuth
         * @return Response string containing access token and other token information
         * @throws IOException If a network request fails
         * @throws InterruptedException If a token request is interrupted
         */
        private String exchangeCodeForTokens(String code) throws IOException, InterruptedException {
            String params = "code=" + code +
                    "&client_id=" + CLIENT_ID +
                    "&client_secret=" + CLIENT_SECRET +
                    "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                    "&grant_type=authorization_code";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TOKEN_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(params))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        }

        /**
         * @param accessToken String
         * @return Response string containing user information
         * @throws IOException If a network request fails
         * @throws InterruptedException If a token request is interrupted
         */
        private String getUserInfo(String accessToken) throws IOException, InterruptedException {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(USERINFO_URL + "?access_token=" + accessToken))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        }

        /**
         * @param query String
         * @return Map<String, String>
         */
        @NotNull
        private Map<String, String> parseQuery(String query) {
            Map<String, String> params = new HashMap<>();
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length > 1) {
                        params.put(pair[0], pair[1]);
                    }
                }
            }
            return params;
        }

        /**
         * @param exchange HttpExchange
         * @throws IOException IOException
         */
        private void sendSuccessResponse(@NotNull HttpExchange exchange) throws IOException {
            String response = "<html><body>Login successful! You can close this window.</body></html>";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }

        /**
         * Sends an error response with the specified message to the client in an HTTP exchange.
         * @param exchange The HTTP exchange object that represents the incoming request and allows sending responses.
         * @param message  A string containing the error message to be included in the response body.
         * @throws IOException If an I/O error occurs while sending the response.
         */
        private void sendErrorResponse(@NotNull HttpExchange exchange, String message) throws IOException {
            String response = "<html><body>Error: " + message + "</body></html>";
            exchange.sendResponseHeaders(400, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    /**
     * Microsoft OAuth 2.0 Authentication Handler.
     */
    public static class MicrosoftAuthHandler {
        private static final String CLIENT_ID = dotenv.get("MICROSOFT_CLIENT_ID");
        private static final String AUTHORITY = dotenv.get("MICROSOFT_AUTHORITY_ID");
        private static final String REDIRECT_URI = "http://localhost:8080/auth/microsoft/callback";
        private static final String[] SCOPES = {"User.Read"};

        private final Runnable onSuccess;
        private final Consumer<String> onError;
        private final HostServices hostServices;
        private HttpServer server;

        /**
         * Creates a new instance of MicrosoftAuthHandler to manage Microsoft authentication.
         * @param onSuccess A {@code Runnable} that will be executed when authentication succeeds.
         * @param onError A {@code Consumer<String>} that will handle errors during the authentication process.
         * The error message will be passed to the consumer.
         * @param hostServices A {@code HostServices} instance to help with OS-level operations, such as opening the default web browser.
         */
        public MicrosoftAuthHandler(Runnable onSuccess, Consumer<String> onError, HostServices hostServices) {
            this.onSuccess = onSuccess;
            this.onError = onError;
            this.hostServices = hostServices;
        }

        /**
         * Starts Microsoft authentication by:
         * 1. Creating callback server
         * 2. Building auth URL using MSAL
         * 3. Opening browser for user login
         */
        public void startAuthentication() {
            try {
                // Start the server first
                startCallbackServer();

                PublicClientApplication pca = PublicClientApplication.builder(CLIENT_ID)
                        .authority(AUTHORITY)
                        .build();

                // Generate the authorization URL
                String authUrl = pca.getAuthorizationRequestUrl(
                        AuthorizationRequestUrlParameters
                                .builder(REDIRECT_URI, Collections.singleton(SCOPES[0]))
                                .responseMode(ResponseMode.QUERY) // Explicitly set response mode
                                .build()
                ).toString();

                System.out.println("Authorization URL: " + authUrl); // Debug logging
                hostServices.showDocument(authUrl);

            } catch (Exception e) {
                Platform.runLater(() -> onError.accept("Microsoft login error: " + e.getMessage()));
            }
        }

        /**
         * Starts the callback server.
         */
        private void startCallbackServer() {
            new Thread(() -> {
                try {
                    server = HttpServer.create(new InetSocketAddress(8080), 0);
                    server.createContext("/auth/microsoft/callback", this::handleCallback);
                    server.setExecutor(null); // Use default executor
                    server.start();
                    System.out.println("Callback server started on port 8080");
                } catch (IOException e) {
                    Platform.runLater(() -> onError.accept("Failed to start callback server: " + e.getMessage()));
                }
            }).start();
        }

        /**
         * Handles the callback received from the authentication server.
         * @param exchange The HTTP exchange object containing the request and response data.
         * @throws IOException If there is an error while handling the HTTP exchange or sending the response.
         */
        private void handleCallback(@NotNull HttpExchange exchange) throws IOException {
            try {
                String query = exchange.getRequestURI().getQuery();
                System.out.println("Received callback with query: " + query); // Debug logging

                if (query == null) {
                    sendResponse(exchange, 400, "Missing query parameters");
                    Platform.runLater(() -> onError.accept("No query parameters received"));
                    return;
                }

                Map<String, String> params = parseQuery(query);
                String code = params.get("code");
                String error = params.get("error");

                if (error != null) {
                    sendResponse(exchange, 400, "Error: " + error);
                    Platform.runLater(() -> onError.accept("Authentication error: " + error));
                    return;
                }

                if (code == null) {
                    sendResponse(exchange, 400, "Missing authorization code");
                    Platform.runLater(() -> onError.accept("No authorization code received"));
                    return;
                }

                sendResponse(exchange, 200, "Authentication successful! You may close this window.");
                exchange.close();

                // Process the authorization code
                acquireToken(code);

            } finally {
                server.stop(0); // Ensure the server stops after handling the request
            }
        }

        /**
         * Exchanges authorization code for tokens using the MSAL library
         * @param code Authorization code from callback
         */
        private void acquireToken(String code) {
            try {
                PublicClientApplication pca = PublicClientApplication.builder(CLIENT_ID)
                        .authority(AUTHORITY)
                        .build();

                IAuthenticationResult result = pca.acquireToken(
                        AuthorizationCodeParameters
                                .builder(code, new URI(REDIRECT_URI))
                                .scopes(Collections.singleton(SCOPES[0]))
                                .build()
                ).join();

                System.out.println("Successfully acquired token for: " + result.account().username());
                Platform.runLater(onSuccess);

            } catch (Exception e) {
                Platform.runLater(() -> onError.accept("Failed to acquire token: " + e.getMessage()));
                e.printStackTrace();
            }
        }

        /**
         * Parses a query string into a map of key-value pairs.
         * The query string is expected to be in the format of URL query parameters.
         * @param query the query string to parse; can be null or empty
         * @return a map containing the parsed key-value pairs from the query string
         */
        @NotNull
        private Map<String, String> parseQuery(String query) {
            Map<String, String> params = new HashMap<>();
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length > 1) {
                        params.put(pair[0], pair[1]);
                    }
                }
            }
            return params;
        }

        /**
         * Sends a response to the client.
         */
        private void sendResponse(@NotNull HttpExchange exchange, int statusCode, String message) throws IOException {
            String response = "<html><body>" + message + "</body></html>";
            exchange.sendResponseHeaders(statusCode, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    /**
     * GitHub OAuth 2.0 Authentication Handler.
     */
    public static class GithubAuthHandler {
        private static final String CLIENT_ID = dotenv.get("GITHUB_CLIENT_ID");
        private static final String CLIENT_SECRET = dotenv.get("GITHUB_CLIENT_SECRET");
        private static final String REDIRECT_URI = "http://localhost:8080/auth/github/callback";
        private static final String SCOPE = "user:email";
        private static final String AUTH_URL = "https://github.com/login/oauth/authorize";
        private static final String TOKEN_URL = "https://github.com/login/oauth/access_token";
        private static final String USER_API_URL = "https://api.github.com/user";

        private final Runnable onSuccess;
        private final Consumer<String> onError;
        private final HostServices hostServices;

        /**
         * Creates new GithubAuthHandler instance
         * @param onSuccess Runnable for success
         * @param onError Consumer<String> for errors
         * @param hostServices HostServices for browser
         */
        public GithubAuthHandler(Runnable onSuccess, Consumer<String> onError, HostServices hostServices) {
            this.onSuccess = onSuccess;
            this.onError = onError;
            this.hostServices = hostServices;
        }

        /**
         * Initiates the GitHub OAuth flow:
         * 1. Builds auth URL
         * 2. Opens browser
         * 3. Starts callback server
         */
        public void startAuthentication() {
            try {
                String authUrl = buildAuthUrl();
                openBrowser(authUrl);
                startCallbackServer();
            } catch (Exception e) {
                onError.accept("GitHub login error: " + e.getMessage());
            }
        }

        /**
         * Builds the authentication URL for GitHub.
         */
        @NotNull
        private String buildAuthUrl() throws UnsupportedEncodingException {
            return AUTH_URL + "?client_id=" + CLIENT_ID +
                    "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                    "&scope=" + URLEncoder.encode(SCOPE, StandardCharsets.UTF_8) +
                    "&response_type=code";
        }

        /**
         * Opens the specified URL in the default web browser.
         */
        private void openBrowser(String url) {
            hostServices.showDocument(url);
        }

        /**
         * Starts the callback server.
         */
        private void startCallbackServer() {
            new Thread(() -> {
                try {
                    HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
                    server.createContext("/auth/github/callback", exchange -> {
                        try {
                            handleCallback(exchange);
                        } finally {
                            exchange.close();
                            server.stop(0);
                        }
                    });
                    server.start();
                } catch (IOException e) {
                    onError.accept("Failed to start callback server: " + e.getMessage());
                }
            }).start();
        }

        /**
         * Handles the callback received from the authentication server.
         */
        private void handleCallback(@NotNull HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseQuery(query);
            String code = params.get("code");

            if (code == null) {
                sendErrorResponse(exchange, "No authorization code received");
                onError.accept("GitHub login failed: No authorization code received");
                return;
            }

            try {
                String accessToken = exchangeCodeForToken(code);
                String userInfo = getUserInfo(accessToken);
                JsonObject userJson = JsonParser.parseString(userInfo).getAsJsonObject();

                System.out.println("Logged in as GitHub user: " + userJson.get("login").getAsString());
                sendSuccessResponse(exchange);
                onSuccess.run();
            } catch (Exception e) {
                sendErrorResponse(exchange, "Authentication failed");
                onError.accept("GitHub login failed: " + e.getMessage());
            }
        }

        /**
         * Exchanges GitHub auth code for access token
         * @param code Authorization code
         * @return Access token string
         */
        private String exchangeCodeForToken(String code) throws IOException, InterruptedException {
            String params = "client_id=" + CLIENT_ID +
                    "&client_secret=" + CLIENT_SECRET +
                    "&code=" + code +
                    "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TOKEN_URL))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(params))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject tokenJson = JsonParser.parseString(response.body()).getAsJsonObject();
            return tokenJson.get("access_token").getAsString();
        }

        /**
         * Fetches user info from GitHub API
         * @param accessToken Valid access token
         * @return JSON string of user data
         */
        private String getUserInfo(String accessToken) throws IOException, InterruptedException {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(USER_API_URL))
                    .header("Authorization", "token " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        }

        /**
         * Parses URL query string into key-value pairs
         * @param query The query string to parse
         * @return Map of parameter names to values
         */
        @NotNull
        private Map<String, String> parseQuery(String query) {
            Map<String, String> params = new HashMap<>();
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length > 1) {
                        params.put(pair[0], pair[1]);
                    }
                }
            }
            return params;
        }

        /**
         * Sends HTML success response to browser
         * @param exchange HTTP exchange to respond to
         */
        private void sendSuccessResponse(@NotNull HttpExchange exchange) throws IOException {
            String response = "<html><body>GitHub login successful! You can close this window.</body></html>";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }

        /**
         * Sends HTML error response to browser
         * @param exchange HTTP exchange to respond to
         * @param message Error message to display
         */
        private void sendErrorResponse(@NotNull HttpExchange exchange, String message) throws IOException {
            String response = "<html><body>Error: " + message + "</body></html>";
            exchange.sendResponseHeaders(400, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
}