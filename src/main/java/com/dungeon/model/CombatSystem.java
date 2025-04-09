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
        MELEE(1.0),
        RANGED(0.8),
        SPECIAL(1.5);
        
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
        double baseDamage = player.getEquippedWeapon() != null ? player.getEquippedWeapon().getType().getDamage() : 1;
        
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
        
        // Calculate final damage
        int damage = (int) Math.max(1, Math.round(baseDamage * variation));
        
        // Apply armor reduction if player has armor (each armor point reduces damage by 5%)
        if (player.getEquippedArmor() != null) {
            double armorValue = player.getEquippedArmor().getDefense();
            double reduction = armorValue * 0.05;
            // Cap reduction at 80%
            reduction = Math.min(0.8, reduction);
            damage = (int) Math.max(1, Math.round(damage * (1 - reduction)));
        }
        
        // Apply damage to player
        player.takeDamage(damage);
        
        return damage;
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
        double baseDamage = player.getEquippedWeapon() != null ? player.getEquippedWeapon().getType().getDamage() : 1;
        
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
            // Use the weapon's damage value, affected by weapon type
            baseDamage = player.getEquippedWeapon().getType().getDamage();
            
            // Apply strength bonus (5% damage per strength point)
            double strengthBonus = player.getStrength() * 0.05;
            baseDamage = baseDamage * (1 + strengthBonus);
        }
        
        return baseDamage;
    }
    
    // Calculate damage reduction from armor
    private double calculateDamageReduction(Player player) {
        double reduction = 0.0; // Default: no reduction
        
        if (player.getEquippedArmor() != null) {
            // Base reduction from armor value (percentage)
            reduction = player.getEquippedArmor().getDefense() / 100.0;
            
            // Apply agility bonus (smaller reduction for higher agility)
            reduction = reduction * (1 + player.getAgility() * 0.02);
        }
        
        // Cap reduction at 75%
        return Math.min(reduction, 0.75);
    }
    
    // Calculate crit damage for a player attack
    private double calculateCriticalDamage(Player player, double baseDamage) {
        double critChance = 0.05; // Base 5% chance
        
        if (player.getEquippedWeapon() != null) {
            // Increase crit chance based on weapon type
            if (player.getEquippedWeapon().getType() == Player.WeaponType.DAGGER) {
                critChance += 0.1; // Daggers have higher crit chance
            }
        }
        
        return critChance;
    }
} 