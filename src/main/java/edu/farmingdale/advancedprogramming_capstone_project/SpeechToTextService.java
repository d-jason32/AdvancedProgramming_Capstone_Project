package edu.farmingdale.advancedprogramming_capstone_project;

import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * Provides speech-to-text conversion functionality using Azure Cognitive Services.
 * This service supports real-time transcription using a microphone input and callbacks
 * for individual transcriptions and the full session transcript.
 */
public class SpeechToTextService {

    private final TextArea localOutputTextArea;
    private Consumer<String> onFinalTranscriptReadyToSend;
    private Consumer<String> onSessionTranscriptReady;
    private SpeechConfig speechConfig;
    private SpeechRecognizer speechRecognizer;
    private AudioConfig audioConfig;
    private volatile boolean isRealtimeActive = false;
    private final Object lock = new Object();
    private final StringBuilder fullTranscriptBuilder = new StringBuilder();

    /**
     * Constructor for SpeechToTextService using Azure.
     * @param localOutputTextArea TextArea for displaying status messages and transcripts locally.
     * @param sendCallback        Consumer callback function for individual utterances (can be null).
     */
    public SpeechToTextService(TextArea localOutputTextArea, Consumer<String> sendCallback) {
        this.localOutputTextArea = localOutputTextArea;
        this.onFinalTranscriptReadyToSend = sendCallback;

        final String AZURE_SPEECH_KEY = "03vTYwgGjsoNzzn9b2iD1KTb9pHbatnkHeLBglE2fO4fCRpZg3qtJQQJ99BDACYeBjFXJ3w3AAAYACOGOQY8";
        final String AZURE_SPEECH_REGION = "eastus";

        if (AZURE_SPEECH_KEY == null || AZURE_SPEECH_KEY.trim().isEmpty()) {
            updateLocalTextArea("FATAL ERROR: Azure Speech Key not found. Set AZURE_SPEECH_KEY environment variable.");
            this.speechConfig = null;
            return;
        }
        if (AZURE_SPEECH_REGION == null || AZURE_SPEECH_REGION.trim().isEmpty()) {
            updateLocalTextArea("FATAL ERROR: Azure Speech Region not found. Set AZURE_SPEECH_REGION environment variable.");
            this.speechConfig = null;
            return;
        }

        if (this.onFinalTranscriptReadyToSend == null) {
            updateLocalTextArea("INFO: Callback for individual utterances is null. Only final session transcript will be available via onSessionTranscriptReady.");
        }

        try {
            this.speechConfig = SpeechConfig.fromSubscription(AZURE_SPEECH_KEY, AZURE_SPEECH_REGION);
            updateLocalTextArea("Azure Speech Config Initialized (Region: " + AZURE_SPEECH_REGION + "). Ready.");
        } catch (Exception e) {
            updateLocalTextArea("FATAL ERROR: Failed to initialize Azure SpeechConfig: " + e.getMessage());
            e.printStackTrace();
            this.speechConfig = null;
        }
    }

    /**
     * Sets the callback function to be executed when the full session transcript
     * is ready after transcription stops.
     * @param callback The Consumer that accepts the full transcript string.
     */
    public void setOnSessionTranscriptReady(Consumer<String> callback) {
        this.onSessionTranscriptReady = callback;
        if (this.onSessionTranscriptReady == null) {
            updateLocalTextArea("WARNING: The callback for the full session transcript was set to null.");
        }
    }


    /**
     * Allows updating the individual utterance callback function after initialization if needed.
     * @param callback The new Consumer callback.
     */
    public void setOnFinalTranscriptReadyToSend(Consumer<String> callback) {
        this.onFinalTranscriptReadyToSend = callback;
        if (this.onFinalTranscriptReadyToSend == null) {
            updateLocalTextArea("INFO: Callback for individual utterances was set to null.");
        }
    }

    /**
     * Starts the real-time transcription process using the default microphone.
     * Clears any previously accumulated transcript.
     * @return true if the start process was initiated successfully, false otherwise.
     */
    public boolean startRealtimeTranscription() {
        if (this.speechConfig == null) {
            updateLocalTextArea("ERROR: Cannot start. Azure Speech Config was not initialized successfully.");
            return false;
        }
        synchronized (lock) {
            if (isRealtimeActive) {
                updateLocalTextArea("INFO: Realtime Transcription is already active.");
                return true;
            }

            synchronized (fullTranscriptBuilder) {
                fullTranscriptBuilder.setLength(0);
                updateLocalTextArea("Transcript buffer cleared for new session.");
            }

            updateLocalTextArea("Attempting to start Realtime Transcription...");

            try {
                audioConfig = AudioConfig.fromDefaultMicrophoneInput();
                updateLocalTextArea("AudioConfig: Using default microphone.");

                speechRecognizer = new SpeechRecognizer(speechConfig, audioConfig);
                updateLocalTextArea("SpeechRecognizer created.");

                setupAzureEventHandlers();
                Future<Void> task = speechRecognizer.startContinuousRecognitionAsync();

                isRealtimeActive = true;
                updateLocalTextArea("Azure Continuous Recognition requested successfully. Listening...");
                return true;

            } catch (Exception e) {
                updateLocalTextArea("ERROR starting Azure Realtime Transcription: " + e.getMessage());
                updateLocalTextArea("Check microphone permissions and availability.");
                e.printStackTrace();
                stopRealtimeTranscriptionInternal(false);
                return false;
            }
        }
    }

    /**
     * Sets up the event handlers for the Azure SpeechRecognizer using addEventListener.
     */
    private void setupAzureEventHandlers() {
        if (speechRecognizer == null) return;

        speechRecognizer.recognizing.addEventListener((s, e) -> {
            if (e.getResult().getReason() == ResultReason.RecognizingSpeech) {
                String partialText = e.getResult().getText();
                if (partialText != null && !partialText.isEmpty()) {
                    updateLocalTextArea("\nPartial: " + partialText);
                }
            }
        });

        speechRecognizer.recognized.addEventListener((s, e) -> {
            ResultReason reason = e.getResult().getReason();
            String recognizedText = e.getResult().getText();

            if (reason == ResultReason.RecognizedSpeech) {
                if (recognizedText != null && !recognizedText.trim().isEmpty()) {
                    updateLocalTextArea("Final: " + recognizedText);
                    // Append to the full transcript buffer
                    synchronized (fullTranscriptBuilder) {
                        fullTranscriptBuilder.append(recognizedText).append(" "); // <<< ADDED SPACE
                    }
                    // Send the individual utterance if that callback is set
                    if (onFinalTranscriptReadyToSend != null) {
                        onFinalTranscriptReadyToSend.accept(recognizedText);
                    }
                }
            } else if (reason == ResultReason.NoMatch) {
                updateLocalTextArea("INFO: No speech could be recognized (NoMatch).");
            }
        });

        speechRecognizer.sessionStarted.addEventListener((s, e)
                -> updateLocalTextArea("Azure Session Started (ID: " + e.getSessionId() + ")"));

        speechRecognizer.sessionStopped.addEventListener((s, e) -> {
            updateLocalTextArea("Azure Session Stopped (ID: " + e.getSessionId() + ")");
            stopRealtimeTranscriptionInternal(false);
        });

        speechRecognizer.canceled.addEventListener((s, e) -> {
            updateLocalTextArea("Azure Transcription CANCELED.");
            CancellationReason reason = e.getReason();

            if (reason == CancellationReason.Error) {
                CancellationErrorCode errorCode = e.getErrorCode();
                String errorDetails = e.getErrorDetails();
                updateLocalTextArea("ERROR: Code=" + errorCode + ", Details=" + errorDetails);

                if (errorCode == CancellationErrorCode.ConnectionFailure) {
                    updateLocalTextArea("Suggestion: Check network connection and Azure service status.");
                } else if (errorCode == CancellationErrorCode.AuthenticationFailure) {
                    updateLocalTextArea("Suggestion: Verify Azure subscription key and region are correct and active.");
                } else if (errorCode == CancellationErrorCode.ServiceTimeout || errorCode == CancellationErrorCode.ServiceUnavailable) {
                    updateLocalTextArea("Suggestion: Azure service might be temporarily unavailable or timed out. Try again later.");
                } else if (errorCode == CancellationErrorCode.RuntimeError && errorDetails != null && errorDetails.contains("SPXERR_MIC_ERROR")) {
                    updateLocalTextArea("Suggestion: Microphone error detected. Check if it's connected, enabled, and not used by another app.");
                }
            } else if (reason == CancellationReason.EndOfStream) {
                updateLocalTextArea("INFO: End of audio stream reached.");
            } else {
                updateLocalTextArea("INFO: Cancellation Reason: " + reason);
            }
            stopRealtimeTranscriptionInternal(false);
        });
    }

    /**
     * Public method to request to stop the transcription.
     */
    public void stopRealtimeTranscription() {
        updateLocalTextArea("Requesting to stop Realtime Transcription...");
        stopRealtimeTranscriptionInternal(true);
    }

    /**
     * Retrieves the accumulated transcript since the last start
     * and clears the internal buffer. Called internally by stopRealtimeTranscriptionInternal.
     *
     * @return The full transcript accumulated during the session.
     */
    private String consumeFullTranscript() {
        synchronized (fullTranscriptBuilder) {
            String transcript = fullTranscriptBuilder.toString().trim();
            fullTranscriptBuilder.setLength(0);
            return transcript;
        }
    }

    /**
     * Internal method to handle the actual stopping and cleanup.
     * Retrieves the full transcript and invokes the session transcript callback.
     * @param attemptRecognizerStop If true, tries to call stopContinuousRecognitionAsync.
     */
    private void stopRealtimeTranscriptionInternal(boolean attemptRecognizerStop) {
        String finalTranscript = "";

        synchronized (lock) {
            if (!isRealtimeActive && speechRecognizer == null && audioConfig == null) {
                return;
            }

            if (isRealtimeActive) {
                isRealtimeActive = false;
                updateLocalTextArea("Processing stop request...");

                finalTranscript = consumeFullTranscript();
                updateLocalTextArea("Full transcript captured (" + finalTranscript.length() + " chars).");
            } else {
                updateLocalTextArea("Stop requested, but wasn't fully active. Cleaning up resources...");
            }

            if (attemptRecognizerStop && speechRecognizer != null) {
                try {
                    updateLocalTextArea("Sending stop signal to Azure recognizer...");
                    speechRecognizer.stopContinuousRecognitionAsync();
                } catch (Exception e) {
                    updateLocalTextArea("WARNING: Error requesting Azure recognizer stop: " + e.getMessage());
                }
            }

            if (speechRecognizer != null) {
                try {
                    speechRecognizer.close();
                    updateLocalTextArea("Azure SpeechRecognizer closed.");
                } catch (Exception e) {
                    updateLocalTextArea("ERROR closing Azure SpeechRecognizer: " + e.getMessage());
                } finally {
                    speechRecognizer = null;
                }
            }

            if (audioConfig != null) {
                try {
                    audioConfig.close();
                    updateLocalTextArea("Azure AudioConfig closed (Microphone Released).");
                } catch (Exception e) {
                    updateLocalTextArea("ERROR closing Azure AudioConfig: " + e.getMessage());
                } finally {
                    audioConfig = null;
                }
            }

            updateLocalTextArea("Realtime Transcription resources cleaned up.");
        }

        if (onSessionTranscriptReady != null && !finalTranscript.isEmpty()) {
            updateLocalTextArea("Invoking session transcript callback...");
            try {
                onSessionTranscriptReady.accept(finalTranscript);
            } catch (Exception e) {
                updateLocalTextArea("ERROR executing session transcript callback: " + e.getMessage());
                e.printStackTrace();
            }
        } else if (finalTranscript != null && !finalTranscript.isEmpty()) {
            updateLocalTextArea("Session transcript generated but no callback is set.");
        } else if (onSessionTranscriptReady != null) {
            updateLocalTextArea("Session ended, but transcript was empty. No callback invoked.");
        }
    }

    /**
     * Placeholder for transcribing a local audio file.
     * @param filePath Path to the audio file.
     */
    public void transcribeAudioFile(String filePath) {
        if (this.speechConfig == null) {
            updateLocalTextArea("ERROR: Azure Speech Config Not Initialized. Cannot Transcribe File.");
            return;
        }
        // TODO: Implement file transcription logic using AudioConfig.fromWavFileInput(filePath)
        updateLocalTextArea("Azure File Transcription for '" + filePath + "' is not yet implemented.");
    }

    /**
     * Safely updates the local TextArea from any thread.
     * Appends a newline character after the message.
     * @param message The message to append.
     */
    private void updateLocalTextArea(String message) {
        final String formattedMessage = message + "\n";
        if (Platform.isFxApplicationThread()) {
            if (localOutputTextArea != null) {
                localOutputTextArea.appendText(formattedMessage);
            } else {
                System.out.print("STT_Service_Local (UI Thread): " + formattedMessage);
            }
        } else {
            Platform.runLater(() -> {
                if (localOutputTextArea != null) {
                    localOutputTextArea.appendText(formattedMessage);
                } else {
                    System.out.print("STT_Service_Local (Worker Thread): " + formattedMessage);
                }
            });
        }
    }

    /**
     * Call this method when your application is shutting down to release global resources.
     * Crucial to call from your Application's stop() method.
     */
    public void close() {
        updateLocalTextArea("Closing SpeechToTextService...");
        stopRealtimeTranscription();

        if (speechConfig != null) {
            try {
                speechConfig.close();
                updateLocalTextArea("Azure SpeechConfig closed.");
            } catch (Exception e) {
                updateLocalTextArea("ERROR closing Azure SpeechConfig: " + e.getMessage());
            } finally {
                speechConfig = null;
            }
        }
        updateLocalTextArea("SpeechToTextService closed.");
    }
}