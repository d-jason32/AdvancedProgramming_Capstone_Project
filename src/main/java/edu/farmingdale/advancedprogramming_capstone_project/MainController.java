package edu.farmingdale.advancedprogramming_capstone_project;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * MainController handles the main menu actions:
 * - Starting a new call (opens the default browser)
 * - Joining an existing call (opens the default browser)
 * - Getting a summary (potentially triggered differently now)
 * - Opening the transcription window
 * - Navigating to the profile page
 */
public class MainController {

    @FXML
    public Label summaryLabel;
    @FXML
    private Label sessionLabel;
    @FXML
    private TextField joinSessionField;

    /**
     * Initializes the controller. Sets up the initial UI state.
     */
    @FXML
    public void initialize() {
        logInfo("MainController initializing...");

        if (sessionLabel != null) {
            sessionLabel.setText("");
        } else {
            logError("sessionLabel is null. Check FXML connection.");
        }
        if (summaryLabel != null) {
            summaryLabel.setText("");
        } else {
            logError("summaryLabel is null. Check FXML connection.");
        }
        logInfo("MainController initialized.");
    }

    /**
     * Shuts Down the controller. Placeholder for any cleanup needed in the main view.
     */
    public void shutdown() {
        logInfo("MainController shutting down...");
        logInfo("MainController shutdown complete.");
    }

    /**
     * Logs an informational message to the standard output.
     * @param message The message to log.
     */
    private void logInfo(String message) {
        System.out.println("INFO: (MainController): " + message);
    }

    /**
     * Logs an error message to the standard error stream.
     * Consider enhancing this to show a user-facing dialog for critical errors.
     * @param message The error message to log.
     */
    private void logError(String message) {
        System.err.println("ERROR: " + message);
        Platform.runLater(() -> showErrorAlert("Error", message));
    }

    /**
     * Logs an error message and prints the stack trace of an exception.
     * @param message The error message.
     * @param e The exception that occurred.
     */
    private void logError(String message, Throwable e) {
        System.err.println("ERROR: " + message);
        if (e != null) {
            e.printStackTrace();
        }
        Platform.runLater(() -> {
            assert e != null;
            showErrorAlert("Error", message + "\n" + e.getMessage());
        });
    }

    /**
     * Called when the "Start New Call" button is pressed.
     * Generates a session code, displays it, and opens the call URL in the default browser.
     * Also opens the transcription window.
     */
    @FXML
    public void startNewCall() {
        String sessionCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        sessionLabel.setText("Session Code: " + sessionCode);

        String callUrl = "https://collaboard-djb7e8caezeqbnef.centralus-01.azurewebsites.net?room=" + sessionCode;
        try {
            CapstoneApp.getStaticHostServices().showDocument(callUrl);
            openTranscriptionWindowAndStart();
        } catch (Exception e) {
            logError("Failed to open call URL or transcription window.", e);
            showErrorAlert("Operation Failed", "Could not open the call URL or the transcription window. Please check your browser and system settings.");
        }
    }

    /**
     * Called when the "Join Call" button is pressed.
     * Reads the session code from the input field and opens the corresponding call URL.
     */
    @FXML
    public void joinCall() {
        if (joinSessionField == null) {
            logError("Join session field is not available.");
            return;
        }
        String sessionCode = joinSessionField.getText().trim();
        if (sessionCode.isEmpty()) {
            logInfo("Join Call attempt failed: Session code field is empty.");
            showErrorAlert("Input Required", "Please enter a valid session code to join.");
            return;
        }
        logInfo("Attempting to join Session: " + sessionCode);
        String callUrl = "https://collaboard-djb7e8caezeqbnef.centralus-01.azurewebsites.net?room=" + sessionCode;
        try {
            CapstoneApp.getStaticHostServices().showDocument(callUrl);
            openTranscriptionWindowAndStart();
        } catch (Exception e) {
            logError("Failed to open join call URL for session: " + sessionCode, e);
        }
    }

    /**
     * Opens the transcription window and automatically starts the transcription process.
     * Called by startNewCall() and joinCall().
     */
    private void openTranscriptionWindowAndStart() {
        openTranscriptionWindowInternal(true);
    }

    /**
     * Called when the "Live Transcription" button is pressed.
     * Opens the dedicated window for transcription without automatically starting.
     */
    @FXML
    public void openTranscriptionWindowManually() {
        openTranscriptionWindowInternal(false);
    }

    /**
     * Internal helper method to load and show the transcription window.
     * @param autoStart If true, calls startTranscribing() on the controller after loading.
     */
    private void openTranscriptionWindowInternal(boolean autoStart) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("TranscriptionView.fxml"));
            Parent root = loader.load();

            TranscriptionController controller = loader.getController();
            if (controller == null) {
                logError("Failed to get TranscriptionController after loading FXML.");
                return;
            }

            Stage stage = new Stage();
            stage.setTitle("Live Transcription");
            stage.setScene(new Scene(root, 600, 450));
            stage.initModality(Modality.APPLICATION_MODAL);

            stage.setOnCloseRequest(_ -> {
                logInfo("Transcription window close requested. Shutting down controller.");
                controller.shutDown();
            });

            stage.show();
            if (autoStart) {
                logInfo("Auto-starting transcription...");
                controller.startTranscribing();
            } else {
                logInfo("Opened transcription window manually (no auto-start).");
            }

        } catch (IOException e) {
            logError("Failed to load TranscriptionView.fxml", e);
        } catch (Exception e) {
            logError("An unexpected error occurred opening transcription window", e);
        }
    }

    /**
     * Called when the "Get Summary" button is pressed.
     * NOTE: This currently uses a placeholder prompt. The source of content to summarize
     * needs to be determined (e.g., from a file, previous session data, etc.).
     * It requests a summary from the GeminiService and displays it in a new window.
     */
    @FXML
    public void getSummary() {
        // FIXME: Replace placeholder prompt with actual content source
        String prompt = "Sample whiteboard and chat content.";
        logInfo("Requesting summary for placeholder content...");

        CompletableFuture<String> summaryFuture = GeminiService.getSummaryAsync(prompt);

        summaryFuture.thenAcceptAsync(summary -> {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("SummaryView.fxml"));
                        Parent root = loader.load();
                        SummaryController loadedSummaryController = loader.getController();

                        if (loadedSummaryController != null) {
                            loadedSummaryController.displaySummary(summary);
                            launchNewWindow("Session Summary", root, 600, 400);
                        } else {
                            logError("Failed to load SummaryController from SummaryView.fxml. Cannot display summary window.");
                            showErrorAlert("Error", "Could not load the summary display component.");
                        }
                    } catch (IOException e) {
                        logError("Failed to load SummaryView.fxml: " + e.getMessage(), e);
                        showErrorAlert("Error", "Could not open the Summary window due to an FXML loading error.");
                    } catch (Exception e) {
                        logError("An unexpected error occurred while preparing summary window: " + e.getMessage(), e);
                        showErrorAlert("Error", "An unexpected error occurred while trying to show the summary.");
                    }
                }, Platform::runLater)
                .exceptionally(ex -> {
                    logError("Error occurred while getting summary from Gemini: " + ex.getMessage(), ex);
                    Platform.runLater(() -> showErrorAlert("Summary Failed", "Failed to generate summary: " + ex.getMessage()));
                    return null;
                });
    }

    /**
     * Handles the action to navigate to the user's profile page.
     *
     * @param event The action event triggering this method.
     */
    @FXML
    public void goToProfile(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("profilePage.fxml"));
            Parent profilePageRoot = loader.load();

            Stage ownerStage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();

            Stage profileStage = createProfileStage(profilePageRoot, ownerStage);

            // blocks main scene
            profileStage.initOwner(ownerStage);
            profileStage.initModality(Modality.WINDOW_MODAL);

            profileStage.show();

        } catch (IOException e) {
            logError("Failed to load profilePage.fxml: " + e.getMessage(), e);
            showErrorAlert("Error", "Could not open the Profile page due to an FXML loading error.");
        } catch (Exception e) {
            logError("An unexpected error occurred opening profile page: " + e.getMessage(), e);
            showErrorAlert("Error", "An unexpected error occurred while trying to open the profile page.");
        }
    }

    /**
     * Creates and configures the Stage for the profile page.
     *
     * @param profileRoot The root node of the profile page scene.
     * @param ownerStage  The stage that owns this new stage (usually the main application window).
     * @return The configured Stage for the profile page.
     */
    @NotNull
    private Stage createProfileStage(Parent profileRoot, Stage ownerStage) {
        Stage profileStage = new Stage();
        profileStage.setTitle("Profile");

        Scene scene = new Scene(profileRoot,
                ownerStage.getWidth(),
                ownerStage.getHeight());
        profileStage.setScene(scene);

        profileStage.setX(ownerStage.getX() + 50);
        profileStage.setY(ownerStage.getY() + 50);

        return profileStage;
    }

    /**
     * Helper method to launch a new, independent window (Stage).
     *
     * @param title  The title for the new window.
     * @param root   The root Parent node for the window's scene.
     * @param width  The desired initial width of the window.
     * @param height The desired initial height of the window.
     */
    private void launchNewWindow(String title, Parent root, int width, int height) {
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(new Scene(root, width, height));
        stage.show();
    }

    /**
     * Helper method to show a standard error alert dialog.
     * Ensures the alert is shown on the JavaFX Application Thread.
     * @param title The title of the alert window.
     * @param content The main message text of the alert.
     */
    private void showErrorAlert(String title, String content) {
        Runnable alertTask = () -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);

            Stage ownerStage = findCurrentStage();
            if (ownerStage != null) {
                alert.initOwner(ownerStage);
                alert.initModality(Modality.WINDOW_MODAL);
            } else {
                logInfo("Could not find owner stage for alert: " + title);
            }

            alert.showAndWait();
        };
        if (Platform.isFxApplicationThread()) {
            alertTask.run();
        } else {
            Platform.runLater(alertTask);
        }
    }

    private Stage findCurrentStage() {
        if (sessionLabel != null && sessionLabel.getScene() != null) {
            return (Stage) sessionLabel.getScene().getWindow();
        } else if (joinSessionField != null && joinSessionField.getScene() != null) {
            return (Stage) joinSessionField.getScene().getWindow();
        }
        return null;
    }
}