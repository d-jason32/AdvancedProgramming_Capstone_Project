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
    private ConnDbOps cdbop;
    public static HostServices hostServices;
    private static Runnable onLoginSuccess;
    private List<String> authDB;
    private static Stage authStage = new Stage();
    Runnable mainScreenCallback;
    private Runnable devModeCallback;

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

    public static void setOnLoginSuccess(Runnable onLoginSuccess) {
        LoginController.onLoginSuccess = onLoginSuccess;
    }

    /**
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

    /** Enter Button checks cases in which the enter button works
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

        if (authenticated && mainScreenCallback != null) {
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
     * Start Authentication via Microsoft
     * @param event
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

    /**
     * Starts Authentication via Google
     * @param event
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

    /**
     * Start Authentication via Github
     * @param event
     * @throws IOException
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
     * Leads to Sign Up Page
     * @param event
     * @throws IOException
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
        signUpController.setHostServices(hostServices);

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
        ResetPasswordController.setHostServices(hostServices);  // Add this line

        Stage loginStage = new Stage();
        loginStage.setScene(new Scene(resetPwRoot));
        loginStage.setTitle("AI Whiteboard Teaching Tool - Reset Password");
        loginStage.show();

        currentStage.close();
    }

    /**
     * Determines state in which Main is run as a thread
     * @param callback
     */
    public void setMainScreenCallback(Runnable callback) {
        this.mainScreenCallback = callback;
        System.out.println("Callback set: " + callback);
    }


}
