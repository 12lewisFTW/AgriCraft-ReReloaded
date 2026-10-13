package com.agricraft.agricraft.compat.starfield;

import com.agricraft.agricraft.api.compat.ExternalSeedSupport;
import com.agricraft.agricraft.common.block.entity.SeedAnalyzerBlockEntity;
import com.agricraft.agricraft.common.item.MagnifyingGlassItem;
import com.agricraft.agricraft.common.item.TrowelItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import java.util.Optional;

/** Intercepts only supported native contexts, before ordinary crop/analyzer block interaction. */
public final class StarfieldTools {
    private StarfieldTools() {}
    public static Optional<InteractionResult> use(UseOnContext context) {
        var sticks = StarfieldCompat.sticks(context);
        if (sticks.isPresent()) return sticks;
        var level = context.getLevel(); var pos = context.getClickedPos(); var stack = context.getItemInHand();
        if (level.isClientSide() && level.getBlockEntity(pos) instanceof SeedAnalyzerBlockEntity analyzer
                && context.getPlayer() != null && context.getPlayer().isShiftKeyDown() && ExternalSeedSupport.accepts(stack, level))
            return Optional.of(analyzer.hasSeed() ? InteractionResult.FAIL : InteractionResult.SUCCESS);
        if (level.getBlockEntity(pos) instanceof SeedAnalyzerBlockEntity analyzer && StarfieldCompat.isActive()
                && context.getPlayer() instanceof ServerPlayer player) {
            if (!StarfieldInspectionSync.withinReach(player, pos) || !StarfieldCompat.mayUseTools(player, pos)) return Optional.of(InteractionResult.FAIL);
            if (ExternalSeedSupport.accepts(stack, level) && player.isShiftKeyDown()) {
                if (analyzer.hasSeed()) return Optional.of(InteractionResult.FAIL);
                if (!analyzer.insertSeed(stack.copyWithCount(1)).isEmpty()) return Optional.of(InteractionResult.FAIL);
                stack.shrink(1); return Optional.of(InteractionResult.SUCCESS);
            }
            return Optional.empty();
        }
        if (stack.getItem() instanceof TrowelItem) {
            if (level.isClientSide()) {
                boolean held = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                        net.minecraft.world.item.component.CustomData.EMPTY).copyTag().contains(StarfieldGenomes.TRANSFER_KEY);
                return held || clientEntry(context).isPresent() ? Optional.of(com.agricraft.agricraft.api.config.CompatConfig.enableStarfield
                        ? InteractionResult.SUCCESS : InteractionResult.FAIL) : Optional.empty();
            }
            return StarfieldCompat.trowel(context);
        }
        if (!StarfieldCompat.rejectHarvestFallback(stack)) return Optional.empty();
        boolean nativePlant = level.isClientSide() ? clientEntry(context).isPresent() : StarfieldCompat.inspect(level, pos).isPresent();
        if (!nativePlant) return Optional.empty();
        if (context.getPlayer() instanceof ServerPlayer player && !StarfieldInspectionSync.withinReach(player, pos)) return Optional.of(InteractionResult.FAIL);
        if (stack.getItem() instanceof MagnifyingGlassItem) return Optional.of(InteractionResult.SUCCESS);
        if (context.getPlayer() instanceof ServerPlayer player) player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("agricraft.starfield.action_unavailable"), true);
        return Optional.of(InteractionResult.FAIL);
    }
    static Optional<InteractionResult> clientSticks(UseOnContext context) {
        var stack = context.getItemInHand();
        boolean material = stack.getItem() instanceof com.agricraft.agricraft.common.item.CropSticksItem;
        boolean remove = stack.isEmpty() && context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
        if ((material || remove) && clientEntry(context).isPresent()) return Optional.of(InteractionResult.SUCCESS);
        var pos = context.getClickedPos();
        var crop = context.getLevel().getBlockEntity(pos);
        if (!(crop instanceof com.agricraft.agricraft.common.block.entity.CropBlockEntity)) crop = context.getLevel().getBlockEntity(pos.above());
        if (crop instanceof com.agricraft.agricraft.common.block.entity.CropBlockEntity
                && ExternalSeedSupport.accepts(stack, context.getLevel())) return Optional.of(InteractionResult.SUCCESS);
        return Optional.empty();
    }
    private static Optional<StarfieldPlantData.Entry> clientEntry(UseOnContext context) {
        var root = context.getClickedPos(); var state = context.getLevel().getBlockState(root);
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)
                && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)
                == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER) root = root.below();
        var position = root;
        return StarfieldPlantClient.get(root).filter(entry -> net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .getKey(context.getLevel().getBlockState(position).getBlock()).equals(entry.form()));
    }
}
