package com.agricraft.agricraft.compat.starfield;

import com.agricraft.agricraft.api.compat.NativeCropGeneRules;
import com.agricraft.agricraft.api.genetic.AgriGenome;
import com.agricraft.agricraft.api.stat.AgriStat;
import com.agricraft.agricraft.api.stat.AgriStatRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;

/** Safe optional facade: reads an existing genome, never repairs or creates native progress. */
public final class StarfieldGeneEffects {
    private StarfieldGeneEffects() {}

    private static double trait(ServerLevel level, BlockPos root, AgriStat stat) {
        if (!StarfieldCompat.isActive()) return 0;
        var state = level.getBlockState(root);
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)
                && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)
                == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER) root = root.below();
        var data = StarfieldPlantData.existing(level);
        if (data == null || !data.writable(GlobalPos.of(level.dimension(), root))) return 0;
        var entry = data.get(GlobalPos.of(level.dimension(), root));
        if (entry.isEmpty() || !entry.get().form().equals(BuiltInRegistries.BLOCK.getKey(level.getBlockState(root).getBlock()))
                || StarfieldCompat.species(entry.get().type()).isEmpty()) return 0;
        var genome = AgriGenome.fromNBT(entry.get().genome());
        return NativeCropGeneRules.normalized(genome.getStatGene(stat).getTrait(), stat.getMin(), stat.getMax());
    }

    public static int[] phases(ServerLevel level, BlockPos root, int[] nativeDays) {
        return NativeCropGeneRules.phases(nativeDays, trait(level, root, AgriStatRegistry.getInstance().growthStat()));
    }

    public static int regrow(ServerLevel level, BlockPos root, int nativeDays) {
        return NativeCropGeneRules.duration(nativeDays, trait(level, root, AgriStatRegistry.getInstance().growthStat()));
    }

    static double gain(ServerLevel level, BlockPos root) {
        return trait(level, root, AgriStatRegistry.getInstance().gainStat());
    }

    static double strength(ServerLevel level, BlockPos root) {
        return trait(level, root, AgriStatRegistry.getInstance().strengthStat());
    }

    static int resistanceDays(ServerLevel level, BlockPos root) {
        return NativeCropGeneRules.resistanceDays(trait(level, root, AgriStatRegistry.getInstance().resistanceStat()));
    }
}
