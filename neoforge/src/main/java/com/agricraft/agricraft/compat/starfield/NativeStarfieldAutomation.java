package com.agricraft.agricraft.compat.starfield;

import com.agricraft.agricraft.api.compat.ExternalHarvestSupport;
import com.agricraft.agricraft.api.compat.NativeCropGeneRules;
import com.stardew.craft.api.v1.agriculture.StardewCropRuntime;
import com.stardew.craft.block.crop.StardewCropBlock;
import com.stardew.craft.block.crop.WildSeedCropBlock;
import com.stardew.craft.block.nature.ForageBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

/** Real Junimo/automation context, without player, tool or farming skill impersonation. */
public final class NativeStarfieldAutomation implements ExternalHarvestSupport.Provider {
    public NativeStarfieldAutomation() {}
    @Override public ExternalHarvestSupport.Availability inspect(Level level, BlockPos pos) {
        if (!StarfieldCompat.isActive()) return ExternalHarvestSupport.Availability.UNHANDLED;
        var block = level.getBlockState(pos).getBlock();
        // The pinned crop API cannot pick forage, dig roots or harvest giant blocks.
        if (block instanceof ForageBlock || BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("stardewcraft")
                && BuiltInRegistries.BLOCK.getKey(block).getPath().startsWith("giant_"))
            return ExternalHarvestSupport.Availability.UNSUPPORTED;
        var plant = StarfieldCompat.inspect(level, pos);
        if (plant.isEmpty()) return block instanceof StardewCropBlock
                ? ExternalHarvestSupport.Availability.UNSUPPORTED : ExternalHarvestSupport.Availability.UNHANDLED;
        if (!(level instanceof ServerLevel server) || block instanceof WildSeedCropBlock
                || !StarfieldPlantData.get(server).writable(GlobalPos.of(level.dimension(), plant.get().root()))
                || StardewCropBlock.isPlayerPlacedDecorative(server, plant.get().root(), level.getBlockState(plant.get().root())))
            return ExternalHarvestSupport.Availability.UNSUPPORTED;
        return plant.get().mature() ? ExternalHarvestSupport.Availability.READY : ExternalHarvestSupport.Availability.NOT_READY;
    }
    @Override public ExternalHarvestSupport.Result harvest(Level level, BlockPos pos, Consumer<ItemStack> output) {
        var available = inspect(level, pos);
        if (available == ExternalHarvestSupport.Availability.UNHANDLED) return ExternalHarvestSupport.Result.UNHANDLED;
        if (available != ExternalHarvestSupport.Availability.READY) return ExternalHarvestSupport.Result.REJECTED;
        return StardewCropRuntime.harvestForAutomation((ServerLevel) level, pos, 0, output).harvested()
                ? ExternalHarvestSupport.Result.HARVESTED : ExternalHarvestSupport.Result.REJECTED;
    }
    static ItemStack collect(ServerLevel level, BlockPos pos, Consumer<ItemStack> output,
            Function<Consumer<ItemStack>, ItemStack> original) {
        var plant = StarfieldCompat.inspect(level, pos);
        if (plant.isEmpty()) return original.apply(output);
        var root = plant.get().root();
        if (!plant.get().mature() || plant.get().species().seasonalForage()
                || !StarfieldPlantData.get(level).writable(GlobalPos.of(level.dimension(), root))) return ItemStack.EMPTY;
        NativeStarfieldTransfer.Snapshot crop;
        try { crop = NativeStarfieldTransfer.capture(level, root); }
        catch (RuntimeException failure) {
            com.agricraft.agricraft.AgriCraft.LOGGER.warn("Starfield automation rejected before harvest: {}", failure.getMessage());
            return ItemStack.EMPTY;
        }
        var staged = new ArrayList<ItemStack>();
        var result = new ItemStack[] {ItemStack.EMPTY};
        double gain = StarfieldGeneEffects.gain(level, root);
        try {
            StarfieldHarvestTransaction.run(level, () -> {
                result[0] = original.apply(stack -> { if (!stack.isEmpty()) staged.add(stack.copy()); });
                if (result[0].isEmpty()) { staged.clear(); return; }
                var after = StardewCropRuntime.inspect(level, root);
                if (after != null && after.mature()
                        || crop.upper() != null && level.getBlockState(root).getBlock() != crop.lower().getBlock()
                        && level.getBlockState(root.above()).getBlock() == crop.upper().getBlock())
                    throw new IllegalStateException("Native automation did not complete crop transition");
                NativeStarfieldSticks.afterHarvest(level, root, crop.entry());
                var transformed = new ArrayList<ItemStack>();
                for (var stack : staged) {
                    boolean primary = plant.get().species().produce().filter(BuiltInRegistries.ITEM.getKey(stack.getItem())::equals).isPresent();
                    if (primary) StarfieldGenomes.writeProduce(stack, crop.entry());
                    int count = primary ? NativeCropGeneRules.yield(stack.getCount(), gain) : stack.getCount();
                    while (count > 0) {
                        int size = Math.min(count, stack.getMaxStackSize());
                        transformed.add(stack.copyWithCount(size)); count -= size;
                    }
                }
                staged.clear(); staged.addAll(transformed);
            }, () -> {
                if (!NativeStarfieldTransfer.restore(level, root, crop))
                    throw new IllegalStateException("Cannot restore rejected native automation harvest");
            }, () -> {});
        } catch (StarfieldHarvestTransaction.Aborted failure) { return ItemStack.EMPTY; }
        if (!result[0].isEmpty()) NativeStarfieldLifecycle.consumed(crop.entry().plantingId());
        // A throwing sink cannot undo already accepted inventory items; restoring the crop
        // after delivery would permit duplication. The crop stays committed, with no retry.
        for (var stack : staged) output.accept(stack.copy());
        return result[0].isEmpty() || staged.isEmpty() ? ItemStack.EMPTY : staged.getFirst().copy();
    }
}
