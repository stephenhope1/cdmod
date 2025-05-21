package com.example.modid.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.world.World;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
// ActiveTargetGoal might be added later when proper AI is implemented
// import net.minecraft.entity.ai.goal.ActiveTargetGoal; 

public class DeepDwellerEntity extends HostileEntity {

    public static final String ID = "deep_dweller";

    public DeepDwellerEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        // Ensures the entity doesn't despawn easily during testing.
        // This might be removed or made conditional later.
        this.setNoMinionStatus(true); 
        this.setPersistenceRequired(true); 
    }

    public static DefaultAttributeContainer.Builder createDeepDwellerAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0D) 
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.23D) // Adjusted: comparable to player walking, slightly slower than zombie
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0D) 
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0D) 
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.5D);
    }

import net.minecraft.util.math.BlockPos; // Added import
import net.minecraft.server.network.ServerPlayerEntity; // Added import
import net.minecraft.world.WorldAccess; // For initialize
import net.minecraft.entity.SpawnReason; // For initialize
import net.minecraft.entity.EntityData; // For initialize
import net.minecraft.nbt.NbtCompound; // For initialize
import org.jetbrains.annotations.Nullable; // For initialize

import net.minecraft.util.math.Vec3d; // Added import

    // TODO: Add a method setSpeed(double speed) to be called by CreatureManager based on config
    
    // --- Initial Target Location (from previous subtask) ---
    @Nullable
    private BlockPos initialTargetLocation;
    private static final int TARGET_LOCATION_RADIUS_MIN = 20;
    // --- End Initial Target Location ---

import net.minecraft.world.RaycastContext; // Added import
import net.minecraft.util.hit.BlockHitResult; // Added import
import net.minecraft.util.math.MathHelper; // Added import for clamp

    // --- Sound Perception System ---
    @Nullable
    private Vec3d currentSoundSourceLocation; // Location of the sound being investigated
    private float currentSoundPerceivedIntensity; // Perceived intensity of that sound
    private boolean isActivelyListening;
    private int activeListeningTicksLeft;
    private static final int ACTIVE_LISTENING_DURATION_BASE = 10 * 20; // 10 seconds
    private static final float ACTIVE_LISTENING_CHANCE = 0.3f;
    // Modifier for effective distance calculation during active listening
    private static final float ACTIVE_LISTENING_EFFECTIVE_DISTANCE_MULTIPLIER = 0.5f; 
    private static final float HIGH_PI_THRESHOLD_FOR_CHASE = 10.0f; // PI > 10 triggers chase
    private boolean hasHighPiSound; // Flag for future chase goal
    
    // Constants for PI calculation (Hearing)
    private static final float AIR_BLOCK_COST = 0.5f; 
    // --- End Sound Perception System ---

    // --- Sight System ---
    private boolean hasLineOfSightToPlayer;
    private boolean playerInShortSightRange;
    private boolean playerInMediumSightRangeWithSound;
    private boolean playerInMediumSightRangeMoving;

    private static final double SHORT_SIGHT_RADIUS = 5.0;
    private static final double MEDIUM_SIGHT_RADIUS = 15.0;
    private static final double SPRINTING_SIGHT_RADIUS_MULTIPLIER = 1.5; // Medium sight becomes 15*1.5 = 22.5 for sprinting
    private static final float SIGHT_PI_THRESHOLD = 5.0f; // PI from hearing needed to 'sharpen' medium range sight
    private static final double PLAYER_MOVEMENT_THRESHOLD_SQR = 0.0009; 
    // --- End Sight System ---

    // --- Smell System ---
    @Nullable
    private ScentNode currentSmellTargetNode;
    private boolean isInvestigatingSmell;
    private UUID targetPlayerForSmellUuid; // To track which player's scent is being followed

    private static final float SMELL_DETECTION_RADIUS = 40.0f; // How close the entity needs to be to a player to start smelling their trail.
    private static final float SMELL_PI_THRESHOLD = 3.0f; // Perceived intensity needed to investigate a scent node.
    private static final int SCENT_NODE_SAMPLE_RATE = 6; // Process 1 out of every 6 nodes.
import net.minecraft.sound.SoundCategory; // Added for playing sound

    // --- End Smell System ---
    
    // --- Phase 2 Ambience Fields ---
    private int presenceSoundTicks;
    private static final int PRESENCE_SOUND_INTERVAL_MIN = 4 * 20; // 4 seconds
    private static final int PRESENCE_SOUND_INTERVAL_MAX = 8 * 20; // 8 seconds
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator; // For safe removal from map while iterating

    // --- End Phase 2 Ambience Fields ---
    
    // --- Light Extinguishing Effect ---
    private Map<BlockPos, BlockState> extinguishedLights; 
    private static final int LIGHT_EXTINGUISH_RADIUS = 40;
    private static final int LIGHT_EXTINGUISH_RADIUS_VERTICAL = 10; // Define vertical radius
import net.minecraft.fluid.Fluids; // For checking water source
import net.minecraft.block.Block; // For block replacement

    // --- End Light Extinguishing Effect ---

    // --- Freezing Effect ---
    private static final int FREEZE_RADIUS = 10;
    private static final int FREEZE_VERTICAL_RADIUS = 4; // Smaller vertical effect
    // --- End Freezing Effect ---

    // --- Stomp Sound Effect ---
    private int stompSoundTicks;
    private static final int STOMP_SOUND_INTERVAL_MIN = 25; // Approx 1.25 seconds (based on 0.23 speed)
    private static final int STOMP_SOUND_INTERVAL_MAX = 45; // Approx 2.25 seconds
import net.minecraft.entity.mob.MobEntity; // For Fleeing Mobs
import net.minecraft.entity.effect.StatusEffectInstance; // For Fleeing Mobs
import net.minecraft.entity.effect.StatusEffects; // For Fleeing Mobs
import java.util.List; // For Fleeing Mobs (already imported but for context)

    // --- End Stomp Sound Effect ---

    // --- Fleeing Mobs Effect ---
    private static final int FLEE_RADIUS = 30; // Radius to affect other mobs
    private static final int FLEE_EFFECT_DURATION = 5 * 20; // 5 seconds of slowness/weakness
    private static final double FLEE_SPEED_MULTIPLIER = 1.2; // How fast mobs try to flee
    // --- End Fleeing Mobs Effect ---

    // --- For Temporary Freezing Reversion ---
    private Map<BlockPos, BlockState> frozenBlocks; // Stores original states of frozen blocks
    // --- End Temporary Freezing Reversion ---

    private static final float SOLID_BLOCK_COST = 1.0f; // For hearing (Sound Perception)
    private static final int MAX_RAYCAST_STEPS = 64; // For hearing (Sound Perception)
    // --- End Sound Perception System --- (This comment was misplaced, it's part of Sound Perception)

    // --- Sight System Methods ---
    /**
     * Checks if the entity has a clear line of sight to the target entity.
     * Considers the creature's eye position to the target's bounding box.
     */
    public boolean canSee(Entity targetEntity) {
        if (targetEntity == null || this.getWorld() == null) {
            return false;
        }

        Vec3d selfEyes = this.getEyePos();
        Vec3d targetEyes = targetEntity.getEyePos(); // Check eye to eye first

        // Check line of sight to target's eyes
        if (hasClearPath(selfEyes, targetEyes)) {
            return true;
        }

        // If eye-to-eye is blocked, check line of sight to target's feet (center of bounding box bottom)
        // This helps if the target is peeking over an edge.
        Vec3d targetFeet = new Vec3d(targetEntity.getX(), targetEntity.getY(), targetEntity.getZ());
        return hasClearPath(selfEyes, targetFeet);
    }

    private boolean hasClearPath(Vec3d start, Vec3d end) {
        BlockHitResult hitResult = this.getWorld().raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER, // Consider blocks that collide
                RaycastContext.FluidHandling.NONE, // Ignore fluids
                this // Entity to ignore (self)
        ));
        return hitResult.getType() == BlockHitResult.Type.MISS;
    }
    // --- End Sight System Methods ---
    
    private static final int TARGET_LOCATION_RADIUS_MAX = 30;
    private static final int TARGET_LOCATION_FIND_ATTEMPTS = 10;


    // This method will be called by CreatureManager after spawning the entity.
    public void setInitialTargetPoint(ServerPlayerEntity player) {
        if (player == null || this.getWorld().isClient) {
            return;
        }
        World world = this.getWorld();
        for (int i = 0; i < TARGET_LOCATION_FIND_ATTEMPTS; i++) {
            double angle = world.getRandom().nextDouble() * 2 * Math.PI;
            double yAngle = world.getRandom().nextDouble() * Math.PI - (Math.PI / 2); // -PI/2 to PI/2 for spherical distribution
            double distance = TARGET_LOCATION_RADIUS_MIN + world.getRandom().nextDouble() * (TARGET_LOCATION_RADIUS_MAX - TARGET_LOCATION_RADIUS_MIN);
            
            double xOffset = Math.cos(angle) * Math.cos(yAngle) * distance;
            double zOffset = Math.sin(angle) * Math.cos(yAngle) * distance;
            double yOffset = Math.sin(yAngle) * distance;

            BlockPos candidatePos = player.getBlockPos().add((int)xOffset, (int)yOffset, (int)zOffset);

            // Check if the block is valid (air, and block below is solid for pathing)
            if (world.isAir(candidatePos) && world.isAir(candidatePos.up()) && !world.getBlockState(candidatePos.down()).isAir()) {
                this.initialTargetLocation = candidatePos;
                com.example.modid.ExampleMod.LOGGER.info("Deep Dweller {} set initial target location to {} relative to player {}", 
                    this.getUuidAsString(), this.initialTargetLocation, player.getName().getString());
                // Initialize pathfinding goal towards this location now that it's set
                // This will be done when goals are created/reset.
                return;
            }
        }
        com.example.modid.ExampleMod.LOGGER.warn("Deep Dweller {} failed to find a valid initial target location after {} attempts.", this.getUuidAsString(), TARGET_LOCATION_FIND_ATTEMPTS);
        // If no valid point is found, it will rely on its standard AI (which is currently just looking around)
        // or a future "seek player" goal.
    }

    @Nullable
    public BlockPos getInitialTargetLocation() {
        return this.initialTargetLocation;
    }
    
    // Called after entity is spawned and added to world
    @Override
    public void initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
        // Note: Player information is not directly available here.
        // setInitialTargetPoint needs to be called by CreatureManager post-spawn.
        com.example.modid.ExampleMod.LOGGER.info("DeepDwellerEntity {} initialized. Spawn reason: {}", this.getUuidAsString(), spawnReason);
    }


import com.example.modid.entity.ai.goal.GoToTargetLocationGoal; // Added import

    @Override
import com.example.modid.entity.ai.goal.InvestigateSoundGoal; 
import com.example.modid.entity.ai.goal.InvestigateSmellGoal; // Added import

    protected void initGoals() {
        super.initGoals(); // Important to call super

        // --- Custom Goals ---
        // Priority 0: Investigate interesting sounds (this also handles direct sight via high PI).
        this.goalSelector.add(0, new InvestigateSoundGoal(this, 1.0D)); 

        // Priority 1: Go to the initial target location set at spawn.
        this.goalSelector.add(1, new GoToTargetLocationGoal(this, 1.0D)); 
        
        // Priority 2: Investigate smells.
        this.goalSelector.add(2, new InvestigateSmellGoal(this, 0.8D)); // Slightly slower speed for smelling

        // --- Standard "Idle" Behaviors (lower priority) ---
        // These will run if no higher-priority goals are active.
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 16.0F)); 
        this.goalSelector.add(9, new LookAroundGoal(this)); 

        // Target selectors remain empty for now, as per previous design.
    }

    public void clearInitialTargetLocation() {
        // com.example.modid.ExampleMod.LOGGER.info("Deep Dweller {} cleared its initial target location.", this.getUuidAsString());
        this.initialTargetLocation = null;
    }

    /**
     * Processes a sound made by a player.
     * This will be called by mixins when players generate notable sounds.
     *
     * @param player The player who made the sound.
     * @param soundLocation The Vec3d location of the sound.
     * @param soundIntensity The raw intensity of the sound at its source.
     */
    public void processSound(ServerPlayerEntity player, Vec3d soundLocation, float soundIntensity) {
        if (this.getWorld().isClient() || player == null || soundLocation == null) {
            return;
        }
        com.example.modid.ExampleMod.LOGGER.debug("DeepDweller {} processing sound from {} at {} (raw intensity: {})", 
            this.getUuidAsString(), player.getName().getString(), soundLocation, soundIntensity);

        Vec3d selfPos = this.getEyePos(); // Use eye position for hearing
        double distance = selfPos.distanceTo(soundLocation);

        // Simplified initial effective distance
        float effectiveDistance = (float) distance;

        // Custom Raycast for block type estimation
        int nonAirBlocks = 0;
        if (distance > 0 && distance < MAX_RAYCAST_STEPS) { // Only do detailed check within reasonable range
            Vec3d direction = soundLocation.subtract(selfPos).normalize();
            int steps = (int) Math.ceil(distance); // Number of 1-block steps
            float airBlockCount = 0;
            float solidBlockCount = 0;

            for (int i = 0; i < steps; i++) {
                BlockPos currentBlockPos = new BlockPos(
                    (int)(selfPos.x + direction.x * i),
                    (int)(selfPos.y + direction.y * i),
                    (int)(selfPos.z + direction.z * i)
                );
                if (!this.getWorld().getBlockState(currentBlockPos).isAir()) {
                    solidBlockCount++;
                } else {
                    airBlockCount++;
                }
            }
            // Calculate effective distance based on block types
            effectiveDistance = (airBlockCount * AIR_BLOCK_COST) + (solidBlockCount * SOLID_BLOCK_COST);
        } else if (distance >= MAX_RAYCAST_STEPS) {
            // For long distances, assume mostly air with some obstruction penalty
            effectiveDistance = (float) (distance * ((AIR_BLOCK_COST + SOLID_BLOCK_COST) / 2.0f)) ; // Average cost
        }


        // Apply active listening modifier if applicable
        if (isActivelyListening) {
            effectiveDistance *= ACTIVE_LISTENING_EFFECTIVE_DISTANCE_MULTIPLIER;
        }

        float perceivedIntensity = soundIntensity - effectiveDistance;
        perceivedIntensity = Math.max(0, perceivedIntensity); // Ensure PI is not negative

        com.example.modid.ExampleMod.LOGGER.info("DeepDweller {}: Sound from {}, rawIntensity {}, dist {}, effectiveDist {}, PI: {}",
                this.getUuidAsString(), player.getName().getString(), soundIntensity, distance, effectiveDistance, perceivedIntensity);
        
        // --- Effects when PI > 0 (Step 3 & 4) ---
        if (perceivedIntensity > 0) {
            // Increase Alert Level (player who made sound, not necessarily closest player)
            com.example.modid.manager.AlertLevelManager alertManager = com.example.modid.ExampleMod.getAlertLevelManager();
            if (alertManager != null) {
                // The multiplier for PI to alert increase can be configurable
                float alertIncreaseAmount = perceivedIntensity * 0.5f; 
                alertManager.increaseAlertLevel(player, alertIncreaseAmount);
                com.example.modid.ExampleMod.LOGGER.debug("DeepDweller: Increased alert for {} by {} due to sound PI {}", 
                    player.getName().getString(), alertIncreaseAmount, perceivedIntensity);
            }

            // Active Listening Trigger
            if (!isActivelyListening && this.random.nextFloat() < ACTIVE_LISTENING_CHANCE) {
                isActivelyListening = true;
                activeListeningTicksLeft = ACTIVE_LISTENING_DURATION_BASE + this.random.nextInt(6*20); // Base + 0-6 seconds variation
                com.example.modid.ExampleMod.LOGGER.info("DeepDweller {} started actively listening for {} ticks.", this.getUuidAsString(), activeListeningTicksLeft);
                // TODO: Play sound cue / trigger animation placeholder
                // Example: this.getWorld().playSound(null, this.getBlockPos(), SoundEvents.ENTITY_WARDEN_LISTENING, SoundCategory.HOSTILE, 1.0f, 1.0f);
            }

            // Initiate Investigation (if PI is higher than current, and not already in high PI chase mode)
            // If already in high PI chase, only a new *higher* PI sound can override the target.
            boolean canUpdateTarget = false;
            if (this.hasHighPiSound) { // Currently in chase mode
                if (perceivedIntensity > this.currentSoundPerceivedIntensity && perceivedIntensity > HIGH_PI_THRESHOLD_FOR_CHASE) {
                    // Only a new, stronger high PI sound can change target
                    canUpdateTarget = true;
                }
            } else { // Not in chase mode
                if (perceivedIntensity > this.currentSoundPerceivedIntensity) {
                    canUpdateTarget = true;
                }
            }

            if (canUpdateTarget) {
                this.currentSoundSourceLocation = soundLocation;
                this.currentSoundPerceivedIntensity = perceivedIntensity;
                com.example.modid.ExampleMod.LOGGER.info("DeepDweller {} updated sound investigation target to {} with PI {}", 
                    this.getUuidAsString(), soundLocation, perceivedIntensity);
                // This might implicitly stop the GoToInitialTargetGoal if it's running,
                // as the InvestigateSoundGoal (to be added) should have higher priority.
            }

            // Trigger Chase (if PI > HIGH_PI_THRESHOLD_FOR_CHASE)
            if (perceivedIntensity > HIGH_PI_THRESHOLD_FOR_CHASE) {
                if (!this.hasHighPiSound) { // Log only when first entering high PI mode
                     com.example.modid.ExampleMod.LOGGER.info("DeepDweller {} perceived HIGH PI sound ({} > {}). Chase mode ACTIVATED for sound from {} at {}.",
                        this.getUuidAsString(), perceivedIntensity, HIGH_PI_THRESHOLD_FOR_CHASE, player.getName().getString(), soundLocation);
                }
                this.hasHighPiSound = true;
                // Ensure the high PI sound becomes the primary target, even if canUpdateTarget was false due to a slightly lower PI than an existing high PI sound.
                // This logic ensures the *strongest* high PI sound is focused on.
                if (perceivedIntensity >= this.currentSoundPerceivedIntensity) { // Use >= to allow updating to same PI if location changed
                    this.currentSoundSourceLocation = soundLocation; 
                    this.currentSoundPerceivedIntensity = perceivedIntensity; 
                }
            }
        }
    }
    
    @Override
import net.minecraft.entity.player.PlayerEntity; // Added for finding nearest player
import java.util.List; // Added for finding nearest player

import com.example.modid.manager.ScentManager; // Added
import com.example.modid.manager.ScentNode; // Added
import java.util.Collections; // For emptyList

    public DeepDwellerEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.setNoMinionStatus(true); 
        this.setPersistenceRequired(true); 
        this.presenceSoundTicks = this.random.nextInt(PRESENCE_SOUND_INTERVAL_MAX / 2); 
        this.extinguishedLights = new HashMap<>(); 
        this.stompSoundTicks = STOMP_SOUND_INTERVAL_MIN + this.random.nextInt(STOMP_SOUND_INTERVAL_MAX - STOMP_SOUND_INTERVAL_MIN + 1);
        this.frozenBlocks = new HashMap<>(); // Initialize map for frozen blocks
    }

    public void tick() {
        super.tick();
        if (this.getWorld().isClient()) {
            return;
        }

        long currentTime = this.getWorld().getTime();

        // --- Active Listening Timer (from Hearing System) ---
        if (activeListeningTicksLeft > 0) {
            activeListeningTicksLeft--;
            if (activeListeningTicksLeft == 0) {
                isActivelyListening = false;
                com.example.modid.ExampleMod.LOGGER.info("DeepDweller {} stopped actively listening.", this.getUuidAsString());
            }
        }
        // --- End Active Listening Timer ---

        // --- Sight System Logic (Tick Update) ---
        // Find the nearest player
        ServerPlayerEntity nearestPlayer = null;
        double closestPlayerDistSq = Double.MAX_VALUE;
        List<ServerPlayerEntity> players = this.getWorld().getNonSpectatingEntities(ServerPlayerEntity.class, this.getBoundingBox().expand(MEDIUM_SIGHT_RADIUS * SPRINTING_SIGHT_RADIUS_MULTIPLIER + 5.0)); // Expand by a bit more than max sight range

        for (ServerPlayerEntity player : players) {
            if (player.isCreative() || player.isSpectator()) continue; // Ignore creative/spectator players
            double distSq = this.squaredDistanceTo(player);
            if (distSq < closestPlayerDistSq) {
                closestPlayerDistSq = distSq;
                nearestPlayer = player;
            }
        }
        
        // Reset sight flags
        this.playerInShortSightRange = false;
        this.playerInMediumSightRangeWithSound = false;
        this.playerInMediumSightRangeMoving = false;
        boolean previousHasLineOfSight = this.hasLineOfSightToPlayer; // Store previous state for logging changes
        this.hasLineOfSightToPlayer = false;


        if (nearestPlayer != null) {
            boolean canSeePlayer = canSee(nearestPlayer);
            double distanceToPlayer = Math.sqrt(closestPlayerDistSq);

            // 4. Short-Range True Vision
            if (distanceToPlayer <= SHORT_SIGHT_RADIUS && canSeePlayer) {
                this.playerInShortSightRange = true;
            }

            // 5. Medium-Range True Vision with Sound Cue
            if (distanceToPlayer <= MEDIUM_SIGHT_RADIUS && canSeePlayer && this.currentSoundPerceivedIntensity >= SIGHT_PI_THRESHOLD) {
                // Check if the sound source is reasonably close to the player being seen or if the sound is general environmental cue
                if (this.currentSoundSourceLocation != null && this.currentSoundSourceLocation.isInRange(nearestPlayer.getPos(), MEDIUM_SIGHT_RADIUS * 0.75)) {
                    this.playerInMediumSightRangeWithSound = true;
                }
            }
            
            // 6. Medium-Range True Vision with Player Movement
            double currentMediumRadius = MEDIUM_SIGHT_RADIUS;
            if (nearestPlayer.isSprinting()) {
                currentMediumRadius *= SPRINTING_SIGHT_RADIUS_MULTIPLIER;
            }
            // Check player's velocity magnitude squared. Persist last known position to compare.
            // For simplicity, using getVelocity().lengthSquared() is fine for now.
            boolean isPlayerMoving = nearestPlayer.getVelocity().lengthSquared() > PLAYER_MOVEMENT_THRESHOLD_SQR;

            if (distanceToPlayer <= currentMediumRadius && canSeePlayer && isPlayerMoving) {
                this.playerInMediumSightRangeMoving = true;
            }

            // 7. Update hasLineOfSightToPlayer
            this.hasLineOfSightToPlayer = this.playerInShortSightRange || this.playerInMediumSightRangeWithSound || this.playerInMediumSightRangeMoving;

            if (this.hasLineOfSightToPlayer && !previousHasLineOfSight) {
                 com.example.modid.ExampleMod.LOGGER.info("DeepDweller {} GAINED line of sight to player {}. Short: {}, MedSound: {}, MedMoving: {}", 
                    this.getUuidAsString(), nearestPlayer.getName().getString(), this.playerInShortSightRange, this.playerInMediumSightRangeWithSound, this.playerInMediumSightRangeMoving);
            } else if (!this.hasLineOfSightToPlayer && previousHasLineOfSight) {
                 com.example.modid.ExampleMod.LOGGER.info("DeepDweller {} LOST line of sight to player {}.", this.getUuidAsString(), nearestPlayer.getName().getString());
            }


            // 8. AI Goal for Reacting to Sight (Placeholder)
            if (this.hasLineOfSightToPlayer) {
                // If player is seen, treat this as a high-confidence "sound" source for hearing system.
                // This can make the InvestigateSoundGoal or future ChaseGoal pick up the player.
                // It also sets hasHighPiSound if the "sight" is considered strong enough.
                float sightBasedPi = HIGH_PI_THRESHOLD_FOR_CHASE + 5.0f; // Stronger than normal hearing threshold
                
                // Update current sound source to player's location, effectively overriding weaker sounds
                this.currentSoundSourceLocation = nearestPlayer.getPos();
                this.currentSoundPerceivedIntensity = sightBasedPi;
                this.hasHighPiSound = true; // Direct sight implies high alert/chase state
                
                // com.example.modid.ExampleMod.LOGGER.debug("DeepDweller {} sees player {}, updating sound source to player location with PI {}", 
                //    this.getUuidAsString(), nearestPlayer.getName().getString(), sightBasedPi);

                // The InvestigateSoundGoal will be re-evaluated. If a ChaseGoal exists, it should take precedence.
                // The GoToTargetLocationGoal should be interrupted by InvestigateSoundGoal or ChaseGoal due to priority.
            }
        } else { // No player nearby
            if (previousHasLineOfSight) { // Was seeing a player last tick, but now no one is nearby
                com.example.modid.ExampleMod.LOGGER.info("DeepDweller {} lost line of sight (player out of range).", this.getUuidAsString());
            }
        }
        // --- End Sight System Logic ---

        // --- Smell System Logic (Tick Update) ---
        if (!this.hasHighPiSound && !this.hasLineOfSightToPlayer && !this.isActivelyListening) { // Only try to smell if not already highly engaged
            ServerPlayerEntity playerToSmell = null;
            
            if (targetPlayerForSmellUuid != null) { // If already following a specific player's scent
                playerToSmell = this.getWorld().getServer().getPlayerManager().getPlayer(targetPlayerForSmellUuid);
                if (playerToSmell == null || playerToSmell.isRemoved() || this.squaredDistanceTo(playerToSmell) > SMELL_DETECTION_RADIUS * 2) { // Lost specific player
                    targetPlayerForSmellUuid = null; 
                    playerToSmell = null;
                    clearCurrentSmellTarget();
                }
            }
            
            if (playerToSmell == null && nearestPlayer != null && this.squaredDistanceTo(nearestPlayer) < SMELL_DETECTION_RADIUS * SMELL_DETECTION_RADIUS) {
                playerToSmell = nearestPlayer; // Default to nearest player if within general smell detection radius
                targetPlayerForSmellUuid = nearestPlayer.getUuid(); // Start tracking this player
            }

            if (playerToSmell != null) {
                ScentManager scentManager = ExampleMod.getScentManager();
                if (scentManager != null) {
                    List<ScentNode> trail = scentManager.getScentTrail(playerToSmell.getUuid());
                    ScentNode bestNode = null;
                    float highestIntensity = 0;

                    for (int i = 0; i < trail.size(); i++) {
                        if (i % SCENT_NODE_SAMPLE_RATE == 0) { // Process 1/6th of nodes
                            ScentNode node = trail.get(i);
                            long ageSeconds = (currentTime - node.creationTime()) / 20;
                            float currentIntensity = node.initialIntensity() - (ageSeconds * ScentManager.SCENT_DECAY_PER_SECOND);
                            
                            if (currentIntensity > SMELL_PI_THRESHOLD && currentIntensity > highestIntensity) {
                                // Basic LOS check to the scent node to simulate "air current" carrying scent.
                                // Not perfect, but adds a bit of realism.
                                if (hasClearPath(this.getEyePos(), Vec3d.ofCenter(node.position()))) {
                                     highestIntensity = currentIntensity;
                                     bestNode = node;
                                }
                            }
                        }
                    }

                    if (bestNode != null) {
                        // Don't switch if already investigating a smell unless new one is significantly better or different player
                        if (this.currentSmellTargetNode == null || !this.currentSmellTargetNode.equals(bestNode) || 
                           (this.targetPlayerForSmellUuid != null && !this.targetPlayerForSmellUuid.equals(playerToSmell.getUuid()))) {
                            
                            this.currentSmellTargetNode = bestNode;
                            this.isInvestigatingSmell = true;
                            this.targetPlayerForSmellUuid = playerToSmell.getUuid(); // Ensure we are tracking the correct player
                            com.example.modid.ExampleMod.LOGGER.info("DeepDweller {} started investigating smell from player {} at node {} with intensity {}", 
                                this.getUuidAsString(), playerToSmell.getName().getString(), bestNode.position(), highestIntensity);
                        }
                    } else if (this.isInvestigatingSmell && this.targetPlayerForSmellUuid != null && this.targetPlayerForSmellUuid.equals(playerToSmell.getUuid())) {
                        // If currently investigating this player's smell but no good nodes found, might have lost the trail
                        // com.example.modid.ExampleMod.LOGGER.debug("DeepDweller {} lost scent trail for player {}. No more nodes above threshold.", this.getUuidAsString(), playerToSmell.getName().getString());
                        // clearCurrentSmellTarget(); // Let InvestigateSmellGoal handle losing trail for now
                    }
                }
            } else if (this.isInvestigatingSmell) { // No player to smell, but was investigating
                 clearCurrentSmellTarget();
            }
        } else if (this.isInvestigatingSmell) { // If highly engaged by sound/sight, stop investigating smell
            clearCurrentSmellTarget();
        }
        // --- End Smell System Logic ---
        
        // --- Light Extinguishing Effect (Step 3 & part of 8) ---
        if (this.isAlive() && !this.getWorld().isClient() && this.age % 20 == 0) { // Run every second
            BlockPos centerPos = this.getBlockPos();
            World world = this.getWorld();
            
            // Restore lights that are now outside the radius
            Iterator<Map.Entry<BlockPos, BlockState>> iterator = extinguishedLights.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<BlockPos, BlockState> entry = iterator.next();
                BlockPos lightPos = entry.getKey();
                // Check squared horizontal distance and absolute vertical distance separately for cylindrical check
                double horizontalDistSq = new Vec3d(lightPos.getX(), centerPos.getY(), lightPos.getZ()).squaredDistanceTo(centerPos.getX(), centerPos.getY(), centerPos.getZ());
                int verticalDist = Math.abs(lightPos.getY() - centerPos.getY());

                if (horizontalDistSq > LIGHT_EXTINGUISH_RADIUS * LIGHT_EXTINGUISH_RADIUS || verticalDist > LIGHT_EXTINGUISH_RADIUS_VERTICAL) {
                    if (world.getBlockState(lightPos).isAir()) { 
                        world.setBlockState(lightPos, entry.getValue(), 3);
                        // com.example.modid.ExampleMod.LOGGER.debug("Restored light at {} as DeepDweller moved away.", lightPos);
                    }
                    iterator.remove(); 
                }
            }

            // Extinguish new lights within the radius
            for (int y = -LIGHT_EXTINGUISH_RADIUS_VERTICAL; y <= LIGHT_EXTINGUISH_RADIUS_VERTICAL; y++) { 
                for (int x = -LIGHT_EXTINGUISH_RADIUS; x <= LIGHT_EXTINGUISH_RADIUS; x++) {
                    for (int z = -LIGHT_EXTINGUISH_RADIUS; z <= LIGHT_EXTINGUISH_RADIUS; z++) {
                        BlockPos currentPos = centerPos.add(x, y, z);
                        // Check squared horizontal distance for circular horizontal radius
                        if (new Vec3d(currentPos.getX(), centerPos.getY(), currentPos.getZ()).squaredDistanceTo(centerPos.getX(), centerPos.getY(), centerPos.getZ()) <= LIGHT_EXTINGUISH_RADIUS * LIGHT_EXTINGUISH_RADIUS) {
                            BlockState blockState = world.getBlockState(currentPos);
                            
                            boolean isTargetLightSource = blockState.isOf(Blocks.TORCH) || 
                                                        blockState.isOf(Blocks.WALL_TORCH) || 
                                                        blockState.isOf(Blocks.LANTERN) || 
                                                        blockState.isOf(Blocks.GLOWSTONE);

                            if (isTargetLightSource && !extinguishedLights.containsKey(currentPos)) {
                                extinguishedLights.put(currentPos.toImmutable(), blockState); // Store original state
                                world.setBlockState(currentPos, Blocks.AIR.getDefaultState(), 3); // Replace with Air
                                // com.example.modid.ExampleMod.LOGGER.debug("Extinguished light {} at {} due to DeepDweller.", blockState.getBlock().getName().getString(), currentPos);
                            }
                        }
                    }
                }
            }
        }
        // --- End Light Extinguishing Effect ---

        // --- Freezing Water and Magma (Step 5) ---
        if (this.isAlive() && !this.getWorld().isClient() && this.age % 40 == 0) { // Run every 2 seconds
            BlockPos centerPos = this.getBlockPos();
            World world = this.getWorld();
            for (int y = -FREEZE_VERTICAL_RADIUS; y <= FREEZE_VERTICAL_RADIUS; y++) {
                for (int x = -FREEZE_RADIUS; x <= FREEZE_RADIUS; x++) {
                    for (int z = -FREEZE_RADIUS; z <= FREEZE_RADIUS; z++) {
                        BlockPos currentPos = centerPos.add(x, y, z);
                        if (new Vec3d(currentPos.getX(), centerPos.getY() + y, currentPos.getZ()).squaredDistanceTo(centerPos.getX(), centerPos.getY() + y, centerPos.getZ()) <= FREEZE_RADIUS * FREEZE_RADIUS) {
                            BlockState blockState = world.getBlockState(currentPos);
                            if (blockState.isOf(Blocks.WATER) && blockState.getFluidState().isStill()) {
                                world.setBlockState(currentPos, Blocks.ICE.getDefaultState());
                                // com.example.modid.ExampleMod.LOGGER.debug("Froze water at {}", currentPos);
                            } else if (blockState.isOf(Blocks.MAGMA_BLOCK)) {
                                world.setBlockState(currentPos, Blocks.OBSIDIAN.getDefaultState());
                                // com.example.modid.ExampleMod.LOGGER.debug("Cooled magma at {}", currentPos);
                            }
                            // TODO: Consider reverting these changes when the creature moves away or despawns.
                            // This would require tracking these changed blocks similar to extinguishedLights.
                        }
                    }
                }
            }
        }
        // --- End Freezing Water and Magma ---

        // --- Stomp Sound Effect (Alternative to Screen Shake - Step 6) ---
        // Check if entity is moving: velocity vector length squared > threshold, and on ground
        boolean isMovingOnGround = this.getVelocity().horizontalLengthSquared() > 0.001 && this.isOnGround(); 
        if (this.isAlive() && isMovingOnGround) { 
            this.stompSoundTicks--;
            if (this.stompSoundTicks <= 0) {
                // Play sound at entity's location, client-side volume falloff will handle proximity effect
                this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), ExampleMod.DWELLER_STOMP_EVENT, SoundCategory.HOSTILE, 1.5f, 0.8f);
                this.stompSoundTicks = STOMP_SOUND_INTERVAL_MIN + this.random.nextInt(STOMP_SOUND_INTERVAL_MAX - STOMP_SOUND_INTERVAL_MIN + 1);
                // com.example.modid.ExampleMod.LOGGER.debug("DeepDweller {} played stomp sound. Next in {} ticks.", this.getUuidAsString(), this.stompSoundTicks);
            }
        }
        // --- End Stomp Sound Effect ---

        // --- Other Mobs Flee (Step 7) ---
        if (this.isAlive() && !this.getWorld().isClient() && this.age % 20 == 0) { // Check every second
            List<MobEntity> nearbyMobs = this.getWorld().getEntitiesByClass(
                MobEntity.class, 
                this.getBoundingBox().expand(FLEE_RADIUS), 
                (entity) -> entity != this && entity.isAlive() && !(entity instanceof DeepDwellerEntity) // Exclude self, other dwellers
            );

            for (MobEntity mob : nearbyMobs) {
                // Apply status effects
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, FLEE_EFFECT_DURATION, 1));
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, FLEE_EFFECT_DURATION, 0));

                // Make them path away
                Vec3d selfPosVec = this.getPos();
                Vec3d mobPosVec = mob.getPos();
                Vec3d fleeDir = mobPosVec.subtract(selfPosVec).normalize(); // Direction away from dweller
                
                // Calculate a target position further away
                double fleeDistance = 10.0; // How far to set the flee target
                BlockPos fleeToPos = new BlockPos(
                    (int)(mobPosVec.x + fleeDir.x * fleeDistance),
                    (int)(mobPosVec.y + fleeDir.y * fleeDistance), // May need Y-level adjustment for pathing
                    (int)(mobPosVec.z + fleeDir.z * fleeDistance)
                );
                
                // Ensure the Y-level is somewhat reasonable (e.g., find ground)
                // This is a simplified approach; proper ground finding is more complex.
                BlockPos groundPos = mob.getWorld().getTopPosition(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, fleeToPos);

                mob.getNavigation().startMovingTo(groundPos.getX(), groundPos.getY(), groundPos.getZ(), FLEE_SPEED_MULTIPLIER);
                // com.example.modid.ExampleMod.LOGGER.debug("Mob {} is fleeing from DeepDweller {} to {}", mob.getName().getString(), this.getUuidAsString(), groundPos);
            }
        }
        // --- End Light Extinguishing Effect ---

        // --- Freezing Water and Magma (Step 5 & 8) ---
        if (this.isAlive() && !this.getWorld().isClient() && this.age % 40 == 0) { // Run every 2 seconds
            BlockPos centerPos = this.getBlockPos();
            World world = this.getWorld();

            // Restore frozen blocks that are now outside the radius
            Iterator<Map.Entry<BlockPos, BlockState>> frozenIterator = frozenBlocks.entrySet().iterator();
            while (frozenIterator.hasNext()) {
                Map.Entry<BlockPos, BlockState> entry = frozenIterator.next();
                BlockPos frozenPos = entry.getKey();
                double horizontalDistSq = new Vec3d(frozenPos.getX(), centerPos.getY(), frozenPos.getZ()).squaredDistanceTo(centerPos.getX(), centerPos.getY(), centerPos.getZ());
                int verticalDist = Math.abs(frozenPos.getY() - centerPos.getY());

                if (horizontalDistSq > FREEZE_RADIUS * FREEZE_RADIUS || verticalDist > FREEZE_VERTICAL_RADIUS) {
                    // Restore only if current block is the one we changed it to (ice/obsidian)
                    BlockState currentBlockState = world.getBlockState(frozenPos);
                    if (currentBlockState.isOf(Blocks.ICE) || currentBlockState.isOf(Blocks.OBSIDIAN)) {
                         world.setBlockState(frozenPos, entry.getValue(), 3); // Restore original
                         // com.example.modid.ExampleMod.LOGGER.debug("Restored frozen block at {} as DeepDweller moved away.", frozenPos);
                    }
                    frozenIterator.remove();
                }
            }
            
            // Freeze new blocks
            for (int y = -FREEZE_VERTICAL_RADIUS; y <= FREEZE_VERTICAL_RADIUS; y++) {
                for (int x = -FREEZE_RADIUS; x <= FREEZE_RADIUS; x++) {
                    for (int z = -FREEZE_RADIUS; z <= FREEZE_RADIUS; z++) {
                        BlockPos currentPos = centerPos.add(x, y, z);
                        // Cylindrical check
                        if (new Vec3d(currentPos.getX(), centerPos.getY() + y, currentPos.getZ()).squaredDistanceTo(centerPos.getX(), centerPos.getY() + y, centerPos.getZ()) <= FREEZE_RADIUS * FREEZE_RADIUS) {
                            BlockState blockState = world.getBlockState(currentPos);
                            Block newFrozenBlock = null;
                            
                            if (blockState.isOf(Blocks.WATER) && blockState.getFluidState().isStill()) {
                                newFrozenBlock = Blocks.ICE;
                            } else if (blockState.isOf(Blocks.MAGMA_BLOCK)) {
                                newFrozenBlock = Blocks.OBSIDIAN;
                            }

                            if (newFrozenBlock != null && !frozenBlocks.containsKey(currentPos)) {
                                frozenBlocks.put(currentPos.toImmutable(), blockState); // Store original
                                world.setBlockState(currentPos, newFrozenBlock.getDefaultState());
                                // com.example.modid.ExampleMod.LOGGER.debug("Froze {} at {}", blockState.getBlock().getName().getString(), currentPos);
                            }
                        }
                    }
                }
            }
        }
        // --- End Other Mobs Flee ---


        // --- Phase 2 Audio Cues (Dweller Presence Clicks) ---
        if (this.isAlive()) { 
            boolean isActive = this.hasHighPiSound || 
                               this.isInvestigatingSmell || 
                               (this.getNavigation().getTargetPos() != null && !this.getNavigation().isIdle()); 

            if (isActive) {
                this.presenceSoundTicks--;
                if (this.presenceSoundTicks <= 0) {
                    this.getWorld().playSound(null, this.getBlockPos(), ExampleMod.DWELLER_PRESENCE_CLICKS_EVENT, SoundCategory.HOSTILE, 0.7f, 1.0f);
                    this.presenceSoundTicks = PRESENCE_SOUND_INTERVAL_MIN + this.random.nextInt(PRESENCE_SOUND_INTERVAL_MAX - PRESENCE_SOUND_INTERVAL_MIN + 1);
                }
            }
        }
        // --- End Phase 2 Audio Cues ---
    }

    @Override
    public void remove(RemovalReason reason) {
        World world = getWorld();
        if (!world.isClient) {
            // Restore all extinguished lights
            if (extinguishedLights != null) {
                com.example.modid.ExampleMod.LOGGER.info("DeepDweller {} is being removed. Reason: {}. Restoring {} extinguished lights.", 
                    this.getUuidAsString(), reason, extinguishedLights.size());
                for (Map.Entry<BlockPos, BlockState> entry : extinguishedLights.entrySet()) {
                    if (world.getBlockState(entry.getKey()).isAir()) { 
                        world.setBlockState(entry.getKey(), entry.getValue(), 3);
                    }
                }
                extinguishedLights.clear();
            }
            // Restore all frozen blocks
            if (frozenBlocks != null) {
                 com.example.modid.ExampleMod.LOGGER.info("DeepDweller {} is being removed. Restoring {} frozen blocks.", 
                    this.getUuidAsString(), frozenBlocks.size());
                for (Map.Entry<BlockPos, BlockState> entry : frozenBlocks.entrySet()) {
                    BlockState currentBlockState = world.getBlockState(entry.getKey());
                    if (currentBlockState.isOf(Blocks.ICE) || currentBlockState.isOf(Blocks.OBSIDIAN)) {
                        world.setBlockState(entry.getKey(), entry.getValue(), 3); // Restore original
                    }
                }
                frozenBlocks.clear();
            }
        }
        super.remove(reason);
    }
    
    // Getter methods for AI goals
    @Nullable
    public Vec3d getCurrentSoundSourceLocation() {
        return currentSoundSourceLocation;
    }

    public float getCurrentSoundPerceivedIntensity() {
        return currentSoundPerceivedIntensity;
    }
    
    public boolean isActivelyListening() {
        return isActivelyListening;
    }

    public boolean hasHighPiSound() {
        return hasHighPiSound;
    }
    
    public void clearHighPiSoundFlag() {
        this.hasHighPiSound = false;
    }

    public void clearCurrentSoundSource() {
        this.currentSoundSourceLocation = null;
        this.currentSoundPerceivedIntensity = 0f;
    }

    @Nullable
    public ScentNode getCurrentSmellTargetNode() {
        return currentSmellTargetNode;
    }

    public boolean isInvestigatingSmell() {
        return isInvestigatingSmell;
    }

    public void clearCurrentSmellTarget() {
        this.currentSmellTargetNode = null;
        this.isInvestigatingSmell = false;
        // this.targetPlayerForSmellUuid = null; // Keep targetPlayerForSmellUuid so it can try to re-acquire trail if player comes back in range
        // com.example.modid.ExampleMod.LOGGER.info("DeepDweller {} cleared current smell target.", this.getUuidAsString());
    }
    
    public UUID getTargetPlayerForSmellUuid() {
        return targetPlayerForSmellUuid;
    }


    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        // Override to prevent despawning based on distance, will rely on persistence or manager.
        return false; 
    }
    
    @Override
import net.minecraft.entity.damage.DamageSource; // Added import

    public boolean cannotDespawn() {
        // Further ensure it doesn't despawn unless explicitly told to or killed.
        return true;
    }

    // --- Invincibility ---
    @Override
    public boolean damage(DamageSource source, float amount) {
        // Prevent all damage by returning false.
        // Optionally, log the damage attempt for debugging.
        // com.example.modid.ExampleMod.LOGGER.info("DeepDwellerEntity: Damage attempt by {} for {} hp - blocked.", source.getName(), amount);
        return false; 
    }

    @Override
    public booleanisFireImmune() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) {
        // Make it invulnerable to pretty much everything.
        // Specific checks can be added if needed, e.g. damageSource == DamageSource.OUT_OF_WORLD
        if (damageSource.isOutOfWorld()) { // Still allow /kill command or falling into void to remove it
            return false;
        }
        return true;
    }
    // --- End Invincibility ---

    @Override
    protected boolean isDisallowedInPeaceful() {
        return true; // Standard for hostile entities, will not spawn in peaceful.
    }
}
