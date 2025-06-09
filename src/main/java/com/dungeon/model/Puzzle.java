package com.dungeon.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;

/**
 * Represents a puzzle that can be solved by the player
 */
public class Puzzle {
    // Types of puzzles
    public enum PuzzleType {
        SEQUENCE,  // Complete the sequence
        RIDDLE,    // Answer a riddle
        PATTERN,   // Identify a pattern
        MATH,      // Solve a math problem
        WORD,      // Word puzzle
        LOGIC      // Logic puzzle
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
     * Creates a random puzzle of any type
     * @return A new random puzzle
     */
    public static Puzzle createRandomPuzzle() {
        Random random = new Random();
        int puzzleType = random.nextInt(6);
        
        switch (puzzleType) {
            case 0:
                return createSequencePuzzle();
            case 1:
                return createRiddlePuzzle();   
            case 2:
                return createMathPuzzle();
            case 3:
                return createWordPuzzle();
            default:
                return createLogicPuzzle();
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
    
    public static Puzzle createMathPuzzle() {
        Random random = new Random();
        int puzzleType = random.nextInt(3);
        String question;
        String answer;
        String description = "Solve the mathematical problem.";

        switch (puzzleType) {
            case 0: // Basic arithmetic
                int num1 = random.nextInt(20) + 1;
                int num2 = random.nextInt(20) + 1;
                int operation = random.nextInt(4);
                switch (operation) {
                    case 0: // Addition
                        question = num1 + " + " + num2 + " = ?";
                        answer = String.valueOf(num1 + num2);
                        break;
                    case 1: // Subtraction
                        question = num1 + " - " + num2 + " = ?";
                        answer = String.valueOf(num1 - num2);
                        break;
                    case 2: // Multiplication
                        question = num1 + " × " + num2 + " = ?";
                        answer = String.valueOf(num1 * num2);
                        break;
                    default: // Division
                        num2 = random.nextInt(10) + 1; // Avoid division by zero
                        question = num1 + " ÷ " + num2 + " = ?";
                        answer = String.valueOf(num1 / num2);
                        break;
                }
                break;

            case 1: // Word problem
                int apples = random.nextInt(10) + 1;
                int oranges = random.nextInt(10) + 1;
                question = "If you have " + apples + " apples and " + oranges + 
                          " oranges, how many fruits do you have in total?";
                answer = String.valueOf(apples + oranges);
                break;

            default: // Pattern with operations
                int start = random.nextInt(5) + 1;
                int[] operations = new int[3];
                for (int i = 0; i < operations.length; i++) {
                    operations[i] = random.nextInt(5) + 1;
                }
                question = "If " + start + " × " + operations[0] + " + " + 
                          operations[1] + " - " + operations[2] + " = ?";
                answer = String.valueOf(start * operations[0] + operations[1] - operations[2]);
                break;
        }

        return new Puzzle(PuzzleType.MATH, description, question, answer);
    }

    public static Puzzle createWordPuzzle() {
        Random random = new Random();
        int puzzleType = random.nextInt(3);
        String question;
        String answer;
        String description = "Solve the word puzzle.";

        switch (puzzleType) {
            case 0: // Anagram
                String[] words = {
                    "HEART", "EARTH",
                    "LISTEN", "SILENT",
                    "STATE", "TASTE",
                    "NIGHT", "THING",
                    "STARE", "TEARS"
                };
                int index = random.nextInt(words.length / 2) * 2;
                question = "Unscramble this word: " + scrambleWord(words[index]);
                answer = words[index];
                break;

            case 1: // Missing vowels
                String[] wordsNoVowels = {
                    "APPLE", "PPL",
                    "ELEPHANT", "LPHNT",
                    "ORANGE", "RNG",
                    "UMBRELLA", "MBRLL",
                    "ISLAND", "SLND"
                };
                index = random.nextInt(wordsNoVowels.length / 2) * 2;
                question = "Add the missing vowels to: " + wordsNoVowels[index + 1];
                answer = wordsNoVowels[index];
                break;

            default: // Word chain
                String[] wordChains = {
                    "COLD -> WARM -> HOT", "COLD", "WARM", "HOT",
                    "SMILE -> LAUGH -> CRY", "SMILE", "LAUGH", "CRY",
                    "START -> BEGIN -> END", "START", "BEGIN", "END",
                    "UP -> DOWN -> SIDE", "UP", "DOWN", "SIDE",
                    "DAY -> NIGHT -> MORNING", "DAY", "NIGHT", "MORNING"
                };
                index = random.nextInt(wordChains.length / 4) * 4;
                int position = random.nextInt(3); // 0 for first, 1 for middle, 2 for last
                String positionText = position == 0 ? "first" : (position == 1 ? "middle" : "last");
                question = "What's the " + positionText + " word in this chain: " + wordChains[index];
                answer = wordChains[index + 1 + position];
                break;
        }

        return new Puzzle(PuzzleType.WORD, description, question, answer);
    }

    public static Puzzle createLogicPuzzle() {
        Random random = new Random();
        int puzzleType = random.nextInt(3);
        String question;
        String answer;
        String description = "Solve the logic puzzle.";

        switch (puzzleType) {
            case 0: // Color sequence
                String[] colors = {"RED", "BLUE", "GREEN", "YELLOW", "PURPLE"};
                // Create a repeating pattern of 3 colors
                int patternLength = 3;
                int[] sequence = new int[4];
                int startColor = random.nextInt(colors.length);
                
                // Generate a sequence that repeats every 3 colors
                for (int i = 0; i < sequence.length; i++) {
                    sequence[i] = (startColor + i) % patternLength;
                }
                
                question = "What color comes next in this sequence: " +
                          colors[sequence[0]] + ", " + colors[sequence[1]] + ", " +
                          colors[sequence[2]] + ", " + colors[sequence[3]] + ", ?";
                // The answer is the next color in the repeating pattern
                answer = colors[(startColor + 4) % patternLength];
                break;

            case 1: // True/False logic
                String[] logicQuestions = {
                    "If all A are B, and all B are C, then all A are C. (True/False)", "TRUE",
                    "If it's raining, the ground is wet. The ground is wet, so it's raining. (True/False)", "FALSE",
                    "All birds can fly. Penguins are birds, so penguins can fly. (True/False)", "FALSE",
                    "If you study, you'll pass. You passed, so you studied. (True/False)", "FALSE",
                    "All squares are rectangles. All rectangles are quadrilaterals. So all squares are quadrilaterals. (True/False)", "TRUE"
                };
                int index = random.nextInt(logicQuestions.length / 2) * 2;
                question = logicQuestions[index];
                answer = logicQuestions[index + 1];
                break;

            default: // Pattern completion
                String[] patterns = {
                    "2, 4, 8, 16, ?", "32",
                    "1, 3, 6, 10, ?", "15",
                    "3, 6, 9, 12, ?", "15",
                    "1, 2, 4, 7, ?", "11",
                    "2, 3, 5, 7, ?", "11"
                };
                index = random.nextInt(patterns.length / 2) * 2;
                question = patterns[index];
                answer = patterns[index + 1];
                break;
        }

        return new Puzzle(PuzzleType.LOGIC, description, question, answer);
    }

    private static String scrambleWord(String word) {
        List<Character> chars = new ArrayList<>();
        for (char c : word.toCharArray()) {
            chars.add(c);
        }
        Collections.shuffle(chars);
        StringBuilder scrambled = new StringBuilder();
        for (char c : chars) {
            scrambled.append(c);
        }
        return scrambled.toString();
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
