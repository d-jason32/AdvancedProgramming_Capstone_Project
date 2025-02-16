module edu.farmingdale.advancedprogramming_capstone_project {
    requires javafx.controls;
    requires javafx.fxml;


    opens edu.farmingdale.advancedprogramming_capstone_project to javafx.fxml;
    exports edu.farmingdale.advancedprogramming_capstone_project;
}