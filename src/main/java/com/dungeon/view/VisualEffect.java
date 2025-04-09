package com.dungeon.view;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class VisualEffect {
    private Point2D position;
    private double duration;
    private double currentTime;
    private EffectType type;
    private double size;
    private Color color;
    private String text;
    
    public enum EffectType {
        EXPLOSION,
        DAMAGE_TEXT,
        HEAL_TEXT,
        PICKUP_TEXT,
        SPARKLE
    }
    
    public VisualEffect(Point2D position, EffectType type, double duration) {
        this.position = position;
        this.type = type;
        this.duration = duration;
        this.currentTime = 0;
        this.size = 30;
        
        // Default colors based on effect type
        switch (type) {
            case EXPLOSION:
                this.color = Color.ORANGE;
                break;
            case DAMAGE_TEXT:
                this.color = Color.RED;
                break;
            case HEAL_TEXT:
                this.color = Color.GREEN;
                break;
            case PICKUP_TEXT:
                this.color = Color.YELLOW;
                break;
            case SPARKLE:
                this.color = Color.CYAN;
                break;
        }
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public void setColor(Color color) {
        this.color = color;
    }
    
    public void update(double deltaTime) {
        currentTime += deltaTime;
    }
    
    public boolean isFinished() {
        return currentTime >= duration;
    }
    
    public void render(GraphicsContext gc) {
        double progress = currentTime / duration;
        
        switch (type) {
            case EXPLOSION:
                renderExplosion(gc, progress);
                break;
            case DAMAGE_TEXT:
            case HEAL_TEXT:
            case PICKUP_TEXT:
                renderFloatingText(gc, progress);
                break;
            case SPARKLE:
                renderSparkle(gc, progress);
                break;
        }
    }
    
    private void renderExplosion(GraphicsContext gc, double progress) {
        double alpha = 1.0 - progress;
        double currentSize = size * (1.0 + progress);
        
        gc.setGlobalAlpha(alpha);
        gc.setFill(color);
        gc.fillOval(
            position.getX() - currentSize / 2, 
            position.getY() - currentSize / 2, 
            currentSize, 
            currentSize
        );
        
        // Draw inner explosion
        gc.setFill(Color.YELLOW);
        gc.fillOval(
            position.getX() - currentSize / 4, 
            position.getY() - currentSize / 4, 
            currentSize / 2, 
            currentSize / 2
        );
        
        gc.setGlobalAlpha(1.0);
    }
    
    private void renderFloatingText(GraphicsContext gc, double progress) {
        if (text == null) return;
        
        double alpha = 1.0 - progress;
        double offsetY = -30 * progress; // Text floats upward
        
        gc.setGlobalAlpha(alpha);
        gc.setFill(color);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.5);
        
        // Set font size based on effect type
        int fontSize = 14;
        if (type == EffectType.DAMAGE_TEXT) {
            fontSize = 18;
        }
        
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, fontSize));
        
        // Draw text with outline for better visibility
        gc.strokeText(text, position.getX(), position.getY() + offsetY);
        gc.fillText(text, position.getX(), position.getY() + offsetY);
        
        gc.setGlobalAlpha(1.0);
    }
    
    private void renderSparkle(GraphicsContext gc, double progress) {
        double alpha = 1.0 - progress;
        double currentSize = size * (1.0 - progress * 0.5);
        
        gc.setGlobalAlpha(alpha);
        
        // Draw sparkle particles
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2 * i / 8;
            double distance = currentSize / 2 * progress;
            double x = position.getX() + Math.cos(angle) * distance;
            double y = position.getY() + Math.sin(angle) * distance;
            
            gc.setFill(color);
            gc.fillOval(x - 2, y - 2, 4, 4);
        }
        
        gc.setGlobalAlpha(1.0);
    }
}
