package edu.farmingdale.advancedprogramming_capstone_project;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * TranscriptionController handles displaying live audio transcription.
 */
public class TranscriptionController {

    @FXML private TextArea transcriptionTextArea;
    @FXML private Button startBtn;
    @FXML private Button stopBtn;
    @FXML private Label statusLb;
    @FXML private Button summaryBtn;

    private SpeechToTextService speechToTextService;
    private volatile boolean isTranscriptionActive = false;
    private String lastFullTranscript = "";

    /**
     * Initializes the controller, setting up UI elements and the SpeechToTextService.
     * Configures callbacks for handling transcription results and session completion.
     */
    @FXML
    public void initialize() {
        if (stopBtn != null) {
            stopBtn.setDisable(true);
        } else {
            logError("stopBtn is null. Check FXML file connection.", null);
        }
        if (summaryBtn != null) {
            summaryBtn.setDisable(true);
        }
        updateStatus("Ready To Transcribe");

        if (speechToTextService == null) {
            Consumer<String> handleFinalUtterance = (utterance) -> logInfo("Intermediate Utterance: " + utterance);

            try {
                speechToTextService = new SpeechToTextService(transcriptionTextArea, handleFinalUtterance);
                logInfo("SpeechToTextService instance created.");

                speechToTextService.setOnSessionTranscriptReady(this::requestAndDisplaySummary);
                logInfo("Session transcript callback set.");

            } catch (Exception e) {
                logError("Unexpected error creating SpeechToTextService: " + e.getMessage(), e);
                speechToTextService = null;
            }
        }

        if (speechToTextService == null) {
            logError("SpeechToTextService could not be initialized. Transcription will not work.", null);
            if (startBtn != null) startBtn.setDisable(true);
            updateStatus("ERROR: Transcription Service Failed Initialization");
        }
        logInfo("TranscriptionController initialized.");
    }

    /**
     * Starts the real-time transcription process.
     * Disables the start button and enables the stop button upon successful start.
     * Runs the potentially blocking service start call on a background thread.
     */
    @FXML
    public void startTranscribing() {
        if (isTranscriptionActive) {
            logInfo("Start requested, but transcription is already active.");
            return;
        }
        if (speechToTextService == null) {
            logError("Cannot start transcription, SpeechToTextService is not initialized.", null);
            showErrorAlert("Error", "Transcription service failed to initialize. Cannot start.");
            if (startBtn != null) startBtn.setDisable(true);
            if (stopBtn != null) stopBtn.setDisable(true);
            updateStatus("ERROR: Service Not Initialized");
            return;
        }

        updateStatus("Starting Transcription...");
        transcriptionTextArea.clear();
        lastFullTranscript = "";

        if (startBtn != null) startBtn.setDisable(true);
        if (stopBtn != null) stopBtn.setDisable(true);
        if (summaryBtn != null) summaryBtn.setDisable(true);

        new Thread(() -> {
            boolean started = false;
            try {
                started = speechToTextService.startRealtimeTranscription();

            } catch (Exception e) {
                logError("Unexpected exception during transcription start thread", e);
                Platform.runLater(() -> {
                    updateStatus("Error during transcription start.");
                    showErrorAlert("Start Error", "An unexpected error occurred while starting transcription: " + e.getMessage());
                    if (startBtn != null) startBtn.setDisable(false);
                    if (stopBtn != null) stopBtn.setDisable(true);
                });
                return;
            }

            final boolean finalStarted = started;
            Platform.runLater(() -> {
                if (finalStarted) {
                    isTranscriptionActive = true;
                    if (stopBtn != null) stopBtn.setDisable(false);
                    updateStatus("Transcription Active...");
                    logInfo("Transcription started successfully.");
                } else {
                    updateStatus("Failed to start transcription. Check logs.");
                    showErrorAlert("Start Failed", "Could not start transcription. Please check microphone permissions and Azure configuration.");
                    if (startBtn != null) startBtn.setDisable(false);
                    if (stopBtn != null) stopBtn.setDisable(true);
                }
            });
        }).start();
    }

    /**
     * Stops the active real-time transcription process.
     * Disables the stop button and enables the start button.
     * The summary generation is triggered automatically via the callback
     * set in `initialize` when the service confirms the session end.
     * Runs the potentially blocking service stop call on a background thread.
     */
    @FXML
    private void stopTranscription() {
        if (!isTranscriptionActive) {
            logInfo("Stop requested, but transcription is not active.");
            return;
        }

        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Confirm Stop");
        confirmationAlert.setHeaderText("Stop Transcription and Leave Call?");
        confirmationAlert.setContentText("Are you sure you want to stop the live transcription?");

        Stage ownerStage = (Stage) stopBtn.getScene().getWindow();
        if (stopBtn != null && stopBtn.getScene() != null) {
            ownerStage = (Stage) stopBtn.getScene().getWindow();
        }
        if (ownerStage != null) {
            confirmationAlert.initOwner(ownerStage);
            confirmationAlert.initModality(Modality.WINDOW_MODAL);
        }

        Optional<ButtonType> result = confirmationAlert.showAndWait();

        if (result.isEmpty() || result.get() != ButtonType.OK) {
            logInfo("Stop transcription cancelled by user.");
            return;
        }

        if (speechToTextService == null) {
            logError("Cannot stop transcription, SpeechToTextService is not initialized.", null);
            isTranscriptionActive = false;
            if (startBtn != null) startBtn.setDisable(false);
            if (stopBtn != null) stopBtn.setDisable(true);
            if (summaryBtn != null) summaryBtn.setDisable(true);
            updateStatus("Ready (Service was null)");
            return;
        }

        updateStatus("Stopping Transcription...");
        if (startBtn != null) startBtn.setDisable(true);
        if (stopBtn != null) stopBtn.setDisable(true);
        if (summaryBtn != null) summaryBtn.setDisable(true);

        new Thread(() -> {
            try {
                speechToTextService.stopRealtimeTranscription();
                Platform.runLater(() -> {
                    isTranscriptionActive = false;
                    startBtn.setDisable(false);
                    updateStatus("Transcription Stopped. Processing summary...");
                    logInfo("Stop transcription request processed.");
                });
            } catch (Exception e) {
                logError("Unexpected exception during transcription stop thread", e);
                Platform.runLater(() -> {
                    updateStatus("Error during transcription stop.");
                    showErrorAlert("Stop Error", "An unexpected error occurred while stopping transcription: " + e.getMessage());
                    isTranscriptionActive = false;
                    startBtn.setDisable(false);
                    stopBtn.setDisable(true);
                });
            }
        }).start();
    }

    /**
     * Callback method triggered by SpeechToTextService when a full session transcript is ready.
     * Initiates the call to GeminiService for summarization and opens the summary window.
     *
     * @param fullTranscript The complete text transcribed during the session.
     */
    private void requestAndDisplaySummary(String fullTranscript) {
        lastFullTranscript = fullTranscript;

        if (fullTranscript == null || fullTranscript.trim().isEmpty()) {
            logAndUpdateStatus("Received empty transcript. Skipping summary generation.");
            Platform.runLater(() -> {
                if (summaryBtn != null) summaryBtn.setDisable(true);
                updateStatus("No speech transcribed. Summary not generated.");
                showErrorAlert("Summary Skipped", "No speech was detected during the session, so no summary could be generated.");
            });
            return;
        }

        logAndUpdateStatus("Received full transcript (" + fullTranscript.length() + " chars). Requesting summary...");
        Platform.runLater(() -> updateStatus("Generating summary..."));

        String promptForGemini = """
            Please provide a detailed summary of the following meeting transcript.
            Include the main topics discussed, key decisions made, any action items assigned (and who they were assigned to, if mentioned), and any significant points of disagreement or agreement.
            Structure the summary clearly, perhaps using bullet points for key items. Must be longer than just one sentence.

            --- TRANSCRIPT START ---
            %s
            --- TRANSCRIPT END ---
            """.formatted(fullTranscript);

        CompletableFuture<String> summaryFuture = GeminiService.getSummaryAsync(promptForGemini);

        summaryFuture.thenAcceptAsync(summary -> {
                    logAndUpdateStatus("Received summary response from Gemini.");
                    Platform.runLater(() -> {
                        updateStatus("Summary received.");
                        if (summaryBtn != null) summaryBtn.setDisable(false);
                        openSummaryWindow(summary);
                    });
                }, Platform::runLater)
                .exceptionally(ex -> {
                    logError("Error occurred while getting summary from Gemini: " + ex.getMessage(), ex);
                    Platform.runLater(() -> {
                        updateStatus("Error generating summary.");
                        if (summaryBtn != null) summaryBtn.setDisable(false);
                        showErrorAlert("Summary Failed", "Failed to generate summary:\n" + ex.getMessage());
                    });
                    return null;
                });
    }

    /**
     * Opens a new window to display the generated summary text using SummaryView.fxml.
     * Includes error handling for FXML loading and controller access.
     * @param summaryText The summary text to display.
     */
    private void openSummaryWindow(String summaryText) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("SummaryView.fxml"));
            Parent root = loader.load();
            SummaryController loadedSummaryController = loader.getController();

            if (loadedSummaryController != null) {
                loadedSummaryController.displaySummary(summaryText);
            } else {
                logError("Failed to load SummaryController from SummaryView.fxml. Cannot open window.", null);
                showErrorAlert("Error", "Could not load the summary display component.");
                appendTranscriptionLog("\n\n--- SUMMARY (Window Load Failed) ---\n" + summaryText + "\n---------------\n");
                return;
            }

            Stage stage = new Stage();
            stage.setTitle("Session Summary");
            stage.setScene(new Scene(root, 600, 400));
            Stage owner = (Stage) transcriptionTextArea.getScene().getWindow();
            if (owner != null) {
                stage.initOwner(owner);
                stage.initModality(Modality.WINDOW_MODAL);
            }
            stage.show();

        } catch (IOException e) {
            logError("Failed to load SummaryView.fxml: " + e.getMessage(), e);
            showErrorAlert("Error", "Could not open the Summary window due to an FXML loading error.");
            appendTranscriptionLog("\n\n--- SUMMARY (Window IO Error) ---\n" + summaryText + "\n---------------\n");
        } catch (Exception e) {
            logError("An unexpected error occurred opening summary window: " + e.getMessage(), e);
            showErrorAlert("Error", "An unexpected error occurred while trying to show the summary.");
            appendTranscriptionLog("\n\n--- SUMMARY (Window Unexpected Error) ---\n" + summaryText + "\n---------------\n");
        }
    }

    /**
     * Handles the action for the manual summary button (if present).
     * Attempts to re-request and display the summary based on the last full transcript captured.
     */
    @FXML
    private void handleShowSummary() {
        if (lastFullTranscript != null && !lastFullTranscript.trim().isEmpty()) {
            logAndUpdateStatus("Manual summary request for last transcript...");
            requestAndDisplaySummary(lastFullTranscript);
        } else {
            logAndUpdateStatus("No previous transcript available to summarize manually.");
            updateStatus("No transcript available for summary.");
            showErrorAlert("No Data", "There is no transcript from the previous session available to summarize.");
        }
    }

    /**
     * Shuts down the transcription service gracefully.
     * This should be called when the transcription window is closed or the application exits.
     * Ensures transcription is stopped and resources are released.
     */
    public void shutDown() {
        logAndUpdateStatus("Shutting Down Transcription Controller and Service...");
        if (isTranscriptionActive && speechToTextService != null) {
            logInfo("Transcription was active during shutdown, attempting to stop (no confirmation)...");

            try {
                speechToTextService.stopRealtimeTranscription();
                logInfo("Transcription stopped during shutdown.");
            } catch (Exception e) {
                logError("Error stopping transcription during shutdown", e);
            }
            isTranscriptionActive = false;
        } else if (isTranscriptionActive) {
            logInfo("Transcription was marked active during shutdown, but service was null. Resetting flag.");
            isTranscriptionActive = false;
        }

        SpeechToTextService serviceToClose = this.speechToTextService;
        if (serviceToClose != null) {
            try {
                serviceToClose.close();
                logAndUpdateStatus("SpeechToTextService Closed Successfully");
            } catch (Exception e) {
                logError("Error closing SpeechToTextService: " + e.getMessage(), e);
            } finally {
                this.speechToTextService = null;
            }
        } else {
            logInfo("SpeechToTextService was already null during shutdown.");
        }
        logAndUpdateStatus("Transcription Controller Shutdown complete.");
    }

    /**
     * Logs an informational message and updates the status label in the UI.
     * @param message The message to log and display.
     */
    private void logAndUpdateStatus(String message) {
        logInfo(message);
        updateStatus(message);
    }

    /**
     * Logs an informational message to the standard output.
     * @param message The message to log.
     */
    private void logInfo(String message) {
        System.out.println("INFO (Transcription): " + message);
    }

    /**
     * Logs an error message to the standard error stream and prints the exception's stack trace.
     * @param message The error message.
     * @param e The exception that occurred (can be null).
     */
    private void logError(String message, Throwable e) {
        System.err.println("ERROR (Transcription): " + message);
        if (e != null) {
            e.printStackTrace();
        }
        updateStatus("ERROR: " + message);
        showErrorAlert("Transcription Error", message);
    }

    /**
     * Updates the status label in the UI, ensuring it runs on the JavaFX Application Thread.
     * @param message The status message to display.
     */
    private void updateStatus(String message) {
        if (statusLb != null) {
            if (Platform.isFxApplicationThread()) {
                statusLb.setText(message);
            } else {
                Platform.runLater(() -> statusLb.setText(message));
            }
        } else {
            if (!message.equals("Ready To Transcribe")) {
                System.err.println("ERROR: Status Label (statusLb) is null. Cannot update UI status to: " + message);
            }
        }
    }

    /**
     * Appends text to the transcription text area in the UI.
     * Ensures the update happens on the JavaFX Application Thread.
     * This method might be redundant if SpeechToTextService updates the TextArea directly.
     * @param text The text to append.
     */
    public void appendTranscriptionLog(String text) {
        if (transcriptionTextArea != null) {
            final String textToAppend = text + "\n";
            if (Platform.isFxApplicationThread()) {
                transcriptionTextArea.appendText(textToAppend);
            } else {
                Platform.runLater(() -> transcriptionTextArea.appendText(textToAppend));
            }
        } else {
            System.err.println("ERROR: Transcription Text Area is null. Cannot append text: " + text);
        }
    }

    /**
     * Helper method to show a standard error alert dialog.
     * Ensures the alert is shown on the JavaFX Application Thread.
     * @param title The title of the alert window.
     * @param content The main message text of the alert.
     */
    private void showErrorAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            Stage ownerStage = null;
            if (transcriptionTextArea != null && transcriptionTextArea.getScene() != null) {
                ownerStage = (Stage) transcriptionTextArea.getScene().getWindow();
            }
            if (ownerStage != null) {
                alert.initOwner(ownerStage);
                alert.initModality(Modality.WINDOW_MODAL);
            }
            alert.showAndWait();
        });
    }
}