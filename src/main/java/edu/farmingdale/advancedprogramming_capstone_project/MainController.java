package edu.farmingdale.advancedprogramming_capstone_project;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.Modality;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

// Testing
/**
 * MainController handles the main menu actions:
 * - Starting a new call (opens the default browser)
 * - Joining an existing call (opens the default browser)
 * - Getting a summary and live transcription
 */
public class MainController {
    @FXML private Label sessionLabel;           // Displays the generated session code.
    @FXML private TextField joinSessionField;   // Field for a user to enter session code.
    @FXML private TextField transcriptionArea;  // Field for live transcription.
    @FXML private BorderPane borderPane1;

    boolean isLightMode = false;           // Keeps track of light or dark mode.



    /**
     * Handles the "Start New Call" button action.
     * <p>
     * Generates a unique session code, constructs the Collaboard URL,
     * and opens a new browser window for the session. Afterwards,
     * it launches the transcription UI.
     */
    @FXML
    public void startNewCall() {
        // Generate an 8-character uppercase session identifier
        String sessionCode = UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();

        // Build the Collaboard room URL using the session code
        String callUrl = "https://collaboard-djb7e8caezeqbnef.centralus-01.azurewebsites.net?room="
                + sessionCode;

        // Open the Collaboard session in a separate window
        BrowserViewController.open(callUrl);

        // After opening the browser, start the live transcription feature
        openTranscriptionWindowAndStart();
    }

    /**
     * Handles the "Join Call" button action.
     * <p>
     * Reads the session code from the input field, validates it,
     * constructs the Collaboard URL, and opens the session window.
     * If the input is empty, it logs a prompt to the console.
     */
    @FXML
    public void joinCall() {
        // Retrieve and trim the entered session code
        String sessionCode = joinSessionField.getText().trim();

        // Validate that the user provided a non-empty code
        if (sessionCode.isEmpty()) {
            System.out.println("Please enter a valid session code.");
            return;
        }

        // Build the Collaboard room URL using the provided session code
        String callUrl = "https://collaboard-djb7e8caezeqbnef.centralus-01.azurewebsites.net?room="
                + sessionCode;

        // Open the Collaboard session in a separate window
        BrowserViewController.open(callUrl);

        // After opening the browser, start the live transcription feature
        openTranscriptionWindowAndStart();
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

            // Optional: block the main window while profile is open
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
                System.out.println("Could not find owner stage for alert: " + title);
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
                System.out.println("Failed to get TranscriptionController after loading FXML.");
                return;
            }

            Stage stage = new Stage();
            stage.setTitle("Live Transcription");
            stage.setScene(new Scene(root, 600, 450));
            stage.initModality(Modality.APPLICATION_MODAL);

            stage.setOnCloseRequest(_ -> {
                System.out.println("Transcription window close requested. Shutting down controller.");
                controller.shutDown();
            });

            stage.show();
            if (autoStart) {
                System.out.println("Auto-starting transcription...");
                controller.startTranscribing();
            } else {
                System.out.println("Opened transcription window manually (no auto-start).");
            }

        } catch (IOException e) {
            System.out.println("Failed to load TranscriptionView.fxml" + e);
        } catch (Exception e) {
            System.out.println("An unexpected error occurred opening transcription window" + e);
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
        System.out.println("Requesting summary for placeholder content...");

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
                            System.out.println("Failed to load SummaryController from SummaryView.fxml. Cannot display summary window.");
                            showErrorAlert("Error", "Could not load the summary display component.");
                        }
                    } catch (IOException e) {
                        System.out.println("Failed to load SummaryView.fxml: " + e.getMessage() + e);
                        showErrorAlert("Error", "Could not open the Summary window due to an FXML loading error.");
                    } catch (Exception e) {
                        System.out.println("An unexpected error occurred while preparing summary window: " + e.getMessage() + e);
                        showErrorAlert("Error", "An unexpected error occurred while trying to show the summary.");
                    }
                }, Platform::runLater)
                .exceptionally(ex -> {
                    System.out.println("Error occurred while getting summary from Gemini: " + ex.getMessage() + ex);
                    Platform.runLater(() -> showErrorAlert("Summary Failed", "Failed to generate summary: " + ex.getMessage()));
                    return null;
                });
    }

    /**
     * Method to change the theme from dark to light and vice versa.
     * @param event
     */
    @FXML
    void changeTheme(ActionEvent event) {
        ObservableList<String> stylesheets = borderPane1.getStylesheets();
        stylesheets.clear();
        /*
        If the stylesheet is in dark mode, change it to light mode.
         */
        if (isLightMode) {
            stylesheets.add(getClass().getResource("/edu/farmingdale/advancedprogramming_capstone_project/styling/main_page_styles.css").toExternalForm());
        } else {
            stylesheets.add(getClass().getResource("/edu/farmingdale/advancedprogramming_capstone_project/styling/light_mode.css").toExternalForm());
        }
        isLightMode = !isLightMode;
    }

}