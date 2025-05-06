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
        // Create a brand-new Browser tied to the same Engine:
        Browser sessionBrowser = CapstoneApp.getEngine().newBrowser();
        BrowserView view = BrowserView.newInstance(sessionBrowser);

        sessionBrowser.on(MediaStreamCaptureStarted.class, e ->
                System.out.println("Session capturing: " + e.mediaStreamType())
        );
        // (Also set the same single-permission and device-selection callbacks on this browser
        // if you didn't register them globally on the Engine.)

        sessionBrowser.navigation().loadUrl(url);

        Stage browserStage = new Stage();
        browserStage.setTitle("Collaboard Session");
        browserStage.setScene(new Scene(view, 900, 700));
        browserStage.show();
    }
}
