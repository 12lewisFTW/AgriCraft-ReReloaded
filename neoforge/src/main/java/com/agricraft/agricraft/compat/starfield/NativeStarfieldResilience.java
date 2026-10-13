package com.agricraft.agricraft.compat.starfield;

import com.agricraft.agricraft.api.compat.NativeCropGeneRules;
import com.stardew.craft.api.v1.agriculture.StardewAgricultureDataApi;
import com.stardew.craft.api.v1.internal.crop.StardewCropRuntimeRegistry;
import com.stardew.craft.block.crop.StardewCropBlock;
import com.stardew.craft.api.v1.agriculture.StardewCropDailyContext;
import com.stardew.craft.farming.SeasonLocationRules;
import com.stardew.craft.farm.OfflineFarmCatchUp;
import com.stardew.craft.time.StardewTimeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Narrow soil/season gates. Native code still owns progress, water settlement and death. */
final class NativeStarfieldResilience {
    private static final List<String> SEASONS = List.of("spring", "summer", "fall", "winter");
    private static final Method SEASON;
    private static final ThreadLocal<StardewCropDailyContext> ACTIVE_DAY;
    static {
        try {
            SEASON = StardewCropBlock.class.getDeclaredMethod("isInSeason", Level.class); SEASON.setAccessible(true);
            Field active = StardewCropRuntimeRegistry.class.getDeclaredField("ACTIVE_DAY"); active.setAccessible(true);
            @SuppressWarnings("unchecked") var context = (ThreadLocal<StardewCropDailyContext>) active.get(null);
            ACTIVE_DAY = context;
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException("Verified native season accessor missing", failure); }
    }

    static int day() {
        var context = StardewCropRuntimeRegistry.activeDay();
        return context == null ? OfflineFarmCatchUp.computeAbsoluteDay() : context.absoluteDay();
    }

    static boolean soil(ServerLevel level, BlockPos root, boolean infertile, BooleanSupplier original) {
        boolean advanced = original.getAsBoolean();
        return advanced || infertile && NativeCropGeneRules.recoverSoilSkip(day(), StarfieldGeneEffects.strength(level, root));
    }

    static boolean paused(ServerLevel level, BlockPos root, BlockState state) {
        var context = StardewCropRuntimeRegistry.activeDay();
        int season = context == null ? StardewTimeManager.get().getCurrentSeason() : context.season();
        return grace(level, root, state, day(), season, context != null && context.offlineCatchUp()) > 0;
    }

    static int grace(ServerLevel level, BlockPos root, BlockState state, int day, int season, boolean offline) {
        int days = StarfieldGeneEffects.resistanceDays(level, root);
        int remaining = NativeCropGeneRules.graceRemaining(day, days);
        if (remaining == 0 || level.dimension() != com.stardew.craft.core.ModDimensions.STARDEW_VALLEY
                || !(state.getBlock() instanceof StardewCropBlock block)
                || !com.stardew.craft.block.terrain.TerrainSoils.cropSupport(level.getBlockState(root.below()))
                || SeasonLocationRules.seedsIgnoreSeasonsHere(level, root)) return 0;
        try {
            var data = StardewAgricultureDataApi.crop(level, root, state);
            boolean inSeason = offline && data != null ? data.seasons().contains(SEASONS.get(season))
                    : allowed(block, level, day, season);
            if (inSeason) return 0;
            int previousSeason = Math.floorMod(season - 1, 4);
            boolean wasAllowed = offline && data != null ? data.seasons().contains(SEASONS.get(previousSeason))
                    : allowed(block, level, day, previousSeason);
            return wasAllowed ? remaining : 0;
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException("Cannot read native season rule", failure); }
    }

    /** Query the native rule for a calendar season without changing the world clock. */
    private static boolean allowed(StardewCropBlock block, ServerLevel level, int day, int season) throws ReflectiveOperationException {
        var previous = ACTIVE_DAY.get();
        ACTIVE_DAY.set(new StardewCropDailyContext(false, season, false, day, false));
        try { return (boolean) SEASON.invoke(block, level); }
        finally { if (previous == null) ACTIVE_DAY.remove(); else ACTIVE_DAY.set(previous); }
    }

    static Collection<BlockPos> giantEligible(ServerLevel level, Collection<BlockPos> roots, int day, boolean offline) {
        int season = Math.floorMod(Math.floorDiv(day - 1, 28), 4);
        return roots.stream().filter(root -> !level.hasChunkAt(root)
                || grace(level, root, level.getBlockState(root), day, season, offline) == 0).toList();
    }
}
