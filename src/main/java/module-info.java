module edu.farmingdale.advancedprogramming_capstone_project {
    requires javafx.fxml;
    requires javafx.web;
    requires java.sql;
    requires client.sdk;
    requires java.net.http;
    requires com.azure.communication.identity;

    requires java.desktop;


    opens edu.farmingdale.advancedprogramming_capstone_project to javafx.fxml;
    exports edu.farmingdale.advancedprogramming_capstone_project;
}
