package com.agricraft.agricraft.starfield.prototype;

import com.agricraft.agricraft.api.config.CompatConfig;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public final class FabricLoadingGameTests implements FabricGameTest {
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void ordinarySprinklerStillPaysWatersAndGrowsVanillaWheat(GameTestHelper helper) {
        helper.runAfterDelay(10, () -> {
            var level = helper.getLevel();
            var cropPos = helper.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1)); var sprinklerPos = cropPos.above(3);
            int oldConsumption = com.agricraft.agricraft.api.config.IrrigationConfig.sprinklerWaterConsumption;
            int oldInterval = com.agricraft.agricraft.api.config.IrrigationConfig.sprinklerInterval;
            double oldChance = com.agricraft.agricraft.api.config.IrrigationConfig.sprinklerGrowthChance;
            long oldTime = level.getDayTime();
            try {
                com.agricraft.agricraft.api.config.IrrigationConfig.sprinklerWaterConsumption = 40;
                com.agricraft.agricraft.api.config.IrrigationConfig.sprinklerInterval = 1;
                com.agricraft.agricraft.api.config.IrrigationConfig.sprinklerGrowthChance = 1;
                level.setDayTime(6000);
                level.random.setSeed(246); // Reproducible native wheat growth chance in this scratch test world.
                level.setBlock(cropPos.below(), net.minecraft.world.level.block.Blocks.FARMLAND.defaultBlockState(), 3);
                level.setBlock(cropPos, net.minecraft.world.level.block.Blocks.WHEAT.defaultBlockState(), 3);
                level.setBlock(sprinklerPos.above(), com.agricraft.agricraft.common.registry.ModBlocks.IRRIGATION_CHANNEL.get().defaultBlockState(), 3);
                level.setBlock(sprinklerPos, com.agricraft.agricraft.common.registry.ModBlocks.SPRINKLER.get().defaultBlockState(), 3);
                var channel = (com.agricraft.agricraft.common.block.entity.IrrigationChannelBlockEntity) level.getBlockEntity(sprinklerPos.above());
                var sprinkler = (com.agricraft.agricraft.common.block.entity.SprinklerBlockEntity) level.getBlockEntity(sprinklerPos);
                for (int i = 0; i < 49; i++) com.agricraft.agricraft.common.block.entity.SprinklerBlockEntity.tick(level, sprinklerPos, level.getBlockState(sprinklerPos), sprinkler);
                helper.assertTrue(level.getBlockState(cropPos.below()).getValue(net.minecraft.world.level.block.FarmBlock.MOISTURE) == 0,
                        "Empty ordinary sprinkler supplied free water");
                for (int sweep = 0; sweep < 64; sweep++) {
                    channel.setContent(500);
                    for (int i = 0; i < 49; i++) com.agricraft.agricraft.common.block.entity.SprinklerBlockEntity.tick(level, sprinklerPos, level.getBlockState(sprinklerPos), sprinkler);
                    var saved = sprinkler.saveWithoutMetadata(level.registryAccess());
                    helper.assertTrue(channel.getContent() + saved.getInt("water_buffer") == 402,
                            "Ordinary sprinkler did not pay 98 mB for a sweep");
                    // Empty the carry-over buffer to give each independent sweep the same budget.
                    saved.putInt("water_buffer", 0); sprinkler.loadWithComponents(saved, level.registryAccess());
                }
                helper.assertTrue(level.getBlockState(cropPos.below()).getValue(net.minecraft.world.level.block.FarmBlock.MOISTURE) == 7,
                        "Ordinary sprinkler no longer waters vanilla farmland");
                helper.assertTrue(level.getBlockState(cropPos).getValue(net.minecraft.world.level.block.CropBlock.AGE) > 0,
                        "Ordinary sprinkler no longer gives vanilla crops growth ticks");
                helper.succeed();
            } finally {
                com.agricraft.agricraft.api.config.IrrigationConfig.sprinklerWaterConsumption = oldConsumption;
                com.agricraft.agricraft.api.config.IrrigationConfig.sprinklerInterval = oldInterval;
                com.agricraft.agricraft.api.config.IrrigationConfig.sprinklerGrowthChance = oldChance;
                level.setDayTime(oldTime);
                for (var pos : java.util.List.of(cropPos, cropPos.below(), sprinklerPos, sprinklerPos.above()))
                    level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }
        });
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void loadsWithoutNativeNeoForgeClasses(GameTestHelper helper) {
        helper.assertTrue(!com.agricraft.agricraft.api.compat.ExternalCrossBreedingSupport.supportsSpecies(net.minecraft.resources.ResourceLocation.parse("stardewcraft:potato_crop")),
                "Fabric linked optional native breeding");
        helper.assertTrue(FabricLoader.getInstance().isModLoaded("agricraft"), "AgriCraft did not initialize");
        helper.assertTrue(com.agricraft.agricraft.api.compat.ExternalHarvestSupport.inspect(helper.getLevel(),
                helper.absolutePos(net.minecraft.core.BlockPos.ZERO)) == com.agricraft.agricraft.api.compat.ExternalHarvestSupport.Availability.UNHANDLED,
                "Fabric linked optional native automation");
        helper.assertTrue(!FabricLoader.getInstance().isModLoaded(CompatConfig.STARFIELD_MOD_ID), "Starfield unexpectedly present on Fabric");
        helper.assertTrue(com.agricraft.agricraft.api.compat.ExternalIrrigationSupport.irrigate(helper.getLevel(),
                helper.absolutePos(net.minecraft.core.BlockPos.ZERO), net.minecraft.world.level.block.Blocks.FARMLAND.defaultBlockState())
                        == com.agricraft.agricraft.api.compat.ExternalIrrigationSupport.Result.UNHANDLED,
                "Fabric linked optional native irrigation");
        try {
            Class.forName("com.agricraft.agricraft.compat.starfield.NativeStarfieldCatalog");
            helper.fail("NeoForge native bridge leaked into Fabric");
        } catch (ClassNotFoundException expected) {
            helper.succeed();
        }
    }
}
