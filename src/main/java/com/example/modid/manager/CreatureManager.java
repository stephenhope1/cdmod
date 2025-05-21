package com.example.modid.manager;

import com.example.modid.entity.DeepDwellerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

public class CreatureManager {

import com.example.modid.ExampleMod;
import net.minecraft.entity.SpawnReason;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.entity.ai.pathing.Path; // For pathfinding check
import net.minecraft.entity.mob.MobEntity; // For Navigation


public class CreatureManager {

    private DeepDwellerEntity activeDeepDweller;
    private long lastSpawnAttemptTick = 0;

    // Configuration (placeholders, move to config later)
    public int spawnAttemptInterval = 2400; // Approx 2 minutes (20 ticks/sec * 60 sec/min * 2 min)
    public double spawnChance = 0.15;        // 15% chance per attempt
    public int minSpawnDistance = 40;       // Min blocks away from player (reduced for testing)
    public int maxSpawnDistance = 70;       // Max blocks away from player (reduced for testing)
    public int spawnHeightRange = 10;       // Max Y distance difference from player for spawn point search
    public int maxSpawnAttemptsPerCycle = 10; // Number of random positions to try per valid spawn cycle


    public CreatureManager() {
        this.activeDeepDweller = null;
    }

    public boolean isCreatureSpawned() {
        return activeDeepDweller != null && activeDeepDweller.isAlive();
    }

    @Nullable
    public DeepDwellerEntity getSpawnedCreature() {
        if (activeDeepDweller != null && activeDeepDweller.isAlive()) {
            return activeDeepDweller;
        }
        // If the creature is dead or removed, nullify the reference
        if (activeDeepDweller != null && !activeDeepDweller.isAlive()) {
            setSpawnedCreature(null); // Creature is no longer valid
        }
        return null;
    }

    public void setSpawnedCreature(@Nullable DeepDwellerEntity creature) {
        this.activeDeepDweller = creature;
        if (creature != null) {
            com.example.modid.ExampleMod.LOGGER.info("Deep Dweller tracked: {}", creature.getUuidAsString());
        } else {
            com.example.modid.ExampleMod.LOGGER.info("Deep Dweller untracked (killed or despawned).");
        }
    }

    public void trySpawnCreature(ServerWorld world, ServerPlayerEntity player, AlertLevelManager alertLevelManager) {
        long currentTick = world.getServer().getTicks();

        // 1. Phase 2 Check
        if (!alertLevelManager.hasReachedPhase2Threshold(player)) {
            return;
        }

        // 2. Single Instance Check
        if (isCreatureSpawned()) {
            return;
        }

        // 3. Timed Spawn Chance
        if (currentTick - lastSpawnAttemptTick < spawnAttemptInterval) {
            return;
        }
        lastSpawnAttemptTick = currentTick;

        if (world.getRandom().nextDouble() > spawnChance) {
            ExampleMod.LOGGER.info("Deep Dweller spawn chance failed this cycle for player {}", player.getName().getString());
            return;
        }
        ExampleMod.LOGGER.info("Attempting Deep Dweller spawn for player {} (Alert: {})", player.getName().getString(), alertLevelManager.getAlertLevel(player));


        // 4. Spawning Conditions
        BlockPos playerPos = player.getBlockPos();
        BlockPos spawnPos = null;

        for (int i = 0; i < maxSpawnAttemptsPerCycle; i++) {
            // Generate random spherical coordinates
            double angle = world.getRandom().nextDouble() * 2 * Math.PI;
            double distance = minSpawnDistance + world.getRandom().nextDouble() * (maxSpawnDistance - minSpawnDistance);
            int xOffset = (int) (Math.cos(angle) * distance);
            int zOffset = (int) (Math.sin(angle) * distance);
            
            // Try to find a Y level within a range around the player's Y, or deeper
            int yBase = Math.min(playerPos.getY(), world.getBottomY() + spawnHeightRange + 5); // Bias towards deeper spawns if player is high
            int yOffset = world.getRandom().nextInt(spawnHeightRange * 2) - spawnHeightRange;
            int spawnY = Math.max(world.getBottomY() + 1, Math.min(yBase + yOffset, world.getTopY() - 2));

            BlockPos candidatePos = new BlockPos(playerPos.getX() + xOffset, spawnY, playerPos.getZ() + zOffset);

            // A. Valid Spawnable Block (Simplified: Check if block is replaceable and two blocks above are air)
            if (world.getBlockState(candidatePos).isReplaceable() &&
                world.isAir(candidatePos.up()) &&
                world.isAir(candidatePos.up(2))) {
                
                // B. Not in LOS (Simplified: Raycast from player head to candidatePos center)
                Vec3d playerEyePos = player.getEyePos();
                Vec3d candidateVec = Vec3d.ofCenter(candidatePos);
                BlockHitResult hitResult = world.raycast(new RaycastContext(playerEyePos, candidateVec, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
                if (hitResult.getType() == BlockHitResult.Type.MISS || !hitResult.getBlockPos().equals(candidatePos)) { // If miss, or hit something else first

                    // C. Pathfinding Availability (Very Simplified: check if it can path to player's general area)
                    // This is a placeholder for a more robust pathfinding check to a point *near* the player.
                    // For now, we assume if it's not in LOS and a valid spawnable block, it's "good enough" for Phase 2.
                    // A full pathfinding check here can be expensive.
                    // We could try pathing to player.getBlockPos().add(0,0,0) but it might fail if player is in a tight spot.
                    // Let's spawn it and let its AI handle pathing. If it can't reach, that's part of the challenge.
                    // We will rely on the creature's own follow_range attribute.

                    spawnPos = candidatePos;
                    ExampleMod.LOGGER.info("Found suitable spawn position for Deep Dweller: {} (Attempt {}/{})", spawnPos, i + 1, maxSpawnAttemptsPerCycle);
                    break; 
                } else {
                    // ExampleMod.LOGGER.debug("Spawn candidate {} in LOS or obstructed by self.", candidatePos);
                }
            } else {
                 // ExampleMod.LOGGER.debug("Spawn candidate {} not valid (not replaceable or not enough space).", candidatePos);
            }
        }

        if (spawnPos == null) {
            ExampleMod.LOGGER.info("Failed to find a suitable spawn location for Deep Dweller for player {} this cycle.", player.getName().getString());
            return;
        }

        // 5. Actual Spawning
        DeepDwellerEntity deepDweller = ExampleMod.DEEP_DWELLER_ENTITY_TYPE.create(world);
        if (deepDweller != null) {
            deepDweller.refreshPositionAndAngles(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, world.getRandom().nextFloat() * 360.0F, 0.0F);
            // Call initialize before adding to world if it relies on world access during its setup,
            // though typically initialize is called by the world itself after adding.
            // For setting custom data like target player, do it after creation and before/immediately after adding to world.
            world.spawnEntity(deepDweller); // Add to world first so it has a world instance
            deepDweller.setInitialTargetPoint(player); // Now set the target point using the player reference
            setSpawnedCreature(deepDweller);
            ExampleMod.LOGGER.info("!!! Deep Dweller SPAWNED at {} for player {} !!! Initial target: {}", spawnPos, player.getName().getString(), deepDweller.getInitialTargetLocation());
        } else {
            ExampleMod.LOGGER.error("Failed to create DeepDwellerEntity instance!");
        }
    }

    // Method to be called when the Deep Dweller dies or is removed
    // This will be hooked up via a mixin later (Step 4)
    public void onCreatureRemoved(DeepDwellerEntity creature) {
        if (activeDeepDweller == creature) {
            setSpawnedCreature(null);
        }
    }
}
