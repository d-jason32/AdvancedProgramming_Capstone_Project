package edu.farmingdale.advancedprogramming_capstone_project;

import com.teamdev.jxbrowser.browser.Browser;
import com.teamdev.jxbrowser.engine.Engine;
import com.teamdev.jxbrowser.engine.EngineOptions;
import com.teamdev.jxbrowser.media.MediaDeviceType;
import com.teamdev.jxbrowser.media.callback.SelectMediaDeviceCallback;
import com.teamdev.jxbrowser.permission.PermissionType;
import com.teamdev.jxbrowser.permission.callback.RequestPermissionCallback;
import com.teamdev.jxbrowser.view.javafx.BrowserView;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

import static com.teamdev.jxbrowser.engine.RenderingMode.HARDWARE_ACCELERATED;
import static javafx.stage.StageStyle.UNDECORATED;

/**
 * Main application class for the AI Whiteboard Teaching Tool.
 * It starts with a splash screen and then opens the login screen.
 * It also stores HostServices for opening URLs in the external browser.
 */
public class CapstoneApp extends Application {

    // This static field holds the HostServices reference for opening URLs.
    static HostServices hostServices;
    private static Runnable mainScreenCallback;

    // JxBrowser shared engine/browser/view
    private static Engine engine;
    private static Browser browser;
    private static BrowserView browserView;

    /**
     * @param primaryStage the primary stage for this application, onto which
     * the application scene can be set.
     * Applications may create other stages, if needed, but they will not be
     * primary stages.
     * @throws Exception if it fails to load the splash screen or login screen
     */
    @Override
    public void start(Stage primaryStage) throws Exception {

        // Initialize JxBrowser
        engine = Engine.newInstance(
                EngineOptions.newBuilder(HARDWARE_ACCELERATED)
                        .licenseKey("OK6AEKNYF3G5C6P2KP1QRPO105XCS6S5IVNLC5U02BUMZ4OJXBF58C6AYSQIEUMOEYPB17697RPSTGNXO6PCBJN615NC6X2L0KRP13YNL1ZZBS5I8CGSBTLVRSDQPPHNI30ARV6V65Z2KMLF8")
                        .build()
        );

        // CapstoneApp.start(), right after Engine.newInstance(...)
        engine.permissions().set(RequestPermissionCallback.class, (params, tell) -> {
            // Grant _all_ permissions unconditionally, including camera & mic
            tell.grant();
        });

        engine.mediaDevices().set(SelectMediaDeviceCallback.class, params -> {
            // Always pick the first available device of the requested type
            return SelectMediaDeviceCallback.Response
                    .select(params.mediaDevices().get(0));
        });

        System.out.println("Video devices: " +
                engine.mediaDevices().list(MediaDeviceType.VIDEO_DEVICE));
        System.out.println("Audio devices: " +
                engine.mediaDevices().list(MediaDeviceType.AUDIO_DEVICE));

        // Create browser and view
        browser     = engine.newBrowser();
        browserView = BrowserView.newInstance(browser);

        // Get Host Services for OAuth Web Connection
        hostServices = getHostServices();

        // Load the splash screen first.
        FXMLLoader splashLoader = new FXMLLoader(getClass().getResource("splash-screen.fxml"));
        Parent splashRoot = splashLoader.load();
        SplashScreenController splashController = splashLoader.getController();

        // Create and set up the splash screen scene.
        Scene splashScene = new Scene(splashRoot, 600, 480);
        splashScene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("styling/style.css")).toExternalForm());
        Stage splashStage = new Stage();
        splashStage.setScene(splashScene);
        splashStage.setResizable(false);
        splashStage.initStyle(UNDECORATED);
        splashStage.show();

        // When the splash loading is finished, close it and load the login screen.
        // If the login screen authenticates, the main screen loads.
        splashController.setLoadingBarFinished(() -> {
            Platform.runLater(() -> {
                try {
                    splashStage.close();
                    FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("login-screen.fxml"));
                    Parent loginRoot = loginLoader.load();
                    LoginController loginController = loginLoader.getController();

                    // Create the main screen callback first
                    mainScreenCallback = () -> {
                        try {
                            FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("main.fxml"));
                            Parent mainRoot = mainLoader.load();
                            Stage stage = new Stage();
                            stage.setScene(new Scene(mainRoot, 1000, 800));
                            stage.setTitle("AI Whiteboard Program");
                            stage.show();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    };


                    // Set the callback BEFORE showing the login screen
                    LoginController.setOnLoginSuccess(CapstoneApp.getMainScreenCallback());
                    LoginController.setHostServices(hostServices);

                    // Show login screen
                    primaryStage.setScene(new Scene(loginRoot));
                    primaryStage.setTitle("AI Whiteboard Teaching Tool Login");
                    primaryStage.show();

                    // Debug output
                    System.out.println("Main callback set on login controller: " +
                            (loginController.mainScreenCallback != null));

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
    }

    @Override
    public void stop() {
        if (browser != null) browser.close();
        if (engine  != null) engine.close();
    }

    /**
     * Allow other classes (e.g. BrowserViewController) to get the Engine
     */
    public static Engine getEngine() {
        return engine;
    }

    /**
     * Allow other classes to get the shared Browser if needed
     */
    public static Browser getBrowser() {
        return browser;
    }

    public static BrowserView getBrowserView() {
        return browserView;
    }

    public static Runnable getMainScreenCallback(){
        return mainScreenCallback;
    }

    public static void main(String[] args) {
        launch(args);
    }
}