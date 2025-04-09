package com.dungeon.model;

import javafx.geometry.Point2D;
import java.util.*;

public class DungeonGenerator {
    // Constants for dungeon generation
    private static final int MIN_ROOMS = 10;
    private static final int MAX_ROOMS = 15;
    private static final int MIN_SPECIAL_ROOMS = 3; // Minimum special rooms (puzzle/treasure)
    private static final double TREASURE_CHANCE = 0.3; // 30% chance for treasure room
    private static final double PUZZLE_CHANCE = 0.4; // 40% chance for puzzle room
    
    // Room dimensions
    private static final int ROOM_WIDTH = 10;
    private static final int ROOM_HEIGHT = 10;
    
    // Direction vectors for room connections
    private static final int[][] DIRECTIONS = {
        {0, -1}, // North
        {1, 0},  // East
        {0, 1},  // South
        {-1, 0}  // West
    };
    
    private Random random;
    private int difficulty; // 1-3, affects dungeon complexity

    public DungeonGenerator() {
        this.random = new Random();
        this.difficulty = 1;
    }
    
    public DungeonGenerator(int difficulty) {
        this.random = new Random();
        this.difficulty = Math.min(3, Math.max(1, difficulty)); // Clamp between 1-3
    }

    public List<DungeonRoom> generateDungeon() {
        // Calculate number of rooms based on difficulty
        int targetRooms = MIN_ROOMS + (difficulty - 1) * 3;
        targetRooms = Math.min(MAX_ROOMS, targetRooms);
        
        // Create dungeon map (hash map of coordinates to rooms)
        Map<Point2D, DungeonRoom> dungeonMap = new HashMap<>();
        List<DungeonRoom> roomList = new ArrayList<>();
        Queue<DungeonRoom> roomsToProcess = new LinkedList<>();
        
        // Create spawn room at origin
        DungeonRoom spawnRoom = new DungeonRoom(0, 0, ROOM_WIDTH, ROOM_HEIGHT, DungeonRoom.RoomType.SPAWN);
        dungeonMap.put(new Point2D(0, 0), spawnRoom);
        roomList.add(spawnRoom);
        roomsToProcess.add(spawnRoom);
        
        // Create boss room - will be placed farthest from spawn
        DungeonRoom bossRoom = new DungeonRoom(0, 0, ROOM_WIDTH, ROOM_HEIGHT, DungeonRoom.RoomType.BOSS);
        DungeonRoom farthestRoom = spawnRoom;
        int maxDistance = 0;
        
        // Generate connected rooms using BFS approach
        while (!roomsToProcess.isEmpty() && roomList.size() < targetRooms) {
            DungeonRoom currentRoom = roomsToProcess.poll();
            
            // Try to add rooms in each direction
            for (int[] dir : DIRECTIONS) {
                // Stop if we've reached our target room count
                if (roomList.size() >= targetRooms) {
                    break;
                }
                
                // Calculate potential new room coordinates
                int newX = currentRoom.getX() + dir[0] * ROOM_WIDTH;
                int newY = currentRoom.getY() + dir[1] * ROOM_HEIGHT;
                Point2D newPos = new Point2D(newX, newY);
                
                // Skip if room already exists at this position
                if (dungeonMap.containsKey(newPos)) {
                    continue;
                }
                
                // Room creation probability decreases with distance from origin 
                // to create more interesting, branching layouts
                double distanceFromOrigin = Math.sqrt(newX * newX + newY * newY);
                double createChance = 0.9 - (distanceFromOrigin * 0.1);
                createChance = Math.max(0.3, createChance); // Minimum 30% chance
                
                // Higher difficulty increases chance of room creation
                createChance += (difficulty - 1) * 0.1;
                
                if (random.nextDouble() < createChance) {
                    // Determine room type - Combat is default
                    DungeonRoom.RoomType roomType = DungeonRoom.RoomType.COMBAT;
                    
                    // 30% chance for treasure room, 40% chance for puzzle room (if not treasure)
                    if (random.nextDouble() < TREASURE_CHANCE) {
                        roomType = DungeonRoom.RoomType.TREASURE;
                    } else if (random.nextDouble() < PUZZLE_CHANCE) {
                        roomType = DungeonRoom.RoomType.PUZZLE;
                    }
                    
                    // Create new room
                    DungeonRoom newRoom = new DungeonRoom(newX, newY, ROOM_WIDTH, ROOM_HEIGHT, roomType);
                    dungeonMap.put(newPos, newRoom);
                    roomList.add(newRoom);
                    roomsToProcess.add(newRoom);
                    
                    // Connect rooms bidirectionally
                    currentRoom.connect(newRoom);
                    
                    // Check if this is the farthest room from spawn
                    if (distanceFromOrigin > maxDistance) {
                        maxDistance = (int)distanceFromOrigin;
                        farthestRoom = newRoom;
                    }
                }
            }
        }
        
        // Ensure we have a minimum number of special rooms
        ensureMinimumSpecialRooms(roomList);
        
        // Place boss room at or near the farthest position from spawn
        if (farthestRoom != spawnRoom) {
            // Remove the room at the farthest position
            roomList.remove(farthestRoom);
            // Replace with boss room at same position
            bossRoom = new DungeonRoom(farthestRoom.getX(), farthestRoom.getY(), ROOM_WIDTH, ROOM_HEIGHT, DungeonRoom.RoomType.BOSS);
            roomList.add(bossRoom);
            
            // Connect boss room to the same rooms that were connected to the farthest room
            for (DungeonRoom connectedRoom : farthestRoom.getConnectedRooms()) {
                bossRoom.connect(connectedRoom);
                
                // Update the connected room's connections (already handled by connect method)
                // The connect method in DungeonRoom already handles bidirectional connections
            }
        }
        
        // Ensure connectivity of the entire dungeon
        ensureDungeonConnectivity(roomList);
        
        return roomList;
    }
    
    private void ensureMinimumSpecialRooms(List<DungeonRoom> rooms) {
        // Count existing special rooms
        int treasureCount = 0;
        int puzzleCount = 0;

        for (DungeonRoom room : rooms) {
            if (room.getType() == DungeonRoom.RoomType.TREASURE) {
                treasureCount++;
            } else if (room.getType() == DungeonRoom.RoomType.PUZZLE) {
                puzzleCount++;
            }
        }
        
        // Calculate how many more special rooms we need
        int targetSpecialRooms = MIN_SPECIAL_ROOMS + (difficulty - 1);
        int currentSpecialRooms = treasureCount + puzzleCount;
        int roomsToConvert = Math.max(0, targetSpecialRooms - currentSpecialRooms);
        
        // Convert some combat rooms to special rooms if needed
        if (roomsToConvert > 0) {
            // We need to implement a way to change room types
            // Since DungeonRoom doesn't have a setType method, we'll need to replace rooms
            
            // Get list of eligible combat rooms (not spawn or boss)
            List<Integer> combatRoomIndices = new ArrayList<>();
            for (int i = 0; i < rooms.size(); i++) {
                DungeonRoom room = rooms.get(i);
                if (room.getType() == DungeonRoom.RoomType.COMBAT) {
                    combatRoomIndices.add(i);
                }
            }
            
            // Shuffle to randomize selection
            Collections.shuffle(combatRoomIndices);
            
            // Convert rooms
            for (int i = 0; i < Math.min(roomsToConvert, combatRoomIndices.size()); i++) {
                int index = combatRoomIndices.get(i);
                DungeonRoom oldRoom = rooms.get(index);
                
                // Determine new room type
                DungeonRoom.RoomType newType = (i % 2 == 0) 
                    ? DungeonRoom.RoomType.TREASURE 
                    : DungeonRoom.RoomType.PUZZLE;
                
                // Create new room with the new type
                DungeonRoom newRoom = new DungeonRoom(
                    oldRoom.getX(), 
                    oldRoom.getY(), 
                    oldRoom.getWidth(), 
                    oldRoom.getHeight(), 
                    newType
                );
                
                // Connect to the same rooms
                for (DungeonRoom connectedRoom : oldRoom.getConnectedRooms()) {
                    newRoom.connect(connectedRoom);
                }
                
                // Replace in the list
                rooms.set(index, newRoom);
            }
        }
    }
    
    private void ensureDungeonConnectivity(List<DungeonRoom> rooms) {
        // Simple connectivity check - make sure all rooms are reachable from spawn
        Set<DungeonRoom> visited = new HashSet<>();
        Queue<DungeonRoom> queue = new LinkedList<>();
        
        // Find spawn room
        DungeonRoom spawn = null;
        for (DungeonRoom room : rooms) {
            if (room.getType() == DungeonRoom.RoomType.SPAWN) {
                spawn = room;
                break;
            }
        }
        
        if (spawn == null) return; // No spawn room found
        
        // BFS from spawn
        queue.add(spawn);
        visited.add(spawn);
        
        while (!queue.isEmpty()) {
            DungeonRoom current = queue.poll();
            
            for (DungeonRoom neighbor : current.getConnectedRooms()) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        
        // Connect any unvisited rooms to the nearest visited room
        if (visited.size() < rooms.size()) {
            for (DungeonRoom room : rooms) {
                if (!visited.contains(room)) {
                    // Find closest visited room
                    DungeonRoom closest = null;
                    int minDistance = Integer.MAX_VALUE;
                    
                    for (DungeonRoom visitedRoom : visited) {
                        int distance = calculateManhattanDistance(room, visitedRoom);
                        if (distance < minDistance) {
                            minDistance = distance;
                            closest = visitedRoom;
                        }
                    }
                    
                    // Connect rooms bidirectionally
                    if (closest != null) {
                        room.connect(closest);
                        
                        // Mark as visited and add to queue to continue connectivity check
                        visited.add(room);
                        queue.add(room);
                    }
                }
            }
        }
    }
    
    private int calculateManhattanDistance(DungeonRoom a, DungeonRoom b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY());
    }
    
    public void setDifficulty(int difficulty) {
        this.difficulty = Math.min(3, Math.max(1, difficulty));
    }
}
