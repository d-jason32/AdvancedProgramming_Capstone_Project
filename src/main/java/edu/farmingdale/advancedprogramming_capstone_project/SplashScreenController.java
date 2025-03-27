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
     * splashLabel | loadingBar | checkLoadingBarFinished | progressText
     */
    @FXML private Label splashLabel;
    @FXML private ProgressBar loadingBar;
    @FXML private Label progressText;
    private Runnable checkLoadingBarFinished;

    /**
     * method for progressBar on Splash Screen
     * the bar will start from 0% and progress until 100%
     * when it completes, it will close and main fxml file will launch
     * takes estimated time of 4 seconds to complete
     */
    private void startLoadingBar() {
        Thread barLoad = new Thread(() -> {
            for (int i = 0; i <= 100; i++) { // from 0% to 100%

                try {
                    final int progressValue = i; // final variable for lambda
                    Platform.runLater(() -> {
                        if (loadingBar != null && progressText != null) {
                            loadingBar.setProgress(progressValue / 100.0); // update progress bar
                            progressText.setText(progressValue + "%"); // update text label
                        }
                    });
                    Thread.sleep(35); // adjust speed as needed

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Loading Bar Did Not Run Properly");
                    return;
                }
            }
            Platform.runLater(this::loadingBarFinished); // call method when done
        });
        barLoad.setDaemon(true); // allows thread to terminate after all others are finished
        barLoad.start();
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
        progressText.setText("0%");
        loadingBar.setProgress(0.0); // always starts from 0%
        startLoadingBar(); // calls function
    }
}
