package com.dungeon.controllers;

import com.dungeon.data.ScoreManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputDialog;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.Optional;

public class VictoryController {
    @FXML
    private Text congratsText;
    
    @FXML
    private Text scoreText;
    
    @FXML
    private Text timeText;
    
    @FXML
    private Text enemiesText;
    
    @FXML
    private Button nextLevelButton;
    
    @FXML
    private Button mainMenuButton;
    
    @FXML
    private Button quitButton;
    
    private int score;
    private String timeElapsed;
    private int enemiesDefeated;
    private int currentLevel;
    private GameController gameController;
    
    public void setGameStats(int score, String timeElapsed, int enemiesDefeated, int level) {
        this.score = score;
        this.timeElapsed = timeElapsed;
        this.enemiesDefeated = enemiesDefeated;
        this.currentLevel = level;
        
        String levelText = level >= 3 ? "Congratulations! You have defeated the final boss and won the game!" : "Victory! You have completed level " + level + "!";
        congratsText.setText(levelText);
        scoreText.setText("Final Score: " + score);
        timeText.setText("Time: " + timeElapsed);
        enemiesText.setText("Enemies Defeated: " + enemiesDefeated);
        
        // Disable next level button if on final level (e.g. level 3 or higher based on game design)
        nextLevelButton.setDisable(level >= 3); 

        // Prompt for player name and save score
        // Run on JavaFX application thread to ensure UI operations are safe
        Platform.runLater(() -> {
            TextInputDialog dialog = new TextInputDialog("Player");
            dialog.setTitle("Victory!");
            dialog.setHeaderText("You won with a score of: " + score + "! Enter your name for the high scores:");
            dialog.setContentText("Name:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(name -> {
                if (!name.trim().isEmpty()) {
                    ScoreManager.saveScore(name, this.score);
                } else {
                    ScoreManager.saveScore("Champion", this.score); // Default name for victors
                }
            });
        });
    }
    
    public void setGameController(GameController gameController) {
        this.gameController = gameController;
    }
    
    @FXML
    @SuppressWarnings("unused")
    private void startNextLevel() {
        if (currentLevel >= 3) return; // Should not happen if button is disabled, but good check

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/dungeon/fxml/GameScene.fxml"));
            Parent gameRoot = loader.load();
            Scene gameScene = new Scene(gameRoot, 1024, 768);
            
            Stage stage = (Stage) nextLevelButton.getScene().getWindow();
            
            // Configure stage properties
            stage.setResizable(true);
            stage.setMinWidth(800);
            stage.setMinHeight(600);
            
            stage.setScene(gameScene);
            
            // Make sure the stage is showing
            if (!stage.isShowing()) {
                stage.show();
            }
            
            GameController newGameController = loader.getController(); // Renamed for clarity
            Platform.runLater(() -> {
                System.out.println("Initializing game controller for next level...");
                newGameController.onSceneReady();
                
                // Set the next level - add 1 to current level
                newGameController.setLevel(this.currentLevel + 1);
                
                // Force focus on the game canvas
                gameScene.getRoot().requestFocus();
            });
        } catch (Exception e) {
            System.err.println("Error starting next level: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    @SuppressWarnings("unused")
    private void returnToMainMenu() {
        if (gameController != null) {
            // Use the consistent method from GameController
            gameController.returnToMainMenu();
        } else {
            // Fallback to original implementation
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/dungeon/fxml/MainMenu.fxml"));
                Parent menuRoot = loader.load();
                Scene menuScene = new Scene(menuRoot, 1024, 768);
                
                // Add stylesheet
                try {
                    menuScene.getStylesheets().add(getClass().getResource("/com/dungeon/styles/main.css").toExternalForm());
                } catch (Exception e) {
                    System.out.println("Stylesheet not found: " + e.getMessage());
                }
                
                Stage stage = (Stage) mainMenuButton.getScene().getWindow();
                
                // Configure stage properties to match Main.java
                stage.setResizable(true);
                stage.setMinWidth(800);
                stage.setMinHeight(600);
                stage.centerOnScreen();
                
                stage.setScene(menuScene);
                stage.show();
            } catch (Exception e) {
                System.err.println("Error returning to main menu: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    @FXML
    @SuppressWarnings("unused")
    private void quitGame() {
        Platform.exit();
    }
} 