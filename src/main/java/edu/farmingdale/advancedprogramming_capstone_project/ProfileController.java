package edu.farmingdale.advancedprogramming_capstone_project;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

/**
 * Controller for profilePage.fxml.
 *  • Lets the user pick a new avatar picture (Change button)
 */
public class ProfileController implements Initializable {

    @FXML private Circle avatarCircle;
    @FXML private Button changeAvatarBtn;

    /* Other fields (already in your FXML) — keep them if you use them */
    @FXML private TextField  firstNameField;
    @FXML private TextField  lastNameField;
    @FXML private TextField  emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextArea   bioArea;
    @FXML private Button     saveBtn;

    /* ------------ persistent prefs ------------ */
    private final Preferences prefs = Preferences.userNodeForPackage(ProfileController.class);

    /* ------------ life‑cycle ------------ */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        /* Load saved profile fields */
        firstNameField.setText(prefs.get("firstName", ""));
        lastNameField.setText (prefs.get("lastName" , ""));
        emailField.setText    (prefs.get("email"    , ""));
        passwordField.setText (prefs.get("password" , ""));
        bioArea.setText       (prefs.get("bio"      , ""));

        /* Hook up Change button */
        changeAvatarBtn.setOnAction(e -> chooseNewAvatar());

        /* Hook up Save button */
        saveBtn.setOnAction(e -> saveProfile());

        TextField[] fields = { firstNameField, lastNameField, emailField, passwordField};

        /*
        After each text field is clicked on, every
        field will be checked if it is correct.
         */
        for (TextField field : fields) {
            field.setOnMouseClicked(e -> {
                checkIfCorrect();
            });
        }
    }

    /* ------------ avatar logic ------------ */
    private void chooseNewAvatar() {

        FileChooser fc = new FileChooser();
        fc.setTitle("Select profile picture");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));

        Window owner = changeAvatarBtn.getScene().getWindow();
        File chosen = fc.showOpenDialog(owner);
        if (chosen == null) return;            // user cancelled

        Image img = new Image(chosen.toURI().toString(), false);
        setAvatar(img);
    }

    private void setAvatar(Image img) {
        avatarCircle.setFill(new ImagePattern(img));
    }

    /* ------------ save profile ------------ */
    private void saveProfile() {
        prefs.put("firstName", firstNameField.getText());
        prefs.put("lastName",  lastNameField.getText());
        prefs.put("email",     emailField.getText());
        prefs.put("password",  passwordField.getText());
        prefs.put("bio",       bioArea.getText());
    }

    Pattern firstNamePattern = Pattern.compile("^[a-zA-Z]{2,25}$");
    Pattern lastNamePattern = Pattern.compile("^[a-zA-Z]{2,25}$");
    Pattern emailPattern = Pattern.compile("^[a-zA-Z0-9._%+-]{1,25}@farmingdale\\.edu$");
    Pattern passwordPattern = Pattern.compile("^[a-zA-Z]{2,25}$");

    /**
     * Checks if every text field is valid.
     */
    void checkIfCorrect(){
        if (firstNamePattern.matcher(firstNameField.getText()).matches()){
            firstNameField.setStyle("-fx-border-color:#6dff7c; -fx-border-width:2px;");
        }
        else {
            firstNameField.setStyle("-fx-border-color:red; -fx-border-width:2px;");
        }
        if (lastNamePattern.matcher(lastNameField.getText()).matches()){
            lastNameField.setStyle("-fx-border-color:#6dff7c; -fx-border-width:2px;");
        }
        else {
            lastNameField.setStyle("-fx-border-color:red; -fx-border-width:2px;");
        }
        if (lastNamePattern.matcher(lastNameField.getText()).matches()){
            lastNameField.setStyle("-fx-border-color:#6dff7c; -fx-border-width:2px;");
        }
        else {
            lastNameField.setStyle("-fx-border-color:red; -fx-border-width:2px;");
        }
        if (emailPattern.matcher(emailField.getText()).matches()){
            emailField.setStyle("-fx-border-color:#6dff7c; -fx-border-width:2px;");
        }
        else {
            emailField.setStyle("-fx-border-color:red; -fx-border-width:2px;");
        }
        if (passwordPattern.matcher(passwordField.getText()).matches()){
            passwordField.setStyle("-fx-border-color:#6dff7c; -fx-border-width:2px;");
        }
        else {
            passwordField.setStyle("-fx-border-color:red; -fx-border-width:2px;");
        }
    }

}