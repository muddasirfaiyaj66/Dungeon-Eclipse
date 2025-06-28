package com.dungeon.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.io.FileNotFoundException;
import java.util.*;

public class Puzzle {
    public enum PuzzleType {
        SEQUENCE, RIDDLE, PATTERN, MATH, WORD, LOGIC
    }

    public enum Difficulty {
        EASY, MEDIUM, HARD, EXTREME
    }

    private PuzzleType type;
    private Difficulty difficulty;
    private String description;
    private String question;
    private String answer;
    private String hint;
    private boolean solved;

    // Default constructor needed for Jackson
    public Puzzle() {}

    // Constructor
    public Puzzle(PuzzleType type, String description, String question, String answer) {
        this.type = type;
        this.description = description;
        this.question = question;
        this.answer = answer;
        this.solved = false;
    }

    // Getters and setters (Jackson needs setters or public fields)
    public PuzzleType getType() { return type; }
    public void setType(PuzzleType type) { this.type = type; }

    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public String getHint() { return hint; }
    public void setHint(String hint) { this.hint = hint; }

    public boolean isSolved() { return solved; }
    public void setSolved(boolean solved) { this.solved = solved; }

    // Checks user answer ignoring case and trimming
    public boolean checkAnswer(String userAnswer) {
        if (userAnswer == null) return false;
        boolean correct = userAnswer.trim().equalsIgnoreCase(answer.trim());
        if (correct) solved = true;
        return correct;
    }

    // Load puzzles from puzzles.json in resources folder
    public static List<Puzzle> loadPuzzlesFromResources() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = Puzzle.class.getClassLoader().getResourceAsStream("puzzles.json");
            if (is == null) throw new FileNotFoundException("puzzles.json not found in resources!");
            return mapper.readValue(is, new TypeReference<List<Puzzle>>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // Returns a random puzzle from JSON loaded puzzles, fallback if empty
    public static Puzzle createRandomPuzzle() {
        List<Puzzle> puzzles = loadPuzzlesFromResources();
        if (puzzles.isEmpty()) {
            return new Puzzle(PuzzleType.RIDDLE, "Solve this riddle.", "What has hands but can’t clap?", "clock");
        }
        Collections.shuffle(puzzles);
        return puzzles.get(0);
    }
}
