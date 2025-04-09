package com.dungeon.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class MainMenuController {
    
    @FXML
    private Button newGameButton;
    
    @FXML
    private Button tutorialButton;
    
    @FXML
    private Button highScoreButton;
    
    @FXML
    private Button optionsButton;
    
    @FXML
    private Button exitButton;
    
    @FXML
    @SuppressWarnings("unused")
    private void startNewGame() {
        try {
            System.out.println("Starting new game...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/dungeon/fxml/GameScene.fxml"));
            Parent gameRoot = loader.load();
            
            // Create a properly sized scene
            Scene gameScene = new Scene(gameRoot, 1024, 768);
            
            // Get the current stage from the button
            Stage stage = (Stage) newGameButton.getScene().getWindow();
            
            // Configure stage properties
            stage.setResizable(true);
            stage.setMinWidth(800);
            stage.setMinHeight(600);
            
            // Get the game controller before setting the scene
            GameController gameController = loader.getController();
            
            // Set the scene
            stage.setScene(gameScene);
            
            // Make sure the stage is showing
            if (!stage.isShowing()) {
                stage.show();
            }
            
            // Make sure we initialize the game controller after the scene is shown
            Platform.runLater(() -> {
                System.out.println("Initializing game controller...");
                gameController.onSceneReady();
                
                // Force focus on the game canvas
                gameScene.getRoot().requestFocus();
                
                System.out.println("Game initialized successfully");
            });
        } catch (Exception e) {
            System.err.println("Error starting new game: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    @SuppressWarnings("unused")
    private void openTutorial() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/dungeon/fxml/Tutorial.fxml"));
            Parent tutorialRoot = loader.load();
            Scene tutorialScene = new Scene(tutorialRoot);
            
            Stage tutorialStage = new Stage();
            tutorialStage.setTitle("How to Play");
            tutorialStage.setScene(tutorialScene);
            tutorialStage.show();
        } catch (Exception e) {
            // If the tutorial screen doesn't exist yet, show a simple alert
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Tutorial");
            alert.setHeaderText("How to Play Dungeon Eclipse");
            alert.setContentText(
                "WASD: Move your character\n" +
                "Mouse: Aim\n" +
                "Left Click: Attack\n" +
                "E: Interact with objects\n" +
                "I: Open inventory\n" +
                "ESC: Pause game\n\n" +
                "Explore dungeons, defeat enemies, solve puzzles, and collect treasures!"
            );
            alert.showAndWait();
            e.printStackTrace();
        }
    }
    
    @FXML
    @SuppressWarnings("unused")
    private void openHighScores() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/dungeon/fxml/HighScores.fxml"));
            Parent highScoresRoot = loader.load();
            Scene highScoresScene = new Scene(highScoresRoot);
            
            Stage highScoresStage = new Stage();
            highScoresStage.setTitle("High Scores");
            highScoresStage.setScene(highScoresScene);
            highScoresStage.show();
        } catch (Exception e) {
            // If the high scores screen doesn't exist yet, show a simple alert
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("High Scores");
            alert.setHeaderText("High Scores");
            alert.setContentText("No scores recorded yet. Play the game to set new records!");
            alert.showAndWait();
            e.printStackTrace();
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void openOptions() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/dungeon/fxml/Options.fxml"));
            Parent optionsRoot = loader.load();
            Scene optionsScene = new Scene(optionsRoot);
            
            Stage optionsStage = new Stage();
            optionsStage.setTitle("Options");
            optionsStage.setScene(optionsScene);
            optionsStage.show();
        } catch (Exception e) {
            // If the options screen doesn't exist yet, show a simple alert
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Options");
            alert.setHeaderText("Game Options");
            alert.setContentText("Options menu is coming soon!");
            alert.showAndWait();
            e.printStackTrace();
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void exitGame() {
        Platform.exit();
    }
}
