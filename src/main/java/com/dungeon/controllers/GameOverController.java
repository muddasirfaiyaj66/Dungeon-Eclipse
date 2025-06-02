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

        // Prompt for player name and save score
        // Run on JavaFX application thread to ensure UI operations are safe
        Platform.runLater(() -> {
            TextInputDialog dialog = new TextInputDialog("Player");
            dialog.setTitle("Game Over");
            dialog.setHeaderText("You scored: " + score + "! Enter your name for the high scores:");
            dialog.setContentText("Name:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(name -> {
                if (!name.trim().isEmpty()) {
                    ScoreManager.saveScore(name, this.score);
                } else {
                    ScoreManager.saveScore("Anonymous", this.score);
                }
            });
        });
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
            // Create scene without fixed dimensions for fullscreen
            Scene gameScene = new Scene(gameRoot);
            
            Stage stage = (Stage) tryAgainButton.getScene().getWindow();
            
            // Configure stage properties
            stage.setResizable(true);
            stage.setMinWidth(800);
            stage.setMinHeight(600);
            
            stage.setScene(gameScene);
            // Re-assert full-screen mode after setting the new scene
            stage.setFullScreen(true);
            
            // Make sure the stage is showing
            if (!stage.isShowing()) {
                stage.show();
            }
            
            GameController newGameController = loader.getController(); // Renamed to avoid confusion
            Platform.runLater(() -> {
                System.out.println("Initializing game controller from game over screen...");
                newGameController.onSceneReady();
                
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
        System.out.println("gameController reference exists: " + (this.gameController != null));
        
        boolean success = false;
        
        // First try using the gameController reference
        if (this.gameController != null) {
            try {
                System.out.println("Calling gameController.returnToMainMenu()");
                this.gameController.returnToMainMenu();
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