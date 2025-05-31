package com.dungeon.model;

import com.dungeon.model.entity.Enemy;
import com.dungeon.model.entity.Entity;
import com.dungeon.model.entity.Player;
import com.dungeon.model.entity.Projectile;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Manages all projectiles in the game world
 */
public class ProjectileManager {
    private static final double DEFAULT_PROJECTILE_SPEED = 400.0;
    private static final double DEFAULT_PROJECTILE_SIZE = 8.0;
    private static final double DEFAULT_PROJECTILE_DAMAGE = 10.0;
    private static final double DEFAULT_PROJECTILE_LIFETIME = 2.0;
    
    private final List<Projectile> projectiles;
    private double playerFireCooldown;
    private final Random random;
    
    public ProjectileManager() {
        this.projectiles = new ArrayList<>();
        this.playerFireCooldown = 0;
        this.random = new Random();
    }
    
    /**
     * Updates all projectiles and handles expired ones
     * @param deltaTime Time since last update in seconds
     */
    public void update(double deltaTime) {
        // Update cooldown
        if (playerFireCooldown > 0) {
            playerFireCooldown -= deltaTime;
        }
        
        // Update projectiles and remove expired ones
        Iterator<Projectile> iterator = projectiles.iterator();
        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();
            projectile.update(deltaTime);
            
            // Remove expired projectiles
            if (projectile.isExpired()) {
                iterator.remove();
            }
        }
    }
    
    /**
     * Renders all projectiles
     * @param gc Graphics context to render on
     */
    public void render(GraphicsContext gc) {
        for (Projectile projectile : projectiles) {
            projectile.render(gc);
        }
    }
    
    /**
     * Fires a projectile from player toward a target point
     * @param player The player entity firing the projectile
     * @param targetX Target X coordinate
     * @param targetY Target Y coordinate
     * @param weaponType The type of weapon being used
     * @return The created projectile or null if on cooldown
     */
    public Projectile firePlayerProjectile(Player player, double targetX, double targetY, Weapon.WeaponType weaponType) {
        // Check if weapon is on cooldown
        if (playerFireCooldown > 0) {
            return null;
        }
        
        // Get player center position
        double playerX = player.getX() + player.getWidth() / 2;
        double playerY = player.getY() + player.getHeight() / 2;
        
        // Calculate direction vector
        double dirX = targetX - playerX;
        double dirY = targetY - playerY;
        
        // Normalize direction
        double length = Math.sqrt(dirX * dirX + dirY * dirY);
        if (length > 0) {
            dirX /= length;
            dirY /= length;
        }
        
        // Adjust projectile properties based on weapon type
        double projectileSpeed = DEFAULT_PROJECTILE_SPEED;
        double projectileSize = DEFAULT_PROJECTILE_SIZE;
        Color projectileColor = Color.BLUE;
        double damage = player.getEquippedWeapon() != null ? player.getEquippedWeapon().getDamage() : DEFAULT_PROJECTILE_DAMAGE;
        boolean piercing = false;
        boolean explosive = false;
        double explosionRadius = 0;
        
        // Set weapon-specific properties
        switch (weaponType) {
            case BOW:
                projectileColor = Color.BROWN;
                projectileSpeed *= 1.2;
                playerFireCooldown = 1.0 / (player.getEquippedWeapon() != null ? 
                    player.getEquippedWeapon().getAttackSpeed() : 1.0);
                break;
                
            case DAGGER:
                projectileColor = Color.SILVER;
                projectileSize *= 0.8;
                projectileSpeed *= 1.5;
                playerFireCooldown = 0.3; // Fast cooldown
                break;
                
            case SPEAR:
                projectileColor = Color.GRAY;
                projectileSize *= 1.2;
                piercing = true;
                playerFireCooldown = 0.8;
                break;
                
            case AXE:
                projectileColor = Color.DARKGRAY;
                projectileSize *= 1.3;
                explosive = true;
                explosionRadius = 50;
                playerFireCooldown = 1.2; // Slow cooldown
                break;
                
            case SWORD:
            default:
                projectileColor = Color.LIGHTBLUE;
                playerFireCooldown = 0.6;
                break;
        }
        
        // Create the projectile
        Projectile projectile = new Projectile(
            playerX, playerY,
            dirX * projectileSpeed, dirY * projectileSpeed,
            damage, projectileSize, projectileColor, true
        );
        
        // Set special properties
        if (piercing) {
            projectile.setPiercing(true);
        }
        
        if (explosive) {
            projectile.setExplosive(true, explosionRadius);
        }
        
        // Add to list
        projectiles.add(projectile);
        
        return projectile;
    }
    
    /**
     * Creates an enemy projectile aimed at the player
     * @param enemy The enemy firing the projectile
     * @param player The target player
     * @param accuracy How accurate the aim is (0-1, with 1 being perfect aim)
     * @return The created projectile
     */
    public Projectile fireEnemyProjectile(Enemy enemy, Player player, double accuracy) {
        if (enemy == null || player == null) return null;
        
        // Get positions
        double enemyX = enemy.getX() + enemy.getWidth() / 2;
        double enemyY = enemy.getY() + enemy.getHeight() / 2;
        double playerX = player.getX() + player.getWidth() / 2;
        double playerY = player.getY() + player.getHeight() / 2;
        
        // Calculate direction to player with accuracy factor
        double dirX = playerX - enemyX;
        double dirY = playerY - enemyY;
        
        // Add randomness based on accuracy
        if (accuracy < 1.0) {
            double inaccuracy = (1.0 - accuracy) * Math.PI * 0.25; // Max 45 degrees off
            double angle = Math.atan2(dirY, dirX);
            angle += (random.nextDouble() * inaccuracy * 2) - inaccuracy;
            
            // Recalculate direction with new angle
            dirX = Math.cos(angle);
            dirY = Math.sin(angle);
        } else {
            // Normalize direction
            double length = Math.sqrt(dirX * dirX + dirY * dirY);
            if (length > 0) {
                dirX /= length;
                dirY /= length;
            }
        }
        
        // Set projectile properties based on enemy type
        double projectileSpeed = DEFAULT_PROJECTILE_SPEED * 0.7; // Slower than player projectiles
        double projectileSize = DEFAULT_PROJECTILE_SIZE;
        Color projectileColor = Color.RED;
        
        // Adjust based on enemy type
        if (enemy.getType() == Enemy.EnemyType.MAGE) {
            projectileColor = Color.PURPLE;
            projectileSize *= 1.2;
        } else if (enemy.getType() == Enemy.EnemyType.BOSS) {
            projectileColor = Color.DARKRED;
            projectileSize *= 1.5;
            projectileSpeed *= 1.2;
        }
        
        // Create the projectile
        Projectile projectile = new Projectile(
            enemyX, enemyY,
            dirX * projectileSpeed, dirY * projectileSpeed,
            enemy.getDamage(), projectileSize, projectileColor, false
        );
        
        // Boss projectiles can be explosive
        if (enemy.getType() == Enemy.EnemyType.BOSS && random.nextDouble() < 0.3) {
            projectile.setExplosive(true, 40);
        }
        
        // Add to list
        projectiles.add(projectile);
        
        return projectile;
    }
    
    /**
     * Creates a spread of multiple projectiles (like a shotgun)
     * @param sourceX Source X position
     * @param sourceY Source Y position
     * @param targetX Target X position
     * @param targetY Target Y position
     * @param count Number of projectiles in the spread
     * @param spreadAngle Angle of the spread in radians
     * @param damage Damage per projectile
     * @param fromPlayer Whether fired by the player
     */
    public void fireSpreadProjectiles(double sourceX, double sourceY, double targetX, double targetY,
                                     int count, double spreadAngle, double damage, boolean fromPlayer) {
        // Calculate base direction
        double dirX = targetX - sourceX;
        double dirY = targetY - sourceY;
        double baseAngle = Math.atan2(dirY, dirX);
        
        // Calculate start angle
        double startAngle = baseAngle - (spreadAngle / 2);
        double angleStep = spreadAngle / (count - 1);
        
        // Create projectiles in spread pattern
        for (int i = 0; i < count; i++) {
            double angle = startAngle + (angleStep * i);
            
            // Calculate direction from angle
            double newDirX = Math.cos(angle);
            double newDirY = Math.sin(angle);
            
            // Create projectile
            Color color = fromPlayer ? Color.BLUE : Color.RED;
            Projectile projectile = new Projectile(
                sourceX, sourceY,
                newDirX * DEFAULT_PROJECTILE_SPEED, newDirY * DEFAULT_PROJECTILE_SPEED,
                damage, DEFAULT_PROJECTILE_SIZE, color, fromPlayer
            );
            
            projectiles.add(projectile);
        }
    }
    
    /**
     * Checks projectile collisions with entities
     * @param entities List of entities to check collisions with
     */
    public void checkCollisions(List<Entity> entities) {
        if (entities == null || entities.isEmpty()) return;
        
        Iterator<Projectile> iterator = projectiles.iterator();
        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();
            
            for (Entity entity : entities) {
                // Skip null entities or wrong targets (player projectiles hit enemies, enemy projectiles hit player)
                if (entity == null || !entity.isAlive() || 
                    (projectile.isFromPlayer() && entity instanceof Player) ||
                    (!projectile.isFromPlayer() && entity instanceof Enemy)) {
                    continue;
                }
                
                if (projectile.collidesWith(entity)) {
                    // Apply damage
                    entity.takeDamage((int)projectile.getDamage());
                    
                    // Handle explosive projectiles
                    if (projectile.isExplosive()) {
                        projectile.explode(entities.toArray(new Entity[0]));
                    }
                    
                    // Remove non-piercing projectiles
                    if (!projectile.isPiercing()) {
                        iterator.remove();
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * Checks projectile collisions with walls
     * @param walls List of wall rectangles [x, y, width, height]
     */
    public void checkWallCollisions(List<double[]> walls) {
        if (walls == null || walls.isEmpty()) return;
        
        Iterator<Projectile> iterator = projectiles.iterator();
        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();
            
            for (double[] wall : walls) {
                if (wall.length < 4) continue;
                
                if (projectile.collidesWithWall(wall[0], wall[1], wall[2], wall[3])) {
                    // Handle explosive projectiles
                    if (projectile.isExplosive()) {
                        // We could pass nearby entities here if we had a spatial system
                        projectile.explode(new Entity[0]);
                    }
                    
                    iterator.remove();
                    break;
                }
            }
        }
    }
    
    /**
     * Gets all active projectiles
     * @return List of projectiles
     */
    public List<Projectile> getProjectiles() {
        return new ArrayList<>(projectiles);
    }
    
    /**
     * Clears all projectiles
     */
    public void clearProjectiles() {
        projectiles.clear();
    }
    
    /**
     * Gets the current count of active projectiles
     * @return The number of active projectiles
     */
    public int getProjectileCount() {
        return projectiles.size();
    }
} 
