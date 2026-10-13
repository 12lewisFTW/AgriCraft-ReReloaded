package com.agricraft.agricraft.compat.starfield;

import com.stardew.craft.player.PlayerDataManager;
import com.stardew.craft.player.PlayerStardewData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Invokes the native algorithm verbatim; only its publication/rollback boundary is changed. */
final class NativeStarfieldHarvest {
    static void harvest(ServerLevel level, BlockPos pos, Player player, Runnable original) {
        if (NativeStarfieldSpecials.forageEntry(level, pos).isPresent()) {
            NativeStarfieldSpecials.harvestForage(level, pos, player, original); return;
        }
        var root = NativeStarfieldLifecycle.rootAt(level, pos);
        if (!NativeStarfieldLifecycle.root(level.getBlockState(root)) || StarfieldCompat.species(
                net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(root).getBlock())).isEmpty()) {
            original.run(); return;
        }
        // Unsupported future sidecar data is never rewritten by a transaction.
        if (!StarfieldPlantData.get(level).writable(net.minecraft.core.GlobalPos.of(level.dimension(), root)))
            throw new StarfieldHarvestTransaction.Aborted(new IllegalStateException("Unsupported Starfield plant data"));
        NativeStarfieldTransfer.Snapshot crop;
        try { crop = NativeStarfieldTransfer.capture(level, root); }
        catch (RuntimeException failure) { throw new StarfieldHarvestTransaction.Aborted(failure); }
        var manager = PlayerDataManager.get();
        var data = player instanceof ServerPlayer && manager != null ? manager.getData(player.getUUID()) : null;
        var saved = data == null ? null : data.toNBT(level.registryAccess());
        double gain = com.stardew.craft.block.crop.StardewCropBlock.isPlayerPlacedDecorative(level, root, level.getBlockState(root))
                ? 0 : StarfieldGeneEffects.gain(level, root);
        StarfieldHarvestTransaction.run(level, gain, stack -> StarfieldGenomes.writeProduce(stack, crop.entry()), () -> {
            original.run();
            var after = com.stardew.craft.api.v1.agriculture.StardewCropRuntime.inspect(level, root);
            if (after != null && after.mature()
                    || crop.upper() != null && level.getBlockState(root).getBlock() != crop.lower().getBlock()
                    && level.getBlockState(root.above()).getBlock() == crop.upper().getBlock())
                throw new IllegalStateException("Native harvest did not complete crop transition");
            NativeStarfieldSticks.afterHarvest(level, root, crop.entry());
        }, () -> {
            if (!NativeStarfieldTransfer.restore(level, root, crop))
                throw new IllegalStateException("Cannot restore rejected native harvest at " + root);
        }, () -> {
            if (manager != null && player instanceof ServerPlayer) {
                if (saved == null) manager.removePlayerData(player.getUUID());
                else manager.savePlayerData(player.getUUID(), PlayerStardewData.fromNBT(saved, player.getUUID(), level.registryAccess()));
            }
        });
        NativeStarfieldLifecycle.consumed(crop.entry().plantingId());
    }
}
