package com.dungeon.model.entity;

import com.dungeon.view.EffectsManager;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EnemyAbility {
    private static final double PROJECTILE_SPEED = 300;
    
    public enum AbilityType {
        FIREBALL,
        POISON_CLOUD,
        TELEPORT,
        CHARGE,
        SUMMON_MINION
    }
    
    public static void useAbility(Enemy enemy, Player player, AbilityType abilityType, 
                                 List<Enemy> enemies, EffectsManager effectsManager) {
        switch (abilityType) {
            case FIREBALL:
                castFireball(enemy, player, effectsManager);
                break;
            case POISON_CLOUD:
                castPoisonCloud(enemy, player, effectsManager);
                break;
            case TELEPORT:
                teleport(enemy, player, effectsManager);
                break;
            case CHARGE:
                charge(enemy, player, effectsManager);
                break;
            case SUMMON_MINION:
                summonMinion(enemy, enemies, effectsManager);
                break;
        }
    }
    
    private static void castFireball(Enemy enemy, Player player, EffectsManager effectsManager) {
        Point2D enemyPos = enemy.getPosition();
        Point2D playerPos = player.getPosition();
        Point2D direction = playerPos.subtract(enemyPos).normalize();
        
        // Create projectile
        Projectile fireball = new Projectile(
            enemyPos.getX(), 
            enemyPos.getY(), 
            direction.getX() * PROJECTILE_SPEED, 
            direction.getY() * PROJECTILE_SPEED,
            15, // damage
            2.0, // lifetime
            Color.ORANGE
        );
        
        // Add visual effect
        effectsManager.addExplosionEffect(enemyPos, 0.3);
    }
    
    private static void castPoisonCloud(Enemy enemy, Player player, EffectsManager effectsManager) {
        Point2D enemyPos = enemy.getPosition();
        
        // Create poison cloud effect
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2 * i / 8;
            double distance = 30;
            double x = enemyPos.getX() + Math.cos(angle) * distance;
            double y = enemyPos.getY() + Math.sin(angle) * distance;
            
            effectsManager.addExplosionEffect(new Point2D(x, y), 1.0);
        }
        
        // Check if player is in range
        double distanceToPlayer = enemyPos.distance(player.getPosition());
        if (distanceToPlayer < 100) {
            player.takeDamage(5);
            effectsManager.addDamageEffect(player.getPosition(), 5);
        }
    }
    
    private static void teleport(Enemy enemy, Player player, EffectsManager effectsManager) {
        Point2D oldPos = enemy.getPosition();
        
        // Calculate new position (away from player)
        Point2D playerPos = player.getPosition();
        Point2D direction = oldPos.subtract(playerPos).normalize();
        Point2D newPos = playerPos.add(direction.multiply(200));
        
        // Teleport
        enemy.setPosition(newPos);
        
        // Add visual effects
        effectsManager.addSparkleEffect(oldPos, 0.5);
        effectsManager.addSparkleEffect(newPos, 0.5);
    }
    
    private static void charge(Enemy enemy, Player player, EffectsManager effectsManager) {
        Point2D enemyPos = enemy.getPosition();
        Point2D playerPos = player.getPosition();
        Point2D direction = playerPos.subtract(enemyPos).normalize();
        
        // Set high velocity toward player
        enemy.setVelocity(direction.multiply(500));
        
        // Add visual effect
        effectsManager.addExplosionEffect(enemyPos, 0.3);
        
        // Check if hit player
        double distanceToPlayer = enemyPos.distance(playerPos);
        if (distanceToPlayer < 50) {
            player.takeDamage(20);
            effectsManager.addDamageEffect(playerPos, 20);
        }
    }
    
    private static void summonMinion(Enemy enemy, List<Enemy> enemies, EffectsManager effectsManager) {
        Point2D enemyPos = enemy.getPosition();
        
        // Create minion
        Enemy minion = new Enemy(
            enemyPos.getX() + 30, 
            enemyPos.getY() + 30, 
            Enemy.EnemyType.GOBLIN
        );
        
        enemies.add(minion);
        
        // Add visual effect
        effectsManager.addSparkleEffect(minion.getPosition(), 1.0);
    }
    
    public static class Projectile {
        private Point2D position;
        private Point2D velocity;
        private double damage;
        private double lifetime;
        private double currentTime;
        private Color color;
        private double size = 10;
        
        public Projectile(double x, double y, double vx, double vy, double damage, double lifetime, Color color) {
            this.position = new Point2D(x, y);
            this.velocity = new Point2D(vx, vy);
            this.damage = damage;
            this.lifetime = lifetime;
            this.currentTime = 0;
            this.color = color;
        }
        
        public void update(double deltaTime) {
            position = position.add(velocity.multiply(deltaTime));
            currentTime += deltaTime;
        }
        
        public boolean isExpired() {
            return currentTime >= lifetime;
        }
        
        public void render(GraphicsContext gc) {
            gc.setFill(color);
            gc.fillOval(position.getX() - size/2, position.getY() - size/2, size, size);
            
            // Add trail effect
            double alpha = 0.7;
            for (int i = 1; i <= 3; i++) {
                Point2D trailPos = position.subtract(velocity.normalize().multiply(i * 5));
                gc.setGlobalAlpha(alpha);
                gc.fillOval(trailPos.getX() - size/2, trailPos.getY() - size/2, size, size);
                alpha -= 0.2;
            }
            gc.setGlobalAlpha(1.0);
        }
        
        public Point2D getPosition() {
            return position;
        }
        
        public double getDamage() {
            return damage;
        }
        
        public double getSize() {
            return size;
        }
    }
}
