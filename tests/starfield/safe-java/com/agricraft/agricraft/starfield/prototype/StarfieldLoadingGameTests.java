package com.agricraft.agricraft.starfield.prototype;

import com.agricraft.agricraft.api.config.CompatConfig;
import com.agricraft.agricraft.compat.starfield.StarfieldCompat;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agricraft")
@PrefixGameTestTemplate(false)
public final class StarfieldLoadingGameTests {
    @GameTest(templateNamespace = "agricraft", template = "starfield_probe")
    public static void optionalLoadingModes(GameTestHelper helper) {
        String mode = System.getProperty("agricraft.starfield.testMode");
        boolean installed = ModList.get().isLoaded(CompatConfig.STARFIELD_MOD_ID);
        helper.assertTrue(installed == !mode.equals("absent"), "Runtime mod presence differs from the test mode");
        boolean available = installed && !mode.equals("incompatible");
        helper.assertTrue(StarfieldCompat.isAvailable() == available, "Dependency availability differs from the actual runtime");
        boolean seedHook = java.util.Arrays.stream(ItemStack.class.getDeclaredMethods())
                .anyMatch(method -> method.getName().contains("agricraft$nativeSeed"));
        helper.assertTrue(seedHook == available, "Optional ItemStack mixin applied without a verified native dependency");
        if (!available) {
            try {
                var bus = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
                var field = bus.getClass().getDeclaredField("listeners");field.setAccessible(true);
                var listeners = (java.util.Map<?, ?>) field.get(bus);
                helper.assertTrue(listeners.keySet().stream().noneMatch(owner ->
                                (owner instanceof Class<?> type ? type : owner.getClass().getNestHost()).getName()
                                        .startsWith("com.agricraft.agricraft.compat.starfield.")),
                        "Absent/incompatible integration registered gameplay event listeners");
            } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
        }
        if (mode.equals("absent")) {
            try {
                Class.forName("com.stardew.craft.api.v1.agriculture.StardewCropRuntime", false,
                        StarfieldLoadingGameTests.class.getClassLoader());
                helper.fail("Compile-only Starfield classes leaked onto the runtime classpath");
            } catch (ClassNotFoundException expectedAbsence) {
                // The optional binary itself is absent, not merely missing from ModList.
            }
        }
        helper.assertTrue(CompatConfig.enableStarfield == !mode.equals("disabled"), "The real configuration was not applied");
        var expected = switch (mode) {
            case "enabled" -> StarfieldCompat.Status.READY;
            case "disabled" -> StarfieldCompat.Status.DISABLED;
            case "incompatible" -> StarfieldCompat.Status.INCOMPATIBLE;
            default -> StarfieldCompat.Status.ABSENT;
        };
        helper.assertTrue(StarfieldCompat.status() == expected, "Expected " + expected + ", got " + StarfieldCompat.status());
        if (!mode.equals("enabled")) {
            helper.assertTrue(com.agricraft.agricraft.api.compat.ExternalHarvestSupport.inspect(helper.getLevel(),
                    helper.absolutePos(net.minecraft.core.BlockPos.ZERO)) == com.agricraft.agricraft.api.compat.ExternalHarvestSupport.Availability.UNHANDLED,
                    "Inactive integration exposed automation harvest");
            var nativeOutput = new ItemStack(Items.WHEAT, 3);
            helper.assertTrue(StarfieldCompat.automation(helper.getLevel(), net.minecraft.core.BlockPos.ZERO,
                    stack -> {}, sink -> nativeOutput) == nativeOutput,
                    "Inactive automation facade changed original output");
            var plan = java.util.Optional.of("native plan");
            helper.assertTrue(StarfieldCompat.production(new Object(), nativeOutput, plan) == plan,
                    "Inactive production facade changed native plan");
            helper.assertTrue(!com.agricraft.agricraft.api.compat.ExternalCrossBreedingSupport.supportsSpecies(ResourceLocation.parse("stardewcraft:potato_crop"))
                            && !com.agricraft.agricraft.api.compat.ExternalCrossBreedingSupport.supportsSticks(helper.getLevel(), helper.absolutePos(net.minecraft.core.BlockPos.ZERO)),
                    "Inactive integration exposed native breeding/soil support");
            helper.assertTrue(com.agricraft.agricraft.api.compat.ExternalIrrigationSupport.irrigate(
                    helper.getLevel(), helper.absolutePos(net.minecraft.core.BlockPos.ZERO), Blocks.FARMLAND.defaultBlockState())
                            == com.agricraft.agricraft.api.compat.ExternalIrrigationSupport.Result.UNHANDLED,
                    "Inactive integration exposed native irrigation");
            helper.assertTrue(StarfieldCompat.inspection(helper.getLevel(), helper.absolutePos(net.minecraft.core.BlockPos.ZERO)).isEmpty()
                            && !com.agricraft.agricraft.api.compat.ExternalSeedSupport.accepts(new ItemStack(Items.WHEAT_SEEDS), helper.getLevel()),
                    "Inactive integration exposed optional seed/inspection operations");
            var originalCalls = new java.util.concurrent.atomic.AtomicInteger();
            StarfieldCompat.harvest(helper.getLevel(), helper.absolutePos(net.minecraft.core.BlockPos.ZERO),
                    null, originalCalls::incrementAndGet);
            helper.assertTrue(originalCalls.get() == 1, "Inactive harvest facade did not preserve the original operation");
            helper.assertTrue(StarfieldCompat.species().isEmpty()
                            && StarfieldCompat.species(ResourceLocation.parse("stardewcraft:potato_crop")).isEmpty()
                            && StarfieldCompat.seed(new ItemStack(Items.WHEAT_SEEDS)).isEmpty()
                            && StarfieldCompat.soil(Blocks.FARMLAND.defaultBlockState()).isEmpty(),
                    "Inactive compatibility exposes crop/seed/soil data");
        }
        var entity = new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(), 0, 0, 0, new ItemStack(Items.DIAMOND));
        var entityCalls = new java.util.concurrent.atomic.AtomicInteger();
        helper.assertTrue(com.agricraft.agricraft.compat.starfield.StarfieldHarvestTransaction.addEntity(
                helper.getLevel(), entity, () -> { entityCalls.incrementAndGet(); return true; }) && entityCalls.get() == 1,
                "An ordinary entity operation was captured outside harvest");
        helper.assertTrue(com.agricraft.agricraft.compat.starfield.StarfieldHarvestTransaction.awardExperience(() -> true),
                "An ordinary XP operation was captured outside harvest");
        helper.assertTrue(!StarfieldCompat.isVerifiedVersion("0.6.3")
                        && !StarfieldCompat.isVerifiedVersion("0.7.0")
                        && !StarfieldCompat.isVerifiedVersion("0.6.3fix1-unverified"),
                "Unknown versions must not link the native bridge");
        helper.succeed();
    }
    @GameTest(templateNamespace = "agricraft", template = "starfield_probe")
    public static void ordinarySprinklerStillPaysWatersAndGrowsVanillaWheat(GameTestHelper helper) {
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

}
