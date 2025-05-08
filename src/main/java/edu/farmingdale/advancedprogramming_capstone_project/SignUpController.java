package edu.farmingdale.advancedprogramming_capstone_project;

import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;



public class SignUpController implements Initializable {
    private ConnDbOps cdbop;
    public Text errorTextPlaceholder;
    public List<String> authDB;

    private static Stage authStage = new Stage();

    public TextField firstName;
    public TextField lastName;
    public TextField usernameField;
    public TextField passwordField;
    public TextField confirmPasswordField;

    public Text stateLink;
    public Button enterButton;
    public HostServices hostServices;
    private Runnable onSignUpSuccess;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cdbop = new ConnDbOps();
        cdbop.connectToDatabase();
        ConnDbOps.AuthService.initializeAuthDB(cdbop);
        authDB = ConnDbOps.AuthService.getAuthDB();
    }

    public void setOnSignUpSuccess(Runnable callback) {
        this.onSignUpSuccess = callback;
        System.out.println("SignUp callback set: " + (callback != null));
    }

    /** Starts Sign Up process on a thread
     * @param onSignUpSuccess Runnable
     */
    public void setSignUpSuccess(Runnable onSignUpSuccess){
        this.onSignUpSuccess = onSignUpSuccess;
    }

    public void onMicrosoftButtonPress(ActionEvent actionEvent) {
        // Add null check for hostServices
        if (hostServices == null) {
            errorTextPlaceholder.setText("Browser services not available");
        }

        new OAuthService.MicrosoftAuthHandler(
                () -> {
                    // This runs when authentication succeeds
                    if (onSignUpSuccess != null) {
                        Platform.runLater(onSignUpSuccess);
                    }
                },
                error -> Platform.runLater(() -> errorTextPlaceholder.setText(error)),
                hostServices
        ).startAuthentication();
    }

    public void onGoogleButtonPress(ActionEvent actionEvent) {
        // Add null check for hostServices
        if (hostServices == null) {
            errorTextPlaceholder.setText("Browser services not available");
        }

        new OAuthService.GoogleAuthHandler(
                () -> {
                    // This runs when authentication succeeds
                    if (onSignUpSuccess != null) {
                        Platform.runLater(onSignUpSuccess);
                    }
                },
                error -> Platform.runLater(() -> errorTextPlaceholder.setText(error)),
                hostServices
        ).startAuthentication();
    }

    public void onGithubButtonPress(ActionEvent actionEvent) throws IOException {
        if (hostServices == null) {
            errorTextPlaceholder.setText("Browser services not available");
        }

        new OAuthService.GithubAuthHandler(
                () -> {
                    if (onSignUpSuccess != null) {
                        Platform.runLater(onSignUpSuccess);
                    }
                },
                error -> Platform.runLater(() -> errorTextPlaceholder.setText(error)),
                hostServices
        ).startAuthentication();
    }

    public void onSignInTextPressed(MouseEvent event) throws IOException {
        Node source = (Node) event.getSource();
        Stage currentStage = (Stage) source.getScene().getWindow();

        FXMLLoader signInLoader = new FXMLLoader(getClass().getResource("login-screen.fxml"));
        Parent signInRoot = signInLoader.load();
        LoginController loginController = signInLoader.getController();

        LoginController.setOnLoginSuccess(CapstoneApp.getMainScreenCallback());
        LoginController.setHostServices(hostServices);
        // Switch to the main screen
        authStage.setScene(new Scene(signInRoot));
        authStage.setTitle("AI Whiteboard Teaching Tool Login");
        authStage.show();
        currentStage.close();
    }

    /**
     * Start regex to process credentials
     * @param fn
     * @param ln
     * @param email
     * @param password
     * @param confirmPassword
     * @return
     */
    private boolean isValidCredentials(String fn, String ln, String email, String password, String confirmPassword){
        if (email.isEmpty() || password.isEmpty() || fn.isEmpty() || ln.isEmpty() || confirmPassword.isEmpty()) {
            errorTextPlaceholder.setText("Please fill in all text fields");
            return false;
        }
        if(!fn.matches("^[a-zA-Z]{2,25}$")){
            errorTextPlaceholder.setText("First Name must be 2-25 characters");
        }
        if(!ln.matches("^[a-zA-Z]{2,25}$")){
            errorTextPlaceholder.setText("Last Name must be 2-25 characters");
        }

        if (!email.matches("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@" + "(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$")) {
            errorTextPlaceholder.setText("Must be valid email");
            return false;
        }

        if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$")) {
            errorTextPlaceholder.setText("Password must contain 8+ chars with: uppercase, lowercase, number, and special character");
            return false;
        }
        if(!password.equals(confirmPassword)){
            errorTextPlaceholder.setText("Passwords Do Not Match");
            return false;
        }

        errorTextPlaceholder.setText("Successfully created Username and Password.");
        System.out.println("Account Valid!");
        return true;

    }

    private boolean isAccountExisting(String email, List<String> authDB) {
        for (String s : authDB) {
            if (s.equals(email)) {
                errorTextPlaceholder.setText("Account already exists");
                System.out.println("Account Exists!");
                return true;
            }
        }
        return false;
    }

    public void onEnterButtonPress(ActionEvent actionEvent) {
        cdbop.listAllUsers();
        ConnDbOps.AuthService.initializeAuthDB(cdbop);
        authDB = ConnDbOps.AuthService.getAuthDB();
        String fn = firstName.getText().trim();
        String ln = lastName.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        if(!isAccountExisting(username, authDB) && isValidCredentials(fn, ln, username, password, confirmPasswordField.getText())){
            cdbop.insertUser(String.valueOf(authDB.size() + 1),fn, ln, usernameField.getText(), passwordField.getText());
            ConnDbOps.AuthService.initializeAuthDB(cdbop);
            authDB = ConnDbOps.AuthService.getAuthDB();
        }
    }

    public void setHostServices(HostServices hostServices) {
    }
}
