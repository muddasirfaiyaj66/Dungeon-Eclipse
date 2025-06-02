package com.dungeon.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Modality;
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
            
            // Create a scene that respects existing stage dimensions (fullscreen)
            Scene gameScene = new Scene(gameRoot);
            
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
            
            // Re-assert full-screen mode after setting the new scene
            stage.setFullScreen(true);
            
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

            // Get the HighScoresController and pass a reference to this MainMenuController
            HighScoresController highScoresController = loader.getController();
            highScoresController.setMainMenuController(this); // Optional: if HighScoresController needs to call back

            Scene highScoresScene = new Scene(highScoresRoot);
            
            // Get current stage from the button that triggered the event
            Stage stage = (Stage) highScoreButton.getScene().getWindow(); 
            stage.setScene(highScoresScene);
            stage.setTitle("High Scores");
            stage.show();
        } catch (Exception e) {
            System.err.println("Error loading HighScores.fxml: " + e.getMessage());
            e.printStackTrace();
            // Fallback alert if loading fails
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Could Not Load High Scores");
            alert.setContentText("There was an error trying to display the high scores. Please try again later.");
            alert.showAndWait();
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void openOptions() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/dungeon/fxml/Options.fxml"));
            Parent optionsRoot = loader.load();

            Scene optionsScene = new Scene(optionsRoot);
            
            // Apply stylesheet if you have one
            try {
                String cssPath = "/com/dungeon/styles/main.css"; 
                String css = getClass().getResource(cssPath).toExternalForm();
                if (css != null) {
                    optionsScene.getStylesheets().add(css);
                } else {
                    System.out.println("Options stylesheet not found at: " + cssPath);
                }
            } catch (NullPointerException e) {
                System.out.println("Options stylesheet not found or error constructing path: " + e.getMessage());
            }

            // Create a new stage for the options window
            Stage optionsStage = new Stage();
            optionsStage.setTitle("Options");
            optionsStage.initModality(Modality.APPLICATION_MODAL); // Makes it block the main menu
            
            // Set the owner of the new stage to the main menu's stage
            Stage ownerStage = (Stage) optionsButton.getScene().getWindow();
            optionsStage.initOwner(ownerStage);
            
            optionsStage.setScene(optionsScene);
            optionsStage.setResizable(false); // Options dialogs are often not resizable
            
            System.out.println("Showing options window (modal from MainMenu).");
            optionsStage.showAndWait(); // Show and wait for it to be closed
            System.out.println("Options window closed, returning to Main Menu.");

        } catch (Exception e) {
            System.err.println("Error loading Options.fxml from MainMenu: " + e.getMessage());
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Could Not Load Options");
            alert.setContentText("There was an error trying to display the options menu.");
            alert.showAndWait();
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void exitGame() {
        Platform.exit();
    }
}
