module edu.farmingdale.advancedprogramming_capstone_project {
    requires javafx.fxml;
    requires javafx.web;
    requires java.net.http;
    requires client.sdk;

    opens edu.farmingdale.advancedprogramming_capstone_project to javafx.fxml;
    exports edu.farmingdale.advancedprogramming_capstone_project;
}
