package com.dungeon.controllers;

import com.dungeon.model.DungeonRoom;
import com.dungeon.model.Puzzle;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class PuzzleController {
    @FXML
    private Label puzzleDescription;
    
    @FXML
    private Label puzzleContent;
    
    @FXML
    private TextField answerField;
    
    @FXML
    private Button submitButton;
    
    @FXML
    private VBox sequenceContainer;
    
    @FXML
    private VBox patternContainer;
    
    @FXML
    private VBox riddleContainer;
    
    private Puzzle puzzle;
    private DungeonRoom room;
    private GameController gameController;
    
    public void initialize(Puzzle puzzle, DungeonRoom room, GameController gameController) {
        this.puzzle = puzzle;
        this.room = room;
        this.gameController = gameController;
        
        // Set up puzzle UI based on puzzle type
        puzzleDescription.setText(puzzle.getDescription());
        
        // Hide all containers first
        sequenceContainer.setVisible(false);
        patternContainer.setVisible(false);
        riddleContainer.setVisible(false);
        
        // Show appropriate container based on puzzle type
        switch (puzzle.getType()) {
            case SEQUENCE:
                setupSequencePuzzle();
                break;
            case RIDDLE:
                setupRiddlePuzzle();
                break;
            case PATTERN:
                setupPatternPuzzle();
                break;
        }
        
        // Set up submit button action
        submitButton.setOnAction(event -> checkAnswer());
    }
    
    private void setupSequencePuzzle() {
        sequenceContainer.setVisible(true);
        puzzleContent.setText(puzzle.getSequence());
    }
    
    private void setupRiddlePuzzle() {
        riddleContainer.setVisible(true);
        puzzleContent.setText(puzzle.getRiddle());
    }
    
    private void setupPatternPuzzle() {
        patternContainer.setVisible(true);
        puzzleContent.setText(puzzle.getPattern());
    }
    
    private void checkAnswer() {
        String userAnswer = answerField.getText().trim();
        
        if (userAnswer.equalsIgnoreCase(puzzle.getAnswer())) {
            // Correct answer
            puzzle.setSolved(true);
            room.setLocked(false);
            
            // Close puzzle window
            Stage stage = (Stage) submitButton.getScene().getWindow();
            stage.close();
            
            // Update game state
            gameController.onPuzzleSolved(room);
        } else {
            // Wrong answer
            answerField.setStyle("-fx-border-color: red;");
            answerField.setText("");
            answerField.setPromptText("Try again");
        }
    }
}
