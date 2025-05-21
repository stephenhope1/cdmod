package com.example.modid.entity.ai.goal;

import com.example.modid.entity.DeepDwellerEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.EnumSet;

public class InvestigateSoundGoal extends Goal {
    private final DeepDwellerEntity mob;
    private final double speed;
    private Vec3d soundLocation;
    private BlockPos navigationTarget;
    private int searchTicks; // Ticks spent "searching" at the location

    private static final int MAX_SEARCH_RADIUS = 15; // Max radius to search around sound source
    private static final int MIN_SEARCH_RADIUS = 3;  // Min radius (for very high PI)
    private static final float RADIUS_REDUCTION_PER_PI = 0.5f; // How much radius decreases per PI point
    private static final int ARRIVAL_THRESHOLD_SQUARED = 9; // Within 3 blocks (squared)
    private static final int SEARCH_DURATION_TICKS = 5 * 20; // 5 seconds of searching

    public InvestigateSoundGoal(DeepDwellerEntity mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (this.mob.hasHighPiSound()) { // Don't investigate if in "chase" mode
            // com.example.modid.ExampleMod.LOGGER.debug("InvestigateSoundGoal: Cannot start, mob hasHighPiSound for {}", this.mob.getUuidAsString());
            return false;
        }
        this.soundLocation = this.mob.getCurrentSoundSourceLocation();
        if (this.soundLocation == null || this.mob.getCurrentSoundPerceivedIntensity() <= 0) {
            // com.example.modid.ExampleMod.LOGGER.debug("InvestigateSoundGoal: Cannot start, soundLocation null or PI zero for {}", this.mob.getUuidAsString());
            return false;
        }
        // Don't restart if already at the location and searching, or if already very close to sound source
        if (this.navigationTarget != null && this.mob.getBlockPos().isWithinDistance(this.navigationTarget, Math.sqrt(ARRIVAL_THRESHOLD_SQUARED) + 2) && searchTicks > 0) {
             return false;
        }
        if (this.mob.getPos().squaredDistanceTo(this.soundLocation) < ARRIVAL_THRESHOLD_SQUARED + 4) { // If already very close to the direct sound source
            // com.example.modid.ExampleMod.LOGGER.debug("InvestigateSoundGoal: Cannot start, already too close to sound source {} for {}", this.soundLocation, this.mob.getUuidAsString());
            // mob.clearCurrentSoundSource(); // Clear it as we are "at" it.
            return false;
        }
        // com.example.modid.ExampleMod.LOGGER.info("InvestigateSoundGoal: Can start for {}, sound at {}, PI: {}", this.mob.getUuidAsString(), this.soundLocation, this.mob.getCurrentSoundPerceivedIntensity());
        return true;
    }

    @Override
    public boolean shouldContinue() {
        if (this.mob.hasHighPiSound() || this.soundLocation == null || this.mob.getCurrentSoundSourceLocation() == null) {
            // com.example.modid.ExampleMod.LOGGER.info("InvestigateSoundGoal: Should NOT continue for {} (high PI, or soundLocation/currentSound cleared)", this.mob.getUuidAsString());
            return false;
        }
        // If a new, different sound source has been set in the entity, this goal should re-evaluate.
        if (!this.soundLocation.equals(this.mob.getCurrentSoundSourceLocation())) {
            // com.example.modid.ExampleMod.LOGGER.info("InvestigateSoundGoal: Sound source changed for {}, stopping current investigation.", this.mob.getUuidAsString());
            return false; 
        }
        if (searchTicks > 0) return true; // Continue if in searching phase

        boolean should = !this.mob.getNavigation().isIdle() && this.navigationTarget != null &&
               this.mob.getBlockPos().getSquaredDistance(this.navigationTarget) > ARRIVAL_THRESHOLD_SQUARED;
        // if (!should) com.example.modid.ExampleMod.LOGGER.info("InvestigateSoundGoal: Reached nav target or stopping for {}", this.mob.getUuidAsString());
        return should;
    }

    @Override
    public void start() {
        this.soundLocation = this.mob.getCurrentSoundSourceLocation(); // Refresh at start
        if (this.soundLocation == null) return; // Should not happen due to canStart

        float pi = this.mob.getCurrentSoundPerceivedIntensity();
        float radius = Math.max(MIN_SEARCH_RADIUS, MAX_SEARCH_RADIUS - (pi * RADIUS_REDUCTION_PER_PI));

        // Find a random point within the sphere around soundLocation
        double randomAngle = this.mob.getRandom().nextDouble() * 2 * Math.PI;
        double randomYAngle = this.mob.getRandom().nextDouble() * Math.PI - (Math.PI/2);
        double randomDist = this.mob.getRandom().nextDouble() * radius;

        double xOffset = Math.cos(randomAngle) * Math.cos(randomYAngle) * randomDist;
        double zOffset = Math.sin(randomAngle) * Math.cos(randomYAngle) * randomDist;
        double yOffset = Math.sin(randomYAngle) * randomDist;
        
        this.navigationTarget = new BlockPos((int)(this.soundLocation.x + xOffset), (int)(this.soundLocation.y + yOffset), (int)(this.soundLocation.z + zOffset));
        
        // Ensure target is within valid world height
        this.navigationTarget = new BlockPos(
            this.navigationTarget.getX(),
            Math.min(this.mob.getWorld().getTopY() -1, Math.max(this.mob.getWorld().getBottomY(), this.navigationTarget.getY())),
            this.navigationTarget.getZ()
        );

        com.example.modid.ExampleMod.LOGGER.info("InvestigateSoundGoal: {} starting path to {} (sound source: {}, PI: {}, radius: {})",
                this.mob.getUuidAsString(), this.navigationTarget, this.soundLocation, pi, radius);
        this.mob.getNavigation().startMovingTo(this.navigationTarget.getX(), this.navigationTarget.getY(), this.navigationTarget.getZ(), this.speed);
        this.searchTicks = 0; // Reset search ticks
    }

    @Override
    public void tick() {
        if (this.navigationTarget == null) return;

        if (this.mob.getBlockPos().isWithinDistance(this.navigationTarget, Math.sqrt(ARRIVAL_THRESHOLD_SQUARED))) {
            if (searchTicks == 0) { // Just arrived
                com.example.modid.ExampleMod.LOGGER.info("InvestigateSoundGoal: {} arrived at investigation area {}. Starting search phase.", this.mob.getUuidAsString(), this.navigationTarget);
            }
            searchTicks++;
            this.mob.getLookControl().lookAt(this.navigationTarget.getX(), this.navigationTarget.getY(), this.navigationTarget.getZ()); // Look at the center of search area
            if (searchTicks >= SEARCH_DURATION_TICKS) {
                com.example.modid.ExampleMod.LOGGER.info("InvestigateSoundGoal: {} finished searching at {}.", this.mob.getUuidAsString(), this.navigationTarget);
                this.soundLocation = null; // Mark investigation as complete for this sound
                this.mob.clearCurrentSoundSource(); // Clear it in the entity
            }
        } else {
            // If navigation target changed by something else, stop.
            if (this.mob.getNavigation().getTargetPos() == null || !this.mob.getNavigation().getTargetPos().equals(this.navigationTarget)) {
                // This can happen if another goal with higher priority interrupts.
                // com.example.modid.ExampleMod.LOGGER.debug("InvestigateSoundGoal: Navigation target changed for {}, stopping.", this.mob.getUuidAsString());
                // this.soundLocation = null; // Stop this goal
            }
        }
    }

    @Override
    public void stop() {
        com.example.modid.ExampleMod.LOGGER.info("InvestigateSoundGoal: Stopped for {}. Nav target was {}. Current sound source in mob: {}",
                this.mob.getUuidAsString(), this.navigationTarget, this.mob.getCurrentSoundSourceLocation());
        
        // Only stop navigation if this goal was the one pathing to its specific navigationTarget
        if (this.navigationTarget != null && this.mob.getNavigation().getTargetPos() != null && this.mob.getNavigation().getTargetPos().equals(this.navigationTarget)) {
            this.mob.getNavigation().stop();
        }
        
        // If the goal is stopping and the mob's current sound source is the one we were investigating, clear it.
        // This ensures that if the goal is interrupted, the mob doesn't immediately try to re-investigate the same sound unless processSound is called again.
        if (this.soundLocation != null && this.soundLocation.equals(this.mob.getCurrentSoundSourceLocation())) {
            // this.mob.clearCurrentSoundSource(); // This might be too aggressive, let searchTicks complete or new sound override
        }
        this.navigationTarget = null;
        this.searchTicks = 0;
    }
}
