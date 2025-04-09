package com.dungeon.model;

/**
 * Represents armor that the player can equip to reduce damage from enemies
 */
public class Armor extends Item {
    private int defense;
    private double mobilityFactor; // Affects movement speed (lower = slower)
    private ArmorType armorType;
    
    /**
     * Types of armor with different attributes
     */
    public enum ArmorType {
        LIGHT(0.7, 1.1),    // Light defense, high mobility
        MEDIUM(1.0, 1.0),   // Balanced
        HEAVY(1.5, 0.8);    // High defense, reduced mobility
        
        private final double defenseMultiplier;
        private final double mobilityFactor;
        
        ArmorType(double defenseMultiplier, double mobilityFactor) {
            this.defenseMultiplier = defenseMultiplier;
            this.mobilityFactor = mobilityFactor;
        }
        
        public double getDefenseMultiplier() {
            return defenseMultiplier;
        }
        
        public double getMobilityFactor() {
            return mobilityFactor;
        }
    }
    
    /**
     * Creates a new armor piece
     * @param name Armor name
     * @param description Armor description
     * @param baseDefense Base defense value
     * @param armorType Type of armor
     * @param iconPath Path to armor icon (optional)
     */
    public Armor(String name, String description, int baseDefense, ArmorType armorType, String iconPath) {
        super(name, description, ItemType.ARMOR, baseDefense, false, iconPath);
        
        this.armorType = armorType;
        this.defense = (int)(baseDefense * armorType.getDefenseMultiplier());
        this.mobilityFactor = armorType.getMobilityFactor();
    }
    
    /**
     * Creates a new armor piece with default icon
     * @param name Armor name
     * @param description Armor description
     * @param baseDefense Base defense value
     * @param armorType Type of armor
     */
    public Armor(String name, String description, int baseDefense, ArmorType armorType) {
        this(name, description, baseDefense, armorType, null);
    }
    
    /**
     * Gets the armor's defense value
     * @return Defense value
     */
    public int getDefense() {
        return defense;
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
        return new Armor("Leather Armor", "Basic protection that doesn't restrict movement",
                5, ArmorType.LIGHT);
    }
    
    /**
     * Creates a random armor of the specified tier
     * @param tier Tier/quality of the armor (1-3)
     * @return A randomly generated armor piece
     */
    public static Armor createRandomArmor(int tier) {
        // Clamp tier between 1-3
        tier = Math.max(1, Math.min(3, tier));
        
        // Base defense increases with tier
        int baseDefense = 5 + (tier - 1) * 5;
        
        // Random armor type
        ArmorType[] types = ArmorType.values();
        ArmorType randomType = types[(int)(Math.random() * types.length)];
        
        // Generate name and description based on tier and type
        String[] tierNames = {"Basic", "Reinforced", "Masterwork"};
        String name = tierNames[tier - 1] + " " + getArmorTypeName(randomType);
        String description = generateDescription(randomType, tier);
        
        return new Armor(name, description, baseDefense, randomType);
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
        
        switch (type) {
            case LIGHT:
                return "A " + quality + " set of light leather armor that allows quick movement.";
            case MEDIUM:
                return "A " + quality + " chainmail suit offering balanced protection and mobility.";
            case HEAVY:
                return "A " + quality + " plate armor providing strong protection at the cost of mobility.";
            default:
                return "A " + quality + " set of armor.";
        }
    }
} 