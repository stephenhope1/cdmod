package com.example.modid.manager;

import net.minecraft.server.network.ServerPlayerEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AlertLevelManager {

    public static final double MAX_ALERT_LEVEL = 100.0;
    public static final double PHASE_2_THRESHOLD = 75.0; // Example threshold
    private static final double DEPTH_MODIFIER_THRESHOLD_Y = 40.0;
    private static final double DEPTH_MODIFIER_FACTOR = 1.5;
    private static final double SNEAKING_MODIFIER_FACTOR = 0.5;

    private final Map<UUID, Double> playerAlertLevels;

    public AlertLevelManager() {
        this.playerAlertLevels = new HashMap<>();
    }

    /**
     * Gets the current alert level for a given player.
     * Defaults to 0.0 if the player is not yet tracked.
     *
     * @param player The player.
     * @return The player's current alert level.
     */
    public double getAlertLevel(ServerPlayerEntity player) {
        return playerAlertLevels.getOrDefault(player.getUuid(), 0.0);
    }

    /**
     * Increases the alert level for a player based on sound intensity and other factors.
     *
     * @param player         The player who generated the sound.
     * @param soundIntensity The intensity of the sound generated.
     */
    public void increaseAlertLevel(ServerPlayerEntity player, double soundIntensity) {
        if (player == null) return; // Should not happen with proper event handling

        double currentLevel = getAlertLevel(player);
        double increaseAmount = soundIntensity;

        // Factor in player's depth (Y-coordinate)
        if (player.getY() < DEPTH_MODIFIER_THRESHOLD_Y) {
            increaseAmount *= DEPTH_MODIFIER_FACTOR;
        }

        // Factor in if the player is sneaking
        if (player.isSneaking()) {
            increaseAmount *= SNEAKING_MODIFIER_FACTOR;
        }

        double newLevel = currentLevel + increaseAmount;

        if (newLevel > MAX_ALERT_LEVEL) {
            newLevel = MAX_ALERT_LEVEL;
        }
        
        playerAlertLevels.put(player.getUuid(), newLevel);
        // com.example.modid.ExampleMod.LOGGER.info("Alert level for " + player.getName().getString() + " increased to " + newLevel + " (increase: " + increaseAmount + ")");
    }

    private static final double BASE_DECAY_RATE = 0.1; // Per call; adjust based on call frequency
    private static final double DEPTH_DECAY_MODIFIER_FACTOR = 0.75; // Slower decay at depth

    /**
     * Updates the alert level decay for a specific player.
     * This method should be called periodically.
     *
     * @param player The player whose alert level decay is to be updated.
     */
    public void updateAlertLevelDecay(ServerPlayerEntity player) {
        if (player == null) return;

        double currentLevel = getAlertLevel(player);
        if (currentLevel == 0) return; // No decay if already at 0

        double decayAmount = BASE_DECAY_RATE;

        // Factor in player's depth (Y-coordinate) - Slower decay at depth
        if (player.getY() < DEPTH_MODIFIER_THRESHOLD_Y) {
            decayAmount *= DEPTH_DECAY_MODIFIER_FACTOR;
        }

        // TODO: Implement accelerated decay if the player has been "quiet"
        // (minimal sound generation) for a certain duration.
        // This would require tracking last sound generation time or similar.

        double newLevel = currentLevel - decayAmount;

        if (newLevel < 0) {
            newLevel = 0;
        }
        playerAlertLevels.put(player.getUuid(), newLevel);
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sound.SoundCategory; // Keep this import
import com.example.modid.ExampleMod; // Keep this import

// ... (other imports should be fine)

// ... (class definition and existing fields)
    // Make sure this is initialized in the constructor
    private final java.util.List<FlickeringTorch> flickeringTorches;

    public AlertLevelManager() {
        this.playerAlertLevels = new HashMap<>();
        this.flickeringTorches = new java.util.ArrayList<>(); // Initialize here
    }
    
    // ... (getAlertLevel method)

    @Override
    public void increaseAlertLevel(ServerPlayerEntity player, double soundIntensity) {
        if (player == null) return; 

        double currentLevel = getAlertLevel(player);
        double oldLevel = currentLevel; // Store old level for alert-driven sounds
        double increaseAmount = soundIntensity;

        if (player.getY() < DEPTH_MODIFIER_THRESHOLD_Y) {
            increaseAmount *= DEPTH_MODIFIER_FACTOR;
        }

        if (player.isSneaking()) {
            increaseAmount *= SNEAKING_MODIFIER_FACTOR;
        }

        double newLevel = currentLevel + increaseAmount;

        if (newLevel > MAX_ALERT_LEVEL) {
            newLevel = MAX_ALERT_LEVEL;
        }
        
        playerAlertLevels.put(player.getUuid(), newLevel);
        ExampleMod.LOGGER.info("Alert level for " + player.getName().getString() + " increased to " + newLevel + " (increase: " + increaseAmount + ")");
    
        // Call alert-driven sound logic if level increased significantly
        if (newLevel > oldLevel) {
            playAlertDrivenSounds(player, oldLevel, newLevel);
        }
    }
    
    // ... (updateAlertLevelDecay method - ensure it's the version from the previous correct step)
    // The version of updateAlertLevelDecay from step 3 (turn 16) was correct.

    // --- Ambient Sound Logic (ensure this is the version from step 3 - turn 16) ---
    private static final double AMBIENT_SOUND_BASE_CHANCE = 0.005; 
    private static final double AMBIENT_SOUND_CHANCE_ALERT_FACTOR = 0.025;

    public void playAmbientSounds(ServerPlayerEntity player) {
        if (player == null || player.getWorld().isClient) {
            return;
        }

        double alertLevel = getAlertLevel(player);
        // Only play general ambient sounds if alert is below a certain threshold
        if (alertLevel > PHASE_2_THRESHOLD * 0.7 && player.getWorld().getRandom().nextDouble() < 0.75) { // Reduce chance significantly if high alert
             return; 
        }

        double chanceToPlay = AMBIENT_SOUND_BASE_CHANCE + (alertLevel / MAX_ALERT_LEVEL) * AMBIENT_SOUND_CHANCE_ALERT_FACTOR;
        if (alertLevel < 20) chanceToPlay *= 0.5; // Less frequent if alert is very low


        if (player.getWorld().getRandom().nextDouble() < chanceToPlay) {
            SoundEvent selectedSound = null;
            int soundChoice = player.getWorld().getRandom().nextInt(4); 

            switch (soundChoice) {
                case 0: selectedSound = ExampleMod.CAVE_WHISTLES_EVENT; break;
                case 1: selectedSound = ExampleMod.WHOOPS_EVENT; break;
                case 2: selectedSound = ExampleMod.PEBBLES_DROPPING_EVENT; break;
                case 3: selectedSound = ExampleMod.DISTANT_CLICKING_EVENT; break;
            }

            if (selectedSound != null) {
                float volume = 0.5f + player.getWorld().getRandom().nextFloat() * 0.3f; 
                float pitch = 0.8f + player.getWorld().getRandom().nextFloat() * 0.4f;  

                player.getWorld().playSound(
                        null, 
                        player.getBlockPos(),
                        selectedSound,
                        SoundCategory.AMBIENT,
                        volume,
                        pitch
                );
                // ExampleMod.LOGGER.info("Played ambient sound {} for {} at alert {}", selectedSound.getId().toString(), player.getName().getString(), alertLevel);
            }
        }
    }
    // --- End Ambient Sound Logic ---

    // --- Alert-Driven Sound Logic ---
    private static final double ALERT_SOUND_BASE_CHANCE = 0.15; // Slightly higher base chance
    private static final double ALERT_SOUND_MIN_INCREASE_FOR_CHANCE = 3.0; // Lowered min increase

    public void playAlertDrivenSounds(ServerPlayerEntity player, double oldLevel, double newLevel) {
        if (player == null || player.getWorld().isClient) {
            return;
        }

        double increase = newLevel - oldLevel;
        
        if (increase < ALERT_SOUND_MIN_INCREASE_FOR_CHANCE && newLevel < PHASE_2_THRESHOLD * 0.5) { // Don't play for tiny increases unless closer to phase 2
             return; 
        }

        double chanceToPlay = ALERT_SOUND_BASE_CHANCE + (newLevel / MAX_ALERT_LEVEL) * 0.25; // Increase chance with higher alert
        if (player.getWorld().getRandom().nextDouble() < chanceToPlay) {
            SoundEvent selectedSound = null;
            if (newLevel > PHASE_2_THRESHOLD * 0.6 && player.getWorld().getRandom().nextBoolean()) {
                selectedSound = ExampleMod.DISTANT_SHRIEKING_EVENT;
            } else {
                selectedSound = ExampleMod.HEAVY_FOOTSTEPS_EVENT;
            }

            if (selectedSound != null) {
                BlockPos playerPos = player.getBlockPos();
                int offsetX = (player.getWorld().getRandom().nextBoolean() ? 1 : -1) * (8 + player.getWorld().getRandom().nextInt(12)); // 8-19 blocks away
                int offsetZ = (player.getWorld().getRandom().nextBoolean() ? 1 : -1) * (8 + player.getWorld().getRandom().nextInt(12));
                // Ensure sound doesn't come from straight up or down, keep it roughly on player's Y level or slightly offset
                int offsetY = player.getWorld().getRandom().nextInt(5) - 2; // -2 to +2 blocks Y offset
                BlockPos soundOrigin = playerPos.add(offsetX, offsetY, offsetZ);

                float volume = 0.7f + (float)(newLevel / MAX_ALERT_LEVEL) * 0.8f; 
                if (volume > 1.5f) volume = 1.5f;
                float pitch = 0.7f + player.getWorld().getRandom().nextFloat() * 0.3f;  

                player.getWorld().playSound(
                        null, 
                        soundOrigin.getX(), soundOrigin.getY(), soundOrigin.getZ(),
                        selectedSound,
                        SoundCategory.AMBIENT, 
                        volume,
                        pitch
                );
                 ExampleMod.LOGGER.info("Played alert-driven sound {} for {} (alert {} -> {}) from {}", 
                    selectedSound.getId().toString(), player.getName().getString(), oldLevel, newLevel, soundOrigin.toString());
            }
        }
    }
    // --- End Alert-Driven Sound Logic ---


    // --- Torch Flicker Logic ---
    private static final double TORCH_FLICKER_MIN_ALERT = 20.0; // Lowered min alert
    private static final int TORCH_FLICKER_RADIUS = 12; // Slightly larger radius
    private static final double TORCH_FLICKER_CHANCE_BASE = 0.008; // Slightly increased base chance
    private static final int TORCH_FLICKER_DURATION_TICKS_MIN = 2;
    private static final int TORCH_FLICKER_DURATION_TICKS_MAX = 6; // Max duration increased

    private static class FlickeringTorch {
        BlockPos pos;
        BlockState originalState;
        int ticksRemaining;
        // Using String for world registry key identifier for more robust (though slightly less performant) comparison
        String worldRegistryKeyId; 

        FlickeringTorch(BlockPos pos, BlockState originalState, int ticksRemaining, World world) {
            this.pos = pos;
            this.originalState = originalState;
            this.ticksRemaining = ticksRemaining;
            this.worldRegistryKeyId = world.getRegistryKey().getValue().toString();
        }
    }
import com.example.modid.entity.DeepDwellerEntity; // Added for check
import com.example.modid.ExampleMod; // For CreatureManager access

    // private final java.util.List<FlickeringTorch> flickeringTorches; // Already declared and initialized in constructor

    public void handleTorchFlicker(ServerPlayerEntity player) {
        if (player == null || player.getWorld().isClient) {
            return;
        }

        // If Deep Dweller is spawned and player is close to it, suppress this alert-based flicker
        // as the Dweller's own light extinguishing effect should dominate.
        CreatureManager creatureManager = ExampleMod.getCreatureManager();
        if (creatureManager != null && creatureManager.isCreatureSpawned()) {
            DeepDwellerEntity dweller = creatureManager.getSpawnedCreature();
            if (dweller != null && dweller.getWorld() == player.getWorld() && 
                dweller.distanceTo(player) < DeepDwellerEntity.LIGHT_EXTINGUISH_RADIUS + 10) { // If player is within dweller's aura + buffer
                // ExampleMod.LOGGER.debug("AlertLevelManager#handleTorchFlicker suppressed for {} due to nearby Deep Dweller.", player.getName().getString());
                // Potentially restore any torches this manager was flickering if dweller's effect takes over
                // For now, just returning is simpler. The dweller's effect will extinguish them anyway.
                return; 
            }
        }

        double alertLevel = getAlertLevel(player);
        if (alertLevel < TORCH_FLICKER_MIN_ALERT) {
            return;
        }

        World world = player.getWorld();
        BlockPos playerPos = player.getBlockPos();
        String currentWorldId = world.getRegistryKey().getValue().toString();

        if (world.getRandom().nextDouble() > 0.3) return; // Reduce how often we scan for torches overall

        for (int x = -TORCH_FLICKER_RADIUS; x <= TORCH_FLICKER_RADIUS; x++) {
            for (int y = -TORCH_FLICKER_RADIUS; y <= TORCH_FLICKER_RADIUS; y++) {
                for (int z = -TORCH_FLICKER_RADIUS; z <= TORCH_FLICKER_RADIUS; z++) {
                    if (playerPos.add(x,y,z).getManhattanDistance(playerPos) > TORCH_FLICKER_RADIUS) continue; // Check spherical radius

                    BlockPos currentPos = playerPos.add(x, y, z);
                    BlockState blockState = world.getBlockState(currentPos);

                    if (blockState.isOf(Blocks.TORCH) || blockState.isOf(Blocks.WALL_TORCH)) {
                        boolean isAlreadyFlickering = false;
                        for (FlickeringTorch ft : flickeringTorches) {
                            if (ft.pos.equals(currentPos) && ft.worldRegistryKeyId.equals(currentWorldId)) {
                                isAlreadyFlickering = true;
                                break;
                            }
                        }
                        if (isAlreadyFlickering) continue;

                        double chance = TORCH_FLICKER_CHANCE_BASE * Math.pow((alertLevel / MAX_ALERT_LEVEL), 2);
                        if (world.getRandom().nextDouble() < chance) {
                            int duration = TORCH_FLICKER_DURATION_TICKS_MIN + world.getRandom().nextInt(TORCH_FLICKER_DURATION_TICKS_MAX - TORCH_FLICKER_DURATION_TICKS_MIN + 1);
                            flickeringTorches.add(new FlickeringTorch(currentPos, blockState, duration, world));
                            world.setBlockState(currentPos, Blocks.CAVE_AIR.getDefaultState(), 3); 
                            ExampleMod.LOGGER.info("Flickering torch OFF at {} for {} (Alert: {})", currentPos, player.getName().getString(), alertLevel);
                        }
                    }
                }
            }
        }
    }

    public void updateFlickeringTorches(MinecraftServer server) {
        if (flickeringTorches.isEmpty()) return;
        java.util.Iterator<FlickeringTorch> iterator = flickeringTorches.iterator();
        while (iterator.hasNext()) {
            FlickeringTorch ft = iterator.next();
            ft.ticksRemaining--;
            if (ft.ticksRemaining <= 0) {
                boolean restored = false;
                for (net.minecraft.server.world.ServerWorld world : server.getWorlds()) {
                    if (world.getRegistryKey().getValue().toString().equals(ft.worldRegistryKeyId)) {
                        BlockState currentState = world.getBlockState(ft.pos);
                        if (currentState.isAir()) { // Only restore if it's still air
                            world.setBlockState(ft.pos, ft.originalState, 3);
                           // ExampleMod.LOGGER.info("Restored torch at {} in world {}", ft.pos, world.getRegistryKey().getValue());
                        } else {
                           // ExampleMod.LOGGER.warn("Torch at {} was replaced by {} before it could be restored. Skipping.", ft.pos, currentState.getBlock().getName().getString());
                        }
                        restored = true;
                        break; 
                    }
                }
                iterator.remove(); // Remove after processing
            }
        }
    }
    // --- End Torch Flicker Logic ---
    
    // ... (hasReachedPhase2Threshold method)
    // ... (updateAllPlayersDecay method - commented out)
}
