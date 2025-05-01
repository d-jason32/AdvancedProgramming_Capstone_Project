package edu.farmingdale.advancedprogramming_capstone_project;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;
import static javafx.stage.StageStyle.UNDECORATED;

/**
 * Main application class for the AI Whiteboard Teaching Tool.
 * It starts with a splash screen and then opens the login screen.
 * It also stores HostServices for opening URLs in the external browser.
 */
public class CapstoneApp extends Application {

    private static HostServices hostServices;

    /**
     * @param primaryStage the primary stage for this application, onto which
     * the application scene can be set.
     * Applications may create other stages, if needed, but they will not be
     * primary stages.
     * @throws Exception if it fails to load the splash screen or login screen
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        hostServices = getHostServices();

        FXMLLoader splashLoader = new FXMLLoader(getClass().getResource("splash-screen.fxml"));
        Parent splashRoot = splashLoader.load();
        SplashScreenController splashController = splashLoader.getController();

        Scene splashScene = new Scene(splashRoot, 600, 480);
        splashScene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("styling/style.css")).toExternalForm());
        Stage splashStage = new Stage();
        splashStage.setScene(splashScene);
        splashStage.setResizable(false);
        splashStage.initStyle(UNDECORATED);
        splashStage.show();

        splashController.setLoadingBarFinished(() -> Platform.runLater(() -> {
            try {
                splashStage.close();
                FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("login-screen.fxml"));
                Parent loginRoot = loginLoader.load();
                LoginController loginController = loginLoader.getController();

                loginController.setHostServices(hostServices);
                loginController.setOnLoginSuccess(() -> Platform.runLater(() -> {
                    try {
                        FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("main.fxml"));
                        Parent mainRoot = mainLoader.load();

                        primaryStage.setScene(new Scene(mainRoot, 1000, 800));
                        primaryStage.setTitle("AI Whiteboard Program");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }));

                primaryStage.setScene(new Scene(loginRoot));
                primaryStage.setTitle("AI Whiteboard Teaching Tool Login");
                primaryStage.show();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }

    /**
     * Getter to allow other classes to access the HostServices.
     */
    public static HostServices getStaticHostServices() {
        return hostServices;
    }

    /**
     * Main method to launch the application.
     * @param args Command-line arguments.
     */
    public static void main(String[] args) {
        launch(args);
    }
}