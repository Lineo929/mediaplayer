package com.example.biim3210;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Entry point for the JavaFX Media Player assignment.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        MediaPlayerApp app = new MediaPlayerApp(primaryStage);
        app.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}