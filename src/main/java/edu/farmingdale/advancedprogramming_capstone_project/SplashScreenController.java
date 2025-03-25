package edu.farmingdale.advancedprogramming_capstone_project;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import java.net.URL;
import java.util.ResourceBundle;

public class SplashScreenController implements Initializable {

    /**
     * initializes three classes for splash screen:
     * splashLabel | ProgressBar | Runnable
     */
    @FXML private Label splashLabel;
    @FXML private ProgressBar loadingBar;
    private Runnable checkLoadingBarFinished;

    /**
     * method for progressBar on Splash Screen
     * the bar will start from 0% and progress until 100%
     * when it completes, it will close and main fxml file will launch
     * takes estimated time of 3.5 seconds to complete
     */
    private void startLoadingBar() {
        Thread barLoad = new Thread(() -> {
            for (double i = 0.0; i <= 1.0; i = i + 0.01) {

                try {
                    final double progressTime = i;
                    Platform.runLater(() -> loadingBar.setProgress(progressTime));
                    Thread.sleep(50);
                }

                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Loading Bar Did Not Run Properly");
                    return;
                }
            }
            loadingBarFinished(); // calls function for when bar is finished (100%)
        });
        barLoad.setDaemon(true); // terminates thread when the application exits
        barLoad.start(); //
    }

    /**
     * this method is called when the loading bar is finished
     * checks if a callback function (checkLoadingBarFinished) has been set.
     * if it has, it runs the callback function
     */
    protected void loadingBarFinished() {
        if (checkLoadingBarFinished != null) {
            checkLoadingBarFinished.run();
        }

        else {
            System.out.println("Loading Bar Did Not Run Properly");
        }
    }

    /**
     * @param callFunction
     * calls the function for loading bar when it finishes (100%)
     */
    protected void setLoadingBarFinished(Runnable callFunction) {
        this.checkLoadingBarFinished = callFunction;
    }

    /**
     * @param location
     * The location used to resolve relative paths for the root object, or
     * {@code null} if the location is not known.
     *
     * @param resource
     * The resources used to localize the root object, or {@code null} if
     * the root object was not localized.
     */
    @Override
    public void initialize(URL location, ResourceBundle resource) {
        splashLabel.setText("Whiteboard Teaching Tool");

        loadingBar.setProgress(0.0); // always starts from 0%
        startLoadingBar(); // calls function
    }
}
