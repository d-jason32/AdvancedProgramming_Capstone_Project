package edu.farmingdale.advancedprogramming_capstone_project;

import com.teamdev.jxbrowser.browser.Browser;
import com.teamdev.jxbrowser.browser.event.MediaStreamCaptureStarted;
import com.teamdev.jxbrowser.view.javafx.BrowserView;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Controller responsible for launching a separate window
 * containing the JxBrowser-based web view for Collaboard sessions.
 */
public class BrowserViewController {

    /**
     * Opens a new Stage (window) which hosts a fresh BrowserView instance.
     *
     * @param url the URL of the Collaboard room to open
     */
    public static void open(String url) {
        // 1) Create a fresh Browser & view
        Browser sessionBrowser = CapstoneApp.getEngine().newBrowser();
        BrowserView view = BrowserView.newInstance(sessionBrowser);

        // 2) Load the Collaboard URL
        sessionBrowser.navigation().loadUrl(url);

        // 3) Create and show the Stage
        Stage browserStage = new Stage();
        browserStage.setTitle("Collaboard Session");
        browserStage.setScene(new Scene(view, 900, 700));

        // 4) On window-close: notify page, then tear down Browser
        browserStage.setOnCloseRequest(evt -> {
            // Ask the page to leave the room (you’ll implement this next)
            sessionBrowser.mainFrame().ifPresent(frame ->
                    frame.executeJavaScript("window.dispatchEvent(new Event('beforeunload'));")
            );
            // Actually close the Browser (stops camera/mic)
            sessionBrowser.close();
        });

        browserStage.show();
    }
}