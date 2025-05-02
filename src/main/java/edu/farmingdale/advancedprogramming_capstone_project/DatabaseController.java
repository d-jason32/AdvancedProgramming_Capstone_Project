package edu.farmingdale.advancedprogramming_capstone_project;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

/**
 * Class that connects a database to the GUI.
 * @author Moaath Alrajab
 * @author Jason Devaraj
 */
public class DatabaseController implements Initializable {
    private ConnDbOps cdbop;
    @FXML
    TextField id, first_name, last_name, email, password;

    @FXML
    private TableView<Person> tv;

    @FXML
    private TableColumn<Person, Integer> tv_id;

    @FXML
    private TableColumn<Person, String> tv_fn, tv_ln, tv_email, tv_password;

    @FXML
    ImageView img_view;

    @FXML
    private TextArea feedback;

    private final ObservableList<Person> data = FXCollections.observableArrayList();

    /**
     * Initializes table view.
     * @param url Pointer To Resource File
     * @param resourceBundle Resource Bundle
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        tv_id.setCellValueFactory(new PropertyValueFactory<>("id"));
        tv_fn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        tv_ln.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        tv_email.setCellValueFactory(new PropertyValueFactory<>("email"));
        tv_password.setCellValueFactory(new PropertyValueFactory<>("password"));

        tv.setItems(data);

        cdbop = new ConnDbOps();

        try {
            cdbop.connectToDatabase();
            feedback.setText("Connected to " + cdbop.MYSQL_SERVER_URL);
        } catch (Exception e) {
            feedback.setText(String.valueOf(e));
        }
        display();

        TextField[] fields = { id, first_name, last_name, email, password};

        /*
        After each text field is clicked on, every
        field will be checked if it is correct.
         */
        for (TextField field : fields) {
            field.setOnMouseClicked(e -> checkIfCorrect());
        }
    }

    /**
     * Opens finder to select an image.
     */
    @FXML
    protected void showImage() {
        File file= (new FileChooser()).showOpenDialog(img_view.getScene().getWindow());
        if(file!=null){
            img_view.setImage(new Image(file.toURI().toString()));
        }
    }

    /**
     * Allows you to select an item in a table.
     * @param mouseEvent Mouse Event
     */
    @FXML
    protected void selectedItemTV(MouseEvent mouseEvent) {
        Person p= tv.getSelectionModel().getSelectedItem();
        id.setText(Integer.toString(p.getId()));
        first_name.setText(p.getFirstName());
        last_name.setText(p.getLastName());
        email.setText(p.getEmail());
        password.setText(p.getPassword());
    }

    /**
     * Method to connect to a database.
     * @param event Event
     */
    @FXML
    void connectButton(ActionEvent event) {
        try {
            cdbop.connectToDatabase();
            feedback.setText("Connected to " + cdbop.MYSQL_SERVER_URL);
        } catch (Exception e) {
            feedback.setText(String.valueOf(e));
        }
        display();
    }

    /**
     * Method to enter student id and delete it from the database.
     * @param event Event
     */
    @FXML
    void deleteByID(ActionEvent event) {
        cdbop.delete(id.getText());
        display();
        feedback.setText("Deleted!");
    }

    /**
     * Assigns display to the display button.
     * @param event Event
     */
    @FXML
    void displayButton(ActionEvent event) {
        display();
    }

    /**
     * Adds a database to the table view.
     */
    void display(){
        data.clear();
        data.addAll(cdbop.displayAllUsers());
        tv.setItems(data);
    }

    /**
     * Edit a student record based on their id.
     * @param event Event
     * @throws SQLException SQL Exception
     */
    @FXML
    void editButton(ActionEvent event) throws SQLException {
        String num = id.getText();
        String firstName = first_name.getText();
        String lastName = last_name.getText();
        String dept = email.getText();
        String majorText = password.getText();

        cdbop.editUser(num, firstName, lastName, dept, majorText);
        feedback.setText("Edit user: " + firstName + " " + lastName);
        display();

    }

    /**
     * Insert a student into the database.
     * @param event Event
     */
    @FXML
    void insertButton(ActionEvent event) {
        String num = id.getText();
        String firstName = first_name.getText();
        String lastName = last_name.getText();
        String dept = email.getText();
        String majorText = password.getText();

        cdbop.insertUser(num, firstName, lastName, dept, majorText);
        feedback.setText("Added user: " + firstName + " " + lastName);
        display();
    }

    /**
     * Query button gets the id from the text field, searches the database
     * and displays the entire Person.
     * @param event Event
     */
    @FXML
    void queryButton(ActionEvent event) {
        String userID = id.getText();
        String s = cdbop.queryUser(userID);
        feedback.setText(s);
    }
    Pattern idPattern = Pattern.compile("^\\d+$");
    Pattern firstNamePattern = Pattern.compile("^[a-zA-Z]{2,25}$");
    Pattern lastNamePattern = Pattern.compile("^[a-zA-Z]{2,25}$");
    Pattern emailPattern = Pattern.compile("^[a-zA-Z0-9._%+-]{1,25}@farmingdale\\.edu$");
    Pattern passwordPattern = Pattern.compile("^[a-zA-Z]{2,25}$");

    /**
     * Checks if every text field is valid.
     */
    void checkIfCorrect(){
        if (idPattern.matcher(id.getText()).matches()){
            id.setStyle("-fx-border-color:#6dff7c; -fx-border-width:2px;");
        }
        else {
            id.setStyle("-fx-border-color:red; -fx-border-width:2px;");
        }
        if (firstNamePattern.matcher(first_name.getText()).matches()){
            first_name.setStyle("-fx-border-color:#6dff7c; -fx-border-width:2px;");
        }
        else {
            first_name.setStyle("-fx-border-color:red; -fx-border-width:2px;");
        }
        if (lastNamePattern.matcher(last_name.getText()).matches()){
            last_name.setStyle("-fx-border-color:#6dff7c; -fx-border-width:2px;");
        }
        else {
            last_name.setStyle("-fx-border-color:red; -fx-border-width:2px;");
        }
        if (emailPattern.matcher(email.getText()).matches()){
            email.setStyle("-fx-border-color:#6dff7c; -fx-border-width:2px;");
        }
        else {
            email.setStyle("-fx-border-color:red; -fx-border-width:2px;");
        }
        if (passwordPattern.matcher(password.getText()).matches()){
            password.setStyle("-fx-border-color:#6dff7c; -fx-border-width:2px;");
        }
        else {
            password.setStyle("-fx-border-color:red; -fx-border-width:2px;");
        }
    }
}