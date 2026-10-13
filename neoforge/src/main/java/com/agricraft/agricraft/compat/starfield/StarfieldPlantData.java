package com.agricraft.agricraft.compat.starfield;

import com.agricraft.agricraft.AgriCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;

/** Genome/identity sidecar only. CropGrowthManager remains the sole owner of phase/day state. */
public final class StarfieldPlantData extends SavedData {
    public static final String STORAGE = "agricraft_starfield_plants";
    private final Map<GlobalPos, Entry> plants = new HashMap<>();
    // Runtime-only index. Saved schema and authoritative entries remain unchanged.
    private final Map<GlobalPos, Map<GlobalPos, Entry>> chunks = new HashMap<>();
    private final ListTag preserved = new ListTag();
    private final Set<GlobalPos> blockedRoots = new HashSet<>();
    private CompoundTag futureSchema;

    public record Entry(UUID plantingId, ResourceLocation type, CompoundTag genome,
                        CompoundTag seedTemplate, String sticks, ResourceLocation form) {
        public Entry(UUID plantingId, ResourceLocation type, CompoundTag genome, CompoundTag seedTemplate, String sticks) {
            this(plantingId, type, genome, seedTemplate, sticks, type);
        }
        public Entry { genome = genome.copy(); seedTemplate = seedTemplate.copy(); }
        @Override public CompoundTag genome() { return genome.copy(); }
        @Override public CompoundTag seedTemplate() { return seedTemplate.copy(); }

        public CompoundTag encode() {
            var tag = new CompoundTag();
            tag.putUUID("plantingId", plantingId);
            tag.putString("type", type.toString());
            tag.put("genome", genome());
            tag.put("seed", seedTemplate());
            tag.putString("sticks", sticks);
            if (!form.equals(type)) tag.putString("form", form.toString());
            return tag;
        }

        public static Entry decode(CompoundTag tag) {
            if (!tag.hasUUID("plantingId")) throw new IllegalArgumentException("Missing planting UUID");
            var type = ResourceLocation.parse(tag.getString("type"));
            var genome = tag.getCompound("genome");
            StarfieldGenomes.validate(genome, type);
            String sticks = tag.getString("sticks");
            if (!List.of("", "wooden", "iron", "obsidian").contains(sticks))
                throw new IllegalArgumentException("Unknown stick variant: " + sticks);
            var form = tag.contains("form") ? ResourceLocation.parse(tag.getString("form")) : type;
            if (!form.equals(type) && (!type.getNamespace().equals("stardewcraft")
                    || !List.of("spring_wild_seed_crop", "summer_wild_seed_crop", "fall_wild_seed_crop", "winter_wild_seed_crop").contains(type.getPath())
                    || !form.getNamespace().equals("stardewcraft") || !form.getPath().startsWith("forage_")))
                throw new IllegalArgumentException("Unsupported special crop form");
            return new Entry(tag.getUUID("plantingId"), type, genome, tag.getCompound("seed"), sticks,
                    form);
        }
    }

    public static StarfieldPlantData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new Factory<>(StarfieldPlantData::new, StarfieldPlantData::load), STORAGE);
    }

    public static StarfieldPlantData existing(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().get(
                new Factory<>(StarfieldPlantData::new, StarfieldPlantData::load), STORAGE);
    }

    public boolean writable() { return futureSchema == null; }
    public boolean writable(GlobalPos root) { return writable() && !blockedRoots.contains(root); }
    public Optional<Entry> get(GlobalPos key) { return Optional.ofNullable(plants.get(key)); }
    public Map<GlobalPos, Entry> inChunk(ServerLevel level, ChunkPos chunk) {
        return Map.copyOf(chunks.getOrDefault(GlobalPos.of(level.dimension(), chunk.getWorldPosition()), Map.of()));
    }

    private static GlobalPos chunkKey(GlobalPos root) {
        return GlobalPos.of(root.dimension(), new ChunkPos(root.pos()).getWorldPosition());
    }

    private void index(GlobalPos root, Entry entry) {
        plants.put(root, entry);
        chunks.computeIfAbsent(chunkKey(root), ignored -> new HashMap<>()).put(root, entry);
    }

    void put(ServerLevel level, BlockPos root, Entry entry) {
        if (!writable(GlobalPos.of(level.dimension(), root))) throw new IllegalStateException("Unknown or invalid Starfield sidecar data; preserving saved entry");
        index(GlobalPos.of(level.dimension(), root.immutable()), entry);
        setDirty();
        StarfieldPlantSync.changed(level, root, Optional.of(entry));
    }

    void remove(ServerLevel level, BlockPos root) {
        if (!writable()) return;
        var key = GlobalPos.of(level.dimension(), root);
        boolean changed = plants.remove(key) != null;
        var chunk = chunks.get(chunkKey(key));
        if (chunk != null) {
            chunk.remove(key);
            if (chunk.isEmpty()) chunks.remove(chunkKey(key));
        }
        if (blockedRoots.remove(key)) {
            preserved.removeIf(value -> {
                var row = (CompoundTag) value;
                return row.contains("root", Tag.TAG_LONG) && row.getLong("root") == root.asLong()
                        && row.getString("dimension").equals(level.dimension().location().toString());
            });
            changed = true;
        }
        if (changed) {
            setDirty();
            StarfieldPlantSync.changed(level, root, Optional.empty());
        }
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (futureSchema != null) return futureSchema.copy();
        tag.putInt("schema", 1);
        var entries = preserved.copy();
        plants.forEach((key, entry) -> {
            var value = entry.encode();
            value.putString("dimension", key.dimension().location().toString());
            value.putLong("root", key.pos().asLong());
            entries.add(value);
        });
        tag.put("plants", entries);
        return tag;
    }

    public static StarfieldPlantData load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new StarfieldPlantData();
        if (tag.getInt("schema") != 1) {
            data.futureSchema = tag.copy();
            AgriCraft.LOGGER.error("Unknown Starfield sidecar schema {}; data is preserved and writes disabled", tag.getInt("schema"));
            return data;
        }
        for (var value : tag.getList("plants", Tag.TAG_COMPOUND)) {
            var row = (CompoundTag) value;
            GlobalPos key = null;
            try {
                if (!row.contains("root", Tag.TAG_LONG)) throw new IllegalArgumentException("Missing root");
                var dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(row.getString("dimension")));
                key = GlobalPos.of(dimension, BlockPos.of(row.getLong("root")));
                if (data.plants.containsKey(key)) throw new IllegalArgumentException("Duplicate root");
                data.index(key, Entry.decode(row));
            } catch (RuntimeException exception) {
                if (key != null) data.blockedRoots.add(key);
                data.preserved.add(row.copy());
                AgriCraft.LOGGER.warn("Invalid Starfield sidecar entry preserved: {}", exception.getMessage());
            }
        }
        return data;
    }
}
