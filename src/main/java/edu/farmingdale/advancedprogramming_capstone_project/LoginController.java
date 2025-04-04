package edu.farmingdale.advancedprogramming_capstone_project;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class LoginController {
    @FXML
    private Button enterButton;

    @FXML
    private Text text;

    @FXML
    private TextField passwordField;

    @FXML
    private TextField usernameField;

    @FXML
    void onEnterButtonPress(ActionEvent event) throws IOException {
        if(usernameField.getText().equals("tester") && passwordField.getText().equals("12345"))
        {
            text.setText("Logging in...");
            text.setFill(Color.GREEN);
            FXMLLoader fxmlLoader = new FXMLLoader(CapstoneApp.class.getResource("main.fxml"));

            Scene scene = new Scene(fxmlLoader.load());
            Stage mainstage = new Stage();
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("style.css")).toExternalForm());
            mainstage.setTitle("AI Whiteboard Program - Main Screen");
            mainstage.setScene(scene);
            mainstage.show();
            Stage currentStage = (Stage) enterButton.getScene().getWindow();
            currentStage.close();
        }
        else{
            System.out.println(usernameField.getText());
            text.setText("Incorrect Username and Password. Try Again.");
            text.setFill(Color.RED);
        }
    }
}
