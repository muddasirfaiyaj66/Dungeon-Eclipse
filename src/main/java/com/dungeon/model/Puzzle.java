package com.dungeon.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Represents a puzzle that can be solved by the player
 */
public class Puzzle {
    // Types of puzzles
    public enum PuzzleType {
        SEQUENCE,  // Complete the sequence
        RIDDLE,    // Answer a riddle
        PATTERN    // Identify a pattern
    }
    
    private final PuzzleType type;
    private final String description;
    private final String question;
    private final String answer;
    private boolean solved;
    
    // Type-specific puzzle content
    private String sequence;  // For sequence puzzles
    private String riddle;    // For riddle puzzles
    private String pattern;   // For pattern puzzles
    
    /**
     * Creates a new puzzle
     * @param type The type of puzzle
     * @param description Description of the puzzle
     * @param question The puzzle question or prompt
     * @param answer The correct answer
     */
    public Puzzle(PuzzleType type, String description, String question, String answer) {
        this.type = type;
        this.description = description;
        this.question = question;
        this.answer = answer;
        this.solved = false;
        
        // Set type-specific content
        switch (type) {
            case SEQUENCE:
                this.sequence = question;
                break;
            case RIDDLE:
                this.riddle = question;
                break;
            case PATTERN:
                this.pattern = question;
                break;
        }
    }
    
    /**
     * Checks if the provided answer is correct
     * @param userAnswer The answer provided by the player
     * @return true if the answer is correct
     */
    public boolean checkAnswer(String userAnswer) {
        if (userAnswer == null) return false;
        
        boolean correct = userAnswer.trim().equalsIgnoreCase(answer.trim());
        if (correct) {
            solved = true;
        }
        
        return correct;
    }
    
    /**
     * Creates a random sequence puzzle
     * @return A new sequence puzzle
     */
    public static Puzzle createSequencePuzzle() {
        Random random = new Random();
        int puzzleType = random.nextInt(4);
        
        String sequence;
        String answer;
        String description = "Complete the sequence by providing the next number.";
        
        switch (puzzleType) {
            case 0: // Arithmetic sequence
                int start = random.nextInt(5) + 1;
                int increment = random.nextInt(5) + 1;
                sequence = createArithmeticSequence(start, increment, 5);
                answer = String.valueOf(start + increment * 5);
                break;
                
            case 1: // Geometric sequence
                start = random.nextInt(3) + 1;
                int multiplier = random.nextInt(2) + 2;
                sequence = createGeometricSequence(start, multiplier, 5);
                answer = String.valueOf(start * (int)Math.pow(multiplier, 5));
                break;
                
            case 2: // Fibonacci-like
                start = random.nextInt(3) + 1;
                int second = random.nextInt(5) + 2;
                sequence = createFibonacciLikeSequence(start, second, 5);
                
                // Calculate the answer (last two numbers added)
                String[] numbers = sequence.split(",");
                int secondLast = Integer.parseInt(numbers[numbers.length - 2].trim());
                int last = Integer.parseInt(numbers[numbers.length - 1].trim());
                answer = String.valueOf(secondLast + last);
                break;
                
            default: // Alternating sequence
                int[] values = new int[] {
                    random.nextInt(5) + 1,
                    random.nextInt(5) + 6
                };
                sequence = createAlternatingSequence(values, 5);
                answer = String.valueOf(values[5 % 2]);
                break;
        }
        
        return new Puzzle(PuzzleType.SEQUENCE, description, sequence, answer);
    }
    
    /**
     * Creates a random riddle puzzle
     * @return A new riddle puzzle
     */
    public static Puzzle createRiddlePuzzle() {
        List<String[]> riddles = Arrays.asList(
            new String[] {"What has a head, a tail, but no body?", "coin"},
            new String[] {"What has an eye but cannot see?", "needle"},
            new String[] {"What gets wet while drying?", "towel"},
            new String[] {"What can you catch but not throw?", "cold"},
            new String[] {"What has many keys but can't open a single lock?", "piano"},
            new String[] {"What is so fragile that saying its name breaks it?", "silence"},
            new String[] {"What belongs to you but others use it more than you do?", "name"},
            new String[] {"What has a neck but no head?", "bottle"},
            new String[] {"What has cities but no houses, forests but no trees, and rivers but no water?", "map"},
            new String[] {"What can run but never walks, has a mouth but never talks?", "river"}
        );
        
        Collections.shuffle(riddles);
        String[] selectedRiddle = riddles.get(0);
        
        String description = "Solve the riddle by providing the correct answer.";
        return new Puzzle(PuzzleType.RIDDLE, description, selectedRiddle[0], selectedRiddle[1]);
    }
    
    /**
     * Creates a random pattern puzzle
     * @return A new pattern puzzle
     */
    public static Puzzle createPatternPuzzle() {
        Random random = new Random();
        int patternType = random.nextInt(3);
        
        String pattern;
        String answer;
        String description = "Identify the pattern and provide the missing value.";
        
        switch (patternType) {
            case 0: // Letter pattern
                String[] letterPatterns = {
                    "A, C, E, ?, I", "G",
                    "Z, Y, X, ?, V", "W",
                    "A, B, D, G, ?", "K",
                    "O, T, T, F, F, S, S, ?", "E",
                    "B, C, D, F, G, H, ?", "J"
                };
                int index = random.nextInt(letterPatterns.length / 2) * 2;
                pattern = letterPatterns[index];
                answer = letterPatterns[index + 1];
                break;
                
            case 1: // Word pattern
                String[] wordPatterns = {
                    "CAT, DOG, BIRD, ?", "FISH",
                    "ONE, TWO, THREE, ?", "FOUR",
                    "APPLE, ORANGE, BANANA, ?", "GRAPE",
                    "NORTH, EAST, SOUTH, ?", "WEST",
                    "RED, BLUE, YELLOW, ?", "GREEN"
                };
                index = random.nextInt(wordPatterns.length / 2) * 2;
                pattern = wordPatterns[index];
                answer = wordPatterns[index + 1];
                break;
                
            default: // Number pattern with operations
                int start = random.nextInt(5) + 1;
                int[] operations = new int[5];
                for (int i = 0; i < operations.length; i++) {
                    operations[i] = random.nextInt(5) + 1;
                }
                
                StringBuilder patternBuilder = new StringBuilder();
                int current = start;
                for (int i = 0; i < 4; i++) {
                    patternBuilder.append(current);
                    if (i < 3) {
                        patternBuilder.append(", ");
                    } else {
                        patternBuilder.append(", ?");
                    }
                    
                    // Apply operation
                    if (i % 2 == 0) {
                        current += operations[i];
                    } else {
                        current *= operations[i];
                    }
                }
                
                pattern = patternBuilder.toString();
                answer = String.valueOf(current);
                break;
        }
        
        return new Puzzle(PuzzleType.PATTERN, description, pattern, answer);
    }
    
    /**
     * Creates a random puzzle of any type
     * @return A new random puzzle
     */
    public static Puzzle createRandomPuzzle() {
        Random random = new Random();
        int puzzleType = random.nextInt(3);
        
        switch (puzzleType) {
            case 0:
                return createSequencePuzzle();
            case 1:
                return createRiddlePuzzle();
            default:
                return createPatternPuzzle();
        }
    }
    
    // Helper methods for creating sequences
    
    private static String createArithmeticSequence(int start, int increment, int count) {
        StringBuilder sequence = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sequence.append(start + i * increment);
            if (i < count - 1) {
                sequence.append(", ");
            }
        }
        return sequence.toString();
    }
    
    private static String createGeometricSequence(int start, int multiplier, int count) {
        StringBuilder sequence = new StringBuilder();
        int current = start;
        for (int i = 0; i < count; i++) {
            sequence.append(current);
            if (i < count - 1) {
                sequence.append(", ");
            }
            current *= multiplier;
        }
        return sequence.toString();
    }
    
    private static String createFibonacciLikeSequence(int first, int second, int count) {
        StringBuilder sequence = new StringBuilder();
        sequence.append(first).append(", ").append(second);
        
        int a = first;
        int b = second;
        
        for (int i = 2; i < count; i++) {
            int next = a + b;
            sequence.append(", ").append(next);
            a = b;
            b = next;
        }
        
        return sequence.toString();
    }
    
    private static String createAlternatingSequence(int[] values, int count) {
        StringBuilder sequence = new StringBuilder();
        
        for (int i = 0; i < count; i++) {
            sequence.append(values[i % values.length]);
            if (i < count - 1) {
                sequence.append(", ");
            }
        }
        
        return sequence.toString();
    }
    
    // Getters and setters
    
    public PuzzleType getType() {
        return type;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getQuestion() {
        return question;
    }
    
    public String getAnswer() {
        return answer;
    }
    
    public boolean isSolved() {
        return solved;
    }
    
    public void setSolved(boolean solved) {
        this.solved = solved;
    }
    
    public String getSequence() {
        return sequence;
    }
    
    public String getRiddle() {
        return riddle;
    }
    
    public String getPattern() {
        return pattern;
    }
}
