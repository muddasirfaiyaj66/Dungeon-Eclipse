package com.dungeon.view;

import com.dungeon.model.DungeonRoom;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.List;

public class DungeonRenderer {
    private static final int TILE_SIZE = 8;
    private final Canvas canvas;
    private final GraphicsContext gc;

    public DungeonRenderer(Canvas canvas) {
        this.canvas = canvas;
        this.gc = canvas.getGraphicsContext2D();
    }

    public void render(List<DungeonRoom> rooms) {
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        for (DungeonRoom room : rooms) {
            renderRoom(room);
            renderConnections(room);
        }
    }

    private void renderRoom(DungeonRoom room) {
        // Set room color based on type
        switch (room.getType()) {
            case SPAWN -> gc.setFill(Color.GREEN);
            case BOSS -> gc.setFill(Color.RED);
            case COMBAT -> gc.setFill(Color.ORANGE);
            case PUZZLE -> gc.setFill(Color.BLUE);
            case TREASURE -> gc.setFill(Color.GOLD);
        }

        // Draw room
        gc.fillRect(
            room.getX() * TILE_SIZE,
            room.getY() * TILE_SIZE,
            room.getWidth() * TILE_SIZE,
            room.getHeight() * TILE_SIZE
        );

        // Draw room border
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);
        gc.strokeRect(
            room.getX() * TILE_SIZE,
            room.getY() * TILE_SIZE,
            room.getWidth() * TILE_SIZE,
            room.getHeight() * TILE_SIZE
        );
    }

    private void renderConnections(DungeonRoom room) {
        gc.setStroke(Color.GRAY);
        gc.setLineWidth(2);
        for (DungeonRoom connected : room.getConnectedRooms()) {
            // Only draw each connection once
            if (room.hashCode() < connected.hashCode()) {
                gc.strokeLine(
                    room.getCenter().getX() * TILE_SIZE,
                    room.getCenter().getY() * TILE_SIZE,
                    connected.getCenter().getX() * TILE_SIZE,
                    connected.getCenter().getY() * TILE_SIZE
                );
            }
        }
    }
}
