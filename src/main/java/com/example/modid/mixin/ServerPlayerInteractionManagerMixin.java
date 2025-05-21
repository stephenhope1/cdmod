package com.example.modid.mixin;

import com.example.modid.ExampleMod;
import com.example.modid.manager.AlertLevelManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin {

    @Shadow public ServerPlayerEntity player;

import com.example.modid.manager.CreatureManager; // Added import
import com.example.modid.entity.DeepDwellerEntity; // Added import
import net.minecraft.util.math.Vec3d; // Added import

    private static final double BLOCK_BREAK_ALERT_INTENSITY = 5.0; // For AlertLevelManager
    private static final float BLOCK_BREAK_SOUND_PI_INTENSITY = 3.0f; // For DeepDwellerEntity PI system

    @Inject(method = "tryBreakBlock", at = @At(value = "RETURN", ordinal = 0), cancellable = true)
    private void onTryBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && this.player != null && !this.player.getWorld().isClient()) {
            // --- Alert Level Increase (existing logic) ---
            AlertLevelManager alertManager = ExampleMod.getAlertLevelManager();
            if (alertManager != null) {
                alertManager.increaseAlertLevel(this.player, BLOCK_BREAK_ALERT_INTENSITY);
            }

            // --- Deep Dweller Sound Perception ---
            CreatureManager creatureManager = ExampleMod.getCreatureManager();
            if (creatureManager != null && creatureManager.isCreatureSpawned()) {
                DeepDwellerEntity deepDweller = creatureManager.getSpawnedCreature();
                // Check if creature is in the same world and loaded
                if (deepDweller != null && deepDweller.getWorld() == this.player.getWorld() && deepDweller.isAlive()) {
                    Vec3d soundLocation = Vec3d.ofCenter(pos);
                    deepDweller.processSound(this.player, soundLocation, BLOCK_BREAK_SOUND_PI_INTENSITY);
                    // ExampleMod.LOGGER.info("Block broken sound event processed for DeepDweller by {}", this.player.getName().getString());
                }
            }
        }
    }
}
