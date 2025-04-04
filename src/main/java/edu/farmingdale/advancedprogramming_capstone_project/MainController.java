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
 * - Starting a new call
 * - Joining an existing call
 * - Getting a summary and live transcription
 */
public class MainController {

    @FXML private Label sessionLabel;      // Shows the generated session code
    @FXML private TextField joinSessionField; // Field for user to enter session code
    @FXML private TextField transcriptionArea; // Field for live transcription (if needed)

    /**
     * Called when the "Start New Call" button is pressed.
     * It generates a session code and opens the Jitsi Meet call in the external browser.
     */
    @FXML
    public void startNewCall() {
        try {
            // Generate session code and launch video call window
            String sessionCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            sessionLabel.setText("Session Code: " + sessionCode);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("VideoCallView.fxml"));
            Parent root = loader.load();
            VideoCallController vcController = loader.getController();
            vcController.setRoomCode(sessionCode);
            launchNewWindow("Video Call - Session: " + sessionCode, root, 800, 600);

            // Automatically open the transcription window and start transcription
            openTranscriptionWindow();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method that builds the Jitsi Meet URL and opens it using HostServices.
     * @param sessionCode The unique code for the call session.
     */
    public void openCallInBrowser(String sessionCode) {
        try {
            // Build the URL by loading the local jitsi.html file and appending the session code.
            String jitsiUrl = getClass().getResource("jitsi.html").toExternalForm() + "?room=" + sessionCode;
            // Open the URL in the default external browser using HostServices.
            CapstoneApp.getStaticHostServices().showDocument(jitsiUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when the "Join Call" button is pressed.
     * It gets the session code from the text field and opens the call.
     */
    @FXML
    public void joinCall() {
        try {
            // Retrieve and trim the session code from the text field.
            String sessionCode = joinSessionField.getText().trim();
            if (sessionCode.isEmpty()) {
                System.out.println("Please enter a valid session code.");
                return;
            }
            System.out.println("Joining Session: " + sessionCode);
            // Open the call in the external browser.
            openCallInBrowser(sessionCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when the "Get Summary" button is pressed.
     * It sends text to the Gemini API and opens a window to show the summary.
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
     * Called when the "Live Transcription" button is pressed.
     * It opens a new window for live transcription.
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
     * It loads and displays the profile page in a new window.
     */
    public void goToProfile(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("profilePage.fxml"));
            Parent profilePage = loader.load();
            Stage profileStage = new Stage();
            Scene scene = new Scene(profilePage, 600, 450); // Set desired window size
            profileStage.setScene(scene);
            profileStage.setTitle("Profile");
            profileStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method to launch a new window.
     * @param title The window title.
     * @param root The root node of the scene.
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
