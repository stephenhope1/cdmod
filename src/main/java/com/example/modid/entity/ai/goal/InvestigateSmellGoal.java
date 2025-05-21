package com.example.modid.entity.ai.goal;

import com.example.modid.ExampleMod;
import com.example.modid.entity.DeepDwellerEntity;
import com.example.modid.manager.ScentManager;
import com.example.modid.manager.ScentNode;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class InvestigateSmellGoal extends Goal {
    private final DeepDwellerEntity mob;
    private final double speed;
    private ScentNode currentTargetNode; // The specific node this goal instance is targeting
    private BlockPos navigationTargetPos; // The BlockPos to navigate to

    private static final int ARRIVAL_THRESHOLD_SQUARED = 4; // Within 2 blocks
    private static final float LOSE_TRAIL_CHANCE = 0.1f; // 10% chance to lose trail upon reaching a node
    private static final int MAX_CONSECUTIVE_NODE_FOLLOWS = 5; // Max nodes to follow in one "goal session" before re-evaluating
    private int nodesFollowedThisSession = 0;

    public InvestigateSmellGoal(DeepDwellerEntity mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (!this.mob.isInvestigatingSmell() || this.mob.getCurrentSmellTargetNode() == null) {
            return false;
        }
        // Don't start if already very close to the target node
        ScentNode targetNodeFromEntity = this.mob.getCurrentSmellTargetNode();
        if (targetNodeFromEntity != null && this.mob.getBlockPos().isWithinDistance(targetNodeFromEntity.position(), Math.sqrt(ARRIVAL_THRESHOLD_SQUARED) + 1)) {
            return false; // Already there or very close
        }
        // com.example.modid.ExampleMod.LOGGER.info("InvestigateSmellGoal: Can start for {}, target node: {}", this.mob.getUuidAsString(), targetNodeFromEntity.position());
        return true;
    }

    @Override
    public boolean shouldContinue() {
        if (!this.mob.isInvestigatingSmell() || this.currentTargetNode == null) {
            // com.example.modid.ExampleMod.LOGGER.info("InvestigateSmellGoal: Should NOT continue for {} (not investigating or currentTargetNode is null)", this.mob.getUuidAsString());
            return false;
        }
        // If the entity picked a new smell target (e.g. stronger smell), this instance of the goal should stop.
        if (this.mob.getCurrentSmellTargetNode() != null && !this.mob.getCurrentSmellTargetNode().equals(this.currentTargetNode)) {
            // com.example.modid.ExampleMod.LOGGER.info("InvestigateSmellGoal: Entity picked a new smell target for {}, stopping this instance.", this.mob.getUuidAsString());
            return false;
        }
        if (nodesFollowedThisSession >= MAX_CONSECUTIVE_NODE_FOLLOWS) {
            // com.example.modid.ExampleMod.LOGGER.info("InvestigateSmellGoal: Reached max nodes ({}) this session for {}.", MAX_CONSECUTIVE_NODE_FOLLOWS, this.mob.getUuidAsString());
            return false;
        }

        return !this.mob.getNavigation().isIdle() || this.mob.getBlockPos().getSquaredDistance(this.navigationTargetPos) > ARRIVAL_THRESHOLD_SQUARED;
    }

    @Override
    public void start() {
        this.currentTargetNode = this.mob.getCurrentSmellTargetNode(); // Get the node selected by the entity's tick logic
        if (this.currentTargetNode == null) {
            // com.example.modid.ExampleMod.LOGGER.warn("InvestigateSmellGoal: Started with null currentTargetNode for {}. This shouldn't happen.", this.mob.getUuidAsString());
            return; 
        }
        this.navigationTargetPos = this.currentTargetNode.position();
        this.nodesFollowedThisSession = 0;
        
        com.example.modid.ExampleMod.LOGGER.info("InvestigateSmellGoal: {} starting path to ScentNode at {} (Player UUID: {})",
                this.mob.getUuidAsString(), this.navigationTargetPos, this.mob.getTargetPlayerForSmellUuid());
        this.mob.getNavigation().startMovingTo(this.navigationTargetPos.getX(), this.navigationTargetPos.getY(), this.navigationTargetPos.getZ(), this.speed);
    }

    @Override
    public void tick() {
        if (this.navigationTargetPos == null || this.currentTargetNode == null) return;

        this.mob.getLookControl().lookAt(Vec3d.ofCenter(this.navigationTargetPos));

        if (this.mob.getBlockPos().isWithinDistance(this.navigationTargetPos, Math.sqrt(ARRIVAL_THRESHOLD_SQUARED))) {
            com.example.modid.ExampleMod.LOGGER.info("InvestigateSmellGoal: {} reached ScentNode at {}.", this.mob.getUuidAsString(), this.navigationTargetPos);
            nodesFollowedThisSession++;

            if (this.mob.getRandom().nextFloat() < LOSE_TRAIL_CHANCE) {
                com.example.modid.ExampleMod.LOGGER.info("InvestigateSmellGoal: {} 'lost the trail' by chance at {}.", this.mob.getUuidAsString(), this.navigationTargetPos);
                this.mob.clearCurrentSmellTarget(); // This will cause shouldContinue to return false
                return;
            }
            
            // Try to find the next node in the trail
            UUID targetPlayerId = this.mob.getTargetPlayerForSmellUuid();
            if (targetPlayerId == null) {
                this.mob.clearCurrentSmellTarget(); return;
            }

            ScentManager scentManager = ExampleMod.getScentManager();
            if (scentManager == null) {
                this.mob.clearCurrentSmellTarget(); return;
            }

            List<ScentNode> trail = scentManager.getScentTrail(targetPlayerId);
            int currentNodeIndex = -1;
            for(int i=0; i < trail.size(); i++) {
                if(trail.get(i).equals(this.currentTargetNode)) {
                    currentNodeIndex = i;
                    break;
                }
            }

            ScentNode nextBestNode = null;
            float highestNextIntensity = 0;

            // Search "forward" in the trail (towards more recent nodes, which are at the start of the list)
            // but only consider nodes close to the current one.
            // The entity's main tick logic will handle broader re-evaluation. This goal is for local continuation.
            if (currentNodeIndex > 0) { // If there are more recent nodes
                 // Check a few nodes before the current one in the list (more recent)
                for (int i = Math.max(0, currentNodeIndex - (ScentManager.MAX_TRAIL_LENGTH/10) ); i < currentNodeIndex; i++) { // Check ~10% of trail length as next step
                     if (i % DeepDwellerEntity.SCENT_NODE_SAMPLE_RATE == 0) { // Adhere to sampling
                        ScentNode candidateNode = trail.get(i);
                        // Ensure it's reasonably close to the current node to be a "next step"
                        if (candidateNode.position().getManhattanDistance(this.currentTargetNode.position()) < 15) {
                            long ageSeconds = (this.mob.getWorld().getTime() - candidateNode.creationTime()) / 20;
                            float currentIntensity = candidateNode.initialIntensity() - (ageSeconds * ScentManager.SCENT_DECAY_PER_SECOND);
                            if (currentIntensity > DeepDwellerEntity.SMELL_PI_THRESHOLD && currentIntensity > highestNextIntensity) {
                                highestNextIntensity = currentIntensity;
                                nextBestNode = candidateNode;
                            }
                        }
                    }
                }
            }

            if (nextBestNode != null) {
                com.example.modid.ExampleMod.LOGGER.info("InvestigateSmellGoal: {} found next ScentNode at {} (Intensity: {}).", this.mob.getUuidAsString(), nextBestNode.position(), highestNextIntensity);
                this.currentTargetNode = nextBestNode; // Update internal target for this goal instance
                this.navigationTargetPos = nextBestNode.position();
                this.mob.getNavigation().startMovingTo(this.navigationTargetPos.getX(), this.navigationTargetPos.getY(), this.navigationTargetPos.getZ(), this.speed);
            } else {
                com.example.modid.ExampleMod.LOGGER.info("InvestigateSmellGoal: {} found no suitable next ScentNode for player {}. Investigation ends.", this.mob.getUuidAsString(), targetPlayerId);
                this.mob.clearCurrentSmellTarget(); // No more nodes to follow locally
            }
        }
    }

    @Override
    public void stop() {
        com.example.modid.ExampleMod.LOGGER.info("InvestigateSmellGoal: Stopped for {}. Target node was {}. Nodes followed: {}",
                this.mob.getUuidAsString(), this.currentTargetNode != null ? this.currentTargetNode.position() : "null", nodesFollowedThisSession);
        
        if (this.navigationTargetPos != null && this.mob.getNavigation().getTargetPos() != null && this.mob.getNavigation().getTargetPos().equals(this.navigationTargetPos)) {
             this.mob.getNavigation().stop();
        }
        
        // If the goal is stopping for any reason other than finding a new target via entity's main tick,
        // ensure the mob clears its smell investigation state.
        // The entity's main tick logic might re-assign a smell target if conditions are met.
        if (this.mob.getCurrentSmellTargetNode() != null && this.mob.getCurrentSmellTargetNode().equals(this.currentTargetNode)) {
             this.mob.clearCurrentSmellTarget();
        }
        this.currentTargetNode = null;
        this.navigationTargetPos = null;
        this.nodesFollowedThisSession = 0;
    }
}
