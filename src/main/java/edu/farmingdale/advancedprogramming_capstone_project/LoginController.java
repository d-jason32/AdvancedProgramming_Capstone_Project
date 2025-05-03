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
import org.jetbrains.annotations.NotNull;
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

/**
 * The LoginController class is responsible for managing the login process in the application.
 * It handles user interaction for login, sign-up, password reset, and supports login
 * via third-party providers like Microsoft, Google, and GitHub. This class also facilitates
 * database interaction and manages state changes related to authentication.
 */
public class LoginController implements Initializable  {
    private ProfileConnDbOps cdbop;
    private List<String> databaseLoginInfo;
    private List<testUser> testDB = new ArrayList<>();
    public HostServices hostServices;
    private Runnable onLoginSuccess;

    static Dotenv dotenv = Dotenv.load();

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


    /**
     * @param url
     * The location used to resolve relative paths for the root object, or
     * {@code null} if the location is not known.
     * @param resourceBundle
     * The resources used to localize the root object, or {@code null} if
     * the root object was not localized.
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cdbop = new ProfileConnDbOps();
        cdbop.connectToDatabase();
        databaseLoginInfo = cdbop.displayAllUsers();
    }

    /**
     * Tester Initializer
     */
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
    /**
     * @param event ActionEvent
     */
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
    /**
     * @param event ActionEvent
     */
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

    /**
     * @param hostServices HostServices
     */
    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    //Starts authentication via Microsoft
    /**
     * @param event ActionEvent
     */
    @FXML
    private void onMicrosoftButtonPress(ActionEvent event) {
        // Add null check for hostServices
        if (hostServices == null) {
            errorTextPlaceholder.setText("Browser services not available");
        }

        new OAuthService.MicrosoftAuthHandler(
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
    /**
     * @param event ActionEvent
     */
    @FXML
    private void onGoogleButtonPress(ActionEvent event) {
        // Add null check for hostServices
        if (hostServices == null) {
            errorTextPlaceholder.setText("Browser services not available");
        }

        new OAuthService.GoogleAuthHandler(
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

    //Starts authentication via GitHub
    /**
     * @param event ActionEvent
     */
    @FXML
    private void onGithubButtonPress(ActionEvent event) throws IOException {
        if (hostServices == null) {
            errorTextPlaceholder.setText("Browser services not available");
        }

        new OAuthService.GithubAuthHandler(
                () -> {
                    if (onLoginSuccess != null) {
                        Platform.runLater(onLoginSuccess);
                    }
                },
                error -> Platform.runLater(() -> errorTextPlaceholder.setText(error)),
                hostServices
        ).startAuthentication();
    }
    
    /**
     * @param event MouseEvent
     */
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

    /**
     * @param callback Runnable
     */
    public void setOnLoginSuccess(Runnable callback){
        this.onLoginSuccess = callback;
    }

    /**
     * @param event ActionEvent
     * @throws IOException IOException
     */
    @FXML
    void onPasswordResetPressed(ActionEvent event) throws IOException {
        FXMLLoader passwordResetLoader = new FXMLLoader(getClass().getResource("password-reset-screen.fxml"));
        Parent mainRoot = passwordResetLoader.load();


        // Switch to the main screen
        Stage passwordResetStage = new Stage();
        passwordResetStage.setScene(new Scene(mainRoot, 1280, 800));
        passwordResetStage.setTitle("AI Whiteboard Program - Reset Password");
    }

    /**
     * @param mouseEvent MouseEvent
     */
    @FXML
    public void onSignUpTextPressed(MouseEvent mouseEvent) {
    }

    /**
     * @param mouseEvent MouseEvent
     */
    public void onPasswordResetPressed(MouseEvent mouseEvent) {
    }

    /**
     * Represents a user for authentication purposes.
     * This class handles storing a hashed password, providing authentication functionality, 
     * and managing user email generation and password resets.
     */
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

}