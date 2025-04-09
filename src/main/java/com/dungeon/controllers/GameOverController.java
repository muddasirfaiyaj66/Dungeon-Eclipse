package com.dungeon.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class GameOverController {
    @FXML
    private Text scoreText;
    
    @FXML
    private Text levelText;
    
    @FXML
    private Text enemiesText;
    
    @FXML
    private Button tryAgainButton;
    
    @FXML
    private Button mainMenuButton;
    
    @FXML
    private Button quitButton;
    
    private int score;
    private int levelsCompleted;
    private int enemiesDefeated;
    
    public void setGameStats(int score, int levelsCompleted, int enemiesDefeated) {
        this.score = score;
        this.levelsCompleted = levelsCompleted;
        this.enemiesDefeated = enemiesDefeated;
        
        scoreText.setText("Score: " + score);
        levelText.setText("Levels Completed: " + levelsCompleted);
        enemiesText.setText("Enemies Defeated: " + enemiesDefeated);
    }
    
    @FXML
    @SuppressWarnings("unused")
    private void restartGame() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/dungeon/fxml/GameScene.fxml"));
            Parent gameRoot = loader.load();
            Scene gameScene = new Scene(gameRoot, 1024, 768);
            
            Stage stage = (Stage) tryAgainButton.getScene().getWindow();
            
            // Configure stage properties
            stage.setResizable(true);
            stage.setMinWidth(800);
            stage.setMinHeight(600);
            
            stage.setScene(gameScene);
            
            // Make sure the stage is showing
            if (!stage.isShowing()) {
                stage.show();
            }
            
            GameController gameController = loader.getController();
            Platform.runLater(() -> {
                System.out.println("Initializing game controller from game over screen...");
                gameController.onSceneReady();
                
                // Force focus on the game canvas
                gameScene.getRoot().requestFocus();
            });
        } catch (Exception e) {
            System.err.println("Error restarting game: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    @SuppressWarnings("unused")
    private void returnToMainMenu() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/dungeon/fxml/MainMenu.fxml"));
            Parent menuRoot = loader.load();
            Scene menuScene = new Scene(menuRoot);
            
            Stage stage = (Stage) mainMenuButton.getScene().getWindow();
            stage.setScene(menuScene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    @SuppressWarnings("unused")
    private void quitGame() {
        Platform.exit();
    }
} 