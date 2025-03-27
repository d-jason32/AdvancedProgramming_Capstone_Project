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
        if(usernameField.getText().equals("moses") && passwordField.getText().equals("12345"))
        {
            text.setText("Logging in...");
            text.setFill(Color.GREEN);
            FXMLLoader fxmlLoader = new FXMLLoader(CapstoneApp.class.getResource("main.fxml"));

            Scene scene = new Scene(fxmlLoader.load());
            Stage stage = new Stage();
            stage.setTitle("AI Whiteboard Program - Main Screen");
            stage.setScene(scene);
            stage.show();
        }
        else{
            System.out.println(usernameField.getText());
            text.setText("Incorrect Username and Password. Try Again.");
            text.setFill(Color.RED);
        }
    }
}
