package com.agricraft.agricraft.starfield.prototype;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid="agricraft",bus=EventBusSubscriber.Bus.MOD)
public final class ClientAcceptancePackets {
    public record Message(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Message> TYPE=new Type<>(ResourceLocation.parse("agricraft:acceptance_probe"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Message> CODEC=new StreamCodec<>() {
            public Message decode(RegistryFriendlyByteBuf buf){return new Message(buf.readNbt());}
            public void encode(RegistryFriendlyByteBuf buf,Message value){buf.writeNbt(value.data);}
        };
        public Type<Message> type(){return TYPE;}
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("acceptance1").playBidirectional(Message.TYPE,Message.CODEC,(packet,context)->{
            if(context.player() instanceof net.minecraft.server.level.ServerPlayer player) ClientAcceptanceServer.report(player,packet.data());
            else ClientAcceptanceClient.control(packet.data());
        });
    }
}
