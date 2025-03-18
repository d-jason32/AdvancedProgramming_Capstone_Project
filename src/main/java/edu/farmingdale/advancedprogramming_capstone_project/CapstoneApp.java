package edu.farmingdale.advancedprogramming_capstone_project;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * CapstoneApp is the main class that starts the JavaFX application.
 * It loads the main.fxml file, which is the main menu of the application.
 */
public class CapstoneApp extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load the main screen layout from FXML
        Parent root = FXMLLoader.load(getClass().getResource("main.fxml"));
        Scene scene = new Scene(root, 600, 400);

        // Apply the stylesheet
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        // Set up the main application window
        primaryStage.setTitle("AI Whiteboard Teaching Tool");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Main method to launch the application.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
