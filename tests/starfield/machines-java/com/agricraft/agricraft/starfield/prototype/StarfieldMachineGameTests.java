package com.agricraft.agricraft.starfield.prototype;

import com.agricraft.agricraft.compat.starfield.*;
import com.buuz135.industrial.block.agriculturehusbandry.tile.PlantGathererTile;
import com.mojang.authlib.GameProfile;
import com.stardew.craft.core.ModDimensions;
import com.stardew.craft.farm.*;
import com.stardew.craft.manager.CropGrowthManager;
import me.desht.pneumaticcraft.common.entity.drone.DroneEntity;
import me.desht.pneumaticcraft.common.drone.progwidgets.ProgWidgetHarvest;
import me.desht.pneumaticcraft.common.drone.ai.DroneAIHarvest;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.items.ItemStackHandler;
import java.util.*;
import java.util.function.Consumer;

@GameTestHolder("agricraft")
@PrefixGameTestTemplate(false)
public final class StarfieldMachineGameTests {
    private static final ResourceLocation POTATO=ResourceLocation.parse("stardewcraft:potato_crop");
    private record Plot(ServerLevel level,BlockPos machine,UUID owner){}
    private static Plot plot(GameTestHelper h,String name) {
        var level=h.getLevel().getServer().getLevel(ModDimensions.STARDEW_VALLEY);
        var owner=UUID.nameUUIDFromBytes(("agricraft:machine:"+name).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var registry=FarmInstanceRegistry.get(level.getServer());var farm=registry.getFarm(owner);
        if(farm==null)farm=registry.createFarm(owner,name,"Machine acceptance",FarmType.STANDARD);
        farm.markInitialized();farm.setGraceDaysLeft(0);var pos=farm.getOrigin().offset(64,10,64);level.getChunk(pos);
        return new Plot(level,pos,owner);
    }
    private static StarfieldPlantData.Entry mature(ServerLevel level,BlockPos root) throws ReflectiveOperationException {
        level.getChunk(root);level.setBlock(root,Blocks.AIR.defaultBlockState(),3);
        level.setBlock(root.below(),com.stardew.craft.block.ModBlocks.FARMLAND.get().defaultBlockState().setValue(FarmBlock.MOISTURE,7),3);
        level.setBlock(root,com.stardew.craft.block.ModBlocks.POTATO_CROP.get().defaultBlockState(),3);
        StarfieldCompat.setSticks(level,root,"iron");var old=StarfieldCompat.planting(level,root).orElseThrow();
        var genome=old.genome();genome.getCompound("genes").getCompound("gain").putInt("dom",com.agricraft.agricraft.api.stat.AgriStatRegistry.getInstance().gainStat().getMax());
        var entry=new StarfieldPlantData.Entry(old.plantingId(),old.type(),genome,old.seedTemplate(),old.sticks());
        var put=StarfieldPlantData.class.getDeclaredMethod("put",ServerLevel.class,BlockPos.class,StarfieldPlantData.Entry.class);
        put.setAccessible(true);put.invoke(StarfieldPlantData.get(level),level,root,entry);
        for(int day=1;day<=50 && !StarfieldCompat.inspect(level,root).orElseThrow().mature();day++) {
            level.setBlock(root.below(),level.getBlockState(root.below()).setValue(FarmBlock.MOISTURE,7),3);
            CropGrowthManager.get(level).settleCrops(level,List.of(GlobalPos.of(level.dimension(),root)),day,0,false);
        }
        if(!StarfieldCompat.inspect(level,root).orElseThrow().mature())throw new IllegalStateException("Native machine fixture is immature");return entry;
    }
    private static void check(GameTestHelper h,ServerLevel level,BlockPos root,List<ItemStack> output,StarfieldPlantData.Entry entry) {
        int count=output.stream().mapToInt(ItemStack::getCount).sum();h.assertTrue(count>=2,"Machine lost genetic yield");
        h.assertTrue(!output.isEmpty() && output.stream().allMatch(stack->stack.getOrDefault(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY)
                .copyTag().getCompound(StarfieldGenomes.PRODUCE_KEY).getCompound("genome").equals(entry.genome())),"Machine output lost full inherited genotype");
        h.assertTrue(level.getBlockState(root).getBlock() instanceof com.agricraft.agricraft.common.block.CropBlock
                && StarfieldCompat.planting(level,root).isEmpty() && CropGrowthManager.get(level).getState(level,root)==null,"Machine did not consume native crop once or retain sticks");
    }
    @GameTest(batch="starfield_machines",templateNamespace="agricraft",template="starfield_probe")
    public static void realIndustrialForegoingGathererStoresInheritedNativeHarvest(GameTestHelper h) throws Exception {
        var plot=plot(h,"industrial-gatherer");var block=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("industrialforegoing:plant_gatherer"));
        plot.level.setBlock(plot.machine,block.defaultBlockState(),3);var tile=(PlantGathererTile)plot.level.getBlockEntity(plot.machine);
        var root=tile.getPointedBlockPos();
        try {
            var entry=mature(plot.level,root);tile.getEnergyStorage().receiveEnergy(Integer.MAX_VALUE,false);
            var field=PlantGathererTile.class.getDeclaredField("output");field.setAccessible(true);var inventory=(ItemStackHandler)field.get(tile);
            tile.work();var items=new ArrayList<ItemStack>();for(int slot=0;slot<inventory.getSlots();slot++)if(!inventory.getStackInSlot(slot).isEmpty())items.add(inventory.getStackInSlot(slot).copy());
            check(h,plot.level,root,items,entry);int before=items.stream().mapToInt(ItemStack::getCount).sum();
            for(int i=0;i<100;i++)tile.work();int after=0;for(int slot=0;slot<inventory.getSlots();slot++)after+=inventory.getStackInSlot(slot).getCount();
            h.assertTrue(after==before,"Repeated real gatherer work duplicated crop output");h.succeed();
        } finally {plot.level.setBlock(root,Blocks.AIR.defaultBlockState(),3);plot.level.setBlock(root.below(),Blocks.AIR.defaultBlockState(),3);plot.level.setBlock(plot.machine,Blocks.AIR.defaultBlockState(),3);}
    }
    @GameTest(batch="starfield_machines",templateNamespace="agricraft",template="starfield_probe")
    public static void realPneumaticDroneHarvestAIUsesRegisteredNativeHandlerOnce(GameTestHelper h) throws Exception {
        var plot=plot(h,"pneumatic-drone");var root=plot.machine.east(3);var entry=mature(plot.level,root);
        var owner=FakePlayerFactory.get(plot.level,new GameProfile(plot.owner,"MachineOwner"));
        var drone=new DroneEntity(plot.level,owner);drone.setPos(root.getX()+.5,root.getY()+1,root.getZ()+.5);
        var drops=new ArrayList<ItemEntity>();Consumer<EntityJoinLevelEvent> observer=event->{if(event.getLevel()==plot.level && event.getEntity() instanceof ItemEntity item)drops.add(item);};
        NeoForge.EVENT_BUS.addListener(observer);
        try {
            var widget=new ProgWidgetHarvest();widget.setRequiresTool(false);var ai=new DroneAIHarvest<>(drone,widget);
            var method=DroneAIHarvest.class.getDeclaredMethod("doBlockInteraction",BlockPos.class,double.class);method.setAccessible(true);
            method.invoke(ai,root,1d);check(h,plot.level,root,drops.stream().map(ItemEntity::getItem).toList(),entry);
            int count=drops.size();method.invoke(ai,root,1d);h.assertTrue(drops.size()==count,"Repeated real drone AI duplicated harvest");h.succeed();
        } finally {NeoForge.EVENT_BUS.unregister(observer);drops.forEach(ItemEntity::discard);drone.discard();plot.level.setBlock(root,Blocks.AIR.defaultBlockState(),3);plot.level.setBlock(root.below(),Blocks.AIR.defaultBlockState(),3);}
    }
}
