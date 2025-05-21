package com.example.modid.manager;

import net.minecraft.util.math.BlockPos;

public record ScentNode(BlockPos position, long creationTime, float initialIntensity) {
    // creationTime will be world.getTime()
}
