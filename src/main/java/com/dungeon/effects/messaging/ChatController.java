package com.dungeon.effects.messaging;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
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
    chatHistory.scrollTo(chatHistory.getItems().size() - 1); // scroll after user message
    messageInput.clear();

    new Thread(() -> {
        try {
            String prompt = PromptBuilder.buildHintPrompt(userMessage);
            String hint = gemini.ask(prompt);

            // UI update must be inside Platform.runLater
            Platform.runLater(() -> {
                chatHistory.getItems().add("🤖 Game Bot: " + hint);
                chatHistory.scrollTo(chatHistory.getItems().size() - 1); // scroll after bot reply
            });

        } catch (Exception e) {
            Platform.runLater(() -> {
                chatHistory.getItems().add("❌ Error fetching hint.");
                chatHistory.scrollTo(chatHistory.getItems().size() - 1);
            });
            e.printStackTrace();
        }
    }).start();
}


@FXML
public void initialize() {
    messageInput.setOnAction(event -> sendMessage());

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
                    // ❌ Remove this line:
                    // setPrefWidth(lv.getWidth() - 20);
                }
            }
        };
        cell.prefWidthProperty().bind(lv.widthProperty().subtract(20)); 
        return cell;
    });
}

    @FXML
    private void clearChat() {
        chatHistory.getItems().clear();

}
}
