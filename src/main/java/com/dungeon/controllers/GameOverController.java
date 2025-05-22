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
    private GameController gameController;
    
    public void setGameStats(int score, int levelsCompleted, int enemiesDefeated) {
        this.score = score;
        this.levelsCompleted = levelsCompleted;
        this.enemiesDefeated = enemiesDefeated;
        
        scoreText.setText("Score: " + score);
        levelText.setText("Levels Completed: " + levelsCompleted);
        enemiesText.setText("Enemies Defeated: " + enemiesDefeated);
    }
    
    public void setGameController(GameController gameController) {
        this.gameController = gameController;
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
        System.out.println("returnToMainMenu called in GameOverController");
        System.out.println("gameController reference exists: " + (gameController != null));
        
        boolean success = false;
        
        // First try using the gameController reference
        if (gameController != null) {
            try {
                System.out.println("Calling gameController.returnToMainMenu()");
                gameController.returnToMainMenu();
                success = true;
            } catch (Exception e) {
                System.err.println("Error using gameController to return to main menu: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // If the first approach failed, try the direct method
        if (!success) {
            System.out.println("Using direct method to return to main menu");
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
                
                // Get the current stage
                Stage stage = null;
                
                if (mainMenuButton != null && mainMenuButton.getScene() != null && 
                    mainMenuButton.getScene().getWindow() != null) {
                    stage = (Stage) mainMenuButton.getScene().getWindow();
                    System.out.println("Got stage from mainMenuButton");
                } else {
                    // Try to find any active stage
                    for (javafx.stage.Window window : javafx.stage.Window.getWindows()) {
                        if (window instanceof Stage && window.isShowing()) {
                            stage = (Stage) window;
                            System.out.println("Found active stage: " + stage.getTitle());
                            break;
                        }
                    }
                }
                
                if (stage == null) {
                    System.err.println("Could not find an active stage to show main menu!");
                    return;
                }
                
                // Configure stage properties to match Main.java
                stage.setResizable(true);
                stage.setMinWidth(800);
                stage.setMinHeight(600);
                stage.centerOnScreen();
                
                stage.setScene(menuScene);
                stage.show();
                System.out.println("Main menu displayed via direct method");
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