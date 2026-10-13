package com.agricraft.agricraft.compat.starfield;

import com.agricraft.agricraft.common.block.CropBlock;
import com.agricraft.agricraft.common.block.CropStickVariant;
import com.agricraft.agricraft.common.block.SimpleFluidloggedBlock;
import com.agricraft.agricraft.common.block.entity.CropBlockEntity;
import com.stardew.craft.block.crop.StardewCropBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import java.util.ArrayList;
import java.util.Optional;

/** Player placement keeps the native seed's own season/location/placement gates. */
final class NativeStarfieldSticks {
    private static final ThreadLocal<Boolean> USING = ThreadLocal.withInitial(() -> false);
    static void afterHarvest(ServerLevel level, BlockPos root, StarfieldPlantData.Entry entry) {
        if (entry.sticks().isEmpty() || !level.getBlockState(root).isAir()) return;
        var variant = CropStickVariant.valueOf(entry.sticks().toUpperCase(java.util.Locale.ROOT));
        var empty = com.agricraft.agricraft.common.registry.ModBlocks.CROP.get().defaultBlockState()
                .setValue(CropBlock.STICK_VARIANT, variant);
        if (!empty.canSurvive(level, root) || !level.setBlock(root, empty, 3))
            throw new IllegalStateException("Cannot retain sticks after native harvest");
    }

    static Optional<InteractionResult> use(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level) || USING.get()) return Optional.empty();
        var clicked = context.getClickedPos();
        if (!level.hasChunkAt(clicked)) return Optional.empty();
        var root = NativeStarfieldLifecycle.rootAt(level, clicked);
        var stack = context.getItemInHand();
        var variant = CropStickVariant.fromItem(stack);
        var entry = NativeStarfieldLifecycle.ensure(level, root);
        boolean remove = stack.isEmpty() && context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
        if (entry.isPresent() && (variant != null || remove)) {
            if (!allowed(context, level, root) || level.getBlockState(root).hasProperty(StardewCropBlock.POTTED)
                    && level.getBlockState(root).getValue(StardewCropBlock.POTTED)) return Optional.of(InteractionResult.FAIL);
            var old = entry.get();
            // One material per planting; remove it before applying a different material.
            if (remove ? old.sticks().isEmpty() : !old.sticks().isEmpty()) return Optional.of(InteractionResult.FAIL);
            USING.set(true);
            try {
                if (!StarfieldCompat.setSticks(level, root, remove ? "" : variant.getSerializedName())) return Optional.of(InteractionResult.FAIL);
                if (!context.getPlayer().isCreative()) {
                    if (remove) context.getPlayer().getInventory().placeItemBackInInventory(
                            CropStickVariant.toItem(CropStickVariant.valueOf(old.sticks().toUpperCase(java.util.Locale.ROOT))));
                    else stack.shrink(1);
                }
                if (!remove) variant.playSound(level, root);
                return Optional.of(InteractionResult.SUCCESS);
            } finally { USING.remove(); }
        }
        if (StarfieldCompat.seed(stack).isEmpty()) return Optional.empty();
        // Accept a click on the empty sticks themselves or on the supporting soil.
        if (!(level.getBlockEntity(root) instanceof CropBlockEntity)) root = clicked.above();
        if (!(level.getBlockEntity(root) instanceof CropBlockEntity crop)) return Optional.empty();
        if (!allowed(context, level, root) || crop.hasPlant() || crop.hasWeeds() || crop.isCrossCropSticks()
                || !crop.hasCropSticks()) return Optional.of(InteractionResult.FAIL);
        var old = level.getBlockState(root);
        if (old.getValue(SimpleFluidloggedBlock.WATERLOGGED) || old.getValue(SimpleFluidloggedBlock.LAVALOGGED))
            return Optional.of(InteractionResult.FAIL);
        var species = StarfieldCompat.seed(stack).orElseThrow();
        if (!StarfieldPlantData.get(level).writable(GlobalPos.of(level.dimension(), root))
                || species.twoParts() && (!level.hasChunkAt(root.above()) || !level.getBlockState(root.above()).isAir()
                || level.isOutsideBuildHeight(root.above()))) return Optional.of(InteractionResult.FAIL);
        try { StarfieldGenomes.readSeed(stack, species.id()); }
        catch (RuntimeException invalid) { return Optional.of(InteractionResult.FAIL); }
        var position = root;
        var savedCrop = crop.saveWithFullMetadata(level.registryAccess());
        var savedSeed = stack.copy();
        var upper = level.getBlockState(root.above());
        var snapshots = new ArrayList<BlockSnapshot>();
        snapshots.add(BlockSnapshot.create(level.dimension(), level, root, 3));
        if (species.twoParts()) snapshots.add(BlockSnapshot.create(level.dimension(), level, root.above(), 3));
        boolean committed = false;
        USING.set(true);
        try {
            if (!level.setBlock(root, Blocks.AIR.defaultBlockState(), 3)) return Optional.of(InteractionResult.FAIL);
            var soil = root.below();
            var redirected = new UseOnContext(context.getPlayer(), context.getHand(),
                    new BlockHitResult(Vec3.atCenterOf(soil), Direction.UP, soil, false));
            var result = NativeStarfieldLifecycle.useSeed(redirected, () -> stack.getItem().useOn(redirected));
            if (!result.consumesAction() || !level.getBlockState(root).is(net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(species.id())))
                return Optional.of(InteractionResult.FAIL);
            var planted = NativeStarfieldLifecycle.ensure(level, root).orElseThrow();
            StarfieldPlantData.get(level).put(level, root, new StarfieldPlantData.Entry(planted.plantingId(), planted.type(),
                    StarfieldGenomes.readSeed(savedSeed, species.id()), (net.minecraft.nbt.CompoundTag) savedSeed.copyWithCount(1).save(level.registryAccess()),
                    old.getValue(CropBlock.STICK_VARIANT).getSerializedName()));
            boolean canceled = snapshots.size() == 1 ? EventHooks.onBlockPlace(context.getPlayer(), snapshots.getFirst(), context.getClickedFace())
                    : EventHooks.onMultiBlockPlace(context.getPlayer(), snapshots, context.getClickedFace());
            if (canceled || !level.getBlockState(root).is(net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(species.id())))
                return Optional.of(InteractionResult.FAIL);
            if (context.getPlayer().isCreative()) context.getPlayer().setItemInHand(context.getHand(), savedSeed);
            committed = true;
            return Optional.of(InteractionResult.SUCCESS);
        } finally {
            try { if (!committed) {
                // The material already remains in the restored empty sticks.
                NativeStarfieldLifecycle.ensure(level, position).ifPresent(rejected -> NativeStarfieldLifecycle.consumed(rejected.plantingId()));
                level.setBlock(position, old, 3);
                if (species.twoParts()) level.setBlock(position.above(), upper, 3);
                if (level.getBlockEntity(position) instanceof CropBlockEntity restored) restored.loadWithComponents(savedCrop, level.registryAccess());
                else throw new IllegalStateException("Cannot restore rejected Starfield seed placement");
                context.getPlayer().setItemInHand(context.getHand(), savedSeed);
            } } finally { USING.remove(); }
        }
    }

    private static boolean allowed(UseOnContext context, ServerLevel level, BlockPos root) {
        return context.getPlayer() instanceof ServerPlayer player && level.hasChunkAt(root)
                && StarfieldInspectionSync.withinReach(player, root) && StarfieldCompat.mayUseTools(player, root)
                && player.mayUseItemAt(root, context.getClickedFace(), context.getItemInHand());
    }
}
