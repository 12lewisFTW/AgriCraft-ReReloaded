package com.agricraft.agricraft.compat.starfield;

import com.agricraft.agricraft.api.compat.ExternalIrrigationSupport;
import com.stardew.craft.block.crop.StardewCropBlock;
import com.stardew.craft.block.terrain.TerrainSoils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Guarded native rules; only wets a reached soil tile, never settles a day or runs randomTick. */
public final class NativeStarfieldIrrigation implements ExternalIrrigationSupport.Provider {
    @Override public ExternalIrrigationSupport.Result irrigate(Level level, BlockPos pos, BlockState state) {
        if (!StarfieldCompat.isActive() || !(level instanceof ServerLevel) || !level.hasChunkAt(pos))
            return ExternalIrrigationSupport.Result.UNHANDLED;
        if (state.getBlock() instanceof StardewCropBlock)
            return StarfieldCompat.species(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock())).isPresent()
                    ? ExternalIrrigationSupport.Result.CONTINUE : ExternalIrrigationSupport.Result.UNHANDLED;
        if (!TerrainSoils.cropSupport(state)) return ExternalIrrigationSupport.Result.UNHANDLED;
        if (!TerrainSoils.blocksSprinklers(state) && state.hasProperty(FarmBlock.MOISTURE)
                && state.getValue(FarmBlock.MOISTURE) < FarmBlock.MAX_MOISTURE)
            level.setBlock(pos, state.setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE), Block.UPDATE_ALL);
        return ExternalIrrigationSupport.Result.STOP;
    }
}
