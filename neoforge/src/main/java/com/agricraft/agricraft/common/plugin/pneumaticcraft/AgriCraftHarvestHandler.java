package com.agricraft.agricraft.common.plugin.pneumaticcraft;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.crop.AgriCrop;
import me.desht.pneumaticcraft.api.drone.IDrone;
import me.desht.pneumaticcraft.api.harvesting.HarvestHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class AgriCraftHarvestHandler extends HarvestHandler {
    public AgriCraftHarvestHandler() {
    }

    @Override
    public boolean canHarvest(Level level, BlockGetter chunkCache, BlockPos pos, BlockState state, IDrone drone) {
        var nativeCrop = com.agricraft.agricraft.api.compat.ExternalHarvestSupport.inspect(level, pos);
        if (nativeCrop != com.agricraft.agricraft.api.compat.ExternalHarvestSupport.Availability.UNHANDLED)
            return nativeCrop == com.agricraft.agricraft.api.compat.ExternalHarvestSupport.Availability.READY;
        return AgriApi.getCrop(level, pos).map(AgriCrop::canBeHarvested).orElse(false);
    }

    @Override
    public void harvest(Level level, BlockGetter chunkCache, BlockPos pos, BlockState state, IDrone drone) {
        AgriApi.harvest(level, pos, drop -> this.spawnEntity(level, pos, drop));
    }

    @Override
    public java.util.List<ItemStack> addFilterItems(Level level, BlockGetter chunkCache, BlockPos pos, BlockState state, IDrone drone) {
        // Native crops publish their harvest through the native runtime, rather
        // than ordinary block-breaking loot. An empty loot table would make the
        // drone reject them even with no programmed item filter.
        if (level instanceof net.minecraft.server.level.ServerLevel server
                && com.agricraft.agricraft.api.compat.ExternalHarvestSupport.inspect(level, pos)
                != com.agricraft.agricraft.api.compat.ExternalHarvestSupport.Availability.UNHANDLED) {
            return com.agricraft.agricraft.compat.starfield.StarfieldCompat.inspect(server, pos)
                    .flatMap(plant -> plant.species().produce())
                    .map(id -> java.util.List.of(new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id))))
                    .orElseGet(java.util.List::of);
        }
        return super.addFilterItems(level, chunkCache, pos, state, drone);
    }

    private void spawnEntity(Level world, BlockPos pos, ItemStack stack) {
        double x = pos.getX() + 0.5 + 0.25 * world.getRandom().nextDouble();
        double y = pos.getY() + 0.5 + 0.25 * world.getRandom().nextDouble();
        double z = pos.getZ() + 0.5 + 0.25 * world.getRandom().nextDouble();
        world.addFreshEntity(new ItemEntity(world, x, y, z, stack));
    }
}
