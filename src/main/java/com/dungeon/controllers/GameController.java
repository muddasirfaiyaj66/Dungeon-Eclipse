package com.dungeon.controllers;

import com.dungeon.effects.EffectsManager;
import com.dungeon.model.DungeonGenerator;
import com.dungeon.model.DungeonRoom;
import com.dungeon.model.entity.Enemy;
import com.dungeon.model.entity.EnemyAbility;
import com.dungeon.model.entity.Entity;
import com.dungeon.model.Door; 
import com.dungeon.model.Inventory;
import com.dungeon.model.Item;
import com.dungeon.model.entity.Player;
import com.dungeon.model.entity.Projectile;
import com.dungeon.model.entity.ProjectileAttack;
import com.dungeon.model.Puzzle;
import com.dungeon.view.DungeonRenderer;
import com.dungeon.view.LightingEffect;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle; 
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.HashMap;

public class GameController {
    // Enum for door directions
    public enum Direction {
        NORTH,
        SOUTH,
        EAST,
        WEST
    }

    @FXML
    private Canvas gameCanvas;
    @FXML
    private javafx.scene.layout.BorderPane rootPane;
    @FXML
    private javafx.scene.layout.StackPane canvasContainer;
    
    private DungeonGenerator dungeonGenerator;
    private DungeonRenderer dungeonRenderer;
    private List<DungeonRoom> currentDungeon;
    private Player player;
    private List<Enemy> enemies;
    private Set<KeyCode> activeKeys;
    private boolean gameLoopRunning;
    private boolean roomTransitionInProgress;
    private double mouseX, mouseY;
    private Random random;
    private EffectsManager effectsManager;
    private LightingEffect lightingEffect;
    private List<String> floatingTexts; // For displaying damage, pickups, etc.
    private List<ProjectileAttack> playerProjectiles;
    private List<EnemyAbility.Projectile> enemyProjectiles;
    private List<Item> roomItems; // Items in the current room
    private java.util.Map<DungeonRoom, Puzzle> puzzles; // Puzzles for puzzle rooms
    private List<Door> doors; // Doors in the current room
    private double timeSinceLastSpawn;
    private boolean bossDefeated;
    private boolean roomCleared;
    private DungeonRoom currentRoom;
    private long lastUpdateTime;
    private List<Projectile> projectiles = new ArrayList<>();
    private boolean puzzleCompleted;
    private int currentLevel = 1;
    private int enemiesDefeated = 0;
    private long startTime;
    private boolean gameOver = false;
    private boolean victory = false;
    private boolean isDebugMode = true; // Enable debug mode by default to diagnose issues

    // Room transition constants
    private static final double DOOR_WIDTH = 40;
    private static final double DOOR_HEIGHT = 40;
    private static final double ROOM_PADDING = 50; // Distance from room edge to door
    private static final double TRANSITION_DURATION = 0.5; // seconds

    private javafx.scene.layout.Pane minimapPane;
    private java.util.Map<DungeonRoom, Rectangle> minimapCells;

    // Game state
    private boolean isPaused = false;
    private javafx.scene.layout.VBox pauseMenu;
    
    // Add resize handling
    private void handleResize() {
        if (canvasContainer == null || gameCanvas == null) {
            System.err.println("WARNING: canvasContainer or gameCanvas is null in handleResize");
            return;
        }
        
        System.out.println("Setting up window resize handling...");
        
        // Bind canvas size to parent container size
        gameCanvas.widthProperty().bind(canvasContainer.widthProperty());
        gameCanvas.heightProperty().bind(canvasContainer.heightProperty());
        
        // Add listener for canvas width changes
        gameCanvas.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            
            System.out.println("Canvas width changed: " + oldVal + " -> " + newVal);
            
            // Force rendering if player exists and game is not paused
            if (player != null && !isPaused) {
                // Keep player in the bounds of the new canvas
                double playerX = player.getX();
                if (playerX > newVal.doubleValue() - player.getWidth()) {
                    player.setPosition(newVal.doubleValue() - player.getWidth(), player.getY());
                }
                
                // Render the updated scene
                render();
            }
        });
        
        // Add listener for canvas height changes
        gameCanvas.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            
            System.out.println("Canvas height changed: " + oldVal + " -> " + newVal);
            
            // Force rendering if player exists and game is not paused
            if (player != null && !isPaused) {
                // Keep player in the bounds of the new canvas
                double playerY = player.getY();
                if (playerY > newVal.doubleValue() - player.getHeight()) {
                    player.setPosition(player.getX(), newVal.doubleValue() - player.getHeight());
                }
                
                // Render the updated scene
                render();
            }
        });
        
        // Also bind the root pane to the scene size
        if (rootPane != null && rootPane.getScene() != null) {
            rootPane.prefWidthProperty().bind(rootPane.getScene().widthProperty());
            rootPane.prefHeightProperty().bind(rootPane.getScene().heightProperty());
        }
        
        System.out.println("Window resize handling set up successfully");
    }
    
    private void createPauseMenu() {
        pauseMenu = new javafx.scene.layout.VBox(10);
        pauseMenu.setAlignment(javafx.geometry.Pos.CENTER);
        pauseMenu.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7); -fx-padding: 20px;");
        pauseMenu.setPrefWidth(300);
        pauseMenu.setPrefHeight(200);
        
        // Create title
        javafx.scene.text.Text title = new javafx.scene.text.Text("Paused");
        title.setFont(javafx.scene.text.Font.font("Verdana", javafx.scene.text.FontWeight.BOLD, 24));
        title.setFill(javafx.scene.paint.Color.WHITE);
        
        // Create buttons
        javafx.scene.control.Button resumeButton = new javafx.scene.control.Button("Resume Game");
        resumeButton.setPrefWidth(200);
        resumeButton.setOnAction(e -> resumeGame());
        
        javafx.scene.control.Button optionsButton = new javafx.scene.control.Button("Options");
        optionsButton.setPrefWidth(200);
        optionsButton.setOnAction(e -> showOptions());
        
        javafx.scene.control.Button exitButton = new javafx.scene.control.Button("Exit to Main Menu");
        exitButton.setPrefWidth(200);
        exitButton.setOnAction(e -> exitToMainMenu());
        
        // Add components to menu
        pauseMenu.getChildren().addAll(title, resumeButton, optionsButton, exitButton);
        
        // Center the pause menu
        javafx.scene.layout.StackPane.setAlignment(pauseMenu, javafx.geometry.Pos.CENTER);
        
        // Initially hide the pause menu
        pauseMenu.setVisible(false);
        
        // Add to root pane
        rootPane.getChildren().add(pauseMenu);
    }
    
    private void togglePauseGame() {
        isPaused = !isPaused;
        
        if (isPaused) {
            pauseMenu.setVisible(true);
            // Stop the game loop
            gameLoopRunning = false;
        } else {
            pauseMenu.setVisible(false);
            // Resume the game loop
            gameLoopRunning = true;
            startGameLoop();
        }
    }
    
    private void resumeGame() {
        isPaused = false;
        pauseMenu.setVisible(false);
        gameLoopRunning = true;
        startGameLoop();
        
        // Make sure canvas has focus
        gameCanvas.requestFocus();
    }
    
    private void showOptions() {
        // In a full implementation, you would show options dialog
        // For now, just show a message
        System.out.println("Options would be shown here");
    }
    
    private void exitToMainMenu() {
        try {
            // Load the main menu
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/dungeon/fxml/MainMenu.fxml"));
            Parent menuRoot = loader.load();
            Scene menuScene = new Scene(menuRoot);
            
            // Get the current stage and set the new scene
            Stage stage = (Stage) gameCanvas.getScene().getWindow();
            stage.setScene(menuScene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {
        dungeonGenerator = new DungeonGenerator();
        dungeonRenderer = new DungeonRenderer(gameCanvas);
        activeKeys = new HashSet<>();
        enemies = new ArrayList<>();
        roomItems = new ArrayList<>();
        puzzles = new java.util.HashMap<>();
        doors = new ArrayList<>();
        playerProjectiles = new ArrayList<>();
        enemyProjectiles = new ArrayList<>();
        effectsManager = new EffectsManager(rootPane, gameCanvas);
        floatingTexts = new ArrayList<>();
        random = new Random();
        timeSinceLastSpawn = 0;
        bossDefeated = false;
        gameLoopRunning = true;
        roomTransitionInProgress = false;
        roomCleared = false;
        puzzleCompleted = false;
        
        // Set the start time when game begins
        startTime = System.currentTimeMillis();
        
        // Create the pause menu
        createPauseMenu();
        
        setupGame();
        startGameLoop();
    }

    public void onSceneReady() {
        // Setup input handling
        setupInputHandling();
        
        // Initialize lighting effect after canvas is attached to scene
        lightingEffect = new LightingEffect(gameCanvas);
        
        // Setup window resize handling
        handleResize();
        
        // Request focus on the canvas so it can receive key events
        gameCanvas.setFocusTraversable(true);
        gameCanvas.requestFocus();
    }

    private void setupGame() {
        // Initialize game components
        random = new Random();
        activeKeys = new HashSet<>();
        enemies = new ArrayList<>();
        roomItems = new ArrayList<>();
        doors = new ArrayList<>();
        playerProjectiles = new ArrayList<>();
        projectiles = new ArrayList<>();
        
        // Initialize effects manager if not already initialized
        if (effectsManager == null) {
        effectsManager = new EffectsManager(rootPane, gameCanvas);
        }
        
        // Set gameLoopRunning flag
        gameLoopRunning = true;
        
        // Generate dungeon
        System.out.println("Generating dungeon...");
        dungeonGenerator = new DungeonGenerator();
        currentDungeon = dungeonGenerator.generateDungeon();
        
        System.out.println("Dungeon created with " + currentDungeon.size() + " rooms");
        
        // Set current room to spawn room
        java.util.Optional<DungeonRoom> spawnRoom = currentDungeon.stream()
            .filter(room -> room.getType() == DungeonRoom.RoomType.SPAWN)
            .findFirst();
            
        if (spawnRoom.isPresent()) {
            currentRoom = spawnRoom.get();
            currentRoom.setVisited(true);
            
            System.out.println("Found spawn room: " + currentRoom);
            
            // Generate puzzles for puzzle rooms
            generatePuzzles();
            
            // Find spawn room and place player
            Point2D spawnPoint = new Point2D(gameCanvas.getWidth() / 2, gameCanvas.getHeight() / 2);
            player = new Player(spawnPoint.getX(), spawnPoint.getY());
            
            System.out.println("Player created at " + spawnPoint);
            
            populateRoom(currentRoom);
            createDoors();
        } else {
            System.err.println("ERROR: No spawn room found in dungeon!");
        }
    }

    private void generatePuzzles() {
        for (DungeonRoom room : currentDungeon) {
            if (room.getType() == DungeonRoom.RoomType.PUZZLE) {
                // Create a random puzzle for each puzzle room
                puzzles.put(room, Puzzle.createRandomPuzzle());
            }
        }
    }

    private void setupInputHandling() {
        Scene scene = gameCanvas.getScene();
        activeKeys = new HashSet<>();
        
        scene.setOnKeyPressed(e -> {
            activeKeys.add(e.getCode());
            
            // Handle inventory key
            if (e.getCode() == KeyCode.I) {
                openInventory();
            } else if (e.getCode() == KeyCode.F) {  
                // Interact with puzzle or door
                if (currentRoom.getType() == DungeonRoom.RoomType.PUZZLE && !puzzles.get(currentRoom).isSolved()) {
                    interactWithPuzzle();
                } else {
                    // Try to interact with doors
                    checkDoorInteraction();
                }
            } else if (e.getCode() == KeyCode.ESCAPE) {
                // Toggle pause menu
                togglePauseGame();
            }
            
            // Handle projectile attack with E key
            if (e.getCode() == KeyCode.E) {
                firePlayerProjectile();
            }
            
            // Handle melee attack with Space key
            if (e.getCode() == KeyCode.SPACE) {
                // Implement melee attack
                attackEnemiesInRange();
            }
            
            // Handle weapon selection with number keys
            if (e.getCode().isDigitKey()) {
                try {
                    int weaponIndex = Integer.parseInt(e.getCode().getName()) - 1;
                    if (weaponIndex >= 0 && weaponIndex < 4) {
                        System.out.println("Digit key pressed: " + e.getCode().getName() + ", weapon index: " + weaponIndex);
                        selectWeapon(weaponIndex);
                    }
                } catch (NumberFormatException ex) {
                    System.out.println("Error parsing digit: " + ex.getMessage());
                }
            }
        });
        
        scene.setOnKeyReleased(e -> activeKeys.remove(e.getCode()));
        
        // Add mouse movement handler for aiming
        scene.setOnMouseMoved(e -> {
            if (player != null) {
                mouseX = e.getX();
                mouseY = e.getY();
                player.updateMousePosition(e.getX(), e.getY());
            }
        });
        
        // Add mouse drag handler for aiming while moving
        scene.setOnMouseDragged(e -> {
            if (player != null) {
                mouseX = e.getX();
                mouseY = e.getY();
                player.updateMousePosition(e.getX(), e.getY());
            }
        });
        
        // Add mouse click handler for shooting and UI interaction
        scene.setOnMouseClicked(e -> {
            if (player != null) {
                // Check if pause button was clicked
                double buttonSize = 30;
                double padding = 10;
                double buttonX = gameCanvas.getWidth() - buttonSize - padding;
                double buttonY = padding;
                
                if (e.getX() >= buttonX && e.getX() <= buttonX + buttonSize &&
                    e.getY() >= buttonY && e.getY() <= buttonY + buttonSize) {
                    // Pause button clicked
                    togglePauseGame();
                    return;
                }
                
                // Otherwise handle left-click for shooting
                if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY && !isPaused) {
                firePlayerProjectile();
            }
            }
        });
    }

    private void attackEnemiesInRange() {
        // Find enemies in melee range and damage them
        double meleeRange = 50; // Melee attack range
        for (Enemy enemy : enemies) {
            if (isInMeleeRange(player, enemy)) {
                // Apply melee damage
                double meleeDamage = 20;
                enemy.takeDamage(meleeDamage);
                
                // Show damage text
                effectsManager.showFloatingText("-" + (int)meleeDamage, 
                    enemy.getPosition(), Color.ORANGE);
            }
        }
    }

    private void startGameLoop() {
        // Don't create a new AnimationTimer if we already have one running
        if (gameLoopRunning) {
        lastUpdateTime = System.nanoTime();
        
            AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                    if (gameLoopRunning && !roomTransitionInProgress && !isPaused) {
                    double deltaTime = (now - lastUpdateTime) / 1_000_000_000.0;
                    lastUpdateTime = now;
                    
                    update(deltaTime);
                    render();
                }
            }
            };
            
            timer.start();
        }
    }

    private void update(double deltaTime) {
        // Debug game state
        System.out.println("UPDATE CALLED | gameLoopRunning=" + gameLoopRunning + 
                          " | roomTransitionInProgress=" + roomTransitionInProgress);
                          
        // Check if player is initialized
        if (player == null) {
            System.out.println("ERROR: Player is null in update method");
            setupGame(); // Try to re-initialize the game
            return;
        }
        
        // Check if current room is initialized
        if (currentRoom == null) {
            System.out.println("ERROR: Current room is null in update method");
            setupGame(); // Try to re-initialize the game
            return;
        }
        
        // Skip update if room transition is in progress
        if (roomTransitionInProgress) {
            return;
        }
        
        // Debug message for room type
        String roomType = currentRoom.getType().toString();
        System.out.println("Current room type: " + roomType + " | Enemies: " + enemies.size() + " | Items: " + roomItems.size() + " | Projectiles: " + playerProjectiles.size());
        
        // Handle player input
        player.handleInput(activeKeys, deltaTime);
        
        // Move player
        movePlayer(deltaTime);
        
        // Check for item pickups
        checkItemPickups();
        
        // Check for door interactions
        checkDoorInteraction();
        
        // Check for room transitions
        checkRoomTransition();
        
        // Update enemies
        updateEnemies(deltaTime);
        
        // Update projectiles
        updateProjectiles(deltaTime);
        
        // Update player projectiles
        updatePlayerProjectiles(deltaTime);
        
        // Check for enemy defeat conditions
        checkRoomClearConditions();
        
        // Check player status
        checkPlayerStatus();
        
        // Check victory condition
        checkVictoryCondition();
        
        // Update effects
        effectsManager.update(deltaTime);
    }
    
    private void checkItemPickups() {
        Iterator<Item> itemIterator = roomItems.iterator();
        while (itemIterator.hasNext()) {
            Item item = itemIterator.next();
            
            // Debug print
            System.out.println("Checking item: " + item.getName() + " at " + item.getX() + "," + item.getY());
            System.out.println("Player position: " + player.getX() + "," + player.getY());

            // Improved collision detection for items
            Point2D playerCenter = player.getPosition().add(player.getSize() / 2, player.getSize() / 2);
            Point2D itemCenter = new Point2D(item.getX(), item.getY());
            double distance = playerCenter.distance(itemCenter);

            // If player is close enough to item
            if (distance < (player.getSize() / 2 + item.getSize() / 2)) {
                System.out.println("Player picked up: " + item.getName());
                
                // Handle item pickup based on type
                switch (item.getType()) {
                    case POTION:
                        player.heal(20); // Heal player by 20 HP
                        effectsManager.showFloatingText("+" + 20 + " HP", 
                            player.getPosition(), Color.GREEN);
                        player.addScore(25); // Add score for potion
                        break;
                    case WEAPON:
                        // Add weapon to inventory
                        player.addItem(item);
                        effectsManager.showFloatingText("Picked up " + item.getName(), 
                            player.getPosition(), Color.YELLOW);
                        player.addScore(50); // Add score for weapon
                        break;
                    case ARMOR:
                        // Add armor to inventory
                        player.addItem(item);
                        effectsManager.showFloatingText("Picked up " + item.getName(), 
                            player.getPosition(), Color.BLUE);
                        player.addScore(50); // Add score for armor
                        break;
                    case KEY:
                        // Unlock all doors in the current room
                        unlockDoorsWithKey();
                        effectsManager.showFloatingText("Doors unlocked!", 
                            player.getPosition(), Color.GOLD);
                        player.addScore(100); // Add score for key
                        break;
                    default:
                        // Generic pickup
                        player.addItem(item);
                        effectsManager.showFloatingText("Picked up " + item.getName(), 
                            player.getPosition(), Color.WHITE);
                        player.addScore(25); // Default score addition
                        break;
                }
                
                // Remove item from room
                itemIterator.remove();
            }
        }
    }
    
    private void unlockDoorsWithKey() {
        for (Door door : doors) {
            if (door.requiresKey()) {
                door.unlockWithKey();
            }
        }
    }
    
    private void checkDoorInteraction() {
        // Check if player is near a door and pressing F
        if (activeKeys.contains(KeyCode.F)) {
            for (Door door : doors) {
                if (isPlayerTouchingDoor(door)) {
                    if (door.isLocked()) {
                        if (door.requiresKey()) {
                            // Show message that door requires a key
                            effectsManager.showFloatingText("This door requires a key", 
                                new Point2D(door.getX() + door.getWidth()/2, door.getY() - 20), 
                                Color.YELLOW);
                        } else {
                            // Show message about what's needed to unlock the door
                            String message = "";
                            switch (currentRoom.getType()) {
                                case COMBAT:
                                    message = "Defeat all enemies to unlock";
                                    break;
                                case BOSS:
                                    message = "Defeat the boss to unlock";
                                    break;
                                default:
                                    message = "This door is locked";
                            }
                            effectsManager.showFloatingText(message, 
                                new Point2D(door.getX() + door.getWidth()/2, door.getY() - 20), 
                                Color.RED);
                        }
                    }
                    // If door is unlocked, the transition will be handled in checkRoomTransition
                }
            }
            // Remove the key press to prevent multiple interactions
            activeKeys.remove(KeyCode.F);
        }
    }
    
    private boolean isPlayerTouchingDoor(Door door) {
        // Calculate player center
        Point2D playerCenter = player.getPosition().add(player.getSize() / 2, player.getSize() / 2);
        
        // Calculate door center and dimensions
        double doorCenterX = door.getX() + door.getWidth() / 2;
        double doorCenterY = door.getY() + door.getHeight() / 2;
        
        // Calculate distance between centers
        double distanceX = Math.abs(playerCenter.getX() - doorCenterX);
        double distanceY = Math.abs(playerCenter.getY() - doorCenterY);
        
        // Check if player is close enough to interact with door
        return distanceX < (player.getSize() / 2 + door.getWidth() / 2 + 10) &&
               distanceY < (player.getSize() / 2 + door.getHeight() / 2 + 10);
    }
    
    private void checkRoomClearConditions() {
        // For combat rooms, check if all enemies are defeated
        if (currentRoom.getType() == DungeonRoom.RoomType.COMBAT && enemies.isEmpty()) {
            // Unlock all doors
            for (Door door : doors) {
                if (!door.requiresKey()) {
                    door.setLocked(false);
                }
            }
            
            // Show message
            if (!roomCleared) {
                effectsManager.showFloatingText("Room cleared! Doors unlocked", 
                    new Point2D(gameCanvas.getWidth() / 2, gameCanvas.getHeight() / 4), 
                    Color.GREEN);
                roomCleared = true;
            }
        }
        
        // For boss rooms, check if boss is defeated
        if (currentRoom.getType() == DungeonRoom.RoomType.BOSS && enemies.isEmpty()) {
            // Unlock all doors
            for (Door door : doors) {
                door.setLocked(false);
            }
            
            // Show victory message
            if (!roomCleared) {
                effectsManager.showFloatingText("Boss defeated! Victory!", 
                    new Point2D(gameCanvas.getWidth() / 2, gameCanvas.getHeight() / 4), 
                    Color.GOLD);
                roomCleared = true;
            }
        }
    }
    
    private void render() {
        if (gameCanvas == null) {
            System.err.println("ERROR: Cannot render - gameCanvas is null");
            return;
        }
        
        GraphicsContext gc = gameCanvas.getGraphicsContext2D();
        
        // Clear the canvas
        gc.clearRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());
        
        // Draw the room background with appropriate lighting
        drawRoomBackground(gc);
        
        // Draw debug information if in debug mode
        if (isDebugMode) {
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Verdana", FontWeight.BOLD, 12));
            
            gc.fillText("DEBUG MODE", 10, 20);
            gc.fillText("Current Room: " + currentRoom.getType() + " at (" + currentRoom.getX() + "," + currentRoom.getY() + ")", 10, 40);
            gc.fillText("Enemies: " + enemies.size(), 10, 60);
            gc.fillText("Doors: " + doors.size(), 10, 80);
            gc.fillText("Items: " + roomItems.size(), 10, 100);
            gc.fillText("Canvas Size: " + gameCanvas.getWidth() + "x" + gameCanvas.getHeight(), 10, 120);
        }
        
        // Draw the room doors
        if (doors.isEmpty()) {
            System.out.println("No doors to render");
        } else {
            System.out.println("Rendering " + doors.size() + " doors");
        for (Door door : doors) {
                // Draw door rectangle
            Color doorColor = door.isLocked() ? Color.RED : Color.GREEN;
            gc.setFill(doorColor);
            gc.fillRect(door.getX(), door.getY(), door.getWidth(), door.getHeight());
            
            // Add door frame
            gc.setStroke(Color.DARKGRAY);
            gc.setLineWidth(3);
            gc.strokeRect(door.getX() - 2, door.getY() - 2, door.getWidth() + 4, door.getHeight() + 4);
            
            // Add door handle
            gc.setFill(Color.GOLD);
            gc.fillOval(
                door.getX() + door.getWidth() * 0.8, 
                door.getY() + door.getHeight() / 2, 
                5, 
                5
                );
                
                // Draw key icon if door requires key
                if (door.requiresKey()) {
                    gc.setFill(Color.YELLOW);
                    gc.fillOval(
                        door.getX() + door.getWidth() / 2 - 7,
                        door.getY() + door.getHeight() / 2 - 7,
                        14, 14
                    );
                    gc.setStroke(Color.BLACK);
                    gc.setLineWidth(1);
                    gc.strokeOval(
                        door.getX() + door.getWidth() / 2 - 7,
                        door.getY() + door.getHeight() / 2 - 7,
                        14, 14
                    );
                }
                
                // Display the connected room type
                if (isDebugMode) {
                    String roomTypeText = door.getConnectedRoom().getType().toString();
                    gc.setFill(Color.WHITE);
                    gc.setFont(Font.font("Verdana", FontWeight.NORMAL, 10));
                    gc.fillText(roomTypeText, 
                        door.getX() + door.getWidth() / 2 - 20,
                        door.getY() + door.getHeight() + 15);
                }
            }
        }
        
        // Draw room items
        for (Item item : roomItems) {
            // Draw item as a circle with color based on type
            gc.setFill(getItemColor(item.getType()));
            gc.fillOval(item.getX() - item.getSize()/2, item.getY() - item.getSize()/2, 
                        item.getSize(), item.getSize());
            
            // Draw item outline
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(1);
            gc.strokeOval(item.getX() - item.getSize()/2, item.getY() - item.getSize()/2, 
                        item.getSize(), item.getSize());
            
            // Draw item name for debug or when player is close
            if (isDebugMode || (player != null && 
                player.getPosition().distance(new Point2D(item.getX(), item.getY())) < 100)) {
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("Verdana", FontWeight.NORMAL, 10));
                gc.fillText(item.getName(), item.getX() - 20, item.getY() - 10);
            }
        }
        
        // Draw enemies
        if (enemies.isEmpty()) {
            System.out.println("No enemies to render in current room");
        } else {
            System.out.println("Rendering " + enemies.size() + " enemies");
        for (Enemy enemy : enemies) {
                System.out.println("Rendering enemy at: " + enemy.getX() + "," + enemy.getY() + " of type: " + enemy.getType());
                // Draw enemy as a red rectangle
                if (enemy.getType() == Enemy.EnemyType.BOSS) {
                    gc.setFill(Color.DARKRED);
                    // Draw boss larger
                    gc.fillRect(enemy.getX() - 5, enemy.getY() - 5, enemy.getSize() + 10, enemy.getSize() + 10);
                } else {
                    gc.setFill(Color.RED);
                    gc.fillRect(enemy.getX(), enemy.getY(), enemy.getSize(), enemy.getSize());
                }
                
                // Draw enemy health bar
                double healthPercentage = (double) enemy.getHealth() / enemy.getMaxHealth();
                double healthBarWidth = enemy.getSize() * healthPercentage;
                
                gc.setFill(Color.BLACK);
                gc.fillRect(enemy.getX(), enemy.getY() - 8, enemy.getSize(), 5);
                gc.setFill(Color.GREEN);
                gc.fillRect(enemy.getX(), enemy.getY() - 8, healthBarWidth, 5);
                
                // Draw enemy type if debug mode is on
                if (isDebugMode) {
                    gc.setFill(Color.WHITE);
                    gc.setFont(Font.font("Verdana", FontWeight.NORMAL, 10));
                    gc.fillText(enemy.getType().toString(), enemy.getX(), enemy.getY() - 12);
                }
            }
        }
        
        // Draw player projectiles
        for (ProjectileAttack attack : playerProjectiles) {
            attack.render(gc);
        }
        
        // Draw projectiles
        for (Projectile projectile : projectiles) {
            projectile.render(gc);
        }
        
        // Draw the player
        if (player != null) {
            player.render(gc);
        }
        
        // Draw UI elements
        renderUI(gc);
    }
    
    private void drawPauseButton(GraphicsContext gc) {
        // Draw pause button in top-right corner
        double buttonSize = 30;
        double padding = 10;
        double x = gameCanvas.getWidth() - buttonSize - padding;
        double y = padding;
        
        // Draw button background
        gc.setFill(Color.DARKGRAY);
        gc.fillRoundRect(x, y, buttonSize, buttonSize, 5, 5);
        
        // Draw pause icon
        gc.setFill(Color.WHITE);
        gc.fillRect(x + buttonSize * 0.3, y + buttonSize * 0.25, buttonSize * 0.15, buttonSize * 0.5);
        gc.fillRect(x + buttonSize * 0.55, y + buttonSize * 0.25, buttonSize * 0.15, buttonSize * 0.5);
        
        // Draw button border
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);
        gc.strokeRoundRect(x, y, buttonSize, buttonSize, 5, 5);
    }
    
    private void drawRoomBackground(GraphicsContext gc) {
        if (currentRoom == null) {
            System.err.println("ERROR: Cannot draw room background - currentRoom is null");
            gc.setFill(Color.BLACK);
            gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());
            return;
        }
        
        System.out.println("Drawing background for room type: " + currentRoom.getType());
        
        // Set the base background color based on room type
        Color baseColor;
        switch (currentRoom.getType()) {
            case SPAWN:
                baseColor = Color.rgb(0, 80, 0); // Dark green for spawn
                break;
            case COMBAT:
                baseColor = Color.rgb(70, 30, 30); // Dark red for combat
                break;
            case PUZZLE:
                baseColor = Color.rgb(30, 30, 80); // Dark blue for puzzle
                break;
            case TREASURE:
                baseColor = Color.rgb(80, 80, 30); // Gold for treasure
                break;
            case BOSS:
                baseColor = Color.rgb(80, 0, 0); // Deep red for boss
                break;
            default:
                baseColor = Color.rgb(30, 30, 30); // Dark gray default
                break;
        }
        
        // Fill the entire canvas with the base color
        gc.setFill(baseColor);
        gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());
        
        // Draw floor tiles based on room type
        double tileSize = 40;
        
        switch (currentRoom.getType()) {
            case COMBAT:
                drawCombatRoomFloor(gc, tileSize);
                break;
            case PUZZLE:
                drawPuzzleRoomFloor(gc, tileSize);
                break;
            case TREASURE:
                drawTreasureRoomFloor(gc, tileSize);
                break;
            case BOSS:
                drawBossRoomFloor(gc, tileSize);
                break;
            case SPAWN:
            default:
                drawSpawnRoomFloor(gc, tileSize);
                break;
        }
        
        // Draw room borders
        gc.setStroke(Color.DARKGRAY);
        gc.setLineWidth(5);
        gc.strokeRect(10, 10, gameCanvas.getWidth() - 20, gameCanvas.getHeight() - 20);
    }
    
    private void drawCombatRoomFloor(GraphicsContext gc, double tileSize) {
        // Checkered pattern in dark red
        for (int x = 0; x < gameCanvas.getWidth(); x += tileSize) {
            for (int y = 0; y < gameCanvas.getHeight(); y += tileSize) {
                if ((x / tileSize + y / tileSize) % 2 == 0) {
                    gc.setFill(Color.rgb(60, 20, 20));
                } else {
                    gc.setFill(Color.rgb(40, 10, 10));
                }
                gc.fillRect(x, y, tileSize, tileSize);
            }
        }
        
        // Add some blood stains
        gc.setFill(Color.rgb(120, 0, 0, 0.3));
        for (int i = 0; i < 10; i++) {
            double x = random.nextDouble() * gameCanvas.getWidth();
            double y = random.nextDouble() * gameCanvas.getHeight();
            double size = 20 + random.nextDouble() * 30;
            gc.fillOval(x, y, size, size);
        }
    }
    
    private void drawPuzzleRoomFloor(GraphicsContext gc, double tileSize) {
        // Blue tiled pattern with symbols
        for (int x = 0; x < gameCanvas.getWidth(); x += tileSize) {
            for (int y = 0; y < gameCanvas.getHeight(); y += tileSize) {
                gc.setFill(Color.rgb(20, 30, 50));
                gc.fillRect(x, y, tileSize, tileSize);
                
                // Add tile borders
                gc.setStroke(Color.rgb(30, 40, 70));
                gc.strokeRect(x, y, tileSize, tileSize);
            }
        }
        
        // Add arcane symbols
        gc.setFill(Color.rgb(100, 150, 255, 0.2));
        for (int i = 0; i < 8; i++) {
            double x = random.nextDouble() * gameCanvas.getWidth();
            double y = random.nextDouble() * gameCanvas.getHeight();
            double size = 30 + random.nextDouble() * 40;
            
            // Draw a random arcane symbol (simplified)
            double angle = random.nextDouble() * Math.PI * 2;
            double[] xPoints = new double[5];
            double[] yPoints = new double[5];
            
            for (int j = 0; j < 5; j++) {
                xPoints[j] = x + Math.cos(angle + j * Math.PI * 2 / 5) * size / 2;
                yPoints[j] = y + Math.sin(angle + j * Math.PI * 2 / 5) * size / 2;
            }
            
            gc.fillPolygon(xPoints, yPoints, 5);
        }
    }
    
    private void drawTreasureRoomFloor(GraphicsContext gc, double tileSize) {
        // Gold/yellow tiled pattern
        for (int x = 0; x < gameCanvas.getWidth(); x += tileSize) {
            for (int y = 0; y < gameCanvas.getHeight(); y += tileSize) {
                if ((x / tileSize + y / tileSize) % 2 == 0) {
                    gc.setFill(Color.rgb(80, 70, 20));
                } else {
                    gc.setFill(Color.rgb(60, 50, 10));
                }
                gc.fillRect(x, y, tileSize, tileSize);
            }
        }
        
        // Add gold coins scattered around
        gc.setFill(Color.GOLD);
        for (int i = 0; i < 30; i++) {
            double x = random.nextDouble() * gameCanvas.getWidth();
            double y = random.nextDouble() * gameCanvas.getHeight();
            double size = 3 + random.nextDouble() * 5;
            gc.fillOval(x, y, size, size);
        }
    }
    
    private void drawBossRoomFloor(GraphicsContext gc, double tileSize) {
        // Dark pattern with lava cracks
        for (int x = 0; x < gameCanvas.getWidth(); x += tileSize) {
            for (int y = 0; y < gameCanvas.getHeight(); y += tileSize) {
                gc.setFill(Color.rgb(30, 10, 10));
                gc.fillRect(x, y, tileSize, tileSize);
            }
        }
        
        // Add lava cracks
        gc.setFill(Color.rgb(255, 50, 0, 0.7));
        for (int i = 0; i < 15; i++) {
            double x = random.nextDouble() * gameCanvas.getWidth();
            double y = random.nextDouble() * gameCanvas.getHeight();
            double width = 5 + random.nextDouble() * 100;
            double height = 3 + random.nextDouble() * 5;
            double angle = random.nextDouble() * Math.PI;
            
            gc.save();
            gc.translate(x, y);
            gc.rotate(Math.toDegrees(angle));
            gc.fillRect(-width/2, -height/2, width, height);
            gc.restore();
        }
    }
    
    private void drawSpawnRoomFloor(GraphicsContext gc, double tileSize) {
        // Green/brown natural pattern
        for (int x = 0; x < gameCanvas.getWidth(); x += tileSize) {
            for (int y = 0; y < gameCanvas.getHeight(); y += tileSize) {
                if ((x / tileSize + y / tileSize) % 2 == 0) {
                    gc.setFill(Color.rgb(30, 50, 30));
                } else {
                    gc.setFill(Color.rgb(40, 60, 40));
                }
                gc.fillRect(x, y, tileSize, tileSize);
            }
        }
    }
    
    private void updateLighting() {
        if (lightingEffect == null) {
            return;
        }
        
        // Clear previous lights
        lightingEffect.clearLights();
        
        // Set ambient light based on room type
        switch (currentRoom.getType()) {
            case COMBAT:
                lightingEffect.setAmbientLight(0.3); // Dim red lighting
                break;
                
            case PUZZLE:
                lightingEffect.setAmbientLight(0.4); // Moderate blue lighting
                break;
                
            case TREASURE:
                lightingEffect.setAmbientLight(0.5); // Brighter gold lighting
                break;
                
            case BOSS:
                lightingEffect.setAmbientLight(0.2); // Very dark with red tint
                break;
                
            case SPAWN:
            default:
                lightingEffect.setAmbientLight(0.6); // Bright natural lighting
                break;
        }
        
        // Add player light
        lightingEffect.addLightSource(
            player.getPosition(), 
            150, 
            Color.WHITE, 
            LightingEffect.LightSource.LightType.FLICKERING
        );
        
        // Add door lights
        for (Door door : doors) {
            Color lightColor = door.isLocked() ? Color.RED : Color.GREEN;
            lightingEffect.addLightSource(
                new Point2D(door.getX() + door.getWidth()/2, door.getY() + door.getHeight()/2),
                60,
                lightColor,
                LightingEffect.LightSource.LightType.PULSING
            );
        }
        
        // Add item lights
        for (Item item : roomItems) {
            Color itemColor = getItemColor(item.getType());
            lightingEffect.addLightSource(
                new Point2D(item.getX(), item.getY()),
                item.getSize() * 3,
                itemColor,
                LightingEffect.LightSource.LightType.PULSING
            );
        }
        
        // Add enemy lights
        for (Enemy enemy : enemies) {
            Color enemyColor = Color.RED;
            if (enemy.getType() == Enemy.EnemyType.BOSS) {
                enemyColor = Color.DARKRED;
            } else if (enemy.getType() == Enemy.EnemyType.MAGE) {
                enemyColor = Color.BLUE;
            }
            
            lightingEffect.addLightSource(
                enemy.getPosition(),
                enemy.getSize() * 2,
                enemyColor,
                enemy.getType() == Enemy.EnemyType.BOSS ? 
                    LightingEffect.LightSource.LightType.PULSING : 
                    LightingEffect.LightSource.LightType.FLICKERING
            );
        }
        
        // Add environmental lights based on room type
        if (currentRoom.getType() == DungeonRoom.RoomType.BOSS) {
            // Add lava lights for boss room
            for (int i = 0; i < 5; i++) {
                double x = random.nextDouble() * gameCanvas.getWidth();
                double y = random.nextDouble() * gameCanvas.getHeight();
                lightingEffect.addLightSource(
                    new Point2D(x, y),
                    30 + random.nextDouble() * 50,
                    Color.ORANGE,
                    LightingEffect.LightSource.LightType.FLICKERING
                );
            }
        } else if (currentRoom.getType() == DungeonRoom.RoomType.PUZZLE) {
            // Add arcane lights for puzzle room
            for (int i = 0; i < 4; i++) {
                double x = 100 + i * (gameCanvas.getWidth() - 200) / 3;
                double y = gameCanvas.getHeight() / 2;
                lightingEffect.addLightSource(
                    new Point2D(x, y),
                    70,
                    Color.CYAN,
                    LightingEffect.LightSource.LightType.PULSING
                );
            }
        }
    }
    
    private void renderUI(GraphicsContext gc) {
        // Health bar background
        gc.setFill(Color.DARKRED);
        gc.fillRect(10, 30, 100, 10);
        
        // Health bar foreground
        gc.setFill(Color.RED);
        gc.fillRect(10, 30, 100 * (player.getHealth() / player.getMaxHealth()), 10);
        
        // Health text
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 12));
        gc.fillText("Health: " + (int)player.getHealth() + "/" + (int)player.getMaxHealth(), 120, 40);
        
        // Score text
        gc.fillText("Score: " + player.getScore(), 10, 60);
        
        // Enemies defeated
        gc.fillText("Enemies Defeated: " + enemiesDefeated, 10, 80);
        
        // Current level
        gc.fillText("Level: " + currentLevel, 10, 100);
        
        // Calculate time elapsed
        long currentTime = System.currentTimeMillis();
        long timeElapsed = currentTime - startTime;
        long seconds = (timeElapsed / 1000) % 60;
        long minutes = (timeElapsed / (1000 * 60)) % 60;
        
        // Time text
        gc.fillText(String.format("Time: %02d:%02d", minutes, seconds), 10, 120);
        
        // Debug info
        gc.setFont(Font.font("Monospace", 10));
        gc.setFill(Color.YELLOW);
        gc.fillText("Room Type: " + currentRoom.getType(), 10, 140);
        gc.fillText("Enemies: " + enemies.size(), 10, 155);
        gc.fillText("Items: " + roomItems.size(), 10, 170);
        gc.fillText("Doors: " + doors.size(), 10, 185);
        gc.fillText("Position: " + (int)player.getX() + "," + (int)player.getY(), 10, 200);
        
        // Show connected rooms
        int y = 215;
        gc.fillText("Connected Rooms:", 10, y);
        y += 15;
        for (DungeonRoom room : currentRoom.getConnectedRooms()) {
            gc.fillText(" - " + room.getType() + " at (" + room.getX() + "," + room.getY() + ")", 10, y);
            y += 15;
        }
        
        // Controls help
        gc.setFill(Color.LIGHTBLUE);
        gc.fillText("WASD: Move | E: Shoot | Space: Melee | F: Interact | I: Inventory", 10, gameCanvas.getHeight() - 20);
    }

    public Player getPlayer() {
        return player;
    }

    private Color getItemColor(Item.ItemType type) {
        switch (type) {
            case WEAPON: return Color.RED;
            case POTION: return Color.GREEN;
            case ARMOR: return Color.BLUE;
            default: return Color.WHITE;
        }
    }

    private void createDoors() {
        // First remove existing doors
        doors.clear();
        
        if (currentRoom == null) {
            System.err.println("ERROR: Cannot create doors for null room");
            return;
        }
        
        System.out.println("Creating doors for room " + currentRoom.getType() + 
                          " at (" + currentRoom.getX() + "," + currentRoom.getY() + ")");
        
        List<DungeonRoom> connectedRooms = currentRoom.getConnectedRooms();
        System.out.println("Connected rooms: " + connectedRooms.size());
        
        if (connectedRooms.isEmpty()) {
            System.err.println("WARNING: Room has no connected rooms!");
            return;
        }
        
        // Determine room bounds on the canvas
        double roomWidth = gameCanvas.getWidth() - 100;  // 50px margin on each side
        double roomHeight = gameCanvas.getHeight() - 100;
        double doorWidth = DOOR_WIDTH;
        double doorHeight = DOOR_HEIGHT;
        
        // Create doors for each connected room
        for (DungeonRoom connectedRoom : connectedRooms) {
            if (connectedRoom == null) {
                System.err.println("WARNING: Connected room is null!");
                continue;
            }
            
            // Determine direction to the connected room
            int dx = connectedRoom.getX() - currentRoom.getX();
            int dy = connectedRoom.getY() - currentRoom.getY();
            
            System.out.println("Adding door to " + connectedRoom.getType() + 
                              " at direction (" + dx + "," + dy + ")");
            
            // Calculate door position based on direction
            double doorX, doorY;
            Door.DoorDirection doorDirection;
            
            if (dx > 0) {      // East door
                doorX = roomWidth - doorWidth + 50;
                doorY = (roomHeight / 2) - (doorHeight / 2) + 50;
                doorDirection = Door.DoorDirection.EAST;
            } else if (dx < 0) { // West door
                doorX = 50;  // Left margin
                doorY = (roomHeight / 2) - (doorHeight / 2) + 50;
                doorDirection = Door.DoorDirection.WEST;
            } else if (dy > 0) { // South door
                doorX = (roomWidth / 2) - (doorWidth / 2) + 50;
                doorY = roomHeight - doorHeight + 50;
                doorDirection = Door.DoorDirection.SOUTH;
            } else {            // North door
                doorX = (roomWidth / 2) - (doorWidth / 2) + 50;
                doorY = 50;  // Top margin
                doorDirection = Door.DoorDirection.NORTH;
            }
            
            // Create the door
            Door door = new Door(
                doorX, doorY, doorWidth, doorHeight,
                currentRoom, connectedRoom, doorDirection
            );
            
            // Lock doors based on room type
            if (currentRoom.getType() == DungeonRoom.RoomType.COMBAT ||
                currentRoom.getType() == DungeonRoom.RoomType.BOSS) {
                // Combat rooms: doors are locked until enemies are defeated
                door.setLocked(!enemies.isEmpty());
                System.out.println("Combat/Boss room door locked state: " + door.isLocked() + 
                                 " (enemies: " + enemies.size() + ")");
            } else if (currentRoom.getType() == DungeonRoom.RoomType.PUZZLE) {
                // Puzzle rooms: doors are locked until puzzle is solved
                door.setLocked(!puzzleCompleted);
                door.setRequiresKey(true);
                System.out.println("Puzzle room door locked state: " + door.isLocked() + 
                                 " (puzzle completed: " + puzzleCompleted + ")");
            }
            
            doors.add(door);
            System.out.println("Added door at " + doorX + "," + doorY + 
                              " to " + connectedRoom.getType() + 
                              " (locked: " + door.isLocked() + ", requires key: " + door.requiresKey() + ")");
        }
        
        // Debug output of created doors
        if (doors.isEmpty()) {
            System.err.println("WARNING: No doors were created!");
        } else {
            System.out.println("Created " + doors.size() + " doors");
        }
    }

    private void populateRoom(DungeonRoom room) {
        // Clear existing entities
        enemies.clear();
        roomItems.clear();
        roomCleared = false;
        
        System.out.println("Populating room of type: " + room.getType() + " at position " + room.getX() + "," + room.getY());
        
        // Get room center
        double centerX = gameCanvas.getWidth() / 2;
        double centerY = gameCanvas.getHeight() / 2;
        Point2D roomCenter = new Point2D(centerX, centerY);
        
        switch (room.getType()) {
            case COMBAT:
            // Add 2-4 enemies in combat rooms
            int enemyCount = 2 + new Random().nextInt(3);
                System.out.println("Spawning " + enemyCount + " enemies in combat room");
                
            for (int i = 0; i < enemyCount; i++) {
                    // Calculate random position within the room, keeping away from the center
                    Random rnd = new Random();
                    double angle = rnd.nextDouble() * Math.PI * 2; // Random angle
                    double distance = 100 + rnd.nextDouble() * 150; // Distance from center
                    
                    double offsetX = Math.cos(angle) * distance;
                    double offsetY = Math.sin(angle) * distance;
                    
                    double enemyX = centerX + offsetX;
                    double enemyY = centerY + offsetY;
                    
                    // Keep enemies within room bounds
                    enemyX = Math.max(50, Math.min(gameCanvas.getWidth() - 50, enemyX));
                    enemyY = Math.max(50, Math.min(gameCanvas.getHeight() - 50, enemyY));
                    
                    // Create and add enemy
                    Enemy.EnemyType type = rnd.nextBoolean() ? Enemy.EnemyType.GOBLIN : Enemy.EnemyType.SKELETON;
                    Enemy enemy = new Enemy(enemyX, enemyY, type);
                    enemies.add(enemy);
                    
                    System.out.println("Added enemy at " + enemyX + "," + enemyY + " of type: " + type);
                }
                
                // Add a health potion
                if (new Random().nextDouble() < 0.5) {
                    spawnBasicItem(roomCenter.add(50, 50));
                    System.out.println("Added health potion to combat room");
                }
                break;
                
            case BOSS:
            // Add boss enemy
                System.out.println("Spawning boss in boss room");
                Enemy bossEnemy = new Enemy(centerX, centerY, Enemy.EnemyType.BOSS);
                // Double the boss's max health
                bossEnemy.setMaxHealth(bossEnemy.getMaxHealth() * 2);
                bossEnemy.heal(bossEnemy.getMaxHealth()); // Heal to full health
            enemies.add(bossEnemy);
                break;
                
            case TREASURE:
            // Add random items
                System.out.println("Spawning items in treasure room");
                Random rnd = new Random();
                int itemCount = 2 + rnd.nextInt(3); // 2-4 items
                
                for (int i = 0; i < itemCount; i++) {
                    double angle = rnd.nextDouble() * Math.PI * 2; // Random angle
                    double distance = 50 + rnd.nextDouble() * 100; // Distance from center
                    
                    double offsetX = Math.cos(angle) * distance;
                    double offsetY = Math.sin(angle) * distance;
                    
                    spawnBasicItem(roomCenter.add(offsetX, offsetY));
                }
                break;
                
            case PUZZLE:
                // Add a key
                System.out.println("Adding key to puzzle room");
                placeKeyInRoom(room);
                break;
                
            case SPAWN:
            default:
                // No special spawns in spawn room
                break;
        }
        
        // Create doors for the new room
        createDoors();
    }

    private void spawnItems(DungeonRoom room) {
        Random random = new Random();
        int itemCount = 0;
        
        // Determine number of items based on room type
        switch (room.getType()) {
            case COMBAT:
                itemCount = random.nextInt(2); // 0-1 items
                break;
            case PUZZLE:
                itemCount = 1; // Always spawn a key
                // Add a key to unlock doors
                placeKeyInRoom(room);
                break;
            case TREASURE:
                itemCount = random.nextInt(3) + 2; // 2-4 items
                break;
            case BOSS:
                itemCount = random.nextInt(2) + 1; // 1-2 special items
                break;
            case SPAWN:
                // No items in spawn room
                return;
        }
        
        // Add random items
        for (int i = 0; i < itemCount; i++) {
            addItemToRoom(room);
        }
    }
    
    private void placeKeyInRoom(DungeonRoom room) {
        // Get the center of the room on the canvas
        double centerX = gameCanvas.getWidth() / 2;
        double centerY = gameCanvas.getHeight() / 2;
        
        // Add some randomness to key position
        Random random = new Random();
        double keyX = centerX + (random.nextDouble() - 0.5) * 100;
        double keyY = centerY + (random.nextDouble() - 0.5) * 100;
        
        // Create and add the key
        Item roomKey = new Item("Room Key", "Opens locked doors", Item.ItemType.KEY, 1, true);
        roomKey.setX(keyX);
        roomKey.setY(keyY);
        roomKey.setSize(20);
        
        System.out.println("Added key at position: " + keyX + "," + keyY);
        roomItems.add(roomKey);
    }
    
    private void spawnBasicItem(Point2D position) {
        Random random = new Random();
        
        // Randomize item type
        Item.ItemType type;
        String name;
        String description;
        int value;
        boolean consumable;
        
        double rnd = random.nextDouble();
        if (rnd < 0.6) {
            // 60% chance for a potion
            type = Item.ItemType.POTION;
            name = "Health Potion";
            description = "Restores 20 health";
            value = 20;
            consumable = true;
        } else if (rnd < 0.8) {
            // 20% chance for a weapon
            type = Item.ItemType.WEAPON;
            name = "Sword";
            description = "A sharp weapon";
            value = 15;
            consumable = false;
        } else {
            // 20% chance for treasure
            type = Item.ItemType.TREASURE;
            name = "Gold Coins";
            description = "Valuable treasure";
            value = 50;
            consumable = true;
        }
        
        // Create the item
        Item item = new Item(name, description, type, value, consumable);
        item.setX(position.getX());
        item.setY(position.getY());
        item.setSize(20);
        
        System.out.println("Added item: " + name + " at " + position.getX() + "," + position.getY());
        
        roomItems.add(item);
    }
    
    private void spawnEnemy(DungeonRoom room) {
        Random random = new Random();
        Point2D roomPos = new Point2D(room.getX(), room.getY());
        Point2D randomOffset = new Point2D(
            random.nextDouble() * room.getWidth(),
            random.nextDouble() * room.getHeight()
        );
        Point2D spawnPos = roomPos.add(randomOffset).multiply(8); // Scale to pixel coordinates
        
        Enemy.EnemyType type = random.nextBoolean() ? Enemy.EnemyType.GOBLIN : Enemy.EnemyType.SKELETON;
        enemies.add(new Enemy(spawnPos.getX(), spawnPos.getY(), type));
    }

    private boolean isColliding(Entity entity1, Entity entity2) {
        Point2D center1 = entity1.getPosition().add(entity1.getSize() / 2, entity1.getSize() / 2);
        Point2D center2 = entity2.getPosition().add(entity2.getSize() / 2, entity2.getSize() / 2);
        double distance = center1.distance(center2);
        double combinedRadius = (entity1.getSize() + entity2.getSize()) / 2;
        return distance < combinedRadius;
    }

    private boolean isInMeleeRange(Entity attacker, Entity target) {
        double attackRange = 40; // Melee attack range
        
        Point2D attackerCenter = attacker.getPosition().add(new Point2D(attacker.getSize() / 2, attacker.getSize() / 2));
        Point2D targetCenter = target.getPosition().add(new Point2D(target.getSize() / 2, target.getSize() / 2));
        
        return attackerCenter.distance(targetCenter) < (attacker.getSize() / 2 + target.getSize() / 2 + attackRange);
    }

    private void showFloatingText(String text, Point2D position) {
        // This would be implemented with a UI element that floats up and fades out
        // For now, we'll just print to console
        System.out.println("Floating text at " + position + ": " + text);
    }

    private void dropRandomItem(Point2D position) {
        Random random = new Random();
        if (random.nextDouble() < 0.3) { // 30% chance to drop an item
            Item.ItemType type = Item.ItemType.values()[random.nextInt(Item.ItemType.values().length)];
            String name;
            String description;
            int value;
            boolean consumable;
            
            switch (type) {
                case WEAPON:
                    name = "Sword";
                    description = "A sharp blade";
                    value = 10;
                    consumable = false;
                    break;
                case POTION:
                    name = "Health Potion";
                    description = "Restores 20 HP";
                    value = 20;
                    consumable = true;
                    break;
                case ARMOR:
                    name = "Shield";
                    description = "Provides protection";
                    value = 15;
                    consumable = false;
                    break;
                default:
                    name = "Gold Coins";
                    description = "Valuable treasure";
                    value = 50;
                    consumable = true;
                    type = Item.ItemType.TREASURE;
                    break;
            }
            
            // Create and add the item
            Item item = new Item(name, description, type, value, consumable);
            item.setX(position.getX());
            item.setY(position.getY());
            item.setSize(20);
            roomItems.add(item);
        }
    }

    private boolean isPlayerTouchingItem(Item item) {
        double playerCenterX = player.getPosition().getX() + player.getSize() / 2;
        double playerCenterY = player.getPosition().getY() + player.getSize() / 2;
        double itemCenterX = item.getX() + 5; // Assuming item size is 10x10
        double itemCenterY = item.getY() + 5;
        
        double distance = Math.sqrt(
            Math.pow(playerCenterX - itemCenterX, 2) + 
            Math.pow(playerCenterY - itemCenterY, 2)
        );
        
        return distance < (player.getSize() / 2 + 5); // Player radius + item radius
    }

    private void checkRoomTransition() {
        // Check if player is at a door to a connected room
        for (Door door : doors) {
            if (isPlayerTouchingDoor(door)) {
                System.out.println("Player touching door to " + door.getConnectedRoom().getType() + 
                                  " - Door locked: " + door.isLocked() + 
                                  ", Requires key: " + door.requiresKey());
                
                if (!door.isLocked()) {
                    // Get the connected room
                    DungeonRoom targetRoom = door.getConnectedRoom();
                    if (targetRoom != null) {
                        System.out.println("Door unlocked - transitioning to room: " + targetRoom.getType() + 
                                          " at position " + targetRoom.getX() + "," + targetRoom.getY());
                        
                        // Calculate entry position based on door direction
                        Point2D entryPosition = getEntryPosition(door);
                        
                        // Pass target room and entry position to transition method
                        transitionToRoom(targetRoom, entryPosition);
                        
                    // Update difficulty based on room type if it's a boss room
                        if (targetRoom.getType() == DungeonRoom.RoomType.BOSS) {
                        adjustDifficultyForLevel();
                    }
                        
                    break;
                    } else {
                        System.err.println("ERROR: Connected room is null!");
                    }
                } else {
                    // Show message about locked door if player is touching it
                    if (door.requiresKey()) {
                        effectsManager.showFloatingText("This door requires a key", 
                            new Point2D(door.getX() + door.getWidth()/2, door.getY() - 20), 
                            Color.YELLOW);
                        
                        // Try to unlock with key if player has one
                        if (player.getInventory().hasItem(Item.ItemType.KEY)) {
                            if (door.unlock(player.getInventory())) {
                                effectsManager.showFloatingText("Door unlocked!", 
                                    new Point2D(door.getX() + door.getWidth()/2, door.getY() - 10),
                                    Color.GREEN);
                                // Door is now unlocked, but wait for next check to transition
                            }
                        }
                    } else {
                        // Door is locked for other reasons (enemies not defeated)
                        String message = "Defeat all enemies to unlock";
                        if (currentRoom.getType() == DungeonRoom.RoomType.PUZZLE) {
                            message = "Solve the puzzle to unlock";
                        }
                        effectsManager.showFloatingText(message, 
                            new Point2D(door.getX() + door.getWidth()/2, door.getY() - 20), 
                            Color.RED);
                    }
                }
            }
        }
    }

    private void transitionToRoom(DungeonRoom room) {
        if (roomTransitionInProgress || room == null) {
            System.out.println("Cannot transition to room: " + (room == null ? "null" : room.getType()) + 
                               ", transition in progress: " + roomTransitionInProgress);
            return;
        }
        
        System.out.println("Starting transition to " + room.getType() + " room at position " + room.getX() + "," + room.getY());
        roomTransitionInProgress = true;
        
        // Create a fade transition effect
        Rectangle fadeRect = new Rectangle(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());
        fadeRect.setFill(Color.BLACK);
        fadeRect.setOpacity(0);
        rootPane.getChildren().add(fadeRect);
        
        // Create fade out transition
        javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(Duration.seconds(TRANSITION_DURATION / 2), fadeRect);
        fadeOut.setFromValue(0);
        fadeOut.setToValue(1);
        
        // Create fade in transition
        javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(Duration.seconds(TRANSITION_DURATION / 2), fadeRect);
        fadeIn.setFromValue(1);
        fadeIn.setToValue(0);
        
        // Add visual effects for room transition
        effectsManager.addRoomTransitionEffect();
        
        // Set up the sequence
        fadeOut.setOnFinished(e -> {
            // Change room
            currentRoom = room;
            currentRoom.setVisited(true);
            
            // Reset player position to center of new room
            double roomCenterX = currentRoom.getX() + currentRoom.getWidth() / 2 - player.getSize() / 2;
            double roomCenterY = currentRoom.getY() + currentRoom.getHeight() / 2 - player.getSize() / 2;
            player.setPosition(new Point2D(roomCenterX, roomCenterY));
            
            // Clear enemies and items from previous room
            enemies.clear();
            projectiles.clear();
            roomItems.clear();
            doors.clear();
            
            // Populate new room
            populateRoom(currentRoom);
            
            // Create doors for the new room
            createDoors();
            
            // Update minimap
            updateMinimap();
        });
        
        fadeIn.setOnFinished(e -> {
            // Remove the fade rectangle
            rootPane.getChildren().remove(fadeRect);
            roomTransitionInProgress = false;
            
            // Display room entrance message
            String roomTypeText = "";
            switch (currentRoom.getType()) {
                case COMBAT:
                    roomTypeText = "Combat Room";
                    break;
                case PUZZLE:
                    roomTypeText = "Puzzle Room";
                    break;
                case TREASURE:
                    roomTypeText = "Treasure Room";
                    break;
                case BOSS:
                    roomTypeText = "Boss Room";
                    break;
                case SPAWN:
                    roomTypeText = "Starting Room";
                    break;
            }
            
            // Show floating text for room type
            showFloatingText("Entered: " + roomTypeText, 
                new Point2D(gameCanvas.getWidth() / 2, gameCanvas.getHeight() / 4));
                
            // Initialize room-specific lighting
            updateLighting();
        });
        
        // Start the transition
        fadeOut.play();
    }
    
    private void handleDoorInteraction() {
        for (Door door : doors) {
            if (door.contains(new Point2D(player.getX(), player.getY()))) {
                // Check if door is locked
                if (door.isLocked()) {
                    if (door.requiresKey()) {
                        // Try to unlock door with key
                        if (door.unlock(player.getInventory())) {
                            // Show unlock effect
                            effectsManager.showFloatingText("Door Unlocked!", 
                                new Point2D(door.getX() + door.getWidth() / 2, door.getY() - 10),
                                Color.YELLOW, 20);
                            
                            // Add particles for unlocking effect
                            for (int i = 0; i < 15; i++) {
                                effectsManager.addParticle(
                                    new Point2D(door.getX() + door.getWidth() / 2, door.getY() + door.getHeight() / 2),
                                    Color.YELLOW,
                                    1.0
                                );
                            }
                        } else {
                            // Show message that key is required
                            effectsManager.showFloatingText("Key Required!", 
                                new Point2D(door.getX() + door.getWidth() / 2, door.getY() - 10),
                                Color.RED, 20);
                        }
                    } else {
                        // Show message about room not being cleared
                        effectsManager.showFloatingText("Clear the room first!", 
                            new Point2D(door.getX() + door.getWidth() / 2, door.getY() - 10),
                            Color.RED, 20);
                    }
                } else {
                    // Door is unlocked, transition to connected room
                    DungeonRoom connectedRoom = door.getConnectedRoom();
                    Point2D entryPosition = getEntryPosition(door);
                    transitionToRoom(connectedRoom, entryPosition);
                }
                break; // Only interact with one door at a time
            }
        }
    }
    
    private Point2D getEntryPosition(Door door) {
        // Calculate entry position based on door location
        double entryX, entryY;
        double offset = 50; // Distance from door to place player
        
        // Determine direction based on door position
        if (door.getY() < 50) { // North door
            entryX = door.getX() + door.getWidth() / 2;
            entryY = door.getY() + offset;
        } else if (door.getY() > gameCanvas.getHeight() - 50) { // South door
            entryX = door.getX() + door.getWidth() / 2;
            entryY = door.getY() - offset;
        } else if (door.getX() < 50) { // West door
            entryX = door.getX() + offset;
            entryY = door.getY() + door.getHeight() / 2;
        } else { // East door
            entryX = door.getX() - offset;
            entryY = door.getY() + door.getHeight() / 2;
        }
        
        return new Point2D(entryX, entryY);
    }
    
    private void transitionToRoom(DungeonRoom newRoom, Point2D entryPosition) {
        if (newRoom == null) return;
        
        // Start room transition effect
        effectsManager.startRoomTransition(newRoom.getType(), () -> {
            // This runs after the transition effect completes
            currentRoom = newRoom;
            currentRoom.setVisited(true);
            
            // Set player position
            player.setPosition(entryPosition.getX(), entryPosition.getY());
            
            // Clear existing room data
            enemies.clear();
            projectiles.clear();
            roomItems.clear();
            doors.clear();
            
            // Populate new room
            populateRoom(currentRoom);
            
            // Create doors for the new room
            createDoors();
            
            // Update minimap
            updateMinimap();
        });
    }
    
    private boolean isRoomCleared() {
        if (currentRoom.getType() == DungeonRoom.RoomType.COMBAT) {
            return enemies.isEmpty();
        } else if (currentRoom.getType() == DungeonRoom.RoomType.BOSS) {
            // Check for boss death
            return enemies.isEmpty();
        } else if (currentRoom.getType() == DungeonRoom.RoomType.PUZZLE) {
            // This would depend on puzzle completion logic
            return puzzleCompleted;
        }
        
        // Other room types don't need clearing
        return true;
    }
    
    private void updateDoorLockStates() {
        boolean cleared = isRoomCleared();
        
        for (Door door : doors) {
            // Only update doors that don't require keys
            if (!door.requiresKey()) {
                door.setLocked(!cleared);
            }
        }
    }
    
    private void handleItemPickup() {
        Iterator<Item> iterator = roomItems.iterator();
        while (iterator.hasNext()) {
            Item item = iterator.next();
            
            // Check if player collides with item
            if (item.checkPlayerCollision(player)) {
                // Add to player's inventory
                player.addItem(item);
                
                // Show pickup effect
                effectsManager.addPickupEffect(
                    new Point2D(item.getX(), item.getY()),
                    item.getName()
                );
                
                // Remove item from room
                iterator.remove();
            }
        }
    }
    
    private Point2D getRandomRoomPosition() {
        Random random = new Random();
        
        // Add padding to keep items away from edges
        double padding = 100;
        double x = padding + random.nextDouble() * (gameCanvas.getWidth() - padding * 2);
        double y = padding + random.nextDouble() * (gameCanvas.getHeight() - padding * 2);
        
        return new Point2D(x, y);
    }

    private void addItemToRoom(DungeonRoom room) {
        if (room == null) return;
        
        // Get the center position of the room
        Point2D roomCenter = room.getCenter();
        
        // Add some randomness to the spawn position
        Random random = new Random();
        double offsetX = (random.nextDouble() - 0.5) * 100;
        double offsetY = (random.nextDouble() - 0.5) * 100;
        Point2D spawnPos = roomCenter.add(offsetX, offsetY);
        
        // Randomize item type based on room type
            Item.ItemType type;
        String name;
        String description;
        int value;
        boolean consumable;
        
        if (room.getType() == DungeonRoom.RoomType.TREASURE) {
            // Treasure rooms have better loot
            if (random.nextDouble() < 0.4) {
                type = Item.ItemType.POTION;
                name = "Health Potion";
                description = "Restores 20 health";
                value = 20;
                consumable = true;
            } else {
                type = Item.ItemType.TREASURE;
                name = "Gold Coins";
                description = "Valuable treasure";
                value = 100;
                consumable = true;
            }
        } else {
            // Other rooms have basic loot
            if (random.nextDouble() < 0.7) {
                type = Item.ItemType.POTION;
                name = "Health Potion";
                description = "Restores 20 health";
                value = 20;
                consumable = true;
            } else {
                type = Item.ItemType.TREASURE;
                name = "Gold Coins";
                description = "Valuable treasure";
                value = 30;
                consumable = true;
            }
        }
        
        // Create the item
        Item item = new Item(name, description, type, value, consumable);
        item.setX(spawnPos.getX());
        item.setY(spawnPos.getY());
        item.setSize(20);
        roomItems.add(item);
    }

    private void updateEnemies(double deltaTime) {
        // Update each enemy in the room
        Iterator<Enemy> enemyIterator = enemies.iterator();
        while (enemyIterator.hasNext()) {
            Enemy enemy = enemyIterator.next();
            
            // Update enemy state
            enemy.update(deltaTime);
            
            // Skip if enemy is dead
            if (enemy.getHealth() <= 0) {
                // Mark enemy as defeated and remove from list
                onEnemyDefeated(enemy);
                enemyIterator.remove();
                continue;
            }
            
            // Get distance to player
            Point2D playerCenter = player.getPosition().add(player.getSize() / 2, player.getSize() / 2);
            Point2D enemyCenter = enemy.getPosition().add(enemy.getSize() / 2, enemy.getSize() / 2);
            double distanceToPlayer = playerCenter.distance(enemyCenter);
            
            // Move enemy toward player if not too close
            if (distanceToPlayer > enemy.getSize() + player.getSize() + 5) {
                Point2D direction = playerCenter.subtract(enemyCenter).normalize();
                double dx = direction.getX() * enemy.getSpeed() * deltaTime;
                double dy = direction.getY() * enemy.getSpeed() * deltaTime;
                
                // Move enemy
                Point2D newPosition = new Point2D(
                    enemy.getPosition().getX() + dx,
                    enemy.getPosition().getY() + dy
                );
                enemy.setPosition(newPosition);
            }
            
            // Enemy attack logic - check if close enough to player
            if (distanceToPlayer < enemy.getSize() + player.getSize() + 20) {
                // Melee attack
                player.takeDamage(enemy.getDamage());
                effectsManager.showFloatingText("-" + (int)enemy.getDamage(), playerCenter, Color.RED);
            }
        }
        
        // Check if room is cleared
        if (currentRoom.getType() == DungeonRoom.RoomType.COMBAT && enemies.isEmpty() && !roomCleared) {
            roomCleared = true;
            // Unlock doors when the room is cleared
            for (Door door : doors) {
                if (!door.requiresKey()) {
                    door.setLocked(false);
                }
            }
        }
}

private void updateProjectiles(double deltaTime) {
        Iterator<Projectile> iterator = projectiles.iterator();
        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();
            projectile.update(deltaTime);
            
            // Check if projectile has expired
            if (projectile.isExpired()) {
                iterator.remove();
                continue;
            }
            
            // Check for collisions with entities
            Point2D projectilePos = projectile.getPosition();
            double projectileSize = projectile.getSize();
            
            // If from player, check enemy collisions
            if (projectile.isFromPlayer()) {
                for (Enemy enemy : enemies) {
                    Point2D enemyPos = enemy.getPosition();
                    double enemySize = enemy.getSize();
                    
                    if (checkCollision(projectilePos, projectileSize, enemyPos, enemySize)) {
                        // Apply damage to enemy
                        enemy.takeDamage(projectile.getDamage());
                        
                        // Show damage text
                        effectsManager.showFloatingText("-" + (int)projectile.getDamage(), 
                            enemyPos, Color.RED);
                        
                        // Remove projectile
                        iterator.remove();
                        break;
                    }
                }
            } 
            // If from enemy, check player collision
            else if (player != null) {
                Point2D playerPos = player.getPosition();
                double playerSize = player.getSize();
                
                if (checkCollision(projectilePos, projectileSize, playerPos, playerSize)) {
                    // Apply damage to player
                    player.takeDamage(projectile.getDamage());
                    
                    // Show damage text
                    effectsManager.showFloatingText("-" + (int)projectile.getDamage(), 
                        playerPos, Color.RED);
                    
                    // Remove projectile
                    iterator.remove();
                }
            }
        }
    }

    private boolean checkCollision(Point2D pos1, double size1, Point2D pos2, double size2) {
        double dx = pos1.getX() - pos2.getX();
        double dy = pos1.getY() - pos2.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        return distance < (size1 + size2) / 2;
}

private void updateMinimap() {
        // Simplified minimap implementation without creating a separate UI component
        // Draw minimap directly on the canvas in the corner
        
        if (!gameCanvas.isVisible()) return;
        
        GraphicsContext gc = gameCanvas.getGraphicsContext2D();
        double minimapSize = 120;
        double minimapX = gameCanvas.getWidth() - minimapSize - 20;
        double minimapY = 20;
        
        // Draw minimap background
        gc.setFill(Color.rgb(0, 0, 0, 0.7));
        gc.fillRect(minimapX, minimapY, minimapSize, minimapSize);
        
        // Draw minimap border
        gc.setStroke(Color.GOLD);
        gc.setLineWidth(2);
        gc.strokeRect(minimapX, minimapY, minimapSize, minimapSize);
        
        // Find the dungeon bounds
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        
        for (DungeonRoom room : currentDungeon) {
            minX = Math.min(minX, room.getX());
            minY = Math.min(minY, room.getY());
            maxX = Math.max(maxX, room.getX());
            maxY = Math.max(maxY, room.getY());
        }
        
        // Calculate scale to fit dungeon in minimap
        int dungeonWidth = maxX - minX + 1;
        int dungeonHeight = maxY - minY + 1;
        
        double cellSize = Math.min(
            (minimapSize - 20) / dungeonWidth,
            (minimapSize - 20) / dungeonHeight
        );
        
        // Center the minimap
        double offsetX = minimapX + 10 + (minimapSize - 20 - cellSize * dungeonWidth) / 2;
        double offsetY = minimapY + 10 + (minimapSize - 20 - cellSize * dungeonHeight) / 2;
        
        // Draw rooms
        for (DungeonRoom room : currentDungeon) {
            double roomX = offsetX + (room.getX() - minX) * cellSize;
            double roomY = offsetY + (room.getY() - minY) * cellSize;
            
            // Set color based on room state
            if (room == currentRoom) {
                // Current room (player location)
                gc.setFill(Color.YELLOW);
            } else if (room.isVisited()) {
                // Visited room - color based on type
                switch (room.getType()) {
                    case COMBAT: gc.setFill(Color.RED); break;
                    case PUZZLE: gc.setFill(Color.BLUE); break;
                    case TREASURE: gc.setFill(Color.GOLD); break;
                    case BOSS: gc.setFill(Color.DARKRED); break;
                    case SPAWN: gc.setFill(Color.GREEN); break;
                    default: gc.setFill(Color.GRAY);
                }
            } else if (currentRoom.getConnectedRooms().contains(room)) {
                // Adjacent unvisited room
                gc.setFill(Color.GRAY);
            } else {
                // Unexplored room
                gc.setFill(Color.BLACK);
            }
            
            // Draw the room
            gc.fillRect(roomX, roomY, cellSize - 2, cellSize - 2);
            
            // Draw room border
            gc.setStroke(Color.DARKGRAY);
            gc.setLineWidth(1);
            gc.strokeRect(roomX, roomY, cellSize - 2, cellSize - 2);
        }
    }

public void openInventory() {
        // Display inventory contents in floating text
        if (player.getInventory().getItems().isEmpty()) {
            effectsManager.showFloatingText("Inventory is empty", 
            new Point2D(gameCanvas.getWidth()/2, gameCanvas.getHeight()/2), 
            Color.YELLOW);
            return;
        }
        
        // Show inventory contents with floating text
        effectsManager.showFloatingText("Inventory:", 
            new Point2D(gameCanvas.getWidth()/2, gameCanvas.getHeight()/3), 
            Color.YELLOW);
            
        // List items in inventory
        List<Item> inventoryItems = player.getInventory().getItems();
        int yOffset = 30;
        
        for (Item item : inventoryItems) {
            effectsManager.showFloatingText("- " + item.getName(), 
                new Point2D(gameCanvas.getWidth()/2, gameCanvas.getHeight()/3 + yOffset), 
                Color.WHITE);
            yOffset += 25;
        }
        
        System.out.println("Opened inventory with " + inventoryItems.size() + " items");
}

public void interactWithPuzzle() {
        // Only interact if we're in a puzzle room
        if (currentRoom == null || currentRoom.getType() != DungeonRoom.RoomType.PUZZLE) {
            effectsManager.showFloatingText("Not a puzzle room!", 
                new Point2D(gameCanvas.getWidth()/2, gameCanvas.getHeight()/2), 
                Color.RED);
            return;
        }
        
        // Get the puzzle for this room
        Puzzle puzzle = puzzles.get(currentRoom);
        if (puzzle == null) {
            return; // No puzzle for this room
        }
        
        if (puzzle.isSolved()) {
            effectsManager.showFloatingText("Puzzle already solved!", 
                new Point2D(gameCanvas.getWidth()/2, gameCanvas.getHeight()/2), 
                Color.GREEN);
            return;
        }
        
        // Open the puzzle UI
        try {
            // Pause the game loop while the puzzle is open
            gameLoopRunning = false;
            
            // Load the puzzle UI
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/dungeon/fxml/Puzzle.fxml"));
            Parent puzzleRoot = loader.load();
            Scene puzzleScene = new Scene(puzzleRoot);
            
            // Set up the controller
            PuzzleController controller = loader.getController();
            controller.initialize(puzzle, currentRoom, this);
            
            // Create a modal stage for the puzzle
            Stage puzzleStage = new Stage();
            puzzleStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            puzzleStage.setTitle("Solve the Puzzle");
            puzzleStage.setScene(puzzleScene);
            
            // Resume game loop when puzzle is closed
            puzzleStage.setOnCloseRequest(event -> gameLoopRunning = true);
            
            // Show the puzzle
            puzzleStage.showAndWait();
            
            // Resume game loop
            gameLoopRunning = true;
        } catch (Exception e) {
            e.printStackTrace();
            // Resume game loop in case of error
            gameLoopRunning = true;
        }
    }

    private void spawnRandomItem(Point2D position) {
        Random random = new Random();
        double offsetX = (random.nextDouble() - 0.5) * 60;
        double offsetY = (random.nextDouble() - 0.5) * 60;
        
        Point2D itemPos = position.add(offsetX, offsetY);
        
        // Determine reward item type
        Item.ItemType itemType;
        String itemName;
        String itemDescription;
        int itemValue;
        boolean consumable;
        
        if (random.nextDouble() < 0.3) {
            // 30% chance for a key
            itemType = Item.ItemType.KEY;
            itemName = "Dungeon Key";
            itemDescription = "Opens locked doors";
            itemValue = 1;
            consumable = true;
        } else if (random.nextDouble() < 0.6) {
            // 30% chance for a weapon
            itemType = Item.ItemType.WEAPON;
            String[] weapons = {"Enchanted Sword", "Magic Staff", "Hunter's Bow"};
            itemName = weapons[random.nextInt(weapons.length)];
            itemDescription = "A powerful weapon";
            itemValue = 10 + random.nextInt(10);
            consumable = false;
        } else {
            // 40% chance for a potion
            itemType = Item.ItemType.POTION;
            itemName = "Health Potion";
            itemDescription = "Restores health when consumed";
            itemValue = 25 + random.nextInt(25);
            consumable = true;
        }
        
        // Create and add the reward item
        Item rewardItem = new Item(itemName, itemDescription, itemType, itemValue, consumable);
        rewardItem.setX(itemPos.getX());
        rewardItem.setY(itemPos.getY());
        rewardItem.setSize(20);
        roomItems.add(rewardItem);
        
        // Add visual effect for the reward
        effectsManager.addExplosionEffect(itemPos, 1.0);
    }

    private void showGameOverScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/dungeon/fxml/GameOver.fxml"));
            Parent gameOverRoot = loader.load();
            Scene gameOverScene = new Scene(gameOverRoot);
            
            GameOverController controller = loader.getController();
            controller.setGameStats((int)player.getScore(), currentLevel - 1, enemiesDefeated);
            
            // Stop the game loop
            gameLoopRunning = false;
            
            // Transition to game over screen
            Stage stage = (Stage) gameCanvas.getScene().getWindow();
            stage.setScene(gameOverScene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkVictoryCondition() {
        // Check if boss is defeated in the boss room
        if (currentRoom != null && currentRoom.getType() == DungeonRoom.RoomType.BOSS && 
            bossDefeated && !victory) {
            victory = true;
            showVictoryScreen();
        }
    }

    private void showVictoryScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/dungeon/fxml/Victory.fxml"));
            Parent victoryRoot = loader.load();
            Scene victoryScene = new Scene(victoryRoot);
            
            VictoryController controller = loader.getController();
            
            // Calculate elapsed time
            long endTime = System.currentTimeMillis();
            long timeInSeconds = (endTime - startTime) / 1000;
            String timeElapsed = String.format("%02d:%02d", timeInSeconds / 60, timeInSeconds % 60);
            
            controller.setGameStats((int)player.getScore(), timeElapsed, enemiesDefeated, currentLevel);
            
            // Stop the game loop
            gameLoopRunning = false;
            
            // Transition to victory screen
            Stage stage = (Stage) gameCanvas.getScene().getWindow();
            stage.setScene(victoryScene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Update the enemy defeated counter
    private void onEnemyDefeated(Enemy enemy) {
        enemiesDefeated++;
        player.addScore((int)enemy.getScoreValue());
        dropItemFromEnemy(enemy);
        
        // If in boss room and all enemies are defeated, mark boss as defeated
        if (currentRoom != null && currentRoom.getType() == DungeonRoom.RoomType.BOSS && 
            enemies.isEmpty()) {
            bossDefeated = true;
            
            // Trigger victory screen after a short delay to allow for effects
            javafx.application.Platform.runLater(() -> {
                // Create a delay timer to show victory screen after 1.5 seconds
                javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(Duration.seconds(1.5));
                delay.setOnFinished(e -> checkVictoryCondition());
                delay.play();
            });
        }
    }

    // Add a basic checkPlayerStatus method
    private void checkPlayerStatus() {
        if (player != null && player.getHealth() <= 0 && !gameOver) {
            // Mark game as over to prevent multiple game over screens
            gameOver = true;
            
            // Show game over screen
            showGameOverScreen();
        }
    }

    public void setLevel(int level) {
        this.currentLevel = level;
        adjustDifficultyForLevel();
    }

    private void adjustDifficultyForLevel() {
        // Increase enemy health and damage based on level
        for (Enemy enemy : enemies) {
            // Increase damage based on level
            if (currentLevel > 1) {
                double multiplier = 1.0 + (currentLevel - 1) * 0.2;
                enemy.setDamage(enemy.getDamage() * multiplier);
            }
        }
        
        // Spawn more enemies in combat rooms for levels > 1
        if (currentLevel > 1) {
            for (int i = 0; i < currentLevel - 1; i++) {
                for (DungeonRoom room : currentDungeon) {
                    if (room.getType() == DungeonRoom.RoomType.COMBAT && room != currentRoom) {
                        spawnEnemy(room);
                    }
                }
            }
        }
}

public void onPuzzleSolved(DungeonRoom room) {
        // Mark puzzle as solved
    puzzleCompleted = true;
        
        // Show success message
        Point2D roomCenter = new Point2D(gameCanvas.getWidth() / 2, gameCanvas.getHeight() / 2);
        effectsManager.showFloatingText("Puzzle Solved!", roomCenter, Color.GREEN);
        
        // Unlock doors
        for (Door door : doors) {
            if (!door.requiresKey()) {
                door.setLocked(false);
            }
        }
        
        // Spawn reward
        spawnRandomItem(roomCenter);
    }

    private void updatePlayerProjectiles(double deltaTime) {
        // Update player projectile attacks
        Iterator<ProjectileAttack> iterator = playerProjectiles.iterator();
        while (iterator.hasNext()) {
            ProjectileAttack attack = iterator.next();
            attack.update(deltaTime);
            
            // Check if attack is expired
            if (attack.isExpired()) {
                iterator.remove();
                continue;
            }
            
            // Check for collisions with enemies
            Point2D attackPos = attack.getPosition();
            double attackSize = attack.getSize();
            
            for (Enemy enemy : enemies) {
                Point2D enemyPos = enemy.getPosition();
                double enemySize = enemy.getSize();
                
                if (checkCollision(attackPos, attackSize, enemyPos, enemySize)) {
                    // Apply damage to enemy
                    enemy.takeDamage(attack.getDamage());
                    
                    // Show damage text
                    effectsManager.showFloatingText("-" + (int)attack.getDamage(), 
                        enemyPos, Color.RED);
                    
                    // Remove projectile attack
                    iterator.remove();
                    break;
                }
            }
        }
    }

    private void firePlayerProjectile() {
        // Create a new projectile attack from player position
        Point2D playerPos = player.getPosition();
        Point2D targetPos = new Point2D(mouseX, mouseY);
        Point2D direction = targetPos.subtract(playerPos).normalize();
        double damage = 15;
        
        // Add projectile to player projectiles
        ProjectileAttack attack = new ProjectileAttack(
            playerPos.getX(), 
            playerPos.getY(), 
            direction, 
            damage
        );
        playerProjectiles.add(attack);
        
        // Add visual effect
        effectsManager.showFloatingText("Fired!", playerPos, Color.YELLOW);
    }

    private void movePlayer(double deltaTime) {
        if (player == null) return;
        
        // Limit player movement to stay within the room boundaries
        double minX = 10; // Left wall + margin
        double minY = 10; // Top wall + margin
        double maxX = gameCanvas.getWidth() - player.getSize() - 10; // Right wall - player size - margin
        double maxY = gameCanvas.getHeight() - player.getSize() - 10; // Bottom wall - player size - margin
        
        // Get current position
        Point2D currentPos = player.getPosition();
        
        // Ensure player stays within bounds
        double newX = Math.max(minX, Math.min(maxX, currentPos.getX()));
        double newY = Math.max(minY, Math.min(maxY, currentPos.getY()));
        
        // Update player position if it changed due to boundaries
        if (newX != currentPos.getX() || newY != currentPos.getY()) {
            player.setPosition(new Point2D(newX, newY));
        }
    }
    
    private void dropItemFromEnemy(Enemy enemy) {
        if (enemy == null) return;
        
        Random random = new Random();
        if (random.nextDouble() > 0.3) return; // 30% drop chance
        
        // Get enemy position
        Point2D position = enemy.getPosition();
        
        // Randomize drop type
        Item.ItemType type;
        String name;
        String description;
        int value;
        boolean consumable;
        
        if (random.nextDouble() < 0.8) {
            // 80% chance for a potion
            type = Item.ItemType.POTION;
            name = "Health Potion";
            description = "Restores 20 health";
            value = 20;
            consumable = true;
        } else {
            // 20% chance for a weapon
            type = Item.ItemType.WEAPON;
            String[] weapons = {"Dagger", "Short Sword", "Axe"};
            name = weapons[random.nextInt(weapons.length)];
            description = "A basic weapon";
            value = 5 + random.nextInt(5);
            consumable = false;
        }
        
        // Create the item
        Item item = new Item(name, description, type, value, consumable);
        item.setX(position.getX());
        item.setY(position.getY());
        item.setSize(20);
        roomItems.add(item);
}

    // Handle weapon selection
    private void selectWeapon(int weaponIndex) {
        if (player == null) return;
        
        System.out.println("Attempting to select weapon: " + weaponIndex);
        
        // Implement weapon selection logic
        player.selectWeapon(weaponIndex);
        
        // Show message about selected weapon
        String weaponName = "Unknown";
        switch (weaponIndex) {
            case 0: weaponName = "Sword"; break;
            case 1: weaponName = "Bow"; break;
            case 2: weaponName = "Staff"; break;
            case 3: weaponName = "Axe"; break;
        }
        
        effectsManager.showFloatingText("Selected: " + weaponName, 
            player.getPosition(), Color.LIGHTYELLOW);
    }
}
