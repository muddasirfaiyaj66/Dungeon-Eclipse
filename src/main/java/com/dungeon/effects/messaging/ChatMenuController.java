package com.dungeon.effects.messaging;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

public class ChatMenuController {

    @FXML private TextField messageInput;
    @FXML private ListView<String> chatHistory;

    private final GeminiService gemini = new GeminiService();

    @FXML
    public void initialize() {
        chatHistory.setCellFactory(lv -> {
            ListCell<String> cell = new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
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

        messageInput.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                sendMessage();
            }
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
                String prompt = PromptBuilder.buildConversationResponse(userMessage);
                String reply = gemini.ask(prompt);

                Platform.runLater(() -> {
                    chatHistory.getItems().add("🤖 Game Bot: " + reply);
                    chatHistory.scrollTo(chatHistory.getItems().size() - 1);
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    chatHistory.getItems().add("❌ Error: Could not contact Gemini API.");
                    chatHistory.scrollTo(chatHistory.getItems().size() - 1);
                });
            }
        }).start();
    }

    @FXML
    private void clearChat() {
        chatHistory.getItems().clear();
    }
}
