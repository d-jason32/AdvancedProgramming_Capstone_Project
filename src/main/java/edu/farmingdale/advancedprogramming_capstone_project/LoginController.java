package edu.farmingdale.advancedprogramming_capstone_project;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.application.HostServices;

import javafx.stage.Stage;

//env support
import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * The LoginController class is responsible for managing the login process in the application.
 * It handles user interaction for login, sign-up, password reset, and supports login
 * via third-party providers like Microsoft, Google, and GitHub. This class also facilitates
 * database interaction and manages state changes related to authentication.
 */
public class LoginController implements Initializable {

    private static Stage authStage = new Stage();
    // Database connection and auth service
    private ConnDbOps cdbop;
    private List<String> authDB; // Stores registered emails

    public static HostServices hostServices; // For browser integration
    static Runnable onLoginSuccess; //Runs when Authentication is successful
    Runnable mainScreenCallback; //Main app callback
    private Runnable devModeCallback;

    static Dotenv dotenv = Dotenv.load();

    // FXML components
    @FXML
    public Text stateLink;
    @FXML
    public Label stateText;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField usernameField;
    @FXML
    private Text errorTextPlaceholder;

    /**
     * Starts Login Page with initializing ConnOpsDb and making sure that the communication layer (AuthService) is also initialized.
     * The purpose of AuthService is for ease of retrieving user information. As Lists are easier to retrieve information from than converting string values from ConnDbOps
     * each time you want a value.
     * @param url
     * @param resourceBundle
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cdbop = new ConnDbOps();
        cdbop.connectToDatabase();
        ConnDbOps.AuthService.initializeAuthDB(cdbop);
        cdbop.listAllUsers();
        authDB = ConnDbOps.AuthService.getAuthDB();
        this.mainScreenCallback = CapstoneApp.getMainScreenCallback();
        System.out.println("Login callback initialized: " + (mainScreenCallback != null));
    }

    /**
     * Function is called when authentication is deemed successful so the User can transition to the next page.
     * @param onLoginSuccess
     */
    public static void setOnLoginSuccess(Runnable onLoginSuccess) {
        LoginController.onLoginSuccess = onLoginSuccess;
    }

    /**
     * For development purposes to ease getting into the main application.
     * @param event ActionEvent
     */
    @FXML
    void onDevButtonPressed(ActionEvent event) {
        dotenv.entries().forEach(entry ->
                System.out.println(entry.getKey() + "=" + entry.getValue())
        );

        // Try main callback first, then static onLoginSuccess as fallback
        if (mainScreenCallback != null) {
            Platform.runLater(mainScreenCallback);
        } else if (onLoginSuccess != null) {
            Platform.runLater(onLoginSuccess);
        } else {
            System.err.println("No dev mode callback available");
        }
    }

    //Reads input from email and password input fields and preforms operations to ensure it works

    /** Enter Button checks cases in which passwords or emails may or may not be valid.
     * @param event ActionEvent
     */
    @FXML
    void onEnterButtonPress(ActionEvent event) {
        String email = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty()) {
            errorTextPlaceholder.setText("Please enter your email address");
            return;
        }

        if (password.isEmpty()) {
            errorTextPlaceholder.setText("Please enter your password");
            return;
        }

        boolean authenticated = false;
        for (String s : authDB) {
            if (authDB.contains(email)) {
                authenticated = password.equals(cdbop.queryPasswordByEmail(email));
                break;
            }
        }
        System.out.println(mainScreenCallback + " " + authenticated);

        if (authenticated) {
            Platform.runLater(mainScreenCallback);
        } else {
            Platform.runLater(() ->
                    errorTextPlaceholder.setText("Invalid username or password"));
        }
    }


    /** Allows browser functionality
     * @param hostServices HostServices
     */
    public static void setHostServices(HostServices hostServices) {
        LoginController.hostServices = hostServices;
    }

    /**
     * Handles Microsoft authentication button press event.
     * Starts OAuth 2.0 flow with Microsoft OAuth provider.
     *
     * Creates new user and inserts in database if checks are fulfilled or just logs in if email is found
     */
    @FXML
    private void onMicrosoftButtonPress(ActionEvent event) {
        if (hostServices == null) {
            errorTextPlaceholder.setText("Browser services not available");
            return;
        }

        new OAuthService.MicrosoftAuthHandler(
                userData -> {
                    // Handle successful authentication
                    String email = userData.get("email");
                    if (!authDB.contains(email)) {
                        // Create new account
                        String firstName = userData.get("firstName");
                        String lastName = userData.get("lastName");
                        String password = SignUpController.generateRandomPassword();

                        cdbop.insertUser(
                                String.valueOf(ConnDbOps.AuthService.lastID + 1),
                                firstName,
                                lastName,
                                email,
                                password
                        );
                        ConnDbOps.AuthService.initializeAuthDB(cdbop);
                        authDB = ConnDbOps.AuthService.getAuthDB();
                    }

                    if (onLoginSuccess != null) {
                        Platform.runLater(onLoginSuccess);
                    }
                },
                () -> {
                    // Success callback
                    if (onLoginSuccess != null) {

                        Platform.runLater(onLoginSuccess);
                    }
                },
                error -> Platform.runLater(() -> errorTextPlaceholder.setText(error)),
                hostServices
        ).startAuthentication();
    }


    /**
     * Handles Google authentication button press event.
     * Starts OAuth 2.0 flow with Google identity provider.
     *
     * Creates new user and inserts in database if checks are fulfilled or just logs in if email is found
     */
    @FXML
    private void onGoogleButtonPress(ActionEvent event) {
        if (hostServices == null) {
            errorTextPlaceholder.setText("Browser services not available");
            return;
        }

        new OAuthService.GoogleAuthHandler(
                userData -> {
                    // Handle successful authentication
                    String email = userData.get("email");
                    if (!authDB.contains(email)) {
                        // Create new account
                        String firstName = userData.get("firstName");
                        String lastName = userData.get("lastName");
                        String password = SignUpController.generateRandomPassword();

                        cdbop.insertUser(
                                String.valueOf(ConnDbOps.AuthService.lastID + 1),
                                firstName,
                                lastName,
                                email,
                                password
                        );
                        ConnDbOps.AuthService.initializeAuthDB(cdbop);
                        authDB = ConnDbOps.AuthService.getAuthDB();
                    }

                    if (onLoginSuccess != null) {
                        Platform.runLater(onLoginSuccess);
                    }
                },
                error -> Platform.runLater(() -> errorTextPlaceholder.setText(error)),
                hostServices
        ).startAuthentication();
    }

    /**
     * Handles GitHub authentication button press event.
     * Starts OAuth 2.0 flow with GitHub OAuth provider.
     *
     * Creates new user and inserts in database if checks are fulfilled or just logs in if email is found
     * @throws IOException If there's an error loading the authentication view
     */
    @FXML
    private void onGithubButtonPress(ActionEvent event) throws IOException {
        if (hostServices == null) {
            errorTextPlaceholder.setText("Browser services not available");
            return;
        }

        new OAuthService.GithubAuthHandler(
                userData -> {
                    String email = userData.get("email");
                    if (!authDB.contains(email)) {
                        String firstName = userData.get("firstName");
                        String lastName = userData.get("lastName");
                        String password = SignUpController.generateRandomPassword();

                        cdbop.insertUser(
                                String.valueOf(ConnDbOps.AuthService.lastID + 1),
                                firstName,
                                lastName,
                                email,
                                password
                        );
                        ConnDbOps.AuthService.initializeAuthDB(cdbop);
                        authDB = ConnDbOps.AuthService.getAuthDB();
                    }
                },
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
     * Handles navigation to the sign-up view.
     *
     * @throws IOException If there's an error loading the sign-up view
     */
    @FXML
    public void onSignUpTextPressed(MouseEvent event) throws IOException {
        Node source = (Node) event.getSource();
        Stage currentStage = (Stage) source.getScene().getWindow();

        FXMLLoader signUpLoader = new FXMLLoader(getClass().getResource("signup-screen.fxml"));
        Parent signUpRoot = signUpLoader.load();
        SignUpController signUpController = signUpLoader.getController();

        // Pass the callback from CapstoneApp
        signUpController.setOnSuccess(CapstoneApp.getMainScreenCallback());
        SignUpController.setHostServices(hostServices);

        authStage.setScene(new Scene(signUpRoot));
        authStage.setTitle("AI Whiteboard Teaching Tool - Sign Up");
        authStage.show();

        // Close the current (login) window
        currentStage.close();
    }


    /**
     * Leads to Reset Password Page
     * @param event
     * @throws IOException
     */
    @FXML
    void onPasswordResetPressed(MouseEvent event) throws IOException {
        Node source = (Node) event.getSource();
        Stage currentStage = (Stage) source.getScene().getWindow();

        FXMLLoader resetPwLoader = new FXMLLoader(getClass().getResource("password-reset-screen.fxml"));
        Parent resetPwRoot = resetPwLoader.load();
        ResetPasswordController resetPasswordController = resetPwLoader.getController();
        resetPasswordController.setMainScreenCallback(CapstoneApp.getMainScreenCallback());
        ResetPasswordController.setHostServices(hostServices);

        Stage loginStage = new Stage();
        loginStage.setScene(new Scene(resetPwRoot));
        loginStage.setTitle("AI Whiteboard Teaching Tool - Reset Password");
        loginStage.show();

        currentStage.close();
    }

    /**
     * Registers a callback to be executed upon successful authentication.
     * This callback handles navigation to the application's main screen.
     *
     * @param callback The Runnable to execute after successful login.
     *                 Will be called on the JavaFX Application Thread.
     */
    public void setMainScreenCallback(Runnable callback) {
        this.mainScreenCallback = callback;
        System.out.println("Callback set: " + callback);
    }


}
