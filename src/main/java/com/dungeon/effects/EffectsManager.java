package com.dungeon.effects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.dungeon.audio.SoundManager;
import com.dungeon.model.Door;
import com.dungeon.model.DungeonRoom;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PathTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

public class EffectsManager {
    private final Pane effectsPane;
    private final Canvas effectsCanvas;
    private final GraphicsContext gc;
    private final List<VisualEffect> effects;
    private final Random random;
    private final SoundManager soundManager;
    
    // Transition overlay
    private Rectangle transitionOverlay;
    
    // Constants
    private static final double FLOATING_TEXT_DURATION = 1.5; // seconds
    private static final double PARTICLE_DURATION = 0.8; // seconds
    private static final double EXPLOSION_DURATION = 1.0; // seconds

    public void showFloatingText(String s, Point2D position, Color green) {
        showFloatingText(s, position, green, 20);
    }

    public void render(GraphicsContext gc) {
    }

    // Enum for different room effect types
    public enum RoomEffectType {
        COMBAT,
        PUZZLE,
        TREASURE,
        BOSS
    }
    
    public EffectsManager(Pane effectsPane, Canvas effectsCanvas) {
        this.effectsPane = effectsPane;
        this.effectsCanvas = effectsCanvas;
        this.gc = effectsCanvas.getGraphicsContext2D();
        this.effects = new ArrayList<>();
        this.random = new Random();
        this.soundManager = SoundManager.getInstance();
        
        // Initialize transition overlay
        this.transitionOverlay = new Rectangle(0, 0, 
                                              effectsCanvas.getWidth(), 
                                              effectsCanvas.getHeight());
        this.transitionOverlay.setFill(Color.rgb(51, 54, 57)); // Dark slate-blue
        this.transitionOverlay.setOpacity(0);
        this.transitionOverlay.setMouseTransparent(true);
        this.effectsPane.getChildren().add(transitionOverlay);
    }
    
    public void update(double deltaTime) {
        // Clear canvas
        gc.clearRect(0, 0, effectsCanvas.getWidth(), effectsCanvas.getHeight());
        
        // Update and render canvas-based effects
        Iterator<VisualEffect> iterator = effects.iterator();
        while (iterator.hasNext()) {
            VisualEffect effect = iterator.next();
            effect.update(deltaTime);
            effect.render(gc);
            
            if (effect.isExpired()) {
                iterator.remove();
            }
        }
    }
    
    public void resize(double width, double height) {
        effectsCanvas.setWidth(width);
        effectsCanvas.setHeight(height);
        
        // Resize transition overlay
        transitionOverlay.setWidth(width);
        transitionOverlay.setHeight(height);
    }
    
    public void startRoomTransition(DungeonRoom.RoomType roomType, Runnable onMidpoint, Runnable onComplete) {
        // Play transition sound
        soundManager.playSound("teleport");
    
        // Force resize the overlay to match the canvas just before the transition
        transitionOverlay.setWidth(effectsCanvas.getWidth());
        transitionOverlay.setHeight(effectsCanvas.getHeight());
    
        // Create a fade-in transition for the overlay
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(1.5), transitionOverlay);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
    
        // Create a fade-out transition
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(1.5), transitionOverlay);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        // --- Create and add the room text ---
        Text roomTypeText = new Text(roomType.toString().replace("_", " ") + " ROOM");
        roomTypeText.setFont(Font.font("Verdana", FontWeight.BOLD, 48));
        roomTypeText.setFill(Color.WHITE);
        roomTypeText.setOpacity(0); // Opacity will be bound to the overlay
        effectsPane.getChildren().add(roomTypeText);

        // Center the text on the next frame, once its bounds are calculated
        Platform.runLater(() -> {
            roomTypeText.setLayoutX((effectsCanvas.getWidth() - roomTypeText.getLayoutBounds().getWidth()) / 2);
            roomTypeText.setLayoutY(effectsCanvas.getHeight() / 2);
        });

        // Bind the text's opacity to the overlay's so they fade together
        roomTypeText.opacityProperty().bind(transitionOverlay.opacityProperty());
    
        // When the fade-in is complete, run the midpoint logic
        fadeIn.setOnFinished(e -> {
            if (onMidpoint != null) {
                onMidpoint.run();
            }
            // After the room is updated, start the fade-out
            fadeOut.play();
        });
    
        // When the fade-out is complete, run the completion logic
        fadeOut.setOnFinished(e -> {
            // Clean up the text node
            roomTypeText.opacityProperty().unbind();
            effectsPane.getChildren().remove(roomTypeText);

            if (onComplete != null) {
                onComplete.run();
            }
        });
    
        // Start the fade-in
        fadeIn.play();
    }
    
    
    
    private Color getRoomTypeColor(DungeonRoom.RoomType roomType) {
        switch (roomType) {
            case COMBAT:
                return Color.RED;
            case PUZZLE:
                return Color.BLUE;
            case TREASURE:
                return Color.GOLD;
            case BOSS:
                return Color.DARKRED;
            case SPAWN:
                return Color.GREEN;
            default:
                return Color.WHITE;
        }
    }
    
    private Text createRoomTypeText(DungeonRoom.RoomType roomType) {
        String message;
        Color color = getRoomTypeColor(roomType);
        
        switch (roomType) {
            case COMBAT:
                message = "COMBAT ROOM";
                break;
            case PUZZLE:
                message = "PUZZLE ROOM";
                break;
            case TREASURE:
                message = "TREASURE ROOM";
                break;
            case BOSS:
                message = "BOSS ROOM";
                break;
            case SPAWN:
                message = "SPAWN ROOM";
                break;
            default:
                message = "NEW ROOM";
        }
        
        Text text = new Text(message);
        text.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        text.setFill(color);
        
        // Enhanced glow effect
        DropShadow shadow = new DropShadow(BlurType.GAUSSIAN, color, 10, 0.8, 0, 0);
        text.setEffect(shadow);
        
        // Center the text
        text.setLayoutX((effectsCanvas.getWidth() - text.getLayoutBounds().getWidth()) / 2);
        text.setLayoutY((effectsCanvas.getHeight() - text.getLayoutBounds().getHeight()) / 2 + 50);
        
        return text;
    }
    
    /**
     * Adds appropriate visual effects when entering a room
     * @param roomType The type of room being entered
     */
    private void addRoomEnterEffect(DungeonRoom.RoomType roomType) {
        switch (roomType) {
            case COMBAT:
                addCombatRoomEffect();
                break;
            case PUZZLE:
                addPuzzleRoomEffect();
                break;
            case TREASURE:
                addTreasureRoomEffect();
                break;
            case BOSS:
                addBossRoomEffect();
                break;
            case SPAWN:
                // No special effect for spawn room
                break;
        }
    }
    
    private void addCombatRoomEffect() {
        // Add red flash effect
        Rectangle flash = new Rectangle(0, 0, effectsCanvas.getWidth(), effectsCanvas.getHeight());
        flash.setFill(Color.RED);
        flash.setOpacity(0.3);
        flash.setBlendMode(BlendMode.ADD);
        effectsPane.getChildren().add(flash);
        
        // Flash animation
        FadeTransition flashFade = new FadeTransition(Duration.seconds(0.5), flash);
        flashFade.setFromValue(0.3);
        flashFade.setToValue(0);
        flashFade.setOnFinished(e -> effectsPane.getChildren().remove(flash));
        flashFade.play();
        
        // Add sword clash effect
        for (int i = 0; i < 20; i++) {
            addParticle(
                new Point2D(effectsCanvas.getWidth() / 2, effectsCanvas.getHeight() / 2),
                Color.ORANGERED,
                1.5
            );
        }
    }
    
    private void addPuzzleRoomEffect() {
        // Add blue glow effect
        Rectangle glow = new Rectangle(0, 0, effectsCanvas.getWidth(), effectsCanvas.getHeight());
        glow.setFill(Color.BLUE);
        glow.setOpacity(0.2);
        glow.setBlendMode(BlendMode.ADD);
        effectsPane.getChildren().add(glow);
        
        // Glow animation
        FadeTransition glowFade = new FadeTransition(Duration.seconds(0.8), glow);
        glowFade.setFromValue(0.2);
        glowFade.setToValue(0);
        glowFade.setOnFinished(e -> effectsPane.getChildren().remove(glow));
        glowFade.play();
        
        // Add question mark symbols
        for (int i = 0; i < 5; i++) {
            double x = random.nextDouble() * effectsCanvas.getWidth();
            double y = random.nextDouble() * effectsCanvas.getHeight();
            
            Text questionMark = new Text("?");
            questionMark.setFont(Font.font("Arial", FontWeight.BOLD, 30 + random.nextInt(20)));
            questionMark.setFill(Color.CORNFLOWERBLUE);
            questionMark.setLayoutX(x);
            questionMark.setLayoutY(y);
            questionMark.setOpacity(0.8);
            effectsPane.getChildren().add(questionMark);
            
            // Animation
            TranslateTransition tt = new TranslateTransition(Duration.seconds(1.5), questionMark);
            tt.setByY(-100);
            
            FadeTransition ft = new FadeTransition(Duration.seconds(1.5), questionMark);
            ft.setFromValue(0.8);
            ft.setToValue(0);
            
            ParallelTransition pt = new ParallelTransition(tt, ft);
            pt.setOnFinished(e -> effectsPane.getChildren().remove(questionMark));
            pt.play();
        }
    }
    
    private void addTreasureRoomEffect() {
        // Add gold particles
        for (int i = 0; i < 50; i++) {
            double x = random.nextDouble() * effectsCanvas.getWidth();
            double y = random.nextDouble() * effectsCanvas.getHeight();
            
            Circle goldParticle = new Circle(x, y, 2 + random.nextDouble() * 3);
            goldParticle.setFill(Color.GOLD);
            goldParticle.setOpacity(0.9);
            goldParticle.setBlendMode(BlendMode.ADD);
            effectsPane.getChildren().add(goldParticle);
            
            // Animation
            double targetX = x + (random.nextDouble() - 0.5) * 200;
            double targetY = y + (random.nextDouble() - 0.5) * 200;
            
            Path path = new Path();
            path.getElements().add(new MoveTo(x, y));
            path.getElements().add(new LineTo(targetX, targetY));
            
            PathTransition pathTransition = new PathTransition(
                Duration.seconds(1 + random.nextDouble()), goldParticle, path);
            
            FadeTransition fadeTransition = new FadeTransition(
                Duration.seconds(1 + random.nextDouble()), goldParticle);
            fadeTransition.setFromValue(0.9);
            fadeTransition.setToValue(0);
            
            ParallelTransition parallelTransition = new ParallelTransition(
                pathTransition, fadeTransition);
            parallelTransition.setOnFinished(e -> effectsPane.getChildren().remove(goldParticle));
            parallelTransition.play();
        }
        
        // Add treasure text
        showFloatingText("TREASURE!", 
            new Point2D(effectsCanvas.getWidth() / 2, effectsCanvas.getHeight() / 2),
            Color.GOLD,
            36);
    }
    
    private void addBossRoomEffect() {
        // Add dramatic red flash
        Rectangle flash = new Rectangle(0, 0, effectsCanvas.getWidth(), effectsCanvas.getHeight());
        flash.setFill(Color.DARKRED);
        flash.setOpacity(0.5);
        effectsPane.getChildren().add(flash);
        
        // Flash animation with pulsing
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(flash.opacityProperty(), 0.5)),
            new KeyFrame(Duration.seconds(0.2), new KeyValue(flash.opacityProperty(), 0.3)),
            new KeyFrame(Duration.seconds(0.4), new KeyValue(flash.opacityProperty(), 0.5)),
            new KeyFrame(Duration.seconds(0.6), new KeyValue(flash.opacityProperty(), 0.3)),
            new KeyFrame(Duration.seconds(0.8), new KeyValue(flash.opacityProperty(), 0))
        );
        timeline.setOnFinished(e -> effectsPane.getChildren().remove(flash));
        timeline.play();
        
        // Add warning text
        Text warningText = new Text("DANGER!");
        warningText.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        warningText.setFill(Color.RED);
        
        // Add glow effect
        Glow glow = new Glow(0.8);
        DropShadow shadow = new DropShadow(BlurType.GAUSSIAN, Color.BLACK, 10, 0.8, 0, 0);
        glow.setInput(shadow);
        warningText.setEffect(glow);
        
        // Center the text
        warningText.setLayoutX((effectsCanvas.getWidth() - warningText.getLayoutBounds().getWidth()) / 2);
        warningText.setLayoutY((effectsCanvas.getHeight() - warningText.getLayoutBounds().getHeight()) / 2);
        warningText.setOpacity(0);
        effectsPane.getChildren().add(warningText);
        
        // Warning text animation
        FadeTransition textFadeIn = new FadeTransition(Duration.seconds(0.3), warningText);
        textFadeIn.setFromValue(0);
        textFadeIn.setToValue(1);
        
        ScaleTransition textScale = new ScaleTransition(Duration.seconds(1.0), warningText);
        textScale.setFromX(1.0);
        textScale.setFromY(1.0);
        textScale.setToX(1.5);
        textScale.setToY(1.5);
        
        FadeTransition textFadeOut = new FadeTransition(Duration.seconds(0.5), warningText);
        textFadeOut.setFromValue(1);
        textFadeOut.setToValue(0);
        textFadeOut.setDelay(Duration.seconds(1.0));
        
        ParallelTransition textAnimation = new ParallelTransition(
            textFadeIn, textScale, textFadeOut);
        textAnimation.setOnFinished(e -> effectsPane.getChildren().remove(warningText));
        textAnimation.play();
    }
    
    private void addSpawnRoomEffect() {
        // Add soft green glow
        Rectangle glow = new Rectangle(0, 0, effectsCanvas.getWidth(), effectsCanvas.getHeight());
        glow.setFill(Color.GREEN);
        glow.setOpacity(0.2);
        glow.setBlendMode(BlendMode.ADD);
        effectsPane.getChildren().add(glow);
        
        // Glow animation
        FadeTransition glowFade = new FadeTransition(Duration.seconds(1.0), glow);
        glowFade.setFromValue(0.2);
        glowFade.setToValue(0);
        glowFade.setOnFinished(e -> effectsPane.getChildren().remove(glow));
        glowFade.play();
        
        // Add welcome text
        showFloatingText("Begin Your Journey", 
            new Point2D(effectsCanvas.getWidth() / 2, effectsCanvas.getHeight() / 2),
            Color.FORESTGREEN,
            30);
    }
    
    public void addRoomTransitionEffect() {
        // Implementation for adding a room transition effect
    }
    
    // General visual effects
    public void showFloatingText(String text, Point2D position) {
        showFloatingText(text, position, Color.WHITE, 20);
    }
    
    public void showFloatingText(String text, Point2D position, Color color, double fontSize) {
        Text floatingText = new Text(text);
        floatingText.setFont(Font.font("Arial", FontWeight.BOLD, fontSize));
        floatingText.setFill(color);
        
        // Add drop shadow
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.BLACK);
        shadow.setRadius(2);
        floatingText.setEffect(shadow);
        
        // Position text
        floatingText.setLayoutX(position.getX() - floatingText.getLayoutBounds().getWidth() / 2);
        floatingText.setLayoutY(position.getY());
        floatingText.setOpacity(0.9);
        effectsPane.getChildren().add(floatingText);
        
        // Animation
        TranslateTransition translate = new TranslateTransition(Duration.seconds(FLOATING_TEXT_DURATION), floatingText);
        translate.setByY(-50);
        
        FadeTransition fade = new FadeTransition(Duration.seconds(FLOATING_TEXT_DURATION), floatingText);
        fade.setFromValue(0.9);
        fade.setToValue(0);
        
        ParallelTransition transition = new ParallelTransition(translate, fade);
        transition.setOnFinished(e -> effectsPane.getChildren().remove(floatingText));
        transition.play();
    }
    
    public void addDamageEffect(Point2D position, double damage) {
        // Show damage number
        String damageText = String.format("%.0f", damage);
        showFloatingText(damageText, position, Color.RED, 24);
        
        // Add hit particles
        for (int i = 0; i < 10; i++) {
            addParticle(position, Color.RED, 1.0);
        }
    }
    
    public void addHealEffect(Point2D position, double amount) {
        // Show heal number
        String healText = "+" + String.format("%.0f", amount);
        showFloatingText(healText, position, Color.GREEN, 24);
        
        // Add healing particles
        for (int i = 0; i < 15; i++) {
            addParticle(position, Color.LIGHTGREEN, 1.2);
        }
    }
    
    public void addPickupEffect(Point2D position, String itemName) {
        // Show pickup text
        showFloatingText("Got " + itemName, position, Color.YELLOW, 20);
        
        // Add sparkle particles
        for (int i = 0; i < 12; i++) {
            addParticle(position, Color.YELLOW, 1.0);
        }
    }
    
    public void addExplosionEffect(Point2D position, double scale) {
        // Add explosion particles
        for (int i = 0; i < 30; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 50 + random.nextDouble() * 100;
            double lifetime = 0.5 + random.nextDouble() * 0.5;
            
            Point2D velocity = new Point2D(
                Math.cos(angle) * speed,
                Math.sin(angle) * speed
            );
            
            Color color = random.nextBoolean() ? 
                Color.ORANGE : (random.nextBoolean() ? Color.RED : Color.YELLOW);
            
            effects.add(new ParticleEffect(
                position.getX(), position.getY(),
                velocity,
                5 * scale + random.nextDouble() * 5 * scale,
                color,
                lifetime
            ));
        }
    }
    
    public void addParticle(Point2D position, Color color, double size) {
        double angle = random.nextDouble() * Math.PI * 2;
        double speed = 30 + random.nextDouble() * 70;
        
        double velocityX = Math.cos(angle) * speed;
        double velocityY = Math.sin(angle) * speed;
        
        effects.add(new ParticleEffect(
            position.getX(), position.getY(),
            new Point2D(velocityX, velocityY),
            2 * size + random.nextDouble() * 3 * size,
            color,
            PARTICLE_DURATION * (0.7 + random.nextDouble() * 0.6)
        ));
    }
    
    // Inner classes for canvas-based effects
    private abstract class VisualEffect {
        protected double lifetime;
        protected double maxLifetime;
        
        public VisualEffect(double lifetime) {
            this.lifetime = lifetime;
            this.maxLifetime = lifetime;
        }
        
        public abstract void update(double deltaTime);
        public abstract void render(GraphicsContext gc);
        
        public boolean isExpired() {
            return lifetime <= 0;
        }
        
        protected double getLifetimePercentage() {
            return lifetime / maxLifetime;
        }
    }
    
    private class ParticleEffect extends VisualEffect {
        private Point2D position;
        private Point2D velocity;
        private double size;
        private Color color;
        
        public ParticleEffect(double x, double y, Point2D velocity, double size, Color color, double lifetime) {
            super(lifetime);
            this.position = new Point2D(x, y);
            this.velocity = velocity;
            this.size = size;
            this.color = color;
        }
        
        @Override
        public void update(double deltaTime) {
            // Update position
            position = position.add(velocity.multiply(deltaTime));
            
            // Apply gravity and friction
            velocity = velocity.multiply(0.95); // Air resistance
            velocity = velocity.add(0, 100 * deltaTime); // Gravity
            
            // Update lifetime
            lifetime -= deltaTime;
        }
        
        @Override
        public void render(GraphicsContext gc) {
            double alpha = getLifetimePercentage();
            double currentSize = size * (0.5 + getLifetimePercentage() * 0.5);
            
            gc.setGlobalAlpha(alpha);
            gc.setFill(color);
            gc.fillOval(
                position.getX() - currentSize / 2,
                position.getY() - currentSize / 2,
                currentSize,
                currentSize
            );
            gc.setGlobalAlpha(1.0);
        }
    }
    
    // Add new method for door opening animation
    public void playDoorOpeningAnimation(Door door) {
        System.out.println("Playing door animation for door at: " + door.getX() + "," + door.getY());
        
        // Create door visual representation with larger size for visibility
        Rectangle doorVisual = new Rectangle(
            door.getX(),
            door.getY(),
            door.getWidth(),
            door.getHeight()
        );
        doorVisual.setFill(Color.BROWN);
        doorVisual.setStroke(Color.BLACK);
        doorVisual.setStrokeWidth(3);
        
        // Add door frame
        Rectangle doorFrame = new Rectangle(
            door.getX() - 5,
            door.getY() - 5,
            door.getWidth() + 10,
            door.getHeight() + 10
        );
        doorFrame.setFill(Color.DARKGRAY);
        doorFrame.setStroke(Color.BLACK);
        doorFrame.setStrokeWidth(3);
        
        // Add door handle
        Circle doorHandle = new Circle(
            door.getX() + door.getWidth() - 15,
            door.getY() + door.getHeight() / 2,
            8
        );
        doorHandle.setFill(Color.GOLD);
        doorHandle.setStroke(Color.BLACK);
        doorHandle.setStrokeWidth(2);
        
        // Create a group for the door elements
        javafx.scene.Group doorGroup = new javafx.scene.Group(doorVisual, doorHandle);
        doorGroup.setLayoutX(door.getX());
        doorGroup.setLayoutY(door.getY());
        
        // Add all elements to the effects pane
        effectsPane.getChildren().addAll(doorFrame, doorGroup);
        
        // Create door opening animation
        RotateTransition doorOpen = new RotateTransition(Duration.seconds(0.5), doorGroup);
        doorOpen.setFromAngle(0);
        doorOpen.setToAngle(90);
        doorOpen.setAxis(Rotate.Z_AXIS);
        
        // Set the pivot point for rotation (left edge of the door)
        doorGroup.setTranslateX(0);
        doorGroup.setTranslateY(door.getHeight() / 2);
        
        // Add slight bounce effect
        ScaleTransition doorBounce = new ScaleTransition(Duration.seconds(0.1), doorGroup);
        doorBounce.setFromX(1.0);
        doorBounce.setFromY(1.0);
        doorBounce.setToX(1.05);
        doorBounce.setToY(1.05);
        doorBounce.setAutoReverse(true);
        doorBounce.setCycleCount(2);
        
        // Create sequential animation
        SequentialTransition sequence = new SequentialTransition(
            doorBounce,
            doorOpen
        );
        
        // Add cleanup
        sequence.setOnFinished(e -> {
            System.out.println("Door animation finished");
            effectsPane.getChildren().removeAll(doorFrame, doorGroup);
        });
        
        sequence.play();
    }
    
    // Add method to check if player is near door
    public boolean isPlayerNearDoor(Point2D playerPosition, Door door) {
        double distance = playerPosition.distance(
            door.getX() + door.getWidth() / 2,
            door.getY() + door.getHeight() / 2
        );
        System.out.println("Player distance from door: " + distance);
        return distance < 150; // Increased detection range for door animation
    }
}

