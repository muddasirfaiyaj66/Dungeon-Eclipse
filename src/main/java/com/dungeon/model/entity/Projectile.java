package com.dungeon.model.entity;

import javafx.geometry.Point2D;
import com.dungeon.model.entity.Entity;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Represents a projectile in the game, used for ranged attacks
 */
public class Projectile {
    private double x;
    private double y;
    private double velocityX;
    private double velocityY;
    private double damage;
    private double size;
    private Color color;
    private boolean fromPlayer;
    private boolean expired = false;
    
    // Projectile lifetime
    private double lifetime = 2.0; // seconds
    private double currentLifetime = 0.0;
    
    // Projectile effect
    private boolean piercing = false;
    private boolean explosive = false;
    private double explosionRadius = 0;
    
    /**
     * Creates a new projectile
     * @param x Initial X position
     * @param y Initial Y position
     * @param velocityX X velocity
     * @param velocityY Y velocity
     * @param damage Damage amount
     * @param size Size of the projectile
     * @param color Color of the projectile
     * @param fromPlayer Whether the projectile was fired by the player
     */
    public Projectile(double x, double y, double velocityX, double velocityY, 
                     double damage, double size, Color color, boolean fromPlayer) {
        this.x = x;
        this.y = y;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.damage = damage;
        this.size = size;
        this.color = color;
        this.fromPlayer = fromPlayer;
    }
    
    /**
     * Updates the projectile position and lifetime
     * @param deltaTime Time since last update in seconds
     */
    public void update(double deltaTime) {
        if (expired) return;
        
        // Update position
        x += velocityX * deltaTime;
        y += velocityY * deltaTime;
        
        // Update lifetime
        currentLifetime += deltaTime;
        if (currentLifetime >= lifetime) {
            expired = true;
        }
    }
    
    /**
     * Renders the projectile on the canvas
     * @param gc Graphics context to render on
     */
    public void render(GraphicsContext gc) {
        if (expired) return;
        
        gc.setFill(color);
        gc.fillOval(x - size/2, y - size/2, size, size);
        
        // Add glow effect
        gc.setGlobalAlpha(0.3);
        gc.fillOval(x - size, y - size, size * 2, size * 2);
        gc.setGlobalAlpha(1.0);
        
        // Add trail effect (optional - could be more complex with particle system)
        double trailLength = Math.sqrt(velocityX * velocityX + velocityY * velocityY) * 0.1;
        if (trailLength > 0) {
            double angle = Math.atan2(velocityY, velocityX);
            double trailX = x - Math.cos(angle) * trailLength;
            double trailY = y - Math.sin(angle) * trailLength;
            
            gc.setStroke(color);
            gc.setLineWidth(size * 0.7);
            gc.setGlobalAlpha(0.5);
            gc.strokeLine(x, y, trailX, trailY);
            gc.setGlobalAlpha(1.0);
        }
    }
    
    /**
     * Checks collision with an entity
     * @param entity The entity to check collision with
     * @return true if colliding
     */
    public boolean collidesWith(Entity entity) {
        if (entity == null || expired) return false;
        
        double entityCenterX = entity.getX() + entity.getWidth() / 2;
        double entityCenterY = entity.getY() + entity.getHeight() / 2;
        
        double dx = x - entityCenterX;
        double dy = y - entityCenterY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        // Collision if distance is less than sum of radii
        return distance < (size / 2 + Math.min(entity.getWidth(), entity.getHeight()) / 2);
    }
    
    /**
     * Handles collision with walls or obstacles
     * @param wallX X position of the wall
     * @param wallY Y position of the wall
     * @param wallWidth Width of the wall
     * @param wallHeight Height of the wall
     * @return true if colliding
     */
    public boolean collidesWithWall(double wallX, double wallY, double wallWidth, double wallHeight) {
        if (expired) return false;
        
        // Simple bounding box collision
        return (x + size/2 > wallX && x - size/2 < wallX + wallWidth &&
                y + size/2 > wallY && y - size/2 < wallY + wallHeight);
    }
    
    /**
     * Makes this projectile explode, affecting entities in the explosion radius
     * @param entities Array of entities to check for explosion damage
     * @return Array of affected entities
     */
    public Entity[] explode(Entity[] entities) {
        if (!explosive || explosionRadius <= 0 || entities == null) {
            return new Entity[0];
        }
        
        // Find entities in explosion radius
        java.util.List<Entity> affectedEntities = new java.util.ArrayList<>();
        
        for (Entity entity : entities) {
            if (entity == null) continue;
            
            double entityCenterX = entity.getX() + entity.getWidth() / 2;
            double entityCenterY = entity.getY() + entity.getHeight() / 2;
            
            double dx = x - entityCenterX;
            double dy = y - entityCenterY;
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            if (distance <= explosionRadius) {
                // Calculate damage falloff based on distance
                double damageMultiplier = 1.0 - (distance / explosionRadius);
                int explosionDamage = (int)Math.max(1, damage * damageMultiplier);
                entity.takeDamage(explosionDamage);
                affectedEntities.add(entity);
            }
        }
        
        // Mark as expired after explosion
        expired = true;
        
        return affectedEntities.toArray(new Entity[0]);
    }
    
    /**
     * Sets this projectile to be piercing (can hit multiple enemies)
     * @param piercing Whether this projectile can pierce through enemies
     */
    public void setPiercing(boolean piercing) {
        this.piercing = piercing;
    }
    
    /**
     * Sets this projectile to be explosive
     * @param explosive Whether this projectile explodes on impact
     * @param radius The explosion radius
     */
    public void setExplosive(boolean explosive, double radius) {
        this.explosive = explosive;
        this.explosionRadius = radius;
    }
    
    /**
     * Marks this projectile as expired (to be removed)
     */
    public void expire() {
        this.expired = true;
    }
    
    /**
     * Gets the current position of the projectile
     * @return Point2D containing the current position
     */
    public Point2D getPosition() {
        return new Point2D(x, y);
    }
    
    // Getters and setters
    
    public double getX() {
        return x;
    }
    
    public double getY() {
        return y;
    }
    
    public double getDamage() {
        return damage;
    }
    
    public double getSize() {
        return size;
    }
    
    public boolean isFromPlayer() {
        return fromPlayer;
    }
    
    public boolean isExpired() {
        return expired;
    }
    
    public boolean isPiercing() {
        return piercing;
    }
    
    public boolean isExplosive() {
        return explosive;
    }
    
    public double getExplosionRadius() {
        return explosionRadius;
    }
}
