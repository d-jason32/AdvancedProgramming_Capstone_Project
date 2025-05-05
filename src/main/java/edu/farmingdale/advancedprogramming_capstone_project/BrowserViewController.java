package edu.farmingdale.advancedprogramming_capstone_project;

import com.teamdev.jxbrowser.browser.Browser;
import com.teamdev.jxbrowser.view.javafx.BrowserView;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Controller responsible for launching a separate window
 * containing the JxBrowser-based web view for Collaboard sessions.
 */
public class BrowserViewController {

    /**
     * Opens a new Stage (window) which hosts the shared BrowserView.
     * <p>
     * This method retrieves the singleton Browser and BrowserView
     * instances managed by CapstoneApp, navigates to the specified URL,
     * and displays the content in a new JavaFX Stage.
     *
     * @param url the URL of the Collaboard room to open
     */
    public static void open(String url) {
        // Retrieve the shared Browser instance from the main application
        Browser browser = CapstoneApp.getBrowser();

        // Retrieve the JavaFX node that wraps the Browser
        BrowserView view = CapstoneApp.getBrowserView();

        // Navigate the browser to the target Collaboard session URL
        browser.navigation().loadUrl(url);

        // Create a new window (Stage) to display the BrowserView
        Stage browserStage = new Stage();
        browserStage.setTitle("Collaboard Session");

        // Set the scene containing the BrowserView, with preferred dimensions
        browserStage.setScene(new Scene(view, 900, 700));

        // Show the window to the user
        browserStage.show();
    }
}
