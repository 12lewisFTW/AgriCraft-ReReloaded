package com.agricraft.agricraft.api.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.function.Consumer;

/** Optional native crops keep their own harvest context and state transitions. */
public final class ExternalHarvestSupport {
    public enum Availability { UNHANDLED, NOT_READY, READY, UNSUPPORTED }
    public enum Result { UNHANDLED, REJECTED, HARVESTED }
    public interface Provider {
        Availability inspect(Level level, BlockPos pos);
        Result harvest(Level level, BlockPos pos, Consumer<ItemStack> output);
    }
    private static Provider provider;
    private ExternalHarvestSupport() {}
    public static void register(Provider value) { provider = value; }
    public static Availability inspect(Level level, BlockPos pos) {
        return provider == null ? Availability.UNHANDLED : provider.inspect(level, pos);
    }
    /** The sink must accept each complete stack; a throwing sink is not retryable. */
    public static Result harvest(Level level, BlockPos pos, Consumer<ItemStack> output) {
        return provider == null ? Result.UNHANDLED : provider.harvest(level, pos, output);
    }
}
