package com.agricraft.agricraft.compat.starfield;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class StarfieldPlantSync {
    private static UUID session = UUID.randomUUID();
    private static final Map<GlobalPos, Set<ServerPlayer>> sentChunks = new HashMap<>();
    private StarfieldPlantSync() {}

    public record Payload(UUID session, ResourceLocation dimension, BlockPos root,
                          boolean forgetChunk, CompoundTag entry) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(ResourceLocation.parse("agricraft:starfield_plant"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = new StreamCodec<>() {
            @Override public Payload decode(RegistryFriendlyByteBuf buf) {
                return new Payload(buf.readUUID(), buf.readResourceLocation(), buf.readBlockPos(), buf.readBoolean(), buf.readNbt());
            }
            @Override public void encode(RegistryFriendlyByteBuf buf, Payload value) {
                buf.writeUUID(value.session); buf.writeResourceLocation(value.dimension); buf.writeBlockPos(value.root);
                buf.writeBoolean(value.forgetChunk); buf.writeNbt(value.entry);
            }
        };
        @Override public Type<Payload> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        StarfieldInspectionSync.register(event);
        event.registrar("1").playToClient(Payload.TYPE, Payload.CODEC,
                (payload, context) -> StarfieldPlantClient.receive(payload, context.connection(), context.player().level()));
    }

    public static UUID session() { return session; }
    public static void reset() { sentChunks.clear(); session = UUID.randomUUID(); StarfieldInspectionSync.reset(); }
    public static void logout(ServerPlayer player) {
        StarfieldInspectionSync.logout(player);
        sentChunks.values().forEach(players -> players.remove(player));
        sentChunks.values().removeIf(Set::isEmpty);
    }
    private static GlobalPos chunkKey(ServerLevel level, ChunkPos chunk) {
        return GlobalPos.of(level.dimension(), chunk.getWorldPosition());
    }

    public static void sent(ChunkWatchEvent.Sent event) {
        if (!StarfieldCompat.isActive()) return;
        StarfieldCompat.scan(event.getLevel(), event.getChunk());
        var key = chunkKey(event.getLevel(), event.getPos());
        sentChunks.computeIfAbsent(key, ignored -> new HashSet<>()).add(event.getPlayer());
        // Reset this chunk before a full snapshot, including the empty snapshot case.
        PacketDistributor.sendToPlayer(event.getPlayer(), new Payload(session, event.getLevel().dimension().location(),
                event.getPos().getWorldPosition(), true, new CompoundTag()));
        StarfieldPlantData.get(event.getLevel()).inChunk(event.getLevel(), event.getPos()).forEach((position, entry) ->
                PacketDistributor.sendToPlayer(event.getPlayer(), payload(event.getLevel(), position.pos(), Optional.of(entry))));
    }

    public static void unwatch(ChunkWatchEvent.UnWatch event) {
        var players = sentChunks.get(chunkKey(event.getLevel(), event.getPos()));
        if (players == null) return;
        if (players.remove(event.getPlayer())) {
            // A large teleport can move ClientChunkCache's range before its
            // vanilla forget packet arrives. Out-of-range drop() emits no
            // client ChunkEvent.Unload, so clear our sidecar explicitly.
            PacketDistributor.sendToPlayer(event.getPlayer(), new Payload(session, event.getLevel().dimension().location(),
                    event.getPos().getWorldPosition(), true, new CompoundTag()));
        }
        if (players.isEmpty()) sentChunks.remove(chunkKey(event.getLevel(), event.getPos()));
    }

    static Payload payload(ServerLevel level, BlockPos root, Optional<StarfieldPlantData.Entry> entry) {
        return new Payload(session, level.dimension().location(), root.immutable(), false,
                entry.map(StarfieldPlantData.Entry::encode).orElseGet(CompoundTag::new));
    }

    public static void changed(ServerLevel level, BlockPos root, Optional<StarfieldPlantData.Entry> entry) {
        var players = sentChunks.get(chunkKey(level, new ChunkPos(root)));
        if (players != null) for (var player : players) PacketDistributor.sendToPlayer(player, payload(level, root, entry));
    }
}
