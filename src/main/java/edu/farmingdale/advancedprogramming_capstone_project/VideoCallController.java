package edu.farmingdale.advancedprogramming_capstone_project;

import javafx.fxml.FXML;
import javafx.scene.web.WebView;

/**
 * VideoCallController loads the Jitsi Meet interface from jitsi.html.
 * It appends the session code as a query parameter so that all participants join the same room.
 */
public class VideoCallController {
    @FXML private WebView webView;

    // Default room code if none is provided.
    private String roomCode = "TestRoom";

    /**
     * Sets the room code (session code) for the video call.
     * This method should be called by MainController before launching the video call window.
     * @param roomCode the unique session code provided or generated.
     */
    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
        // If the WebView is already initialized, reload the Jitsi Meet interface with the new room code.
        if (webView != null) {
            loadJitsiMeet();
        }
    }

    /**
     * Called automatically when the FXML is loaded.
     */
    public void initialize() {
        loadJitsiMeet();
    }

    /**
     * Helper method to load the Jitsi Meet interface into the WebView.
     * The session code is appended as a query parameter to the URL.
     */
    private void loadJitsiMeet() {
        String url = getClass().getResource("jitsi.html").toExternalForm() + "?room=" + roomCode;
        webView.getEngine().load(url);
    }
}