package com.agricraft.agricraft.compat.starfield;

import com.agricraft.agricraft.api.genetic.AgriGenePair;
import com.agricraft.agricraft.api.genetic.AgriGeneRegistry;
import com.agricraft.agricraft.api.genetic.AgriGenome;
import com.agricraft.agricraft.api.genetic.GeneStat;
import com.agricraft.agricraft.api.stat.AgriStatRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;

/** Standard AgriGenome NBT in a namespaced envelope; never replaces foreign components. */
public final class StarfieldGenomes {
    public static final String SEED_KEY = "agricraft:starfield_seed";
    public static final String PRODUCE_KEY = "agricraft:starfield_produce";
    public static final String TRANSFER_KEY = "agricraft:starfield_plant";
    public static final int SCHEMA = 1;

    private StarfieldGenomes() {}

    public static CompoundTag baseline(ResourceLocation type) {
        var registry = AgriGeneRegistry.getInstance();
        var species = registry.getGeneSpecies();
        var pairs = new ArrayList<AgriGenePair<Integer>>();
        for (var stat : AgriStatRegistry.getInstance()) {
            var gene = registry.getGeneStat(stat).orElseThrow();
            pairs.add(new AgriGenePair<>(gene, new GeneStat.IntAllele(stat.getMin(), gene)));
        }
        var genome = new AgriGenome(new AgriGenePair<>(species, species.getAllele(type.toString())), pairs);
        var tag = new CompoundTag();
        genome.writeToNBT(tag);
        return tag;
    }

    public static void validate(CompoundTag genome, ResourceLocation type) {
        if (!genome.contains("genes", Tag.TAG_COMPOUND)) throw new IllegalArgumentException("Missing genome");
        var genes = genome.getCompound("genes");
        var species = genes.getCompound("species");
        if (!species.contains("dom", Tag.TAG_STRING) || !species.getString("dom").equals(type.toString())
                || ResourceLocation.tryParse(species.getString("rec")) == null)
            throw new IllegalArgumentException("Genome species does not match " + type);
        for (var stat : AgriStatRegistry.getInstance()) {
            var pair = genes.getCompound(stat.getId());
            if (!pair.contains("dom", Tag.TAG_INT) || !pair.contains("rec", Tag.TAG_INT))
                throw new IllegalArgumentException("Incomplete gene pair: " + stat.getId());
        }
    }

    /** No random rolls or item mutation on read. Missing envelope means configured baseline. */
    public static CompoundTag readSeed(ItemStack seed, ResourceLocation type) {
        var tag = seed.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(SEED_KEY)) {
            if (!tag.contains(PRODUCE_KEY)) return baseline(type);
            if (!tag.contains(PRODUCE_KEY, Tag.TAG_COMPOUND)) throw new IllegalArgumentException("Invalid produce envelope");
            var produce = tag.getCompound(PRODUCE_KEY);
            if (produce.getInt("schema") != SCHEMA || !produce.getString("type").equals(type.toString())
                    || !produce.getString("produce").equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(seed.getItem()).toString()))
                throw new IllegalArgumentException("Unknown produce schema, species or item");
            var genome = produce.getCompound("genome");
            validate(genome, type);
            return genome.copy();
        }
        if (!tag.contains(SEED_KEY, Tag.TAG_COMPOUND)) throw new IllegalArgumentException("Invalid seed envelope");
        var data = tag.getCompound(SEED_KEY);
        if (data.getInt("schema") != SCHEMA || !data.getString("type").equals(type.toString()))
            throw new IllegalArgumentException("Unknown seed schema or species");
        var genome = data.getCompound("genome");
        validate(genome, type);
        return genome.copy();
    }

    public static void writeSeed(ItemStack seed, ResourceLocation type, CompoundTag genome) {
        validate(genome, type);
        var tag = seed.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        var data = new CompoundTag();
        data.putInt("schema", SCHEMA);
        data.putString("type", type.toString());
        data.put("genome", genome.copy());
        tag.put(SEED_KEY, data);
        seed.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void writeProduce(ItemStack produce, StarfieldPlantData.Entry entry) {
        validate(entry.genome(), entry.type());
        var tag = produce.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        var data = new CompoundTag();
        data.putInt("schema", SCHEMA);
        data.putString("type", entry.type().toString());
        data.putString("produce", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(produce.getItem()).toString());
        data.put("genome", entry.genome());
        tag.put(PRODUCE_KEY, data);
        produce.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** Remove only compatibility heredity; native quality/flavour and foreign data survive. */
    public static void clearHeredity(ItemStack stack) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(SEED_KEY) && !tag.contains(PRODUCE_KEY)) return;
        tag.remove(SEED_KEY); tag.remove(PRODUCE_KEY);
        if (tag.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
        else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
