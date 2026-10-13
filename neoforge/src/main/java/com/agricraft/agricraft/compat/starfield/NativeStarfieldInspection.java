package com.agricraft.agricraft.compat.starfield;

import com.stardew.craft.block.crop.StardewCropBlock;
import com.stardew.craft.manager.CropGrowthManager;
import com.stardew.craft.manager.FertilizerManager;
import com.stardew.craft.time.StardewTimeManager;
import com.stardew.craft.farming.SeasonLocationRules;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.FarmBlock;
import java.lang.reflect.Method;
import java.util.Optional;

/** Reads native progress and invokes the pinned native schedule calculation, never advances a crop. */
final class NativeStarfieldInspection {
    private static final Method SPEED = method("getTotalSpeedBoost", ServerLevel.class, BlockPos.class, CropGrowthManager.CropGrowthState.class);
    private static final Method SCHEDULE = method("applySpeedGroToPhaseDays", int[].class, float.class);
    private static final Method SEASON = method("isInSeason", net.minecraft.world.level.Level.class);
    private static Method method(String name, Class<?>... parameters) {
        try { var method = StardewCropBlock.class.getDeclaredMethod(name, parameters); method.setAccessible(true); return method; }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException("Verified native inspection accessor missing", failure); }
    }
    static Optional<CompoundTag> read(ServerLevel level, BlockPos position) {
        var plant = StarfieldCompat.inspect(level, position);
        if (plant.isEmpty()) return Optional.empty();
        var root = plant.get().root();
        if (!(level.getBlockState(root).getBlock() instanceof StardewCropBlock)) return NativeStarfieldSpecials.inspection(level, root);
        var block = (StardewCropBlock) level.getBlockState(root).getBlock();
        var entry = NativeStarfieldLifecycle.ensure(level, root); if (entry.isEmpty()) return Optional.empty();
        var growth = CropGrowthManager.get(level).getState(level, root);
        var soil = level.getBlockState(root.below());
        try {
            int remaining = -1, phase = -1, phases = block.getPhaseDaysForDisplay().length;
            boolean regrowing = growth != null && growth.regrowing;
            if (plant.get().mature()) remaining = 0;
            else if (growth != null && growth.sourcePhases) {
                phase = growth.phase;
                int steps;
                if (regrowing) steps = Math.max(0, growth.dayInPhase);
                else {
                    var schedule = StarfieldGeneEffects.phases(level, root, (int[]) SCHEDULE.invoke(null, block.getPhaseDaysForDisplay(), (float) SPEED.invoke(block, level, root, growth)));
                    steps = -Math.max(0, growth.dayInPhase);
                    for (int i = Math.max(0, growth.phase); i < schedule.length; i++) steps += Math.max(0, schedule[i]);
                    steps = Math.max(0, steps);
                }
                // Native infertile-soil settlement is applied to a scratch state only.
                var scratch = new CropGrowthManager.CropGrowthState(); scratch.infertileProgress = growth.infertileProgress;
                boolean infertile = com.stardew.craft.block.terrain.TerrainSoils.infertile(soil);
                remaining = 0;
                int forecastDay = Math.max(NativeStarfieldResilience.day(), growth.lastDailyDay);
                double strength = StarfieldGeneEffects.strength(level, root);
                while (steps > 0 && remaining < 100000) {
                    remaining++;
                    if (scratch.advanceOnSoil(infertile) || infertile
                            && com.agricraft.agricraft.api.compat.NativeCropGeneRules.recoverSoilSkip(forecastDay + remaining, strength)) steps--;
                }
                if (steps > 0) remaining = -1;
            }
            if (growth != null) phase = growth.phase;
            var tag = new CompoundTag(); tag.putInt("schema", 1);
            tag.putString("dimension", level.dimension().location().toString()); tag.putLong("root", root.asLong());
            tag.put("entry", entry.get().encode()); tag.putString("state", level.getBlockState(root).toString()); tag.putString("soil", soil.toString());
            tag.putBoolean("mature", plant.get().mature()); tag.putBoolean("regrowing", regrowing);
            tag.putInt("phase", phase); tag.putInt("phases", phases); tag.putInt("remaining", remaining);
            tag.putBoolean("wet", soil.hasProperty(FarmBlock.MOISTURE) && soil.getValue(FarmBlock.MOISTURE) > 0);
            tag.putBoolean("lastWatered", growth != null && growth.lastDailyWatered);
            int season = StardewTimeManager.get().getCurrentSeason();
            tag.putString("season", new String[]{"spring", "summer", "fall", "winter"}[Math.floorMod(season, 4)]);
            boolean inSeason = plant.get().metadata().isPresent() ? plant.get().metadata().get().seasons().contains(tag.getString("season"))
                    : (boolean) SEASON.invoke(block, level);
            tag.putBoolean("inSeason", SeasonLocationRules.seedsIgnoreSeasonsHere(level, root) || inSeason);
            int grace = NativeStarfieldResilience.grace(level, root, level.getBlockState(root), NativeStarfieldResilience.day(), season, false);
            tag.putBoolean("seasonPaused", grace > 0); tag.putInt("graceRemaining", grace);
            var seasons = new ListTag(); plant.get().metadata().ifPresent(meta -> meta.seasons().forEach(value -> seasons.add(StringTag.valueOf(value)))); tag.put("seasons", seasons);
            var fertilizer = FertilizerManager.get(level).getFertilizer(level, root.below());
            tag.putString("fertilizer", fertilizer == null ? "" : fertilizer.getSerializedName());
            return Optional.of(tag);
        } catch (ReflectiveOperationException | RuntimeException failure) {
            com.agricraft.agricraft.AgriCraft.LOGGER.warn("Native Starfield inspection unavailable: {}", failure.toString()); return Optional.empty();
        }
    }
}
