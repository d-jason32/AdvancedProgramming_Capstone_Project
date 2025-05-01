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
import javafx.stage.Modality;
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
     */
    @FXML
    public void startNewCall() {
        // Generate a unique session code.
        String sessionCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Build the call URL (update the file name if needed).
        String callUrl = "https://collaboard-djb7e8caezeqbnef.centralus-01.azurewebsites.net?room=" + sessionCode;

        // Open the URL using HostServices.
        CapstoneApp.getStaticHostServices().showDocument(callUrl);

    }

    /**
     * Called when the "Join Call" button is pressed.
     */
    @FXML
    public void joinCall() {
        try {
            String sessionCode = joinSessionField.getText().trim();
            if (sessionCode.isEmpty()) {
                System.out.println("Please enter a valid session code.");
                return;
            }
            System.out.println("Joining Session: " + sessionCode);
            String callUrl = "https://collaboard-djb7e8caezeqbnef.centralus-01.azurewebsites.net?room=" + sessionCode;
            CapstoneApp.getStaticHostServices().showDocument(callUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when the "Get Summary" button is pressed.
     */
    @FXML
    public void getSummary() {
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
     */
    public void goToProfile(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("profilePage.fxml"));
            Parent profilePage = loader.load();

            // --- reference to the main (owner) window ---
            Stage primaryStage = (Stage) ((javafx.scene.Node) event.getSource())
                    .getScene()
                    .getWindow();

            Stage profileStage = new Stage();
            profileStage.setTitle("Profile");

            // Scene sized to match the main window
            Scene scene = new Scene(profilePage,
                    primaryStage.getWidth(),
                    primaryStage.getHeight());
            profileStage.setScene(scene);

            // Mirror position & size
            profileStage.setX(primaryStage.getX());
            profileStage.setY(primaryStage.getY());
            profileStage.setWidth(primaryStage.getWidth());
            profileStage.setHeight(primaryStage.getHeight());

            // Optional: block main window while profile is open
            profileStage.initOwner(primaryStage);
            profileStage.initModality(Modality.WINDOW_MODAL);

            profileStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method to launch a new window.
     */
    private void launchNewWindow(String title, Parent root, int width, int height) {
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(new Scene(root, width, height));
        stage.show();
    }

    /**
     * Method to open the database.
     * @param event
     */
    @FXML
    void goToDatabase(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("database.fxml"));
            Parent root = loader.load();
            launchNewWindow("Live Transcription", root, 1280, 800);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}