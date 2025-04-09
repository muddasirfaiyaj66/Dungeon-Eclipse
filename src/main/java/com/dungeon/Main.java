package com.dungeon;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    private static final String TITLE = "Dungeon Eclipse";
    private static final int WINDOW_WIDTH = 1024;
    private static final int WINDOW_HEIGHT = 768;

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            // Load the main menu FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/dungeon/fxml/MainMenu.fxml"));
            Parent root = loader.load();
            
            // Create scene with the proper dimensions
            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
            
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
            
            // Set minimum window size
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            
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
