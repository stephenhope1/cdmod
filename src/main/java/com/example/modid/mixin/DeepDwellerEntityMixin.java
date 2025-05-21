package com.example.modid.mixin;

import com.example.modid.ExampleMod;
import com.example.modid.entity.DeepDwellerEntity;
import com.example.modid.manager.CreatureManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeepDwellerEntity.class)
public abstract class DeepDwellerEntityMixin extends Entity {

    // Constructor needed for Mixin inheritance, even if abstract
    public DeepDwellerEntityMixin(net.minecraft.entity.EntityType<?> type, net.minecraft.world.World world) {
        super(type, world);
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void onRemoveMixin(Entity.RemovalReason reason, CallbackInfo ci) {
        if (!this.getWorld().isClient()) { // Only run on server
            DeepDwellerEntity thisEntity = (DeepDwellerEntity)(Object)this;
            CreatureManager creatureManager = ExampleMod.getCreatureManager(); // Assumes getter exists
            if (creatureManager != null) {
                // Notify the manager that this specific instance is being removed
                ExampleMod.LOGGER.info("DeepDwellerEntityMixin: Intercepted remove() for entity {} due to {}", thisEntity.getUuidAsString(), reason.toString());
                creatureManager.onCreatureRemoved(thisEntity);
            }
        }
    }

    // It might also be useful to inject into onDeath, but remove() should cover most cases including despawning.
    // @Inject(method = "onDeath", at = @At("HEAD"))
    // private void onDeathMixin(DamageSource source, CallbackInfo ci) {
    //     if (!this.getWorld().isClient()) {
    //         // ... similar logic ...
    //     }
    // }
}
