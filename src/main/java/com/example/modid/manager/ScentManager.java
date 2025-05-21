package com.example.modid.manager;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import com.example.modid.ExampleMod; // For logging

public class ScentManager {

    // Configuration for scent trails
    public static final int MAX_TRAIL_LENGTH = 100; // Max number of nodes per player
    public static final float DEFAULT_SCENT_INTENSITY = 10.0f; // Initial intensity of a new scent node
    public static final long SCENT_NODE_LIFESPAN_TICKS = 20 * 60 * 5; // 5 minutes for a node to "exist" before decay makes it negligible
    public static final float SCENT_DECAY_PER_SECOND = 0.1f; // Intensity lost per second
    public static final float WATER_SCENT_INTENSITY_MULTIPLIER = 0.1f; // Scent is much weaker if player is wet

    private final Map<UUID, List<ScentNode>> playerScentTrails;

    public ScentManager() {
        this.playerScentTrails = new ConcurrentHashMap<>();
    }

    public void addScentNode(PlayerEntity player, BlockPos position) {
        if (player == null || position == null || player.getWorld().isClient()) {
            return;
        }

        UUID playerId = player.getUuid();
        playerScentTrails.putIfAbsent(playerId, new CopyOnWriteArrayList<>());
        List<ScentNode> trail = playerScentTrails.get(playerId);

        float intensity = DEFAULT_SCENT_INTENSITY;
        if (player.isWet()) { // Check if player is wet (in water, rain)
            intensity *= WATER_SCENT_INTENSITY_MULTIPLIER;
        }
        
        // Optional: Add stinky inventory check here later if needed
        // float stinkyMultiplier = getStinkyInventoryMultiplier(player);
        // intensity *= stinkyMultiplier;


        ScentNode newNode = new ScentNode(position, player.getWorld().getTime(), intensity);
        trail.add(0, newNode); // Add to the beginning (most recent)

        // Maintain trail length
        while (trail.size() > MAX_TRAIL_LENGTH) {
            trail.remove(trail.size() - 1); // Remove the oldest node
        }
        // ExampleMod.LOGGER.debug("Added scent node for player {} at {}. Trail size: {}. Intensity: {}", player.getName().getString(), position, trail.size(), intensity);
    }

    public List<ScentNode> getScentTrail(UUID playerId) {
        return playerScentTrails.getOrDefault(playerId, new CopyOnWriteArrayList<>());
    }

    // This method can be called periodically to clean up very old trails from disconnected players
    public void cleanupOldTrails(long currentTick) {
        // For now, trails are capped by length.
        // If we stored by time, we could remove old trails entirely.
        // This could also be used to remove nodes that are extremely old even within a capped list.
        playerScentTrails.forEach((uuid, trail) -> {
            trail.removeIf(node -> (currentTick - node.creationTime()) > SCENT_NODE_LIFESPAN_TICKS * 2); // Remove very old nodes
            if (trail.isEmpty()) {
                // Consider removing player from map if trail is empty and player offline for long
            }
        });
    }

    // Placeholder for stinky inventory check
    // private float getStinkyInventoryMultiplier(PlayerEntity player) {
    //    float multiplier = 1.0f;
    //    // Iterate player's inventory
    //    // If specific items (raw meat, rotten flesh, etc.) are found, increase multiplier
    //    // Example: if (player.getInventory().containsAny(Set.of(Items.ROTTEN_FLESH, Items.BEEF))) {
    //    //     multiplier *= 1.5f;
    //    // }
    //    return multiplier;
    // }
}
