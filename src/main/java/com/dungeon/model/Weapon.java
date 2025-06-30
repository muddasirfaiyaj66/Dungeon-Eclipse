package com.dungeon.model;

import javafx.scene.paint.Color;

/**
 * Represents a weapon that the player can equip to deal damage to enemies
 */
public class Weapon extends Item {
    private final int baseDamage;
    private final int actualDamage;
    private final int originalBaseDamage;
    private double attackRange;
    private double attackSpeed; // Attacks per second
    private WeaponType weaponType;
    private boolean isPuzzleReward = false;
    
    /**
     * Types of weapons with different attributes
     */
    public enum WeaponType {
        SWORD(1.0, 50, 1.5, "/com/dungeon/assets/images/SWORD.png", Color.LIGHTGRAY),    // Balanced
        DAGGER(0.7, 40, 2.0, "/com/dungeon/assets/images/DAGGER.png", Color.SILVER),   // Fast but short range
        AXE(1.3, 45, 1.0, "/com/dungeon/assets/images/AXE.png", Color.BROWN),      // High damage but slow
        SPEAR(0.9, 70, 1.2, "/com/dungeon/assets/images/SPEAR.png", Color.SANDYBROWN),    // Long range
        BOW(0.8, 200, 0.8, "/com/dungeon/assets/images/BOW.png", Color.GREENYELLOW);     // Ranged weapon
        
        private final double damageMultiplier;
        private final double range;
        private final double speedMultiplier;
        private final String imagePath;
        private final Color projectileColor; // Added for projectile visual
        
        WeaponType(double damageMultiplier, double range, double speedMultiplier, String imagePath, Color projectileColor) {
            this.damageMultiplier = damageMultiplier;
            this.range = range;
            this.speedMultiplier = speedMultiplier;
            this.imagePath = imagePath;
            this.projectileColor = projectileColor;
        }
        
        public double getDamageMultiplier() {
            return damageMultiplier;
        }
        
        public double getRange() {
            return range;
        }
        
        public double getSpeedMultiplier() {
            return speedMultiplier;
        }
        
        public String getImagePath() {
            return imagePath;
        }

        public Color getProjectileColor() {
            return projectileColor;
        }
    }
    
    /**
     * Creates a new weapon
     * @param name Weapon name
     * @param description Weapon description
     * @param baseDamage Base damage value
     * @param weaponType Type of weapon
     */
    public Weapon(String name, String description, int baseDamage, WeaponType weaponType, boolean isPuzzleReward) {
        super(name, description, ItemType.WEAPON, baseDamage, false, weaponType.getImagePath());
        this.originalBaseDamage = baseDamage;
        this.baseDamage = baseDamage;
        this.weaponType = weaponType;
        this.isPuzzleReward = isPuzzleReward;
        this.actualDamage = isPuzzleReward ? (int)Math.round(baseDamage * 0.5) : baseDamage;
        this.attackRange = weaponType.getRange();
        this.attackSpeed = 1.0 * weaponType.getSpeedMultiplier();
    }
    
    /**
     * Gets the weapon's calculated damage value
     * @return Calculated damage value
     */
    public int getDamage() {
        return actualDamage;
    }

    /**
     * Gets the weapon's original base damage value
     * @return Original base damage value
     */
    public int getOriginalBaseDamage() {
        return originalBaseDamage;
    }
    
    /**
     * Gets the weapon's attack range
     * @return Attack range
     */
    public double getAttackRange() {
        return attackRange;
    }
    
    /**
     * Gets the weapon's attack speed (attacks per second)
     * @return Attack speed
     */
    public double getAttackSpeed() {
        return attackSpeed;
    }
    
    /**
     * Gets the weapon's type
     * @return Weapon type
     */
    public WeaponType getWeaponType() {
        return weaponType;
    }
    
    /**
     * Calculates the weapon's DPS (Damage Per Second)
     * @return DPS value
     */
    public double getDPS() {
        return baseDamage * attackSpeed;
    }
    
    /**
     * Creates a basic starter weapon for new players
     * @return A basic sword
     */
    public static Weapon createBasicWeapon() {
        return new Weapon("Rusty Sword", "A basic sword with moderate damage", 
                20, WeaponType.SWORD, false);
    }
    
    /**
     * Creates a random weapon of the specified tier
     * @param tier Tier/quality of the weapon (1-3)
     * @return A randomly generated weapon
     */
    public static Weapon createRandomWeapon(int tier, boolean isPuzzleReward) {
        tier = Math.max(1, Math.min(3, tier));
        WeaponType[] types = WeaponType.values();
        WeaponType randomType = types[(int)(Math.random() * types.length)];
        int baseDamage = getBaseDamageForWeaponType(randomType);
        baseDamage += (tier - 1) * 5;
        int shownDamage = isPuzzleReward ? (int)Math.round(baseDamage * 0.5) : baseDamage;
        String[] tierNames = {"Basic", "Refined", "Masterwork"};
        String name = tierNames[tier - 1] + " " + getWeaponTypeName(randomType);
        String description = generateDescription(randomType, tier, shownDamage, isPuzzleReward);
        return new Weapon(name, description, baseDamage, randomType, isPuzzleReward);
    }
    
    /**
     * Gets the base damage for a specific weapon type
     */
    private static int getBaseDamageForWeaponType(WeaponType type) {
        switch (type) {
            case DAGGER: return 17;  // 17 * 0.7 = 12 damage
            case SPEAR: return 18;   // 18 * 0.9 = 16 damage  
            case SWORD: return 20;   // 20 * 1.0 = 20 damage
            case BOW: return 23;     // 23 * 0.8 = 18 damage
            case AXE: return 19;     // 19 * 1.3 = 25 damage
            default: return 20;
        }
    }
    
    /**
     * Gets a display name for the weapon type
     */
    private static String getWeaponTypeName(WeaponType type) {
        switch (type) {
            case SWORD: return "Sword";
            case DAGGER: return "Dagger";
            case AXE: return "Battle Axe";
            case SPEAR: return "Spear";
            case BOW: return "Longbow";
            default: return "Weapon";
        }
    }
    
    /**
     * Generates a description based on weapon type and tier
     */
    private static String generateDescription(WeaponType type, int tier, int damage, boolean isPuzzleReward) {
        String quality = (tier == 1) ? "adequate" : (tier == 2) ? "good" : "excellent";
        String rewardNote = isPuzzleReward ? " (Puzzle Reward: Half Damage)" : "";
        String damageText = "Deals " + damage + " damage" + rewardNote + ".";
        switch (type) {
            case SWORD:
                return "A " + quality + " sword. " + damageText + " Balanced attack properties.";
            case DAGGER:
                return "A " + quality + " dagger. " + damageText + " Strikes quickly but with shorter range.";
            case AXE:
                return "A " + quality + " battle axe. " + damageText + " Swings slowly.";
            case SPEAR:
                return "A " + quality + " spear. " + damageText + " Extended reach.";
            case BOW:
                return "A " + quality + " bow. " + damageText + " For attacking from a distance.";
            default:
                return "A " + quality + " weapon. " + damageText;
        }
    }

    public boolean isPuzzleReward() {
        return isPuzzleReward;
    }
} 