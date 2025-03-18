package edu.farmingdale.advancedprogramming_capstone_project;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

/**
 * TranscriptionController handles displaying real-time audio transcription.
 */
public class TranscriptionController {
    @FXML private TextArea transcriptionTextArea;  // Shows live transcriptions

    /**
     * Updates the transcription text in the UI.
     *
     * @param text The latest transcribed text.
     */
    public void updateTranscription(String text) {
        transcriptionTextArea.appendText(text + "\n");
    }
}
