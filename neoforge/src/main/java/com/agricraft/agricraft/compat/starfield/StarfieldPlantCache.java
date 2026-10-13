package com.agricraft.agricraft.compat.starfield;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Client-only contents, with no references to client classes (also testable headlessly). */
public final class StarfieldPlantCache {
    private Object world;
    private ResourceLocation dimension;
    private UUID session;
    private final Map<BlockPos, StarfieldPlantData.Entry> plants = new HashMap<>();
    private final Map<ChunkPos, Map<BlockPos, StarfieldPlantData.Entry>> sticks = new HashMap<>();
    private record Inspection(net.minecraft.nbt.CompoundTag tag, long received) {}
    private final Map<BlockPos, Inspection> inspections = new HashMap<>();

    public void bind(Object world, ResourceLocation dimension) {
        if (this.world != world || !dimension.equals(this.dimension)) {
            clear(); this.world = world; this.dimension = dimension;
        }
    }

    public void apply(UUID session, ResourceLocation dimension, BlockPos root,
                      Optional<StarfieldPlantData.Entry> entry) {
        if (world == null || !dimension.equals(this.dimension)) return;
        if (!session.equals(this.session)) { plants.clear(); sticks.clear(); inspections.clear(); this.session = session; }
        if (!entry.equals(Optional.ofNullable(plants.get(root)))) inspections.remove(root);
        if (entry.isPresent()) plants.put(root.immutable(), entry.get());
        else plants.remove(root);
        var chunk = new ChunkPos(root);
        var index = sticks.get(chunk);
        if (index != null) { index.remove(root); if (index.isEmpty()) sticks.remove(chunk); }
        entry.filter(value -> !value.sticks().isEmpty()).ifPresent(value -> sticks.computeIfAbsent(chunk,
                ignored -> new HashMap<>()).put(root.immutable(), value));
    }

    public Optional<StarfieldPlantData.Entry> get(BlockPos root) { return Optional.ofNullable(plants.get(root)); }
    public void applyInspection(UUID session, ResourceLocation dimension, BlockPos root, net.minecraft.nbt.CompoundTag tag, long now) {
        if (world == null || !dimension.equals(this.dimension) || !session.equals(this.session)) return;
        var entry = get(root);
        if (tag.isEmpty()) { inspections.remove(root); return; }
        if (tag.getInt("schema") == 1 && tag.getLong("root") == root.asLong()
                && dimension.toString().equals(tag.getString("dimension"))
                && entry.isPresent() && entry.get().encode().equals(tag.getCompound("entry")))
            inspections.put(root.immutable(), new Inspection(tag.copy(), now));
    }
    public Optional<net.minecraft.nbt.CompoundTag> inspection(BlockPos root, long now) {
        var value = inspections.get(root);
        return value == null || now < value.received || now - value.received > 20 ? Optional.empty() : Optional.of(value.tag.copy());
    }
    public void forget(ChunkPos chunk) {
        sticks.remove(chunk);
        plants.keySet().removeIf(root -> new ChunkPos(root).equals(chunk));
        inspections.keySet().removeIf(root -> new ChunkPos(root).equals(chunk));
    }
    public void forEachSticks(ChunkPos center, int radius, java.util.function.BiConsumer<BlockPos, StarfieldPlantData.Entry> render) {
        for (int x = center.x - radius; x <= center.x + radius; x++) for (int z = center.z - radius; z <= center.z + radius; z++) {
            var entries = sticks.get(new ChunkPos(x, z));
            if (entries != null) entries.forEach(render);
        }
    }
    public void clear() { plants.clear(); sticks.clear(); inspections.clear(); world = null; dimension = null; session = null; }
}
