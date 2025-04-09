package com.dungeon.view;

import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LightingEffect {
    private Canvas lightCanvas;
    private GraphicsContext lightGc;
    private List<LightSource> lightSources;
    private Random random;
    private double ambientLight;
    private Color ambientColor;
    private double time;
    
    public LightingEffect(Canvas gameCanvas) {
        // Create a separate canvas for lighting effects with the same dimensions
        this.lightCanvas = new Canvas(gameCanvas.getWidth(), gameCanvas.getHeight());
        this.lightGc = lightCanvas.getGraphicsContext2D();
        this.lightSources = new ArrayList<>();
        this.random = new Random();
        this.ambientLight = 0.2; // 20% ambient light
        this.ambientColor = Color.rgb(20, 20, 40); // Dark blue ambient
        this.time = 0;
    }
    
    public void update(double deltaTime) {
        time += deltaTime;
        
        // Update all light sources
        for (LightSource light : lightSources) {
            light.update(deltaTime);
        }
    }
    
    public void render(GraphicsContext gc) {
        // Clear the light canvas
        lightGc.setFill(ambientColor);
        lightGc.fillRect(0, 0, lightCanvas.getWidth(), lightCanvas.getHeight());
        
        // Draw all light sources
        lightGc.setGlobalBlendMode(BlendMode.ADD);
        for (LightSource light : lightSources) {
            light.render(lightGc);
        }
        
        // Apply lighting to the main canvas
        gc.setGlobalAlpha(0.8); // Adjust the strength of the lighting effect
        gc.setGlobalBlendMode(BlendMode.MULTIPLY);
        gc.drawImage(lightCanvas.snapshot(null, null), 0, 0);
        gc.setGlobalBlendMode(BlendMode.SRC_OVER);
        gc.setGlobalAlpha(1.0);
    }
    
    public void addLightSource(Point2D position, double radius, Color color, LightSource.LightType type) {
        lightSources.add(new LightSource(position, radius, color, type));
    }
    
    public void clearLights() {
        lightSources.clear();
    }
    
    public void setAmbientLight(double intensity) {
        this.ambientLight = Math.max(0.0, Math.min(1.0, intensity));
        int value = (int)(ambientLight * 255);
        this.ambientColor = Color.rgb(value/5, value/5, value/3);
    }
    
    public static class LightSource {
        private Point2D position;
        private double radius;
        private Color color;
        private LightType type;
        private double flickerIntensity;
        private double time;
        private double currentRadius;
        private double pulseSpeed;
        
        public enum LightType {
            STATIC,
            FLICKERING,
            PULSING
        }
        
        public LightSource(Point2D position, double radius, Color color, LightType type) {
            this.position = position;
            this.radius = radius;
            this.color = color;
            this.type = type;
            this.flickerIntensity = 0.2;
            this.time = 0;
            this.currentRadius = radius;
            this.pulseSpeed = 1.0 + Math.random() * 0.5;
        }
        
        public void update(double deltaTime) {
            time += deltaTime;
            
            // Update light properties based on type
            switch (type) {
                case FLICKERING:
                    // Random flickering
                    double flicker = 1.0 - flickerIntensity + Math.random() * flickerIntensity * 2;
                    currentRadius = radius * flicker;
                    break;
                    
                case PULSING:
                    // Smooth pulsing
                    double pulse = 0.8 + 0.4 * Math.sin(time * pulseSpeed * 2);
                    currentRadius = radius * pulse;
                    break;
                    
                case STATIC:
                default:
                    currentRadius = radius;
                    break;
            }
        }
        
        public void render(GraphicsContext gc) {
            // Create a radial gradient for the light
            RadialGradient gradient = new RadialGradient(
                0, 0,
                position.getX(), position.getY(),
                currentRadius,
                false,
                CycleMethod.NO_CYCLE,
                new Stop(0, color),
                new Stop(0.7, color.deriveColor(0, 1, 1, 0.5)),
                new Stop(1, color.deriveColor(0, 1, 1, 0))
            );
            
            gc.setFill(gradient);
            gc.fillOval(
                position.getX() - currentRadius, 
                position.getY() - currentRadius, 
                currentRadius * 2, 
                currentRadius * 2
            );
        }
        
        public void setPosition(Point2D position) {
            this.position = position;
        }
        
        public Point2D getPosition() {
            return position;
        }
    }
}
