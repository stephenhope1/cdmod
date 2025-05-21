package com.example.modid.entity.ai.goal;

import com.example.modid.entity.DeepDwellerEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import java.util.EnumSet;

public class GoToTargetLocationGoal extends Goal {
    private final DeepDwellerEntity mob;
    private final double speed;
    private BlockPos targetLocation;
    private static final int ARRIVAL_THRESHOLD_SQUARED = 4; // Within 2 blocks (squared)

    public GoToTargetLocationGoal(DeepDwellerEntity mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        this.setControls(EnumSet.of(Goal.Control.MOVE)); // This goal controls movement
    }

    @Override
    public boolean canStart() {
        this.targetLocation = this.mob.getInitialTargetLocation();
        if (this.targetLocation == null) {
            // com.example.modid.ExampleMod.LOGGER.debug("GoToTargetLocationGoal: Cannot start, targetLocation is null for {}", this.mob.getUuidAsString());
            return false;
        }
        // Don't start if already very close (e.g., if spawn somehow was right on target)
        boolean can = this.mob.getPos().squaredDistanceTo(this.targetLocation.getX(), this.targetLocation.getY(), this.targetLocation.getZ()) > ARRIVAL_THRESHOLD_SQUARED;
        // if (can) com.example.modid.ExampleMod.LOGGER.info("GoToTargetLocationGoal: Can start for {}, target: {}", this.mob.getUuidAsString(), this.targetLocation);
        return can;
    }

    @Override
    public boolean shouldContinue() {
        if (this.targetLocation == null || this.mob.getNavigation().isIdle()) {
            // com.example.modid.ExampleMod.LOGGER.info("GoToTargetLocationGoal: Should not continue for {} (target null or idle nav)", this.mob.getUuidAsString());
            return false;
        }
        boolean should = this.mob.getPos().squaredDistanceTo(this.targetLocation.getX(), this.targetLocation.getY(), this.targetLocation.getZ()) > ARRIVAL_THRESHOLD_SQUARED;
        // if (!should) com.example.modid.ExampleMod.LOGGER.info("GoToTargetLocationGoal: Reached target or stopping for {}", this.mob.getUuidAsString());
        return should;
    }

    @Override
    public void start() {
        if (this.targetLocation != null) {
            // com.example.modid.ExampleMod.LOGGER.info("GoToTargetLocationGoal: Starting pathfinding for {} to {}", this.mob.getUuidAsString(), this.targetLocation);
            this.mob.getNavigation().startMovingTo(this.targetLocation.getX(), this.targetLocation.getY(), this.targetLocation.getZ(), this.speed);
        } else {
            // com.example.modid.ExampleMod.LOGGER.warn("GoToTargetLocationGoal: Tried to start with null targetLocation for {}", this.mob.getUuidAsString());
        }
    }

    @Override
    public void stop() {
        // com.example.modid.ExampleMod.LOGGER.info("GoToTargetLocationGoal: Stopped for {}. Current Nav Idle: {}", this.mob.getUuidAsString(), this.mob.getNavigation().isIdle());
        // Clear the initial target location once this goal stops (either by arrival or interruption)
        // so it doesn't try to restart pathing to the same old point if another goal takes over temporarily.
        this.mob.clearInitialTargetLocation(); // Call the method to clear the target in the entity
        
        // Only stop navigation if the mob is the one that initiated it via this goal.
        // This prevents this goal from stopping navigation started by another goal.
        if (this.mob.getNavigation().getTargetPos() != null && this.mob.getNavigation().getTargetPos().equals(this.targetLocation)) {
             this.mob.getNavigation().stop();
        }
    }

    @Override
    public void tick() {
        // If the target location is somehow cleared while running, stop.
        if (this.mob.getInitialTargetLocation() == null) {
            this.targetLocation = null; // Ensure this goal also stops
            return;
        }
        // Optional: If path becomes stuck, could add logic here to stop or replan.
        // For now, rely on default navigation behavior.
        // com.example.modid.ExampleMod.LOGGER.debug("GoToTargetLocationGoal: Ticking for {}. Target: {}", this.mob.getUuidAsString(), this.targetLocation);
    }
}
