package com.dungeon.model.entity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.dungeon.model.Armor;
import com.dungeon.model.Inventory;
import com.dungeon.model.Item;
import com.dungeon.model.Weapon;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

public class Player extends Entity {
    private Image playerImage;
    private static final double DEFAULT_HEALTH = 100;
    private static final double DEFAULT_SPEED = 200; // pixels per second
    private static final double DEFAULT_SIZE = 30;
    private static final double MELEE_DAMAGE = 20;
    private static final double RANGED_DAMAGE = 15;
    private static final double ATTACK_COOLDOWN = 0.5; // seconds
    
    // Combat
    private final Set<ProjectileAttack> projectiles;
    private double attackCooldown;
    private Point2D aimDirection;
    private Point2D facingDirection;
    private boolean isMeleeAttacking;
    private double meleeAttackTimer;
    private List<Weapon> weapons;
    private int currentWeaponIndex;
    private double mouseX;
    private double mouseY;
    private int selectedWeapon = 0;
    
    // Stats and progression
    private int strength;
    private int agility;
    private int intelligence;
    private final IntegerProperty experience = new SimpleIntegerProperty(0);
    private final IntegerProperty level = new SimpleIntegerProperty(1);
    private final IntegerProperty score = new SimpleIntegerProperty(0);
    private int enemiesDefeated;
    private int roomsExplored;
    
    // Equipment and inventory
    private Weapon equippedWeapon;
    private Armor equippedArmor;
    private final Inventory inventory;
    private final int maxInventorySize = 20;
    private Weapon currentWeapon;

    public Player(double x, double y) {
        super(x, y, DEFAULT_HEALTH, DEFAULT_SPEED, DEFAULT_SIZE);
         try {
            // Load the player image from resources
          playerImage = new Image(
    getClass().getResourceAsStream("/com/dungeon/assets/images/player.png"),
    64,   // width
    64,   // height
    true, // preserve ratio
    true  // smooth
);
this.size = 64;

            // Adjust size to match image dimensions if needed
            this.size = Math.max(playerImage.getWidth(), playerImage.getHeight());
        } catch (Exception e) {
            System.err.println("Error loading player image: " + e.getMessage());
            playerImage = null; 
        }
        // Initialize combat
        this.projectiles = new HashSet<>();
        this.attackCooldown = 0;
        this.aimDirection = new Point2D(1, 0);
        this.facingDirection = new Point2D(1, 0);
        this.isMeleeAttacking = false;
        this.meleeAttackTimer = 0;
        
        // Initialize weapons with basic weapons
        this.weapons = new ArrayList<>();
        this.weapons.add(Weapon.createBasicWeapon());
        this.weapons.add(new Weapon("Basic Bow", "A simple bow for ranged attacks", 15, Weapon.WeaponType.BOW));
        this.weapons.add(new Weapon("Basic Staff", "A magical staff for spellcasting", 20, Weapon.WeaponType.SPEAR));
        this.weapons.add(new Weapon("Basic Axe", "A heavy axe for powerful strikes", 25, Weapon.WeaponType.AXE));
        this.currentWeaponIndex = 0;
        
        // Initialize stats
        this.strength = 5;
        this.agility = 5;
        this.intelligence = 5;
        this.enemiesDefeated = 0;
        this.roomsExplored = 0;
        
        // Initialize inventory
        this.inventory = new Inventory(maxInventorySize);
        this.currentWeapon = Weapon.createBasicWeapon();
    }

    public void handleInput(Set<KeyCode> activeKeys, double deltaTime) {
        // Set movement direction based on WASD keys
        double dx = 0, dy = 0;
        
        if (activeKeys.contains(KeyCode.W)) {
            dy -= 1;
        }
        if (activeKeys.contains(KeyCode.S)) {
            dy += 1;
        }
        if (activeKeys.contains(KeyCode.A)) {
            dx -= 1;
        }
        if (activeKeys.contains(KeyCode.D)) {
            dx += 1;
        }
        
        // Update facing direction based on movement
        if (dx != 0 || dy != 0) {
            // Normalize the direction vector
            double length = Math.sqrt(dx * dx + dy * dy);
            dx /= length;
            dy /= length;
            facingDirection = new Point2D(dx, dy);
        }
        
        // Normalize diagonal movement
        if (dx != 0 && dy != 0) {
            double length = Math.sqrt(dx * dx + dy * dy);
            dx /= length;
            dy /= length;
        }
        
        // Apply movement
        if (dx != 0 || dy != 0) {
            move(dx * speed * deltaTime, dy * speed * deltaTime);
        }
        
        // Handle dash with SHIFT key
        if (activeKeys.contains(KeyCode.SHIFT)) {
            // Dash in movement direction
            if (dx != 0 || dy != 0) {
                // Simple dash - teleport a short distance
                move(dx * 50, dy * 50);
            }
        }
    }
    
    public void updateMousePosition(double mouseX, double mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        
        // Calculate aim direction
        double centerX = position.getX() + size / 2;
        double centerY = position.getY() + size / 2;
        
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        
        // Normalize the direction vector
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length > 0) {
            dx /= length;
            dy /= length;
        }
        
        this.aimDirection = new Point2D(dx, dy);
    }

    private void meleeAttack() {
        isMeleeAttacking = true;
        // Melee attack animation will be handled in render method
        // and collision detection in the game controller
    }
    
    private void rangedAttack() {
        // Calculate direction to mouse cursor
        Point2D playerCenter = position.add(size / 2, size / 2);
        Point2D mousePos = new Point2D(mouseX, mouseY);
        Point2D direction = mousePos.subtract(playerCenter).normalize();
        
        // Create projectile
        ProjectileAttack projectile = new ProjectileAttack(
            playerCenter.getX(), 
            playerCenter.getY(),
            direction,
            RANGED_DAMAGE,
            Color.YELLOW,
            ProjectileAttack.ProjectileType.ARROW
        );
        
        // Add to projectiles list
        projectiles.add(projectile);
    }

    @Override
    public void update(double deltaTime) {
        super.update(deltaTime);
        
        // Update projectiles
        projectiles.removeIf(ProjectileAttack::isExpired);
        for (ProjectileAttack projectile : projectiles) {
            projectile.update(deltaTime);
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        if (playerImage != null) {
        // Save the current graphics context state
        gc.save();
        
            // Calculate the center point of the player
        double centerX = position.getX() + size / 2;
        double centerY = position.getY() + size / 2;
        
            // Calculate rotation angle based on facing direction
            double angle;
            if (facingDirection.getY() < 0) {
                // When facing up
                angle = -90;
            } else if (facingDirection.getY() > 0) {
                // When facing down
                angle = 90;
            } else {
                // When facing right or left
                angle = 0;
            }
            
            // Translate to the center of the player
        gc.translate(centerX, centerY);
            
            // Rotate around the center
            gc.rotate(angle);
            
            // Draw the image centered, flip horizontally if facing left
            if (facingDirection.getX() < 0) {
                gc.scale(-1, 1); // Flip horizontally
                gc.drawImage(playerImage, -size/2, -size/2, size, size);
            } else {
        gc.drawImage(playerImage, -size/2, -size/2, size, size);
            }
        
        // Restore the graphics context state
        gc.restore();
    } else {
            // Fallback to basic shape if image fails to load
        gc.setFill(Color.DARKBLUE);
        gc.fillOval(position.getX(), position.getY(), size, size);
        
            // Draw player face (eyes and mouth) looking in facing direction
        double centerX = position.getX() + size / 2;
        double centerY = position.getY() + size / 2;
        
        // Draw eyes
        double eyeSize = size / 8;
            double eyeOffsetX = facingDirection.getX() * size / 6;
            double eyeOffsetY = facingDirection.getY() * size / 6;
        
        // Left eye
        gc.setFill(Color.WHITE);
        gc.fillOval(
            centerX - size/4 + eyeOffsetX/2, 
            centerY - size/4 + eyeOffsetY/2, 
            eyeSize, 
            eyeSize
        );
        
        // Right eye
        gc.fillOval(
            centerX + size/4 + eyeOffsetX/2, 
            centerY - size/4 + eyeOffsetY/2, 
            eyeSize, 
            eyeSize
        );
        }
        
        // Draw weapon if melee attacking
        if (isMeleeAttacking) {
            gc.setStroke(Color.SILVER); // Default weapon color
            gc.setLineWidth(3);
            double centerX = position.getX() + size / 2;
            double centerY = position.getY() + size / 2;
            gc.strokeLine(
                centerX,
                centerY,
                centerX + aimDirection.getX() * size * 1.5,
                centerY + aimDirection.getY() * size * 1.5
            );
        }
        
        // Draw projectiles
        for (ProjectileAttack projectile : projectiles) {
            projectile.render(gc);
        }
    }

    // Inventory methods
    public boolean addItem(Item item) {
        if (item == null) return false;
        return inventory.addItem(item);
    }
    
    public boolean removeItem(Item item) {
        return inventory.removeItem(item);
    }
    
    public boolean useItem(Item item) {
        if (item == null || !inventory.hasItem(item.getType())) return false;
        
        boolean used = false;
        
        switch (item.getType()) {
            case POTION:
                // Healing potion - heals the player
                heal(item.getValue());
                used = true;
                break;
                
            case WEAPON:
                // Convert Item to Weapon
                try {
                    Weapon.WeaponType weaponType = Weapon.WeaponType.valueOf(item.getName().toUpperCase());
                    Weapon weapon = new Weapon(
                        item.getName(),
                        item.getDescription(),
                        item.getValue(),
                        weaponType
                    );
                    this.equippedWeapon = weapon;
                    used = true;
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid weapon type: " + item.getName());
                }
                break;
                
            case ARMOR:
                // Convert Item to Armor
                try {
                    Armor.ArmorType armorType = Armor.ArmorType.valueOf(item.getName().toUpperCase());
                    Armor armor = new Armor(
                        item.getName(),
                        item.getDescription(),
                        item.getValue(),
                        armorType
                    );
                    this.equippedArmor = armor;
                    used = true;
                } catch (IllegalArgumentException e) {
                    // If name doesn't match armor type, use default
                    Armor armor = new Armor(
                        item.getName(),
                        item.getDescription(),
                        item.getValue(),
                        Armor.ArmorType.LIGHT
                    );
                    this.equippedArmor = armor;
                used = true;
                }
                break;
                
            default:
                // Other item types can be added later
                break;
        }
        
        // Remove consumable items after use
        if (used && item.isConsumable()) {
            inventory.removeItem(item.getType(), 1);
        }
        
        return used;
    }

    // Experience and leveling methods
    public void addExperience(int xp) {
        if (xp <= 0) return;
        
        experience.set(experience.get() + xp);
        
        // Check for level up
        // Level formula: Each level requires level * 100 XP
        int xpForNextLevel = level.get() * 100;
        
        while (experience.get() >= xpForNextLevel) {
            levelUp();
            xpForNextLevel = level.get() * 100;
        }
    }
    
    private void levelUp() {
        level.set(level.get() + 1);
        
        // Increase max health by 20 per level
        setMaxHealth(getMaxHealth() + 20);
        heal(getMaxHealth()); // Fully heal on level up
        
        // Increase stats (can be randomized or based on player choices in a full game)
        strength += 2;
        agility += 1;
        intelligence += 1;
    }

    // Score and stats methods
    public void addScore(int points) {
        score.set(score.get() + points);
    }
    
    public void recordEnemyDefeated() {
        enemiesDefeated++;
        // Add score for defeating enemy
        addScore(25);
    }
    
    public void recordRoomExplored() {
        roomsExplored++;
        // Add score for exploring a room
        addScore(10);
    }

    // Getters and setters
    public boolean isMeleeAttacking() {
        return isMeleeAttacking;
    }

    public double getMeleeDamage() {
        return equippedWeapon != null ? equippedWeapon.getDamage() : MELEE_DAMAGE;
    }

    public Set<ProjectileAttack> getProjectiles() {
        return new HashSet<>(projectiles);
    }

    public Inventory getInventory() {
        return inventory;
    }
    
    public Weapon getCurrentWeapon() {
        return weapons.get(currentWeaponIndex);
    }

    public int getStrength() {
        return strength;
    }
    
    public void setStrength(int strength) {
        this.strength = strength;
    }
    
    public int getAgility() {
        return agility;
    }
    
    public void setAgility(int agility) {
        this.agility = agility;
    }
    
    public int getIntelligence() {
        return intelligence;
    }
    
    public void setIntelligence(int intelligence) {
        this.intelligence = intelligence;
    }
    
    public Weapon getEquippedWeapon() {
        return equippedWeapon;
    }
    
    public void setEquippedWeapon(Weapon weapon) {
        this.equippedWeapon = weapon;
    }
    
    public Armor getEquippedArmor() {
        return equippedArmor;
    }
    
    public void setEquippedArmor(Armor armor) {
        this.equippedArmor = armor;
    }
    
    public int getLevel() {
        return level.get();
    }
    
    public IntegerProperty levelProperty() {
        return level;
    }
    
    public int getExperience() {
        return experience.get();
    }
    
    public IntegerProperty experienceProperty() {
        return experience;
    }
    
    public int getScore() {
        return score.get();
    }
    
    public IntegerProperty scoreProperty() {
        return score;
    }
    
    public int getEnemiesDefeated() {
        return enemiesDefeated;
    }
    
    public int getRoomsExplored() {
        return roomsExplored;
    }
    
    public void selectWeapon(int index) {
        if (index >= 0 && index < weapons.size()) {
            this.currentWeaponIndex = index;
            this.selectedWeapon = index;
        }
    }

    public void move(double dx, double dy) {
        // Calculate new position
        double newX = position.getX() + dx;
        double newY = position.getY() + dy;
        
        // Set new position (boundary checking would be done in GameController)
        position = new Point2D(newX, newY);
    }

    public Weapon getWeapon() {
        return currentWeapon;
    }
    
    public void setWeapon(Weapon weapon) {
        this.currentWeapon = weapon;
    }

    public List<Weapon> getWeapons() {
        return weapons;
    }
    
    public void setWeapons(List<Weapon> weapons) {
        this.weapons = weapons;
    }
}
