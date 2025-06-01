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
    private static final javafx.scene.paint.Color DEFAULT_PROJECTILE_COLOR = javafx.scene.paint.Color.YELLOW;
    
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
        
        double damageToDeal = RANGED_DAMAGE; 
        Color projectileColor = DEFAULT_PROJECTILE_COLOR;
        // ProjectileAttack.ProjectileType type = ProjectileAttack.ProjectileType.ARROW; // Default type

        if (equippedWeapon != null) {
            damageToDeal = equippedWeapon.getDamage();
            if (equippedWeapon.getWeaponType() != null) {
                Color colorFromWeapon = equippedWeapon.getWeaponType().getProjectileColor();
                if (colorFromWeapon != null) {
                    projectileColor = colorFromWeapon;
                }
                // Future: Consider deriving ProjectileAttack.ProjectileType from WeaponType
                // For example:
                // if (equippedWeapon.getWeaponType() == Weapon.WeaponType.STAFF) { // Assuming Weapon.WeaponType exists
                //    type = ProjectileAttack.ProjectileType.MAGIC_BOLT; // Assuming such a type exists in ProjectileAttack
                // }
            }
        }
        
        // Create projectile
        ProjectileAttack projectile = new ProjectileAttack(
            playerCenter.getX(), 
            playerCenter.getY(),
            direction,
            damageToDeal,
            projectileColor,
            ProjectileAttack.ProjectileType.ARROW // Keeping ARROW type for now as it was not the reported issue
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
        // Render Player Body (rotates with WASD facingDirection)
        if (playerImage != null) {
            gc.save(); // Save for player body transformations
            double playerCenterX = position.getX() + size / 2;
            double playerCenterY = position.getY() + size / 2;
            double bodyAngle;
            if (facingDirection.getY() < -0.1) { 
                bodyAngle = -90; 
            } else if (facingDirection.getY() > 0.1) { 
                bodyAngle = 90;  
            } else {
                bodyAngle = 0;   
            }
            gc.translate(playerCenterX, playerCenterY);
            gc.rotate(bodyAngle);
            if (facingDirection.getX() < 0 && bodyAngle == 0) { 
                gc.scale(-1, 1); 
            }
            gc.drawImage(playerImage, -size / 2, -size / 2, size, size);
            
            if (equippedArmor != null) {
                Color armorColor = equippedArmor.getArmorType().getTintColor();

                // Apply tint
                gc.setFill(armorColor);
                gc.setGlobalAlpha(0.3); // Keep tint subtle
                gc.fillRect(-size / 2, -size / 2, size, size);

                // Apply outline
                gc.setStroke(armorColor.darker()); // Darker shade for contrast
                gc.setLineWidth(2); // A visible line width, can be adjusted
                gc.setGlobalAlpha(0.7); // Make outline more prominent than tint
                gc.strokeRect(-size / 2, -size / 2, size, size);

                gc.setGlobalAlpha(1.0); // Reset alpha for subsequent drawing
            }
            gc.restore(); // Restore from player body transformations
        } else {
            // Fallback player rendering (unchanged)
            gc.setFill(Color.DARKBLUE);
            gc.fillOval(position.getX(), position.getY(), size, size);
            double cX = position.getX() + size / 2;
            double cY = position.getY() + size / 2;
            double eyeSize = size / 8;
            double eyeOffsetX = facingDirection.getX() * size / 6;
            double eyeOffsetY = facingDirection.getY() * size / 6;
            gc.setFill(Color.WHITE);
            gc.fillOval(cX - size/4 + eyeOffsetX/2, cY - size/4 + eyeOffsetY/2, eyeSize, eyeSize);
            gc.fillOval(cX + size/4 + eyeOffsetX/2, cY + size/4 + eyeOffsetY/2, eyeSize, eyeSize); // Corrected Y offset for right eye
        }

        // Render Equipped Weapon (rotates with mouse aimDirection, independent of body)
        if (equippedWeapon != null && equippedWeapon.getIcon() != null && !isMeleeAttacking) {
            gc.save(); // Save for weapon transformations
            Image weaponIcon = equippedWeapon.getIcon();
            double weaponSize = size * 0.7; // Can be adjusted
            // Position weapon relative to player center, slight offset forward based on aim
            double weaponAnchorX = position.getX() + size / 2 + aimDirection.getX() * (size * 0.2); 
            double weaponAnchorY = position.getY() + size / 2 + aimDirection.getY() * (size * 0.2);

            // Calculate angle for weapon to point towards mouse (aimDirection)
            double weaponAngle = Math.toDegrees(Math.atan2(aimDirection.getY(), aimDirection.getX()));

            gc.translate(weaponAnchorX, weaponAnchorY); // Move to weapon's anchor point
            gc.rotate(weaponAngle); // Rotate weapon to face aim direction
            
            // Draw the weapon icon. Anchor it so rotation is around its hilt or center.
            // For an icon that points right by default, draw it at (-width/2, -height/2) if rotating around center
            // Or adjust if the icon's natural "pointing" direction is different.
            // Assuming weapon icon points right by default, offset slightly to simulate holding it.
            gc.drawImage(weaponIcon, -weaponSize * 0.25, -weaponSize / 2, weaponSize, weaponSize);
            
            gc.restore(); // Restore from weapon transformations
        }
        
        // Melee Attack Animation (uses aimDirection)
        if (isMeleeAttacking) {
            gc.setStroke(Color.SILVER); 
            // If you want to draw the actual weapon icon during melee, do it here, rotated by aimDirection
            // For now, keeping the line animation:
            double cX = position.getX() + size / 2;
            double cY = position.getY() + size / 2;
            gc.strokeLine(
                cX,
                cY,
                cX + aimDirection.getX() * size * 1.5,
                cY + aimDirection.getY() * size * 1.5
            );
        }
        
        // Draw Projectiles (already independent)
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
        if (item == null) return false;
        // The primary logic for equipping items is now in InventoryController.
        // This method will handle consumables or other direct-use items from player's perspective.

        boolean used = false;

        switch (item.getType()) {
            case POTION:
                // Healing potion - heals the player
                heal(item.getValue());
                used = true;
                break;

            // WEAPON and ARMOR are handled by InventoryController setting equipped items directly.
            // No specific action needed here for equipping, but we could add stat changes or effects if desired.
            case WEAPON:
                // Example: if equipping a weapon has an immediate effect beyond just setting it.
                // For now, we assume InventoryController handles the equipping part.
                // If this.equippedWeapon is not null and matches item, it means it's equipped.
                if (this.equippedWeapon != null && this.equippedWeapon.getName().equals(item.getName())) {
                     System.out.println(item.getName() + " is already equipped or handled by InventoryController.");
                }
                // We don't mark 'used = true' here for equipping, as it's not consumed.
                break;

            case ARMOR:
                if (this.equippedArmor != null && this.equippedArmor.getName().equals(item.getName())) {
                    System.out.println(item.getName() + " is already equipped or handled by InventoryController.");
                }
                // We don't mark 'used = true' here for equipping.
                break;
                
            default:
                System.out.println("Player.useItem: Unhandled item type " + item.getType());
                break;
        }
        
        // Remove consumable items after use
        if (used && item.isConsumable()) {
            // Ensure the item is actually in the inventory before trying to remove
            if (inventory.hasItem(item.getType())) { 
                inventory.removeItem(item.getType(), 1);
            }
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
