package com.dungeon.controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.dungeon.audio.SoundManager;
import com.dungeon.effects.EffectsManager;
import com.dungeon.model.Door;
import com.dungeon.model.DungeonGenerator;
import com.dungeon.model.DungeonRoom;
import com.dungeon.model.Item;
import com.dungeon.model.Puzzle;
import com.dungeon.model.entity.Enemy;
import com.dungeon.model.entity.EnemyAbility;
import com.dungeon.model.entity.Entity;
import com.dungeon.model.entity.Player;
import com.dungeon.model.entity.Projectile;
import com.dungeon.model.entity.ProjectileAttack;
import com.dungeon.view.DungeonRenderer;
import com.dungeon.view.LightingEffect;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle; 
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

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
    private int clearedCombatRooms = 0;
    private int roomsClearedInLevel = 0; // Track rooms cleared in current level
    private List<DungeonRoom.RoomType> availableRoomTypes = new ArrayList<>(); // Track available room types
    private long startTime;
    private boolean gameOver = false;
    private boolean victory = false;
    private boolean isDebugMode = true; // Enable debug mode by default to diagnose issues
    private boolean puzzleSolved = false; // Add missing puzzleSolved variable
    private boolean awaitingLevelUp = false; // Track if player needs to return to spawn for level up
    // Add tracking for puzzle and treasure room clear states
    private boolean puzzleClearedInLevel = false;
    private boolean treasureClearedInLevel = false;

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
    
    private SoundManager soundManager;
    
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
        
        // Center the pause menu in the canvasContainer instead of rootPane
        // This ensures it stays within the game area
        pauseMenu.setLayoutX((canvasContainer.getWidth() - pauseMenu.getPrefWidth()) / 2);
        pauseMenu.setLayoutY((canvasContainer.getHeight() - pauseMenu.getPrefHeight()) / 2);
        
        // Add positioning listeners to keep menu centered when window is resized
        canvasContainer.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (pauseMenu != null) {
                pauseMenu.setLayoutX((newVal.doubleValue() - pauseMenu.getWidth()) / 2);
            }
        });
        
        canvasContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (pauseMenu != null) {
                pauseMenu.setLayoutY((newVal.doubleValue() - pauseMenu.getHeight()) / 2);
            }
        });
        
        // Initially hide the pause menu
        pauseMenu.setVisible(false);
        
        // Add to canvasContainer instead of rootPane
        canvasContainer.getChildren().add(pauseMenu);
        
        // Ensure the menu stays on top
        StackPane.setAlignment(pauseMenu, Pos.CENTER);
    }
    
    private void togglePauseGame() {
        isPaused = !isPaused;
        
        if (isPaused) {
            // Update position to ensure it's centered correctly
            double menuWidth = pauseMenu.prefWidth(-1); // Get preferred width
            double menuHeight = pauseMenu.prefHeight(-1); // Get preferred height
            
            // Center in canvas container
            pauseMenu.setLayoutX((canvasContainer.getWidth() - menuWidth) / 2);
            pauseMenu.setLayoutY((canvasContainer.getHeight() - menuHeight) / 2);
            
            pauseMenu.setVisible(true);
            pauseMenu.toFront(); // Ensure it's on top
            
            // Stop the game loop
            gameLoopRunning = false;
            soundManager.stopBackgroundMusic();
        } else {
            pauseMenu.setVisible(false);
            // Resume the game loop
            gameLoopRunning = true;
            soundManager.playBackgroundMusic();
            startGameLoop();
        }
    }
    
    private void resumeGame() {
        isPaused = false;
        pauseMenu.setVisible(false);
        gameLoopRunning = true;
        
        // Ensure we fully restart background music
        soundManager.stopBackgroundMusic(); // Stop first to reset the player
        soundManager.playBackgroundMusic(); // Then start again
        
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
        loadMainMenu();
    }

    // Add this new method to consistently load the main menu
    private void loadMainMenu() {
        try {
            System.out.println("Loading main menu...");
            // Load the main menu
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/dungeon/fxml/MainMenu.fxml"));
            Parent menuRoot = loader.load();
            Scene menuScene = new Scene(menuRoot, 1024, 768);
            
            // Configure styling for the scene - match Main.java implementation
            try {
                System.out.println("Adding stylesheet to main menu");
                menuScene.getStylesheets().add(getClass().getResource("/com/dungeon/styles/main.css").toExternalForm());
            } catch (Exception e) {
                System.out.println("Stylesheet not found: " + e.getMessage());
            }
            
            // Close any active audio resources
            if (soundManager != null) {
                soundManager.stopBackgroundMusic();
                soundManager.stopAllSounds();
            }
            
            // Get the current stage - we need to be careful because gameCanvas might be null
            Stage stage = null;
            
            // Try to get the stage from gameCanvas first
            if (gameCanvas != null && gameCanvas.getScene() != null && gameCanvas.getScene().getWindow() != null) {
                stage = (Stage) gameCanvas.getScene().getWindow();
                System.out.println("Got stage from gameCanvas");
            } 
            // If we can't get it from gameCanvas, try to get it from rootPane
            else if (rootPane != null && rootPane.getScene() != null && rootPane.getScene().getWindow() != null) {
                stage = (Stage) rootPane.getScene().getWindow();
                System.out.println("Got stage from rootPane");
            } 
            // If still null, try alternate approaches by looking in the current scenes
            else {
                System.out.println("Could not get stage from standard components, looking for active stages");
                // This is a bit of a hack, but it can help us find the current window
                for (javafx.stage.Window window : javafx.stage.Window.getWindows()) {
                    if (window instanceof Stage && window.isShowing()) {
                        stage = (Stage) window;
                        System.out.println("Found active stage: " + stage.getTitle());
                        break;
                    }
                }
            }
            
            if (stage == null) {
                System.err.println("Could not find an active stage to show main menu!");
                return;
            }
            
            stage.setScene(menuScene);
            
            // Configure stage properties to match Main.java
            stage.setResizable(true);
            stage.setMinWidth(800);
            stage.setMinHeight(600);
            stage.centerOnScreen();
            
            stage.show();
            System.out.println("Main menu displayed successfully");
        } catch (Exception e) {
            System.err.println("Error loading main menu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {
        soundManager = SoundManager.getInstance();
        soundManager.playSound("start"); // Play start sound immediately
        
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
        // Play start sound first, before any initialization
        soundManager.playSound("start");
        
        // Initialize game components
        random = new Random();
        activeKeys = new HashSet<>();
        enemies = new ArrayList<>();
        roomItems = new ArrayList<>();
        doors = new ArrayList<>();
        playerProjectiles = new ArrayList<>();
        projectiles = new ArrayList<>();
        roomsClearedInLevel = 0;
        puzzleClearedInLevel = false;
        treasureClearedInLevel = false;
        
        // Initialize available room types for the first level
        availableRoomTypes.clear();
        availableRoomTypes.add(DungeonRoom.RoomType.COMBAT);
        availableRoomTypes.add(DungeonRoom.RoomType.PUZZLE);
        availableRoomTypes.add(DungeonRoom.RoomType.TREASURE);
        
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
            
            // Play background music after everything is set up
            soundManager.playBackgroundMusic();
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
        
        // Make sure canvas can receive focus
        gameCanvas.setFocusTraversable(true);
        gameCanvas.requestFocus();
        
        // Add focus listener to ensure canvas keeps focus
        gameCanvas.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                gameCanvas.requestFocus();
            }
        });
        
        // Handle key presses on the canvas
        gameCanvas.setOnKeyPressed(e -> {
            System.out.println("Key pressed: " + e.getCode());
            activeKeys.add(e.getCode());
            
            // Handle inventory key
            if (e.getCode() == KeyCode.I) {
                openInventory();
            } else if (e.getCode() == KeyCode.F) {  
                System.out.println("F key pressed - Current room type: " + 
                    (currentRoom != null ? currentRoom.getType() : "null"));
                
                // Interact with puzzle or door
                if (currentRoom != null && currentRoom.getType() == DungeonRoom.RoomType.PUZZLE) {
                    System.out.println("F key pressed in puzzle room - attempting to interact with puzzle");
                    System.out.println("Puzzle exists: " + (puzzles.get(currentRoom) != null));
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
        
        gameCanvas.setOnKeyReleased(e -> {
            System.out.println("Key released: " + e.getCode());
            activeKeys.remove(e.getCode());
        });
        
        // Add mouse movement handler for aiming
        gameCanvas.setOnMouseMoved(e -> {
            if (player != null) {
                mouseX = e.getX();
                mouseY = e.getY();
                player.updateMousePosition(e.getX(), e.getY());
            }
        });
        
        // Add mouse drag handler for aiming while moving
        gameCanvas.setOnMouseDragged(e -> {
            if (player != null) {
                mouseX = e.getX();
                mouseY = e.getY();
                player.updateMousePosition(e.getX(), e.getY());
            }
        });
        
        // Add mouse click handler for shooting and UI interaction
        gameCanvas.setOnMouseClicked(e -> {
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
        
        // Request focus on the canvas
        gameCanvas.requestFocus();
    }

    private void attackEnemiesInRange() {
        // Find enemies in melee range and damage them
        double meleeRange = 50; // Melee attack range
        for (Enemy enemy : enemies) {
            if (isInMeleeRange(player, enemy)) {
                // Apply melee damage
                double meleeDamage = 20;
                enemy.takeDamage(meleeDamage);
                soundManager.playSound("character");
                
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
        boolean itemsPickedUp = false;
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
                soundManager.playSound("start"); // Changed from "character" to "start"
                
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
                itemsPickedUp = true;
            }
        }
        // If in treasure room, and all items are picked up, mark treasureClearedInLevel and update doors
        if (currentRoom != null && currentRoom.getType() == DungeonRoom.RoomType.TREASURE && roomItems.isEmpty() && !treasureClearedInLevel) {
            treasureClearedInLevel = true;
            createDoors(); // This will unlock the combat room in the spawn room
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
        
        // Check if player is close enough to interact with door (smaller range for actual transition)
        return distanceX < (player.getSize() / 2 + door.getWidth() / 2 + 5) &&
               distanceY < (player.getSize() / 2 + door.getHeight() / 2 + 5);
    }
    
    private void checkRoomClearConditions() {
        if (currentRoom == null || roomCleared) return;
        System.out.println("Checking room clear conditions - Room type: " + currentRoom.getType() + 
                          ", Rooms cleared: " + roomsClearedInLevel + 
                          ", Current level: " + currentLevel + 
                          ", awaiting level up: " + awaitingLevelUp);
        switch (currentRoom.getType()) {
            case COMBAT:
                if (enemies.isEmpty() || enemies.stream().allMatch(enemy -> enemy.getHealth() <= 0)) {
                    roomCleared = true;
                    roomsClearedInLevel++;
                    System.out.println("Combat room cleared! Total rooms cleared: " + roomsClearedInLevel + 
                                      ", Current level: " + currentLevel + 
                                      ", Total needed for boss: " + (currentLevel >= 3 && roomsClearedInLevel >= 3));
                    
                    // After combat room is cleared, show spawn room door
                    if (!awaitingLevelUp) {
                        awaitingLevelUp = true;
                        System.out.println("Setting awaitingLevelUp to true");
                        
                        // Create only the spawn room door in the combat room
                        doors.clear();
                        DungeonRoom spawnRoom = currentDungeon.stream()
                            .filter(room -> room.getType() == DungeonRoom.RoomType.SPAWN)
                            .findFirst()
                            .orElse(null);
                        if (spawnRoom != null) {
                            double roomWidth = gameCanvas.getWidth() - 100;
                            double roomHeight = gameCanvas.getHeight() - 100;
                            double doorWidth = DOOR_WIDTH;
                            double doorHeight = DOOR_HEIGHT;
                            double doorX = 50;
                            double doorY = roomHeight / 2 - doorHeight / 2;
                            Door spawnDoor = new Door(doorX, doorY, doorWidth, doorHeight, currentRoom, spawnRoom, Door.DoorDirection.WEST);
                            spawnDoor.setLocked(false);
                            doors.add(spawnDoor);
                            effectsManager.showFloatingText("Return to spawn room!", new Point2D(doorX + doorWidth/2, doorY - 20), Color.YELLOW);
                            System.out.println("Created spawn door at: " + doorX + "," + doorY);
                        }
                    }
                }
                break;
            case PUZZLE:
                if (puzzleSolved) {
                    roomCleared = true;
                    roomsClearedInLevel++;
                    puzzleClearedInLevel = true;
                    System.out.println("Puzzle room cleared! Total rooms cleared: " + roomsClearedInLevel);
                    // After puzzle is cleared, update spawn room doors
                    createDoors();
                }
                break;
            case TREASURE:
                // No longer set treasureClearedInLevel or createDoors here; handled in checkItemPickups
                break;
            case BOSS:
                if (enemies.isEmpty() || enemies.stream().allMatch(enemy -> enemy.getHealth() <= 0)) {
                    roomCleared = true;
                    showVictoryScreen();
                }
                break;
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
        
        // Draw the room doors with animations
        renderDoorsWithAnimations(gc);
        
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
            
            // Draw item name when player is close
            if (player != null && 
                player.getPosition().distance(new Point2D(item.getX(), item.getY())) < 100) {
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
                
                // Draw enemy type
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("Verdana", FontWeight.NORMAL, 10));
                gc.fillText(enemy.getType().toString(), enemy.getX(), enemy.getY() - 12);
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
        // Set font for UI elements
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 14));
        
        // Health bar background
        gc.setFill(Color.DARKRED);
        gc.fillRect(10, 30, 100, 10);
        
        // Health bar foreground
        gc.setFill(Color.RED);
        gc.fillRect(10, 30, 100 * (player.getHealth() / player.getMaxHealth()), 10);
        
        // Health text
        gc.setFill(Color.WHITE);
        gc.fillText("Health: " + (int)player.getHealth() + "/" + (int)player.getMaxHealth(), 120, 40);
        
        // Enemies defeated
        gc.fillText("Enemies Defeated: " + enemiesDefeated, 10, 60);
        
        // Current level
        gc.fillText("Level: " + currentLevel, 10, 80);
        
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
        doors.clear();
        if (currentRoom == null) {
            System.err.println("ERROR: Cannot create doors for null room");
            return;
        }
        System.out.println("Creating doors for room type: " + currentRoom.getType() + ", Rooms cleared in level: " + roomsClearedInLevel + ", Current level: " + currentLevel + ", Awaiting level up: " + awaitingLevelUp);
        double roomWidth = gameCanvas.getWidth() - 100;
        double roomHeight = gameCanvas.getHeight() - 100;
        double doorWidth = DOOR_WIDTH;
        double doorHeight = DOOR_HEIGHT;
        List<Point2D> existingDoorPositions = new ArrayList<>();
        
        if (currentRoom.getType() == DungeonRoom.RoomType.SPAWN) {
            // Ensure boss room creation works at level 3
            if ((currentLevel == 3 && roomsClearedInLevel >= 3) || (currentLevel >= 3 && awaitingLevelUp)) {
                System.out.println("Boss door condition met! Creating boss door in spawn room.");
                doors.clear();
                DungeonRoom bossRoom = new DungeonRoom(
                    currentRoom.getX() + 1,
                    currentRoom.getY(),
                    800, 600, DungeonRoom.RoomType.BOSS
                );
                double doorX = roomWidth - doorWidth + 50;
                double doorY = roomHeight / 2 - doorHeight / 2;
                Door bossDoor = new Door(doorX, doorY, doorWidth, doorHeight, currentRoom, bossRoom, Door.DoorDirection.EAST);
                bossDoor.setLocked(false);
                doors.add(bossDoor);
                System.out.println("Created boss door in spawn room");
                return;
            }
            
            // If player is returning after clearing 3 rooms, level up and reset
            if (awaitingLevelUp) {
                awaitingLevelUp = false;
                showLevelCompletionMessage();
                
                // Special condition for level 3 - create boss door instead of regular doors
                if (currentLevel >= 3) {
                    // Always create only the boss door in the spawn room after level 3 combat room is cleared
                    doors.clear();
                    DungeonRoom bossRoom = new DungeonRoom(
                        currentRoom.getX() + 1,
                        currentRoom.getY(),
                        800, 600, DungeonRoom.RoomType.BOSS
                    );
                    double doorX = roomWidth - doorWidth + 50;
                    double doorY = roomHeight / 2 - doorHeight / 2;
                    Door bossDoor = new Door(doorX, doorY, doorWidth, doorHeight, currentRoom, bossRoom, Door.DoorDirection.EAST);
                    bossDoor.setLocked(false);
                    doors.add(bossDoor);
                    System.out.println("Created boss door in spawn room after level 3");
                    return;
                } else {
                    // Reset all level-specific flags
                    currentLevel++;
                    roomsClearedInLevel = 0;
                    puzzleClearedInLevel = false;
                    treasureClearedInLevel = false;
                    puzzleSolved = false;
                    puzzleCompleted = false;
                    
                    // Clear and reset available room types
                    availableRoomTypes.clear();
                    availableRoomTypes.add(DungeonRoom.RoomType.COMBAT);
                    availableRoomTypes.add(DungeonRoom.RoomType.PUZZLE);
                    availableRoomTypes.add(DungeonRoom.RoomType.TREASURE);
                    
                    // Clear any existing puzzles
                    puzzles.clear();
                }
            }
            
            // Create all three doors with specific locking logic
            double spacing = roomHeight / 4;
            double startY = spacing;
            for (DungeonRoom.RoomType type : availableRoomTypes) {
                DungeonRoom newRoom = new DungeonRoom(
                    currentRoom.getX() + 1,
                    currentRoom.getY(),
                    800,
                    600,
                    type
                );
                double doorX = roomWidth - doorWidth + 50;
                double doorY = startY;
                startY += spacing;
                Door door = new Door(doorX, doorY, doorWidth, doorHeight, currentRoom, newRoom, Door.DoorDirection.EAST);
                
                // Original door locking logic - same for all levels
                if (type == DungeonRoom.RoomType.COMBAT) {
                    // Combat room is always locked initially
                    door.setLocked(true);
                } else if (type == DungeonRoom.RoomType.PUZZLE) {
                    // Puzzle room is always unlocked
                    door.setLocked(false);
                } else if (type == DungeonRoom.RoomType.TREASURE) {
                    // Treasure room is always locked and requires key at start of level
                    door.setLocked(true);
                    door.setRequiresKey(true);
                }
                
                doors.add(door);
                existingDoorPositions.add(new Point2D(doorX, doorY));
            }
        } else if (currentRoom.getType() == DungeonRoom.RoomType.BOSS) {
            System.out.println("Boss room - no doors to create");
            return;
        } else {
            // Remove the current room type from available types
            availableRoomTypes.remove(currentRoom.getType());
            createDoorsForRoomTypes(availableRoomTypes, roomWidth, roomHeight, doorWidth, doorHeight, existingDoorPositions);
            
            // Only add spawn door if 3 rooms cleared and awaitingLevelUp is true
            if (roomsClearedInLevel >= 3 && awaitingLevelUp) {
                DungeonRoom spawnRoom = currentDungeon.stream()
                    .filter(room -> room.getType() == DungeonRoom.RoomType.SPAWN)
                    .findFirst()
                    .orElse(null);
                if (spawnRoom != null) {
                    double doorX = 50;
                    double doorY = roomHeight / 2 - doorHeight / 2;
                    Door spawnDoor = new Door(doorX, doorY, doorWidth, doorHeight, currentRoom, spawnRoom, Door.DoorDirection.WEST);
                    spawnDoor.setLocked(false);
                    doors.add(spawnDoor);
                    existingDoorPositions.add(new Point2D(doorX, doorY));
                    effectsManager.showFloatingText("Return to spawn room!", new Point2D(doorX + doorWidth/2, doorY - 20), Color.YELLOW);
                    System.out.println("Created spawn door at: " + doorX + "," + doorY);
                }
            }
        }
        System.out.println("Created " + doors.size() + " doors");
    }

    private void createDoorsForRoomTypes(List<DungeonRoom.RoomType> roomTypes, double roomWidth, double roomHeight, 
                                       double doorWidth, double doorHeight, List<Point2D> existingDoorPositions) {
        // Calculate door positions to prevent overlap
        double doorSpacing = roomHeight / (roomTypes.size() + 1);
        double startY = doorSpacing;
        
        for (DungeonRoom.RoomType roomType : roomTypes) {
            // Create a new room of the specified type
            DungeonRoom newRoom = new DungeonRoom(
                currentRoom.getX() + 1,
                currentRoom.getY(),
                800,
                600,
                roomType
            );
            
            // Calculate door position
            double doorX = roomWidth - doorWidth + 50;
            double doorY = startY;
            startY += doorSpacing;
            
            // Create the door
            Door door = new Door(doorX, doorY, doorWidth, doorHeight, currentRoom, newRoom, Door.DoorDirection.EAST);
            
            // Lock doors based on room type and current room
            if (currentRoom.getType() == DungeonRoom.RoomType.COMBAT) {
                // In combat room, all doors are locked until enemies are cleared
                door.setLocked(!enemies.isEmpty());
            } else if (currentRoom.getType() == DungeonRoom.RoomType.PUZZLE) {
                if (roomType == DungeonRoom.RoomType.TREASURE) {
                    // Treasure room door requires key and is locked until puzzle is solved
                    door.setLocked(!puzzleSolved);
                    door.setRequiresKey(true);
                } else if (roomType == DungeonRoom.RoomType.COMBAT) {
                    // Combat room door is always locked in puzzle room
                    door.setLocked(true);
                } else {
                    door.setLocked(false);
                }
            } else if (currentRoom.getType() == DungeonRoom.RoomType.TREASURE) {
                if (roomType == DungeonRoom.RoomType.COMBAT) {
                    // Combat room door is unlocked after treasure room is cleared
                    door.setLocked(false);
                } else {
                    door.setLocked(false);
                }
            }
            
            doors.add(door);
            existingDoorPositions.add(new Point2D(doorX, doorY));
        }
    }

    private void createDoorToRoom(DungeonRoom targetRoom, double roomWidth, double roomHeight, 
                                double doorWidth, double doorHeight, List<Point2D> existingDoorPositions) {
        // Calculate door position based on direction
        double doorX, doorY;
        Door.DoorDirection doorDirection;
        
        // Position doors on the right side of the room
        doorX = roomWidth - doorWidth + 50;
        doorY = (roomHeight / 2) - (doorHeight / 2) + 50;
        doorDirection = Door.DoorDirection.EAST;
        
        // Create the door
        Door door = new Door(doorX, doorY, doorWidth, doorHeight, currentRoom, targetRoom, doorDirection);
        
        // Lock doors based on room type
        if (currentRoom.getType() == DungeonRoom.RoomType.COMBAT ||
            currentRoom.getType() == DungeonRoom.RoomType.BOSS) {
            door.setLocked(!enemies.isEmpty());
        } else if (currentRoom.getType() == DungeonRoom.RoomType.PUZZLE) {
            door.setLocked(!puzzleCompleted);
            door.setRequiresKey(true);
        }
        
        doors.add(door);
        existingDoorPositions.add(new Point2D(doorX, doorY));
    }

    private void populateRoom(DungeonRoom room) {
        // Clear existing entities
        enemies.clear();
        roomItems.clear();
        roomCleared = false;
        
        // Get room center
        double centerX = gameCanvas.getWidth() / 2;
        double centerY = gameCanvas.getHeight() / 2;
        Point2D roomCenter = new Point2D(centerX, centerY);
        
        // Adjust difficulty based on level
        double difficultyMultiplier = 1.0 + (currentLevel - 1) * 0.2;
        
        switch (room.getType()) {
            case PUZZLE:
                System.out.println("Populating puzzle room...");
                // Create a new puzzle for this room
                Puzzle puzzle = Puzzle.createRandomPuzzle();
                puzzles.put(room, puzzle);
                System.out.println("Created puzzle: " + puzzle.getDescription());
                // Only place key if puzzle is already solved
                if (puzzle.isSolved()) {
                    placeKeyInRoom(room);
                }
                break;
                
            case COMBAT:
                // Add enemies with increased difficulty
                int enemyCount = 2 + new Random().nextInt(3);
                System.out.println("Spawning " + enemyCount + " enemies in combat room");
                
                for (int i = 0; i < enemyCount; i++) {
                    Random rnd = new Random();
                    double angle = rnd.nextDouble() * Math.PI * 2;
                    double distance = 100 + rnd.nextDouble() * 150;
                    
                    double offsetX = Math.cos(angle) * distance;
                    double offsetY = Math.sin(angle) * distance;
                    
                    double enemyX = centerX + offsetX;
                    double enemyY = centerY + offsetY;
                    
                    enemyX = Math.max(50, Math.min(gameCanvas.getWidth() - 50, enemyX));
                    enemyY = Math.max(50, Math.min(gameCanvas.getHeight() - 50, enemyY));
                    
                    Enemy.EnemyType type = rnd.nextBoolean() ? Enemy.EnemyType.GOBLIN : Enemy.EnemyType.SKELETON;
                    Enemy enemy = new Enemy(enemyX, enemyY, type);
                    
                    // Scale enemy stats with level
                    enemy.setMaxHealth(enemy.getMaxHealth() * difficultyMultiplier);
                    enemy.setDamage(enemy.getDamage() * difficultyMultiplier);
                    enemy.heal(enemy.getMaxHealth());
                    
                    enemies.add(enemy);
                }
                break;
                
            case BOSS:
                // Add boss enemy with increased difficulty
                System.out.println("Spawning boss in boss room");
                Enemy bossEnemy = new Enemy(centerX, centerY, Enemy.EnemyType.BOSS);
                bossEnemy.setMaxHealth(bossEnemy.getMaxHealth() * difficultyMultiplier * 2);
                bossEnemy.setDamage(bossEnemy.getDamage() * difficultyMultiplier * 1.5);
                bossEnemy.heal(bossEnemy.getMaxHealth());
                enemies.add(bossEnemy);
                break;
                
            case TREASURE:
                // Add better items in higher levels
                System.out.println("Spawning items in treasure room");
                Random rnd = new Random();
                int itemCount = 2 + rnd.nextInt(3);
                
                for (int i = 0; i < itemCount; i++) {
                    double angle = rnd.nextDouble() * Math.PI * 2;
                    double distance = 50 + rnd.nextDouble() * 100;
                    
                    double offsetX = Math.cos(angle) * distance;
                    double offsetY = Math.sin(angle) * distance;
                    
                    spawnLeveledItem(roomCenter.add(offsetX, offsetY));
                }
                break;
        }
        
        // Create doors for the new room
        createDoors();
    }

    private void spawnLeveledItem(Point2D position) {
        Random random = new Random();
        
        // Higher chance of better items in higher levels
        double rnd = random.nextDouble();
        Item.ItemType type;
        String name;
        String description;
        int value;
        boolean consumable;
        
        if (rnd < 0.4) {
            // 40% chance for a potion
            type = Item.ItemType.POTION;
            name = "Health Potion";
            description = "Restores " + (20 * currentLevel) + " health";
            value = 20 * currentLevel;
            consumable = true;
        } else if (rnd < 0.7) {
            // 30% chance for a weapon
            type = Item.ItemType.WEAPON;
            String[] weapons = {"Iron Sword", "Steel Sword", "Magic Sword", "Legendary Blade"};
            int weaponIndex = Math.min(currentLevel - 1, weapons.length - 1);
            name = weapons[weaponIndex];
            description = "A powerful weapon";
            value = 15 * currentLevel;
            consumable = false;
        } else {
            // 30% chance for armor
            type = Item.ItemType.ARMOR;
            String[] armors = {"Leather Armor", "Chain Mail", "Plate Armor", "Magic Armor"};
            int armorIndex = Math.min(currentLevel - 1, armors.length - 1);
            name = armors[armorIndex];
            description = "Provides protection";
            value = 20 * currentLevel;
            consumable = false;
        }
        
        // Create the item
        Item item = new Item(name, description, type, value, consumable);
        item.setX(position.getX());
        item.setY(position.getY());
        item.setSize(20);
        
        roomItems.add(item);
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

    private void transitionToRoom(DungeonRoom newRoom, Point2D entryPosition) {
        if (newRoom == null) return;
        
        // Play transition sound
        soundManager.playSound("teleport");
        
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
            
            // Reset room cleared state
            roomCleared = false;
            
            // Debug print room transition
            System.out.println("Transitioning to room type: " + newRoom.getType() + 
                             ", Rooms cleared in level: " + roomsClearedInLevel);
            
            // If entering a puzzle room, ensure a puzzle exists
            if (newRoom.getType() == DungeonRoom.RoomType.PUZZLE) {
                System.out.println("Entering puzzle room - checking for puzzle");
                if (!puzzles.containsKey(newRoom)) {
                    System.out.println("Creating new puzzle for room");
                    puzzles.put(newRoom, Puzzle.createRandomPuzzle());
                }
                System.out.println("Puzzle exists: " + (puzzles.get(newRoom) != null));
            }
            
            // Populate new room
            populateRoom(currentRoom);
            
            // Create doors for the new room
            createDoors();
            
            // Update minimap
            updateMinimap();
            
            // Ensure canvas has focus
            gameCanvas.requestFocus();
        });
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
                double dx = direction.getX() * enemy.getSpeed() * 0.3 * deltaTime;
                double dy = direction.getY() * enemy.getSpeed() * 0.3 * deltaTime;
                
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
            System.out.println("Not in a puzzle room");
            return;
        }
        
        // Get the puzzle for this room
        Puzzle puzzle = puzzles.get(currentRoom);
        if (puzzle == null) {
            System.out.println("No puzzle found for this room");
            return;
        }
        
        if (puzzle.isSolved()) {
            System.out.println("Puzzle already solved");
            return;
        }
        
        // Create puzzle window programmatically
        try {
            System.out.println("Creating puzzle window...");
            
            // Create root container
            VBox rootContainer = new VBox(20);
            rootContainer.setAlignment(Pos.CENTER);
            rootContainer.setStyle("-fx-background-color: rgba(0, 0, 0, 0.9); -fx-padding: 20; -fx-background-radius: 10;");
            
            // Create title
            Label titleLabel = new Label("Puzzle Room");
            titleLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: white; -fx-font-weight: bold;");
            
            // Create description
            Label descriptionLabel = new Label(puzzle.getDescription());
            descriptionLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #CCCCCC;");
            descriptionLabel.setWrapText(true);
            descriptionLabel.setMaxWidth(400);
            
            // Create question label
            Label questionLabel = new Label(puzzle.getQuestion());
            questionLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #FFD700; -fx-font-weight: bold;");
            questionLabel.setWrapText(true);
            questionLabel.setMaxWidth(400);
            
            // Create answer field
            TextField answerField = new TextField();
            answerField.setPromptText("Enter your answer");
            answerField.setMaxWidth(300);
            answerField.setStyle("-fx-background-color: rgba(255, 255, 255, 0.9); -fx-font-size: 16px; -fx-padding: 8;");
            
            // Create feedback label
            Label feedbackLabel = new Label("");
            feedbackLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: white;");
            feedbackLabel.setWrapText(true);
            feedbackLabel.setMaxWidth(400);
            
            // Create submit button
            Button submitButton = new Button("Submit");
            submitButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 10 20; -fx-background-radius: 5;");
            submitButton.setOnMouseEntered(e -> submitButton.setStyle("-fx-background-color: #45a049; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 10 20; -fx-background-radius: 5;"));
            submitButton.setOnMouseExited(e -> submitButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 10 20; -fx-background-radius: 5;"));
            
            // Add components to container
            rootContainer.getChildren().addAll(titleLabel, descriptionLabel, questionLabel, answerField, feedbackLabel, submitButton);
            
            // Create the scene
            Scene scene = new Scene(rootContainer);
            
            // Create the stage
            Stage puzzleStage = new Stage();
            puzzleStage.initModality(Modality.APPLICATION_MODAL);
            puzzleStage.initStyle(StageStyle.UNDECORATED);
            puzzleStage.setScene(scene);
            
            // Center the stage
            puzzleStage.centerOnScreen();
            
            // Handle answer submission
            submitButton.setOnAction(e -> {
                String userAnswer = answerField.getText().trim();
                if (userAnswer.isEmpty()) {
                    feedbackLabel.setText("Please enter an answer");
                    feedbackLabel.setStyle("-fx-text-fill: #FFA500;");
                    return;
                }
                
                if (puzzle.checkAnswer(userAnswer)) {
                    feedbackLabel.setText("Correct! The door is now unlocked.");
                    feedbackLabel.setStyle("-fx-text-fill: #4CAF50;");
                    submitButton.setDisable(true);
                    answerField.setDisable(true);
                    
                    // Mark puzzle as solved
                    puzzle.setSolved(true);
                    puzzleSolved = true;
                    
                    // Call onPuzzleSolved to handle rewards and door unlocking
                    onPuzzleSolved(currentRoom);
                    
                    // Play success sound
                    soundManager.playSound("puzzle_solved");
                    
                    // Close the window after a delay
                    new Thread(() -> {
                        try {
                            Thread.sleep(2000);
                            Platform.runLater(() -> puzzleStage.close());
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }).start();
                } else {
                    feedbackLabel.setText("Incorrect. Try again!");
                    feedbackLabel.setStyle("-fx-text-fill: #FF4444;");
                    answerField.clear();
                    answerField.requestFocus();
                }
            });
            
            // Add keyboard shortcuts
            scene.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER) {
                    submitButton.fire();
                } else if (e.getCode() == KeyCode.ESCAPE) {
                    puzzleStage.close();
                }
            });
            
            // Show the stage
            puzzleStage.show();
            
        } catch (Exception e) {
            System.err.println("Error creating puzzle window: " + e.getMessage());
            e.printStackTrace();
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
            System.out.println("Showing game over screen...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/dungeon/fxml/GameOver.fxml"));
            Parent gameOverRoot = loader.load();
            Scene gameOverScene = new Scene(gameOverRoot);
            
            GameOverController controller = loader.getController();
            controller.setGameStats((int)player.getScore(), currentLevel - 1, enemiesDefeated);
            
            // Set the reference to this controller so it can properly return to main menu
            System.out.println("Setting game controller reference in GameOverController");
            controller.setGameController(this);
            
            // Stop the game loop
            gameLoopRunning = false;
            soundManager.stopSound("running"); // Stop running sound if still playing
            
            // Transition to game over screen
            Stage stage = (Stage) gameCanvas.getScene().getWindow();
            stage.setScene(gameOverScene);
            
            stage.show();
            
            System.out.println("Game over screen displayed");
        } catch (Exception e) {
            System.err.println("Error showing game over screen: " + e.getMessage());
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
            // Set the reference to this controller so it can properly return to main menu
            controller.setGameController(this);
            
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
        soundManager.playSound("character");
        
        // If in boss room and all enemies are defeated, mark boss as defeated
        if (currentRoom != null && currentRoom.getType() == DungeonRoom.RoomType.BOSS && 
            enemies.isEmpty()) {
            bossDefeated = true;
            soundManager.playSound("start"); // Victory sound
            
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
            soundManager.stopSound("running"); // Stop running sound
            soundManager.playSound("gameOver");
            soundManager.stopBackgroundMusic();
            
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
        
        // Play success sound
        soundManager.playSound("start");
        
        // Show success message
        Point2D roomCenter = new Point2D(gameCanvas.getWidth() / 2, gameCanvas.getHeight() / 2);
        effectsManager.showFloatingText("Puzzle Solved!", roomCenter, Color.GREEN);
        
        // Place the key in the room
        placeKeyInRoom(room);
        
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
        
        // Add visual effect and sound
        effectsManager.showFloatingText("Fired!", playerPos, Color.YELLOW);
        soundManager.playSound("character");
    }

    private void movePlayer(double deltaTime) {
        if (player == null) return;
        
        // Check if player is moving (any WASD key is pressed)
        boolean isMoving = activeKeys.contains(KeyCode.W) || 
                          activeKeys.contains(KeyCode.A) || 
                          activeKeys.contains(KeyCode.S) || 
                          activeKeys.contains(KeyCode.D);
        
        // Play running sound if moving
        if (isMoving) {
            soundManager.playSound("running");
        } else {
            soundManager.stopSound("running");
        }
        
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

    private void renderDoors(GraphicsContext gc) {
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
            
            // Display the connected room type with adjusted position
            String roomType = door.getConnectedRoom().getType().toString();
            gc.setFill(Color.WHITE);
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(2);
            gc.setTextAlign(TextAlignment.CENTER);
            
            // Calculate text position based on door direction
            double textX = door.getX() + door.getWidth() / 2;
            double textY;
            switch (door.getDirection()) {
                case NORTH:
                    textY = door.getY() - 20; // Above the door
                    break;
                case SOUTH:
                    textY = door.getY() + door.getHeight() + 20; // Below the door
                    break;
                case EAST:
                    textX = door.getX() + door.getWidth() + 20; // Right of the door
                    textY = door.getY() + door.getHeight() / 2;
                    break;
                case WEST:
                    textX = door.getX() - 20; // Left of the door
                    textY = door.getY() + door.getHeight() / 2;
                    break;
                default:
                    textY = door.getY() - 20;
            }
            
            // Draw text with outline for better visibility
            gc.strokeText(roomType, textX, textY);
            gc.fillText(roomType, textX, textY);
        }
    }

    private void showLevelCompletionMessage() {
        // Show a floating text in the center of the canvas
        if (gameCanvas != null && effectsManager != null) {
            effectsManager.showFloatingText(
                "Level Complete! Return to the spawn room!",
                new javafx.geometry.Point2D(gameCanvas.getWidth() / 2, gameCanvas.getHeight() / 2),
                javafx.scene.paint.Color.GOLD
            );
        } else {
            System.out.println("Level Complete! Return to the spawn room!");
        }
    }

    private void handleRoomTransition(DungeonRoom newRoom) {
        soundManager.playSound("teleport");
        // ... existing code ...
    }

    private void onPuzzleFailed() {
        soundManager.playSound("fail");
        // ... existing code ...
    }

    private void onPuzzleCompleted() {
        soundManager.playSound("start");
        // ... existing code ...
    }

    private void updatePlayerPosition(double deltaTime) {
        // ... existing movement code ...
        
        // Check if player is near any door and trigger animation
        for (Door door : doors) {
            if (!door.isLocked()) {
                Point2D playerPos = new Point2D(player.getX(), player.getY());
                double distance = playerPos.distance(
                    door.getX() + door.getWidth() / 2,
                    door.getY() + door.getHeight() / 2
                );
                System.out.println("Checking door at " + door.getX() + "," + door.getY() + 
                                 " - Distance: " + distance + 
                                 " - Animation playing: " + door.isAnimationPlaying());
                
                if (distance < 150) { // Door animation range
                    if (!door.isAnimationPlaying()) {
                        System.out.println("Starting door animation");
                        effectsManager.playDoorOpeningAnimation(door);
                        door.setAnimationPlaying(true);
                    }
                } else {
                    door.setAnimationPlaying(false);
                }
            }
        }
        
        // ... rest of the update code ...
    }

    // Add these fields to the class
    private Map<Door, Double> doorAnimations = new HashMap<>();
    private long lastDoorCheckTime = 0;
    
    // Add new method to render doors with animations
    private void renderDoorsWithAnimations(GraphicsContext gc) {
        if (doors.isEmpty()) {
            System.out.println("No doors to render");
            return;
        }
        
        System.out.println("Rendering " + doors.size() + " doors");
        long currentTime = System.currentTimeMillis();
        
        // Check player position to update door animations
        if (currentTime - lastDoorCheckTime > 100) { // Check every 100ms
            lastDoorCheckTime = currentTime;
            for (Door door : doors) {
                if (!door.isLocked()) {
                    Point2D playerPos = new Point2D(player.getX() + player.getSize()/2, 
                                                   player.getY() + player.getSize()/2);
                    double distance = playerPos.distance(
                        door.getX() + door.getWidth() / 2,
                        door.getY() + door.getHeight() / 2
                    );
                    
                    // Update door animation state based on player distance
                    if (distance < 150) {
                        // If player is near, start or continue opening animation
                        Double currentAngle = doorAnimations.getOrDefault(door, 0.0);
                        // Open door gradually up to 75 degrees
                        if (currentAngle < 75) {
                            doorAnimations.put(door, Math.min(currentAngle + 5, 75));
                        }
                    } else {
                        // If player is far, start or continue closing animation
                        Double currentAngle = doorAnimations.getOrDefault(door, 0.0);
                        if (currentAngle > 0) {
                            doorAnimations.put(door, Math.max(currentAngle - 5, 0));
                        } else if (currentAngle == 0) {
                            doorAnimations.remove(door); // Remove fully closed doors from map
                        }
                    }
                }
            }
        }
        
        // Render each door with its animation state
        for (Door door : doors) {
            Double openAngle = doorAnimations.getOrDefault(door, 0.0);
            Color doorColor;
            
            if (door.isLocked()) {
                doorColor = Color.RED;
            } else {
                doorColor = Color.GREEN;
            }
            
            // Draw door frame first
            gc.setFill(Color.DARKGRAY);
            gc.fillRect(door.getX() - 5, door.getY() - 5, door.getWidth() + 10, door.getHeight() + 10);
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(2);
            gc.strokeRect(door.getX() - 5, door.getY() - 5, door.getWidth() + 10, door.getHeight() + 10);
            
            // Draw door with rotation if it has an animation angle
            if (openAngle > 0) {
                // Save current transform
                gc.save();
                
                // Move to door hinge point
                gc.translate(door.getX(), door.getY() + door.getHeight() / 2);
                
                // Rotate
                gc.rotate(openAngle);
                
                // Draw rotated door
                gc.setFill(doorColor);
                gc.fillRect(0, -door.getHeight() / 2, door.getWidth(), door.getHeight());
                gc.setStroke(Color.BLACK);
                gc.setLineWidth(2);
                gc.strokeRect(0, -door.getHeight() / 2, door.getWidth(), door.getHeight());
                
                // Add door handle
                gc.setFill(Color.GOLD);
                gc.fillOval(door.getWidth() - 15, 0, 10, 10);
                
                // Restore transform
                gc.restore();
            } else {
                // Draw regular non-animated door
                gc.setFill(doorColor);
                gc.fillRect(door.getX(), door.getY(), door.getWidth(), door.getHeight());
                gc.setStroke(Color.BLACK); 
                gc.setLineWidth(2);
                gc.strokeRect(door.getX(), door.getY(), door.getWidth(), door.getHeight());
                
                // Draw door handle
                gc.setFill(Color.GOLD);
                gc.fillOval(door.getX() + door.getWidth() - 15, door.getY() + door.getHeight() / 2 - 5, 10, 10);
            }
            
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
            String roomTypeText = door.getConnectedRoom().getType().toString();
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Verdana", FontWeight.NORMAL, 10));
            gc.fillText(roomTypeText, 
                door.getX() + door.getWidth() / 2 - 20,
                door.getY() + door.getHeight() + 15);
        }
    }

    // Add a public method that can be called from other controllers
    public void returnToMainMenu() {
        System.out.println("returnToMainMenu called in GameController");
        loadMainMenu();
    }
}
