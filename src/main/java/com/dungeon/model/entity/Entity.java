package com.dungeon.model.entity;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;

public abstract class Entity {
    // Position and movement
    protected Point2D position;
    protected Point2D velocity;
    protected double size;
    
    // Health and stats
    protected final IntegerProperty health = new SimpleIntegerProperty(100);
    protected final IntegerProperty maxHealth = new SimpleIntegerProperty(100);
    protected double damage;
    protected double speed;
    
    // State
    protected boolean isAlive;
    
    // Dimensions for collision
    protected double width;
    protected double height;

    public Entity(double x, double y, double health, double speed, double size) {
        this.position = new Point2D(x, y);
        this.velocity = new Point2D(0, 0);
        this.maxHealth.set((int)health);
        this.health.set((int)health);
        this.speed = speed;
        this.size = size;
        this.isAlive = true;
        this.width = size;
        this.height = size;
    }

    public void update(double deltaTime) {
        // Update position based on velocity
        position = position.add(velocity.multiply(deltaTime));
        
        // Reset velocity (movement is frame-based)
        velocity = new Point2D(0, 0);
    }

    public void takeDamage(double damage) {
        if (!isAlive || damage <= 0) return;
        
        int newHealth = Math.max(0, health.get() - (int)damage);
        health.set(newHealth);
        
        if (newHealth <= 0) {
            die();
        }
    }

    public void heal(double amount) {
        if (!isAlive || amount <= 0) return;
        
        health.set(Math.min(maxHealth.get(), health.get() + (int)amount));
    }

    protected void die() {
        isAlive = false;
        // Subclasses can override to add death animations/effects
    }

    public abstract void render(GraphicsContext gc);

    // Position methods
    public void setPosition(double x, double y) {
        this.position = new Point2D(x, y);
    }

    public void setPosition(Point2D newPosition) {
        this.position = newPosition;
    }

    public Point2D getPosition() {
        return position;
    }

    public double getX() {
        return position.getX();
    }

    public double getY() {
        return position.getY();
    }

    // Movement methods
    public Point2D getVelocity() {
        return velocity;
    }

    public void setVelocity(Point2D velocity) {
        this.velocity = velocity;
    }

    public void move(double dx, double dy) {
        position = position.add(dx, dy);
    }

    // Health methods
    public int getHealth() {
        return health.get();
    }

    public IntegerProperty healthProperty() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth.get();
    }

    public IntegerProperty maxHealthProperty() {
        return maxHealth;
    }

    public void setMaxHealth(double maxHealth) {
        this.maxHealth.set((int)maxHealth);
    }

    // Stats methods
    public double getSpeed() {
        return speed;
    }

    public double getSize() {
        return size;
    }

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    // State methods
    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean alive) {
        isAlive = alive;
    }

    // Collision methods
    public boolean collidesWith(Entity other) {
        if (other == null) return false;
        return getBoundingBox().intersects(other.getBoundingBox());
    }

    public Rectangle2D getBoundingBox() {
        return new Rectangle2D(position.getX(), position.getY(), width, height);
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }
}
