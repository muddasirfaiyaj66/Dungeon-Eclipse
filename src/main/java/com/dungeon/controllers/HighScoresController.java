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

import com.dungeon.data.ScoreManager; // We will create this next

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
    private void closeWindow() {
        Stage stage = (Stage) backButton.getScene().getWindow();
        stage.close();
    }
} 