package edu.farmingdale.advancedprogramming_capstone_project;

import javafx.application.Application;

import javafx.application.Platform;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.Objects;
import static javafx.stage.StageStyle.UNDECORATED;


/**
 * CapstoneApp is the main launcher for the application.
 * It loads main.fxml, which provides options for starting/joining calls,
 * generating summaries, and live transcription.
 */
public class CapstoneApp extends Application {


    @Override
    public void start(Stage primaryStage) throws Exception {

        // loads splash screen first before main fxml file
        FXMLLoader splashLoader = new FXMLLoader(getClass().getResource("splash-screen.fxml"));
        Parent splashRoot = splashLoader.load();
        SplashScreenController splashController = splashLoader.getController(); // gets from Controller class

        // makes new scene for splash screen setting style and size
        // makes style undecorated (no menu bar & label)
        Scene splashScene = new Scene(splashRoot, 600, 450);
        splashScene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("style.css")).toExternalForm());
        Stage splashStage = new Stage();
        splashStage.setScene(splashScene);
        splashStage.setResizable(false);
        splashStage.initStyle(UNDECORATED);
        splashStage.show();

         // when loading bar is finished in splash scene
         // it will load the main fxml file
        splashController.setLoadingBarFinished(() -> {

            // makes main fxml run after splash screen is closed
            Platform.runLater(() -> {
              
                try {
                    splashStage.close();
                    Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("main.fxml")));
                    Scene scene = new Scene(root, 1000, 800);
                    scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("style.css")).toExternalForm());

                    primaryStage.setTitle("AI Whiteboard Teaching Tool");
                    primaryStage.setScene(scene);
                    primaryStage.show();
                }

                catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });

    }
//test
    public static void main(String[] args) {
        launch(args);
    }
}
