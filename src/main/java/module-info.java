module edu.farmingdale.advancedprogramming_capstone_project {
    requires javafx.fxml;
    requires javafx.web;
    requires java.net.http;
    requires client.sdk;
    requires java.desktop;
    requires com.microsoft.aad.msal4j;
    requires jdk.httpserver;

    opens edu.farmingdale.advancedprogramming_capstone_project to javafx.fxml;
    exports edu.farmingdale.advancedprogramming_capstone_project;
}
