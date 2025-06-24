package com.dungeon.effects.messaging;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class ChatController {

    @FXML private TextField messageInput;
    @FXML private ListView<String> chatHistory;

    private final GeminiService gemini = new GeminiService();

    @FXML
    private void sendMessage() {
        String userMessage = messageInput.getText().trim();
        if (userMessage.isEmpty()) return;

        chatHistory.getItems().add("👤 You: " + userMessage);
        messageInput.clear();

        new Thread(() -> {
            try {
                String prompt = PromptBuilder.buildHintPrompt(userMessage);
                String hint = gemini.ask(prompt);
                Platform.runLater(() -> chatHistory.getItems().add("🤖 Game Bot: " + hint));
            } catch (Exception e) {
                Platform.runLater(() -> chatHistory.getItems().add("❌ Error fetching hint."));
                e.printStackTrace();
            }
        }).start();
    }
}
