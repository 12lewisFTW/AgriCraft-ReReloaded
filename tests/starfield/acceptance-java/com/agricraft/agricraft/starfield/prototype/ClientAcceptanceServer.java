package com.agricraft.agricraft.starfield.prototype;

import com.agricraft.agricraft.AgriCraft;
import com.agricraft.agricraft.compat.starfield.*;
import com.stardew.craft.core.ModDimensions;
import com.stardew.craft.farm.*;
import com.stardew.craft.manager.CropGrowthManager;
import com.stardew.craft.time.StardewTimeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.nio.file.*;
import java.util.*;

@EventBusSubscriber(modid="agricraft")
public final class ClientAcceptanceServer {
    private static MinecraftServer server;
    private static ServerLevel valley;
    private static BlockPos base;
    private static final Map<String,Integer> logins=new HashMap<>();
    private static final Set<String> passed=new HashSet<>();
    private static final List<String> evidence=new ArrayList<>();
    private static final List<StarfieldPlantData.Entry> expected=new ArrayList<>();
    private static int phase,ticks,phaseTicks;
    private static int lastLoginTick;
    private static boolean done,initialized;
    private static boolean successful;
    private static final Set<String> NAMES=Set.of("StarfieldProbeA","StarfieldProbeB");
    @SubscribeEvent public static void start(ServerStartedEvent event) {
        if(!Boolean.getBoolean("agricraft.starfield.clientAcceptanceServer"))return;
        server=event.getServer();valley=server.getLevel(ModDimensions.STARDEW_VALLEY);
        StarfieldCompat.refresh(server.overworld());
        var owner=UUID.nameUUIDFromBytes("OfflinePlayer:StarfieldProbeA".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var registry=FarmInstanceRegistry.get(server);var farm=registry.getFarm(owner);
        if(farm==null)farm=registry.createFarm(owner,"StarfieldProbeA","Client acceptance",FarmType.STANDARD);
        farm.markInitialized();farm.setGraceDaysLeft(0);base=farm.getOrigin().offset(64,30,64);
        var secondOwner=UUID.nameUUIDFromBytes("OfflinePlayer:StarfieldProbeB".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var secondFarm=registry.getFarm(secondOwner);if(secondFarm==null)secondFarm=registry.createFarm(secondOwner,"StarfieldProbeB","Client acceptance",FarmType.STANDARD);
        secondFarm.markInitialized();secondFarm.setGraceDaysLeft(0);
        com.stardew.craft.player.PlayerDataManager.get().getOrCreateData(owner).setProfile("StarfieldProbeA","Crops",0);
        com.stardew.craft.player.PlayerDataManager.get().getOrCreateData(secondOwner).setProfile("StarfieldProbeB","Crops",0);
        StardewTimeManager.get().setCurrentSeason(0);
        AgriCraft.LOGGER.info("CLIENT_ACCEPTANCE_READY base={}",base);
    }
    private static void setupBlocks() {
        expected.clear();
        for(int x=-8;x<=12;x++)for(int z=-8;z<=12;z++) {
            var pos=base.offset(x,-1,z);valley.getChunk(pos);valley.setBlock(pos,Blocks.STONE.defaultBlockState(),3);
            for(int y=0;y<=4;y++)valley.setBlock(pos.above(y+1),Blocks.AIR.defaultBlockState(),3);
        }
        var nativeBlocks=new net.minecraft.world.level.block.Block[]{com.stardew.craft.block.ModBlocks.POTATO_CROP.get(),
                com.stardew.craft.block.ModBlocks.GREEN_BEAN_CROP.get(),com.stardew.craft.block.ModBlocks.SPRING_WILD_SEED_CROP.get()};
        for(int i=0;i<3;i++) {
            var root=base.east(i*2);valley.setBlock(root.below(),com.stardew.craft.block.ModBlocks.FARMLAND.get().defaultBlockState().setValue(FarmBlock.MOISTURE,7),3);
            valley.setBlock(root,nativeBlocks[i].defaultBlockState(),3);StarfieldCompat.setSticks(valley,root,List.of("wooden","iron","obsidian").get(i));
            if(i<2) {
                try {
                    var growth=CropGrowthManager.get(valley).getState(valley,root);
                    growth.phase=StarfieldCompat.species(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(nativeBlocks[i])).orElseThrow().nativePhaseDays().size();
                    var method=com.stardew.craft.block.crop.StardewCropBlock.class.getDeclaredMethod("syncVisualStage",ServerLevel.class,BlockPos.class,CropGrowthManager.CropGrowthState.class);
                    method.setAccessible(true);method.invoke(nativeBlocks[i],valley,root,growth);
                } catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
            }
            if(i==2) {
                try {var method=nativeBlocks[i].getClass().getDeclaredMethod("transformToForage",ServerLevel.class,BlockPos.class);method.setAccessible(true);method.invoke(nativeBlocks[i],valley,root);}
                catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
            }
            expected.add(StarfieldCompat.planting(valley,root).orElseThrow());
        }
        AgriCraft.LOGGER.info("CLIENT_ACCEPTANCE_FIXTURE_READY base={}",base);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if(server==null || !(event.getEntity() instanceof ServerPlayer player) || !NAMES.contains(player.getGameProfile().getName()))return;
        logins.merge(player.getGameProfile().getName(),1,Integer::sum);teleport(player,false,false);
        lastLoginTick=ticks;
    }
    private static void teleport(ServerPlayer player,boolean overworld,boolean far) {
        var target=overworld?server.overworld():valley;var pos=camera(player,overworld,far);
        if(overworld){target.getChunk(pos);target.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);}
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        player.setInvisible(true);
        player.getAbilities().flying=true;player.onUpdateAbilities();
        player.teleportTo(target,pos.getX()+.5,pos.getY()+.1,pos.getZ()+.5,Set.of(),
                !overworld && !far && player.getGameProfile().getName().equals("StarfieldProbeB")?139:180,10);
    }
    private static BlockPos camera(ServerPlayer player,boolean overworld,boolean far) {
        return overworld?new BlockPos(player.getGameProfile().getName().equals("StarfieldProbeB")?6:0,80,0)
                :base.offset(far?1024:player.getGameProfile().getName().equals("StarfieldProbeB")?8:2,0,7);
    }
    private static CompoundTag control(ServerPlayer player) {
        var tag=new CompoundTag();tag.putInt("phase",phase);tag.putLong("base",base.asLong());var plants=new ListTag();
        tag.putLong("camera",camera(player,phase==2,phase==5).asLong());
        for(int i=0;i<expected.size();i++){var row=expected.get(i).encode();row.putLong("root",base.east(i*2).asLong());plants.add(row);}tag.put("plants",plants);
        return tag;
    }
    public static void report(ServerPlayer player,CompoundTag tag) {
        if(server==null || done || !NAMES.contains(player.getGameProfile().getName()) || tag.getInt("phase")!=phase)return;
        String text=player.getGameProfile().getName()+" phase="+phase+" "+tag.getString("result");evidence.add(text);AgriCraft.LOGGER.info("CLIENT_ACCEPTANCE {}",text);
        if(!tag.getBoolean("ok")){finish(false,text);return;}
        if(phase==7 && logins.getOrDefault(player.getGameProfile().getName(),0)<2){finish(false,"Reconnect was not a new real login");return;}
        passed.add(player.getGameProfile().getName());
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        if(server==null || event.getServer()!=server)return;
        ticks++;phaseTicks++;
        if(done){if(phaseTicks>20)server.halt(false);return;}
        if(ticks>12000){finish(false,"Client acceptance timed out at phase "+phase);return;}
        if(!initialized) {
            if(server.getPlayerList().getPlayers().stream().map(player->player.getGameProfile().getName()).collect(java.util.stream.Collectors.toSet()).containsAll(NAMES)
                    && ticks-lastLoginTick>100) {
                setupBlocks();initialized=true;phaseTicks=0;
                for(var player:server.getPlayerList().getPlayers())teleport(player,false,false);
            } else return;
        }
        // Native arrival safety can move a visiting player to their own farm after
        // the dimension event. Keep the camera at the test plot once arrival ends.
        if(phaseTicks%20==0)for(var player:server.getPlayerList().getPlayers()) {
            var target=camera(player,phase==2,phase==5);
            var dimension=phase==2?server.overworld().dimension():valley.dimension();
            if(player.level().dimension()!=dimension || player.blockPosition().distSqr(target)>9)teleport(player,phase==2,phase==5);
        }
        if(passed.containsAll(NAMES)) {
            phase++;passed.clear();phaseTicks=0;
            if(phase==1){StarfieldCompat.setSticks(valley,base,"obsidian");expected.set(0,StarfieldCompat.planting(valley,base).orElseThrow());}
            if(phase==2 || phase==3 || phase==5 || phase==6)for(var player:server.getPlayerList().getPlayers())teleport(player,phase==2,phase==5);
            if(phase==4){valley.setBlock(base.east(4),Blocks.AIR.defaultBlockState(),3);expected.remove(2);}
            if(phase==8){finish(true,"Two real clients passed snapshots, change, dimensions, unload and reconnect");return;}
        }
        if(phaseTicks%20==0)for(var player:server.getPlayerList().getPlayers())if(NAMES.contains(player.getGameProfile().getName()))
            PacketDistributor.sendToPlayer(player,new ClientAcceptancePackets.Message(control(player)));
    }
    private static void finish(boolean ok,String result) {
        done=true;successful=ok;phaseTicks=0;
        try {Path output=Path.of(System.getProperty("agricraft.starfield.acceptanceResult"));Files.createDirectories(output.getParent());
            Files.writeString(output,(ok?"PASS":"FAIL")+" "+result+"\n"+String.join("\n",evidence)+"\n",java.nio.charset.StandardCharsets.UTF_8);}
        catch(java.io.IOException failure){throw new IllegalStateException(failure);}
        phase=8;for(var player:server.getPlayerList().getPlayers())PacketDistributor.sendToPlayer(player,new ClientAcceptancePackets.Message(control(player)));
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        if(server==null || event.getServer()!=server || !done)return;
        // Architectury's development classpath readers leave fixed worker pools
        // alive. Match GameTestServer's explicit process exit, but only AFTER
        // Minecraft saves and the entire server thread (including stop hooks) ends.
        var closing=Thread.currentThread();
        var completion=new Thread(()->{
            try {closing.join();System.exit(successful?0:1);}
            catch(InterruptedException interrupted){Thread.currentThread().interrupt();}
        },"AgriCraft acceptance completion");
        completion.setDaemon(true);completion.start();
    }
}
