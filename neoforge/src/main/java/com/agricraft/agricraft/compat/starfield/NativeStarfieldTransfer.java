package com.agricraft.agricraft.compat.starfield;

import com.agricraft.agricraft.AgriCraft;
import com.agricraft.agricraft.common.item.TrowelItem;
import com.stardew.craft.block.crop.StardewCropBlock;
import com.stardew.craft.farming.CropPlanting;
import com.stardew.craft.manager.CropGrowthManager;
import com.stardew.craft.event.FarmAreaProtectionEvents;
import com.stardew.craft.block.utility.GardenPotBlock;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.Optional;

/** Full native-state transfer on the existing trowel, without AgriCrop/setGrowthStage conversion. */
final class NativeStarfieldTransfer {
    private static final ResourceLocation VALLEY = ResourceLocation.parse("stardewcraft:stardew_valley");
    record Snapshot(StarfieldPlantData.Entry entry, BlockState lower, BlockState upper,
                            CropGrowthManager.CropGrowthState growth) {}

    static Snapshot capture(ServerLevel level, BlockPos root) {
        var lower = level.getBlockState(root);
        var entry = NativeStarfieldLifecycle.ensure(level, root).orElseThrow();
        var growth = CropGrowthManager.get(level).getState(level, root);
        if (growth == null && NativeStarfieldSpecials.forageEntry(level, root).isEmpty()) throw new IllegalStateException("Missing native crop progress");
        return new Snapshot(entry, lower, lower.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                ? level.getBlockState(root.above()) : null, growth == null ? null : copy(growth));
    }

    static Optional<InteractionResult> use(UseOnContext context) {
        if (!(context.getItemInHand().getItem() instanceof TrowelItem) || !(context.getLevel() instanceof ServerLevel level))
            return Optional.empty();
        var custom = context.getItemInHand().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        var root = NativeStarfieldLifecycle.rootAt(level, context.getClickedPos());
        boolean held = custom.contains(StarfieldGenomes.TRANSFER_KEY);
        if (!held && !NativeStarfieldLifecycle.root(level.getBlockState(root)) && NativeStarfieldSpecials.forageEntry(level, root).isEmpty()) return Optional.empty();
        if (context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player
                && !StarfieldInspectionSync.withinReach(player, context.getClickedPos())) return Optional.of(InteractionResult.FAIL);
        if (context.getPlayer() != null && (!context.getPlayer().mayBuild()
                || !level.mayInteract(context.getPlayer(), context.getClickedPos())
                || !context.getPlayer().mayUseItemAt(context.getClickedPos(), context.getClickedFace(), context.getItemInHand())))
            return Optional.of(InteractionResult.FAIL);
        if (context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player
                && !FarmAreaProtectionEvents.canModifyDecorationAt(player, level, held ? context.getClickedPos().above() : root))
            return Optional.of(InteractionResult.FAIL);
        if (!level.dimension().location().equals(VALLEY) || !StarfieldPlantData.get(level).writable())
            return Optional.of(InteractionResult.FAIL);
        try {
            if (held) {
                var saved = custom.getCompound(StarfieldGenomes.TRANSFER_KEY);
                var snapshot = decode(level, saved);
                // Until commit, the material remains owned by the snapshot on the tool.
                NativeStarfieldLifecycle.consumed(snapshot.entry().plantingId());
                var target = context.getClickedPos().above();
                if (!saved.getString("dimension").equals(level.dimension().location().toString())
                        || !StarfieldCompat.soil(level.getBlockState(context.getClickedPos())).isPresent()
                        || !level.isLoaded(target) || !level.getBlockState(target).isAir()
                        || level.isOutsideBuildHeight(target)
                        || GardenPotBlock.isPottedPlant(level, target, snapshot.lower)
                        || snapshot.upper != null && !level.getBlockState(target.above()).isAir()
                        || snapshot.upper != null && level.isOutsideBuildHeight(target.above())
                        || !snapshot.lower.canSurvive(level, target)
                        || snapshot.growth != null && !NativeStarfieldSpecials.plantingAllowed(level, target, snapshot.lower)) return Optional.of(InteractionResult.FAIL);
                var placement = new ArrayList<BlockSnapshot>();
                placement.add(BlockSnapshot.create(level.dimension(), level, target, 3));
                if (snapshot.upper != null) placement.add(BlockSnapshot.create(level.dimension(), level, target.above(), 3));
                if (!CropPlanting.place(level, target, snapshot.lower)) return Optional.of(InteractionResult.FAIL);
                boolean restored = restore(level, target, snapshot);
                if (!restored) {
                    level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
                    return Optional.of(InteractionResult.FAIL);
                }
                boolean canceled = placement.size() == 1
                        ? EventHooks.onBlockPlace(context.getPlayer(), placement.getFirst(), context.getClickedFace())
                        : EventHooks.onMultiBlockPlace(context.getPlayer(), placement, context.getClickedFace());
                if (canceled) {
                    level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
                    return Optional.of(InteractionResult.FAIL);
                }
                custom.remove(StarfieldGenomes.TRANSFER_KEY);
                context.getItemInHand().set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
                return Optional.of(InteractionResult.SUCCESS);
            }
            // A vanilla AgriCraft plant already on the tool is never overwritten.
            if (custom.contains("genes")) return Optional.of(InteractionResult.FAIL);
            var entry = NativeStarfieldLifecycle.ensure(level, root).orElseThrow();
            var block = level.getBlockState(root);
            if (block.hasProperty(StardewCropBlock.POTTED) && block.getValue(StardewCropBlock.POTTED)) return Optional.of(InteractionResult.FAIL);
            var growth = CropGrowthManager.get(level).getState(level, root);
            if (growth == null && NativeStarfieldSpecials.forageEntry(level, root).isEmpty()) return Optional.of(InteractionResult.FAIL);
            BlockState upper = block.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) ? level.getBlockState(root.above()) : null;
            if (upper != null && upper.getBlock() != block.getBlock()) return Optional.of(InteractionResult.FAIL);
            var snapshot = new Snapshot(entry, block, upper, growth == null ? null : copy(growth));
            var encoded = encode(level, snapshot);
            if (!level.setBlock(root, Blocks.AIR.defaultBlockState(), 3)) return Optional.of(InteractionResult.FAIL);
            if (!level.getBlockState(root).isAir() || upper != null && !level.getBlockState(root.above()).isAir()) {
                restore(level, root, snapshot);
                return Optional.of(InteractionResult.FAIL);
            }
            custom.put(StarfieldGenomes.TRANSFER_KEY, encoded);
            context.getItemInHand().set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
            NativeStarfieldLifecycle.consumed(entry.plantingId());
            return Optional.of(InteractionResult.SUCCESS);
        } catch (RuntimeException exception) {
            AgriCraft.LOGGER.warn("Starfield trowel operation rejected: {}", exception.getMessage());
            return Optional.of(InteractionResult.FAIL);
        }
    }

    static boolean restore(ServerLevel level, BlockPos root, Snapshot snapshot) {
        if (level.getBlockState(root) != snapshot.lower && !level.setBlock(root, snapshot.lower, 3)) return false;
        if (snapshot.upper != null && level.getBlockState(root.above()) != snapshot.upper
                && !level.setBlock(root.above(), snapshot.upper, 3)) return false;
        if (level.getBlockState(root) != snapshot.lower || snapshot.upper != null && level.getBlockState(root.above()) != snapshot.upper)
            return false;
        if (snapshot.growth != null) CropGrowthManager.get(level).restoreGrowthState(level, root, snapshot.growth);
        StarfieldPlantData.get(level).put(level, root, snapshot.entry);
        return true;
    }

    private static CompoundTag encode(ServerLevel level, Snapshot snapshot) {
        var tag = new CompoundTag();
        tag.putInt("schema", 1);
        tag.putString("dimension", level.dimension().location().toString());
        tag.put("entry", snapshot.entry.encode());
        tag.put("lower", NbtUtils.writeBlockState(snapshot.lower));
        if (snapshot.upper != null) tag.put("upper", NbtUtils.writeBlockState(snapshot.upper));
        if (snapshot.growth != null) tag.put("growth", growth(snapshot.growth));
        return tag;
    }

    private static Snapshot decode(ServerLevel level, CompoundTag tag) {
        if (tag.getInt("schema") != 1) throw new IllegalArgumentException("Unknown transfer schema");
        var entry = StarfieldPlantData.Entry.decode(tag.getCompound("entry"));
        var species = StarfieldCompat.species(entry.type()).orElseThrow();
        var lookup = level.holderLookup(Registries.BLOCK);
        var lower = NbtUtils.readBlockState(lookup, tag.getCompound("lower"));
        var upper = tag.contains("upper") ? NbtUtils.readBlockState(lookup, tag.getCompound("upper")) : null;
        if (!entry.form().equals(entry.type())) {
            if (!NativeStarfieldSpecials.validForm(entry, lower) || upper != null || tag.contains("growth"))
                throw new IllegalArgumentException("Invalid native forage snapshot");
            var seed = ItemStack.parseOptional(level.registryAccess(), entry.seedTemplate());
            if (seed.isEmpty() || !StarfieldCompat.seed(seed).orElseThrow().id().equals(entry.type()))
                throw new IllegalArgumentException("Invalid forage seed template");
            return new Snapshot(entry, lower, null, null);
        }
        if (!NativeStarfieldLifecycle.root(lower) || !net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(lower.getBlock()).equals(entry.type())
                || lower.getValue(StardewCropBlock.POTTED) || species.twoParts() != (upper != null)
                || upper != null && (upper.getBlock() != lower.getBlock()
                || upper.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) != net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER))
            throw new IllegalArgumentException("Invalid native block snapshot");
        var seed = ItemStack.parseOptional(level.registryAccess(), entry.seedTemplate());
        if (seed.isEmpty() || !StarfieldCompat.seed(seed).orElseThrow().id().equals(entry.type()))
            throw new IllegalArgumentException("Invalid native seed template");
        var savedGrowth = tag.getCompound("growth");
        for (var name : new String[]{"phase", "day", "infertile", "version", "lastDay", "lastGiant"})
            if (!savedGrowth.contains(name, net.minecraft.nbt.Tag.TAG_INT)) throw new IllegalArgumentException("Incomplete native state: " + name);
        var growth = readGrowth(savedGrowth);
        if (growth.phase < 0 || growth.dayInPhase < 0 || growth.infertileProgress < 0 || growth.infertileProgress > 2)
            throw new IllegalArgumentException("Invalid native phase/progress");
        return new Snapshot(entry, lower, upper, growth);
    }

    static CompoundTag growth(CropGrowthManager.CropGrowthState state) {
        var tag = new CompoundTag();
        tag.putInt("phase", state.phase); tag.putInt("day", state.dayInPhase); tag.putInt("infertile", state.infertileProgress);
        tag.putBoolean("regrowing", state.regrowing); tag.putBoolean("source", state.sourcePhases);
        tag.putInt("version", state.sourcePhaseVersion); tag.putInt("lastDay", state.lastDailyDay);
        tag.putBoolean("watered", state.lastDailyWatered); tag.putInt("lastGiant", state.lastGiantDay);
        if (state.planterUuid != null) tag.putUUID("planter", state.planterUuid);
        return tag;
    }

    private static CropGrowthManager.CropGrowthState readGrowth(CompoundTag tag) {
        var state = new CropGrowthManager.CropGrowthState();
        state.phase = tag.getInt("phase"); state.dayInPhase = tag.getInt("day"); state.infertileProgress = tag.getInt("infertile");
        state.regrowing = tag.getBoolean("regrowing"); state.sourcePhases = tag.getBoolean("source");
        state.sourcePhaseVersion = tag.getInt("version"); state.lastDailyDay = tag.getInt("lastDay");
        state.lastDailyWatered = tag.getBoolean("watered"); state.lastGiantDay = tag.getInt("lastGiant");
        state.planterUuid = tag.hasUUID("planter") ? tag.getUUID("planter") : null;
        return state;
    }

    private static CropGrowthManager.CropGrowthState copy(CropGrowthManager.CropGrowthState state) { return readGrowth(growth(state)); }
}
