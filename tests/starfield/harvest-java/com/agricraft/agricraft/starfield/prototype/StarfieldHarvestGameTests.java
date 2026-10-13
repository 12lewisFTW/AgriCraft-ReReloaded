package com.agricraft.agricraft.starfield.prototype;

import com.agricraft.agricraft.api.config.CompatConfig;
import com.agricraft.agricraft.common.registry.ModItems;
import com.agricraft.agricraft.compat.starfield.StarfieldCompat;
import com.mojang.authlib.GameProfile;
import com.stardew.craft.api.v1.agriculture.StardewCropRuntime;
import com.stardew.craft.block.FertilizerType;
import com.stardew.craft.core.ModDimensions;
import com.stardew.craft.event.ScytheHarvestEvents;
import com.stardew.craft.farm.FarmInstanceRegistry;
import com.stardew.craft.farm.FarmType;
import com.stardew.craft.item.tool.ScytheItem;
import com.stardew.craft.manager.CropGrowthManager;
import com.stardew.craft.manager.FertilizerManager;
import com.stardew.craft.player.PlayerStardewDataAPI;
import com.stardew.craft.player.SkillType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/** Compare actual native player harvests, including native item components and XP. */
@GameTestHolder("agricraft")
@PrefixGameTestTemplate(false)
public final class StarfieldHarvestGameTests {
    private record Plot(ServerLevel level, ServerPlayer player, BlockPos plain, BlockPos sticks,
                        java.util.ArrayList<ItemEntity> drops, Consumer<EntityJoinLevelEvent> observer) {
        List<BlockPos> roots() { return List.of(plain, sticks); }
    }

    @GameTest(batch = "starfield_harvest", templateNamespace = "agricraft", template = "starfield_probe")
    public static void qualityExperienceAndQuantityMatchUnmodifiedNativeHarvest(GameTestHelper helper) {
        var plot = plot(helper, "quality");
        var fertilizers = new FertilizerType[]{null, FertilizerType.BASIC_FERTILIZER, FertilizerType.QUALITY_FERTILIZER, FertilizerType.DELUXE_FERTILIZER};
        try {
            for (int skillExp : new int[]{0, 5000}) for (int i = 0; i < fertilizers.length; i++) {
                plant(helper, plot, "potato", 0, "wooden");
                if (fertilizers[i] != null) for (var root : plot.roots()) helper.assertTrue(
                        FertilizerManager.get(plot.level).tryApplyFertilizer(plot.level, root.below(), fertilizers[i]), "Fertilizer rejected");
                mature(helper, plot, 0);
                setXp(plot, skillExp); plot.level.random.setSeed(1701 + i);
                // Real unmodified native path provides the control, including quality components.
                CompatConfig.enableStarfield = false;
                try { helper.assertTrue(hand(plot, plot.plain), "Native control did not harvest"); }
                finally { CompatConfig.enableStarfield = true; }
                var expected = outputs(plot, plot.plain); helper.assertTrue(!expected.isEmpty(), "Native control produced no items"); int xp = xp(plot) - skillExp; clearDrops(plot);
                setXp(plot, skillExp); plot.level.random.setSeed(1701 + i);
                helper.assertTrue(hand(plot, plot.sticks), "Attached root did not harvest");
                helper.assertTrue(outputs(plot, plot.sticks).equals(expected), "Quantity/quality/components differ from native control");
                helper.assertTrue(xp(plot) - skillExp == xp && xp == 14, "Native potato XP changed");
                int count = outputs(plot, plot.sticks).size(); int once = xp(plot);
                for (int click = 0; click < 3; click++) helper.assertTrue(!hand(plot, plot.sticks), "Repeated harvest succeeded");
                helper.assertTrue(xp(plot) == once && outputs(plot, plot.sticks).size() == count, "Repeated click duplicated items/XP");
                for (var root : plot.roots()) helper.assertTrue((root.equals(plot.plain) ? plot.level.getBlockState(root).isAir()
                        : plot.level.getBlockState(root).getBlock() instanceof com.agricraft.agricraft.common.block.CropBlock
                        && plot.level.getBlockState(root).getValue(com.agricraft.agricraft.common.block.CropBlock.STICK_VARIANT)
                        == com.agricraft.agricraft.common.block.CropStickVariant.WOODEN)
                        && StarfieldCompat.planting(plot.level, root).isEmpty(), "One-shot harvest retained crop/identity");
                cleanup(plot);
            }
            helper.succeed();
        } finally { CompatConfig.enableStarfield = true; close(plot); }
    }

    @GameTest(batch = "starfield_harvest", templateNamespace = "agricraft", template = "starfield_probe")
    public static void regrowthUsesNativeDaysAndPreservesBothPartsAndIdentity(GameTestHelper helper) {
        var plot = plot(helper, "regrowth");
        try {
            for (var species : List.of("broccoli", "corn")) {
                int season = species.equals("corn") ? 1 : 2;
                plant(helper, plot, species, season, "obsidian"); mature(helper, plot, season);
                var identity = StarfieldCompat.planting(plot.level, plot.sticks).orElseThrow();
                for (var root : plot.roots()) helper.assertTrue(hand(plot, root), "Initial harvest failed");
                int days = StarfieldCompat.species(ResourceLocation.parse("stardewcraft:" + species + "_crop")).orElseThrow().nativeRegrowDays();
                helper.assertTrue(days == 4 && CropGrowthManager.get(plot.level).getState(plot.level, plot.sticks).regrowing, "Native regrow countdown missing");
                int once = xp(plot); helper.assertTrue(!hand(plot, plot.sticks) && xp(plot) == once, "Immediate regrow harvest duplicated XP");
                for (int day = 1; day <= days; day++) {
                    settle(plot, 100 + day, season);
                    for (var root : plot.roots()) helper.assertTrue(StarfieldCompat.inspect(plot.level, root).orElseThrow().mature() == (day == days), "Regrow matured on wrong day");
                }
                helper.assertTrue(StarfieldCompat.planting(plot.level, plot.sticks).orElseThrow().equals(identity), "Regrowth changed UUID/genome/sticks");
                if (species.equals("corn")) {
                    helper.assertTrue(StarfieldCompat.inspect(plot.level, plot.sticks.above()).orElseThrow().root().equals(plot.sticks), "Tall crop lost upper part");
                    helper.assertTrue(hand(plot, plot.sticks.above()) && !hand(plot, plot.sticks), "Upper and root produced two harvests");
                } else helper.assertTrue(hand(plot, plot.sticks), "Second harvest failed");
                cleanup(plot);
            }
            helper.succeed();
        } finally { close(plot); }
    }

    @GameTest(batch = "starfield_harvest", templateNamespace = "agricraft", template = "starfield_probe")
    public static void nativeScytheAndFarmPermissionsRemainAuthoritative(GameTestHelper helper) {
        var plot = plot(helper, "scythe");
        try {
            plant(helper, plot, "wheat", 1, "iron"); mature(helper, plot, 1);
            helper.assertTrue(!hand(plot, plot.sticks) && outputs(plot, plot.sticks).isEmpty(), "Required scythe bypassed by hand");
            var foreign = FakePlayerFactory.get(plot.level, new GameProfile(UUID.nameUUIDFromBytes("agricraft:harvest:foreign".getBytes(StandardCharsets.UTF_8)), "HarvestForeign"));
            var tool = new ItemStack(com.stardew.craft.item.ModItems.SCYTHE.get());
            foreign.setItemInHand(InteractionHand.MAIN_HAND, tool.copy()); stand(foreign, plot.sticks);
            helper.assertTrue(!ScytheHarvestEvents.harvestSwing(plot.level, foreign, (ScytheItem) tool.getItem()), "Foreign farm scythe harvest succeeded");
            helper.assertTrue(!StardewCropRuntime.harvestForPlayer(foreign, plot.sticks, InteractionHand.MAIN_HAND, true, false).harvested(), "Runtime bypassed farm permission");
            plot.player.setItemInHand(InteractionHand.MAIN_HAND, tool); stand(plot.player, plot.plain);
            int before = xp(plot);
            helper.assertTrue(ScytheHarvestEvents.harvestSwing(plot.level, plot.player, (ScytheItem) tool.getItem()), "Native owner scythe failed");
            helper.assertTrue(plot.level.getBlockState(plot.plain).isAir()
                    && plot.level.getBlockState(plot.sticks).getBlock() instanceof com.agricraft.agricraft.common.block.CropBlock
                    && plot.level.getBlockState(plot.sticks).getValue(com.agricraft.agricraft.common.block.CropBlock.STICK_VARIANT)
                    == com.agricraft.agricraft.common.block.CropStickVariant.IRON
                    && plot.roots().stream().allMatch(root -> StarfieldCompat.planting(plot.level, root).isEmpty()), "Scythe did not harvest both roots or lost sticks");
            helper.assertTrue(xp(plot) > before && !ScytheHarvestEvents.harvestSwing(plot.level, plot.player, (ScytheItem) tool.getItem()), "Scythe repeat produced harvest");
            helper.assertTrue(!tool.isDamageableItem() && tool.getCount() == 1, "Compatibility changed native scythe durability/consumption");
            plant(helper, plot, "potato", 0, "iron"); mature(helper, plot, 0);
            helper.assertTrue(!StardewCropRuntime.harvestForPlayer(foreign, plot.sticks, InteractionHand.MAIN_HAND, false, false).harvested(), "Foreign hand harvest succeeded");
            helper.succeed();
        } finally { close(plot); }
    }

    @GameTest(batch = "starfield_harvest", templateNamespace = "agricraft", template = "starfield_probe")
    public static void agriToolActionsCannotFallThroughToHandHarvest(GameTestHelper helper) {
        var plot = plot(helper, "tools");
        try {
            plant(helper, plot, "potato", 0, "wooden"); mature(helper, plot, 0);
            var before = snapshot(plot, plot.sticks); int xp = xp(plot);
            for (var item : List.of(ModItems.CLIPPER.get(), ModItems.MAGNIFYING_GLASS.get(), ModItems.WOODEN_RAKE.get(),
                    ModItems.IRON_RAKE.get(), ModItems.TROWEL.get(), ModItems.WOODEN_CROP_STICKS.get(),
                    ModItems.IRON_CROP_STICKS.get(), ModItems.OBSIDIAN_CROP_STICKS.get(), ModItems.SEED_BAG.get())) {
                var stack = new ItemStack(item); plot.player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                var hit = new BlockHitResult(Vec3.atCenterOf(plot.sticks), Direction.UP, plot.sticks, false);
                helper.assertTrue(plot.level.getBlockState(plot.sticks).useItemOn(stack, plot.level, plot.player, InteractionHand.MAIN_HAND, hit)
                        == ItemInteractionResult.FAIL, "Agri tool accidentally harvested: " + item);
                helper.assertTrue(snapshot(plot, plot.sticks).equals(before) && xp(plot) == xp && outputs(plot, plot.sticks).isEmpty(), "Tool fallback changed crop/items/XP");
            }
            plot.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            helper.assertTrue(hand(plot, plot.sticks), "Tool refusal disabled ordinary harvest"); helper.succeed();
        } finally { close(plot); }
    }

    @GameTest(batch = "starfield_harvest", templateNamespace = "agricraft", template = "starfield_probe")
    public static void fullInventoryKeepsNativeWorldDropsAndSingleXpAward(GameTestHelper helper) {
        var plot = plot(helper, "inventory");
        try {
            plant(helper, plot, "potato", 0, "wooden"); mature(helper, plot, 0);
            for (int slot = 0; slot < plot.player.getInventory().getContainerSize(); slot++) plot.player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
            int before = xp(plot); helper.assertTrue(hand(plot, plot.sticks), "Full inventory prevented native world-drop harvest");
            helper.assertTrue(!outputs(plot, plot.sticks).isEmpty() && xp(plot) == before + 14 && !hand(plot, plot.sticks), "Full inventory lost/duplicated harvest");
            helper.succeed();
        } finally { plot.player.getInventory().clearContent(); close(plot); }
    }

    @GameTest(batch = "starfield_harvest", templateNamespace = "agricraft", template = "starfield_probe")
    public static void rejectedAndThrowingItemCreationRollBackPublishedDropsCropAndXp(GameTestHelper helper) {
        var plot = plot(helper, "rollback");
        try {
            // Find an actual native potato roll with multiple item entities, so failure of the
            // second item also verifies removal of the already published first item.
            long seed = -1;
            for (int attempt = 0; attempt < 64 && seed < 0; attempt++) {
                plant(helper, plot, "potato", 0, "iron"); mature(helper, plot, 0); plot.level.random.setSeed(attempt);
                helper.assertTrue(hand(plot, plot.sticks), "Probe harvest failed");
                if (outputs(plot, plot.sticks).size() >= 2) seed = attempt;
                cleanup(plot);
            }
            helper.assertTrue(seed >= 0, "No native multi-item potato roll found");
            for (int failureMode = 0; failureMode < 3; failureMode++) {
                plant(helper, plot, "potato", 0, "iron"); mature(helper, plot, 0); plot.level.random.setSeed(seed);
                var before = snapshot(plot, plot.sticks); int xp = xp(plot); int mode = failureMode;
                var joins = new AtomicInteger();
                Consumer<EntityJoinLevelEvent> rejection = event -> {
                    if (event.getLevel() == plot.level && event.getEntity() instanceof ItemEntity entity && area(plot.sticks).contains(entity.position())) {
                        int join = joins.incrementAndGet();
                        if (join == (mode == 0 ? 1 : 2)) {
                            if (mode == 2) throw new IllegalStateException("Injected native item creation failure");
                            event.setCanceled(true);
                        }
                    }
                };
                NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, rejection);
                try { helper.assertTrue(!hand(plot, plot.sticks), "Failed item creation reported success"); }
                finally { NeoForge.EVENT_BUS.unregister(rejection); }
                helper.assertTrue(snapshot(plot, plot.sticks).equals(before) && xp(plot) == xp && outputs(plot, plot.sticks).isEmpty(), "Failed harvest lost crop/progress/genome or left items/XP");
                helper.assertTrue(hand(plot, plot.sticks) && !hand(plot, plot.sticks) && xp(plot) == xp + 14, "Retry failed or duplicated XP");
                cleanup(plot);
            }
            helper.succeed();
        } finally { close(plot); }
    }

    @GameTest(batch = "starfield_harvest", templateNamespace = "agricraft", template = "starfield_probe")
    public static void failedTallRegrowHarvestRestoresUpperPartAndNativeCountdown(GameTestHelper helper) {
        var plot = plot(helper, "tall-rollback");
        try {
            plant(helper, plot, "corn", 1, "obsidian"); mature(helper, plot, 1);
            var before = snapshot(plot, plot.sticks); int xp = xp(plot);
            Consumer<EntityJoinLevelEvent> rejection = event -> {
                if (event.getLevel() == plot.level && event.getEntity() instanceof ItemEntity entity && area(plot.sticks).contains(entity.position())) event.setCanceled(true);
            };
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, rejection);
            try { helper.assertTrue(!hand(plot, plot.sticks.above()), "Rejected upper-part harvest succeeded"); }
            finally { NeoForge.EVENT_BUS.unregister(rejection); }
            helper.assertTrue(snapshot(plot, plot.sticks).equals(before) && xp(plot) == xp && outputs(plot, plot.sticks).isEmpty(), "Tall regrow rollback changed either part/countdown/identity/XP");
            helper.assertTrue(hand(plot, plot.sticks.above()) && !hand(plot, plot.sticks), "Upper-part retry duplicated harvest"); helper.succeed();
        } finally { close(plot); }
    }

    @GameTest(batch = "starfield_harvest", templateNamespace = "agricraft", template = "starfield_probe")
    public static void itemJoinListenerCannotReenterAnotherHarvest(GameTestHelper helper) {
        var plot = plot(helper, "reentrant");
        try {
            plant(helper, plot, "potato", 0, "wooden"); mature(helper, plot, 0);
            var untouched = snapshot(plot, plot.plain); var attempts = new AtomicInteger();
            Consumer<EntityJoinLevelEvent> listener = event -> {
                if (event.getLevel() == plot.level && event.getEntity() instanceof ItemEntity entity && area(plot.sticks).contains(entity.position())) {
                    attempts.incrementAndGet();
                    helper.assertTrue(!hand(plot, plot.plain), "Item listener reentered a second harvest");
                }
            };
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, listener);
            try { helper.assertTrue(hand(plot, plot.sticks), "Outer harvest failed"); }
            finally { NeoForge.EVENT_BUS.unregister(listener); }
            helper.assertTrue(attempts.get() > 0 && snapshot(plot, plot.plain).equals(untouched), "Nested harvest changed neighboring crop");
            helper.assertTrue(hand(plot, plot.plain), "Nested rejection leaked server-thread transaction"); helper.succeed();
        } finally { close(plot); }
    }

    @GameTest(batch = "starfield_gene_harvest", templateNamespace = "agricraft", template = "starfield_probe")
    public static void gainDoublesPrimaryComponentsAndKeepsSunflowerExtrasNative(GameTestHelper helper) {
        var plot = plot(helper, "gene-output");
        try {
            for (var species : List.of("potato", "sunflower", "wheat")) {
                int season = species.equals("potato") ? 0 : 1;
                plant(helper, plot, species, season, "wooden"); mature(helper, plot, season);
                StarfieldGeneTestData.stat(plot.level, plot.sticks, "gain", com.agricraft.agricraft.api.stat.AgriStatRegistry.getInstance().gainStat().getMax());
                int before = xp(plot); plot.level.random.setSeed(913);
                helper.assertTrue(geneHarvest(plot, plot.plain, species), "Native baseline harvest rejected");
                var expected = componentCounts(plot); int nativeXp = xp(plot)-before; clearDrops(plot);
                setXp(plot, before); // Both quality rolls must see the same native farming level.
                plot.level.random.setSeed(913); before = xp(plot);
                helper.assertTrue(geneHarvest(plot, plot.sticks, species), "High gain harvest rejected");
                var actual = componentCounts(plot);
                helper.assertTrue(actual.keySet().equals(expected.keySet()), "Gain changed output composition/quality");
                // Match the known primary produce by item ID; count separately for each component set.
                for (var key : expected.keySet()) {
                    boolean main = key.contains("stardewcraft:"+species);
                    if (key.contains("sunflower_seeds")) main = false;
                    helper.assertTrue(actual.get(key) == expected.get(key)*(main ? 2 : 1), "Primary/extra quantity changed: "+key);
                }
                helper.assertTrue(xp(plot)-before == nativeXp && !hand(plot, plot.sticks), "Gain duplicated XP/repeated harvest");
                helper.assertTrue(plot.drops.stream().allMatch(entity -> entity.getItem().getCount() <= entity.getItem().getMaxStackSize()), "Gain overflowed stack limits");
            }
            helper.succeed();
        } finally { close(plot); }
    }

    @GameTest(batch = "starfield_gene_harvest", templateNamespace = "agricraft", template = "starfield_probe")
    public static void geneticRegrowthIsSavedOnceAndCannotHarvestImmediately(GameTestHelper helper) {
        var plot = plot(helper, "gene-regrow");
        try {
            plant(helper, plot, "corn", 1, "obsidian"); mature(helper, plot, 1);
            for (var root : plot.roots()) StarfieldGeneTestData.stat(plot.level, root, "growth", com.agricraft.agricraft.api.stat.AgriStatRegistry.getInstance().growthStat().getMax());
            var identity = StarfieldCompat.planting(plot.level, plot.sticks).orElseThrow();
            for (var root : plot.roots()) helper.assertTrue(hand(plot, root.above()), "Upper-part harvest failed");
            for (var root : plot.roots()) helper.assertTrue(CropGrowthManager.get(plot.level).getState(plot.level, root).dayInPhase == 2, "Corn regrow was not initialized to 2 days");
            helper.assertTrue(!hand(plot, plot.sticks), "Growth gene allowed immediate harvest");
            var saved = com.agricraft.agricraft.compat.starfield.StarfieldPlantData.get(plot.level).save(new CompoundTag(), plot.level.registryAccess());
            plot.level.getServer().overworld().getDataStorage().set(com.agricraft.agricraft.compat.starfield.StarfieldPlantData.STORAGE,
                    com.agricraft.agricraft.compat.starfield.StarfieldPlantData.load(saved, plot.level.registryAccess()));
            settle(plot, 101, 1);
            helper.assertTrue(CropGrowthManager.get(plot.level).getState(plot.level, plot.sticks).dayInPhase == 1 && !hand(plot, plot.sticks), "Reload reapplied regrow bonus");
            settle(plot, 102, 1);
            helper.assertTrue(hand(plot, plot.sticks) && CropGrowthManager.get(plot.level).getState(plot.level, plot.sticks).dayInPhase == 2
                    && StarfieldCompat.planting(plot.level, plot.sticks).orElseThrow().equals(identity), "Second genetic regrow changed identity/schedule");
            helper.succeed();
        } finally { close(plot); }
    }

    @GameTest(batch = "starfield_gene_harvest", templateNamespace = "agricraft", template = "starfield_probe")
    public static void rejectedGeneticOutputRestoresCropAndDoesNotPublishBonus(GameTestHelper helper) {
        var plot = plot(helper, "gene-rollback");
        try {
            plant(helper, plot, "potato", 0, "iron"); mature(helper, plot, 0);
            StarfieldGeneTestData.stat(plot.level, plot.sticks, "gain", com.agricraft.agricraft.api.stat.AgriStatRegistry.getInstance().gainStat().getMax());
            var before = snapshot(plot, plot.sticks); int experience = xp(plot);
            Consumer<EntityJoinLevelEvent> rejection = event -> { if (event.getLevel() == plot.level && event.getEntity() instanceof ItemEntity) event.setCanceled(true); };
            NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, rejection);
            try { helper.assertTrue(!hand(plot, plot.sticks), "Rejected genetic harvest succeeded"); }
            finally { NeoForge.EVENT_BUS.unregister(rejection); }
            helper.assertTrue(before.equals(snapshot(plot, plot.sticks)) && xp(plot)==experience && outputs(plot, plot.sticks).isEmpty(), "Rejected gain leaked output or crop mutation");
            helper.assertTrue(hand(plot, plot.sticks), "Retry after genetic rollback failed"); helper.succeed();
        } finally { close(plot); }
    }

    @GameTest(batch = "starfield_gene_harvest", templateNamespace = "agricraft", template = "starfield_probe")
    public static void gainSplitsStacksAndRollsBackRejectedOverflow(GameTestHelper helper) {
        var plot = plot(helper, "gene-stacks");
        try {
            var stack = new ItemStack(Items.WHEAT, 63);
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Native component"));
            var restored = new AtomicInteger();
            Runnable capture = () -> com.agricraft.agricraft.compat.starfield.StarfieldHarvestTransaction.primary(() -> {
                var entity = new ItemEntity(plot.level, plot.sticks.getX()+0.5, plot.sticks.getY(), plot.sticks.getZ()+0.5, stack.copy());
                entity.setPickUpDelay(27); plot.level.addFreshEntity(entity);
            });
            plot.level.random.setSeed(143);
            com.agricraft.agricraft.compat.starfield.StarfieldHarvestTransaction.run(plot.level, 0, capture, restored::incrementAndGet, () -> {});
            long nativeRandom = plot.level.random.nextLong(); clearDrops(plot); plot.level.random.setSeed(143);
            com.agricraft.agricraft.compat.starfield.StarfieldHarvestTransaction.run(plot.level, 1, capture, restored::incrementAndGet, () -> {});
            helper.assertTrue(plot.drops.size()==2 && plot.drops.stream().mapToInt(entity -> entity.getItem().getCount()).sum()==126
                    && plot.drops.stream().allMatch(entity -> entity.getItem().getCount() <= 64 && ItemStack.isSameItemSameComponents(entity.getItem(), stack))
                    && !plot.drops.getFirst().getUUID().equals(plot.drops.getLast().getUUID()), "Overflow changed components/limits/UUID");
            helper.assertTrue(plot.level.random.nextLong()==nativeRandom, "Overflow consumed extra native world randomness");
            clearDrops(plot); var attempts = new AtomicInteger();
            Consumer<EntityJoinLevelEvent> reject = event -> { if (event.getLevel()==plot.level && event.getEntity() instanceof ItemEntity && attempts.incrementAndGet()==2) event.setCanceled(true); };
            NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, reject);
            boolean aborted = false;
            try { com.agricraft.agricraft.compat.starfield.StarfieldHarvestTransaction.run(plot.level, 1, capture, restored::incrementAndGet, () -> {}); }
            catch (com.agricraft.agricraft.compat.starfield.StarfieldHarvestTransaction.Aborted failure) { aborted = true; }
            finally { NeoForge.EVENT_BUS.unregister(reject); }
            helper.assertTrue(aborted && restored.get()==1 && plot.drops.stream().allMatch(ItemEntity::isRemoved), "Rejected overflow leaked already published items");
            helper.succeed();
        } finally { close(plot); }
    }

    private static java.util.Map<String,Integer> componentCounts(Plot plot) {
        var result = new java.util.HashMap<String,Integer>();
        for (var entity : plot.drops) if (!entity.isRemoved()) {
            var stack = entity.getItem().copy(); com.agricraft.agricraft.compat.starfield.StarfieldGenomes.clearHeredity(stack); var key = stack.copyWithCount(1).save(plot.level.registryAccess()).toString();
            result.merge(key, stack.getCount(), Integer::sum);
        }
        return result;
    }

    private static boolean geneHarvest(Plot plot, BlockPos root, String species) {
        if (!species.equals("wheat")) return hand(plot, root);
        plot.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(com.stardew.craft.item.ModItems.SCYTHE.get()));
        return StardewCropRuntime.harvestForPlayer(plot.player, root, InteractionHand.MAIN_HAND, true, false).harvested();
    }

    private static Plot plot(GameTestHelper helper, String name) {
        var level = helper.getLevel().getServer().getLevel(ModDimensions.STARDEW_VALLEY);
        var uuid = UUID.nameUUIDFromBytes(("agricraft:harvest:" + name).getBytes(StandardCharsets.UTF_8));
        var registry = FarmInstanceRegistry.get(level.getServer()); var farm = registry.getFarm(uuid);
        if (farm == null) farm = registry.createFarm(uuid, name, "Harvest probe", FarmType.STANDARD);
        farm.markInitialized(); farm.setGraceDaysLeft(0);
        var root = farm.getOrigin().offset(40, 10, 40);
        var player = FakePlayerFactory.get(level, new GameProfile(uuid, "HarvestProbe"));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        // Native farms are outside the GameTest ticking area. Capture actual join events;
        // entity lookup otherwise omits successful drops in non-tracking chunks.
        var drops = new java.util.ArrayList<ItemEntity>();
        Consumer<EntityJoinLevelEvent> observer = event -> {
            if (event.getLevel() == level && event.getEntity() instanceof ItemEntity entity
                    && new AABB(root).inflate(4).contains(entity.position())) drops.add(entity);
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, observer);
        return new Plot(level, player, root, root.east(), drops, observer);
    }
    private static void plant(GameTestHelper helper, Plot plot, String species, int season, String variant) {
        cleanup(plot);
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("stardewcraft:" + species + "_crop"));
        helper.assertTrue(block instanceof com.stardew.craft.block.crop.StardewCropBlock, "Missing native species " + species);
        for (var root : plot.roots()) {
            plot.level.getChunk(root); plot.level.setBlock(root.below(), com.stardew.craft.block.ModBlocks.FARMLAND.get().defaultBlockState().setValue(FarmBlock.MOISTURE, 7), 3);
            plot.level.setBlock(root, block.defaultBlockState(), 3); StarfieldCompat.planting(plot.level, root).orElseThrow();
        }
        helper.assertTrue(StarfieldCompat.setSticks(plot.level, plot.sticks, variant), "Production sticks not attached");
    }
    private static void mature(GameTestHelper helper, Plot plot, int season) {
        for (int day = 1; day <= 40 && !StarfieldCompat.inspect(plot.level, plot.sticks).orElseThrow().mature(); day++) settle(plot, day, season);
        helper.assertTrue(plot.roots().stream().allMatch(root -> StarfieldCompat.inspect(plot.level, root).orElseThrow().mature()), "Native crops did not mature");
    }
    private static void settle(Plot plot, int day, int season) {
        for (var root : plot.roots()) plot.level.setBlock(root.below(), plot.level.getBlockState(root.below()).setValue(FarmBlock.MOISTURE, 7), 3);
        CropGrowthManager.get(plot.level).settleCrops(plot.level, plot.roots().stream().map(root -> GlobalPos.of(plot.level.dimension(), root)).toList(), day, season, false);
    }
    private static boolean hand(Plot plot, BlockPos root) { return StardewCropRuntime.harvestForPlayer(plot.player, root, InteractionHand.MAIN_HAND, false, false).harvested(); }
    private static int xp(Plot plot) { return PlayerStardewDataAPI.getSkillExperience(plot.player, SkillType.FARMING); }
    private static void setXp(Plot plot, int value) { PlayerStardewDataAPI.setExperience(plot.player, SkillType.FARMING, value); }
    private static AABB area(BlockPos root) { return new AABB(root).inflate(1.1); }
    private static List<String> outputs(Plot plot, BlockPos root) {
        return plot.drops.stream().filter(entity -> !entity.isRemoved() && area(root).contains(entity.position()))
                .map(entity -> { var stack = entity.getItem().copy(); com.agricraft.agricraft.compat.starfield.StarfieldGenomes.clearHeredity(stack); return stack.save(plot.level.registryAccess()).toString(); }).sorted().toList();
    }
    private static void clearDrops(Plot plot) { plot.drops.forEach(ItemEntity::discard); plot.drops.clear(); }
    private static void close(Plot plot) { NeoForge.EVENT_BUS.unregister(plot.observer); cleanup(plot); }
    private static void stand(ServerPlayer player, BlockPos root) { player.setPos(root.getX() + 0.5, root.getY(), root.getZ() - 0.5); player.setYRot(0); player.setXRot(0); }
    private static CompoundTag snapshot(Plot plot, BlockPos root) {
        var tag = new CompoundTag(); tag.putString("root", plot.level.getBlockState(root).toString()); tag.putString("upper", plot.level.getBlockState(root.above()).toString());
        tag.put("entry", StarfieldCompat.planting(plot.level, root).orElseThrow().encode());
        var growth = CropGrowthManager.get(plot.level).getState(plot.level, root);
        tag.putInt("phase", growth.phase); tag.putInt("day", growth.dayInPhase); tag.putInt("infertile", growth.infertileProgress);
        tag.putBoolean("regrowing", growth.regrowing); tag.putBoolean("source", growth.sourcePhases); tag.putInt("version", growth.sourcePhaseVersion);
        tag.putInt("lastDay", growth.lastDailyDay); tag.putBoolean("watered", growth.lastDailyWatered); tag.putInt("lastGiant", growth.lastGiantDay);
        if (growth.planterUuid != null) tag.putUUID("planter", growth.planterUuid);
        return tag;
    }
    private static void cleanup(Plot plot) {
        clearDrops(plot);
        for (var root : plot.roots()) {
            plot.level.getChunk(root); plot.level.setBlock(root, Blocks.AIR.defaultBlockState(), 3); plot.level.setBlock(root.above(), Blocks.AIR.defaultBlockState(), 3);
            FertilizerManager.get(plot.level).removeFertilizer(plot.level, root.below()); plot.level.setBlock(root.below(), Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
