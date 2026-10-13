package com.agricraft.agricraft.api.compat;

import com.agricraft.agricraft.api.genetic.AgriGenome;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.Optional;

/** Optional native seeds can be inspected without converting their item or planting engine. */
public final class ExternalSeedSupport {
    private static Provider provider;
    private ExternalSeedSupport() {}
    public interface Provider {
        boolean accepts(ItemStack stack, Level level);
        ItemStack prepare(ItemStack stack, Level level);
        Optional<AgriGenome> genome(ItemStack stack, Level level);
        default boolean mayUseAnalyzer(net.minecraft.world.entity.player.Player player, net.minecraft.core.BlockPos pos) { return true; }
    }
    public static void register(Provider value) { provider = value; }
    public static boolean accepts(ItemStack stack, Level level) { return provider != null && provider.accepts(stack, level); }
    public static ItemStack prepare(ItemStack stack, Level level) { return provider == null ? ItemStack.EMPTY : provider.prepare(stack, level); }
    public static Optional<AgriGenome> genome(ItemStack stack, Level level) { return provider == null ? Optional.empty() : provider.genome(stack, level); }
    public static boolean mayUseAnalyzer(net.minecraft.world.entity.player.Player player, net.minecraft.core.BlockPos pos) {
        return provider == null || provider.mayUseAnalyzer(player, pos);
    }
}
