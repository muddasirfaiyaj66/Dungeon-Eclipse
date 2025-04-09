package com.dungeon.controllers;

import com.dungeon.model.Item;
import com.dungeon.model.Inventory;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.stage.Stage;

public class InventoryController {
    @FXML
    private ListView<Item> itemListView;
    @FXML
    private Button useButton;
    @FXML
    private Button dropButton;
    @FXML
    private Button closeButton;

    private Inventory inventory;
    private GameController gameController;

    @FXML
    @SuppressWarnings("unused")
    public void initialize() {
        itemListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " (" + item.getType() + ")");
                }
            }
        });

        // Enable buttons only when an item is selected
        useButton.setDisable(true);
        dropButton.setDisable(true);

        itemListView.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> {
                boolean hasSelection = (newValue != null);
                useButton.setDisable(!hasSelection);
                dropButton.setDisable(!hasSelection);
            }
        );
    }

    public void setGameController(GameController gameController) {
        this.gameController = gameController;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
        refreshInventoryView();
    }

    private void refreshInventoryView() {
        itemListView.getItems().clear();
        itemListView.getItems().addAll(inventory.getItems());
    }

    @FXML
    @SuppressWarnings("unused")
    private void useItem() {
        Item selectedItem = itemListView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            // Handle item usage based on type
            switch (selectedItem.getType()) {
                case POTION:
                    gameController.getPlayer().heal(20); // Heal for 20 HP
                    inventory.removeItem(selectedItem);
                    break;
                case WEAPON:
                    // Equip weapon logic here
                    break;
                case ARMOR:
                    // Equip armor logic here
                    break;
            }
            refreshInventoryView();
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void dropItem() {
        Item selectedItem = itemListView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            inventory.removeItem(selectedItem);
            refreshInventoryView();
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void closeInventory() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
}
