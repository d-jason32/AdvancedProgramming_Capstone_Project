package edu.farmingdale.advancedprogramming_capstone_project;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.application.HostServices;

//Google Cloud Service Imports
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

//Microsoft Entra ID Imports
import com.microsoft.aad.msal4j.*;

//Password Hashing
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

//env support
import io.github.cdimascio.dotenv.Dotenv;

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
import java.util.*;
import java.util.function.Consumer;




public class LoginController implements Initializable  {
    private ProfileConnDbOps cdbop;
    private List<String> databaseLoginInfo;
    private List<testUser> testDB = new ArrayList<>();
    public HostServices hostServices;
    private Runnable onLoginSuccess;

    private static Dotenv dotenv = Dotenv.load();

    // testertester tester123456

    // FXML components
    @FXML
    public Text stateLink;
    @FXML
    public Label stateText;
    @FXML
    private TextField passwordField;
    @FXML
    private TextField usernameField;
    @FXML
    private Text errorTextPlaceholder;

    //Tester Code
    @FXML private TextField resetEmailField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Text resetMessage;
    @FXML private Button resetButton;
    @FXML private Button backToLoginButton;


    private int state = 0;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cdbop = new ProfileConnDbOps();
        cdbop.connectToDatabase();
        databaseLoginInfo = cdbop.displayAllUsers();
    }

    public void initializeTestDB() {
        testDB.add(new testUser("tester", "12345"));
        testDB.add(new testUser("admin", "admin123"));
        for (int i = 0; i < databaseLoginInfo.size(); i += 2) {
            String username = databaseLoginInfo.get(i);
            String password = databaseLoginInfo.get(i + 1);
            testDB.add(new testUser(username, password));
        }
    }

    //Bypass Login for Testing
    @FXML
    void onDevButtonPressed(ActionEvent event) {
        // Bypass authentication and load the main program
        dotenv.entries().forEach(entry ->
                System.out.println(entry.getKey() + "=" + entry.getValue())
        );
        if (onLoginSuccess != null) {
            Platform.runLater(onLoginSuccess); // Ensure JavaFX thread safety
        }
    }

    //Reads input from email and password input fields and preforms operations to ensure it works
    @FXML
    void onEnterButtonPress(ActionEvent event) {
        databaseLoginInfo = cdbop.displayAllUsers();
        initializeTestDB();
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            errorTextPlaceholder.setText("Username and password cannot be empty");
            return;
        }

        if (!username.matches("^[a-zA-Z0-9]{4,20}$")) {
            errorTextPlaceholder.setText("Username must be 4-20 alphanumeric characters");
            return;
        }

        if (!password.matches("^(?=.*[A-Za-z])(?=.*\\d).{8,}$")) {

            errorTextPlaceholder.setText("Password must be 8+ characters with at least one letter and one number");
            return;
        }

        if (state == 1){
            cdbop.insertUser(usernameField.getText(), passwordField.getText());
            errorTextPlaceholder.setText("Successfully created Username and Password.");
            return;
        }

        boolean authenticated = false;
        for (testUser user : testDB) {
            if (user.authenticate(username, password)) {
                authenticated = true;
                break;
            }
        }

        if (authenticated) {
            if (onLoginSuccess != null) {
                onLoginSuccess.run();
            }
        } else {
            errorTextPlaceholder.setText("Invalid username or password");
        }
    }

    //Sets up the web page functionality
    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    //Starts authentication via Microsoft
    @FXML
    private void onMicrosoftButtonPress(ActionEvent event) {
        // Add null check for hostServices
        if (hostServices == null) {
            errorTextPlaceholder.setText("Browser services not available");
        }

        new MicrosoftAuthHandler(
                () -> {
                    // This runs when authentication succeeds
                    if (onLoginSuccess != null) {
                        Platform.runLater(onLoginSuccess);
                    }
                },
                error -> Platform.runLater(() -> errorTextPlaceholder.setText(error)),
                hostServices
        ).startAuthentication();
    }

    //Starts authentication via Google
    @FXML
    private void onGoogleButtonPress(ActionEvent event) {
        // Add null check for hostServices
        if (hostServices == null) {
            errorTextPlaceholder.setText("Browser services not available");
        }

        new GoogleAuthHandler(
                () -> {
                    // This runs when authentication succeeds
                    if (onLoginSuccess != null) {
                        Platform.runLater(onLoginSuccess);
                    }
                },
                error -> Platform.runLater(() -> errorTextPlaceholder.setText(error)),
                hostServices
        ).startAuthentication();
    }

    //Starts authentication via Github
    @FXML
    private void onGithubButtonPress(ActionEvent event) throws IOException {
        if (hostServices == null) {
            errorTextPlaceholder.setText("Browser services not available");
        }

        new GithubAuthHandler(
                () -> {
                    if (onLoginSuccess != null) {
                        Platform.runLater(onLoginSuccess);
                    }
                },
                error -> Platform.runLater(() -> errorTextPlaceholder.setText(error)),
                hostServices
        ).startAuthentication();
    }


    @FXML
    public void onSignInTextPressed(MouseEvent event) {
        if(state == 0) {
            stateLink.setText("Already have an account? Sign in!");
            stateText.setText("Sign up");
            state = 1;
            System.out.println(state);
        } else {
            stateLink.setText("Don't have an account? Sign up!");
            stateText.setText("Sign in");
            state = 0;
        }
    }

    public void setOnLoginSuccess(Runnable callback){
        this.onLoginSuccess = callback;
    }


    @FXML
    public void onSignUpTextPressed(MouseEvent mouseEvent) {
    }


    public void onPasswordResetPressed(MouseEvent mouseEvent) {
    }




    private static class testUser {
        private String username;
        private String passwordHash;
        private String email;

        public testUser(String username, String password) {
            this.username = username;
            this.passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
            this.email = username + "@example.com";
        }

        public boolean authenticate(String username, String password) {
            return this.username.equals(username)
                    && BCrypt.checkpw(password, this.passwordHash);
        }

        public void resetPassword(String newPassword) {
            this.passwordHash = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        }

        public String getEmail() {
            return email;
        }
    }

    public class GoogleAuthHandler {
        private static final String CLIENT_ID = dotenv.get("GOOGLE_CLIENT_ID");
        private static final String CLIENT_SECRET = dotenv.get("GOOGLE_CLIENT_SECRET");
        private static final String REDIRECT_URI = "http://localhost:8080/auth/google/callback";
        private static final String SCOPE = "email profile";
        private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
        private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

        private final Runnable onSuccess;
        private final Consumer<String> onError;
        private final HostServices hostServices;

        public GoogleAuthHandler(Runnable onSuccess, Consumer<String> onError, HostServices hostServices) {
            this.onSuccess = onSuccess;
            this.onError = onError;
            this.hostServices = hostServices;
        }

        public void startAuthentication() {
            try {
                String authUrl = buildAuthUrl();
                openBrowser(authUrl);
                startCallbackServer();
            } catch (Exception e) {
                onError.accept("Google login error: " + e.getMessage());
            }
        }

        private String buildAuthUrl() throws UnsupportedEncodingException {
            return "https://accounts.google.com/o/oauth2/v2/auth?" +
                    "client_id=" + CLIENT_ID + "&" +
                    "redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) + "&" +
                    "response_type=code&" +
                    "scope=" + URLEncoder.encode(SCOPE, StandardCharsets.UTF_8) + "&" +
                    "access_type=offline&" +
                    "prompt=select_account";
        }

        private void openBrowser(String url) {
            hostServices.showDocument(url);
        }

        @FXML
        void onPasswordResetPressed(ActionEvent event) throws IOException {
            FXMLLoader passwordResetLoader = new FXMLLoader(getClass().getResource("password-reset-screen.fxml"));
            Parent mainRoot = passwordResetLoader.load();


            // Switch to main screen
            Stage passwordResetStage = new Stage();
            passwordResetStage.setScene(new Scene(mainRoot, 1280, 800));
            passwordResetStage.setTitle("AI Whiteboard Program - Reset Password");
        }

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

        private void handleCallback(HttpExchange exchange) throws IOException {
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

                sendSuccessResponse(exchange);
                onSuccess.run();

            } catch (Exception e) {
                sendErrorResponse(exchange, "Authentication failed");
                onError.accept("Google login failed: " + e.getMessage());
            }
        }

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

        private String getUserInfo(String accessToken) throws IOException, InterruptedException {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(USERINFO_URL + "?access_token=" + accessToken))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        }

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

        private void sendSuccessResponse(HttpExchange exchange) throws IOException {
            String response = "<html><body>Login successful! You can close this window.</body></html>";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }

        private void sendErrorResponse(HttpExchange exchange, String message) throws IOException {
            String response = "<html><body>Error: " + message + "</body></html>";
            exchange.sendResponseHeaders(400, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    public class MicrosoftAuthHandler {
        private static final String CLIENT_ID = dotenv.get("MICROSOFT_CLIENT_ID");
        private static final String AUTHORITY = dotenv.get("MICROSOFT_AUTHORITY_ID");
        private static final String REDIRECT_URI = "http://localhost:8080/auth/microsoft/callback";
        private static final String[] SCOPES = {"User.Read"};

        private final Runnable onSuccess;
        private final Consumer<String> onError;
        private final HostServices hostServices;
        private HttpServer server;

        public MicrosoftAuthHandler(Runnable onSuccess, Consumer<String> onError, HostServices hostServices) {
            this.onSuccess = onSuccess;
            this.onError = onError;
            this.hostServices = hostServices;
        }

        public void startAuthentication() {
            try {
                // Start server first
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

        private void handleCallback(HttpExchange exchange) throws IOException {
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
                server.stop(0); // Ensure server stops after handling the request
            }
        }

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

        private void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
            String response = "<html><body>" + message + "</body></html>";
            exchange.sendResponseHeaders(statusCode, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    public class GithubAuthHandler {
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

        public GithubAuthHandler(Runnable onSuccess, Consumer<String> onError, HostServices hostServices) {
            this.onSuccess = onSuccess;
            this.onError = onError;
            this.hostServices = hostServices;
        }

        public void startAuthentication() {
            try {
                String authUrl = buildAuthUrl();
                openBrowser(authUrl);
                startCallbackServer();
            } catch (Exception e) {
                onError.accept("GitHub login error: " + e.getMessage());
            }
        }

        private String buildAuthUrl() throws UnsupportedEncodingException {
            return AUTH_URL + "?client_id=" + CLIENT_ID +
                    "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                    "&scope=" + URLEncoder.encode(SCOPE, StandardCharsets.UTF_8) +
                    "&response_type=code";
        }

        private void openBrowser(String url) {
            hostServices.showDocument(url);
        }

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

        private void handleCallback(HttpExchange exchange) throws IOException {
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

        private void sendSuccessResponse(HttpExchange exchange) throws IOException {
            String response = "<html><body>GitHub login successful! You can close this window.</body></html>";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }

        private void sendErrorResponse(HttpExchange exchange, String message) throws IOException {
            String response = "<html><body>Error: " + message + "</body></html>";
            exchange.sendResponseHeaders(400, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

}