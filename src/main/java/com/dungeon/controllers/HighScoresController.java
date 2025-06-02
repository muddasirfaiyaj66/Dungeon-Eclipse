package com.dungeon.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

import com.dungeon.data.ScoreManager; 

public class HighScoresController {

    @FXML
    private ListView<String> highScoresListView;

    @FXML
    private Button backButton;

    private MainMenuController mainMenuController; // To return to main menu

    public void setMainMenuController(MainMenuController mainMenuController) {
        this.mainMenuController = mainMenuController;
    }

    @FXML
    public void initialize() {
        loadHighScores();
    }

    private void loadHighScores() {
        List<String> scores = ScoreManager.loadScores(); // Load scores as strings
        if (scores.isEmpty()) {
            highScoresListView.getItems().add("No high scores yet!");
        } else {
            highScoresListView.getItems().addAll(scores);
        }
    }

    @FXML
    private void handleBackButtonAction() {
        try {
            System.out.println("Loading main menu from HighScoresController...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/dungeon/fxml/MainMenu.fxml"));
            Parent menuRoot = loader.load();
            // Use standard scene size consistent with GameController's loadMainMenu
            Scene menuScene = new Scene(menuRoot, 1024, 768);

            // Apply stylesheet, similar to GameController's loadMainMenu
            try {
                String cssPath = "/com/dungeon/styles/main.css";
                String css = getClass().getResource(cssPath).toExternalForm();
                menuScene.getStylesheets().add(css);
                System.out.println("Applied stylesheet to main menu scene: " + cssPath);
            } catch (Exception e) {
                System.out.println("Stylesheet not found or error applying: /com/dungeon/styles/main.css - " + e.getMessage());
            }

            Stage stage = (Stage) backButton.getScene().getWindow();
            if (stage == null) {
                System.err.println("Could not find an active stage to show main menu!");
                return;
            }

            stage.setScene(menuScene);
            stage.setTitle("Dungeon Eclipse - Main Menu"); 

            // Configure stage properties to match GameController's loadMainMenu / Main.java
            stage.setResizable(true);
            stage.setMinWidth(800);
            stage.setMinHeight(600);
            stage.centerOnScreen();

            stage.show();
            System.out.println("Main menu displayed successfully from HighScoresController.");

        } catch (IOException e) {
            System.err.println("Error loading MainMenu.fxml from HighScoresController: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 