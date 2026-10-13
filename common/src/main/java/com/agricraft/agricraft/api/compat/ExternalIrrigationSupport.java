package com.agricraft.agricraft.api.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Optional crops can accept paid irrigation without receiving an extra growth tick. */
public final class ExternalIrrigationSupport {
    public enum Result { UNHANDLED, CONTINUE, STOP }
    @FunctionalInterface
    public interface Provider {
        Result irrigate(Level level, BlockPos pos, BlockState state);
    }
    private static Provider provider;
    private ExternalIrrigationSupport() {}
    public static void register(Provider value) { provider = value; }
    /** Called only after the sprinkler's existing water budget check, within its bounded sweep. */
    public static Result irrigate(Level level, BlockPos pos, BlockState state) {
        return provider == null ? Result.UNHANDLED : provider.irrigate(level, pos, state);
    }
}
