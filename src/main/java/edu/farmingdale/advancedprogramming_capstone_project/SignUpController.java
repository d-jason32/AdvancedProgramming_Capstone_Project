package edu.farmingdale.advancedprogramming_capstone_project;

import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;

import static edu.farmingdale.advancedprogramming_capstone_project.LoginController.onLoginSuccess;

/**
 * Handles user registration through both manual form entry and OAuth providers.
 * Manages account creation, validation, and integration with database.
 */
public class SignUpController implements Initializable {
    private ConnDbOps cdbop;
    public Text errorTextPlaceholder;
    public List<String> authDB;

    private static Stage authStage = new Stage();

    public TextField firstName;
    public TextField lastName;
    public TextField emailField;
    public PasswordField passwordField;
    public PasswordField confirmPasswordField;

    public Text stateLink;
    public Button enterButton;
    public static HostServices hostServices;
    private Runnable onSuccessCallback;

    /**
     * Initializes database connection and authentication service
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cdbop = new ConnDbOps();
        cdbop.connectToDatabase();
        ConnDbOps.AuthService.initializeAuthDB(cdbop);
        authDB = ConnDbOps.AuthService.getAuthDB();
    }

    public void setOnSuccess(Runnable callback) {
        this.onSuccessCallback = callback;
        System.out.println("SignUp callback set: " + (callback != null));
    }

    public static void setHostServices(HostServices services) {
        hostServices = services;
    }

    /** Starts Sign Up process on a thread
     * @param onSignUpSuccess Runnable
     */
    public void setSignUpSuccess(Runnable onSignUpSuccess){
        this.onSuccessCallback = onSignUpSuccess;
    }

    /**
     * Handles Microsoft OAuth registration flow
     * @param event Button click event
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
                                String.valueOf(authDB.size() + 1),
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
     * Handles Google OAuth registration flow
     * @param actionEvent Button click event
     */
    public void onGoogleButtonPress(ActionEvent actionEvent) {
        if (hostServices == null) {
            errorTextPlaceholder.setText("Browser services not available");
            return;
        }

        new OAuthService.GoogleAuthHandler(
                userData -> {
                    String email = userData.get("email");
                    String firstName = userData.get("firstName");
                    String lastName = userData.get("lastName");
                    handleOAuthSignUp(firstName, lastName, email);
                },
                error -> Platform.runLater(() -> errorTextPlaceholder.setText(error)),
                hostServices
        ).startAuthentication();
    }

    /**
     * Handles GitHub OAuth registration flow
     * @param actionEvent Button click event
     */
    @FXML
    public void onGithubButtonPress(ActionEvent actionEvent) throws IOException {
        if (hostServices == null) {
            errorTextPlaceholder.setText("Browser services not available");
            return;
        }
        new OAuthService.GithubAuthHandler(
                userData -> {
                    Platform.runLater(() -> {
                        String email = userData.get("email");
                        String firstName = userData.get("firstName");
                        String lastName = userData.get("lastName");
                        handleOAuthSignUp(firstName, lastName, email);
                    });
                },
                () -> {
                    // This runs after handleOAuthSignUp completes
                    if (onLoginSuccess != null) {
                        Platform.runLater(onLoginSuccess);
                    }
                },
                error -> Platform.runLater(() -> errorTextPlaceholder.setText(error)),
                hostServices
        ).startAuthentication();
    }

    /**
     * Processes OAuth signups by creating new accounts or logging in existing users
     * @param firstName User's first name from OAuth provider
     * @param lastName User's last name from OAuth provider
     * @param email User's email from OAuth provider
     */
    public void handleOAuthSignUp(String firstName, String lastName, String email) {
        System.out.println("HandleOAuthSignUp Called");
        if (!isAccountExisting(email, authDB)) {
            String generatedPassword = generateRandomPassword();
            //Creates new user using parameters from OAuth extraction, it is shown when handleOAuthSignUp is called
            cdbop.insertUser(
                    String.valueOf(ConnDbOps.AuthService.lastID + 1),
                    firstName,
                    lastName,
                    email,
                    generatedPassword
            );
            ConnDbOps.AuthService.initializeAuthDB(cdbop);
            authDB = ConnDbOps.AuthService.getAuthDB();

            if (onSuccessCallback != null) {
                onSuccessCallback.run();
            }
        } else {
            if (onSuccessCallback != null) {
                onSuccessCallback.run();
            }
        }
    }

    /**
     * Generates a secure random password fulfilling regex requirements
     * The purpose is so OAuth accounts can link to the database in the same way as manuel users can do so. We need a password
     * to make sure they will fill all parameters that need to be filled when inserting into the Database.
     * @return Generated password string
     */
    static String generateRandomPassword() {
        String upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCase = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String specialChars = "@$!%*?&";
        String allChars = upperCase + lowerCase + numbers + specialChars;

        Random random = new Random();
        StringBuilder password = new StringBuilder();

        // Ensure at least one character from each required category
        password.append(upperCase.charAt(random.nextInt(upperCase.length())));
        password.append(lowerCase.charAt(random.nextInt(lowerCase.length())));
        password.append(numbers.charAt(random.nextInt(numbers.length())));
        password.append(specialChars.charAt(random.nextInt(specialChars.length())));

        // Fill the rest with random characters from all categories
        for (int i = 4; i < 16; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // Shuffle the characters to make the password more random
        char[] passwordArray = password.toString().toCharArray();
        for (int i = 0; i < passwordArray.length; i++) {
            int randomIndex = random.nextInt(passwordArray.length);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[randomIndex];
            passwordArray[randomIndex] = temp;
        }

        return new String(passwordArray);
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
     * Validates registration form inputs against regex rules
     * @param fn First name
     * @param ln Last name
     * @param email Email address
     * @param password Password
     * @param confirmPassword Password confirmation
     * @return true if all validations pass
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
    /**
     * Checks if email already exists in system
     * @param email Email to check
     * @param authDB List of existing emails
     * @return true if email exists
     */
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
    /**
     * Processes manual form registration when enter button pressed
     * @param actionEvent Button click event
     */
    public void onEnterButtonPress(ActionEvent actionEvent) {
        cdbop.listAllUsers();
        ConnDbOps.AuthService.initializeAuthDB(cdbop);
        authDB = ConnDbOps.AuthService.getAuthDB();
        String fn = firstName.getText().trim();
        String ln = lastName.getText().trim();
        String username = emailField.getText().trim();
        String password = passwordField.getText().trim();
        if(!isAccountExisting(username, authDB) && isValidCredentials(fn, ln, username, password, confirmPasswordField.getText())){
            cdbop.insertUser(String.valueOf(ConnDbOps.AuthService.lastID + 1),fn, ln, emailField.getText(), passwordField.getText());
            ConnDbOps.AuthService.initializeAuthDB(cdbop);
            authDB = ConnDbOps.AuthService.getAuthDB();
        }
    }
}
