package com.dungeon.controllers;

import com.dungeon.model.DungeonRoom;
import com.dungeon.model.Puzzle;
import com.dungeon.audio.SoundManager;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.input.KeyCode;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javafx.stage.Stage;
import javafx.scene.Scene;

public class PuzzleController {
    @FXML
    private VBox rootContainer;
    
    @FXML
    private Label puzzleDescription;
    
    @FXML
    private TextField answerField;
    
    @FXML
    private Button submitButton;
    
    private Puzzle puzzle;
    private DungeonRoom room;
    private GameController gameController;
    private SoundManager soundManager;
    
    public void initialize() {
        // Initialize sound manager
        soundManager = SoundManager.getInstance();
        
        // Set up submit button action
        submitButton.setOnAction(e -> checkAnswer());
        
        // Add keyboard shortcut for submitting
        answerField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                checkAnswer();
            }
        });
    }
    
    public void initialize(Puzzle puzzle, DungeonRoom room, GameController gameController) {
        this.puzzle = puzzle;
        this.room = room;
        this.gameController = gameController;
        
        // Update UI with puzzle information
        puzzleDescription.setText(puzzle.getDescription());
        answerField.setPromptText("Enter your answer");
    }
    
    private void checkAnswer() {
        if (puzzle == null) return;

        String userAnswer = answerField.getText().trim().toUpperCase();
        String correctAnswer = puzzle.getAnswer().toUpperCase();

        boolean isCorrect = switch (puzzle.getType()) {
            case MATH -> {
                try {
                    int userNum = Integer.parseInt(userAnswer);
                    int correctNum = Integer.parseInt(correctAnswer);
                    yield userNum == correctNum;
                } catch (NumberFormatException e) {
                    yield false;
                }
            }
            case LOGIC -> {
                if (correctAnswer.equals("TRUE") || correctAnswer.equals("FALSE")) {
                    yield userAnswer.equals(correctAnswer);
                }
                try {
                    int userNum = Integer.parseInt(userAnswer);
                    int correctNum = Integer.parseInt(correctAnswer);
                    yield userNum == correctNum;
                } catch (NumberFormatException e) {
                    yield false;
                }
            }
            default -> userAnswer.equals(correctAnswer);
        };

        if (isCorrect) {
            soundManager.playSound("start");
            showSuccessMessage();
            room.setLocked(false);
            gameController.onPuzzleSolved(room);
            closePuzzleWindow();
        } else {
            soundManager.playSound("character");
            showErrorMessage();
        }
    }

    private void showSuccessMessage() {
        Label successLabel = new Label("Correct! The door is now unlocked.");
        successLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #4CAF50; -fx-font-weight: bold;");
        rootContainer.getChildren().add(successLabel);
        
        // Remove the success message after 2 seconds
        PauseTransition delay = new PauseTransition(Duration.seconds(2));
        delay.setOnFinished(e -> rootContainer.getChildren().remove(successLabel));
        delay.play();
    }

    private void showErrorMessage() {
        Label errorLabel = new Label("Incorrect. Try again!");
        errorLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #f44336; -fx-font-weight: bold;");
        rootContainer.getChildren().add(errorLabel);
        
        // Remove the error message after 2 seconds
        PauseTransition delay = new PauseTransition(Duration.seconds(2));
        delay.setOnFinished(e -> rootContainer.getChildren().remove(errorLabel));
        delay.play();
        }

    private void closePuzzleWindow() {
        Stage stage = (Stage) rootContainer.getScene().getWindow();
        stage.close();
    }
}
