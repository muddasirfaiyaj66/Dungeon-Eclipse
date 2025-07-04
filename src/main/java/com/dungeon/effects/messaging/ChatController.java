package com.dungeon.effects.messaging;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

public class ChatController {

    @FXML private TextField messageInput;
    @FXML private ListView<String> chatHistory;

    @FXML
    public void initialize() {
        // Custom cell style for chat list
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

        // Trigger sendMessage when Enter is pressed
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
                String botReply = sendMessageToServer(userMessage);

                Platform.runLater(() -> {
                    chatHistory.getItems().add("🤖 Game Bot: " + botReply);
                    chatHistory.scrollTo(chatHistory.getItems().size() - 1);
                });

            } catch (IOException e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    chatHistory.getItems().add("❌ Error: Could not connect to server.");
                    chatHistory.scrollTo(chatHistory.getItems().size() - 1);
                });
            }
        }).start();
    }

    @FXML
    private void clearChat() {
        chatHistory.getItems().clear();
    }

    /**
     * Sends a message to the TCP puzzle server and receives a response.
     */
    private String sendMessageToServer(String message) throws IOException {
        try (
            Socket socket = new Socket("localhost", 9999);
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            out.write(message + "\n");
            out.flush();
            return in.readLine();
        }
    }
}
