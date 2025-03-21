package edu.farmingdale.advancedprogramming_capstone_project;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 * ACSCallController loads the ACS Calling interface (acsCall.html) into a WebView
 * and displays the shared session code. It automatically loads the call page with the ACS token.
 */
public class ACSCallController {
    @FXML private WebView webView;         // Displays the ACS Calling page
    @FXML private Label sessionCodeLabel;  // Displays the session code

    private String token; // The ACS access token

    /**
     * Initializes the WebView by loading acsCall.html.
     */
    public void initialize() {
        WebEngine engine = webView.getEngine();
        // Load acsCall.html without token initially.
        String url = getClass().getResource("acsCall.html").toExternalForm();
        engine.load(url);
    }

    /**
     * Sets the session code and updates the UI.
     * @param sessionCode The shared session code.
     */
    public void setSessionCode(String sessionCode) {
        if (sessionCodeLabel != null) {
            sessionCodeLabel.setText("Session: " + sessionCode);
        }
        System.out.println("Joined session: " + sessionCode);
    }

    /**
     * Sets the ACS token, then reloads acsCall.html with the token in the URL.
     * @param token The ACS access token.
     */
    public void setToken(String token) {
        this.token = token;
        System.out.println("ACS Token set: " + token);
        WebEngine engine = webView.getEngine();
        String url = getClass().getResource("acsCall.html").toExternalForm() + "?token=" + token;
        engine.load(url);
    }
}
