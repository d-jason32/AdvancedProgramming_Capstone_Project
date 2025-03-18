package edu.farmingdale.advancedprogramming_capstone_project;

import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import javafx.application.Platform;
import javafx.scene.control.TextArea;

/**
 * SpeechToTextService uses Azure Speech Services to transcribe live audio.
 * Transcription results are displayed in a provided TextArea.
 */
public class SpeechToTextService {
    private SpeechRecognizer recognizer;
    private TextArea transcriptionArea;

    /**
     * Configures the Speech SDK with the provided subscription and region.
     * @param subscriptionKey Azure Speech subscription key.
     * @param region          Azure region (e.g., "eastus").
     * @param transcriptionArea The TextArea for displaying transcription.
     */
    public SpeechToTextService(String subscriptionKey, String region, TextArea transcriptionArea) {
        this.transcriptionArea = transcriptionArea;
        try {
            SpeechConfig config = SpeechConfig.fromSubscription(subscriptionKey, region);
            // Optionally set language: config.setSpeechRecognitionLanguage("en-US");
            AudioConfig audioConfig = AudioConfig.fromDefaultMicrophoneInput();
            recognizer = new SpeechRecognizer(config, audioConfig);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Starts continuous speech recognition.
     */
    public void startRecognition() {
        recognizer.recognizing.addEventListener((s, e) -> Platform.runLater(() ->
                transcriptionArea.setText("Recognizing: " + e.getResult().getText())
        ));

        recognizer.recognized.addEventListener((s, e) -> {
            if (e.getResult().getReason() == ResultReason.RecognizedSpeech) {
                Platform.runLater(() -> {
                    String currentText = transcriptionArea.getText();
                    transcriptionArea.setText(currentText + "\nFinal: " + e.getResult().getText());
                });
            } else if (e.getResult().getReason() == ResultReason.NoMatch) {
                Platform.runLater(() -> transcriptionArea.appendText("\nNo speech could be recognized."));
            }
        });

        recognizer.canceled.addEventListener((s, e) -> Platform.runLater(() ->
                transcriptionArea.appendText("\nRecognition canceled: " + e.getErrorDetails())
        ));

        recognizer.sessionStarted.addEventListener((s, e) -> Platform.runLater(() ->
                transcriptionArea.appendText("\nSession started.")
        ));
        recognizer.sessionStopped.addEventListener((s, e) -> Platform.runLater(() ->
                transcriptionArea.appendText("\nSession stopped.")
        ));

        recognizer.startContinuousRecognitionAsync();
    }

    /**
     * Stops continuous speech recognition.
     */
    public void stopRecognition() {
        if (recognizer != null) {
            recognizer.stopContinuousRecognitionAsync();
        }
    }
}
