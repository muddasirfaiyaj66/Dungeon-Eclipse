package com.dungeon.model;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class ProjectileAttack {
    private static final double PROJECTILE_SPEED = 400; // pixels per second
    private static final double PROJECTILE_SIZE = 10;
    private static final double MAX_DISTANCE = 500;

    private Point2D position;
    private final Point2D direction;
    private final double damage;
    private double distanceTraveled;
    private boolean active;
    private Color color;
    private ProjectileType type;
    
    public enum ProjectileType {
        ARROW,
        MAGIC,
        DEFAULT
    }

    public ProjectileAttack(double x, double y, Point2D direction, double damage) {
        this(x, y, direction, damage, Color.YELLOW, ProjectileType.DEFAULT);
    }
    
    public ProjectileAttack(double x, double y, Point2D direction, double damage, Color color, ProjectileType type) {
        this.position = new Point2D(x, y);
        this.direction = direction.normalize();
        this.damage = damage;
        this.distanceTraveled = 0;
        this.active = true;
        this.color = color;
        this.type = type;
    }

    public void update(double deltaTime) {
        if (!active) return;

        Point2D movement = direction.multiply(PROJECTILE_SPEED * deltaTime);
        position = position.add(movement);
        distanceTraveled += movement.magnitude();

        if (distanceTraveled >= MAX_DISTANCE) {
            active = false;
        }
    }

    public void render(GraphicsContext gc) {
        if (!active) return;

        // Save the current state
        gc.save();
        
        // Translate to the projectile position
        gc.translate(position.getX(), position.getY());
        
        // Rotate to match the direction of travel
        double angle = Math.atan2(direction.getY(), direction.getX());
        gc.rotate(Math.toDegrees(angle));
        
        // Draw based on projectile type
        switch (type) {
            case ARROW:
                // Draw arrow
                gc.setFill(Color.BROWN);
                gc.fillRect(-PROJECTILE_SIZE, -PROJECTILE_SIZE/4, PROJECTILE_SIZE*1.5, PROJECTILE_SIZE/2);
                
                // Arrow head
                gc.setFill(Color.GRAY);
                double[] xPoints = {PROJECTILE_SIZE*1.5, PROJECTILE_SIZE, PROJECTILE_SIZE};
                double[] yPoints = {0, -PROJECTILE_SIZE/2, PROJECTILE_SIZE/2};
                gc.fillPolygon(xPoints, yPoints, 3);
                break;
                
            case MAGIC:
                // Draw magic orb with glow effect
                gc.setGlobalAlpha(0.3);
                gc.setFill(color.deriveColor(0, 1, 1, 0.5));
                gc.fillOval(-PROJECTILE_SIZE, -PROJECTILE_SIZE, PROJECTILE_SIZE*2, PROJECTILE_SIZE*2);
                
                gc.setGlobalAlpha(0.6);
                gc.setFill(color.deriveColor(0, 1, 1, 0.7));
                gc.fillOval(-PROJECTILE_SIZE*0.7, -PROJECTILE_SIZE*0.7, PROJECTILE_SIZE*1.4, PROJECTILE_SIZE*1.4);
                
                gc.setGlobalAlpha(1.0);
                gc.setFill(color);
                gc.fillOval(-PROJECTILE_SIZE/2, -PROJECTILE_SIZE/2, PROJECTILE_SIZE, PROJECTILE_SIZE);
                break;
                
            case DEFAULT:
            default:
                // Draw default projectile
                gc.setFill(color);
                gc.fillOval(-PROJECTILE_SIZE/2, -PROJECTILE_SIZE/2, PROJECTILE_SIZE, PROJECTILE_SIZE);
                break;
        }
        
        // Restore the graphics context
        gc.restore();
    }

    public void hit() {
        active = false;
    }

    public boolean isActive() {
        return active;
    }
    
    /**
     * Checks if the projectile is expired (no longer active)
     * @return true if the projectile is expired and should be removed
     */
    public boolean isExpired() {
        return !active;
    }

    public Point2D getPosition() {
        return position;
    }

    public double getDamage() {
        return damage;
    }
    
    public double getSize() {
        return PROJECTILE_SIZE;
    }
}
