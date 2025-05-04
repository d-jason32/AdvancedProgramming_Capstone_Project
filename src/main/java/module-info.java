module edu.farmingdale.advancedprogramming_capstone_project {
    requires javafx.fxml;
    requires javafx.web;
    requires java.net.http;
    requires client.sdk;
    requires java.prefs;
    requires jdk.httpserver;
    requires com.google.gson;
    requires com.microsoft.aad.msal4j;
    requires jbcrypt;
    requires java.dotenv;
    requires com.fasterxml.jackson.databind;
    requires java.sql;
    requires annotations;
    requires kernel;
    requires layout;

    opens edu.farmingdale.advancedprogramming_capstone_project to javafx.fxml, com.microsoft.aad.msal4j;
    exports edu.farmingdale.advancedprogramming_capstone_project;
}


