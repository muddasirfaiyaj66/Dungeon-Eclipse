package com.dungeon.model;

import javafx.geometry.Point2D;
import java.util.ArrayList;
import java.util.List;

public class DungeonRoom {
    private int x;
    private int y;
    private int width;
    private int height;
    private RoomType type;
    private boolean visited;
    private boolean locked;
    private List<DungeonRoom> connectedRooms;

    public enum RoomType {
        SPAWN,
        COMBAT,
        PUZZLE,
        TREASURE,
        BOSS
    }

    public DungeonRoom(int x, int y, int width, int height, RoomType type) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.type = type;
        this.visited = false;
        this.locked = type == RoomType.PUZZLE; // Puzzle rooms start locked
        this.connectedRooms = new ArrayList<>();
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public RoomType getType() {
        return type;
    }

    public boolean isVisited() {
        return visited;
    }

    public void setVisited(boolean visited) {
        this.visited = visited;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public List<DungeonRoom> getConnectedRooms() {
        return connectedRooms;
    }

    public void connect(DungeonRoom room) {
        if (!connectedRooms.contains(room)) {
            connectedRooms.add(room);
            room.connectedRooms.add(this);
        }
    }

    public boolean isConnectedTo(DungeonRoom room) {
        return connectedRooms.contains(room);
    }

    public boolean intersects(DungeonRoom other) {
        return x < other.x + other.width &&
               x + width > other.x &&
               y < other.y + other.height &&
               y + height > other.y;
    }

    public Point2D getCenter() {
        return new Point2D(x + width / 2.0, y + height / 2.0);
    }

    @Override
    public String toString() {
        return "Room[" + x + "," + y + "," + width + "," + height + "," + type + "]";
    }
}
