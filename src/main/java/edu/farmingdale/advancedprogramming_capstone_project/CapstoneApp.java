package edu.farmingdale.advancedprogramming_capstone_project;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.Objects;
import static javafx.stage.StageStyle.UNDECORATED;

/**
 * Main application class for the AI Whiteboard Teaching Tool.
 * It starts with a splash screen and then opens the login screen.
 * It also stores HostServices for opening URLs in the external browser.
 */
public class CapstoneApp extends Application {

    // This static field holds the HostServices reference for opening URLs.
    private static HostServices hostServices;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Save the HostServices reference for use in controllers.
        hostServices = getHostServices();

        // Load the splash screen first.
        FXMLLoader splashLoader = new FXMLLoader(getClass().getResource("splash-screen.fxml"));
        Parent splashRoot = splashLoader.load();
        SplashScreenController splashController = splashLoader.getController();

        // Create and set up the splash screen scene.
        Scene splashScene = new Scene(splashRoot, 600, 450);
        splashScene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("style.css")).toExternalForm());
        Stage splashStage = new Stage();
        splashStage.setScene(splashScene);
        splashStage.setResizable(false);
        splashStage.initStyle(UNDECORATED);
        splashStage.show();

        // When the splash loading is finished, close it and load the login screen.
        splashController.setLoadingBarFinished(() -> {
            Platform.runLater(() -> {
                try {
                    splashStage.close();
                    Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("login-screen.fxml")));
                    Scene scene = new Scene(root);
                    primaryStage.setTitle("AI Whiteboard Teaching Tool Login");
                    primaryStage.setScene(scene);
                    primaryStage.show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
    }

    /**
     * Getter to allow other classes to access the HostServices.
     */
    public static HostServices getStaticHostServices() {
        return hostServices;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
