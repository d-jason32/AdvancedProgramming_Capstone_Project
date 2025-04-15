package edu.farmingdale.advancedprogramming_capstone_project;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.UUID;

/**
 * MainController handles the main menu actions:
 * - Starting a new call (opens the default browser)
 * - Joining an existing call (opens the default browser)
 * - Getting a summary and live transcription
 */
public class MainController {

    @FXML
    private Label sessionLabel;           // Displays the generated session code.

    @FXML
    private TextField joinSessionField;   // Field for user to enter session code.

    @FXML
    private TextField transcriptionArea;  // Field for live transcription.

    /**
     * Called when the "Start New Call" button is pressed.
     * Generates a session code, builds the call URL, and opens it in the default browser.
     */
    @FXML
    public void startNewCall() {
        // Generate a unique session code.
        String sessionCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        sessionLabel.setText("Session Code: " + sessionCode);

        // Build the call URL using the HTTP endpoint of your ASP.NET Core app.
        String callUrl = "http://localhost:5186/peerjs_call.html?room=" + sessionCode;
        CapstoneApp.getStaticHostServices().showDocument(callUrl);

        // Optionally, open the transcription window if needed.
        openTranscriptionWindow();
    }

    /**
     * Called when the "Join Call" button is pressed.
     * Retrieves the session code from the text field, builds the call URL, and opens it in the default browser.
     */
    @FXML
    public void joinCall() {
        try {
            // Retrieve and trim the session code.
            String sessionCode = joinSessionField.getText().trim();
            if (sessionCode.isEmpty()) {
                System.out.println("Please enter a valid session code.");
                return;
            }
            System.out.println("Joining Session: " + sessionCode);

            // Build the call URL with the session code parameter.
            String callUrl = "http://localhost:5186/peerjs_call.html?room=" + sessionCode;
            CapstoneApp.getStaticHostServices().showDocument(callUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when the "Get Summary" button is pressed.
     * Sends session content to the Gemini API and opens a window to display the summary.
     */
    @FXML
    public void getSummary() {
        // Use a sample prompt for demonstration.
        String prompt = "Sample whiteboard and chat content.";
        GeminiService.getSummaryAsync(prompt).thenAccept(summary -> {
            Platform.runLater(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("SummaryView.fxml"));
                    Parent root = loader.load();
                    SummaryController summaryController = loader.getController();
                    summaryController.setSummary(summary);
                    launchNewWindow("Session Summary", root, 600, 400);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        });
    }

    /**
     * Called when the "Live Transcription" button is pressed.
     * Opens a new window for live transcription.
     */
    @FXML
    public void openTranscriptionWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("TranscriptionView.fxml"));
            Parent root = loader.load();
            launchNewWindow("Live Transcription", root, 400, 300);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when the "My Profile" button is pressed.
     * Loads and displays the profile page in a new window.
     */
    public void goToProfile(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("profilePage.fxml"));
            Parent profilePage = loader.load();
            Stage profileStage = new Stage();
            Scene scene = new Scene(profilePage, 600, 450);
            profileStage.setScene(scene);
            profileStage.setTitle("Profile");
            profileStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method to launch a new window.
     *
     * @param title The window title.
     * @param root  The root node of the scene.
     * @param width The scene width.
     * @param height The scene height.
     */
    private void launchNewWindow(String title, Parent root, int width, int height) {
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(new Scene(root, width, height));
        stage.show();
    }
}