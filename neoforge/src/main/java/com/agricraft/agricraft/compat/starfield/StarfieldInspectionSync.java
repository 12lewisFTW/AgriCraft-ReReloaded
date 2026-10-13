package com.agricraft.agricraft.compat.starfield;

import com.agricraft.agricraft.common.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

/** Read-only request: no client genome, progress, or menu edits are accepted. */
public final class StarfieldInspectionSync {
    private static final HashMap<UUID, Integer> requests = new HashMap<>();
    private StarfieldInspectionSync() {}
    public record Request(ResourceLocation dimension, BlockPos pos, UUID planting) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.parse("agricraft:starfield_inspect"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = new StreamCodec<>() {
            public Request decode(RegistryFriendlyByteBuf buf) { return new Request(buf.readResourceLocation(), buf.readBlockPos(), buf.readUUID()); }
            public void encode(RegistryFriendlyByteBuf buf, Request value) { buf.writeResourceLocation(value.dimension); buf.writeBlockPos(value.pos); buf.writeUUID(value.planting); }
        };
        public Type<Request> type() { return TYPE; }
    }
    public record Response(UUID session, ResourceLocation dimension, BlockPos root, CompoundTag report) implements CustomPacketPayload {
        public static final Type<Response> TYPE = new Type<>(ResourceLocation.parse("agricraft:starfield_inspection"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Response> CODEC = new StreamCodec<>() {
            public Response decode(RegistryFriendlyByteBuf buf) { return new Response(buf.readUUID(), buf.readResourceLocation(), buf.readBlockPos(), buf.readNbt()); }
            public void encode(RegistryFriendlyByteBuf buf, Response value) { buf.writeUUID(value.session); buf.writeResourceLocation(value.dimension); buf.writeBlockPos(value.root); buf.writeNbt(value.report); }
        };
        public Type<Response> type() { return TYPE; }
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(Request.TYPE, Request.CODEC, (payload, context) -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            int tick = player.getServer().getTickCount();
            if (tick - requests.getOrDefault(player.getUUID(), tick - 10) < 10) return;
            requests.put(player.getUUID(), tick);
            inspect(player, payload, true).ifPresent(report -> PacketDistributor.sendToPlayer(player,
                    new Response(StarfieldPlantSync.session(), payload.dimension(), BlockPos.of(report.getLong("root")), report)));
        });
        registrar.playToClient(Response.TYPE, Response.CODEC,
                (payload, context) -> StarfieldPlantClient.receiveInspection(payload, context.connection(), context.player().level()));
    }
    public static boolean withinReach(ServerPlayer player, BlockPos pos) {
        return player.isAlive() && player.serverLevel().hasChunkAt(pos) && player.canInteractWithBlock(pos, 0);
    }
    public static Optional<CompoundTag> inspect(ServerPlayer player, Request request, boolean requireMagnifier) {
        if (!player.level().dimension().location().equals(request.dimension) || !withinReach(player, request.pos)
                || requireMagnifier && !holdsMagnifier(player)) return Optional.empty();
        return StarfieldCompat.inspection(player.serverLevel(), request.pos).filter(tag ->
                tag.getCompound("entry").getUUID("plantingId").equals(request.planting));
    }
    public static Optional<CompoundTag> inspect(ServerPlayer player, BlockPos pos) {
        return withinReach(player, pos) ? StarfieldCompat.inspection(player.serverLevel(), pos) : Optional.empty();
    }
    private static boolean holdsMagnifier(ServerPlayer player) {
        return player.getMainHandItem().is(ModItems.MAGNIFYING_GLASS.get()) || player.getOffhandItem().is(ModItems.MAGNIFYING_GLASS.get())
                || player.getItemBySlot(EquipmentSlot.HEAD).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("magnifying");
    }
    static void reset() { requests.clear(); }
    static void logout(ServerPlayer player) { requests.remove(player.getUUID()); }
}
