package com.dungeon.model;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.application.Platform;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class WeatherSystem {
    public enum WeatherType {
        CLEAR("Clear", 1.0, 1.0, 1.0, Color.TRANSPARENT),
        RAIN("Rain", 0.8, 1.2, 0.9, Color.rgb(100, 150, 255, 0.2)),
        SNOW("Snow", 0.7, 0.8, 1.2, Color.rgb(220, 220, 255, 0.1)),
        STORM("Storm", 0.5, 1.5, 0.7, Color.rgb(50, 50, 100, 0.4)),
        DARKNESS("Darkness", 0.4, 0.8, 1.4, Color.rgb(20, 20, 40, 0.6));

        private final String name;
        private final double visibilityMultiplier;
        private final double enemySpeedMultiplier;
        private final double playerSpeedMultiplier;
        private final Color overlayColor;

        WeatherType(String name, double visibility, double enemySpeed, double playerSpeed, Color overlay) {
            this.name = name;
            this.visibilityMultiplier = visibility;
            this.enemySpeedMultiplier = enemySpeed;
            this.playerSpeedMultiplier = playerSpeed;
            this.overlayColor = overlay;
        }

        public String getName() { return name; }
        public double getVisibilityMultiplier() { return visibilityMultiplier; }
        public double getEnemySpeedMultiplier() { return enemySpeedMultiplier; }
        public double getPlayerSpeedMultiplier() { return playerSpeedMultiplier; }
        public Color getOverlayColor() { return overlayColor; }
    }

    // Thread-safe state variables
    private final AtomicReference<WeatherType> currentWeather = new AtomicReference<>(WeatherType.CLEAR);
    private final AtomicReference<WeatherType> targetWeather = new AtomicReference<>(WeatherType.CLEAR);
    private final AtomicBoolean isTransitioning = new AtomicBoolean(false);
    private final AtomicBoolean thunderActive = new AtomicBoolean(false);
    
    // Thread-safe timers
    private volatile double weatherTimer = 0;
    private volatile double animationTimer = 0;
    private volatile double thunderTimer = 0;
    private volatile double transitionProgress = 0;
    
    private final Random random = new Random();
    private final double weatherDuration = 20.0;
    
    // Weather GIF images (loaded once, thread-safe)
    private volatile Image rainGif;
    private volatile Image thunderGif;
    private volatile Image snowGif;
    
    // Threading components
    private final ExecutorService weatherExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "WeatherThread");
        t.setDaemon(true);
        return t;
    });
    
    private final ScheduledExecutorService animationExecutor = Executors.newScheduledThreadPool(1, r -> {
        Thread t = new Thread(r, "AnimationThread");
        t.setDaemon(true);
        return t;
    });
    
    private volatile boolean isShutdown = false;
    private int weatherUpdateCount = 0;
    private int animationUpdateCount = 0;

    // Control weather changes (for treasure room)
    private volatile boolean weatherChangesAllowed = true;

    public WeatherSystem() {
        System.out.println("🌤️  [WeatherSystem] Initializing multithreaded weather system...");
        loadWeatherImages();
        startWeatherThread();
        startAnimationThread();
        System.out.println("✅ [WeatherSystem] Multithreaded weather system initialized successfully!");
    }

    private void loadWeatherImages() {
        weatherExecutor.submit(() -> {
            try {
                System.out.println("🖼️  [WeatherThread] Loading weather GIF assets...");
                rainGif = new Image(getClass().getResourceAsStream("/com/dungeon/assets/GIFS/rain.gif"));
                thunderGif = new Image(getClass().getResourceAsStream("/com/dungeon/assets/GIFS/thunder.gif"));
                snowGif = new Image(getClass().getResourceAsStream("/com/dungeon/assets/GIFS/snow.gif"));
                System.out.println("✅ [WeatherThread] Weather GIFs loaded successfully!");
                System.out.println("   - Rain GIF: " + (rainGif != null ? "Loaded" : "Failed"));
                System.out.println("   - Thunder GIF: " + (thunderGif != null ? "Loaded" : "Failed"));
                System.out.println("   - Snow GIF: " + (snowGif != null ? "Loaded" : "Failed"));
            } catch (Exception e) {
                System.err.println("❌ [WeatherThread] Error loading weather GIFs: " + e.getMessage());
            }
        });
    }

    private void startWeatherThread() {
        System.out.println("🌦️  [WeatherSystem] Starting WeatherThread...");
        weatherExecutor.submit(() -> {
            System.out.println("🔄 [WeatherThread] WeatherThread started - Thread ID: " + Thread.currentThread().getId());
            while (!isShutdown) {
                try {
                    Thread.sleep(100); // Update every 100ms
                    updateWeatherLogic();
                } catch (InterruptedException e) {
                    System.out.println("⚠️  [WeatherThread] WeatherThread interrupted");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("❌ [WeatherThread] Error in weather thread: " + e.getMessage());
                }
            }
            System.out.println("🛑 [WeatherThread] WeatherThread stopped");
        });
    }

    private void startAnimationThread() {
        System.out.println("🎬 [WeatherSystem] Starting AnimationThread...");
        animationExecutor.scheduleAtFixedRate(() -> {
            if (!isShutdown) {
                updateAnimationLogic();
            }
        }, 0, 16, TimeUnit.MILLISECONDS); // 60 FPS animation updates
        System.out.println("✅ [AnimationThread] AnimationThread started - 60 FPS updates");
    }

    private void updateWeatherLogic() {
        weatherTimer += 0.1; // 100ms = 0.1 seconds
        weatherUpdateCount++;
        
        // Print weather updates every 50 updates (5 seconds)
        if (weatherUpdateCount % 50 == 0) {
            System.out.println("🌤️  [WeatherThread] Weather update #" + weatherUpdateCount + 
                             " - Current: " + currentWeather.get().getName() + 
                             " - Timer: " + String.format("%.1f", weatherTimer) + "s");
        }
        
        if (weatherTimer >= weatherDuration) {
            changeWeather();
            weatherTimer = 0;
        }
        
        if (isTransitioning.get()) {
            transitionProgress += 0.05; // 100ms * 0.5 = 0.05
            if (transitionProgress >= 1.0) {
                WeatherType oldWeather = currentWeather.get();
                currentWeather.set(targetWeather.get());
                isTransitioning.set(false);
                transitionProgress = 0;
                System.out.println("🔄 [WeatherThread] Weather transition complete: " + 
                                 oldWeather.getName() + " → " + currentWeather.get().getName());
            }
        }
        
        // Thunder effect for storm weather
        if (currentWeather.get() == WeatherType.STORM) {
            thunderTimer += 0.1;
            if (thunderTimer > 2.0) {
                if (random.nextDouble() < 0.4) { // 40% chance every 2 seconds
                    thunderActive.set(true);
                    thunderTimer = 0;
                    System.out.println("⚡ [WeatherThread] THUNDER STRIKE! Lightning effect activated");
                }
            }
            
            // Thunder effect duration
            if (thunderActive.get() && thunderTimer > 0.5) {
                thunderActive.set(false);
                System.out.println("⚡ [WeatherThread] Thunder effect ended");
            }
        }
    }

    private void updateAnimationLogic() {
        animationTimer += 0.016; // 16ms = 0.016 seconds (60 FPS)
        animationUpdateCount++;
        
        // Print animation updates every 300 updates (5 seconds at 60 FPS)
        if (animationUpdateCount % 300 == 0) {
            System.out.println("🎬 [AnimationThread] Animation update #" + animationUpdateCount + 
                             " - Timer: " + String.format("%.2f", animationTimer) + "s");
        }
    }

    private void changeWeather() {
        // Don't change weather if changes are not allowed (e.g., in treasure room)
        if (!weatherChangesAllowed) {
            System.out.println("🌤️  [WeatherThread] Weather change blocked - treasure room active");
            return;
        }
        
        WeatherType[] weathers = WeatherType.values();
        WeatherType newWeather;
        do {
            newWeather = weathers[random.nextInt(weathers.length)];
        } while (newWeather == currentWeather.get());
        
        System.out.println("🌪️  [WeatherThread] Weather change triggered: " + 
                         currentWeather.get().getName() + " → " + newWeather.getName());
        startWeatherTransition(newWeather);
    }

    public void startWeatherTransition(WeatherType newWeather) {
        System.out.println("🔄 [WeatherSystem] Manual weather transition: " + 
                         currentWeather.get().getName() + " → " + newWeather.getName());
        targetWeather.set(newWeather);
        isTransitioning.set(true);
        transitionProgress = 0;
    }

    // This method is called from the main game thread (JavaFX thread)
    public void render(GraphicsContext gc, double canvasWidth, double canvasHeight) {
        WeatherType effectiveWeather;
        
        // If weather changes are disabled, force clear weather
        if (!weatherChangesAllowed) {
            effectiveWeather = WeatherType.CLEAR;
        } else {
            effectiveWeather = isTransitioning.get() ? 
            interpolateWeather(currentWeather.get(), targetWeather.get(), transitionProgress) : currentWeather.get();
        }
        
        // Apply weather overlay
        if (effectiveWeather.getOverlayColor() != Color.TRANSPARENT) {
            gc.setFill(effectiveWeather.getOverlayColor());
            gc.fillRect(0, 0, canvasWidth, canvasHeight);
        }
        
        // Render weather effects based on type
        switch (effectiveWeather) {
            case RAIN:
                renderRainEffect(gc, canvasWidth, canvasHeight, false); // Regular rain density
                break;
            case SNOW:
                renderSnowEffect(gc, canvasWidth, canvasHeight);
                break;
            case STORM:
                renderRainEffect(gc, canvasWidth, canvasHeight, true); // Dense rain for storm
                if (thunderActive.get()) {
                    renderThunderEffect(gc, canvasWidth, canvasHeight);
                }
                break;
            default:
                break;
        }
    }

    private WeatherType interpolateWeather(WeatherType from, WeatherType to, double progress) {
        return progress < 0.5 ? from : to;
    }

    private void renderRainEffect(GraphicsContext gc, double canvasWidth, double canvasHeight, boolean dense) {
        if (rainGif == null) return;
        
        // Calculate how many rain GIFs we need to tile across the screen
        double gifWidth = rainGif.getWidth();
        double gifHeight = rainGif.getHeight();
        
        // For dense rain (storm), use more tiles to create thicker rain
        int tilesX = (int) Math.ceil(canvasWidth / gifWidth) + (dense ? 2 : 1);
        int tilesY = (int) Math.ceil(canvasHeight / gifHeight) + (dense ? 2 : 1);
        
        // Add some offset based on animation timer for movement effect
        // Storm rain moves faster than regular rain
        double speedMultiplier = dense ? 1.5 : 1.0;
        double offsetX = (animationTimer * 50 * speedMultiplier) % gifWidth;
        double offsetY = (animationTimer * 100 * speedMultiplier) % gifHeight;
        
        // For dense rain, add additional layers with different offsets
        int layers = dense ? 3 : 1;
        
        for (int layer = 0; layer < layers; layer++) {
            // Different offset for each layer to create depth
            double layerOffsetX = offsetX + (layer * gifWidth / 3);
            double layerOffsetY = offsetY + (layer * gifHeight / 3);
            
            for (int x = -1; x < tilesX; x++) {
                for (int y = -1; y < tilesY; y++) {
                    double drawX = x * gifWidth - layerOffsetX;
                    double drawY = y * gifHeight - layerOffsetY;
                    
                    // For dense rain, add some transparency variation
                    if (dense && layer > 0) {
                        gc.setGlobalAlpha(0.7 - (layer * 0.2)); // Each layer slightly more transparent
                    }
                    
                    gc.drawImage(rainGif, drawX, drawY, gifWidth, gifHeight);
                    
                    // Reset alpha for next iteration
                    if (dense) {
                        gc.setGlobalAlpha(1.0);
                    }
                }
            }
        }
    }

    private void renderSnowEffect(GraphicsContext gc, double canvasWidth, double canvasHeight) {
        if (snowGif == null) return;
        
        // Calculate how many snow GIFs we need to tile across the screen
        double gifWidth = snowGif.getWidth();
        double gifHeight = snowGif.getHeight();
        
        int tilesX = (int) Math.ceil(canvasWidth / gifWidth) + 1;
        int tilesY = (int) Math.ceil(canvasHeight / gifHeight) + 1;
        
        // Add some offset based on animation timer for gentle snow movement
        double offsetX = (animationTimer * 20) % gifWidth; // Slower than rain
        double offsetY = (animationTimer * 30) % gifHeight;
        
        for (int x = -1; x < tilesX; x++) {
            for (int y = -1; y < tilesY; y++) {
                double drawX = x * gifWidth - offsetX;
                double drawY = y * gifHeight - offsetY;
                
                gc.drawImage(snowGif, drawX, drawY, gifWidth, gifHeight);
            }
        }
    }

    private void renderThunderEffect(GraphicsContext gc, double canvasWidth, double canvasHeight) {
        if (thunderGif == null) return;
        
        // Thunder effect covers the entire screen
        double gifWidth = thunderGif.getWidth();
        double gifHeight = thunderGif.getHeight();
        
        // Scale the thunder GIF to cover the screen
        double scaleX = canvasWidth / gifWidth;
        double scaleY = canvasHeight / gifHeight;
        double scale = Math.max(scaleX, scaleY);
        
        double scaledWidth = gifWidth * scale;
        double scaledHeight = gifHeight * scale;
        
        // Center the thunder effect
        double drawX = (canvasWidth - scaledWidth) / 2;
        double drawY = (canvasHeight - scaledHeight) / 2;
        
        gc.drawImage(thunderGif, drawX, drawY, scaledWidth, scaledHeight);
    }

    // Thread-safe getters
    public WeatherType getCurrentWeather() {
        return currentWeather.get();
    }

    public double getVisibilityMultiplier() {
        return currentWeather.get().getVisibilityMultiplier();
    }

    public double getEnemySpeedMultiplier() {
        return currentWeather.get().getEnemySpeedMultiplier();
    }

    public double getPlayerSpeedMultiplier() {
        return currentWeather.get().getPlayerSpeedMultiplier();
    }

    public String getWeatherName() {
        return currentWeather.get().getName();
    }

    // Control weather changes for treasure room
    public void setWeatherChangesAllowed(boolean allowed) {
        this.weatherChangesAllowed = allowed;
        if (!allowed) {
            System.out.println("🌤️  [WeatherSystem] Weather changes disabled for treasure room");
            // Force clear weather when disabling changes
            currentWeather.set(WeatherType.CLEAR);
            targetWeather.set(WeatherType.CLEAR);
            isTransitioning.set(false);
            transitionProgress = 0;
        } else {
            System.out.println("🌤️  [WeatherSystem] Weather changes re-enabled");
        }
    }
    
    public boolean isWeatherChangesAllowed() {
        return weatherChangesAllowed;
    }

    // Cleanup method - call this when shutting down the game
    public void shutdown() {
        System.out.println("🛑 [WeatherSystem] Shutting down multithreaded weather system...");
        isShutdown = true;
        weatherExecutor.shutdown();
        animationExecutor.shutdown();
        
        try {
            if (!weatherExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                weatherExecutor.shutdownNow();
                System.out.println("⚠️  [WeatherSystem] WeatherThread force shutdown");
            }
            if (!animationExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                animationExecutor.shutdownNow();
                System.out.println("⚠️  [WeatherSystem] AnimationThread force shutdown");
            }
            System.out.println("✅ [WeatherSystem] Weather system shutdown complete");
        } catch (InterruptedException e) {
            weatherExecutor.shutdownNow();
            animationExecutor.shutdownNow();
            Thread.currentThread().interrupt();
            System.out.println("❌ [WeatherSystem] Shutdown interrupted");
        }
    }
}