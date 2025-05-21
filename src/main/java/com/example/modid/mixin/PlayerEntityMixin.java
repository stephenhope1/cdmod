package com.example.modid.mixin;

import com.example.modid.ExampleMod;
import com.example.modid.manager.AlertLevelManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {

    protected PlayerEntityMixin(World world) {
        super(null, world); // This is a bit of a hack for the constructor, but PlayerEntity is abstract
    }

import com.example.modid.manager.CreatureManager; // Added
import com.example.modid.entity.DeepDwellerEntity; // Added
import net.minecraft.util.math.Vec3d; // Added

    // For AlertLevelManager
    private static final double FALL_DAMAGE_ALERT_INTENSITY_PER_BLOCK = 0.5; 
    private static final double JUMP_ALERT_INTENSITY = 0.5;
    
    // For DeepDwellerEntity PI system
import net.minecraft.util.math.BlockPos; // Added import
import com.example.modid.manager.ScentManager; // Added import
import org.spongepowered.asm.mixin.Unique; // For unique fields
import org.spongepowered.asm.mixin.Shadow; // If we needed to shadow a field/method

    // For DeepDwellerEntity PI system
    private static final float FALL_DAMAGE_PI_INTENSITY_PER_BLOCK = 0.25f; 
    private static final float JUMP_PI_INTENSITY = 0.2f;

    @Unique
    private BlockPos modid$lastScentBlockPos; // Added field for tracking last position for scent

    @Inject(method = "tick", at = @At("TAIL"))
    private void onPlayerTick(CallbackInfo ci) {
        if ((Object)this instanceof ServerPlayerEntity && !this.getWorld().isClient) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity)(Object)this;
            BlockPos currentBlockPos = serverPlayer.getBlockPos();

            if (modid$lastScentBlockPos == null) { // Initialize on first tick
                modid$lastScentBlockPos = currentBlockPos;
            }

            if (!currentBlockPos.equals(modid$lastScentBlockPos)) {
                // Player has moved to a new block
                ScentManager scentManager = ExampleMod.getScentManager();
                if (scentManager != null) {
                    // Add scent node at the block they just left
                    scentManager.addScentNode(serverPlayer, modid$lastScentBlockPos.toImmutable()); 
                }
                modid$lastScentBlockPos = currentBlockPos.toImmutable(); // Update last known position
            }
        }
    }


    @Inject(method = "handleFallDamage", at = @At("TAIL"))
    private void onFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        if (!this.getWorld().isClient && cir.getReturnValue() && (Object)this instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity)(Object)this;
            
            // --- Alert Level Increase (existing logic) ---
            AlertLevelManager alertManager = ExampleMod.getAlertLevelManager();
            if (alertManager != null) {
                double alertIntensity = fallDistance * FALL_DAMAGE_ALERT_INTENSITY_PER_BLOCK;
                if (alertIntensity > 0) {
                    alertManager.increaseAlertLevel(serverPlayer, alertIntensity);
                }
            }

            // --- Deep Dweller Sound Perception ---
            CreatureManager creatureManager = ExampleMod.getCreatureManager();
            if (creatureManager != null && creatureManager.isCreatureSpawned()) {
                DeepDwellerEntity deepDweller = creatureManager.getSpawnedCreature();
                if (deepDweller != null && deepDweller.getWorld() == serverPlayer.getWorld() && deepDweller.isAlive()) {
                    float piIntensity = fallDistance * FALL_DAMAGE_PI_INTENSITY_PER_BLOCK;
                    if (piIntensity > 0) {
                        // Sound location is the player's feet position when landing
                        deepDweller.processSound(serverPlayer, serverPlayer.getPos(), piIntensity);
                        // ExampleMod.LOGGER.info("Fall damage sound event processed for DeepDweller by {}", serverPlayer.getName().getString());
                    }
                }
            }
        }
    }

    @Inject(method = "jump", at = @At("TAIL"))
    private void onJump(CallbackInfo ci) {
        if (!this.getWorld().isClient && (Object)this instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity)(Object)this;

            // --- Alert Level Increase (existing logic) ---
            AlertLevelManager alertManager = ExampleMod.getAlertLevelManager();
            if (alertManager != null) {
                alertManager.increaseAlertLevel(serverPlayer, JUMP_ALERT_INTENSITY);
            }

            // --- Deep Dweller Sound Perception ---
            CreatureManager creatureManager = ExampleMod.getCreatureManager();
            if (creatureManager != null && creatureManager.isCreatureSpawned()) {
                DeepDwellerEntity deepDweller = creatureManager.getSpawnedCreature();
                if (deepDweller != null && deepDweller.getWorld() == serverPlayer.getWorld() && deepDweller.isAlive()) {
                    // Sound location is the player's position when jumping
                    deepDweller.processSound(serverPlayer, serverPlayer.getPos(), JUMP_PI_INTENSITY);
                    // ExampleMod.LOGGER.info("Jump sound event processed for DeepDweller by {}", serverPlayer.getName().getString());
                }
            }
        }
    }
}
