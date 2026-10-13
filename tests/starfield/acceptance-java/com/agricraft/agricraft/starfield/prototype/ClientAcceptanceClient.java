package com.agricraft.agricraft.starfield.prototype;

import com.agricraft.agricraft.compat.starfield.StarfieldPlantClient;
import com.agricraft.agricraft.compat.starfield.StarfieldPlantData;
import com.agricraft.agricraft.common.util.PlatformClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid="agricraft",value=Dist.CLIENT)
public final class ClientAcceptanceClient {
    private static CompoundTag instruction;
    private static int phase=-1,frames,ticks,reported=-1,reconnectTicks;
    private static Object initialConnection;
    private static boolean reconnected;
    public static void control(CompoundTag tag) {
        int next=tag.getInt("phase");
        if(next!=phase){phase=next;frames=0;ticks=0;}
        instruction=tag.copy();
    }
    @SubscribeEvent public static void frame(RenderLevelStageEvent event) {
        if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_LEVEL)frames++;
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(System.getProperty("agricraft.starfield.acceptanceClient")==null)return;
        var mc=Minecraft.getInstance();mc.options.renderDistance().set(4);mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;
        if(instruction==null) {
            if(mc.getOverlay()==null && mc.getConnection()==null && !(mc.screen instanceof ConnectScreen)) {
                String address=System.getProperty("agricraft.starfield.acceptanceAddress");
                ConnectScreen.startConnecting(new TitleScreen(),mc,ServerAddress.parseString(address),new ServerData("Acceptance",address,ServerData.Type.OTHER),false,null);
            }
            return;
        }
        ticks++;
        if(phase==8){mc.stop();return;}
        if(phase==7 && !reconnected && mc.getConnection()!=null) {
            reconnected=true;initialConnection=mc.getConnection().getConnection();mc.disconnect(new TitleScreen());reconnectTicks=0;return;
        }
        if(phase==7 && mc.getConnection()==null && reconnected) {
            if(++reconnectTicks==20){String address=System.getProperty("agricraft.starfield.acceptanceAddress");
                ConnectScreen.startConnecting(new TitleScreen(),mc,ServerAddress.parseString(address),new ServerData("Acceptance",address,ServerData.Type.OTHER),false,null);}
            return;
        }
        if(mc.level==null || mc.player==null || mc.getConnection()==null)return;
        // Native onboarding can enqueue several screens. Capture only after a
        // full set of unobstructed world frames has actually been rendered.
        if(mc.screen!=null){mc.setScreen(null);frames=0;return;}
        if(reported==phase || frames<20)return;
        if(ticks%100==0)com.agricraft.agricraft.AgriCraft.LOGGER.info("CLIENT_ACCEPTANCE_WAIT phase={} position={} dimension={} rootCached={}",phase,mc.player.blockPosition(),mc.level.dimension(),StarfieldPlantClient.get(BlockPos.of(instruction.getLong("base"))).isPresent());
        try {
            var base=BlockPos.of(instruction.getLong("base"));boolean empty=phase==2 || phase==5;
            if(mc.player.blockPosition().distSqr(BlockPos.of(instruction.getLong("camera")))>9)return;
            if(phase==2 && !mc.level.dimension().location().equals(ResourceLocation.parse("minecraft:overworld")))return;
            if(phase!=2 && !mc.level.dimension().location().equals(ResourceLocation.parse("stardewcraft:stardew_valley")))return;
            var plants=instruction.getList("plants",Tag.TAG_COMPOUND);
            for(int i=0;i<plants.size();i++) {
                var row=plants.getCompound(i);var root=BlockPos.of(row.getLong("root"));var cached=StarfieldPlantClient.get(root);
                if(empty) {if(cached.isPresent() || phase==5 && mc.level.getChunkSource().hasChunk(root.getX()>>4,root.getZ()>>4))return;}
                else if(cached.isEmpty() || !cached.get().encode().equals(StarfieldPlantData.Entry.decode(row).encode()))return;
                if(!empty) {
                    // Loaded blocks and sidecars arrive before asynchronous terrain
                    // meshes. Require unobstructed frames AFTER section compilation,
                    // so return-from-dimension screenshots contain the native crop.
                    if(!mc.levelRenderer.isSectionCompiled(root)){frames=0;return;}
                    var model=PlatformClient.get().getStandaloneModel(ResourceLocation.parse("agricraft:block/"+cached.get().sticks()+"_crop_sticks"));
                    if(model==null || model==mc.getModelManager().getMissingModel())throw new IllegalStateException("Missing stick model");
                }
            }
            if(phase>=4 && !empty && StarfieldPlantClient.get(base.east(4)).isPresent())return;
            if(phase==7 && mc.getConnection().getConnection()==initialConnection)return;
            if(phase==0 || phase==3)Screenshot.grab(mc.gameDirectory,System.getProperty("agricraft.starfield.acceptanceClient")+"-phase"+phase+".png",mc.getMainRenderTarget(),component->{});
            var tag=new CompoundTag();tag.putInt("phase",phase);tag.putBoolean("ok",true);tag.putString("result","real connection/cache, compiled native scene and baked models validated; frames="+frames);
            PacketDistributor.sendToServer(new ClientAcceptancePackets.Message(tag));reported=phase;
        } catch(RuntimeException failure) {
            var tag=new CompoundTag();tag.putInt("phase",phase);tag.putBoolean("ok",false);tag.putString("result",failure.toString());
            PacketDistributor.sendToServer(new ClientAcceptancePackets.Message(tag));reported=phase;
        }
    }
}
