package com.dungeon.view;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class EffectsManager {
    private List<VisualEffect> activeEffects;
    private Random random;
    
    public EffectsManager() {
        activeEffects = new ArrayList<>();
        random = new Random();
    }
    
    public void update(double deltaTime) {
        Iterator<VisualEffect> iterator = activeEffects.iterator();
        while (iterator.hasNext()) {
            VisualEffect effect = iterator.next();
            effect.update(deltaTime);
            
            if (effect.isFinished()) {
                iterator.remove();
            }
        }
    }
    
    public void render(GraphicsContext gc) {
        for (VisualEffect effect : activeEffects) {
            effect.render(gc);
        }
    }
    
    public void addDamageEffect(Point2D position, double damage) {
        VisualEffect effect = new VisualEffect(position, VisualEffect.EffectType.DAMAGE_TEXT, 1.0);
        effect.setText("-" + String.format("%.0f", damage));
        activeEffects.add(effect);
        
        // Add explosion effect for impact
        addExplosionEffect(position, 0.5);
    }
    
    public void addHealEffect(Point2D position, double amount) {
        VisualEffect effect = new VisualEffect(position, VisualEffect.EffectType.HEAL_TEXT, 1.0);
        effect.setText("+" + String.format("%.0f", amount));
        effect.setColor(Color.GREEN);
        activeEffects.add(effect);
        
        // Add sparkle effect
        addSparkleEffect(position, 0.8);
    }
    
    public void addPickupEffect(Point2D position, String itemName) {
        VisualEffect effect = new VisualEffect(position, VisualEffect.EffectType.PICKUP_TEXT, 1.2);
        effect.setText("Picked up " + itemName);
        effect.setColor(Color.YELLOW);
        activeEffects.add(effect);
        
        // Add sparkle effect
        addSparkleEffect(position, 0.8);
    }
    
    public void addExplosionEffect(Point2D position, double duration) {
        VisualEffect effect = new VisualEffect(position, VisualEffect.EffectType.EXPLOSION, duration);
        activeEffects.add(effect);
    }
    
    public void addSparkleEffect(Point2D position, double duration) {
        VisualEffect effect = new VisualEffect(position, VisualEffect.EffectType.SPARKLE, duration);
        effect.setColor(Color.GOLD);
        activeEffects.add(effect);
    }
    
    public void addRoomClearEffect(Point2D position) {
        // Create multiple sparkles around the position
        for (int i = 0; i < 12; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 100;
            double offsetY = (random.nextDouble() - 0.5) * 100;
            Point2D sparklePos = new Point2D(position.getX() + offsetX, position.getY() + offsetY);
            
            VisualEffect sparkle = new VisualEffect(sparklePos, VisualEffect.EffectType.SPARKLE, 1.5);
            sparkle.setColor(Color.GOLD);
            activeEffects.add(sparkle);
        }
        
        // Add "Room cleared!" text
        VisualEffect textEffect = new VisualEffect(position, VisualEffect.EffectType.PICKUP_TEXT, 2.0);
        textEffect.setText("Room cleared!");
        textEffect.setColor(Color.LIGHTGREEN);
        activeEffects.add(textEffect);
    }
    
    public void addPuzzleSolvedEffect(Point2D position) {
        // Create sparkle pattern
        for (int i = 0; i < 16; i++) {
            double angle = Math.PI * 2 * i / 16;
            double distance = 50;
            double x = position.getX() + Math.cos(angle) * distance;
            double y = position.getY() + Math.sin(angle) * distance;
            Point2D sparklePos = new Point2D(x, y);
            
            VisualEffect sparkle = new VisualEffect(sparklePos, VisualEffect.EffectType.SPARKLE, 1.5);
            sparkle.setColor(Color.CYAN);
            activeEffects.add(sparkle);
        }
        
        // Add "Puzzle solved!" text
        VisualEffect textEffect = new VisualEffect(position, VisualEffect.EffectType.PICKUP_TEXT, 2.0);
        textEffect.setText("Puzzle solved!");
        textEffect.setColor(Color.CYAN);
        activeEffects.add(textEffect);
    }
}
