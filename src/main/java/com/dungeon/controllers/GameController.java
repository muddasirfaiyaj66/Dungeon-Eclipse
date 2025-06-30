package com.dungeon.controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.io.IOException; 
import java.util.Collections;

import com.dungeon.audio.SoundManager;
import com.dungeon.effects.EffectsManager;
import com.dungeon.model.Door;
import com.dungeon.model.DungeonGenerator;
import com.dungeon.model.DungeonRoom;
import com.dungeon.model.Item;
import com.dungeon.model.Puzzle;
import com.dungeon.model.Weapon;
import com.dungeon.model.Armor;
import com.dungeon.model.WeatherSystem;
import com.dungeon.model.entity.Player;
import com.dungeon.model.entity.Enemy;
import com.dungeon.model.entity.Entity;
import com.dungeon.model.entity.Projectile;
import com.dungeon.model.entity.ProjectileAttack;
import com.dungeon.model.entity.EnemyAbility;
import com.dungeon.view.DungeonRenderer;
import com.dungeon.view.LightingEffect;
import com.dungeon.utils.UIUtils; // Import the utility class

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
import javafx.scene.control.TextArea;
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
import javafx.animation.FadeTransition;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Window;

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
    private Map<DungeonRoom, Puzzle> puzzles = new HashMap<>();
private List<Puzzle> allPuzzles;
    private EffectsManager effectsManager;
    private LightingEffect lightingEffect;
    private List<String> floatingTexts; // For displaying damage, pickups, etc.
    private List<ProjectileAttack> playerProjectiles;
    private List<EnemyAbility.Projectile> enemyProjectiles;
    private List<Item> roomItems; // Items in the current room
    // private java.util.Map<DungeonRoom, Puzzle> puzzles; // Puzzles for puzzle rooms
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
    private boolean isInventoryOpen = false; // Track if inventory is open
    private boolean justLeveledUp = false; // Track if player just leveled up
    private double bossMinionSummonTimer = 0;
     private boolean bossEnraged = false;
     private Set<Enemy> enemiesToBounceBack = new HashSet<>();
     private int currentWave = 1;
     private final int totalWaves = 2;
     private Map<Enemy, Double> enemyAttackCooldowns = new HashMap<>();
    private static final double ATTACK_COOLDOWN = 2.0; // 2 seconds cooldown
    private static final double BOUNCE_DISTANCE = 100.0; // Distance to bounce back
    
    // Statue-related fields
    private boolean statueVisible = false;
    private Point2D statuePosition;
    private static final double STATUE_SIZE = 200.0;
    private static final double STATUE_INTERACTION_RANGE = 250.0;
    
    // Torch and treasure room fields
    private boolean torchActive = false;
    private Image torchImage;
    private static final double TORCH_LIGHT_RADIUS = 150.0;
    private static final double TORCH_SIZE = 40.0;
    private List<Point2D> spikePositions = new ArrayList<>();
    private List<Point2D> safePositions = new ArrayList<>();
    private int equipmentCollected = 0;
    private int totalEquipmentInRoom = 0;
    private boolean treasureRoomLighted = false;
    private static final double SPIKE_DAMAGE = 5.0;
    private static final double SPIKE_SIZE = 20.0;
    
    private Map<Enemy.EnemyType, javafx.scene.image.Image> enemyImages = new HashMap<>();
    private Map<Weapon.WeaponType, javafx.scene.image.Image> weaponImages = new HashMap<>();
    private GraphicsContext gc;
    private Map<Door, Double> doorAnimations = new HashMap<>();
    private long lastDoorCheckTime = 0;

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
    private Image combatBgImage;
   private Image treasureBgImage;
   private Image puzzleBgImage;
   private Image spawnBgImage;
   private Image bossBgImage;
   private Image statueImage;
private boolean backgroundsLoaded = false;
    private WeatherSystem weatherSystem;
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
            if (effectsManager != null) {
                effectsManager.resize(newVal.doubleValue(), gameCanvas.getHeight());
            }
            
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
            if (effectsManager != null) {
                effectsManager.resize(gameCanvas.getWidth(), newVal.doubleValue());
            }
            
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
    pauseMenu.setPrefHeight(250);

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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/dungeon/fxml/Options.fxml"));
            Parent optionsRoot = loader.load();

            
            Scene optionsScene = new Scene(optionsRoot);

            // Apply stylesheet (optional, but good for consistency)
            try {
                String cssPath = "/com/dungeon/styles/main.css"; 
                String css = getClass().getResource(cssPath).toExternalForm();
                if (css != null) {
                    optionsScene.getStylesheets().add(css);
                } else {
                    System.out.println("Options stylesheet not found at: " + cssPath);
                }
            } catch (NullPointerException e) {
                System.out.println("Options stylesheet not found or error constructing path: " + e.getMessage());
            }

            Stage optionsStage = new Stage();
            UIUtils.setStageIcon(optionsStage); // Set the icon here
            optionsStage.setTitle("Sound Options");
            optionsStage.initModality(Modality.APPLICATION_MODAL); // Block interaction with game window
            
            // Set owner to the main game stage
            if (gameCanvas != null && gameCanvas.getScene() != null && gameCanvas.getScene().getWindow() != null) {
                 optionsStage.initOwner(gameCanvas.getScene().getWindow());
            } else if (rootPane != null && rootPane.getScene() != null && rootPane.getScene().getWindow() != null) {
                 optionsStage.initOwner(rootPane.getScene().getWindow());
            }

            

            optionsStage.setScene(optionsScene);
            optionsStage.setResizable(false); // Typically options dialogs are not resizable
            
            System.out.println("Showing options window (modal). Game remains paused.");
            optionsStage.showAndWait(); // Show and wait for it to be closed

            System.out.println("Options window closed. Requesting focus back to game canvas.");
            
            if (gameCanvas != null) {
                gameCanvas.requestFocus();
            }

        } catch (IOException e) {
            System.err.println("Error loading Options.fxml from GameController: " + e.getMessage());
            e.printStackTrace();
            // Optionally show an error alert to the user here
        } catch (Exception e) {
            System.err.println("An unexpected error occurred while trying to show options: " + e.getMessage());
            e.printStackTrace();
        }
    }

   private void showChat(Window owner) {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/dungeon/fxml/Chat.fxml"));
        Parent chatRoot = loader.load();
        Scene chatScene = new Scene(chatRoot);
        Stage chatStage = new Stage();
        chatStage.setTitle("Game Chat");
        // Do NOT set modality, so the chat window is non-modal and can appear over the puzzle modal
        if (owner != null) {
            chatStage.initOwner(owner);
        }
        chatStage.setScene(chatScene);
        chatStage.setResizable(false); // Prevent resizing
        UIUtils.setStageIcon(chatStage);
        chatStage.show(); // Use show() instead of showAndWait()
        chatStage.setAlwaysOnTop(true); // Force always on top
        chatStage.toFront(); // Bring to front
    } catch (Exception e) {
        System.err.println("Error loading Chat.fxml: " + e.getMessage());
        e.printStackTrace();
        // Optionally show an error alert to the user here
    }
}
    private void exitToMainMenu() {
        System.out.println("Exiting to main menu...");
        soundManager.stopBackgroundMusic();
        if (weatherSystem != null) {
            weatherSystem.shutdown();
        }
        loadMainMenu();
    }

    private void loadMainMenu() {
        try {
            isPaused = false;
            gameLoopRunning = false;
            if (weatherSystem != null) {
                weatherSystem.shutdown();
            }

            // Get the current scene and its root pane
            Scene currentScene = gameCanvas.getScene();
            Parent gameRoot = currentScene.getRoot();

            // 1. Fade out the game screen
            FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), gameRoot);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(event -> {
                try {
                    // 2. Load the main menu content
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/dungeon/fxml/MainMenu.fxml"));
                    Parent mainMenuRoot = loader.load();
                    mainMenuRoot.setOpacity(0.0); // Start transparent for fade-in

                    // 3. Replace the scene's root with the new main menu content
                    currentScene.setRoot(mainMenuRoot);
                    
                    // 4. Fade in the new main menu
                    FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.5), mainMenuRoot);
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.play();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            fadeOut.play();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {
        soundManager = SoundManager.getInstance();
        // DEFERRED: soundManager.playSound("start");
        
        loadEnemyImages();
        loadWeaponImages();
        
        dungeonGenerator = new DungeonGenerator();
        // DEFERRED: dungeonRenderer = new DungeonRenderer(gameCanvas);
        activeKeys = new HashSet<>();
        enemies = new ArrayList<>();
        roomItems = new ArrayList<>();
        puzzles = new java.util.HashMap<>();
        doors = new ArrayList<>();
        playerProjectiles = new ArrayList<>();
        // DEFERRED: effectsManager = new EffectsManager(rootPane, gameCanvas);
        floatingTexts = new ArrayList<>();
        random = new Random();
        timeSinceLastSpawn = 0;
        bossDefeated = false;
        // gameLoopRunning will be managed by startGameLoop
        roomTransitionInProgress = false;
        roomCleared = false;
        puzzleCompleted = false;
        
        // Initialize weather system
        weatherSystem = new WeatherSystem();
        
        // DEFERRED: startTime = System.currentTimeMillis();
        
        createPauseMenu(); // UI structure, should be fine here
        
        // DEFERRED: setupGame();
        // DEFERRED: startGameLoop();
        
        // DEFERRED: gc = gameCanvas.getGraphicsContext2D();
    }

    private void loadEnemyImages() {
        try {
            enemyImages.put(Enemy.EnemyType.GOBLIN, new javafx.scene.image.Image(getClass().getResourceAsStream("/com/dungeon/assets/images/goblin.gif")));
            enemyImages.put(Enemy.EnemyType.SKELETON, new javafx.scene.image.Image(getClass().getResourceAsStream("/com/dungeon/assets/images/skeleton.gif")));
            enemyImages.put(Enemy.EnemyType.ORC, new javafx.scene.image.Image(getClass().getResourceAsStream("/com/dungeon/assets/images/orc.gif")));
            enemyImages.put(Enemy.EnemyType.MAGE, new javafx.scene.image.Image(getClass().getResourceAsStream("/com/dungeon/assets/images/mage.gif")));
            enemyImages.put(Enemy.EnemyType.BOSS, new javafx.scene.image.Image(getClass().getResourceAsStream("/com/dungeon/assets/images/boss.gif")));
        } catch (Exception e) {
            System.err.println("Error loading enemy images: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void onSceneReady() {
        // Setup input handling
        setupInputHandling();
        
        if (gameCanvas != null) {
            dungeonRenderer = new DungeonRenderer(gameCanvas);
            gc = gameCanvas.getGraphicsContext2D();
            lightingEffect = new LightingEffect(gameCanvas); // Initialize lighting effect
            if (rootPane != null) { // rootPane should also be available here
                 effectsManager = new EffectsManager(rootPane, gameCanvas);
            } else {
                 System.err.println("ERROR: rootPane is null in onSceneReady, effectsManager not fully initialized.");
            }
        } else {
            System.err.println("ERROR: gameCanvas is null in onSceneReady, cannot initialize gc, dungeonRenderer, lightingEffect or effectsManager.");
            // Consider returning or throwing an error to prevent further execution
            return; 
        }
        
        // Setup window resize handling
        handleResize();
        
        startTime = System.currentTimeMillis();
        setupGame();
        startGameLoop();
        soundManager.playSound("start");
        
        // Request focus on the canvas so it can receive key events
        if (gameCanvas != null) {
            gameCanvas.setFocusTraversable(true);
            gameCanvas.requestFocus();
        }
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
    allPuzzles = Puzzle.loadPuzzlesFromResources();
    Iterator<Puzzle> puzzleIterator = allPuzzles.iterator();

    for (DungeonRoom room : currentDungeon) {
        if (room.getType() == DungeonRoom.RoomType.PUZZLE) {
            if (!puzzleIterator.hasNext()) {
                // Restart iterator if run out of puzzles
                puzzleIterator = allPuzzles.iterator();
            }
            puzzles.put(room, puzzleIterator.next());
        }
    }
}

   private void setupInputHandling() {
        Scene scene = gameCanvas.getScene();
        activeKeys = new HashSet<>();
        
        // Make sure canvas can receive focus
        gameCanvas.setFocusTraversable(true);
        gameCanvas.requestFocus();
        System.out.println("Canvas focus traversable: " + gameCanvas.isFocusTraversable());
        System.out.println("Canvas focused: " + gameCanvas.isFocused());
        
        // Add focus listener to ensure canvas keeps focus
        gameCanvas.focusedProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("Canvas focus changed: " + oldVal + " -> " + newVal);
            if (!newVal) {
                gameCanvas.requestFocus();
            }
        });
        
        // Handle key presses on the canvas
        gameCanvas.setOnKeyPressed(e -> {
            if (roomTransitionInProgress || isInventoryOpen) return; // Ignore input during transition or if inventory is open
            
            KeyCode code = e.getCode();
            activeKeys.add(code);
            
            // Handle torch toggle
            if (code == KeyCode.V) {
                toggleTorch();
            }
            
            // Handle inventory key
            if (code == KeyCode.I) {
                openInventory();
            } else if (code == KeyCode.F) {  
                // Interact with puzzle or door
                if (currentRoom != null && currentRoom.getType() == DungeonRoom.RoomType.PUZZLE) {
                    interactWithPuzzle();
                } else {
                    // Try to interact with doors
                    checkDoorInteraction();
                }
            } else if (code == KeyCode.ESCAPE) {
                // Toggle pause menu
                togglePauseGame();
                e.consume(); // Consume the event to prevent default JavaFX full-screen exit
            } else if (code == KeyCode.W && activeKeys.contains(KeyCode.CONTROL)) {
                // Ctrl+W to change weather manually
                if (weatherSystem != null) {
                    WeatherSystem.WeatherType[] weathers = WeatherSystem.WeatherType.values();
                    WeatherSystem.WeatherType current = weatherSystem.getCurrentWeather();
                    int currentIndex = 0;
                    for (int i = 0; i < weathers.length; i++) {
                        if (weathers[i] == current) {
                            currentIndex = i;
                            break;
                        }
                    }
                    int nextIndex = (currentIndex + 1) % weathers.length;
                    weatherSystem.startWeatherTransition(weathers[nextIndex]);
                    effectsManager.showFloatingText("Weather: " + weathers[nextIndex].getName(), 
                        new Point2D(gameCanvas.getWidth() / 2, 100), Color.CYAN);
                }
            }
            
            // Handle projectile attack with E key
            if (code == KeyCode.E) {
                firePlayerProjectile();
            }
            
            // Handle melee attack with Space key
            if (code == KeyCode.SPACE) {
                attackEnemiesInRange();
            }
            
            // Handle weapon selection with number keys
            if (code.isDigitKey()) {
                try {
                    int weaponIndex = Integer.parseInt(code.getName()) - 1;
                    if (weaponIndex >= 0 && weaponIndex < 4) {
                        selectWeapon(weaponIndex);
                    }
                } catch (NumberFormatException ex) {
                    // Ignore parsing errors
                }
            }
        });
        
        gameCanvas.setOnKeyReleased(e -> {
            if (roomTransitionInProgress || isInventoryOpen) return; // Ignore input during transition or if inventory is open
            // System.out.println("Key released: " + e.getCode());
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
                if (isInventoryOpen) return; // Ignore input when inventory is open
                if (roomTransitionInProgress) return; // Ignore input during transition
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
        
        for (Enemy enemy : enemies) {
            if (isInMeleeRange(player, enemy)) {
                // Apply melee damage from player's equipped weapon
                double meleeDamage = player.getMeleeDamage(); 
                enemy.takeDamage(meleeDamage);
                soundManager.playSound("character");
                
                // Show damage text
                effectsManager.showFloatingText("-" + (int)meleeDamage, 
                    enemy.getPosition(), Color.ORANGE);
            }
        }
    }

    private void startGameLoop() {
        
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
        // System.out.println("UPDATE CALLED | gameLoopRunning=" + gameLoopRunning + 
        //                   " | roomTransitionInProgress=" + roomTransitionInProgress +
        //                   " | isInventoryOpen=" + isInventoryOpen);
                          
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
        
        // Skip update if room transition is in progress or inventory is open
        if (roomTransitionInProgress || isInventoryOpen) {
            return;
        }
        
        // Update weather system
       
        // Debug message for room type
        // String roomType = currentRoom.getType().toString();
        // System.out.println("Current room type: " + roomType + " | Enemies: " + enemies.size() + " | Items: " + roomItems.size() + " | Projectiles: " + playerProjectiles.size());
        
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

        // Check if we need to show the level up message
        if (justLeveledUp) {
            showLevelCompletionMessage();
            justLeveledUp = false;
        }
        
        // Check statue interaction in puzzle room
        checkStatueInteraction();
        
        // Check spike collision in treasure room
        checkSpikeCollision();
        
        // Update effects
        effectsManager.update(deltaTime);
    }
    
    private void checkItemPickups() {
        Iterator<Item> itemIterator = roomItems.iterator();
        boolean itemsPickedUp = false;
        while (itemIterator.hasNext()) {
            Item item = itemIterator.next();
            
            // Debug print
            // System.out.println("Checking item: " + item.getName() + " at " + item.getX() + "," + item.getY());
            // System.out.println("Player position: " + player.getX() + "," + player.getY());

            // Improved collision detection for items
            Point2D playerCenter = player.getPosition().add(player.getSize() / 2, player.getSize() / 2);
            Point2D itemCenter = new Point2D(item.getX(), item.getY());
            double distance = playerCenter.distance(itemCenter);

            // If player is close enough to item
            if (distance < (player.getSize() / 2 + item.getSize() / 2)) {
                // System.out.println("Player picked up: " + item.getName());
                soundManager.playSound("start"); // Changed from "character" to "start"
                
                // Handle item pickup based on type
                switch (item.getType()) {
                    case POTION:
                        // Calculate healing amount based on potion value
                        double healAmount = item.getValue();
                        double oldHealth = player.getHealth();
                        player.heal(healAmount);
                        double actualHeal = player.getHealth() - oldHealth;
                        
                        // Show healing effect with actual amount healed
                        effectsManager.showFloatingText("+" + (int)actualHeal + " HP", 
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
                
                // Track equipment collection in treasure room
                if (currentRoom != null && currentRoom.getType() == DungeonRoom.RoomType.TREASURE) {
                    equipmentCollected++;
                    System.out.println("Equipment collected: " + equipmentCollected + "/" + totalEquipmentInRoom);
                    
                    // Turn on lights when 2 out of 3 equipment items are collected
                    if (equipmentCollected >= 2 && !treasureRoomLighted) {
                        treasureRoomLighted = true;
                        
                        // Re-enable weather changes after collecting 2 equipment items
                        if (weatherSystem != null) {
                            weatherSystem.setWeatherChangesAllowed(true);
                        }
                        
                        effectsManager.showFloatingText("Lights restored!", 
                            new Point2D(gameCanvas.getWidth() / 2, gameCanvas.getHeight() / 2), Color.YELLOW);
                        soundManager.playSound("start");
                    }
                }
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
                        // Show simple "locked" message for all locked doors
                        Point2D textPosition = calculateSafeTextPosition(door);
                        effectsManager.showFloatingText("locked", textPosition, Color.RED);
                    }
                    // If door is unlocked, the transition will be handled in checkRoomTransition
                }
            }
            // Remove the key press to prevent multiple interactions
            activeKeys.remove(KeyCode.F);
        }
    }
    
    private Point2D calculateSafeTextPosition(Door door) {
        double textX = door.getX() + door.getWidth() / 2;
        double textY = door.getY() - 20; // Default position above door
        
        // Ensure text stays within canvas bounds
        double minY = 30; // Minimum Y to keep text visible
        double maxY = gameCanvas.getHeight() - 30; // Maximum Y to keep text visible
        
        if (textY < minY) {
            textY = door.getY() + door.getHeight() + 20; // Show below door instead
        } else if (textY > maxY) {
            textY = door.getY() - 20; // Keep above door
        }
        
        return new Point2D(textX, textY);
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
        // System.out.println("Checking room clear conditions - Room type: " + currentRoom.getType() + 
        //                   ", Rooms cleared: " + roomsClearedInLevel + 
        //                   ", Current level: " + currentLevel + 
        //                   ", awaiting level up: " + awaitingLevelUp);
        switch (currentRoom.getType()) {
            case COMBAT:
                if (enemies.isEmpty() || enemies.stream().allMatch(enemy -> enemy.getHealth() <= 0)) {
                    if (currentWave < totalWaves) {
                        currentWave++;
                        effectsManager.showFloatingText("Second Wave!", new Point2D(gameCanvas.getWidth() / 2, 80), Color.GOLD);
                        spawnCombatWave(currentWave);
                    } else {
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
                            System.out.println("Created spawn door at: " + doorX + "," + doorY);
                            }
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
        
        // Draw statue in puzzle room
        if (currentRoom != null && currentRoom.getType() == DungeonRoom.RoomType.PUZZLE && statueVisible && statueImage != null) {
            gc.drawImage(statueImage, 
                statuePosition.getX() - STATUE_SIZE/2, 
                statuePosition.getY() - STATUE_SIZE/2, 
                STATUE_SIZE, STATUE_SIZE);
        }
        
        // Apply darkness overlay for treasure room when not lighted
        if (currentRoom != null && currentRoom.getType() == DungeonRoom.RoomType.TREASURE && !treasureRoomLighted) {
            // Apply a dark overlay similar to weather darkness effect
            // This makes the room completely dark except for the torch light area
            gc.setFill(Color.rgb(0, 0, 0, 1.0)); // 100% opacity black overlay - completely invisible
            gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());
            
            // If torch is active, draw the light area on top of the darkness
            if (torchActive && player != null) {
                Point2D lightCenter = new Point2D(player.getPosition().getX() + player.getSize()/2, 
                                                 player.getPosition().getY() + player.getSize()/2);
                
                // Draw the torch light area with a radial gradient effect
                // Use multiple circles with decreasing opacity to create a realistic light effect
                
                // Outer circle (dim light)
                gc.setFill(Color.rgb(255, 255, 200, 0.1));
                gc.fillOval(
                    lightCenter.getX() - TORCH_LIGHT_RADIUS,
                    lightCenter.getY() - TORCH_LIGHT_RADIUS,
                    TORCH_LIGHT_RADIUS * 2,
                    TORCH_LIGHT_RADIUS * 2
                );
                
                // Draw middle circle (medium light)
                gc.setFill(Color.rgb(255, 255, 200, 0.3));
                gc.fillOval(
                    lightCenter.getX() - TORCH_LIGHT_RADIUS * 0.7,
                    lightCenter.getY() - TORCH_LIGHT_RADIUS * 0.7,
                    TORCH_LIGHT_RADIUS * 1.4,
                    TORCH_LIGHT_RADIUS * 1.4
                );
                
                // Draw inner circle (bright light)
                gc.setFill(Color.rgb(255, 255, 200, 0.5));
                gc.fillOval(
                    lightCenter.getX() - TORCH_LIGHT_RADIUS * 0.3,
                    lightCenter.getY() - TORCH_LIGHT_RADIUS * 0.3,
                    TORCH_LIGHT_RADIUS * 0.6,
                    TORCH_LIGHT_RADIUS * 0.6
                );
            }
        }
        
        // Draw the room doors with animations AFTER darkness overlay (so they're visible in torch light)
        renderDoorsWithAnimations(gc);
        
        // Draw torch in treasure room AFTER darkness overlay (so it's visible in dark room)
        if (currentRoom != null && currentRoom.getType() == DungeonRoom.RoomType.TREASURE && torchActive && torchImage != null && player != null) {
            // Draw torch near player
            Point2D playerPos = player.getPosition();
            gc.drawImage(torchImage, 
                playerPos.getX() + player.getSize() - TORCH_SIZE/2, 
                playerPos.getY() - TORCH_SIZE/2, 
                TORCH_SIZE, TORCH_SIZE);
        }
        
        // Draw room items AFTER darkness overlay (so they're visible in torch light)
        for (Item item : roomItems) {
            // In treasure room, only show items if torch is active and item is within light radius
            if (currentRoom != null && currentRoom.getType() == DungeonRoom.RoomType.TREASURE && !treasureRoomLighted) {
                if (!torchActive || player == null) {
                    continue; // Skip rendering if torch is not active
                }
                
                Point2D lightCenter = new Point2D(player.getPosition().getX() + player.getSize()/2, 
                                                 player.getPosition().getY() + player.getSize()/2);
                double distance = lightCenter.distance(new Point2D(item.getX(), item.getY()));
                
                if (distance > TORCH_LIGHT_RADIUS) {
                    continue; // Skip rendering if item is outside light radius
                }
            }
            
            javafx.scene.image.Image itemImage = null;
            switch (item.getType()) {
                case POTION:
                    itemImage = new javafx.scene.image.Image(getClass().getResourceAsStream("/com/dungeon/assets/images/potion.png"));
                    break;
                case ARMOR:
                    itemImage = new javafx.scene.image.Image(getClass().getResourceAsStream("/com/dungeon/assets/images/armor.png"));
                    break;
                case KEY:
                    itemImage = new javafx.scene.image.Image(getClass().getResourceAsStream("/com/dungeon/assets/images/key.png"));
                    break;
                case WEAPON:
                    if (item instanceof Weapon) {
                        Weapon.WeaponType type = ((Weapon) item).getWeaponType();
                        itemImage = weaponImages.get(type);
                    }
                    break;
                default:
                    break;
            }
            double renderItemSize = item.getSize() * 3.0;
            if (itemImage != null) {
                gc.drawImage(itemImage, item.getX() - renderItemSize/2, item.getY() - renderItemSize/2, renderItemSize, renderItemSize);
            } else {
                // Fallback: draw as a circle
            gc.setFill(getItemColor(item.getType()));
                gc.fillOval(item.getX() - renderItemSize/2, item.getY() - renderItemSize/2, renderItemSize, renderItemSize);
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(1);
                gc.strokeOval(item.getX() - renderItemSize/2, item.getY() - renderItemSize/2, renderItemSize, renderItemSize);
            }
            // Draw item name when player is close
            if (player != null && player.getPosition().distance(new Point2D(item.getX(), item.getY())) < 100) {
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("Verdana", FontWeight.NORMAL, 10));
                gc.setTextAlign(TextAlignment.CENTER);
                gc.fillText(item.getName(), item.getX(), item.getY() - renderItemSize/2 - 10);
            }
        }
        
        // Draw spikes in treasure room
        if (currentRoom != null && currentRoom.getType() == DungeonRoom.RoomType.TREASURE && !treasureRoomLighted) {
            Point2D playerPos = player != null ? player.getPosition() : null;
            
            for (Point2D spikePos : spikePositions) {
                // Only show spikes if torch is active and spike is within light radius
                if (torchActive && playerPos != null) {
                    Point2D lightCenter = new Point2D(playerPos.getX() + player.getSize()/2, 
                                                     playerPos.getY() + player.getSize()/2);
                    double distance = lightCenter.distance(spikePos);
                    
                    if (distance <= TORCH_LIGHT_RADIUS) {
                        // Draw spike as a red triangle
                        gc.setFill(Color.RED);
                        gc.setStroke(Color.DARKRED);
                        gc.setLineWidth(2);
                        
                        double[] xPoints = {spikePos.getX(), spikePos.getX() - SPIKE_SIZE/2, spikePos.getX() + SPIKE_SIZE/2};
                        double[] yPoints = {spikePos.getY() - SPIKE_SIZE/2, spikePos.getY() + SPIKE_SIZE/2, spikePos.getY() + SPIKE_SIZE/2};
                        
                        gc.fillPolygon(xPoints, yPoints, 3);
                        gc.strokePolygon(xPoints, yPoints, 3);
                    }
                }
            }
        }
        
        // Draw enemies
        if (enemies.isEmpty()) {
            // System.out.println("No enemies to render in current room");
        } else {
            // System.out.println("Rendering " + enemies.size() + " enemies");
            for (Enemy enemy : enemies) {
                // System.out.println("Rendering enemy at: " + enemy.getX() + "," + enemy.getY() + " of type: " + enemy.getType());
                
                // Get the enemy image
                javafx.scene.image.Image enemyImage = enemyImages.get(enemy.getType());
                if (enemyImage != null) {
                    // Calculate enemy size
                    double size = enemy.getSize();
                    // Increase size by 2x for normal enemies, 3x for boss
                if (enemy.getType() == Enemy.EnemyType.BOSS) {
                        size *= 3.0;
                    } else {
                        size *= 2.0;
                    }
                    // Calculate horizontal flip based on movement direction (like player)
                    double scaleX = 1;
                    Point2D velocity = enemy.getVelocity();
                    if (velocity != null && velocity.getX() < -0.1) {
                        scaleX = -1; // Flip horizontally if moving left
                    }
                    
                    // Save the current graphics context state
                    gc.save();
                    
                    // Translate to enemy center
                    gc.translate(enemy.getX() + enemy.getSize()/2, enemy.getY() + enemy.getSize()/2);
                    gc.scale(scaleX, 1);
                    
                    // Draw enemy image centered
                    gc.drawImage(enemyImage, -size/2, -size/2, size, size);
                    
                    // Restore the graphics context state
                    gc.restore();
                    
                    // Draw health bar above enemy's head
                    double healthBarWidth = size * 0.8; // Slightly smaller than enemy width
                    double healthBarHeight = 5;
                    double healthBarY = enemy.getY() - size/2 - 15; // Position above enemy
                    double healthBarX = enemy.getX() - healthBarWidth/2;
                    
                    // Health bar background
                gc.setFill(Color.BLACK);
                    gc.fillRect(healthBarX, healthBarY, healthBarWidth, healthBarHeight);
                    
                    // Health bar foreground
                    double healthPercentage = (double) enemy.getHealth() / enemy.getMaxHealth();
                gc.setFill(Color.GREEN);
                    gc.fillRect(healthBarX, healthBarY, healthBarWidth * healthPercentage, healthBarHeight);
                
                    // Draw enemy type above health bar
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("Verdana", FontWeight.NORMAL, 10));
                gc.setTextAlign(TextAlignment.CENTER); // Center align text
                gc.fillText(enemy.getType().toString(), 
                    enemy.getX() + enemy.getSize()/2, // Center X position
                    healthBarY - 5); // Position above health bar
                } else {
                    // Fallback to colored rectangle if image not found
                    if (enemy.getType() == Enemy.EnemyType.BOSS) {
                        gc.setFill(Color.DARKRED);
                        gc.fillRect(enemy.getX() - 5, enemy.getY() - 5, enemy.getSize() * 3.0 + 10, enemy.getSize() * 3.0 + 10);
                    } else {
                        gc.setFill(Color.RED);
                        gc.fillRect(enemy.getX(), enemy.getY(), enemy.getSize() * 2.0, enemy.getSize() * 2.0);
                    }
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
        
        // Render weather effects (on top of everything)
        if (weatherSystem != null) {
            weatherSystem.render(gc, gameCanvas.getWidth(), gameCanvas.getHeight());
        }
        
        // Draw UI elements
        renderUI(gc);
    }
    
    private void drawRoomBackground(GraphicsContext gc) {
        if (!backgroundsLoaded) {
            // Load background images once
            combatBgImage = new Image(getClass().getResourceAsStream("/com/dungeon/assets/images/combat.jpg"));
            treasureBgImage = new Image(getClass().getResourceAsStream("/com/dungeon/assets/images/treasure.jpg"));
            puzzleBgImage = new Image(getClass().getResourceAsStream("/com/dungeon/assets/images/puzzle.jpg"));
            spawnBgImage = new Image(getClass().getResourceAsStream("/com/dungeon/assets/images/spawn.jpg"));
            bossBgImage = new Image(getClass().getResourceAsStream("/com/dungeon/assets/images/boss.jpg"));
            statueImage = new Image(getClass().getResourceAsStream("/com/dungeon/assets/images/statue.gif"));
            torchImage = new Image(getClass().getResourceAsStream("/com/dungeon/assets/images/torch.gif"));
            backgroundsLoaded = true;
        }
        if (currentRoom == null) {
            System.err.println("ERROR: Cannot draw room background - currentRoom is null");
            gc.setFill(Color.BLACK);
            gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());
            return;
        }
        // Draw the background image for the room type
        Image bgImage = null;
        switch (currentRoom.getType()) {
            case COMBAT:
                bgImage = combatBgImage;
                break;
            case TREASURE:
                // Only show background if room is lighted, otherwise keep it dark
                if (treasureRoomLighted) {
                bgImage = treasureBgImage;
                }
                break;
            case PUZZLE:
                bgImage = puzzleBgImage;
                break;
            case SPAWN:
                bgImage = spawnBgImage;
                break;
            case BOSS:
                bgImage = bossBgImage;
                break;
            default:
                break;
        }
        if (bgImage != null) {
            gc.drawImage(bgImage, 0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());
        } else {
            // Fallback to color for other room types or dark treasure room
            Color baseColor;
            if (currentRoom.getType() == DungeonRoom.RoomType.TREASURE && !treasureRoomLighted) {
                baseColor = Color.BLACK; // Completely dark for treasure room
            } else {
        switch (currentRoom.getType()) {
                case SPAWN:
                    baseColor = Color.rgb(0, 80, 0); // Dark green for spawn
                break;
            case BOSS:
                    baseColor = Color.rgb(80, 0, 0); // Deep red for boss
                break;
            default:
                    baseColor = Color.rgb(30, 30, 30); // Dark gray default
                break;
                }
        }
            gc.setFill(baseColor);
            gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());
        }
        // Draw room borders for all rooms
        gc.setStroke(Color.DARKGRAY);
        gc.setLineWidth(5);
        gc.strokeRect(10, 10, gameCanvas.getWidth() - 20, gameCanvas.getHeight() - 20);
    }
    
      private void renderUI(GraphicsContext gc) {
        // Set font for UI elements
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 14));
        
        // Create a semi-transparent background for the stats
        double padding = 10;
        double boxWidth = 200;
        double boxHeight = 140; // Increased from 120 to accommodate weather text
        double boxX = padding;
        double boxY = padding;
        
        // Draw background box
        gc.setFill(Color.rgb(0, 0, 0, 0.7));
        gc.fillRect(boxX, boxY, boxWidth, boxHeight);
        
        // Draw border around the box
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);
        gc.strokeRect(boxX, boxY, boxWidth, boxHeight);
        
        // Health bar background
        double healthBarX = boxX + padding;
        double healthBarY = boxY + padding;
        double healthBarWidth = boxWidth - (padding * 2);
        double healthBarHeight = 15;
        
        gc.setFill(Color.DARKRED);
        gc.fillRect(healthBarX, healthBarY, healthBarWidth, healthBarHeight);
        
        // Health bar foreground with smooth color transition
        double healthPercentage = player.getHealth() / player.getMaxHealth();
        Color healthColor;
        if (healthPercentage > 0.6) {
            healthColor = Color.GREEN;
        } else if (healthPercentage > 0.3) {
            healthColor = Color.ORANGE;
        } else {
            healthColor = Color.RED;
        }
        gc.setFill(healthColor);
        gc.fillRect(healthBarX, healthBarY, healthBarWidth * healthPercentage, healthBarHeight);
        
        // Health text
        gc.setFill(Color.WHITE);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText("HP: " + (int)player.getHealth() + "/" + (int)player.getMaxHealth(), 
            healthBarX, healthBarY + healthBarHeight + 15);
        
        // Score
        gc.fillText("Score: " + (int)player.getScore(), 
            healthBarX, healthBarY + healthBarHeight + 35);
        
        // Level
        gc.fillText("Level: " + currentLevel, 
            healthBarX, healthBarY + healthBarHeight + 55);
        
        // Enemies defeated
        gc.fillText("Enemies Defeated: " + enemiesDefeated, 
            healthBarX, healthBarY + healthBarHeight + 75);
        
        // Weather display
        if (weatherSystem != null) {
            String weatherName = weatherSystem.getWeatherName();
            gc.setFill(Color.CYAN);
            gc.fillText("Weather: " + weatherName, 
                healthBarX, healthBarY + healthBarHeight + 95);
        }
        
        // Add torch controls for treasure room only
        if (currentRoom != null && currentRoom.getType() == DungeonRoom.RoomType.TREASURE) {
            gc.setFill(Color.YELLOW);
            gc.fillText("V: Toggle Torch | Torch: " + (torchActive ? "ON" : "OFF"), 
                padding, gameCanvas.getHeight() - 20);
        }
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
                justLeveledUp = true; // Set flag to show message after transition
                
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
                    door.setLocked(true);
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

    private void populateRoom(DungeonRoom room) {
        // Clear existing entities
        enemies.clear();
        roomItems.clear();
        roomCleared = false;
        
        // Get room center
        double centerX = gameCanvas.getWidth() / 2;
        double centerY = gameCanvas.getHeight() / 2;
        Point2D roomCenter = new Point2D(centerX, centerY);
        double minX = 40, minY = 40;
        double maxX = gameCanvas.getWidth() - 40;
        double maxY = gameCanvas.getHeight() - 40;
        
        // Adjust difficulty based on level
        double difficultyMultiplier = 1.0 + (currentLevel - 1) * 0.2;
        
        switch (room.getType()) {
            case PUZZLE:
                System.out.println("Populating puzzle room...");
                // Create a new puzzle for this room
                Puzzle puzzle = Puzzle.createRandomPuzzle();
                puzzles.put(room, puzzle);
                System.out.println("Created puzzle: " + puzzle.getDescription());
                
                // Initialize statue in the center of the room
                statueVisible = true;
                statuePosition = new Point2D(gameCanvas.getWidth() / 2, gameCanvas.getHeight() / 2); // Move 50 pixels to the left
                System.out.println("Statue placed at: " + statuePosition);
                
                // Only place key and random item if puzzle is already solved
                if (puzzle.isSolved()) {
                    // Place key in puzzle room
                    placeKeyInRoom(room);
                    
                    // Place one random item in puzzle room
                    double angle = random.nextDouble() * Math.PI * 2;
                    double distance = 100;
                    double offsetX = Math.cos(angle) * distance;
                    double offsetY = Math.sin(angle) * distance;
                    spawnRandomItem(roomCenter.add(offsetX, offsetY));
                }
                break;
                
            case TREASURE:
                System.out.println("Populating treasure room...");
                
                // Force clear weather and disable weather changes for treasure room until equipment is collected
                if (weatherSystem != null) {
                    weatherSystem.setWeatherChangesAllowed(false);
                    // Force clear weather immediately
                    weatherSystem.startWeatherTransition(WeatherSystem.WeatherType.CLEAR);
                }
                
                // Reset treasure room state
                equipmentCollected = 0;
                totalEquipmentInRoom = 3; // Always spawn 3 equipment items
                treasureRoomLighted = false;
                torchActive = false;
                
                // Generate spikes and safe areas
                generateTreasureRoomLayout();
                
                // Spawn equipment items in safe areas
                spawnTreasureRoomEquipment();
                
                break;
                
            case COMBAT:
                currentWave = 1;
                spawnCombatWave(currentWave);
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
        }
        
        // Create doors for the new room
        createDoors();
    }

    private void spawnCombatWave(int wave) {
        System.out.println("Spawning wave " + wave + " in combat room for level " + currentLevel);
        double centerX = gameCanvas.getWidth() / 2;
        double centerY = gameCanvas.getHeight() / 2;
        double minX = 40, minY = 40;
        double maxX = gameCanvas.getWidth() - 40;
        double maxY = gameCanvas.getHeight() - 40;
        double difficultyMultiplier = 1.0 + (currentLevel - 1) * 0.2;
        java.util.List<Point2D> spawnPoints = new java.util.ArrayList<>();
        spawnPoints.add(new Point2D(minX, minY));
        spawnPoints.add(new Point2D(maxX, minY));
        spawnPoints.add(new Point2D(minX, maxY));
        spawnPoints.add(new Point2D(maxX, maxY));
        int numEnemies = 0;
        switch (currentLevel) {
            case 1: numEnemies = 4; break;
            case 2: numEnemies = 5; break;
            case 3: numEnemies = 7; break;
            default: numEnemies = 5 + (currentLevel - 3); break;
        }
        while (spawnPoints.size() < numEnemies) {
            double rx = minX + random.nextDouble() * (maxX - minX);
            double ry = minY + random.nextDouble() * (maxY - minY);
            spawnPoints.add(new Point2D(rx, ry));
        }
        int spawnIdx = 0;
        switch (currentLevel) {
            case 1:
                spawnEnemyWithType(spawnPoints.get(spawnIdx++), Enemy.EnemyType.SKELETON, difficultyMultiplier);
                spawnEnemyWithType(spawnPoints.get(spawnIdx++), Enemy.EnemyType.SKELETON, difficultyMultiplier);
                spawnEnemyWithType(spawnPoints.get(spawnIdx++), Enemy.EnemyType.GOBLIN, difficultyMultiplier);
                spawnEnemyWithType(spawnPoints.get(spawnIdx++), Enemy.EnemyType.ORC, difficultyMultiplier);
                break;
            case 2:
                spawnEnemyWithType(spawnPoints.get(spawnIdx++), Enemy.EnemyType.GOBLIN, difficultyMultiplier);
                spawnEnemyWithType(spawnPoints.get(spawnIdx++), Enemy.EnemyType.GOBLIN, difficultyMultiplier);
                spawnEnemyWithType(spawnPoints.get(spawnIdx++), Enemy.EnemyType.SKELETON, difficultyMultiplier);
                spawnEnemyWithType(spawnPoints.get(spawnIdx++), Enemy.EnemyType.ORC, difficultyMultiplier);
                spawnEnemyWithType(spawnPoints.get(spawnIdx++), Enemy.EnemyType.ORC, difficultyMultiplier);
                break;
            case 3:
                spawnEnemyWithType(spawnPoints.get(spawnIdx++), Enemy.EnemyType.GOBLIN, difficultyMultiplier);
                spawnEnemyWithType(spawnPoints.get(spawnIdx++), Enemy.EnemyType.GOBLIN, difficultyMultiplier);
                spawnEnemyWithType(spawnPoints.get(spawnIdx++), Enemy.EnemyType.GOBLIN, difficultyMultiplier);
                spawnEnemyWithType(spawnPoints.get(spawnIdx++), Enemy.EnemyType.MAGE, difficultyMultiplier);
                spawnEnemyWithType(spawnPoints.get(spawnIdx++), Enemy.EnemyType.MAGE, difficultyMultiplier);
                spawnEnemyWithType(spawnPoints.get(spawnIdx++), Enemy.EnemyType.ORC, difficultyMultiplier);
                spawnEnemyWithType(spawnPoints.get(spawnIdx++), Enemy.EnemyType.ORC, difficultyMultiplier);
                break;
            default:
                Enemy.EnemyType[] types = {Enemy.EnemyType.GOBLIN, Enemy.EnemyType.SKELETON, Enemy.EnemyType.ORC, Enemy.EnemyType.MAGE};
                for (int i = 0; i < numEnemies; i++) {
                    Enemy.EnemyType randomType = types[random.nextInt(types.length)];
                    spawnEnemyWithType(spawnPoints.get(i), randomType, difficultyMultiplier);
                }
                break;
        }
    }

    private void spawnEnemyWithType(Point2D roomCenter, Enemy.EnemyType type, double difficultyMultiplier) {
        // Calculate random position around the room center
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = 100 + random.nextDouble() * 50;
        double enemyX = roomCenter.getX() + Math.cos(angle) * distance;
        double enemyY = roomCenter.getY() + Math.sin(angle) * distance;
                    
        // Create enemy with specified type
        Enemy enemy = new Enemy(enemyX, enemyY, type);
        
        // Apply difficulty scaling
        enemy.setMaxHealth(enemy.getMaxHealth() * difficultyMultiplier);
        enemy.setDamage(enemy.getDamage() * difficultyMultiplier);
        
        // Increase speed based on level - using velocity instead of speed
        double speedMultiplier = 1.0 + (currentLevel - 1) * 0.3; // 30% speed increase per level
        Point2D currentVelocity = enemy.getVelocity();
        if (currentVelocity != null) {
            enemy.setVelocity(currentVelocity.multiply(speedMultiplier));
        }
        
        enemy.heal(enemy.getMaxHealth());
        
        // Add to enemies list
        enemies.add(enemy);
        
        System.out.println("Spawned " + type + " at position: " + enemyX + "," + enemyY + " with speed multiplier: " + speedMultiplier);
    }

    private void spawnWeapon(Point2D position) {
        // Randomly select a weapon type
        Weapon.WeaponType[] weaponTypes = Weapon.WeaponType.values();
        Weapon.WeaponType randomType = weaponTypes[(int)(Math.random() * weaponTypes.length)];
        
        // Get base damage for the specific weapon type
        int baseDamage;
        switch (randomType) {
            case DAGGER: baseDamage = 17; break;  // 17 * 0.7 = 12 damage
            case SPEAR: baseDamage = 18; break;   // 18 * 0.9 = 16 damage  
            case SWORD: baseDamage = 20; break;   // 20 * 1.0 = 20 damage
            case BOW: baseDamage = 23; break;     // 23 * 0.8 = 18 damage
            case AXE: baseDamage = 19; break;     // 19 * 1.3 = 25 damage
            default: baseDamage = 20; break;
        }
        
        // Create weapon with matching name and type
        Weapon weapon = new Weapon(
            randomType.name(), // Use enum name as weapon name
            "A " + randomType.name().toLowerCase() + " for combat", // Basic description
            baseDamage, // Use weapon-specific base damage
            randomType,
            false // not a puzzle reward
        );
        
        // Set position and size
        weapon.setX(position.getX());
        weapon.setY(position.getY());
        weapon.setSize(20);
        
        roomItems.add(weapon);
    }
    
        private void spawnArmor(Point2D position) {
        // Randomly select an armor type
        Armor.ArmorType[] armorTypes = Armor.ArmorType.values();
        Armor.ArmorType randomType = armorTypes[(int)(Math.random() * armorTypes.length)];
        
        // Create armor with matching name and type
        Armor armor = new Armor(
            randomType.name(), // Use enum name as armor name
            "A " + randomType.name().toLowerCase() + " for protection", // Basic description
            randomType // Corrected: removed the baseDefense integer
        );
        
        // Set position and size
        armor.setX(position.getX());
        armor.setY(position.getY());
        armor.setSize(20);
        
        roomItems.add(armor);
    }
    
    private void spawnPotion(Point2D position) {
        String[] potionTypes = {"Health", "Strength", "Speed"};
        String type = potionTypes[(int)(Math.random() * potionTypes.length)];
        String name = type + " Potion";
        String description = "A potion that restores " + type.toLowerCase() + ".";
        int value = 20;
        
        Item potion = new Item(name, description, Item.ItemType.POTION, value, true);
        potion.setX(position.getX());
        potion.setY(position.getY());
        potion.setSize(20);
        
        roomItems.add(potion);
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

    private boolean isInMeleeRange(Entity attacker, Entity target) {
        double attackRange = 40; // Melee attack range
        
        Point2D attackerCenter = attacker.getPosition().add(new Point2D(attacker.getSize() / 2, attacker.getSize() / 2));
        Point2D targetCenter = target.getPosition().add(new Point2D(target.getSize() / 2, target.getSize() / 2));
        
        return attackerCenter.distance(targetCenter) < (attacker.getSize() / 2 + target.getSize() / 2 + attackRange);
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
                        Point2D textPosition = calculateSafeTextPosition(door);
                        effectsManager.showFloatingText("locked", textPosition, Color.YELLOW);
                        
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
                        // Door is locked for other reasons - show simple "locked" message
                        Point2D textPosition = calculateSafeTextPosition(door);
                        effectsManager.showFloatingText("locked", textPosition, Color.RED);
                    }
                }
            }
        }
    }

    private void transitionToRoom(DungeonRoom newRoom, Point2D entryPosition) {
        System.out.println("Transitioning to room: " + newRoom.getType());
        
        // Hide statue when leaving puzzle room
        if (currentRoom != null && currentRoom.getType() == DungeonRoom.RoomType.PUZZLE) {
            statueVisible = false;
        }
        
        // Re-enable weather changes when leaving treasure room
        if (currentRoom != null && currentRoom.getType() == DungeonRoom.RoomType.TREASURE) {
            if (weatherSystem != null) {
                weatherSystem.setWeatherChangesAllowed(true);
            }
        }

        soundManager.stopSound("running");
        activeKeys.clear(); // Clear any "stuck" keys
        roomTransitionInProgress = true;
        
        // Play transition sound
        soundManager.playSound("teleport");
        
        // Start room transition effect
        effectsManager.startRoomTransition(newRoom.getType(), 
            () -> {
                // This runs after the fade-in (screen is black)
                currentRoom = newRoom;
                currentRoom.setVisited(true);
                
                // Set player position
                player.setPosition(entryPosition.getX(), entryPosition.getY());
                
                // Clear existing room data
                enemies.clear();
                projectiles.clear();
                roomItems.clear();
                doors.clear();
                
                // Reset statue state
                statueVisible = false;
                
                // Reset treasure room state
                torchActive = false;
                treasureRoomLighted = false;
                equipmentCollected = 0;
                spikePositions.clear();
                safePositions.clear();
                
                // Reset room cleared state
                roomCleared = false;
                
                // Populate new room
                populateRoom(currentRoom);
                
                // Create doors for the new room
                createDoors();
                
                // Update minimap
                updateMinimap();
                
                // Force a render to ensure all text positions are updated
                render();
                
                // Ensure canvas has focus
                gameCanvas.requestFocus();
            },
            () -> {
                // This runs after the fade-out is complete
                roomTransitionInProgress = false;
            }
        );
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
        enemiesToBounceBack.clear(); // Clear at the start of each update
        // Update each enemy in the room
        Iterator<Enemy> enemyIterator = enemies.iterator();
        while (enemyIterator.hasNext()) {
            Enemy enemy = enemyIterator.next();
            // Update enemy state
            enemy.update(deltaTime);
            
            // Update attack cooldown
            Double cooldown = enemyAttackCooldowns.getOrDefault(enemy, 0.0);
            if (cooldown > 0) {
                enemyAttackCooldowns.put(enemy, cooldown - deltaTime);
            }
            
            // Skip if enemy is dead
            if (enemy.getHealth() <= 0) {
                enemyAttackCooldowns.remove(enemy);
                onEnemyDefeated(enemy);
                enemyIterator.remove();
                continue;
            }
            
            // Get distance to player
            Point2D playerCenter = player.getPosition().add(player.getSize() / 2, player.getSize() / 2);
            Point2D enemyCenter = enemy.getPosition().add(enemy.getSize() / 2, enemy.getSize() / 2);
            double distanceToPlayer = playerCenter.distance(enemyCenter);
            
            // Check if enemy is in bounce-back state
            boolean isBouncingBack = cooldown > 0;
            
            // Only non-boss, non-mage enemies bounce back if they were actually hit
            if (enemiesToBounceBack.contains(enemy)) {
                // Move enemy away from player during bounce-back
                Point2D bounceDirection = enemyCenter.subtract(playerCenter).normalize();
                double bounceSpeed = 50 * 0.5; // Reduced base speed during bounce
                double dx = bounceDirection.getX() * bounceSpeed * deltaTime;
                double dy = bounceDirection.getY() * bounceSpeed * deltaTime;
                
                Point2D newPosition = new Point2D(
                    enemy.getPosition().getX() + dx,
                    enemy.getPosition().getY() + dy
                );
                
                // Add boundary checking for enemies
                double minX = 10;
                double minY = 10;
                double maxX = gameCanvas.getWidth() - enemy.getSize() - 10;
                double maxY = gameCanvas.getHeight() - enemy.getSize() - 10;
                
                // Ensure enemy stays within bounds
                newPosition = new Point2D(
                    Math.max(minX, Math.min(maxX, newPosition.getX())),
                    Math.max(minY, Math.min(maxY, newPosition.getY()))
                );
                
                enemy.setPosition(newPosition);
            } else {
                // Normal movement logic
                boolean shouldMoveTowardsPlayer = false;
                if (enemy.getType() == Enemy.EnemyType.BOSS) {
                    shouldMoveTowardsPlayer = true; // Boss always moves towards player if not bouncing back
                } else {
                    if (distanceToPlayer > enemy.getSize() + player.getSize() + 5) {
                        shouldMoveTowardsPlayer = true; // Other enemies move if far enough
                    }
                }

                if (shouldMoveTowardsPlayer) {
                    Point2D normalizedDirection = playerCenter.subtract(enemyCenter).normalize();
                    Point2D effectiveMovementDirection = normalizedDirection; // Start with normalized

                    double baseSpeed = 50; // Base speed for enemies
                    double actualSpeed = baseSpeed * (1.0 + (currentLevel - 1) * 0.3); // Scale speed with level

                    // Apply weather effects to enemy speed
                    if (weatherSystem != null) {
                        actualSpeed *= weatherSystem.getEnemySpeedMultiplier();
                    }

                    if (enemy.getType() == Enemy.EnemyType.BOSS) {
                        actualSpeed *= 0.7; // Boss specific speed multiplier (reduced from 1.5 to 1.0)
                        // Apply the "stronger tendency" by scaling the direction vector (reduced from 1.5 to 1.0)
                        effectiveMovementDirection = normalizedDirection.multiply(1.0);
                    }

                    // Set velocity for proper rotation (uses effectiveMovementDirection which might be non-unit for boss)
                    enemy.setVelocity(effectiveMovementDirection.multiply(actualSpeed));

                    // Actual movement based on dx, dy, using the 0.3 factor from original logic
                    double dx = effectiveMovementDirection.getX() * actualSpeed * 0.3 * deltaTime;
                    double dy = effectiveMovementDirection.getY() * actualSpeed * 0.3 * deltaTime;

                    Point2D newPosition = new Point2D(
                        enemy.getPosition().getX() + dx,
                        enemy.getPosition().getY() + dy
                    );

                    // Boundary checking for enemies
                    double minX = 10;
                    double minY = 10;
                    double maxX = gameCanvas.getWidth() - enemy.getSize() - 10;
                    double maxY = gameCanvas.getHeight() - enemy.getSize() - 10;

                    // Ensure enemy stays within bounds
                    newPosition = new Point2D(
                        Math.max(minX, Math.min(maxX, newPosition.getX())),
                        Math.max(minY, Math.min(maxY, newPosition.getY()))
                    );
                    enemy.setPosition(newPosition);
                } else {
                    // This 'else' now only applies to non-boss enemies that are too close
                    // For non-boss enemies close to the player, maintain direction for rotation
                    Point2D direction = playerCenter.subtract(enemyCenter).normalize();
                    enemy.setVelocity(direction.multiply(0.1)); // Small velocity to maintain direction
                }
                
                // Boss projectile attack logic
                if (enemy.getType() == Enemy.EnemyType.BOSS && cooldown <= 0) {
                    // Boss shoots projectiles at player
                    Point2D direction = playerCenter.subtract(enemyCenter).normalize();
                    double projectileDamage = enemy.getDamage() * 0.5; // Projectile does half of melee damage
                    
                    // Create projectile with correct constructor parameters
                    Projectile bossProjectile = new Projectile(
                        enemyCenter.getX(), // x position
                        enemyCenter.getY(), // y position
                        direction.getX() * 200, // velocity x
                        direction.getY() * 200, // velocity y
                        10, // size
                        projectileDamage, // damage
                        Color.RED, // color
                        false // not from player
                    );
                    projectiles.add(bossProjectile);
                    
                    // Set attack cooldown
                    enemyAttackCooldowns.put(enemy, ATTACK_COOLDOWN * 0.5); // Boss attacks more frequently
                    
                    // Remove sound effect for boss projectiles
                    // soundManager.playSound("character");
                }
                // MAGE projectile attack logic
                if (enemy.getType() == Enemy.EnemyType.MAGE && cooldown <= 0) {
                    // Mage shoots projectiles at player
                    Point2D direction = playerCenter.subtract(enemyCenter).normalize();
                    double projectileDamage = 8.0; // Fixed damage for mage projectiles
                    
                    Projectile mageProjectile = new Projectile(
                        enemyCenter.getX(),
                        enemyCenter.getY(),
                        direction.getX() * 180, // slightly slower than boss
                        direction.getY() * 180,
                        8, // size
                        projectileDamage,
                        Color.PURPLE, // mage color
                        false
                    );
                    projectiles.add(mageProjectile);
                    
                    // Set attack cooldown for mage
                    enemyAttackCooldowns.put(enemy, ATTACK_COOLDOWN); // Normal cooldown
                }
                // Enemy melee attack logic
                if (distanceToPlayer < enemy.getSize() + player.getSize() + 20 && cooldown <= 0) {
                    // Calculate damage based on enemy type and level
                    double damage = calculateEnemyDamage(enemy);
                    
                    // Apply damage to player
                    player.takeDamage(damage);
                    enemiesToBounceBack.add(enemy);
                    double damageBlocked = player.getLastDamageBlocked(); // Get amount blocked
                    
                    // Show floating text for damage blocked by armor
                    if (damageBlocked > 0) {
                        effectsManager.showFloatingText("Blocked: " + String.format("%.0f", damageBlocked), 
                            playerCenter.add(0, -15), Color.LIGHTBLUE); // Display slightly above actual damage
                    }

                    // Show damage text with enemy type (actual damage dealt)
                    double actualDamageDealtToPlayer = damage - damageBlocked;
                    if (actualDamageDealtToPlayer > 0) {
                        String enemyType = enemy.getType().toString();
                        effectsManager.showFloatingText("-" + String.format("%.0f", actualDamageDealtToPlayer) + " (" + enemyType + ")", 
                            playerCenter, Color.RED);
                    }
                    
                    // Play hit sound
                    soundManager.playSound("character");
                    
                    // Add a small knockback effect to player
                    Point2D knockbackDirection = playerCenter.subtract(enemyCenter).normalize();
                    double knockbackForce = 5.0;
                    player.setPosition(
                        player.getPosition().add(knockbackDirection.multiply(knockbackForce))
                    );
                    
                    // Start attack cooldown and bounce-back
                    enemyAttackCooldowns.put(enemy, ATTACK_COOLDOWN);
                    
                    // Add bounce-back effect to enemy
                    Point2D bounceDirection = enemyCenter.subtract(playerCenter).normalize();
                    Point2D bouncePosition = enemyCenter.add(bounceDirection.multiply(BOUNCE_DISTANCE));
                    enemy.setPosition(bouncePosition);
                }
            }
            if (enemy.getType() == Enemy.EnemyType.BOSS) {
                // Summon minions every 7 seconds
                bossMinionSummonTimer += deltaTime;
                if (bossMinionSummonTimer >= 7.0) {
                    bossMinionSummonTimer = 0;
                    for (int i = 0; i < 2 + random.nextInt(2); i++) {
                        double angle = random.nextDouble() * Math.PI * 2;
                        double dist = 80 + random.nextDouble() * 40;
                        double minionX = enemy.getX() + Math.cos(angle) * dist;
                        double minionY = enemy.getY() + Math.sin(angle) * dist;
                        Enemy.EnemyType minionType = random.nextBoolean() ? Enemy.EnemyType.GOBLIN : Enemy.EnemyType.SKELETON;
                        Enemy minion = new Enemy(minionX, minionY, minionType);
                        minion.setMaxHealth(minion.getMaxHealth() * 1.2);
                        minion.heal(minion.getMaxHealth());
                        enemies.add(minion);
                        effectsManager.showFloatingText("Minion summoned!", new Point2D(minionX, minionY - 20), Color.LIGHTPINK);
                    }
                }
                // Enrage below 50% health
                if (!bossEnraged && enemy.getHealth() < enemy.getMaxHealth() * 0.5) {
                    bossEnraged = true;
                    enemy.setDamage(enemy.getDamage() * 1.5);
                    Point2D currentVelocity = enemy.getVelocity();
                    if (currentVelocity != null) {
                        enemy.setVelocity(currentVelocity.multiply(1.5));
                    }
                    effectsManager.showFloatingText("Boss is enraged!", new Point2D(enemy.getX(), enemy.getY() - 40), Color.RED);
                }
                // Reset boss ability state when leaving boss room or boss is defeated
                if (currentRoom == null || currentRoom.getType() != DungeonRoom.RoomType.BOSS || enemies.stream().noneMatch(e -> e.getType() == Enemy.EnemyType.BOSS)) {
                    bossMinionSummonTimer = 0;
                    bossEnraged = false;
                }
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

        // Add these fields to GameController:


}

    private double calculateEnemyDamage(Enemy enemy) {
        // Base damage for each enemy type
        double baseDamage;
        switch (enemy.getType()) {
            case SKELETON:
                baseDamage = 5.0;
                break;
            case GOBLIN:
                baseDamage = 15.0;
                break;
            case MAGE:
                baseDamage = 25.0;
                break;
            case BOSS:
                baseDamage = 40.0;
                break;
            default:
                baseDamage = 10.0;
        }
        
        // Scale damage with level
        
        
        return baseDamage;
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
                    double projectileDamage = projectile.getDamage(); // Store original projectile damage
                    
                    // Apply damage to player
                    player.takeDamage(projectileDamage);
                    double damageBlocked = player.getLastDamageBlocked(); // Get amount blocked
                    
                    // Show floating text for damage blocked by armor
                    if (damageBlocked > 0) {
                        effectsManager.showFloatingText("Blocked: " + String.format("%.0f", damageBlocked), 
                            playerPos.add(0, -15), Color.LIGHTBLUE); // Display slightly above actual damage
                    }

                    // Show damage text for actual damage taken
                    double actualDamageDealtToPlayer = projectileDamage - damageBlocked;
                    if (actualDamageDealtToPlayer > 0) {
                        effectsManager.showFloatingText("-" + String.format("%.0f", actualDamageDealtToPlayer), 
                            playerPos, Color.RED);
                    }
                    
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
        if (isInventoryOpen) return; // Prevent opening multiple inventories

        isInventoryOpen = true;
        soundManager.stopSound("running");
        activeKeys.clear();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/dungeon/fxml/Inventory.fxml"));
            Parent inventoryRoot = loader.load();
            
            // Get the controller and set up the inventory
            InventoryController controller = loader.getController();
            controller.setGameController(this);
            controller.setInventory(player.getInventory());
            
            // Create the inventory window
            Stage inventoryStage = new Stage();
            UIUtils.setStageIcon(inventoryStage); // Set the icon here
            inventoryStage.setTitle("Inventory");
            inventoryStage.initModality(Modality.APPLICATION_MODAL);
            inventoryStage.initOwner(gameCanvas.getScene().getWindow());
            inventoryStage.setScene(new Scene(inventoryRoot));
            inventoryStage.setResizable(false);
            
            // Set icon for the inventory window
            UIUtils.setStageIcon(inventoryStage);

            // This call will block until the inventory window is closed
            inventoryStage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // This will run after the inventory window is closed
            isInventoryOpen = false;
        }
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
            // Optionally, show a message that puzzle is already solved
            effectsManager.showFloatingText("Puzzle already solved!",
                new Point2D(gameCanvas.getWidth() / 2, gameCanvas.getHeight() / 2 - 50),
                Color.LIGHTGREEN);
            return;
        }

        // Store only non-movement keys
        Set<KeyCode> storedActiveKeys = new HashSet<>();
        for (KeyCode key : activeKeys) {
            // Only store keys that aren't movement keys
            if (key != KeyCode.W && key != KeyCode.A && key != KeyCode.S && key != KeyCode.D) {
                storedActiveKeys.add(key);
            }
        }
        
        // Clear all keys, including movement keys
        activeKeys.clear();
        isPaused = true;
        gameLoopRunning = false;
        soundManager.stopSound("running");
        
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
           TextArea questionArea = new TextArea(puzzle.getQuestion());
           questionArea.setStyle("-fx-font-size: 18px; -fx-text-fill:rgb(7, 6, 2); -fx-font-weight: bold;");
           questionArea.setWrapText(true);
           questionArea.setMaxWidth(400);
           questionArea.setEditable(false); // This makes it read-only but selectable

            
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
            
            // Create a reference to the puzzle stage for use in lambdas
            final Stage[] puzzleStageRef = new Stage[1];
            // Create hint icon
            ImageView hintIcon = new ImageView(new Image(getClass().getResourceAsStream("/com/dungeon/assets/images/hint.png")));
            hintIcon.setFitWidth(32);
            hintIcon.setFitHeight(32);
            hintIcon.setPreserveRatio(true);
            hintIcon.setStyle("-fx-cursor: hand;");
            hintIcon.setOnMouseClicked(e -> {
                showChat(puzzleStageRef[0]);
            });
            
            // Add components to container
            rootContainer.getChildren().addAll(titleLabel, descriptionLabel, questionArea, answerField, feedbackLabel, submitButton, hintIcon);
            
            // Create the scene
            Scene scene = new Scene(rootContainer);
            
            // Create the stage
            Stage puzzleStage = new Stage();
            UIUtils.setStageIcon(puzzleStage); // Set the icon here
            puzzleStage.setTitle("Puzzle Room");
            puzzleStage.initModality(Modality.APPLICATION_MODAL);
            puzzleStage.initStyle(StageStyle.UNDECORATED);
            
            // Set the owner of the new stage to the main game's stage
            if (gameCanvas != null && gameCanvas.getScene() != null && gameCanvas.getScene().getWindow() != null) {
                puzzleStage.initOwner(gameCanvas.getScene().getWindow());
            } else {
                System.err.println("Warning: Could not set owner for puzzle stage, gameCanvas or its window is null.");
            }
            
            puzzleStage.setScene(scene);
            
            // Center the stage
            puzzleStage.centerOnScreen();
            
            // Add listener for when puzzle window is closed
            puzzleStage.setOnHidden(event -> {
                // Clear any movement keys that might have been pressed during puzzle
                activeKeys.remove(KeyCode.W);
                activeKeys.remove(KeyCode.A);
                activeKeys.remove(KeyCode.S);
                activeKeys.remove(KeyCode.D);
                
                // Restore only non-movement keys
                activeKeys.addAll(storedActiveKeys);
                
                isPaused = false;
                gameLoopRunning = true;
                startGameLoop();
                gameCanvas.requestFocus();
            });
            
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
                    puzzleStage.close(); // This will trigger the setOnHidden listener
                }
            });
            
            // Show the stage
            puzzleStage.show();
            
        } catch (Exception e) {
            System.err.println("Error creating puzzle window: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void showChat() {
        Window owner = (gameCanvas != null && gameCanvas.getScene() != null) ? gameCanvas.getScene().getWindow() : null;
        showChat(owner);
    }

    private void spawnRandomItem(Point2D position) {
        Random random = new Random();
        double offsetX = (random.nextDouble() - 0.5) * 60;
        double offsetY = (random.nextDouble() - 0.5) * 60;
        
        Point2D itemPos = position.add(offsetX, offsetY);
        
        Item rewardItem = null;
        double itemTypeRoll = random.nextDouble();

        if (itemTypeRoll < 0.1) { // 10% chance for a key (optional, can be removed if not desired from puzzles)
            rewardItem = new Item("Dungeon Key", "Opens locked doors", Item.ItemType.KEY, 1, true);
        } else if (itemTypeRoll < 0.45) { // 35% chance for a weapon (10% to 45%)
            // Create a random weapon (tier 1 for now) as a puzzle reward (half damage)
            rewardItem = Weapon.createRandomWeapon(1, true);
        } else if (itemTypeRoll < 0.80) { // 35% chance for armor (45% to 80%)
            // Create a random armor (tier 1 for now) as a puzzle reward
            rewardItem = Armor.createRandomArmor(1, true);
        } else { // 20% chance for a potion (80% to 100%)
            rewardItem = new Item("Health Potion", "Restores health when consumed", Item.ItemType.POTION, 25 + random.nextInt(25), true);
        }
        
        if (rewardItem != null) {
        rewardItem.setX(itemPos.getX());
        rewardItem.setY(itemPos.getY());
            rewardItem.setSize(20); // Default item size on ground
        roomItems.add(rewardItem);
        
        // Add visual effect for the reward
        effectsManager.addExplosionEffect(itemPos, 1.0);
            System.out.println("Puzzle solved, spawned item: " + rewardItem.getName() + " of type " + rewardItem.getType());
        } else {
            System.out.println("Puzzle solved, but failed to spawn a specific item type.");
        }
    }

    private void showGameOverScreen() {
        try {
            System.out.println("Showing game over screen...");
            gameLoopRunning = false;
            soundManager.stopSound("running");
           
            if (weatherSystem != null) {
                    weatherSystem.shutdown();
                        }

            // Get the current scene and its root pane
            Scene currentScene = gameCanvas.getScene();
            Parent gameRoot = currentScene.getRoot();

            // 1. Fade out the game screen
            FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), gameRoot);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(event -> {
                try {
                    // 2. Load the game over screen content
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/dungeon/fxml/GameOver.fxml"));
                    Parent gameOverRoot = loader.load();
                    gameOverRoot.setOpacity(0.0);

                    GameOverController controller = loader.getController();
                    controller.setGameStats((int)player.getScore(), currentLevel - 1, enemiesDefeated);
                    controller.setGameController(this);

                    // 3. Replace the scene's root
                    currentScene.setRoot(gameOverRoot);

                    // 4. Fade in the game over screen
                    FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.5), gameOverRoot);
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.play();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            fadeOut.play();
            
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
            soundManager.stopSound("running");
            soundManager.stopBackgroundMusic();
            if (weatherSystem != null) {
                weatherSystem.shutdown();
            }
            gameLoopRunning = false;

            // Get the current scene and its root pane
            Scene currentScene = gameCanvas.getScene();
            Parent gameRoot = currentScene.getRoot();

            // 1. Fade out the game screen
            FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), gameRoot);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(event -> {
                try {
                    // 2. Load the victory screen content
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/dungeon/fxml/Victory.fxml"));
                    Parent victoryRoot = loader.load();
                    victoryRoot.setOpacity(0.0);

                    VictoryController controller = loader.getController();
                    long endTime = System.currentTimeMillis();
                    long timeInSeconds = (endTime - startTime) / 1000;
                    String timeElapsed = String.format("%02d:%02d", timeInSeconds / 60, timeInSeconds % 60);
                    controller.setGameStats((int)player.getScore(), timeElapsed, enemiesDefeated, currentLevel);
                    controller.setGameController(this);

                    // 3. Replace the scene's root
                    currentScene.setRoot(victoryRoot);

                    // 4. Fade in the victory screen
                    FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.5), victoryRoot);
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.play();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            fadeOut.play();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Update the enemy defeated counter
    private void onEnemyDefeated(Enemy enemy) {
        enemiesDefeated++;
        player.addScore((int)enemy.getScoreValue());
        dropItemFromEnemy(enemy);
        soundManager.playSound("death");
        
        // If in boss room and the defeated enemy is the boss, trigger victory immediately
        if (currentRoom != null && currentRoom.getType() == DungeonRoom.RoomType.BOSS && 
            enemy.getType() == Enemy.EnemyType.BOSS) {
            bossDefeated = true;
            // Trigger victory screen after a short delay to allow for effects
            javafx.application.Platform.runLater(() -> {
                javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(Duration.seconds(1.5));
                delay.setOnFinished(e -> checkVictoryCondition());
                delay.play();
            });
            return;
        }
        // If in boss room and all enemies are defeated, mark boss as defeated
        if (currentRoom != null && currentRoom.getType() == DungeonRoom.RoomType.BOSS && 
            enemies.isEmpty()) {
            bossDefeated = true;
            // Victory music will be played by VictoryController, so no need for "start" sound here
            // Trigger victory screen after a short delay to allow for effects
            javafx.application.Platform.runLater(() -> {
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
        
        // Hide the statue
        statueVisible = false;
        
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
        if (player == null || player.getEquippedWeapon() == null) {
            System.out.println("Player has no equipped weapon to fire projectile.");
            return; 
        }

        Point2D playerPos = player.getPosition().add(player.getSize() / 2, player.getSize() / 2); 
        Point2D targetPos = new Point2D(mouseX, mouseY);
        Point2D direction = targetPos.subtract(playerPos).normalize();
        
        Weapon equippedWeapon = player.getEquippedWeapon();
        double damage = equippedWeapon.getDamage(); 
        Color projectileColor = equippedWeapon.getWeaponType().getProjectileColor(); // Get color from WeaponType
        
        // Determine projectile type (optional, could be always ARROW or vary by weapon)
        ProjectileAttack.ProjectileType projectileType = ProjectileAttack.ProjectileType.ARROW;
        // Example: if (equippedWeapon.getWeaponType() == Weapon.WeaponType.STAFF) { projectileType = ProjectileAttack.ProjectileType.MAGIC_BOLT; }

        ProjectileAttack attack = new ProjectileAttack(
            playerPos.getX(), 
            playerPos.getY(), 
            direction, 
            damage,
            projectileColor, // Use the dynamic color
            projectileType 
        );
        playerProjectiles.add(attack);
        
        soundManager.playSound("damage");
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
        
        // Apply weather effects to player movement
        double weatherSpeedMultiplier = 1.0;
        if (weatherSystem != null) {
            weatherSpeedMultiplier = weatherSystem.getPlayerSpeedMultiplier();
        }
        
        // Check for statue collision in puzzle rooms
        checkStatueCollision();
        
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
    
    private void checkStatueCollision() {
        // Only check statue collision in puzzle rooms when statue is visible
        if (currentRoom != null && currentRoom.getType() == DungeonRoom.RoomType.PUZZLE && 
            statueVisible && statuePosition != null && player != null) {
            
            Point2D playerCenter = player.getPosition().add(player.getSize() / 2, player.getSize() / 2);
            double distance = playerCenter.distance(statuePosition);
            
            // If player is too close to statue, push them back
            double minDistance = (player.getSize() / 2 + STATUE_SIZE / 2);
            if (distance < minDistance) {
                // Calculate direction from statue to player
                Point2D direction = playerCenter.subtract(statuePosition).normalize();
                
                // Calculate new position that maintains minimum distance
                Point2D newPlayerCenter = statuePosition.add(direction.multiply(minDistance));
                Point2D newPlayerPosition = newPlayerCenter.subtract(player.getSize() / 2, player.getSize() / 2);
                
                // Set player to new position
                player.setPosition(newPlayerPosition);
            }
        }
    }
    
    private void dropItemFromEnemy(Enemy enemy) {
        if (enemy == null) return;
        
        Random random = new Random();
        if (random.nextDouble() > 0.3) return; // 30% drop chance
        
        // Get enemy position
        Point2D position = enemy.getPosition();
        
        // Randomize drop type
        Item item;
        
        if (random.nextDouble() < 0.6) {
            // 60% chance for a potion
            item = new Item("Health Potion", "Restores 20 health", Item.ItemType.POTION, 20, true);
        } else if (random.nextDouble() < 0.8) {
            // 20% chance for a weapon
            Weapon.WeaponType[] weaponTypes = Weapon.WeaponType.values();
            Weapon.WeaponType randomType = weaponTypes[random.nextInt(weaponTypes.length)];
            String name = randomType.toString().charAt(0) + randomType.toString().substring(1).toLowerCase();
            
            // Get base damage for the specific weapon type
            int baseDamage;
            switch (randomType) {
                case DAGGER: baseDamage = 17; break;  // 17 * 0.7 = 12 damage
                case SPEAR: baseDamage = 18; break;   // 18 * 0.9 = 16 damage  
                case SWORD: baseDamage = 20; break;   // 20 * 1.0 = 20 damage
                case BOW: baseDamage = 23; break;     // 23 * 0.8 = 18 damage
                case AXE: baseDamage = 19; break;     // 19 * 1.3 = 25 damage
                default: baseDamage = 20; break;
            }
            
            item = new Weapon(name, "A basic " + name.toLowerCase(), baseDamage, randomType, false);
        } else {
            // 20% chance for armor
            Armor.ArmorType[] armorTypes = Armor.ArmorType.values();
            Armor.ArmorType randomType = armorTypes[random.nextInt(armorTypes.length)];
            String name = randomType.toString().charAt(0) + randomType.toString().substring(1).toLowerCase() + " Armor";
            item = new Armor(name, "Basic " + name.toLowerCase(), randomType);
        }
        
        // Set item position and size
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
            case 2: weaponName = "Spear"; break;
            case 3: weaponName = "Axe"; break;
        }
        
        effectsManager.showFloatingText("Selected: " + weaponName, 
            player.getPosition(), Color.LIGHTYELLOW);
    }
  
 private void showLevelCompletionMessage() {
        // Show a floating text in the center of the canvas
        if (gameCanvas != null && effectsManager != null) {
            effectsManager.showFloatingText(
                "Level Complete!",
                new javafx.geometry.Point2D(gameCanvas.getWidth() / 2, 50), // Position at top-middle
                javafx.scene.paint.Color.GOLD
            );
        } else {
            System.out.println("Level Complete!");
        }
    }
    // Add new method to render doors with animations
    private void renderDoorsWithAnimations(GraphicsContext gc) {
        if (doors.isEmpty()) {
            // System.out.println("No doors to render");
            return;
        }
        
        // System.out.println("Rendering " + doors.size() + " doors");
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
            // In treasure room, only show doors if torch is active and door is within light radius
            if (currentRoom != null && currentRoom.getType() == DungeonRoom.RoomType.TREASURE && !treasureRoomLighted) {
                if (!torchActive || player == null) {
                    continue; // Skip rendering if torch is not active
                }
                
                Point2D lightCenter = new Point2D(player.getPosition().getX() + player.getSize()/2, 
                                                 player.getPosition().getY() + player.getSize()/2);
                Point2D doorCenter = new Point2D(door.getX() + door.getWidth()/2, 
                                               door.getY() + door.getHeight()/2);
                double distance = lightCenter.distance(doorCenter);
                
                if (distance > TORCH_LIGHT_RADIUS) {
                    continue; // Skip rendering if door is outside light radius
                }
            }
            
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
            gc.setTextAlign(TextAlignment.CENTER); // Center align text
            gc.fillText(roomTypeText, 
                door.getX() + door.getWidth() / 2, // Center X position
                door.getY() - 5); // Position above door
        }
    }

 
    public void returnToMainMenu() {
        System.out.println("returnToMainMenu called in GameController");
        loadMainMenu();
    }

    public void addItemToRoom(Item item) {
        if (item != null) {
            roomItems.add(item);
           
            effectsManager.showFloatingText("Item dropped!", 
                new Point2D(item.getX(), item.getY()), 
                Color.YELLOW);
        }
    }

    private void loadWeaponImages() {
        try {
            for (Weapon.WeaponType type : Weapon.WeaponType.values()) {
                weaponImages.put(type, new javafx.scene.image.Image(getClass().getResourceAsStream(type.getImagePath())));
            }
        } catch (Exception e) {
            System.err.println("Error loading weapon images: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void checkStatueInteraction() {
        // Only check statue interaction in puzzle rooms when statue is visible
        if (currentRoom != null && currentRoom.getType() == DungeonRoom.RoomType.PUZZLE && 
            statueVisible && statuePosition != null && player != null) {
            
            // Calculate distance between player and statue
            Point2D playerCenter = player.getPosition().add(player.getSize() / 2, player.getSize() / 2);
            double distance = playerCenter.distance(statuePosition);
            
            // Show floating text when player is near statue
            if (distance < STATUE_INTERACTION_RANGE) {
                // Show the floating text slightly above the statue
                Point2D textPosition = new Point2D(statuePosition.getX(), statuePosition.getY() - STATUE_SIZE/2 - 30);
                effectsManager.showFloatingText("Press F to unlock the doors", textPosition, Color.YELLOW);
            }
        }
    }
    
    private void toggleTorch() {
        if (currentRoom != null && currentRoom.getType() == DungeonRoom.RoomType.TREASURE) {
            torchActive = !torchActive;
            String message = torchActive ? "Torch lit!" : "Torch extinguished!";
            Point2D playerPos = player.getPosition();
            effectsManager.showFloatingText(message, playerPos, torchActive ? Color.YELLOW : Color.GRAY);
        }
    }
    
    private void generateTreasureRoomLayout() {
        spikePositions.clear();
        safePositions.clear();
        
        double roomWidth = gameCanvas.getWidth();
        double roomHeight = gameCanvas.getHeight();
        
        // Create a grid of positions
        int gridSize = 8;
        double cellWidth = roomWidth / gridSize;
        double cellHeight = roomHeight / gridSize;
        
        // Generate random spike pattern (about 60% of cells have spikes)
        for (int x = 0; x < gridSize; x++) {
            for (int y = 0; y < gridSize; y++) {
                double posX = x * cellWidth + cellWidth / 2;
                double posY = y * cellHeight + cellHeight / 2;
                
                // Keep center area and door areas safe
                boolean isCenter = (x >= 3 && x <= 4 && y >= 3 && y <= 4);
                boolean isDoorArea = (x == 0 || x == gridSize-1 || y == 0 || y == gridSize-1);
                
                if (!isCenter && !isDoorArea && random.nextDouble() < 0.6) {
                    spikePositions.add(new Point2D(posX, posY));
                } else {
                    safePositions.add(new Point2D(posX, posY));
                }
            }
        }
        
        System.out.println("Generated " + spikePositions.size() + " spikes and " + safePositions.size() + " safe areas");
    }
    
    private void spawnTreasureRoomEquipment() {
        // Spawn 3 equipment items in safe areas, scattered near the middle area
        List<Point2D> availableSafePositions = new ArrayList<>(safePositions);
        double roomCenterX = gameCanvas.getWidth() / 2;
        double roomCenterY = gameCanvas.getHeight() / 2;
        double minDist = Math.min(gameCanvas.getWidth(), gameCanvas.getHeight()) * 0.18; // not too close to center
        double maxDist = Math.min(gameCanvas.getWidth(), gameCanvas.getHeight()) * 0.38; // not too far from center
        // Filter positions to a "middle ring"
        List<Point2D> middleRing = new ArrayList<>();
        for (Point2D pos : availableSafePositions) {
            double dist = pos.distance(roomCenterX, roomCenterY);
            if (dist >= minDist && dist <= maxDist) {
                middleRing.add(pos);
            }
        }
        // If not enough, fallback to all safe positions
        List<Point2D> spawnPositions = middleRing.size() >= 3 ? middleRing : availableSafePositions;
        Collections.shuffle(spawnPositions, random);
        int equipmentCount = Math.min(3, spawnPositions.size());
        for (int i = 0; i < equipmentCount; i++) {
            Point2D position = spawnPositions.get(i);
            // Add a small random offset to scatter
            double offsetX = (random.nextDouble() - 0.5) * 40;
            double offsetY = (random.nextDouble() - 0.5) * 40;
            Point2D scatterPos = new Point2D(position.getX() + offsetX, position.getY() + offsetY);
            Item equipment;
            switch (i) {
                case 0:
                    Weapon.WeaponType[] weaponTypes = Weapon.WeaponType.values();
                    Weapon.WeaponType weaponType = weaponTypes[random.nextInt(weaponTypes.length)];
                    int weaponDamage = Math.min(20 + currentLevel * 5, 30); // Cap at 30
                    equipment = new Weapon(weaponType.name(), "A powerful " + weaponType.name().toLowerCase(), weaponDamage, weaponType, false);
                    break;
                case 1:
                    Armor.ArmorType[] armorTypes = Armor.ArmorType.values();
                    Armor.ArmorType armorType = armorTypes[random.nextInt(armorTypes.length)];
                    equipment = new Armor(armorType.name() + " Armor", "Protective " + armorType.name().toLowerCase(), armorType);
                    break;
                case 2:
                    equipment = new Item("Health Potion", "Restores 30 health", Item.ItemType.POTION, 30, true);
                    break;
                default:
                    equipment = new Item("Gold", "Valuable treasure", Item.ItemType.TREASURE, 50, true);
                    break;
            }
            equipment.setX(scatterPos.getX());
            equipment.setY(scatterPos.getY());
            equipment.setSize(25);
            roomItems.add(equipment);
        }
    }

    private void checkSpikeCollision() {
        if (currentRoom != null && currentRoom.getType() == DungeonRoom.RoomType.TREASURE && 
            player != null && !treasureRoomLighted) {
            
            Point2D playerCenter = player.getPosition().add(player.getSize() / 2, player.getSize() / 2);
            
            for (Point2D spikePos : spikePositions) {
                double distance = playerCenter.distance(spikePos);
                
                if (distance < (player.getSize() / 2 + SPIKE_SIZE / 2)) {
                    // Player hit a spike
                    player.takeDamage(SPIKE_DAMAGE);
                    effectsManager.showFloatingText("-" + (int)SPIKE_DAMAGE, player.getPosition(), Color.RED);
                    soundManager.playSound("damage");
                    
                    // Push player back slightly
                    Point2D direction = playerCenter.subtract(spikePos).normalize();
                    Point2D newPos = player.getPosition().add(direction.multiply(30));
                    player.setPosition(newPos);
                    
                    break; // Only hit one spike at a time
                }
            }
        }
    }

}
