package com.dungeon.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class SplashScreenController {

    @FXML
    private StackPane rootPane;

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            if (rootPane.getScene() != null) {
                // A deep, dark blue for the transition background
                rootPane.getScene().setFill(Color.rgb(51, 54, 57)); 
            }
        });

        // Wait for 2 seconds before starting the fade-out
        PauseTransition delay = new PauseTransition(Duration.seconds(5));
        delay.setOnFinished(event -> fadeOutAndLoadMainMenu());
        delay.play();
    }

    private void fadeOutAndLoadMainMenu() {
        // Create a fade-out transition for the splash screen
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(1), rootPane);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        
        // When the fade-out is finished, load the main menu
        fadeOut.setOnFinished(event -> {
            try {
                // Load the main menu FXML
                Parent mainMenuRoot = FXMLLoader.load(getClass().getResource("/com/dungeon/fxml/MainMenu.fxml"));
                
                // Make the main menu transparent initially for the fade-in
                mainMenuRoot.setOpacity(0.0);
                
                // Get the current scene
                Scene currentScene = rootPane.getScene();
                
                // Replace the root of the scene with the main menu
                currentScene.setRoot(mainMenuRoot);
                
                // Create and play a fade-in transition for the main menu
                FadeTransition fadeIn = new FadeTransition(Duration.seconds(1), mainMenuRoot);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        
        // Start the fade-out animation
        fadeOut.play();
    }
} 