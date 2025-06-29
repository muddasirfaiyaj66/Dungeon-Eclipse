package com.dungeon.model.entity;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Enemy extends Entity {
    private static final double AGGRO_RANGE = 200;
    private static final double ATTACK_RANGE = 50;
    private static final double RANGED_ATTACK_RANGE = 150;
    private static final double MELEE_DAMAGE = 10;
    private static final double RANGED_DAMAGE = 8;
    private static final double BOSS_DAMAGE = 15;
    private static final double ATTACK_COOLDOWN = 1.5;
    private static final double BOSS_ATTACK_COOLDOWN = 2.0;
    
    private EnemyType type;
    private BehaviorType behavior;
    private double attackCooldown;
    private List<ProjectileAttack> projectiles;
    private Random random;
    private double aiUpdateTimer;
    private Point2D targetPosition;
    private AIState aiState;
    private double specialAbilityCooldown;
    private double scoreValue;
    private boolean aggravated = false;
    private int expValue;
    private boolean wasHit = false;
    
    // Enemy types with different stats
    public enum EnemyType {
        GOBLIN(80, 10, 120, 50, 100),
        SKELETON(60, 8, 100, 40, 150),
        ORC(100, 15, 80, 50, 200),
        MAGE(60, 15, 120, 30, 200),
        BOSS(500, 20, 80, 60, 500);
        
        private final int health;
        private final int damage;
        private final double speed;
        private final double size;
        private final int scoreValue;
        
        EnemyType(int health, int damage, double speed, double size, int scoreValue) {
            this.health = health;
            this.damage = damage;
            this.speed = speed;
            this.size = size;
            this.scoreValue = scoreValue;
        }
        
        public int getHealth() { return health; }
        public int getDamage() { return damage; }
        public double getSpeed() { return speed; }
        public double getSize() { return size; }
        public int getScoreValue() { return scoreValue; }
    }
    
    // AI behavior types
    public enum BehaviorType {
        AGGRESSIVE,  // Moves directly toward player when in range
        RANGED,      // Keeps distance and attacks from afar
        PATROLLER,   // Moves in a pattern until player is close
        AMBUSHER     // Stays still until player is very close, then charges
    }
    
    private enum AIState {
        IDLE,
        CHASE,
        ATTACK,
        RETREAT,
        SPECIAL
    }
    
    public Enemy(double x, double y, EnemyType type) {
        this(x, y, type, BehaviorType.AGGRESSIVE, 1);
    }
    
    public Enemy(double x, double y, EnemyType type, BehaviorType behavior, int level) {
        // Call super with values that will be adjusted for type and level
        super(x, y, 
             type.getHealth() + ((level - 1) * 10), 
             type.getSpeed(), 
             type.getSize());
             
        this.type = type;
        this.behavior = behavior;
        this.attackCooldown = 0;
        this.projectiles = new ArrayList<>();
        this.random = new Random();
        this.aiUpdateTimer = 0;
        this.targetPosition = new Point2D(x, y);
        this.aiState = AIState.IDLE;
        this.specialAbilityCooldown = 0;
        
        // Set stats based on enemy type and scale with level
        setDamage(type.getDamage() + ((level - 1) * 2));
        this.scoreValue = type.getScoreValue();
        this.expValue = (int)(scoreValue * 0.2);
        
        // Set up behavior-specific properties
        setupBehavior();
        
        // Set initial patrol point for patrolling enemies
        if (behavior == BehaviorType.PATROLLER) {
            this.targetPosition = new Point2D(x + (random.nextDouble() * 200 - 100),
                                         y + (random.nextDouble() * 200 - 100));
        }
    }
    
    /**
     * Sets up behavior-specific properties like attack and detection ranges
     */
    private void setupBehavior() {
        switch (behavior) {
            case AGGRESSIVE:
                // Already default values
                break;
                
            case RANGED:
                // Increase range, decrease movement speed slightly
                setVelocity(getVelocity().multiply(0.8));
                break;
                
            case PATROLLER:
                // Faster movement when patrolling
                setVelocity(getVelocity().multiply(1.2));
                break;
                
            case AMBUSHER:
                // Faster attack, slower detection
                attackCooldown *= 0.7;
                break;
        }
    }
    
    public void update(double deltaTime, Player player) {
        if (!isAlive()) return;
        
        // Update AI state
        updateAI(deltaTime, player);
        
        // Update position based on velocity
        super.update(deltaTime);
        
        // Update attack cooldown
        if (attackCooldown > 0) {
            attackCooldown -= deltaTime;
        }
        
        // Update special ability cooldown
        if (specialAbilityCooldown > 0) {
            specialAbilityCooldown -= deltaTime;
        }
        
        // Update projectiles
        updateProjectiles(deltaTime);
    }
    
    public void update(double deltaTime) {
        if (!isAlive()) return;
        // Reset wasHit at the start of each update
        wasHit = false;
        // Update position based on current velocity
        super.update(deltaTime);
        // Update attack cooldown
        if (attackCooldown > 0) {
            attackCooldown -= deltaTime;
        }
    }
    
    private void updateAI(double deltaTime, Player player) {
        if (player == null) return;
        
        // Update AI timer
        aiUpdateTimer -= deltaTime;
        if (aiUpdateTimer <= 0) {
            // Reset timer (add some randomness to make enemies less predictable)
            aiUpdateTimer = 0.5 + random.nextDouble() * 0.5;
            
            // Calculate distance to player
            double distanceToPlayer = position.distance(player.getPosition());
            
            // Determine AI state based on distance, type and behavior
            if (distanceToPlayer <= AGGRO_RANGE) {
                // Player is in aggro range
                aggravated = true;
                
                // Different behavior based on behavior type
                switch (behavior) {
                    case AGGRESSIVE:
                        if (distanceToPlayer <= ATTACK_RANGE) {
                            aiState = AIState.ATTACK;
                        } else {
                            aiState = AIState.CHASE;
                        }
                        break;
                        
                    case RANGED:
                        if (distanceToPlayer <= RANGED_ATTACK_RANGE) {
                            aiState = AIState.ATTACK;
                        } else if (distanceToPlayer < RANGED_ATTACK_RANGE * 0.7) {
                            // Too close - back up
                            aiState = AIState.RETREAT;
                        } else {
                            // Too far - get closer
                            aiState = AIState.CHASE;
                        }
                        break;
                        
                    case PATROLLER:
                        if (distanceToPlayer <= ATTACK_RANGE) {
                            aiState = AIState.ATTACK;
                        } else {
                            aiState = AIState.CHASE;
                        }
                        break;
                        
                    case AMBUSHER:
                        // Ambushers wait until player is very close
                        if (distanceToPlayer <= ATTACK_RANGE) {
                            aiState = AIState.ATTACK;
                        } else if (distanceToPlayer <= ATTACK_RANGE * 2) {
                            // Player is close - charge!
                            aiState = AIState.CHASE;
                        } else {
                            // Player is not close enough - stay still
                            aiState = AIState.IDLE;
                        }
                        break;
                }
                
                // Special ability check for boss type
                if (type == EnemyType.BOSS && distanceToPlayer <= ATTACK_RANGE * 1.5) {
                    // Boss has a chance to use special ability
                    if (specialAbilityCooldown <= 0 && random.nextDouble() < 0.3) {
                        aiState = AIState.SPECIAL;
                        specialAbilityCooldown = 8.0; // Long cooldown for special ability
                    }
                }
            } else {
                // Player not in aggro range
                aggravated = false;
                
                // Handle non-aggravated behavior
                if (behavior == BehaviorType.PATROLLER) {
                    handlePatrolBehavior();
                } else {
                    // Other types just idle
                    aiState = AIState.IDLE;
                    if (random.nextDouble() < 0.3) {
                        // Set a new random target position
                        double offsetX = (random.nextDouble() - 0.5) * 100;
                        double offsetY = (random.nextDouble() - 0.5) * 100;
                        targetPosition = new Point2D(position.getX() + offsetX, position.getY() + offsetY);
                    }
                }
            }
        }
        
        // Execute behavior based on current AI state
        switch (aiState) {
            case IDLE:
                moveTowardsTarget(targetPosition, 0.5);
                break;
                
            case CHASE:
                moveTowardsTarget(player.getPosition(), 1.0);
                break;
                
            case RETREAT:
                // Move away from player
                Point2D awayVector = position.subtract(player.getPosition()).normalize();
                targetPosition = position.add(awayVector.multiply(100));
                moveTowardsTarget(targetPosition, 1.0);
                break;
                
            case ATTACK:
                if (attackCooldown <= 0) {
                    attack(player);
                }
                break;
                
            case SPECIAL:
                useSpecialAbility(player);
                break;
        }
    }
    
    private void handlePatrolBehavior() {
        // Calculate direction to patrol point
        Point2D direction = targetPosition.subtract(position);
        double distance = direction.magnitude();
        
        // If close to patrol point, select a new one
        if (distance < 10) {
            targetPosition = new Point2D(position.getX() + (random.nextDouble() * 200 - 100),
                                    position.getY() + (random.nextDouble() * 200 - 100));
            return;
        }
        
        // Set state to move toward patrol point
        aiState = AIState.IDLE;
    }
    
    private void moveTowardsTarget(Point2D target, double speedMultiplier) {
        // Calculate direction to target
        Point2D direction = target.subtract(position);
        
        // Only move if not already at target
        if (direction.magnitude() > 5) {
            direction = direction.normalize();
            setVelocity(direction.multiply(speed * speedMultiplier));
        } else {
            setVelocity(new Point2D(0, 0));
        }
    }
    
    private void attack(Player player) {
        switch (behavior) {
            case RANGED:
                // Ranged attack
                attackCooldown = ATTACK_COOLDOWN;
                
                // Calculate direction to player
                Point2D direction = player.getPosition().subtract(position).normalize();
                
                // Create projectile
                ProjectileAttack projectile = new ProjectileAttack(
                    position.getX() + size/2,
                    position.getY() + size/2,
                    direction,
                    damage,
                    Color.RED,
                    ProjectileAttack.ProjectileType.DEFAULT
                );
                projectiles.add(projectile);
                break;
                
            default:
                // Melee attack
                attackCooldown = ATTACK_COOLDOWN;
                
                // Check if player is in melee range
                if (position.distance(player.getPosition()) <= ATTACK_RANGE) {
                    player.takeDamage(damage);
                }
                break;
        }
    }
    
    private void updateProjectiles(double deltaTime) {
        // Update and remove expired projectiles
        projectiles.removeIf(projectile -> {
            projectile.update(deltaTime);
            return projectile.isExpired();
        });
    }
    
    private void useSpecialAbility(Player player) {
        if (type != EnemyType.BOSS && type != EnemyType.MAGE) return;
        
        // Set cooldown for special ability
        specialAbilityCooldown = 8.0;
        attackCooldown = ATTACK_COOLDOWN * 2;
        
        if (type == EnemyType.BOSS) {
            // Boss special: multi-directional attack
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI * 2 * i / 8;
                Point2D dir = new Point2D(Math.cos(angle), Math.sin(angle));
                
                ProjectileAttack projectile = new ProjectileAttack(
                    position.getX() + size/2,
                    position.getY() + size/2,
                    dir,
                    damage * 1.5,
                    Color.DARKRED,
                    ProjectileAttack.ProjectileType.MAGIC
                );
                projectiles.add(projectile);
            }
        } else if (type == EnemyType.MAGE) {
            // Mage special: teleport and attack
            // First, find a position away from the player
            double angle = Math.random() * Math.PI * 2;
            double distance = RANGED_ATTACK_RANGE * 0.8;
            Point2D newPos = player.getPosition().add(
                new Point2D(Math.cos(angle) * distance, Math.sin(angle) * distance)
            );
            
            // Teleport
            setPosition(newPos);
            
            // Then attack
            Point2D direction = player.getPosition().subtract(position).normalize();
            ProjectileAttack projectile = new ProjectileAttack(
                position.getX() + size/2,
                position.getY() + size/2,
                direction,
                damage * 1.2,
                Color.PURPLE,
                ProjectileAttack.ProjectileType.MAGIC
            );
            projectiles.add(projectile);
        }
    }
    
    @Override
    public void render(GraphicsContext gc) {
        if (!isAlive()) return;
        
        // Set color based on enemy type
        Color enemyColor;
        switch (type) {
            case BOSS:
                enemyColor = Color.DARKRED;
                break;
            case MAGE:
                enemyColor = Color.PURPLE;
                break;
            case GOBLIN:
                enemyColor = Color.GREEN;
                break;
            case SKELETON:
                enemyColor = Color.LIGHTGRAY;
                break;
            case ORC:
                enemyColor = Color.BROWN;
                break;
            default:
                enemyColor = Color.RED;
                break;
        }
        
        // Draw enemy body
        gc.setFill(enemyColor);
        gc.fillOval(position.getX(), position.getY(), size, size);
        
        // Draw health bar
        double healthPercentage = getHealth() / (double) getMaxHealth();
        double healthBarWidth = size;
        double healthBarHeight = 5;
        double healthBarX = position.getX();
        double healthBarY = position.getY() - 10;
        
        // Background
        gc.setFill(Color.RED);
        gc.fillRect(healthBarX, healthBarY, healthBarWidth, healthBarHeight);
        
        // Foreground (current health)
        gc.setFill(Color.GREEN);
        gc.fillRect(healthBarX, healthBarY, healthBarWidth * healthPercentage, healthBarHeight);
        
        // Draw projectiles
        for (ProjectileAttack projectile : projectiles) {
            projectile.render(gc);
        }
    }
    
    // Getters and setters
    public EnemyType getType() {
        return type;
    }
    
    public BehaviorType getBehavior() {
        return behavior;
    }
    
    public List<ProjectileAttack> getProjectiles() {
        return new ArrayList<>(projectiles);
    }
    
    public double getScoreValue() {
        return scoreValue;
    }
    
    public int getExpValue() {
        return expValue;
    }
    
    public boolean isAggravated() {
        return aggravated;
    }
    
    public void setAggravated(boolean aggravated) {
        this.aggravated = aggravated;
    }
    
    public boolean wasHit() {
        return wasHit;
    }
    
    public void setWasHit(boolean hit) {
        this.wasHit = hit;
    }
    
    @Override
    public void takeDamage(double damage) {
        super.takeDamage(damage);
        wasHit = true;
    }
}
