package com.dungeon.controllers;

import com.dungeon.data.ScoreManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

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
        
       
        nextLevelButton.setDisable(level >= 3); 

      
        Platform.runLater(() -> {
            TextInputDialog dialog = new TextInputDialog("Player");
            dialog.setTitle("Victory!");
            dialog.setHeaderText("You won with a score of: " + score + "! Enter your name for the high scores:");
            dialog.setContentText("Name:");

            // Get the dialog pane and style it
            DialogPane dialogPane = dialog.getDialogPane();
            dialogPane.getStyleClass().add("custom-dialog");
            dialogPane.setStyle("-fx-background-color: linear-gradient(to bottom, #1a1a1a, #2b2b2b);" +
                              "-fx-border-color: #4a4a4a;" +
                              "-fx-border-width: 2;" +
                              "-fx-border-radius: 10;" +
                              "-fx-background-radius: 10;");

            // Style the header and content labels
            Label headerLabel = (Label) dialogPane.lookup(".header-panel .label");
            if (headerLabel != null) {
                headerLabel.setStyle("-fx-text-fill: white;" +
                                   "-fx-font-size: 16px;" +
                                   "-fx-font-weight: bold;");
            }

            Label contentLabel = (Label) dialogPane.lookup(".content .label");
            if (contentLabel != null) {
                contentLabel.setStyle("-fx-text-fill: white;" +
                                    "-fx-font-size: 14px;");
            }

            // Style the text field
            TextField textField = dialog.getEditor();
            textField.setStyle("-fx-background-color: rgba(60, 63, 65, 0.8);" +
                             "-fx-text-fill: white;" +
                             "-fx-font-size: 14px;" +
                             "-fx-padding: 8;" +
                             "-fx-background-radius: 5;" +
                             "-fx-border-radius: 5;" +
                             "-fx-border-color: #4a4a4a;" +
                             "-fx-border-width: 1;");

            // Style the buttons
            dialogPane.lookupAll(".button").forEach(node -> {
                if (node instanceof Button) {
                    Button button = (Button) node;
                    button.setStyle("-fx-background-color: linear-gradient(to bottom, #4a4a4a, #3a3a3a);" +
                                  "-fx-text-fill: white;" +
                                  "-fx-font-size: 14px;" +
                                  "-fx-padding: 8 20;" +
                                  "-fx-background-radius: 5;" +
                                  "-fx-cursor: hand;");
                }
            });

            // Set the owner of the dialog to the main game window to make it modal
            Stage owner = (Stage) scoreText.getScene().getWindow();
            dialog.initOwner(owner);

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(name -> {
                if (!name.trim().isEmpty()) {
                    ScoreManager.saveScore(name, this.score);
                } else {
                    ScoreManager.saveScore("Champion", this.score); 
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
        if (currentLevel >= 3) return; 

        try {
            Scene currentScene = nextLevelButton.getScene();
            
            Parent currentRoot = currentScene.getRoot();
            FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), currentRoot);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(event -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/dungeon/fxml/GameScene.fxml"));
                    Parent gameRoot = loader.load();
                    gameRoot.setOpacity(0.0);
                    currentScene.setRoot(gameRoot);
                    
                    FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.5), gameRoot);
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.setOnFinished(e -> {
                        GameController newGameController = loader.getController();
                        Platform.runLater(() -> {
                            newGameController.setLevel(this.currentLevel + 1);
                            newGameController.onSceneReady();
                        });
                    });
                    fadeIn.play();

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            fadeOut.play();

        } catch (Exception e) {
            System.err.println("Error starting next level: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    @SuppressWarnings("unused")
    private void returnToMainMenu() {
       try {
            // Get the current scene and its root pane
            Scene currentScene = mainMenuButton.getScene();
            Parent currentRoot = currentScene.getRoot();

            // 1. Fade out the current screen
            FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), currentRoot);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(event -> {
                try {
                    // 2. Load the main menu content
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/dungeon/fxml/MainMenu.fxml"));
                    Parent mainMenuRoot = loader.load();
                    mainMenuRoot.setOpacity(0.0); // Start transparent for fade-in

                    // 3. Replace the scene's root with the new main menu content
                    currentScene.setRoot(mainMenuRoot);
                    
                    // 4. Fade in the new main menu
                    FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.5), mainMenuRoot);
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.play();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            fadeOut.play();
            
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