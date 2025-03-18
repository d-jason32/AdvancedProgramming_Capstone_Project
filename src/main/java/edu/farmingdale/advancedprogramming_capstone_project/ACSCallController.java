package edu.farmingdale.advancedprogramming_capstone_project;

import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 * ACSCallController is responsible for loading the ACS Calling interface inside a WebView.
 */
public class ACSCallController {
    @FXML
    private WebView webView;  // WebView where the ACS Calling page is loaded

    /**
     * Initializes the WebView by loading the ACS Calling web page.
     */
    public void initialize() {
        WebEngine engine = webView.getEngine();

        // Load the acsCall.html file (which handles video chat)
        String url = getClass().getResource("acsCall.html").toExternalForm();
        engine.load(url);
    }
}
