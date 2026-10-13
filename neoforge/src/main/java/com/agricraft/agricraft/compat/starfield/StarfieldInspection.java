package com.agricraft.agricraft.compat.starfield;

import com.agricraft.agricraft.api.genetic.AgriGenome;
import com.agricraft.agricraft.common.util.LangUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;

/** One renderer for the magnifying glass, Jade and The One Probe. No guessed AGE percentage. */
public final class StarfieldInspection {
    private StarfieldInspection() {}
    public static boolean matches(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
            java.util.Optional<StarfieldPlantData.Entry> entry, CompoundTag tag) {
        if (tag.getInt("schema") != 1 || !level.dimension().location().toString().equals(tag.getString("dimension"))) return false;
        var root = net.minecraft.core.BlockPos.of(tag.getLong("root"));
        return (pos.equals(root) || pos.equals(root.above())) && entry.map(value -> value.encode().equals(tag.getCompound("entry"))).orElse(false)
                && level.getBlockState(root).toString().equals(tag.getString("state"))
                && level.getBlockState(root.below()).toString().equals(tag.getString("soil"));
    }
    public static List<Component> tooltip(CompoundTag tag, boolean extended) {
        if (tag.getInt("schema") != 1 || !tag.contains("entry")) return List.of();
        var entry = StarfieldPlantData.Entry.decode(tag.getCompound("entry"));
        var result = new ArrayList<Component>(); result.add(LangUtils.plantName(entry.type().toString()));
        if (tag.getBoolean("forage")) result.add(Component.translatable("agricraft.starfield.forage",
                net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(entry.form()).getName()));
        if (tag.getBoolean("mature")) result.add(Component.translatable("agricraft.starfield.ready"));
        else {
            result.add(tag.getBoolean("regrowing") ? Component.translatable("agricraft.starfield.regrowing")
                    : tag.getInt("phase") < 0 ? Component.translatable("agricraft.starfield.progress_unknown")
                    : Component.translatable("agricraft.starfield.phase", Math.min(tag.getInt("phase") + 1, tag.getInt("phases")), tag.getInt("phases")));
            result.add(tag.getInt("remaining") < 0 ? Component.translatable("agricraft.starfield.progress_unknown")
                    : Component.translatable("agricraft.starfield.remaining", tag.getInt("remaining")));
        }
        result.add(Component.translatable("agricraft.starfield.water." + (tag.getBoolean("wet") ? "wet" : "dry")));
        result.add(Component.translatable("agricraft.starfield.season", season(tag.getString("season")))
                .append(Component.translatable("agricraft.starfield.season." + (tag.getBoolean("inSeason") ? "allowed" : "outside"))));
        if (tag.getBoolean("seasonPaused")) result.add(Component.translatable("agricraft.starfield.resistance_grace", tag.getInt("graceRemaining")));
        if (extended) {
            var seasons = tag.getList("seasons", net.minecraft.nbt.Tag.TAG_STRING);
            if (!seasons.isEmpty()) {
                var line = Component.translatable("agricraft.starfield.seasons");
                for (int i = 0; i < seasons.size(); i++) { if (i > 0) line.append(", "); line.append(season(seasons.getString(i))); }
                result.add(line);
            }
            result.add(Component.translatable("agricraft.starfield.water.last", Component.translatable("agricraft.starfield." + (tag.getBoolean("lastWatered") ? "yes" : "no"))));
            if (!tag.getString("fertilizer").isEmpty()) result.add(Component.translatable("agricraft.starfield.fertilizer",
                    Component.translatable("item.stardewcraft." + tag.getString("fertilizer"))));
            var genome = AgriGenome.fromNBT(entry.genome());
            genome.getStatGenes().stream().sorted(java.util.Comparator.comparing(pair -> pair.getGene().getId())).forEach(pair ->
                    result.add(Component.translatable("agricraft.starfield.gene", Component.translatable("agricraft.stat." + pair.getGene().getId()),
                            pair.getDominant().trait(), pair.getRecessive().trait())));
            result.add(Component.translatable("agricraft.starfield.genes_preview"));
        }
        return List.copyOf(result);
    }
    private static Component season(String value) { return Component.translatable("agricraft.season." + (value.equals("fall") ? "autumn" : value)); }
}
