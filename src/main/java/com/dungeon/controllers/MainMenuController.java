package com.dungeon.controllers;

import com.dungeon.utils.UIUtils;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

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
    private Button chatButton;
    
    @FXML
    private StackPane mainMenuRoot;
    
    @FXML
    @SuppressWarnings("unused")
    private void startNewGame() {
        try {
            System.out.println("Starting new game...");
            
            // Get the current scene and its root pane
            Scene currentScene = newGameButton.getScene();
            Parent rootPane = currentScene.getRoot();

            // 1. Fade out the main menu
            FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), rootPane);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(event -> {
                try {
                    // 2. Load the game scene content
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/dungeon/fxml/GameScene.fxml"));
                    Parent gameRoot = loader.load();
                    gameRoot.setStyle("-fx-background-color: rgb(70, 86, 105);"); // Prevent flash
                    gameRoot.setOpacity(0.0); // Start transparent for fade-in

                    // 3. Replace the scene's root with the new game content
                    currentScene.setRoot(gameRoot);
                    
                    // 4. Fade in the new game scene
                    FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.5), gameRoot);
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.setOnFinished(e -> {
                        // 5. Initialize the controller and focus after the fade-in is complete
                        Platform.runLater(() -> {
                            System.out.println("Initializing game controller...");
                            GameController gameController = loader.getController();
                            gameController.onSceneReady();
                            gameRoot.requestFocus();
                            System.out.println("Game initialized successfully");
                        });
                    });
                    fadeIn.play();

                } catch (Exception e) {
                    System.err.println("Error during scene transition: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            fadeOut.play();

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
            tutorialStage.initModality(Modality.APPLICATION_MODAL);
            tutorialStage.initOwner(tutorialButton.getScene().getWindow());

            tutorialStage.setScene(tutorialScene);
            UIUtils.setStageIcon(tutorialStage);
            tutorialStage.showAndWait();
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

            HighScoresController highScoresController = loader.getController();
            highScoresController.setMainMenuController(this); 

            Scene highScoresScene = new Scene(highScoresRoot);
            
            Stage highScoresStage = new Stage();
            highScoresStage.setTitle("High Scores");
            highScoresStage.initModality(Modality.APPLICATION_MODAL);
            highScoresStage.initOwner(highScoreButton.getScene().getWindow());
            highScoresStage.setScene(highScoresScene);
            UIUtils.setStageIcon(highScoresStage);
            highScoresStage.showAndWait();
            
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
            
            UIUtils.setStageIcon(optionsStage);
            
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
    private void openChat() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/dungeon/fxml/ChatMenu.fxml"));
            Parent chatRoot = loader.load();
            Scene chatScene = new Scene(chatRoot);
            Stage chatStage = new Stage();
            chatStage.setTitle("Game Chat");
            chatStage.initModality(Modality.APPLICATION_MODAL);
            chatStage.initOwner(chatButton.getScene().getWindow());
            chatStage.setScene(chatScene);
            chatStage.setResizable(false); // Chat window is typically not resizable
            UIUtils.setStageIcon(chatStage);
            chatStage.showAndWait();
        } catch (Exception e) {
            System.err.println("Error loading Chat.fxml: " + e.getMessage());
            e.printStackTrace();
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Could Not Load Chat");
            alert.setContentText("There was an error trying to display the chat window. Please try again later.");
            alert.showAndWait();
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void exitGame() {
        Platform.exit();
    }

    @FXML
    public void initialize() {
        // Set the background image for the main menu
        Image bgImage = new Image(getClass().getResourceAsStream("/com/dungeon/assets/images/mainmenu.jpg"));
        ImageView bgView = new ImageView(bgImage);
        bgView.setPreserveRatio(false);
        bgView.setFitWidth(mainMenuRoot.getWidth());
        bgView.setFitHeight(mainMenuRoot.getHeight());
        bgView.fitWidthProperty().bind(mainMenuRoot.widthProperty());
        bgView.fitHeightProperty().bind(mainMenuRoot.heightProperty());
        bgView.setSmooth(true);
        bgView.setCache(true);
        // Add the background image as the first child
        mainMenuRoot.getChildren().add(0, bgView);
    }
}
