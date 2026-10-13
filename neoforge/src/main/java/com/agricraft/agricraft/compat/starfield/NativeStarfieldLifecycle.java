package com.agricraft.agricraft.compat.starfield;

import com.agricraft.agricraft.AgriCraft;
import com.stardew.craft.block.crop.StardewCropBlock;
import com.stardew.craft.manager.CropGrowthManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/** Narrow lifecycle hooks; no phase executor or world representation substitution. */
final class NativeStarfieldLifecycle {
    private record Planting(ItemStack seed, StarfieldCompat.Species species) {}
    private record Removed(StarfieldPlantData.Entry entry, Object growth) {}
    private static final ThreadLocal<Planting> planting = new ThreadLocal<>();
    private static final Map<GlobalPos, Removed> removedThisTick = new HashMap<>();
    private record Attachment(GlobalPos position, StarfieldPlantData.Entry entry) {}
    private static final Map<UUID, Attachment> detached = new HashMap<>();
    private static final java.util.Set<UUID> consumed = new java.util.HashSet<>();
    private static final Map<LevelChunk, StarfieldPlantData> scannedChunks = new java.util.WeakHashMap<>();

    static boolean root(BlockState state) {
        return state.getBlock() instanceof StardewCropBlock
                && (!state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                || state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER);
    }

    static BlockPos rootAt(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }

    static Optional<StarfieldPlantData.Entry> ensure(ServerLevel level, BlockPos position) {
        var root = rootAt(level, position);
        var state = level.getBlockState(root);
        var forage = NativeStarfieldSpecials.forageEntry(level, root);
        if (forage.isPresent()) return forage;
        if (!root(state) || state.hasBlockEntity()) return Optional.empty();
        var type = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (StarfieldCompat.species(type).isEmpty()) return Optional.empty();
        var data = StarfieldPlantData.get(level);
        if (!data.writable(GlobalPos.of(level.dimension(), root))) return Optional.empty();
        var previous = data.get(GlobalPos.of(level.dimension(), root));
        if (previous.isPresent() && previous.get().type().equals(type)) return previous;
        var context = planting.get();
        var template = context != null && context.species.id().equals(type) ? context.seed.copyWithCount(1)
                : new ItemStack(BuiltInRegistries.ITEM.get(StarfieldCompat.species(type).orElseThrow().seed()));
        var genome = StarfieldGenomes.readSeed(template, type);
        StarfieldGenomes.writeSeed(template, type, genome);
        var entry = new StarfieldPlantData.Entry(UUID.randomUUID(), type, genome,
                (net.minecraft.nbt.CompoundTag) template.save(level.registryAccess()), "");
        data.put(level, root, entry);
        return Optional.of(entry);
    }

    static InteractionResult useSeed(UseOnContext context, Supplier<InteractionResult> original) {
        if (!(context.getLevel() instanceof ServerLevel level)) return original.get();
        var species = StarfieldCompat.seed(context.getItemInHand());
        if (species.isEmpty()) return original.get();
        if (!StarfieldPlantData.get(level).writable(GlobalPos.of(level.dimension(), context.getClickedPos().above())))
            return InteractionResult.FAIL;
        var snapshot = context.getItemInHand().copyWithCount(1);
        try { StarfieldGenomes.readSeed(snapshot, species.get().id()); }
        catch (RuntimeException exception) {
            AgriCraft.LOGGER.warn("Starfield seed planting rejected without item/world mutation: {}", exception.getMessage());
            return InteractionResult.FAIL;
        }
        var previous = planting.get();
        planting.set(new Planting(snapshot, species.get()));
        try {
            var result = original.get();
            if (result.consumesAction() && !context.getItemInHand().isEmpty())
                StarfieldGenomes.writeSeed(context.getItemInHand(), species.get().id(),
                        StarfieldGenomes.readSeed(snapshot, species.get().id()));
            return result;
        } finally {
            if (previous == null) planting.remove(); else planting.set(previous);
        }
    }

    static void placed(BlockState state, Level level, BlockPos pos, BlockState oldState) {
        if (level instanceof ServerLevel server && !state.is(oldState.getBlock()) && root(state)) ensure(server, pos);
    }

    static void removing(BlockState state, Level level, BlockPos pos, BlockState next) {
        if (level instanceof ServerLevel server && !state.is(next.getBlock()) && root(state)) {
            var data = StarfieldPlantData.get(server);
            data.get(GlobalPos.of(level.dimension(), pos)).ifPresent(entry -> {
                removedThisTick.put(GlobalPos.of(level.dimension(), pos.immutable()), new Removed(entry, CropGrowthManager.get(server).getState(level, pos)));
                detached(server,pos,entry);
            });
            data.remove(server, pos);
        }
    }

    // Native rejected giant transactions restore the SAME saved growth object. A new planting
    // creates a different object, so it must never inherit this old UUID/genome by position alone.
    static void restored(ServerLevel level, BlockPos pos, Object growth) {
        var previous = removedThisTick.remove(GlobalPos.of(level.dimension(), pos));
        if (previous != null && previous.growth == growth
                && BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).equals(previous.entry.type()))
            StarfieldPlantData.get(level).put(level, pos, previous.entry);
    }

    static void endTick() {
        if (com.agricraft.agricraft.api.config.CompatConfig.enableStarfield) for (var attachment : detached.values()) {
            var entry = attachment.entry(); var key = attachment.position();
            if (entry.sticks().isEmpty() || consumed.contains(entry.plantingId())) continue;
            var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server == null) continue;
            var level = server.getLevel(key.dimension());
            if (level == null || !level.hasChunkAt(key.pos())) continue;
            var data = StarfieldPlantData.get(level);
            if (data.get(key).filter(current -> current.plantingId().equals(entry.plantingId())).isPresent()) continue;
            if (!level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS)) continue;
            var variant = com.agricraft.agricraft.common.block.CropStickVariant.valueOf(entry.sticks().toUpperCase(java.util.Locale.ROOT));
            var drop = new net.minecraft.world.entity.item.ItemEntity(level,key.pos().getX()+.5,key.pos().getY()+.5,key.pos().getZ()+.5,
                    com.agricraft.agricraft.common.block.CropStickVariant.toItem(variant),0,.2,0);
            drop.setDefaultPickUpDelay();level.addFreshEntity(drop);
        }
        detached.clear(); consumed.clear();
        removedThisTick.clear();
        if (!com.agricraft.agricraft.api.config.CompatConfig.enableStarfield) invalidateScans();
    }
    static void consumed(UUID id) { consumed.add(id); }
    static void detached(ServerLevel level, BlockPos root, StarfieldPlantData.Entry entry) {
        if (!entry.sticks().isEmpty()) detached.put(entry.plantingId(), new Attachment(GlobalPos.of(level.dimension(),root.immutable()),entry));
    }
    static void reset() { removedThisTick.clear(); detached.clear(); consumed.clear(); planting.remove(); invalidateScans(); }
    static void invalidateScans() { scannedChunks.clear(); }

    static void scan(ServerLevel level, LevelChunk chunk) {
        var data = StarfieldPlantData.get(level);
        // The load event and multiple watchers can request the same loaded chunk.
        // Lifecycle hooks maintain it afterwards; replacement SavedData requires migration again.
        if (scannedChunks.get(chunk) == data) return;
        // Compare actual identity even if a datapack temporarily excludes the species from lookup.
        data.inChunk(level, chunk.getPos()).forEach((key, entry) -> {
            if (!(root(level.getBlockState(key.pos())) || NativeStarfieldSpecials.validForm(entry, level.getBlockState(key.pos())))
                    || !BuiltInRegistries.BLOCK.getKey(level.getBlockState(key.pos()).getBlock()).equals(entry.form())) data.remove(level, key.pos());
        });
        for (int sectionIndex = 0; sectionIndex < chunk.getSections().length; sectionIndex++) {
            var section = chunk.getSections()[sectionIndex];
            if (!section.maybeHas(NativeStarfieldLifecycle::root)) continue;
            int baseY = chunk.getSectionYFromSectionIndex(sectionIndex) << 4;
            for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++)
                if (root(section.getBlockState(x, y, z))) ensure(level, chunk.getPos().getWorldPosition().offset(x, baseY + y, z));
        }
        scannedChunks.put(chunk, data);
    }

    static boolean sticks(ServerLevel level, BlockPos position, String variant) {
        if (!java.util.List.of("", "wooden", "iron", "obsidian").contains(variant)) return false;
        var entry = ensure(level, position);
        if (entry.isEmpty()) return false;
        var old = entry.get();
        StarfieldPlantData.get(level).put(level, rootAt(level, position), new StarfieldPlantData.Entry(
                old.plantingId(), old.type(), old.genome(), old.seedTemplate(), variant, old.form()));
        return true;
    }

    static Optional<ItemStack> seed(ServerLevel level, BlockPos position) {
        return ensure(level, position).flatMap(entry -> {
            var seed = ItemStack.parseOptional(level.registryAccess(), entry.seedTemplate());
            if (seed.isEmpty()) {
                AgriCraft.LOGGER.warn("Invalid Starfield seed template preserved for {}", entry.type());
                return Optional.empty();
            }
            StarfieldGenomes.writeSeed(seed, entry.type(), entry.genome());
            return Optional.of(seed.copyWithCount(1));
        });
    }
}
