package edu.farmingdale.advancedprogramming_capstone_project;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.EventObject;

/**
 * MainController handles the main menu functions:
 * - Starting a new call (generates a session code and automatically gets an ACS token)
 * - Joining an existing call using the session code
 * - Generating AI lesson summaries via the Gemini API
 * - Opening a live transcription window
 * - Generating an ACS token for testing (automatically copied to clipboard)
 */
public class MainController {

    @FXML private Label sessionLabel;         // Displays generated session code
    @FXML private TextField joinSessionField;   // Field for entering a session code to join
    @FXML private Label summaryLabel;           // Displays AI summary (for summary window)

    // Your ACS connection string from the Azure portal
    private final String acsConnectionString = "endpoint=https://whiteboardcommunicationservices.unitedstates.communication.azure.com/;accesskey=4zUSuTrFjA9xvqjF5glfzQAXkRRJoInD9mP0n9mBejaGX1Hl7IQ0JQQJ99BCACULyCpOhylNAAAAAZCS1x52";

    /**
     * Starts a new call session: generates a session code, automatically gets an ACS token,
     * and opens the video chat window.
     */
    @FXML
    public void startNewCall() {
        // Generate a new session code.
        String sessionCode = SessionManager.createSession();
        sessionLabel.setText("Session Code: " + sessionCode);
        // Automatically generate an ACS token.
        TokenGeneratorService tokenService = new TokenGeneratorService(acsConnectionString);
        TokenGeneratorService.TokenInfo tokenInfo = tokenService.generateToken();
        // Open the video chat window with the session code and token.
        openVideoChatWindow(sessionCode, tokenInfo.getToken());
    }

    /**
     * Joins an existing call session using the session code entered by the user.
     */
    @FXML
    public void joinCall() {
        String sessionCode = joinSessionField.getText().trim();
        if (sessionCode.isEmpty() || !SessionManager.joinSession(sessionCode)) {
            sessionLabel.setText("Invalid session code.");
            return;
        }
        // Automatically generate a token for this client.
        TokenGeneratorService tokenService = new TokenGeneratorService(acsConnectionString);
        TokenGeneratorService.TokenInfo tokenInfo = tokenService.generateToken();
        openVideoChatWindow(sessionCode, tokenInfo.getToken());
    }

    /**
     * Opens the ACS Calling interface window and passes the session code and token.
     * @param sessionCode The shared session code.
     * @param token The ACS access token.
     */
    private void openVideoChatWindow(String sessionCode, String token) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("acsCall.fxml"));
            Parent root = loader.load();
            ACSCallController controller = loader.getController();
            // Pass session code and token to the ACS call controller.
            controller.setSessionCode(sessionCode);
            controller.setToken(token);
            Stage stage = new Stage();
            Scene scene = new Scene(root, 800, 600);
            stage.setTitle("Video Chat - Session: " + sessionCode);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Calls the Gemini API to generate a lesson summary and displays it in a new window.
     */
    @FXML
    public void getSummary() throws IOException {
        String prompt = "Sample whiteboard and chat content.";
        GeminiService.getSummaryAsync(prompt).thenAccept(summary -> {
            Platform.runLater(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("SummaryView.fxml"));
                    Parent root = loader.load();
                    SummaryController summaryController = loader.getController();
                    summaryController.setSummary(summary);
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
}