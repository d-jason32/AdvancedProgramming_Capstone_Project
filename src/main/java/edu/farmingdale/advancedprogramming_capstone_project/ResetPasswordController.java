package edu.farmingdale.advancedprogramming_capstone_project;

import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ResetPasswordController implements Initializable {
    private static HostServices hostServices;
    private ConnDbOps cdbop;
    private List<String> authDB;
    private Runnable mainScreenCallback;
    private Runnable devModeCallback;

    // FXML components
    @FXML
    public Label stateText;
    @FXML
    private TextField confidentialField;
    @FXML
    private Text errorTextPlaceholder;
    private Runnable onSuccessCallback;  // Instance variable

    public void setOnSuccess(Runnable callback) {
        this.onSuccessCallback = callback;
    }

    public static void setHostServices(HostServices services) {
        hostServices = services;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cdbop = new ConnDbOps();
        cdbop.connectToDatabase();
        ConnDbOps.AuthService.initializeAuthDB(cdbop);
        authDB = ConnDbOps.AuthService.getAuthDB();
        this.mainScreenCallback = CapstoneApp.getMainScreenCallback();
    }

    /**
     * Starts Email Processing
     * @param event
     */
    @FXML
    void onEnterButtonPress(ActionEvent event) {
        String email = confidentialField.getText().trim();
        boolean emailExists = false;

        if (email.isEmpty()) {
            errorTextPlaceholder.setText("Please enter your email address");
            return;
        }

        boolean authenticated = false;
        for (String s : authDB) {
            if (authDB.contains(email)) {
                emailExists = true;
                break;
            }
        }

        if (emailExists) {
            // Email is registered - placeholder for email sending functionality
            errorTextPlaceholder.setText("Verification email sent to " + email);
            // Will Add Email Verification Sys
        } else {
            errorTextPlaceholder.setText("This email is not linked to an account");
        }
    }

    /**
     * Used for testing Purposes
     * @param event
     */
    @FXML
    void onDevButtonPressed(ActionEvent event) {
        if (devModeCallback != null) {
            Platform.runLater(devModeCallback);
        } else if (mainScreenCallback != null) {
            Platform.runLater(mainScreenCallback);
        } else {
            System.err.println("No dev mode callback available");
        }
    }

    /**
     * Used to return to Sign In Page
     * @param event
     * @throws IOException
     */
    @FXML
    private void onSignInTextPressed(MouseEvent event) throws IOException {
        Node source = (Node) event.getSource();
        Stage currentStage = (Stage) source.getScene().getWindow();

        FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("login-screen.fxml"));
        Parent loginRoot = loginLoader.load();

        LoginController loginController = loginLoader.getController();
        loginController.setMainScreenCallback(mainScreenCallback);
        LoginController.setHostServices(hostServices);
        Stage loginStage = new Stage();
        loginStage.setScene(new Scene(loginRoot));
        loginStage.setTitle("AI Whiteboard Teaching Tool - Login");
        loginStage.show();

        currentStage.close();
    }


    /**
     * Used to return to Sign Up page
     * @param event
     * @throws IOException
     */
    @FXML
    private void onSignUpTextPressed(MouseEvent event) throws IOException {
        Node source = (Node) event.getSource();
        Stage currentStage = (Stage) source.getScene().getWindow();

        FXMLLoader signUpLoader = new FXMLLoader(getClass().getResource("signup-screen.fxml"));
        Parent signUpRoot = signUpLoader.load();

        SignUpController signUpController = signUpLoader.getController();
        signUpController.setOnSuccess(mainScreenCallback);
        signUpController.setHostServices(hostServices);
        Stage signUpStage = new Stage();
        signUpStage.setScene(new Scene(signUpRoot));
        signUpStage.setTitle("AI Whiteboard Teaching Tool - Sign Up");
        signUpStage.show();

        currentStage.close();
    }

    /**
     * Sets a thread for main screen
     * @param callback Runnable
     */
    public void setMainScreenCallback(Runnable callback) {
        this.mainScreenCallback = callback;
        System.out.println("Main screen callback set in ResetPasswordController: " + callback);
    }
}