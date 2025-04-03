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
 * MainController handles the main menu functions:
 * - Starting a new call (generates a session code and displays it)
 * - Joining an existing call (by entering a session code)
 * - Generating session summaries via the Gemini API
 * - Opening a live transcription window
 */
public class MainController {

    @FXML private Label sessionLabel;      // Displays the generated session code for a new call
    @FXML private TextField joinSessionField; // User input for joining an existing session

    // This TextArea is used for live transcription
    @FXML private TextField transcriptionArea;

    /**
     * Called when the "Start New Call" button is clicked.
     * It generates a unique session code and launches the video call window.
     */
    @FXML
    public void startNewCall() {
        try {
            // Generate a unique 6-character session code (e.g., "A1B2C3")
            String sessionCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            System.out.println("Generated Session Code: " + sessionCode);
            // Display the generated session code on the main UI for sharing
            sessionLabel.setText("Session Code: " + sessionCode);

            // Load the VideoCallView FXML and pass the generated session code
            FXMLLoader loader = new FXMLLoader(getClass().getResource("VideoCallView.fxml"));
            Parent root = loader.load();
            VideoCallController vcController = loader.getController();
            vcController.setRoomCode(sessionCode);

            // Launch the video call window with the session code in the title
            launchNewWindow("Video Call - Session: " + sessionCode, root, 800, 600);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when the "Join Call" button is clicked.
     * It reads the session code entered by the user and launches the video call window with that code.
     */
    @FXML
    public void joinCall() {
        try {
            // Retrieve and trim the session code from the join session text field
            String sessionCode = joinSessionField.getText().trim();
            if (sessionCode.isEmpty()) {
                System.out.println("Please enter a valid session code.");
                return;
            }
            System.out.println("Joining Session: " + sessionCode);

            // Load the VideoCallView FXML and pass the entered session code
            FXMLLoader loader = new FXMLLoader(getClass().getResource("VideoCallView.fxml"));
            Parent root = loader.load();
            VideoCallController vcController = loader.getController();
            vcController.setRoomCode(sessionCode);

            // Launch the video call window with the provided session code in the title
            launchNewWindow("Video Call - Session: " + sessionCode, root, 800, 600);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when the "Get Summary" button is clicked.
     * Sends the transcript (or sample text) to the Gemini API and displays the summary in a new window.
     */
    @FXML
    public void getSummary() {
        // For demonstration, using a sample prompt.
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
     * Called when the "Live Transcription" button is clicked.
     * Opens a new window to display live transcription.
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

    public void goToProfile(ActionEvent event) {
        try {
            // Load the profilePage.fxml file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("profilePage.fxml"));
            Parent profilePage = loader.load();

            // Create a new Stage (window) for the profile page
            Stage profileStage = new Stage();
            Scene scene = new Scene(profilePage, 600, 450); // Adjust size as needed

            profileStage.setScene(scene);
            profileStage.setTitle("Profile");
            profileStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method to launch a new window with a given title, content, and dimensions.
     * @param title the window title
     * @param root the root node of the scene
     * @param width the scene width
     * @param height the scene height
     */
    private void launchNewWindow(String title, Parent root, int width, int height) {
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(new Scene(root, width, height));
        stage.show();
    }
}
