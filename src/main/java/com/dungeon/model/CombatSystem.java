package com.dungeon.model;

import com.dungeon.model.entity.Enemy;
import com.dungeon.model.entity.Entity;
import com.dungeon.model.entity.Player;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.List;
import java.util.Random;

/**
 * Handles all combat interactions between player and enemies.
 * Manages attack calculations, damage application, and combat outcomes.
 */
public class CombatSystem {
    private static final Random random = new Random();
    
    // Attack types with different damage multipliers
    public enum AttackType {
        NORMAL(1.0),
        HEAVY(1.5),
        QUICK(0.7),
        SPECIAL(2.0);
        
        private final double damageMultiplier;
        
        AttackType(double damageMultiplier) {
            this.damageMultiplier = damageMultiplier;
        }
        
        public double getDamageMultiplier() {
            return damageMultiplier;
        }
    }
    
    /**
     * Executes a player attack against a target enemy
     * 
     * @param player The player performing the attack
     * @param enemy The enemy being attacked
     * @param attackType The type of attack performed
     * @return The amount of damage dealt
     */
    public static int playerAttack(Player player, Enemy enemy, AttackType attackType) {
        if (player == null || enemy == null) return 0;
        
        // Calculate base damage using player's weapon
        double baseDamage = player.getEquippedWeapon() != null ? 
            player.getEquippedWeapon().getDamage() : 10; // Default damage if no weapon
        
        // Apply attack type multiplier
        double multiplier = attackType.getDamageMultiplier();
        
        // Apply player's strength attribute (each point of strength adds 5% damage)
        multiplier += (player.getStrength() * 0.05);
        
        // Apply random variation (±20%)
        double variation = 0.8 + (random.nextDouble() * 0.4);
        
        // Calculate final damage
        int damage = (int) Math.max(1, Math.round(baseDamage * multiplier * variation));
        
        // Apply damage to enemy
        enemy.takeDamage(damage);
        
        return damage;
    }
    
    /**
     * Executes an enemy attack against the player
     * 
     * @param enemy The enemy performing the attack
     * @param player The player being attacked
     * @return The amount of damage dealt
     */
    public static int enemyAttack(Enemy enemy, Player player) {
        if (enemy == null || player == null) return 0;
        
        // Base damage from enemy
        double baseDamage = enemy.getDamage();
        
        // Apply random variation (±20%)
        double variation = 0.8 + (random.nextDouble() * 0.4);
        
        // Calculate final damage before armor
        int damageBeforeArmor = (int) Math.max(1, Math.round(baseDamage * variation));
        
        // Apply damage to player. Player.takeDamage() will handle armor reduction.
        player.takeDamage(damageBeforeArmor);
        
        // To return the damage *dealt* after armor, we'd need to know what Player.takeDamage actually did.
        // For now, let's assume this method should return the damage *before* armor, 
        // or we need to change Player.takeDamage to return actual damage taken.
        // Given Player.takeDamage now returns void, we'll return damageBeforeArmor.
        // Or, more consistently with how player.getLastDamageBlocked() works, calculate it here:
        double damageBlocked = player.getLastDamageBlocked(); // Call this *after* player.takeDamage
        int actualDamageDealt = (int) Math.max(0, damageBeforeArmor - damageBlocked);

        // System.out.println("[CombatSystem.enemyAttack] Damage Before Armor: " + damageBeforeArmor + ", Blocked: " + damageBlocked + ", Actual Dealt: " + actualDamageDealt);
        
        return actualDamageDealt; // Return the damage dealt after armor reduction
    }
    
    /**
     * Executes a special area-of-effect attack that damages multiple enemies
     * 
     * @param player The player performing the attack
     * @param enemies List of enemies in the area of effect
     * @param attackType The type of attack (usually SPECIAL)
     * @return Array of damage values corresponding to each enemy
     */
    public static int[] areaAttack(Player player, List<Enemy> enemies, AttackType attackType) {
        if (player == null || enemies == null || enemies.isEmpty()) return new int[0];
        
        int[] damageDealt = new int[enemies.size()];
        
        // Calculate base damage
        double baseDamage = player.getEquippedWeapon() != null ? 
            player.getEquippedWeapon().getDamage() : 10; // Default damage if no weapon
        
        // Apply attack type multiplier
        double multiplier = attackType.getDamageMultiplier();
        
        // Area attacks deal less damage per enemy (60% damage)
        multiplier *= 0.6;
        
        // Apply player's strength
        multiplier += (player.getStrength() * 0.05);
        
        // Deal damage to each enemy
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            
            // Apply random variation for each enemy
            double variation = 0.8 + (random.nextDouble() * 0.4);
            
            // Calculate damage
            int damage = (int) Math.max(1, Math.round(baseDamage * multiplier * variation));
            
            // Apply damage
            enemy.takeDamage(damage);
            damageDealt[i] = damage;
        }
        
        return damageDealt;
    }
    
    /**
     * Checks if any enemies are in melee range of the player
     * 
     * @param player The player
     * @param enemies List of enemies to check
     * @param meleeRange The distance considered within melee range
     * @return true if any enemy is in melee range
     */
    public static boolean isEnemyInMeleeRange(Player player, List<Enemy> enemies, double meleeRange) {
        if (player == null || enemies == null) return false;
        
        for (Enemy enemy : enemies) {
            double dx = player.getX() - enemy.getX();
            double dy = player.getY() - enemy.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            if (distance <= meleeRange) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Checks if a specific enemy is within attack range
     * 
     * @param attacker The attacker's x,y position
     * @param target The target's x,y position
     * @param attackRange The maximum attack range
     * @return true if the target is in range
     */
    public static boolean isInRange(Entity attacker, Entity target, double attackRange) {
        if (attacker == null || target == null) return false;
        
        double dx = attacker.getX() - target.getX();
        double dy = attacker.getY() - target.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        return distance <= attackRange;
    }
    
    /**
     * Applies damage to an entity from a trap or environmental hazard
     * 
     * @param entity The entity taking damage
     * @param trapDamage The amount of damage the trap inflicts
     * @return The actual damage dealt after modifications
     */
    public static int applyTrapDamage(Entity entity, int trapDamage) {
        if (entity == null || trapDamage <= 0) return 0;
        
        // Traps ignore armor but have a random variation
        double variation = 0.9 + (random.nextDouble() * 0.2);
        int finalDamage = (int) Math.max(1, Math.round(trapDamage * variation));
        
        entity.takeDamage(finalDamage);
        return finalDamage;
    }
    
    // Helper method to calculate damage from weapon
    private double calculateWeaponDamage(Player player) {
        double baseDamage = 10; // Default damage if no weapon
        
        if (player.getEquippedWeapon() != null) {
            // Use the weapon's damage value
            baseDamage = player.getEquippedWeapon().getDamage();
            
            // Apply strength bonus (5% damage per strength point)
            double strengthBonus = player.getStrength() * 0.05;
            baseDamage = baseDamage * (1 + strengthBonus);
        }
        
        return baseDamage;
    }
    
    // Calculate crit damage for a player attack
    private double calculateCriticalDamage(Player player, double baseDamage) {
        double critChance = 0.05; // Base 5% chance
        
        if (player.getEquippedWeapon() != null) {
            // Increase crit chance based on weapon type
            if (player.getEquippedWeapon().getWeaponType() == Weapon.WeaponType.DAGGER) {
                critChance += 0.1; // Daggers have higher crit chance
            }
        }
        
        return critChance;
    }
} 