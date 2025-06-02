package com.dungeon.controllers;

import com.dungeon.model.Item;
import com.dungeon.model.Inventory;
import com.dungeon.model.Armor;
import com.dungeon.model.Weapon;
import com.dungeon.model.entity.Player;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.stage.Stage;
import java.util.List;

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
    private Player player;

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
        this.player = gameController.getPlayer();
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
        if (selectedItem != null && gameController != null) {
            useItem(selectedItem);
            refreshInventoryView();
        }
    }

    public void useItem(Item item) {
        if (item == null) return;
        
        switch (item.getType()) {
            case POTION:
                player.heal(item.getValue());
                if (player.getInventory().hasItem(item.getType())) {
                    player.removeItem(item);
                }
                break;
                
            case WEAPON:
                if (item instanceof Weapon) {
                    Weapon weapon = (Weapon) item;
                    player.setEquippedWeapon(weapon);
                    player.setWeapon(weapon);
                    
                    List<Weapon> playerWeapons = player.getWeapons();
                    if (!playerWeapons.contains(weapon)) {
                        playerWeapons.removeIf(w -> w.getName().equals(weapon.getName()));
                        playerWeapons.add(weapon);
                    }
                    player.selectWeapon(playerWeapons.indexOf(weapon));
                }
                break;
                
            case ARMOR:
                if (item instanceof Armor) {
                    Armor armor = (Armor) item;
                    player.setEquippedArmor(armor);
                }
                break;
                
            case KEY:
                break;
                
            default:
                System.out.println("Unhandled item type in InventoryController.useItem: " + item.getType());
                break;
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void dropItem() {
        Item selectedItem = itemListView.getSelectionModel().getSelectedItem();
        if (selectedItem != null && gameController != null) {
            // Get player position for dropping the item
            double playerX = gameController.getPlayer().getX();
            double playerY = gameController.getPlayer().getY();
            
            // Minimum distance from player (in pixels)
            double minDistance = 50.0;
            double maxDistance = 100.0;
            
            // Generate random angle
            double angle = Math.random() * 2 * Math.PI;
            
            // Generate random distance between min and max
            double distance = minDistance + (Math.random() * (maxDistance - minDistance));
            
            // Calculate position using polar coordinates
            double offsetX = Math.cos(angle) * distance;
            double offsetY = Math.sin(angle) * distance;
            
            Item droppedItem;
            if (selectedItem instanceof Weapon) {
                Weapon weaponToDrop = (Weapon) selectedItem;
                droppedItem = new Weapon(
                    weaponToDrop.getName(),
                    weaponToDrop.getDescription(),
                    weaponToDrop.getBaseDamage(),
                    weaponToDrop.getWeaponType()
                );
                 // Unequip the item if it's a weapon or armor
                Weapon currentWeapon = player.getEquippedWeapon();
                if (currentWeapon != null && currentWeapon.equals(selectedItem)) {
                    player.setEquippedWeapon(null);
                    player.setWeapon(null); // Also clear currentWeapon if it was the one dropped
                }
            } else if (selectedItem instanceof Armor) {
                Armor armorToDrop = (Armor) selectedItem;
                droppedItem = new Armor(
                    armorToDrop.getName(),
                    armorToDrop.getDescription(),
                    armorToDrop.getArmorType()
                );
                // Unequip the item if it's a weapon or armor
                Armor currentArmor = player.getEquippedArmor();
                if (currentArmor != null && currentArmor.equals(selectedItem)) {
                    player.setEquippedArmor(null);
                }
            } else {
                 droppedItem = new Item(
                    selectedItem.getName(),
                    selectedItem.getDescription(),
                    selectedItem.getType(),
                    selectedItem.getValue(),
                    selectedItem.isConsumable()
                );
            }

            droppedItem.setX(playerX + offsetX);
            droppedItem.setY(playerY + offsetY);
            droppedItem.setSize(20); // Set appropriate size for the dropped item
            
            // Add the item to the game scene
            gameController.addItemToRoom(droppedItem);
            
            // Remove from inventory
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
