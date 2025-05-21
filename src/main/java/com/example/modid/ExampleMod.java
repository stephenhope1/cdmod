package com.example.modid;

import com.example.modid.manager.AlertLevelManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleMod implements ModInitializer {
    public static final String MOD_ID = "modid";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static AlertLevelManager alertLevelManager;

    // --- Custom Sound Events ---
    // General Ambient Sounds
    public static final Identifier CAVE_WHISTLES_ID = new Identifier(MOD_ID, "ambient.cave_whistles");
    public static final SoundEvent CAVE_WHISTLES_EVENT = SoundEvent.of(CAVE_WHISTLES_ID);

    public static final Identifier WHOOPS_ID = new Identifier(MOD_ID, "ambient.whoops");
    public static final SoundEvent WHOOPS_EVENT = SoundEvent.of(WHOOPS_ID);

    public static final Identifier PEBBLES_DROPPING_ID = new Identifier(MOD_ID, "ambient.pebbles_dropping");
    public static final SoundEvent PEBBLES_DROPPING_EVENT = SoundEvent.of(PEBBLES_DROPPING_ID);

    public static final Identifier DISTANT_CLICKING_ID = new Identifier(MOD_ID, "ambient.distant_clicking");
    public static final SoundEvent DISTANT_CLICKING_EVENT = SoundEvent.of(DISTANT_CLICKING_ID);

    // Alert-Driven Triggered Sounds
    public static final Identifier DISTANT_SHRIEKING_ID = new Identifier(MOD_ID, "triggered.distant_shrieking");
    public static final SoundEvent DISTANT_SHRIEKING_EVENT = SoundEvent.of(DISTANT_SHRIEKING_ID);

import com.example.modid.entity.DeepDwellerEntity; // Added import
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry; // Added import
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder; // Added import
import net.minecraft.entity.EntityDimensions; // Added import
import net.minecraft.entity.EntityType; // Added import
import net.minecraft.entity.SpawnGroup; // Added import


    public static final Identifier HEAVY_FOOTSTEPS_ID = new Identifier(MOD_ID, "triggered.heavy_footsteps");
    public static final SoundEvent HEAVY_FOOTSTEPS_EVENT = SoundEvent.of(HEAVY_FOOTSTEPS_ID);

    // Phase 2 Ambience Sound
    public static final Identifier DWELLER_PRESENCE_CLICKS_ID = new Identifier(MOD_ID, "ambient.dweller_presence_clicks");
    public static final SoundEvent DWELLER_PRESENCE_CLICKS_EVENT = SoundEvent.of(DWELLER_PRESENCE_CLICKS_ID);

    public static final Identifier DWELLER_STOMP_ID = new Identifier(MOD_ID, "mob.dweller_stomp"); // New stomp sound
    public static final SoundEvent DWELLER_STOMP_EVENT = SoundEvent.of(DWELLER_STOMP_ID);
    // --- End Custom Sound Events ---

    // --- Entity Types ---
import com.example.modid.manager.CreatureManager; // Added import

    public static EntityType<DeepDwellerEntity> DEEP_DWELLER_ENTITY_TYPE;
import com.example.modid.manager.ScentManager; // Added import

    // --- End Entity Types ---

    private static CreatureManager creatureManager; 
    private static ScentManager scentManager; // Added field

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Example Mod - Hostile Depths");
        alertLevelManager = new AlertLevelManager();
        creatureManager = new CreatureManager();
        scentManager = new ScentManager(); // Instantiated

        // Register Entity Types
        DEEP_DWELLER_ENTITY_TYPE = Registry.register(
                Registries.ENTITY_TYPE,
                new Identifier(MOD_ID, DeepDwellerEntity.ID),
                FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, DeepDwellerEntity::new)
                        .dimensions(EntityDimensions.fixed(0.7f, 1.9f)) // Slightly larger than a player
                        .trackRangeBlocks(128) // Ensure it's tracked from far away for AI
                        .build()
        );
        FabricDefaultAttributeRegistry.register(DEEP_DWELLER_ENTITY_TYPE, DeepDwellerEntity.createDeepDwellerAttributes());
        // ---

        // Register sound events
        Registry.register(Registries.SOUND_EVENT, CAVE_WHISTLES_ID, CAVE_WHISTLES_EVENT);
        Registry.register(Registries.SOUND_EVENT, WHOOPS_ID, WHOOPS_EVENT);
        Registry.register(Registries.SOUND_EVENT, PEBBLES_DROPPING_ID, PEBBLES_DROPPING_EVENT);
        Registry.register(Registries.SOUND_EVENT, DISTANT_CLICKING_ID, DISTANT_CLICKING_EVENT);
        Registry.register(Registries.SOUND_EVENT, DISTANT_SHRIEKING_ID, DISTANT_SHRIEKING_EVENT);
        Registry.register(Registries.SOUND_EVENT, HEAVY_FOOTSTEPS_ID, HEAVY_FOOTSTEPS_EVENT);
        Registry.register(Registries.SOUND_EVENT, DWELLER_PRESENCE_CLICKS_ID, DWELLER_PRESENCE_CLICKS_EVENT); 
        Registry.register(Registries.SOUND_EVENT, DWELLER_STOMP_ID, DWELLER_STOMP_EVENT); // Register new stomp sound

        // Register server tick event for alert level decay and ambient sounds
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
    }

    public static AlertLevelManager getAlertLevelManager() {
        return alertLevelManager;
    }

    public static CreatureManager getCreatureManager() { // Added getter
        return creatureManager;
    }

    public static ScentManager getScentManager() { // Added getter
        return scentManager;
    }

    private void onServerTick(MinecraftServer server) {
        int tickCount = server.getTickCount();

        // Process per-player updates at different frequencies
        if (alertLevelManager != null) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (player != null) {
                    // Decay updates less frequently
                    if (tickCount % 20 == 0) { // Every 20 ticks (1 second)
                        alertLevelManager.updateAlertLevelDecay(player);
                        // Creature spawn attempts also happen on this slower tick, per player
                        if (player instanceof ServerPlayerEntity && !player.getWorld().isClient()) {
                             creatureManager.trySpawnCreature((net.minecraft.server.world.ServerWorld) player.getWorld(), (ServerPlayerEntity) player, alertLevelManager);
                        }
                    }

                    // Ambient sounds and torch flickering checks more frequently
                    if (tickCount % 10 == 0) { // Every 10 ticks (0.5 seconds)
                        alertLevelManager.playAmbientSounds(player);
                        alertLevelManager.handleTorchFlicker(player); // This checks for torches and might turn them off
                    }
                    
                    // For debugging:
                    // if (tickCount % 20 == 0) {
                    //    LOGGER.info("Player {}: Alert Level {} | Creature Spawned: {}", 
                    //                   player.getName().getString(), 
                    //                   alertLevelManager.getAlertLevel(player),
                    //                   creatureManager.isCreatureSpawned());
                    // }
                }
            }

            // Update flickering torches state (restoration part) every tick for responsiveness
            if (alertLevelManager != null) { 
                alertLevelManager.updateFlickeringTorches(server);
            }
            // Periodically cleanup old scent trails from ScentManager
            if (scentManager != null && server.getTickCount() % (20 * 60) == 0) { // Every minute
                scentManager.cleanupOldTrails(server.getTickCount());
            }
        }
    }
}
