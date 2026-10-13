package com.agricraft.agricraft.starfield.prototype.save.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** GameTestServer selects a random grid; --spawnPos does not set this location. */
@Mixin(GameTestServer.class)
public abstract class HistoricalPositionMixin {
    @ModifyVariable(method = "startTests", at = @At("STORE"), ordinal = 0)
    private BlockPos historicalPosition(BlockPos original) {
        return Boolean.getBoolean("agricraft.starfield.saveHistoricalTests")
                ? new BlockPos(8701193, -59, 11182101) : original;
    }
}
