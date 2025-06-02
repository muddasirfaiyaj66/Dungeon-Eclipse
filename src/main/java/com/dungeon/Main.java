package com.dungeon;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    private static final String TITLE = "Dungeon Eclipse";

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            // Load the main menu FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/dungeon/fxml/MainMenu.fxml"));
            Parent root = loader.load();
            
            // Create scene without fixed dimensions for fullscreen
            Scene scene = new Scene(root);
            
            // Add stylesheet if it exists
            try {
                scene.getStylesheets().add(getClass().getResource("/com/dungeon/styles/main.css").toExternalForm());
            } catch (Exception e) {
                System.out.println("Stylesheet not found: " + e.getMessage());
            }
            
            // Configure the window
            primaryStage.setTitle(TITLE);
            primaryStage.setScene(scene);
            
            // Make the window resizable
            primaryStage.setResizable(true);
            
            // Set to full screen
            primaryStage.setFullScreen(true);
            primaryStage.setFullScreenExitHint(""); // Optional: remove exit hint
            primaryStage.setFullScreenExitKeyCombination(javafx.scene.input.KeyCombination.NO_MATCH); // Prevent ESC from exiting full-screen
            
            // Set minimum window size (will apply if user exits fullscreen)
            primaryStage.setMinWidth(800);
            
            // Center the window on screen
            primaryStage.centerOnScreen();
            
            // Show the window
            primaryStage.show();
            
            System.out.println("Main window initialized successfully");
        } catch (Exception e) {
            System.err.println("Error initializing main window: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
