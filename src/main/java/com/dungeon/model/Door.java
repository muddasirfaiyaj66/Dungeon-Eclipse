package com.dungeon.model;

import com.dungeon.controllers.GameController;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

public class Door {
    private double x;
    private double y;
    private double width = 60;
    private double height = 60;
    private boolean isLocked;
    private boolean requiresKey;
    private DungeonRoom connectedRoom;
    private Direction direction;

    public Door(double x, double y, double doorWidth, double doorHeight, DungeonRoom currentRoom, DungeonRoom connectedRoom, DoorDirection direction) {
        this.x = x;
        this.y = y;
        this.width = doorWidth;
        this.height = doorHeight;
        this.connectedRoom = connectedRoom;
        
        // Convert DoorDirection to Direction
        switch (direction) {
            case NORTH:
                this.direction = Direction.NORTH;
                break;
            case SOUTH:
                this.direction = Direction.SOUTH;
                break;
            case EAST:
                this.direction = Direction.EAST;
                break;
            case WEST:
                this.direction = Direction.WEST;
                break;
            default:
                // Default to NORTH if direction is invalid
                this.direction = Direction.NORTH;
                break;
        }
        
        // Set default properties
        this.isLocked = false;
        this.requiresKey = false;
        
        // Debug output
        System.out.println("Created door at " + x + "," + y + " to " + connectedRoom.getType() + 
                        " direction: " + this.direction);
    }

    public Door(double x, double y, DungeonRoom connectedRoom, GameController.Direction direction) {
        this.x = x;
        this.y = y;
        this.connectedRoom = connectedRoom;
        
        // Convert GameController.Direction to Door.Direction
        switch (direction) {
            case NORTH:
                this.direction = Direction.NORTH;
                break;
            case SOUTH:
                this.direction = Direction.SOUTH;
                break;
            case EAST:
                this.direction = Direction.EAST;
                break;
            case WEST:
                this.direction = Direction.WEST;
                break;
        }
        
        this.isLocked = false;
        this.requiresKey = false;
    }

    public Door(double x, double y, DungeonRoom connectedRoom, Direction direction) {
        this.x = x;
        this.y = y;
        this.connectedRoom = connectedRoom;
        this.direction = direction;
        this.isLocked = false;
        this.requiresKey = false;
    }

    public void unlockWithKey() {
        if (requiresKey) {
            isLocked = false;
            System.out.println("Door unlocked with key");
        }
    }

    public boolean canTransition() {
        return !isLocked;
    }

    public DungeonRoom getTargetRoom() {
        return connectedRoom;
    }

    public enum Direction {
        NORTH, SOUTH, EAST, WEST
    }
    
    public void render(GraphicsContext gc) {
        if (isLocked) {
            // Red for locked doors
            gc.setFill(Color.RED);
        } else {
            // Green for unlocked doors
            gc.setFill(Color.GREEN);
        }
        
        gc.fillRect(x, y, width, height);
        
        // Draw door frame
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        gc.strokeRect(x, y, width, height);
        
        // Draw lock icon if door requires a key
        if (requiresKey) {
            gc.setFill(Color.YELLOW);
            gc.fillOval(x + width / 2 - 10, y + height / 2 - 10, 20, 20);
            gc.setStroke(Color.BLACK);
            gc.strokeOval(x + width / 2 - 10, y + height / 2 - 10, 20, 20);
        }
    }
    
    public boolean contains(Point2D point) {
        return point.getX() >= x && point.getX() <= x + width &&
               point.getY() >= y && point.getY() <= y + height;
    }
    
    public boolean unlock(Inventory inventory) {
        if (!isLocked || !requiresKey) {
            return true; // Already unlocked or doesn't need a key
        }
        
        if (inventory.hasItem(Item.ItemType.KEY)) {
            // If player has a key, unlock the door and remove the key
            isLocked = false;
            inventory.removeItem(Item.ItemType.KEY, 1);
            return true;
        }
        
        return false; // Player doesn't have a key
    }
    
    // Getters and setters
    public double getX() {
        return x;
    }
    
    public double getY() {
        return y;
    }
    
    public double getWidth() {
        return width;
    }
    
    public double getHeight() {
        return height;
    }
    
    public boolean isLocked() {
        return isLocked;
    }
    
    public void setLocked(boolean locked) {
        this.isLocked = locked;
    }
    
    public boolean requiresKey() {
        return requiresKey;
    }
    
    public void setRequiresKey(boolean requiresKey) {
        this.requiresKey = requiresKey;
    }
    
    public DungeonRoom getConnectedRoom() {
        return connectedRoom;
    }
    
    public Direction getDirection() {
        return direction;
    }


    public enum DoorDirection {WEST, SOUTH, NORTH, EAST}
}
