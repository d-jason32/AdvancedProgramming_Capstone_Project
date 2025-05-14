package edu.farmingdale.advancedprogramming_capstone_project;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.fxml.Initializable;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static edu.farmingdale.advancedprogramming_capstone_project.LoginController.dotenv;

public class OAuthService implements Initializable {

    //Database connection and authDB communication layer
    static ConnDbOps cdbop;
    private static List<String> authDB;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize database connection and load existing users
        cdbop = new ConnDbOps();
        cdbop.connectToDatabase();
        ConnDbOps.AuthService.initializeAuthDB(cdbop);
        cdbop.listAllUsers();
        authDB = ConnDbOps.AuthService.getAuthDB();

    }


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
        private static final String REDIRECT_URI = "http://localhost:8081/auth/google/callback";
        private static final String SCOPE = "email profile";
        private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
        private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

        // Callbacks for handling authentication results
        private final Consumer<Map<String, String>> onUserData;
        private final HostServices hostServices;
        private final Consumer<String> onError;
        private Runnable onSuccess;

        /**
         * Initializes a GoogleAuthHandler instance
         *
         * @param onUserData   Runnable to execute on successful authentication
         * @param onError      Consumer<String> to handle error messages
         * @param hostServices HostServices for browser operations
         */
        public GoogleAuthHandler(Consumer<Map<String, String>> onUserData, Consumer<String> onError, HostServices hostServices) {
            this.onUserData = onUserData; //retrieves user data for database insertion
            this.onError = onError; //Debugger
            this.hostServices = hostServices; //Enables browser functionality
        }

        /**
         * Starts the Google OAuth 2.0 authentication process
         * 1. Builds the authorization URL
         * 2. Opens the browser for user login
         * 3. Starts a callback server to handle their response
         */
        public void startAuthentication() {
            // Start the OAuth flow: build URL, open browser, start callback server
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
            // Open system browser to Google login page
            hostServices.showDocument(url);
        }


        /**
         * Creates an HTTP server to capture OAuth callback responses.
         * It will automatically be terminated after processing the authentication response.
         */
        private void startCallbackServer() {
            // Start local HTTP server to handle OAuth callback
            new Thread(() -> {
                try {
                    HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
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
         * 3. Fetches user info by using an access token
         * 4. Triggers success/error callbacks
         * @param exchange HTTP exchange containing callback data
         */
        private void handleCallback(@NotNull HttpExchange exchange) throws IOException {
            // Process Google's callback with authorization code
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseQuery(query);
            String code = params.get("code");

            if (code == null) {
                sendErrorResponse(exchange, "No authorization code received");
                onError.accept("Google login failed: No authorization code received");
                return;
            }

            try {
                // Exchange code for tokens and get user info
                String tokenResponse = exchangeCodeForTokens(code);
                JsonObject tokenJson = JsonParser.parseString(tokenResponse).getAsJsonObject();
                String accessToken = tokenJson.get("access_token").getAsString();

                String userInfo = getUserInfo(accessToken);
                JsonObject userJson = JsonParser.parseString(userInfo).getAsJsonObject();

                // Extract user data from OAuth response
                String email = userJson.get("email").getAsString();
                String firstName = userJson.get("given_name").getAsString();
                String lastName = userJson.get("family_name").getAsString();
                Map<String, String> userData = new HashMap<>();
                userData.put("email", email);
                userData.put("firstName", firstName);
                userData.put("lastName", lastName);

                // Pass data to the success handler
                Platform.runLater(() -> {
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                    Platform.runLater(() -> onUserData.accept(userData));
                });

                sendSuccessResponse(exchange);

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
            // Exchange authorization code for access tokens
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
            // Fetch user profile using access token
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
            // Parse URL query parameters into key-value pairs
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
            // Send HTML response for successful authentication
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
            // Send HTML error response
            String response = "<html><body>Error: " + message + "</body></html>";
            exchange.sendResponseHeaders(400, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    /**
     * Microsoft OAuth 2.0 Authentication Handler with enhanced debugging. Because it was the most difficult one to do.
     */
    public static class MicrosoftAuthHandler {
        private static final String CLIENT_ID = dotenv.get("AZURE_CLIENT_ID");
        private static final String TENANT_ID = dotenv.get("AZURE_TENANT_ID");
        private static final String REDIRECT_URI = "http://localhost:8082/auth/microsoft/callback";
        private static final String[] SCOPES = {"openid", "profile", "email", "offline_access"};

        // Authentication callbacks and services
        private final Consumer<Map<String, String>> onUserData;
        private final Runnable onSuccess;
        private final Consumer<String> onError;
        private final HostServices hostServices;
        private HttpServer server;
        private String codeVerifier;

        /**
         * Initializes the authentication handler with required callbacks and services.
         * Sets up the communication channels for success/error notifications while
         * preparing the browser interaction layer through HostServices.
         *
         * @param onUserData Consumer for successful authentication containing user profile claims
         * @param onSuccess Runnable to execute upon complete authentication flow
         * @param onError Error handler for authentication failures
         * @param hostServices JavaFX browser integration service
         */
        public MicrosoftAuthHandler(Consumer<Map<String, String>> onUserData, Runnable onSuccess,
                                    Consumer<String> onError, HostServices hostServices) {
            // Initialize with success/error handlers and browser service
            System.out.println("[DEBUG] Initializing MicrosoftAuthHandler");
            this.onUserData = onUserData;
            this.onSuccess = onSuccess;
            this.onError = onError;
            this.hostServices = hostServices;
        }

        /**
         * Initiates the Microsoft authentication workflow including PKCE preparation.
         * Shows the complete sequence from configuration validation through
         * browser redirection, implementing the OAuth 2.0 authorization code grant flow.
         */
        void startAuthentication() {
            System.out.println("[DEBUG] Starting Microsoft authentication process");
            try {
                // Verify configuration first
                if (CLIENT_ID == null || TENANT_ID == null) {
                    throw new Exception("Azure AD configuration is incomplete");
                }

                // Generate PKCE code verifier and challenge
                /*
                PKCE (Proof Key for Code Exchange) is made for OAuth 2.0 Authorization.
                Originally designed for mobile and public clients that cannot securely store a client secret.
                PKCE gives temporary "proof" that the app requesting the authorization is the same one redeeming it.
                 */
                codeVerifier = generateCodeVerifier();
                String codeChallenge = generateCodeChallenge(codeVerifier);

                // Store codeVerifier for later use

                // Start local server for callback
                startCallbackServer();

                // Build authorization URL with PKCE parameters
                String authUrl = String.format(
                        "https://login.microsoftonline.com/%s/oauth2/v2.0/authorize?" +
                                "client_id=%s&" +
                                "response_type=code&" +
                                "redirect_uri=%s&" +
                                "response_mode=query&" +
                                "scope=%s&" +
                                "state=12345&" +
                                "code_challenge=%s&" +
                                "code_challenge_method=S256",
                        TENANT_ID,
                        CLIENT_ID,
                        URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8),
                        URLEncoder.encode(String.join(" ", SCOPES), StandardCharsets.UTF_8),
                        URLEncoder.encode(codeChallenge, StandardCharsets.UTF_8)
                );

                System.out.println("[DEBUG] Constructed Auth URL: " + authUrl);
                hostServices.showDocument(authUrl);

            } catch (Exception e) {
                System.err.println("[ERROR] Microsoft authentication failed: " + e.getMessage());
                Platform.runLater(() -> onError.accept("Microsoft login error: " + e.getMessage()));
            }
        }
        /**
         * Creates an HTTP server to securely capture authorization responses.
         * The server operates on a dedicated port with minimal exposure window,
         * automatically terminating after processing the OAuth callback.
         */
        private void startCallbackServer() {
            // Starts local HTTP server to handle OAuth redirect
            new Thread(() -> {
                try {
                    System.out.println("[DEBUG] Creating HTTP server on port 8082");
                    server = HttpServer.create(new InetSocketAddress(8082), 0);

                    server.createContext("/auth/microsoft/callback", exchange -> {
                        try {
                            System.out.println("[DEBUG] Received callback request");
                            handleCallback(exchange);
                        } catch (Exception e) {
                            System.err.println("[ERROR] Callback handling failed: " + e.getMessage());
                            e.printStackTrace();
                        } finally {
                            exchange.close();
                        }
                    });

                    server.setExecutor(null);
                    server.start();
                    System.out.println("[DEBUG] Callback server started successfully on port 8082");

                } catch (IOException e) {
                    System.err.println("[ERROR] Failed to start callback server: " + e.getMessage());
                    Platform.runLater(() -> onError.accept("Failed to start callback server: " + e.getMessage()));
                }
            }).start();
        }

        /**
         * Processes the authorization response from Microsoft's identity platform.
         * Validates response parameters and initiates token exchange while handling
         * protocol errors according to OAuth 2.0 specifications.
         *
         * @param exchange HTTP connection containing authorization result
         * @throws IOException for network communication failures
         */
        private void handleCallback(HttpExchange exchange) throws IOException {
            // Processes the authorization code from Microsoft's redirect
            System.out.println("[DEBUG] Handling callback from Microsoft");
            try {
                String query = exchange.getRequestURI().getQuery();
                System.out.println("[DEBUG] Callback query: " + query);

                if (query == null) {
                    sendErrorResponse(exchange, "Missing query parameters");
                    Platform.runLater(() -> onError.accept("No query parameters received"));
                    return;
                }

                Map<String, String> params = parseQuery(query);
                String code = params.get("code");
                String error = params.get("error");

                if (error != null) {
                    sendErrorResponse(exchange, "Error: " + error);
                    Platform.runLater(() -> onError.accept("Authentication error: " + error));
                    return;
                }

                if (code == null) {
                    sendErrorResponse(exchange, "Missing authorization code");
                    Platform.runLater(() -> onError.accept("No authorization code received"));
                    return;
                }

                sendSuccessResponse(exchange);
                acquireToken(code); // Exchange code for tokens

            } finally {
                System.out.println("[DEBUG] Stopping callback server");
                server.stop(0); // Shutdown callback server
            }
        }

        /**
         * Does the token exchange.
         * Secures the request with previously generated PKCE verifier and
         *
         * @param code Authorization code from initial authentication phase
         */
        private void acquireToken(String code) {
            // Exchanges authorization code for access token using PKCE
            try {
                String tokenUrl = String.format(
                        "https://login.microsoftonline.com/%s/oauth2/v2.0/token",
                        TENANT_ID
                );
                String params = String.format(
                        "grant_type=authorization_code&" +
                                "client_id=%s&" +
                                "scope=%s&" +
                                "code=%s&" +
                                "redirect_uri=%s&" +
                                "client_secret=%s&" +
                                "code_verifier=%s",
                        URLEncoder.encode(CLIENT_ID, StandardCharsets.UTF_8),
                        URLEncoder.encode(String.join(" ", SCOPES), StandardCharsets.UTF_8),
                        URLEncoder.encode(code, StandardCharsets.UTF_8),
                        URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8),
                        URLEncoder.encode(Objects.requireNonNull(dotenv.get("AZURE_CLIENT_SECRET")), StandardCharsets.UTF_8),
                        URLEncoder.encode(codeVerifier, StandardCharsets.UTF_8)
                );

                HttpClient client = HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .connectTimeout(Duration.ofSeconds(30))
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(tokenUrl))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(params))
                        .build();

                System.out.println("[DEBUG] Sending token request...");
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                System.out.println("[DEBUG] Token response status: " + response.statusCode());
                System.out.println("[DEBUG] Token response body: " + response.body());

                if (response.statusCode() == 200) {
                    processSuccessfulTokenResponse(response.body());
                } else {
                    handleTokenError(response);
                }
            } catch (Exception e) {
                System.out.println("[ERROR] Detailed token acquisition error:");
                e.printStackTrace();
                handleTokenException(e);
            }
        }

        /**
         * Extracts and verifies user identity claims from ID token.
         * Implements JWT validation and claim processing
         *
         * @param responseBody Raw token endpoint response containing JWT
         */
        private void processSuccessfulTokenResponse(String responseBody) {
            try {
                JsonObject tokenJson = JsonParser.parseString(responseBody).getAsJsonObject();
                String idToken = tokenJson.get("id_token").getAsString();

                // Decode JWT to get user claims
                String[] parts = idToken.split("\\.");
                String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                JsonObject claims = JsonParser.parseString(payload).getAsJsonObject();

                String email = claims.has("preferred_username") ?
                        claims.get("preferred_username").getAsString() :
                        claims.get("email").getAsString();
                String firstName = claims.has("given_name") ? claims.get("given_name").getAsString() : "";
                String lastName = claims.has("family_name") ? claims.get("family_name").getAsString() : "";

                Map<String, String> userData = new HashMap<>();
                userData.put("email", email);
                userData.put("firstName", firstName);
                userData.put("lastName", lastName);

                Platform.runLater(() -> {
                    onUserData.accept(userData);
                    onSuccess.run();
                });

            } catch (Exception e) {
                Platform.runLater(() -> onError.accept("Failed to process token response: " + e.getMessage()));
            }
        }

        /**
         * In case the token fails, due to interference between Microsoft and carrying out our request, we will display an error message.
         * @param response
         */
        private void handleTokenError(HttpResponse<String> response) {
            String errorMsg = "Token request failed with status: " + response.statusCode();
            if (response.body() != null) {
                try {
                    JsonObject errorJson = JsonParser.parseString(response.body()).getAsJsonObject();
                    errorMsg += " - " + errorJson.get("error_description").getAsString();
                } catch (Exception e) {
                    errorMsg += " - " + response.body();
                }
            }
            String finalErrorMsg = errorMsg;
            Platform.runLater(() -> onError.accept(finalErrorMsg));
        }

        /**
         * Makes a cryptographically secure PKCE code verifier.
         * Creates a random value using SHA-256
         *
         * @return Base64URL-encoded code verifier
         */
        private void handleTokenException(Exception e) {
            String errorMsg = "Token acquisition failed: ";
            if (e.getMessage() != null) {
                errorMsg += e.getMessage();
            } else {
                errorMsg += "Unknown error - " + e.getClass().getName();
                // Add more details for common exceptions
                if (e instanceof NullPointerException) {
                    errorMsg += " (Null pointer exception - likely missing configuration)";
                } else if (e instanceof java.net.ConnectException) {
                    errorMsg += " (Connection failed - check network/internet access)";
                } else if (e instanceof java.net.UnknownHostException) {
                    errorMsg += " (Unknown host - check your token URL)";
                }
            }

            String finalErrorMsg = errorMsg;
            Platform.runLater(() -> onError.accept(finalErrorMsg));
        }

        private Map<String, String> parseQuery(String query) {
            return Arrays.stream(query.split("&"))
                    .map(param -> param.split("="))
                    .filter(pair -> pair.length > 1)
                    .collect(Collectors.toMap(
                            pair -> pair[0],
                            pair -> pair[1],
                            (first, second) -> first));
        }

        private void sendSuccessResponse(HttpExchange exchange) throws IOException {
            // Sends success response to browser
            String response = "<html><body>Authentication successful! You may close this window.</body></html>";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }

        /**
         * Error management for query
         * @param exchange
         * @param message
         * @throws IOException
         */
        private void sendErrorResponse(HttpExchange exchange, String message) throws IOException {
            String response = "<html><body>Error: " + message + "</body></html>";
            exchange.sendResponseHeaders(400, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
        /**
         * Generates cryptographically secure PKCE code verifier.
         * Creates a high-entropy random value using SHA-256 as per RFC 7636.
         *
         * @return Base64URL-encoded code verifier
         */
        private String generateCodeVerifier() {
            SecureRandom secureRandom = new SecureRandom();
            byte[] codeVerifier = new byte[32];
            secureRandom.nextBytes(codeVerifier);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier);
        }

        /**
         * Derives PKCE code challenge from verifier using S256 transformation.
         * Implements the code_challenge_method specified in RFC 7636.
         *
         * @param codeVerifier Original PKCE verifier value
         * @return Transformed code challenge
         * @throws UnsupportedEncodingException If ASCII encoding fails
         * @throws NoSuchAlgorithmException If SHA-256 algorithm unavailable
         */
        private String generateCodeChallenge(String codeVerifier) throws UnsupportedEncodingException, NoSuchAlgorithmException {
            byte[] bytes = codeVerifier.getBytes("US-ASCII");
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(bytes, 0, bytes.length);
            byte[] digest = messageDigest.digest();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        }
    }

    /**
     * Handles GitHub OAuth 2.0 authentication
     * Manages the complete login flow from browser redirect to user data retrieval,
     * including email address verification through GitHub's specific APIs.
     * As reflective of Google's methodology is in GitHub. Email Address verification is the major difference.
     */
    public static class GithubAuthHandler {
        private static final String CLIENT_ID = dotenv.get("GITHUB_CLIENT_ID");
        private static final String CLIENT_SECRET = dotenv.get("GITHUB_CLIENT_SECRET");
        private static final String REDIRECT_URI = "http://localhost:8083/auth/github/callback";
        private static final String SCOPE = "user:email";
        private static final String AUTH_URL = "https://github.com/login/oauth/authorize";
        private static final String TOKEN_URL = "https://github.com/login/oauth/access_token";
        private static final String USER_API_URL = "https://api.github.com/user";
        private static final String EMAILS_API_URL = "https://api.github.com/user/emails";

        private final Consumer<Map<String, String>> onUserData;
        private final HostServices hostServices;
        private final Consumer<String> onError;
        private final Runnable onSuccess;

        /**
         * Sets up authentication handler with success/error callbacks.
         * @param onUserData Receives user profile (email, name) on success
         * @param onSuccess Runs after successful authentication
         * @param onError Handles error messages
         * @param hostServices JavaFX browser opener
         */
        public GithubAuthHandler(Consumer<Map<String, String>> onUserData, Runnable onSuccess,
                                 Consumer<String> onError, HostServices hostServices) {
            this.onUserData = onUserData;
            this.onSuccess = onSuccess;
            this.onError = onError;
            this.hostServices = hostServices;
        }

        /**
         * Starts GitHub login process:
         * 1. Opens browser to GitHub's auth page
         * 2. Listens for callback on localhost
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
         * Builds GitHub authorization URL with required OAuth parameters.
         * Includes requested permissions (scope) and callback location.
         */
        @NotNull
        private String buildAuthUrl() throws UnsupportedEncodingException {
            return AUTH_URL + "?client_id=" + CLIENT_ID +
                    "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                    "&scope=" + URLEncoder.encode(SCOPE, StandardCharsets.UTF_8) +
                    "&response_type=code";
        }


        private void openBrowser(String url) {
            hostServices.showDocument(url);
        }

        /**
         * Creates an HTTP server to get authorization responses.
         * Automatically ends after processing the OAuth callback.
         */
        private void startCallbackServer() {
            new Thread(() -> {
                try {
                    HttpServer server = HttpServer.create(new InetSocketAddress(8083), 0);
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
         * Handles GitHub's callback after user login:
         * 1. Exchanges code for access token
         * 2. Fetches user profile and primary email
         * 3. Triggers success/error callbacks
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
                String tokenResponse = exchangeCodeForTokens(code);
                JsonObject tokenJson = JsonParser.parseString(tokenResponse).getAsJsonObject();
                String accessToken = tokenJson.get("access_token").getAsString();

                String userInfo = getUserInfo(accessToken);
                JsonObject userJson = JsonParser.parseString(userInfo).getAsJsonObject();

                // Extract user data from OAuth response
                String name = userJson.has("name") ? userJson.get("name").getAsString() : "";
                String[] nameParts = name.split(" ", 2);
                String firstName = nameParts.length > 0 ? nameParts[0] : "";
                String lastName = nameParts.length > 1 ? nameParts[1] : "";
                String email = getPrimaryEmail(accessToken);

                Map<String, String> userData = new HashMap<>();
                userData.put("email", email);
                userData.put("firstName", firstName);
                userData.put("lastName", lastName);

                // FIRST call onUserData with the user info
                if (onUserData != null) {
                    onUserData.accept(userData);
                }

                // THEN call onSuccess
                if (onSuccess != null) {
                    Platform.runLater(onSuccess);
                }

                sendSuccessResponse(exchange);

            } catch (Exception e) {
                sendErrorResponse(exchange, "Authentication failed");
                onError.accept("Github login failed: " + e.getMessage());
            }
        }

        /**
         * Gets user's primary email from GitHub's email API.
         * Required because profile endpoint may not return email.
         */
        private String getPrimaryEmail(String accessToken) throws IOException, InterruptedException {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(EMAILS_API_URL))
                    .header("Authorization", "token " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonArray emails = JsonParser.parseString(response.body()).getAsJsonArray();

            // Find the primary email
            for (JsonElement emailElement : emails) {
                JsonObject emailObj = emailElement.getAsJsonObject();
                if (emailObj.get("primary").getAsBoolean()) {
                    return emailObj.get("email").getAsString();
                }
            }
            return "";
        }

        /**
         * Exchanges temporary code for long-lived access token.
         * Uses client secret for added security.
         */
        private String exchangeCodeForTokens(String code) throws IOException, InterruptedException {
            // Exchanges authorization code for access token
            String params = "code=" + code +
                    "&client_id=" + CLIENT_ID +
                    "&client_secret=" + CLIENT_SECRET +
                    "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TOKEN_URL))
                    .header("Accept", "application/json")  // GitHub returns JSON if you ask for it
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(params))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        }

        /**
         * Fetches basic user profile (name, login) from GitHub API.
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



        @NotNull
        private Map<String, String> parseQuery(String query) {
            // Parses URL query string into key-value pairs
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

        private void sendSuccessResponse(@NotNull HttpExchange exchange) throws IOException {
            String response = "<html><body>GitHub login successful! You can close this window.</body></html>";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }

        private void sendErrorResponse(@NotNull HttpExchange exchange, String message) throws IOException {
            String response = "<html><body>Error: " + message + "</body></html>";
            exchange.sendResponseHeaders(400, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
}

