package com.agricraft.agricraft.compat.starfield;

import com.agricraft.agricraft.common.block.CropStickVariant;
import com.stardew.craft.api.v1.agriculture.StardewCropState;
import com.stardew.craft.api.v1.agriculture.StardewGiantCrops;
import com.stardew.craft.manager.CropGrowthManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Adds stick restitution at the native commit boundary; never executes giant growth. */
final class NativeStarfieldGiants {
    private static final ThreadLocal<ArrayDeque<Frame>> FRAMES = ThreadLocal.withInitial(ArrayDeque::new);
    private static final class Frame {
        Snapshot pending;
        boolean publishing;
    }
    private record Snapshot(ServerLevel level, BlockPos anchor, Map<BlockPos, BlockState> blocks,
                            Map<BlockPos, CropGrowthManager.CropGrowthState> growth,
                            Map<BlockPos, StarfieldPlantData.Entry> entries) {}

    static void begin() {
        var frames = FRAMES.get();
        if (!frames.isEmpty() && frames.peek().publishing)
            throw new IllegalStateException("Reentrant giant formation during stick publication");
        frames.push(new Frame());
    }

    static void end() {
        var frames = FRAMES.get();
        if (!frames.isEmpty()) frames.pop();
        if (frames.isEmpty()) FRAMES.remove();
    }

    static void reset() { FRAMES.remove(); }

    static List<?> plan(Object value, List<?> crops, List<?> plan) {
        var frames = FRAMES.get();
        if (frames.isEmpty()) return plan;
        var frame = frames.peek();
        frame.pending = null;
        if (plan == null || plan.isEmpty()) return plan;
        var context = (StardewGiantCrops.Context) value;
        var level = context.level();
        var data = StarfieldPlantData.get(level);
        var entries = new LinkedHashMap<BlockPos, StarfieldPlantData.Entry>();
        var growth = new LinkedHashMap<BlockPos, CropGrowthManager.CropGrowthState>();
        for (var valueCrop : crops) {
            var root = ((StardewCropState) valueCrop).root();
            var key = GlobalPos.of(level.dimension(), root);
            // Preserve quarantined/future data instead of consuming an unreadable attachment.
            if (!data.writable(key)) return List.of();
            data.get(key).ifPresent(entry -> entries.put(root, entry));
            var state = CropGrowthManager.get(level).getState(level, root);
            if (state != null) growth.put(root, state);
        }
        if (entries.values().stream().noneMatch(entry -> !entry.sticks().isEmpty())) return plan;
        var definition = context.definition();
        var blocks = new LinkedHashMap<BlockPos, BlockState>();
        for (var mutable : BlockPos.betweenClosed(context.anchor(), context.anchor().offset(
                definition.width() - 1, definition.height() - 1, definition.depth() - 1))) {
            var pos = mutable.immutable();
            blocks.put(pos, level.getBlockState(pos));
        }
        frame.pending = new Snapshot(level, context.anchor(), blocks, growth, entries);
        return plan;
    }

    /** Native writes and untracking succeeded; this is before its first neighbour notification. */
    static void commit(ServerLevel level) {
        var frames = FRAMES.get();
        if (frames.isEmpty()) return;
        var frame = frames.peek();
        var saved = frame.pending;
        frame.pending = null;
        if (saved == null || saved.level() != level) return;
        frame.publishing = true;
        try {
            StarfieldHarvestTransaction.run(level, () -> {
                if (!level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) return;
                var counts = new LinkedHashMap<CropStickVariant, Integer>();
                saved.entries().values().stream().filter(entry -> !entry.sticks().isEmpty()).forEach(entry ->
                        counts.merge(CropStickVariant.valueOf(entry.sticks().toUpperCase(java.util.Locale.ROOT)), 1, Integer::sum));
                // The native model can push items spawned inside its footprint below farmland.
                // Emit above the complete shape so they stay collectible after the giant is cut.
                double spawnY = saved.blocks().keySet().stream().mapToInt(BlockPos::getY).max()
                        .orElse(saved.anchor().getY()) + 1.5;
                counts.forEach((variant, count) -> {
                    var template = CropStickVariant.toItem(variant);
                    for (int remaining = count; remaining > 0;) {
                        int amount = Math.min(remaining, template.getMaxStackSize());
                        // Explicit motion avoids consuming randomness belonging to native crop rolls.
                        var item = new ItemEntity(level, saved.anchor().getX() + 1.5, spawnY,
                                saved.anchor().getZ() + 1.5, template.copyWithCount(amount), 0, .2, 0);
                        item.setDefaultPickUpDelay();
                        level.addFreshEntity(item);
                        remaining -= amount;
                    }
                });
            }, () -> restore(saved), () -> {});
            saved.entries().values().forEach(entry -> NativeStarfieldLifecycle.consumed(entry.plantingId()));
        } finally { frame.publishing = false; }
    }

    private static void restore(Snapshot saved) {
        var level = saved.level();
        // Suppress intermediate shape callbacks, just as the native placement transaction does.
        saved.blocks().forEach((pos, state) -> {
            if (level.getBlockState(pos) != state
                    && !level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE))
                throw new IllegalStateException("Giant rollback rejected at " + pos);
        });
        saved.growth().forEach((pos, state) -> CropGrowthManager.get(level).restoreGrowthState(level, pos, state));
        saved.entries().forEach((pos, entry) -> StarfieldPlantData.get(level).put(level, pos, entry));
        // lastGiantDay keeps the native attempted date: an output failure cannot reroll that day.
    }
}
