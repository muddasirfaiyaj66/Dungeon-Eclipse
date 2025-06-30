package com.dungeon;

import com.dungeon.server.PuzzleServer;
import com.dungeon.utils.UIUtils;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Main extends Application {
    private static final String TITLE = "Dungeon Eclipse";

    @Override
    public void start(Stage primaryStage) throws Exception {

         new Thread(() -> {
            try {
                System.out.println("🚀 Starting PuzzleServer...");
                PuzzleServer.start(); 
            } catch (Exception e) {
                System.err.println("❌ Failed to start PuzzleServer: " + e.getMessage());
                e.printStackTrace();
                System.err.println("💡 Try checking if port 9999 is already in use");
            }
        }).start();
        
        // Give the server a moment to start
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Test if the server is accessible
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Wait a bit more for server to fully start
                if (PuzzleServer.testConnection()) {
                    System.out.println("🎉 Puzzle server is ready for puzzle chat!");
                } else {
                    System.err.println("⚠️  Puzzle server may not be working properly");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
        
        try {
            // Set application icon using the utility method
            UIUtils.setStageIcon(primaryStage);

            // Set window title
            primaryStage.setTitle(TITLE);

            // Load the splash screen FXML first
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/dungeon/fxml/SplashScreen.fxml"));
            Parent root = loader.load();
            root.setOpacity(0.0); // Start with content invisible
            
            // Create scene without fixed dimensions for fullscreen
            Scene scene = new Scene(root);
            
            // Add stylesheet if it exists
            try {
                scene.getStylesheets().add(getClass().getResource("/com/dungeon/styles/main.css").toExternalForm());
            } catch (Exception e) {
                System.out.println("Stylesheet not found: " + e.getMessage());
            }
            
            // Configure the window
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
            
            // Prevent the window from being closed by OS controls (e.g., Alt+F4, 'X' button)
            primaryStage.setOnCloseRequest(event -> {
                event.consume(); 
            });
            
            // Fade in the content to avoid initial flicker
            FadeTransition fadeIn = new FadeTransition(Duration.seconds(1), root);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
            
            System.out.println("Main window initialized successfully");
        } catch (Exception e) {
            System.err.println("Error initializing main window: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @Override
    public void stop() throws Exception {
        com.dungeon.audio.SoundManager.getInstance().shutdown();
        com.dungeon.server.PuzzleServer.shutdown();
        super.stop();
    }
    public static void main(String[] args) {
        // Add shutdown hook to ensure PuzzleServer is closed
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("🛑 Application shutdown detected, cleaning up...");
            com.dungeon.server.PuzzleServer.shutdown();
        }));
        
        launch(args);
    }
}