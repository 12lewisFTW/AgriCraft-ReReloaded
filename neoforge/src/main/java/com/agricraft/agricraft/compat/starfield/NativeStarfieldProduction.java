package com.agricraft.agricraft.compat.starfield;

import com.agricraft.agricraft.AgriCraft;
import com.stardew.craft.api.v1.machine.StardewProductionPlan;
import com.stardew.craft.blockentity.SeedMakerBlockEntity;
import com.stardew.craft.item.artisan.SeedMakerOutputResolver;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import java.util.Optional;

/** Final plan decoration after both native addon pipelines, before input consumption. */
final class NativeStarfieldProduction {
    static Optional<?> prepare(Object machine, ItemStack input, Optional<?> proposed) {
        if (proposed.isEmpty()) return proposed;
        var plan = (StardewProductionPlan) proposed.get();
        var output = plan.output();
        StarfieldGenomes.clearHeredity(output);
        if (machine instanceof SeedMakerBlockEntity) {
            var tag = input.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (tag.contains(StarfieldGenomes.PRODUCE_KEY)) {
                try {
                    if (!tag.contains(StarfieldGenomes.PRODUCE_KEY, Tag.TAG_COMPOUND))
                        throw new IllegalArgumentException("Invalid produce envelope");
                    var data = tag.getCompound(StarfieldGenomes.PRODUCE_KEY);
                    var type = ResourceLocation.tryParse(data.getString("type"));
                    var species = type == null ? null : StarfieldCompat.species(type).orElse(null);
                    var item = BuiltInRegistries.ITEM.getKey(input.getItem());
                    if (data.getInt("schema") != StarfieldGenomes.SCHEMA || species == null
                            || !data.getString("produce").equals(item.toString())
                            || !(species.produce().filter(item::equals).isPresent()
                            || species.seasonalForage() && BuiltInRegistries.ITEM.get(species.seed())
                                == SeedMakerOutputResolver.resolve(input.getItem())))
                        throw new IllegalArgumentException("Unknown produce schema or mismatched species/item");
                    var genome = data.getCompound("genome");
                    StarfieldGenomes.validate(genome, type);
                    // Ancient/mixed/foreign seeds keep the native selected result.
                    if (BuiltInRegistries.ITEM.getKey(output.getItem()).equals(species.seed()))
                        StarfieldGenomes.writeSeed(output, type, genome);
                } catch (RuntimeException failure) {
                    AgriCraft.LOGGER.warn("Starfield seed maker rejected inherited input before consumption: {}", failure.getMessage());
                    return Optional.empty();
                }
            }
        }
        return Optional.of(new StardewProductionPlan(output, plan.minutes()));
    }
}
