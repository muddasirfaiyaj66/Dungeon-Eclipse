package com.dungeon.model;

import com.dungeon.model.entity.Player;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Objects;

/**
 * Base class for all game items (weapons, armor, potions, etc.)
 */
public class Item {
    // Types of items
    public enum ItemType {
        WEAPON,
        ARMOR,
        POTION,
        KEY,
        TREASURE
    }
    
    private final String name;
    private final String description;
    private final ItemType type;
    private final int value;  // Flexible value - can mean damage, defense, heal amount, etc.
    private final boolean consumable;
    private final Image icon;
    
    private double x;
    private double y;
    private double width = 20;
    private double height = 20;
    private double pickupRadius = 30;
    private double size;

    /**
     * Creates a new item
     * @param name Item name
     * @param description Item description
     * @param type Item type
     * @param value Item value (damage, defense, heal amount, etc.)
     * @param consumable Whether this item is consumed on use
     * @param iconPath Path to the item's icon image
     */
    public Item(String name, String description, ItemType type, int value, boolean consumable, String iconPath) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.value = value;
        this.consumable = consumable;
        
        // Load image if path is provided
        if (iconPath != null && !iconPath.isEmpty()) {
            this.icon = new Image(getClass().getResourceAsStream(iconPath));
        } else {
            this.icon = null;
        }
    }
    
    /**
     * Creates a new item with default icon
     * @param name Item name
     * @param description Item description
     * @param type Item type
     * @param value Item value
     * @param consumable Whether this item is consumed on use
     */
    public Item(String name, String description, ItemType type, int value, boolean consumable) {
        this(name, description, type, value, consumable, null);
    }
    
    /**
     * Gets the item's name
     * @return Item name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the item's description
     * @return Item description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets the item's type
     * @return Item type
     */
    public ItemType getType() {
        return type;
    }
    
    /**
     * Gets the item's value (damage, defense, heal amount, etc.)
     * @return Item value
     */
    public int getValue() {
        return value;
    }
    
    /**
     * Checks if this item is consumed on use
     * @return true if this item is consumed on use
     */
    public boolean isConsumable() {
        return consumable;
    }
    
    /**
     * Gets the item's icon image
     * @return Item icon
     */
    public Image getIcon() {
        return icon;
    }
    
    @Override
    public String toString() {
        return name;
    }

    public boolean isPlayerTouching(Point2D playerPosition, double playerSize) {
        // Calculate distance between centers
        double itemCenterX = x + size / 2;
        double itemCenterY = y + size / 2;
        double playerCenterX = playerPosition.getX() + playerSize / 2;
        double playerCenterY = playerPosition.getY() + playerSize / 2;
        
        double dx = itemCenterX - playerCenterX;
        double dy = itemCenterY - playerCenterY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        // Collision if distance is less than sum of radii
        return distance < (size / 2 + playerSize / 2);
    }

    public double getSize() {
        return size;
    }

    public void setSize(double size) {
        this.size = size;
    }

    public void render(GraphicsContext gc) {
        switch (type) {
            case WEAPON:
                // Weapon item
                gc.setFill(Color.GRAY);
                gc.fillRect(x - width / 2, y - height / 2, width, height);
                gc.setStroke(Color.DARKGRAY);
                gc.setLineWidth(1);
                gc.strokeRect(x - width / 2, y - height / 2, width, height);
                
                // Draw sword symbol
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(2);
                gc.strokeLine(x, y - 7, x, y + 7);
                gc.strokeLine(x - 5, y - 5, x + 5, y + 5);
                break;
                
            case KEY:
                // Gold key
                gc.setFill(Color.GOLD);
                // Draw key head
                gc.fillOval(x - width / 2, y - height / 2, width / 2, height / 2);
                // Draw key shaft
                gc.fillRect(x - width / 4, y - height / 2 + height / 4, width / 2 + width / 4, height / 6);
                gc.fillRect(x + width / 4, y - height / 2 + height / 4, width / 6, height / 2);
                
                // Add sparkle effect
                gc.setFill(Color.WHITE);
                gc.fillOval(x - width / 2 + 3, y - height / 2 + 3, 2, 2);
                break;
                
            case ARMOR:
                // Armor item
                gc.setFill(Color.SLATEGRAY);
                gc.fillRoundRect(x - width / 2, y - height / 2, width, height, 5, 5);
                
                // Shield symbol
                gc.setStroke(Color.SILVER);
                gc.setLineWidth(2);
                gc.strokeOval(x - 5, y - 5, 10, 10);
                break;
                
            case POTION:
                // Blue potion
                gc.setFill(Color.BLUE);
                gc.fillOval(x - width / 2, y - height / 2, width, height);
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(2);
                gc.strokeOval(x - width / 2, y - height / 2, width, height);
                
                // Draw + symbol
                gc.setFill(Color.WHITE);
                gc.fillRect(x - 5, y - 1, 10, 2);
                gc.fillRect(x - 1, y - 5, 2, 10);
                break;
        }
        
        // Item glow effect (pulsing animation could be added later)
        double glowRadius = width * 1.5;
        gc.setGlobalAlpha(0.2);
        gc.setFill(getGlowColor());
        gc.fillOval(x - glowRadius / 2, y - glowRadius / 2, glowRadius, glowRadius);
        gc.setGlobalAlpha(1.0);
    }
    
    private Color getGlowColor() {
        switch (type) {
            case WEAPON: return Color.BLUE;
            case KEY: return Color.GOLD;
            case ARMOR: return Color.GREEN;
            case POTION: return Color.BLUE;
            default: return Color.WHITE;
        }
    }
    
    public boolean checkPlayerCollision(Player player) {
        return isPlayerTouching(player.getPosition(), player.getSize());
    }
    
    // Getters and setters
    public double getX() {
        return x;
    }
    
    public void setX(double x) {
        this.x = x;
    }
    
    public double getY() {
        return y;
    }
    
    public void setY(double y) {
        this.y = y;
    }
    
    public Point2D getPosition() {
        return new Point2D(x, y);
    }
    
    public void setPosition(Point2D position) {
        this.x = position.getX();
        this.y = position.getY();
    }
    
    public double getWidth() {
        return width;
    }
    
    public double getHeight() {
        return height;
    }
}
