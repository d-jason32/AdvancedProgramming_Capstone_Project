package edu.farmingdale.advancedprogramming_capstone_project;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * CapstoneApp is the main launcher for the application.
 * It loads main.fxml, which provides options for starting/joining calls,
 * generating summaries, and live transcription.
 */
public class CapstoneApp extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("main.fxml"));
        Scene scene = new Scene(root, 600, 400);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        primaryStage.setTitle("AI Whiteboard Teaching Tool");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
//test
    public static void main(String[] args) {
        launch(args);
    }
}
