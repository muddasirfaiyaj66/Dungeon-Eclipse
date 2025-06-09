package com.dungeon.model;

import javafx.scene.paint.Color;

public class Armor extends Item {
    private double mobilityFactor; // Affects movement speed (lower = slower)
    private ArmorType armorType;
    
    public enum ArmorType {
        LIGHT(0.10, 1.1, Color.LIGHTBLUE),    // 10% damage reduction
        MEDIUM(0.20, 1.0, Color.SLATEGRAY),   // 20% damage reduction
        HEAVY(0.30, 0.8, Color.DARKRED);     // 30% damage reduction
        
        private final double damageReductionPercentage;
        private final double mobilityFactor;
        private final Color tintColor; // Color for visual effect
        
        ArmorType(double damageReductionPercentage, double mobilityFactor, Color tintColor) {
            this.damageReductionPercentage = damageReductionPercentage;
            this.mobilityFactor = mobilityFactor;
            this.tintColor = tintColor;
        }
        
        public double getDamageReductionPercentage() {
            return damageReductionPercentage;
        }
        
        public double getMobilityFactor() {
            return mobilityFactor;
        }

        public Color getTintColor() {
            return tintColor;
        }
    }
    
    /**
     * Creates a new armor piece
     * @param name Armor name
     * @param description Armor description
     * @param armorType Type of armor
     * @param iconPath Path to the armor icon (can be null if not used)
     */
    public Armor(String name, String description, ArmorType armorType, String iconPath) {
        
        super(name, description, ItemType.ARMOR, 0, false, iconPath); 
        this.armorType = armorType;
        this.mobilityFactor = armorType.getMobilityFactor();
    }
    
    /**
     * Creates a new armor piece with default icon (null for now as armor images are not drawn on player)
     * @param name Armor name
     * @param description Armor description
     * @param armorType Type of armor
     */
    public Armor(String name, String description, ArmorType armorType) {
        this(name, description, armorType, null);
    }
    
    /**
     * Gets the armor's mobility factor (affects movement speed)
     * @return Mobility factor (lower means slower movement)
     */
    public double getMobilityFactor() {
        return mobilityFactor;
    }
    
    /**
     * Gets the armor's type
     * @return Armor type
     */
    public ArmorType getArmorType() {
        return armorType;
    }
    
    /**
     * Creates a basic starter armor for new players
     * @return Basic leather armor
     */
    public static Armor createBasicArmor() {
        // baseDefense is no longer used in constructor
        return new Armor("Leather Armor", "Basic protection that doesn't restrict movement",
                ArmorType.LIGHT);
    }
    
    /**
     * Creates a random armor of the specified tier
     * @param tier Tier/quality of the armor (1-3)
     * @return A randomly generated armor piece
     */
    public static Armor createRandomArmor(int tier) {
        tier = Math.max(1, Math.min(3, tier));
        // baseDefense is no longer used in constructor
        
        ArmorType[] types = ArmorType.values();
        ArmorType randomType = types[(int)(Math.random() * types.length)];
        
        String[] tierNames = {"Basic", "Reinforced", "Masterwork"};
        String name = tierNames[tier - 1] + " " + getArmorTypeName(randomType);
        String description = generateDescription(randomType, tier);
        
        // Pass randomType to the constructor
        return new Armor(name, description, randomType);
    }
    
    /**
     * Gets a display name for the armor type
     */
    private static String getArmorTypeName(ArmorType type) {
        switch (type) {
            case LIGHT: return "Leather Armor";
            case MEDIUM: return "Chainmail";
            case HEAVY: return "Plate Armor";
            default: return "Armor";
        }
    }
    
    /**
     * Generates a description based on armor type and tier
     */
    private static String generateDescription(ArmorType type, int tier) {
        String quality = (tier == 1) ? "adequate" : (tier == 2) ? "good" : "excellent";
        String reduction = (int)(type.getDamageReductionPercentage() * 100) + "%";
        
        switch (type) {
            case LIGHT:
                return "A " + quality + " set of light leather armor. Reduces damage by " + reduction + ". Allows quick movement.";
            case MEDIUM:
                return "A " + quality + " chainmail suit. Reduces damage by " + reduction + ". Offers balanced protection and mobility.";
            case HEAVY:
                return "A " + quality + " plate armor. Reduces damage by " + reduction + ". Provides strong protection at the cost of mobility.";
            default:
                return "A " + quality + " set of armor. Reduces damage by " + reduction + ".";
        }
    }
} 