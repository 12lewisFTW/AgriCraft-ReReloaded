package com.agricraft.agricraft.starfield.prototype;

import com.agricraft.agricraft.api.genetic.*;
import com.agricraft.agricraft.api.stat.AgriStatRegistry;
import com.stardew.craft.api.v1.agriculture.StardewCropRuntime;
import com.stardew.craft.block.crop.StardewCropBlock;
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
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Iteration-0 sidecar only: no block entity, crop executor, renderer or automatic lifecycle yet. */
final class NativeStickAttachments extends SavedData {
    static final String STORAGE = "agricraft_starfield_stick_probe";
    private final Map<GlobalPos, Attachment> plants = new HashMap<>();

    record Attachment(UUID plantingId, ResourceLocation type, CompoundTag genome) {
        Attachment { genome = genome.copy(); }
        @Override public CompoundTag genome() { return genome.copy(); }
    }

    static NativeStickAttachments get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new Factory<>(NativeStickAttachments::new, NativeStickAttachments::load), STORAGE);
    }

    Attachment attach(ServerLevel level, BlockPos position) {
        var crop = StardewCropRuntime.inspect(level, position);
        if (crop == null || !(level.getBlockState(crop.root()).getBlock() instanceof StardewCropBlock)
                || level.getBlockState(crop.root()).hasBlockEntity())
            throw new IllegalArgumentException("Expected an unchanged native crop without a block entity");
        var key = GlobalPos.of(level.dimension(), crop.root());
        var previous = plants.get(key);
        if (previous != null && previous.type().equals(crop.typeId())) return previous;
        var attachment = new Attachment(UUID.randomUUID(), crop.typeId(), baseline(crop.typeId()));
        plants.put(key, attachment);
        setDirty();
        return attachment;
    }

    Optional<Attachment> inspect(ServerLevel level, BlockPos position) {
        var crop = StardewCropRuntime.inspect(level, position);
        if (crop == null) return Optional.empty();
        var attachment = plants.get(GlobalPos.of(level.dimension(), crop.root()));
        return attachment != null && attachment.type().equals(crop.typeId())
                ? Optional.of(attachment) : Optional.empty();
    }

    void detach(ServerLevel level, BlockPos root) {
        if (plants.remove(GlobalPos.of(level.dimension(), root)) != null) setDirty();
    }

    // Use the six existing AgriCraft gene pairs and configured minima, without rolling traits.
    private static CompoundTag baseline(ResourceLocation species) {
        var registry = AgriGeneRegistry.getInstance();
        var speciesGene = registry.getGeneSpecies();
        var stats = new ArrayList<AgriGenePair<Integer>>();
        for (var stat : AgriStatRegistry.getInstance()) {
            var gene = registry.getGeneStat(stat).orElseThrow();
            stats.add(new AgriGenePair<>(gene, gene.getAllele(stat.getMin())));
        }
        var genome = new AgriGenome(new AgriGenePair<>(speciesGene,
                speciesGene.getAllele(species.toString())), stats);
        var tag = new CompoundTag();
        genome.writeToNBT(tag);
        return tag;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("schema", 1);
        var entries = new ListTag();
        plants.forEach((key, plant) -> {
            var entry = new CompoundTag();
            entry.putString("dimension", key.dimension().location().toString());
            entry.putLong("root", key.pos().asLong());
            entry.putUUID("plantingId", plant.plantingId());
            entry.putString("type", plant.type().toString());
            entry.put("genome", plant.genome());
            entries.add(entry);
        });
        tag.put("plants", entries);
        return tag;
    }

    static NativeStickAttachments load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new NativeStickAttachments();
        if (tag.getInt("schema") != 1) throw new IllegalArgumentException("Unknown probe schema");
        for (var value : tag.getList("plants", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) value;
            var dimension = ResourceKey.create(Registries.DIMENSION,
                    ResourceLocation.parse(entry.getString("dimension")));
            data.plants.put(GlobalPos.of(dimension, BlockPos.of(entry.getLong("root"))),
                    new Attachment(entry.getUUID("plantingId"), ResourceLocation.parse(entry.getString("type")),
                            entry.getCompound("genome")));
        }
        return data;
    }
}
