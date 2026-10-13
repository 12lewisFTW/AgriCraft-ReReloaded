package com.agricraft.agricraft.compat.starfield;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.Optional;

public final class StarfieldPlantClient {
    private static final StarfieldPlantCache CACHE = new StarfieldPlantCache();
    private static Object requestWorld;
    private static long lastRequest = Long.MIN_VALUE;
    private StarfieldPlantClient() {}

    public static void register() {
        if (!StarfieldCompat.isAvailable()) return;
        var bus = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener(StarfieldSticksRenderer::render);
        bus.addListener(StarfieldPlantClient::logout);
        bus.addListener(StarfieldPlantClient::unload);
        bus.addListener(StarfieldPlantClient::unloadChunk);
        com.agricraft.agricraft.client.gui.MagnifyingGlassOverlay.addInspector(StarfieldPlantClient::magnify);
    }

    public static void receive(StarfieldPlantSync.Payload payload, net.minecraft.network.Connection connection,
                               net.minecraft.world.level.Level packetWorld) {
        var minecraft = Minecraft.getInstance();
        var world = minecraft.level;
        if (world == null || world != packetWorld || minecraft.getConnection() == null
                || minecraft.getConnection().getConnection() != connection) return;
        CACHE.bind(world, world.dimension().location());
        if (!world.dimension().location().equals(payload.dimension())) return;
        if (payload.forgetChunk()) CACHE.forget(new ChunkPos(payload.root()));
        else CACHE.apply(payload.session(), payload.dimension(), payload.root(),
                payload.entry() == null || payload.entry().isEmpty() ? Optional.empty()
                        : Optional.of(StarfieldPlantData.Entry.decode(payload.entry())));
    }

    public static Optional<StarfieldPlantData.Entry> get(BlockPos root) {
        var world = Minecraft.getInstance().level;
        if (world == null) { CACHE.clear(); return Optional.empty(); }
        CACHE.bind(world, world.dimension().location());
        return CACHE.get(root);
    }
    public static void forEachSticks(ChunkPos center, int radius,
            java.util.function.BiConsumer<BlockPos, StarfieldPlantData.Entry> render) {
        var world = Minecraft.getInstance().level;
        if (world == null) { CACHE.clear(); return; }
        CACHE.bind(world, world.dimension().location());
        CACHE.forEachSticks(center, radius, render);
    }

    public static void receiveInspection(StarfieldInspectionSync.Response payload, net.minecraft.network.Connection connection,
                                         net.minecraft.world.level.Level packetWorld) {
        var minecraft = Minecraft.getInstance(); var world = minecraft.level;
        if (world == null || world != packetWorld || minecraft.getConnection() == null
                || minecraft.getConnection().getConnection() != connection) return;
        CACHE.bind(world, world.dimension().location());
        CACHE.applyInspection(payload.session(), payload.dimension(), payload.root(), payload.report(), world.getGameTime());
    }
    public static boolean matches(net.minecraft.world.level.Level level, BlockPos pos, net.minecraft.nbt.CompoundTag tag) {
        var root = BlockPos.of(tag.getLong("root"));
        return StarfieldInspection.matches(level, pos, get(root), tag);
    }
    public static Optional<com.agricraft.agricraft.api.tools.magnifying.MagnifyingInspectable> magnify(
            net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.HitResult hit) {
        if (!com.agricraft.agricraft.api.config.CompatConfig.enableStarfield || !(hit instanceof net.minecraft.world.phys.BlockHitResult block)) return Optional.empty();
        var root = block.getBlockPos(); var state = level.getBlockState(root);
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)
                && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)
                == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER) root = root.below();
        var entry = get(root); if (entry.isEmpty()) return Optional.empty();
        if (!net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(root).getBlock()).equals(entry.get().form())) return Optional.empty();
        long now = level.getGameTime();
        if (requestWorld != level) { requestWorld = level; lastRequest = Long.MIN_VALUE; }
        if (lastRequest == Long.MIN_VALUE || now < lastRequest || now - lastRequest >= 10) {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new StarfieldInspectionSync.Request(level.dimension().location(), root, entry.get().plantingId()));
            lastRequest = now;
        }
        var report = CACHE.inspection(root, now).filter(tag -> matches(level, block.getBlockPos(), tag));
        return Optional.of((tooltip, extended) -> tooltip.addAll(report.map(tag -> StarfieldInspection.tooltip(tag, extended))
                .orElseGet(() -> java.util.List.of(net.minecraft.network.chat.Component.translatable("agricraft.starfield.waiting")))));
    }

    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { CACHE.clear(); requestWorld = null; lastRequest = Long.MIN_VALUE; }
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) { CACHE.clear(); requestWorld = null; lastRequest = Long.MIN_VALUE; }
    }
    public static void unloadChunk(ChunkEvent.Unload event) {
        if (event.getLevel().isClientSide()) CACHE.forget(event.getChunk().getPos());
    }
}
