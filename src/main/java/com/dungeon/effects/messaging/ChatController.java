package com.dungeon.effects.messaging;

import java.util.List;

import com.dungeon.model.Puzzle;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class ChatController {

    @FXML private TextField messageInput;
    @FXML private ListView<String> chatHistory;

    private final GeminiService gemini = new GeminiService();
    private Puzzle currentPuzzle = null;
    private List<Puzzle> allPuzzles;

    @FXML
    public void initialize() {
        // Load puzzles from resources/puzzles.json
        allPuzzles = Puzzle.loadPuzzlesFromResources();

        chatHistory.setCellFactory(lv -> {
            ListCell<String> cell = new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(item);
                        setWrapText(true);
                        setStyle("-fx-text-fill: black; -fx-font-family: Consolas; -fx-font-size: 14px;");
                    }
                }
            };
            cell.prefWidthProperty().bind(lv.widthProperty().subtract(20));
            return cell;
        });
    }

    @FXML
    private void sendMessage() {
        String userMessage = messageInput.getText().trim();
        if (userMessage.isEmpty()) return;

        chatHistory.getItems().add("👤 You: " + userMessage);
        chatHistory.scrollTo(chatHistory.getItems().size() - 1);
        messageInput.clear();

        new Thread(() -> {
            try {
                String botReply;

                if (currentPuzzle != null) {
                    String correctAnswer = currentPuzzle.getAnswer().trim().toLowerCase();
                    String userAnswer = userMessage.trim().toLowerCase();

                    if (userAnswer.equals(correctAnswer)) {
                        botReply = "🎉 Congratulations, hero! You’ve solved the puzzle!";
                        currentPuzzle = null;
                    } else {
                        String prompt = PromptBuilder.buildHintPrompt(currentPuzzle, userMessage);
                        botReply = gemini.ask(prompt);
                    }
                } else {
                    Puzzle matchedPuzzle = findMatchingPuzzle(userMessage);

                    if (matchedPuzzle != null) {
                        currentPuzzle = matchedPuzzle;
                        String prompt = PromptBuilder.buildHintPrompt(matchedPuzzle, null);
                        botReply = gemini.ask(prompt);
                    } else {
                        String prompt = PromptBuilder.buildConversationResponse(userMessage);
                        botReply = gemini.ask(prompt);
                    }
                }

                String finalBotReply = botReply;
                Platform.runLater(() -> {
                    chatHistory.getItems().add("🤖 Game Bot: " + finalBotReply);
                    chatHistory.scrollTo(chatHistory.getItems().size() - 1);
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    chatHistory.getItems().add("❌ Error: Unable to generate hint or response.");
                    chatHistory.scrollTo(chatHistory.getItems().size() - 1);
                });
            }
        }).start();
    }

    @FXML
    private void clearChat() {
        chatHistory.getItems().clear();
        currentPuzzle = null;
    }

    private Puzzle findMatchingPuzzle(String input) {
        for (Puzzle puzzle : allPuzzles) {
            if (input.equalsIgnoreCase(puzzle.getQuestion())) {
                return puzzle;
            }
        }
        return null;
    }
}
