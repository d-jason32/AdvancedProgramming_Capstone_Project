package edu.farmingdale.advancedprogramming_capstone_project;

import com.sun.net.httpserver.HttpServer;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Base64;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Handles password reset functionality including:
 * - Email verification
 * - Reset link generation
 * - Password validation and update
 * Uses SendGrid API for email delivery and local HTTP server for reset link handling
 */
public class ResetPasswordController implements Initializable {

    private static final String SENDGRID_API_KEY = "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";
    private static final String FROM_EMAIL = "no.reply.collaboard@gmail.com";

    private static HostServices hostServices;
    private ConnDbOps cdbop;
    private List<String> authDB;
    private Runnable mainScreenCallback;
    private Runnable devModeCallback;


    @FXML public HBox passwordConfirm;
    @FXML public HBox buttonPlacement;
    @FXML public Text instructionText;
    @FXML public Label stateText;
    @FXML private TextField confidentialField;
    @FXML private Text errorTextPlaceholder;
    @FXML private TextField confidentialField1;

    String email;
    static boolean state;


    public static void setHostServices(HostServices services) {
        hostServices = services;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cdbop = new ConnDbOps();
        cdbop.connectToDatabase();
        ConnDbOps.AuthService.initializeAuthDB(cdbop);
        authDB = ConnDbOps.AuthService.getAuthDB();
        this.mainScreenCallback = CapstoneApp.getMainScreenCallback();
        state = true;
        ResetLinkServer.setController(this);
    }

    /**
     * Manages UI state between email input and password reset modes
     */
    private void passwordInputMode() {
        // Make fields visible and managed
        passwordConfirm.setVisible(true);
        passwordConfirm.setManaged(true);

        // Update UI text
        confidentialField.setPromptText("New Password");
        confidentialField1.setPromptText("Confirm Password");
        instructionText.setText("Please enter your new password");
        stateText.setText("Create New Password");

        // Clear fields
        confidentialField.setText("");
        confidentialField1.setText("");
        errorTextPlaceholder.setText("");

        // Request focus after UI update
        Platform.runLater(() -> {
            confidentialField.requestFocus();

            // Ensure fields are clickable
            confidentialField.setMouseTransparent(false);
            confidentialField1.setMouseTransparent(false);
            confidentialField.setFocusTraversable(true);
            confidentialField1.setFocusTraversable(true);
        });

        // Adjust layout
        buttonPlacement.setPadding(new Insets(60, 0, 0, 0));
    }

    /**
     * Handles form submission for both email verification and password reset
     * @param event Button press event
     */
    @FXML
    void onEnterButtonPress(ActionEvent event) throws SQLException {
        if(state) {
            email = confidentialField.getText().trim();

            if (email.isEmpty()) {
                errorTextPlaceholder.setText("Please enter your email address");
                return;
            }

            if (!authDB.contains(email)) {
                errorTextPlaceholder.setText("This email is not linked to an account");
                return;
            }

            // Generate and send reset token
            String resetToken = UUID.randomUUID().toString();
            EmailService.sendPasswordResetEmail(email, resetToken)
                    .thenAccept(success -> {
                        Platform.runLater(() -> {
                            if (success) {
                                ResetLinkServer.startServer();
                                errorTextPlaceholder.setText("Reset email sent to " + email + ". Please check spam folder.");
                            } else {
                                errorTextPlaceholder.setText("Failed to send email");
                            }
                        });
                    });
        }
        else {
            // Password reset logic
            String password = confidentialField.getText().trim();
            String confirmPassword = confidentialField1.getText().trim();

            // Validate inputs
            if (password.isEmpty() || confirmPassword.isEmpty()) {
                errorTextPlaceholder.setText("Please fill in both fields");
                return;
            }

            if (!password.equals(confirmPassword)) {
                errorTextPlaceholder.setText("Passwords must match");
                return;
            }

            if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$")) {
                errorTextPlaceholder.setText("Password must contain 8+ chars with uppercase, lowercase, number and special character");
                return;
            }

            // Verify user exists before updating
            Person user = cdbop.queryUserByEmail(email);
            if (user == null) {
                errorTextPlaceholder.setText("User account not found");
                return;
            }

            // Update password
            cdbop.editUser(
                    String.valueOf(user.getId()),
                    user.getFirstName(),
                    user.getLastName(),
                    email,
                    password
            );

            errorTextPlaceholder.setText("Password updated successfully!");
        }
    }


    /**
     * Used for testing Purposes
     * @param event
     */
    @FXML
    void onDevButtonPressed(ActionEvent event) {
        if (devModeCallback != null) {
            Platform.runLater(devModeCallback);
        } else if (mainScreenCallback != null) {
            Platform.runLater(mainScreenCallback);
        } else {
            System.err.println("No dev mode callback available");
        }
    }

    /**
     * Used to go to Sign In Page
     * @param event
     * @throws IOException
     */
    @FXML
    private void onSignInTextPressed(MouseEvent event) throws IOException {
        Node source = (Node) event.getSource();
        Stage currentStage = (Stage) source.getScene().getWindow();

        FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("login-screen.fxml"));
        Parent loginRoot = loginLoader.load();

        LoginController loginController = loginLoader.getController();
        loginController.setMainScreenCallback(mainScreenCallback);
        LoginController.setHostServices(hostServices);
        Stage loginStage = new Stage();
        loginStage.setScene(new Scene(loginRoot));
        loginStage.setTitle("AI Whiteboard Teaching Tool - Login");
        loginStage.show();

        currentStage.close();
    }

    /**
     * Used to return to Sign Up page
     * @param event
     * @throws IOException
     */
    @FXML
    private void onSignUpTextPressed(MouseEvent event) throws IOException {
        Node source = (Node) event.getSource();
        Stage currentStage = (Stage) source.getScene().getWindow();

        FXMLLoader signUpLoader = new FXMLLoader(getClass().getResource("signup-screen.fxml"));
        Parent signUpRoot = signUpLoader.load();

        SignUpController signUpController = signUpLoader.getController();
        signUpController.setOnSuccess(mainScreenCallback);
        signUpController.setHostServices(hostServices);
        Stage signUpStage = new Stage();
        signUpStage.setScene(new Scene(signUpRoot));
        signUpStage.setTitle("AI Whiteboard Teaching Tool - Sign Up");
        signUpStage.show();

        currentStage.close();
    }
    private void stateListener() {
        passwordInputMode();
        state = false;
    }

    /**
     * Sets a thread for main screen
     * @param callback Runnable
     */
    public void setMainScreenCallback(Runnable callback) {
        this.mainScreenCallback = callback;
        System.out.println("Main screen callback set in ResetPasswordController: " + callback);
    }

    /**
     * Inner class handles HTTP server for reset links
     * Listens on localhost:8080 for token validation from user pressing the link in the email.
     */
    public class ResetLinkServer {
        private static ResetPasswordController controller;

        public static void setController(ResetPasswordController controller) {
            ResetLinkServer.controller = controller;
        }

        public static void startServer() {
            try {
                HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
                server.createContext("/reset-password", exchange -> {
                    String query = exchange.getRequestURI().getQuery();
                    if (query != null && query.contains("token=")) {
                        Platform.runLater(() -> {
                            controller.stateListener();
                        });
                        String response = "Password reset page will open...";
                        exchange.sendResponseHeaders(200, response.length());
                        exchange.getResponseBody().write(response.getBytes());
                    }
                    exchange.close();
                });
                server.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Inner class for SendGrid email service integration
     * Handles email delivery with tokenized links. These links change the state of the page as seen in the stateListener
     */
    public static class EmailService {
        private static final String SENDGRID_API_URL = "https://api.sendgrid.com/v3/mail/send";
        private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
        public static CompletableFuture<Boolean> sendPasswordResetEmail(String recipientEmail, String resetToken) {
            // Create the reset link
            String resetLink = "http://localhost:8080/reset-password?token=" + resetToken +
                    "&email=" + URLEncoder.encode(recipientEmail, StandardCharsets.UTF_8);

            // Prepare the JSON request body for SendGrid API
            String requestBody = String.format(
                    "{\"personalizations\":[{\"to\":[{\"email\":\"%s\"}]}]," +
                            "\"from\":{\"email\":\"%s\"}," +
                            "\"subject\":\"Password Reset Request\"," +
                            "\"content\":[{\"type\":\"text/html\"," +
                            "\"value\":\"<p>Please click the link below to reset your password:</p>" +
                            "<p><a href='%s'>Reset Password</a></p>" +
                            "<p>This link will expire in 1 hour.</p>\"}]}",
                    recipientEmail, FROM_EMAIL, resetLink
            );

            // Create the HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SENDGRID_API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + SENDGRID_API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            // Send the request asynchronously
            return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            System.out.println("Email sent successfully to " + recipientEmail);
                            return true;
                        } else {
                            System.err.println("Failed to send email. Status: " +
                                    response.statusCode() + " - " + response.body());
                            return false;
                        }
                    })
                    .exceptionally(ex -> {
                        System.err.println("Error sending email: " + ex.getMessage());
                        return false;
                    });
        }
    }


}