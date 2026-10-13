package com.agricraft.agricraft.compat.starfield;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public final class StarfieldJadePlugin implements IWailaPlugin {
    private static final Provider PROVIDER = new Provider();
    @Override public void register(IWailaCommonRegistration registration) {
        if (StarfieldCompat.isAvailable()) registration.registerBlockDataProvider(PROVIDER, Block.class);
    }
    @Override public void registerClient(IWailaClientRegistration registration) {
        if (StarfieldCompat.isAvailable()) registration.registerBlockComponent(PROVIDER, Block.class);
    }
    public static final class Provider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        @Override public ResourceLocation getUid() { return ResourceLocation.parse("agricraft:starfield_inspection"); }
        @Override public boolean shouldRequestData(BlockAccessor accessor) {
            if (!com.agricraft.agricraft.api.config.CompatConfig.enableStarfield) return false;
            if (!accessor.getLevel().isClientSide()) return StarfieldCompat.inspect(accessor.getLevel(), accessor.getPosition()).isPresent();
            var root = accessor.getPosition(); var state = accessor.getBlockState();
            if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)
                    && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)
                    == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER) root = root.below();
            return StarfieldPlantClient.get(root).filter(entry -> entry.form().equals(
                    net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(accessor.getBlock()))).isPresent();
        }
        @Override public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getPlayer() instanceof ServerPlayer player)
                StarfieldInspectionSync.inspect(player, accessor.getPosition()).ifPresent(tag -> data.put("agricraft:starfield_inspection", tag));
        }
        @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            var tag = accessor.getServerData().getCompound("agricraft:starfield_inspection");
            if (com.agricraft.agricraft.api.config.CompatConfig.enableStarfield && !tag.isEmpty()
                    && StarfieldPlantClient.matches(accessor.getLevel(), accessor.getPosition(), tag))
                StarfieldInspection.tooltip(tag, accessor.getPlayer().isShiftKeyDown()).forEach(tooltip::add);
        }
    }
}
