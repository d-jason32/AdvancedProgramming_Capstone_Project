package edu.farmingdale.advancedprogramming_capstone_project;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;

public class LoginController {

    /* In progress
    private static final String CLIENT_ID = "395068cb-ce9c-484a-b2cb-ff12ad9a7ae7";
    private static final String AUTHORITY = "https://login.microsoftonline.com/9e4341d9-236d-4377-a13b-819911255480";
    private static final String REDIRECT_URI = "http://localhost:8080";
    private static final String[] SCOPES = {"User.Read"};


     */
    @FXML public Text stateLink;
    @FXML public Label stateText;
    @FXML private Button enterButton;
    @FXML private TextField passwordField;
    @FXML private TextField usernameField;

    private int state = 0;
/*
    @FXML
    private void handleSignIn(ActionEvent event) {
        authenticateAndLoadMainApp();
    }
*/
    @FXML
    void onEnterButtonPress(ActionEvent event) throws IOException {
        loadMainApplication();
    }
/*
    private void authenticateAndLoadMainApp() {
        try {
            PublicClientApplication pca = PublicClientApplication.builder(CLIENT_ID)
                    .authority(AUTHORITY)
                    .build();

            CompletableFuture<IAuthenticationResult> future =
                    (CompletableFuture<IAuthenticationResult>) pca.acquireToken(
                            InteractiveRequestParameters.builder(new URI(REDIRECT_URI))
                                    .scopes(new HashSet<>(Collections.singletonList(SCOPES[0])))
                                    .build()
                    );

            future.thenAccept(result -> {
                Platform.runLater(() -> {
                    System.out.println("Access Token: " + result.accessToken());
                    try {
                        loadMainApplication();
                    } catch (IOException e) {
                        stateText.setText("Failed to load application");
                    }
                });
            }).exceptionally(ex -> {
                Platform.runLater(() ->
                        stateText.setText("Login failed: " + ex.getCause().getMessage()));
                return null;
            });

        } catch (Exception e) {
            stateText.setText("Error: " + e.getMessage());
        }
    }
*/
    private void loadMainApplication() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
        Stage stage = new Stage();
        stage.setScene(new Scene(loader.load(), 1280, 800));
        stage.setTitle("AI Whiteboard Program");
        stage.show();
        ((Stage) enterButton.getScene().getWindow()).close();
    }

    @FXML
    public void onSignInTextPressed(MouseEvent event) {
        if(state == 0) {
            stateLink.setText("Already have an account? Sign in!");
            stateText.setText("Sign up");
            state = 1;
        } else {
            stateLink.setText("Don't have an account? Sign up!");
            stateText.setText("Sign in");
            state = 0;
        }
    }
}
        /*
        if (usernameField.getText().equals("tester") && passwordField.getText().equals("12345")) {

            FXMLLoader fxmlLoader = new FXMLLoader(CapstoneApp.class.getResource("main.fxml"));

            Scene scene = new Scene(fxmlLoader.load(), 1280, 800);
            Stage mainstage = new Stage();
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("style.css")).toExternalForm());
            mainstage.setTitle("AI Whiteboard Program - Main Screen");
            mainstage.setScene(scene);
            mainstage.show();
            Stage currentStage = (Stage) enterButton.getScene().getWindow();
            currentStage.close();
        */
