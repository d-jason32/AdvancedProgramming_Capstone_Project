package edu.farmingdale.advancedprogramming_capstone_project;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import java.io.IOException;

/**
 * MainController handles the main menu options, including:
 * - Creating a session
 * - Opening the ACS Calling interface (video chat)
 * - Generating lesson summaries
 * - Opening live transcription
 */
public class MainController {

    @FXML private Label sessionLabel;  // Displays the session code

    /**
     * Creates a new session and shows the session code on the screen.
     */
    @FXML
    public void createSession() {
        String sessionCode = SessionManager.createSession();
        sessionLabel.setText("Session Code: " + sessionCode);
    }

    /**
     * Opens the ACS video chat window.
     */
    @FXML
    public void openVideoChatWindow() {
        try {
            Stage stage = new Stage();
            Parent root = FXMLLoader.load(getClass().getResource("acsCall.fxml"));
            Scene scene = new Scene(root, 800, 600);
            stage.setTitle("Video Chat & Messaging");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Generates an AI summary of a lesson and displays it in a new window.
     */
    @FXML
    public void getSummary() throws IOException {
        String prompt = "Sample whiteboard and chat content.";

        // Call Gemini AI to get a summary
        GeminiService.getSummaryAsync(prompt).thenAccept(summary -> {
            Platform.runLater(() -> {
                try {
                    // Load the summary window
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("SummaryView.fxml"));
                    Parent root = loader.load();
                    SummaryController summaryController = loader.getController();
                    summaryController.setSummary(summary);

                    // Show the summary window
                    Stage summaryStage = new Stage();
                    summaryStage.setTitle("Lesson Summary");
                    summaryStage.setScene(new Scene(root));
                    summaryStage.show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        });
    }

    /**
     * Opens a new window for real-time audio transcription.
     */
    @FXML
    public void openTranscriptionWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("TranscriptionView.fxml"));
            Parent root = loader.load();
            Stage transcriptionStage = new Stage();
            transcriptionStage.setTitle("Live Transcription");
            transcriptionStage.setScene(new Scene(root));
            transcriptionStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
