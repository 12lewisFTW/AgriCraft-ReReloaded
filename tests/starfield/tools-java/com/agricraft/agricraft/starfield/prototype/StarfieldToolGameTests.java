package com.agricraft.agricraft.starfield.prototype;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.compat.ExternalSeedSupport;
import com.agricraft.agricraft.api.stat.AgriStatRegistry;
import com.agricraft.agricraft.common.block.entity.SeedAnalyzerBlockEntity;
import com.agricraft.agricraft.common.inventory.container.SeedAnalyzerMenu;
import com.agricraft.agricraft.common.item.AgriSeedItem;
import com.agricraft.agricraft.common.item.SeedBagItem;
import com.agricraft.agricraft.common.registry.ModItems;
import com.agricraft.agricraft.compat.starfield.*;
import com.mojang.authlib.GameProfile;
import com.stardew.craft.api.v1.agriculture.StardewCropRuntime;
import com.stardew.craft.block.FertilizerType;
import com.stardew.craft.core.ModDimensions;
import com.stardew.craft.farm.FarmInstanceRegistry;
import com.stardew.craft.farm.FarmType;
import com.stardew.craft.manager.CropGrowthManager;
import com.stardew.craft.manager.FertilizerManager;
import com.stardew.craft.time.StardewTimeManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@GameTestHolder("agricraft")
@PrefixGameTestTemplate(false)
public final class StarfieldToolGameTests {
    private record Plot(ServerLevel level, ServerPlayer player, BlockPos root, BlockPos analyzer) {}

    @GameTest(batch = "starfield_tools", templateNamespace = "agricraft", template = "starfield_probe")
    public static void naturalRemovalReturnsMaterialOnceAndCreativeRemovalReturnsNone(GameTestHelper helper) {
        var plot = plot(helper, "stick-removal");
        var drops = new java.util.ArrayList<net.minecraft.world.entity.item.ItemEntity>();
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> observer = event -> {
            if (event.getLevel() == plot.level && event.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity item
                    && item.getItem().is(ModItems.IRON_CROP_STICKS.get()) && item.blockPosition().distSqr(plot.root) < 4) drops.add(item);
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,true,observer);
        try {
            plant(plot, "potato", false);
            plot.level.setBlock(plot.root, Blocks.AIR.defaultBlockState(), 3);
            for (int i=0;i<2;i++) net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
                    new net.neoforged.neoforge.event.tick.ServerTickEvent.Post(()->true,plot.level.getServer()));
            helper.assertTrue(drops.stream().mapToInt(item->item.getItem().getCount()).sum()==1, "Natural removal lost or duplicated material");
            plant(plot, "potato", false); stand(plot.player, plot.root); plot.player.setGameMode(GameType.CREATIVE);
            helper.assertTrue(plot.player.gameMode.destroyBlock(plot.root), "Creative native break failed");
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
                    new net.neoforged.neoforge.event.tick.ServerTickEvent.Post(()->true,plot.level.getServer()));
            helper.assertTrue(drops.stream().mapToInt(item->item.getItem().getCount()).sum()==1, "Creative removal produced extra material");
            helper.succeed();
        } finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(observer); drops.forEach(net.minecraft.world.entity.item.ItemEntity::discard); cleanup(plot); }
    }

    @GameTest(batch = "starfield_tools", templateNamespace = "agricraft", template = "starfield_probe")
    public static void actualPlayerBreakReturnsOneMaterialAndCanceledBreakReturnsNone(GameTestHelper helper) {
        var plot = plot(helper, "stick-break"); var clock=StardewTimeManager.get(); int season=clock.getCurrentSeason();
        clock.setCurrentSeason(1); clearPlant(plot);
        plot.level.setBlock(plot.root.below(),com.stardew.craft.block.ModBlocks.FARMLAND.get().defaultBlockState().setValue(FarmBlock.MOISTURE,7),3);
        stand(plot.player,plot.root);plot.player.setItemInHand(InteractionHand.MAIN_HAND,variedSeed(plot,"corn",1));
        helper.assertTrue(plot.player.getMainHandItem().useOn(context(plot.player,plot.root.below())).consumesAction()
                && plot.level.getBlockState(plot.root.above()).is(BuiltInRegistries.BLOCK.get(type("corn"))),"Native seed did not place both corn halves");
        StarfieldCompat.setSticks(plot.level,plot.root,"iron");stand(plot.player,plot.root.above());
        clock.setCurrentSeason(season);
        var observed = new java.util.ArrayList<net.minecraft.world.entity.item.ItemEntity>();
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> observer = event -> {
            if(event.getLevel()==plot.level && event.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity item
                    && item.getItem().is(ModItems.IRON_CROP_STICKS.get()) && item.blockPosition().distSqr(plot.root)<4 && !event.loadedFromDisk()) {
                observed.add(item);
            }
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,true,observer);
        var entry = StarfieldCompat.planting(plot.level, plot.root).orElseThrow();
        java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.BreakEvent> cancel = event -> {
            if (event.getLevel() == plot.level && event.getPos().equals(plot.root.above())) event.setCanceled(true);
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancel);
        try { helper.assertTrue(!plot.player.gameMode.destroyBlock(plot.root.above())
                && StarfieldCompat.planting(plot.level, plot.root).orElseThrow().equals(entry), "Canceled break removed plant/material"); }
        finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel); }
        helper.assertTrue(plot.player.gameMode.destroyBlock(plot.root.above()), "Native upper break failed");
        // GameTestServer overrides the ordinary dedicated-server tick dispatch.
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.ServerTickEvent.Post(()->true,plot.level.getServer()));
            try {
                int count = observed.stream().filter(item -> !item.isRemoved()).mapToInt(item -> item.getItem().getCount()).sum();
                helper.assertTrue(count == 1 && plot.level.getBlockState(plot.root).isAir()
                        && plot.level.getBlockState(plot.root.above()).isAir() && StarfieldCompat.planting(plot.level, plot.root).isEmpty(), "Break count=" + count
                        + " lower=" + plot.level.getBlockState(plot.root) + " upper=" + plot.level.getBlockState(plot.root.above())
                        + " entry=" + StarfieldCompat.planting(plot.level, plot.root)+" root="+plot.root+" observed="+observed.stream().map(item->item.getItem()+"/"+item.blockPosition()+"/removed="+item.isRemoved()).toList());
                helper.succeed();
            } finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(observer); observed.forEach(net.minecraft.world.entity.item.ItemEntity::discard); cleanup(plot); }
    }

    @GameTest(batch = "starfield_tools", templateNamespace = "agricraft", template = "starfield_probe")
    public static void playerPlantsNativeSeedsIntoSticksWithBothPartsAndComponents(GameTestHelper helper) {
        var plot = plot(helper, "stick-placement"); var clock = StardewTimeManager.get(); int season = clock.getCurrentSeason();
        try {
            for (var name : List.of("potato", "corn")) {
                clearPlant(plot); clock.setCurrentSeason(name.equals("potato") ? 0 : 1);
                plot.level.setBlock(plot.root.below(), com.stardew.craft.block.ModBlocks.FARMLAND.get().defaultBlockState(), 3);
                plot.level.setBlock(plot.root, com.agricraft.agricraft.common.registry.ModBlocks.CROP.get().defaultBlockState()
                        .setValue(com.agricraft.agricraft.common.block.CropBlock.STICK_VARIANT, com.agricraft.agricraft.common.block.CropStickVariant.OBSIDIAN), 3);
                stand(plot.player, plot.root); var seed = variedSeed(plot, name, 3); var template = seed.copyWithCount(1);
                plot.player.setItemInHand(InteractionHand.MAIN_HAND, seed);
                helper.assertTrue(seed.useOn(context(plot.player, plot.root)).consumesAction(), "Seed placement into sticks failed: " + name);
                var entry = StarfieldCompat.planting(plot.level, plot.root).orElseThrow();
                helper.assertTrue(entry.sticks().equals("obsidian") && entry.genome().equals(StarfieldGenomes.readSeed(template, type(name)))
                        && ItemStack.isSameItemSameComponents(template, StarfieldCompat.seedFromPlant(plot.level, plot.root).orElseThrow())
                        && plot.player.getMainHandItem().getCount() == 2 && plot.level.getBlockEntity(plot.root) == null,
                        "Placement lost sticks, native representation, genome or item components");
                helper.assertTrue(CropGrowthManager.get(plot.level).getState(plot.level, plot.root) != null, "Native scheduler missed placement");
                if (name.equals("corn")) helper.assertTrue(plot.level.getBlockState(plot.root.above()).is(BuiltInRegistries.BLOCK.get(type(name))), "Missing native upper half");
            }
            helper.succeed();
        } finally { clock.setCurrentSeason(season); cleanup(plot); }
    }

    @GameTest(batch = "starfield_tools", templateNamespace = "agricraft", template = "starfield_probe")
    public static void canceledWrongSeasonDoubleAndForeignStickPlantingPreservesInventoryAndWorld(GameTestHelper helper) {
        var plot = plot(helper, "stick-guards"); var clock = StardewTimeManager.get(); int season = clock.getCurrentSeason();
        java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent> cancel = event -> {
            if (event.getLevel() == plot.level && event.getPos().equals(plot.root)) event.setCanceled(true);
        };
        boolean registered = false;
        try {
            plot.level.setBlock(plot.root.below(), com.stardew.craft.block.ModBlocks.FARMLAND.get().defaultBlockState(), 3);
            var empty = com.agricraft.agricraft.common.registry.ModBlocks.CROP.get().defaultBlockState();
            plot.level.setBlock(plot.root, empty, 3); stand(plot.player, plot.root);
            var seed = variedSeed(plot, "potato", 3); var original = seed.copy(); plot.player.setItemInHand(InteractionHand.MAIN_HAND, seed);
            clock.setCurrentSeason(3);
            helper.assertTrue(seed.useOn(context(plot.player, plot.root)) == InteractionResult.FAIL && plot.level.getBlockState(plot.root) == empty
                    && ItemStack.matches(plot.player.getMainHandItem(), original), "Wrong season consumed seed/sticks");
            clock.setCurrentSeason(0);
            var foreign = foreign(plot); stand(foreign, plot.root); foreign.setItemInHand(InteractionHand.MAIN_HAND, original.copy());
            helper.assertTrue(foreign.getMainHandItem().useOn(context(foreign, plot.root)) == InteractionResult.FAIL, "Foreign player planted into sticks");
            var doubled = empty.setValue(com.agricraft.agricraft.common.block.CropBlock.CROP_STATE, com.agricraft.agricraft.common.block.CropState.DOUBLE_STICKS);
            plot.level.setBlock(plot.root, doubled, 3);
            helper.assertTrue(plot.player.getMainHandItem().useOn(context(plot.player, plot.root)) == InteractionResult.FAIL
                    && plot.level.getBlockState(plot.root) == doubled && ItemStack.matches(plot.player.getMainHandItem(), original), "Double breeding sticks were consumed");
            plot.level.setBlock(plot.root, empty, 3);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancel); registered = true;
            helper.assertTrue(plot.player.getMainHandItem().useOn(context(plot.player, plot.root)) == InteractionResult.FAIL
                    && plot.level.getBlockState(plot.root) == empty && plot.level.getBlockEntity(plot.root) instanceof com.agricraft.agricraft.common.block.entity.CropBlockEntity
                    && ItemStack.matches(plot.player.getMainHandItem(), original) && StarfieldCompat.planting(plot.level, plot.root).isEmpty()
                    && CropGrowthManager.get(plot.level).getState(plot.level, plot.root) == null, "Canceled placement failed to restore seed/sticks or leaked native state");
            helper.succeed();
        } finally { if (registered) net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel); clock.setCurrentSeason(season); cleanup(plot); }
    }

    @GameTest(batch = "starfield_tools", templateNamespace = "agricraft", template = "starfield_probe")
    public static void addingAndRemovingSticksAtUpperHalfKeepsNativeGrowthAndIdentity(GameTestHelper helper) {
        var plot = plot(helper, "stick-interaction");
        try {
            plant(plot, "corn", false); StarfieldCompat.setSticks(plot.level, plot.root, "");
            var before = StarfieldCompat.planting(plot.level, plot.root).orElseThrow(); var growth = snapshotGrowth(plot);
            stand(plot.player, plot.root.above()); var sticks = new ItemStack(ModItems.IRON_CROP_STICKS.get(), 3);
            plot.player.setItemInHand(InteractionHand.MAIN_HAND, sticks);
            helper.assertTrue(sticks.useOn(context(plot.player, plot.root.above())).consumesAction() && sticks.getCount() == 2, "Upper interaction did not attach exactly one material");
            helper.assertTrue(sticks.useOn(context(plot.player, plot.root.above())) == InteractionResult.FAIL && sticks.getCount() == 2, "Repeated attachment consumed material");
            plot.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); plot.player.setShiftKeyDown(true);
            var hit = new BlockHitResult(Vec3.atCenterOf(plot.root.above()), Direction.UP, plot.root.above(), false);
            var event = new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(plot.player, InteractionHand.MAIN_HAND, plot.root.above(), hit);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
            var after = StarfieldCompat.planting(plot.level, plot.root).orElseThrow();
            helper.assertTrue(event.isCanceled() && event.getCancellationResult().consumesAction() && after.sticks().isEmpty()
                    && after.plantingId().equals(before.plantingId()) && after.genome().equals(before.genome()) && snapshotGrowth(plot).equals(growth)
                    && plot.player.getInventory().countItem(ModItems.IRON_CROP_STICKS.get()) == 1, "Removing sticks changed native identity/progress or lost material");
            helper.succeed();
        } finally { cleanup(plot); }
    }

    @GameTest(batch = "starfield_tools", templateNamespace = "agricraft", template = "starfield_probe")
    public static void harvestedCoffeeBeanReplantsProduceGenomeAndRejectsCorruption(GameTestHelper helper) {
        var plot = plot(helper, "coffee-replant"); var clock = StardewTimeManager.get(); int season = clock.getCurrentSeason();
        try {
            clock.setCurrentSeason(0); var type = type("coffee_bean"); var genome = StarfieldGenomes.baseline(type);
            for (var stat : AgriStatRegistry.getInstance()) genome.getCompound("genes").getCompound(stat.getId()).putInt("dom", stat.getMax());
            var bean = new ItemStack(BuiltInRegistries.ITEM.get(StarfieldCompat.species(type).orElseThrow().seed()), 3);
            StarfieldGenomes.writeProduce(bean, new StarfieldPlantData.Entry(UUID.randomUUID(), type, genome, new CompoundTag(), ""));
            helper.assertTrue(!bean.get(DataComponents.CUSTOM_DATA).copyTag().contains(StarfieldGenomes.SEED_KEY)
                    && ExternalSeedSupport.genome(bean, plot.level).isPresent(), "Harvest-only coffee genome is unreadable");
            plot.level.setBlock(plot.root.below(), com.stardew.craft.block.ModBlocks.FARMLAND.get().defaultBlockState(), 3);
            stand(plot.player, plot.root); plot.player.setItemInHand(InteractionHand.MAIN_HAND, bean);
            helper.assertTrue(bean.useOn(context(plot.player, plot.root.below())).consumesAction()
                    && StarfieldCompat.planting(plot.level, plot.root).orElseThrow().genome().equals(genome), "Coffee replant lost genome");
            var bad = bean.copy(); var data = bad.get(DataComponents.CUSTOM_DATA).copyTag(); data.remove(StarfieldGenomes.SEED_KEY);
            data.getCompound(StarfieldGenomes.PRODUCE_KEY).putString("produce", "minecraft:stone"); bad.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
            helper.assertTrue(!ExternalSeedSupport.accepts(bad, plot.level), "Mismatched produce item was accepted as inherited seed"); helper.succeed();
        } finally { clock.setCurrentSeason(season); cleanup(plot); }
    }

    @GameTest(batch = "starfield_tools", templateNamespace = "agricraft", template = "starfield_probe")
    public static void analyzerReadsAllNativeBaselinesAndPreservesHeterozygousSeeds(GameTestHelper helper) {
        var plot = plot(helper, "all-seeds");
        try {
            var menu = menu(plot);
            for (var species : StarfieldCompat.species()) {
                var seed = new ItemStack(BuiltInRegistries.ITEM.get(species.seed()), 3); var untouched = seed.copy();
                helper.assertTrue(menu.getSlot(0).mayPlace(seed), "Native seed not accepted: " + species.id());
                menu.getSlot(0).set(seed);
                helper.assertTrue(ItemStack.matches(seed, untouched) && analyzer(plot).getSeed().getItem() == seed.getItem()
                        && analyzer(plot).getSeed().getCount() == 3, "Analysis changed native seed item/count/source");
                var rendered = new CompoundTag(); menu.getGenomeToRender().orElseThrow().writeToNBT(rendered);
                helper.assertTrue(rendered.equals(StarfieldGenomes.baseline(species.id())), "Wrong baseline preview: " + species.id());
                menu.getSlot(0).set(ItemStack.EMPTY);
            }
            var seed = variedSeed(plot, "potato", 7); var original = seed.copy();
            menu.getSlot(0).set(seed);
            var rendered = new CompoundTag(); menu.getGenomeToRender().orElseThrow().writeToNBT(rendered);
            helper.assertTrue(rendered.equals(StarfieldGenomes.readSeed(seed, type("potato")))
                    && ItemStack.isSameItemSameComponents(original, analyzer(plot).extractSeed()) && ItemStack.matches(original, seed),
                    "Analysis/extraction lost either allele or foreign components");
            // The existing ordinary AgriCraft conversion still uses its existing plant registry.
            var wheat = new ItemStack(Items.WHEAT_SEEDS, 4);
            helper.assertTrue(menu.getSlot(0).mayPlace(wheat), "Ordinary vanilla seed regression"); menu.getSlot(0).set(wheat);
            helper.assertTrue(analyzer(plot).getSeed().getItem() instanceof AgriSeedItem && analyzer(plot).getSeed().getCount() == 4
                    && AgriApi.getPlant(AgriSeedItem.getSpecies(analyzer(plot).getSeed())).isPresent(), "Existing AgriCraft seed conversion regressed");
            helper.succeed();
        } finally { cleanup(plot); }
    }

    @GameTest(batch = "starfield_tools", templateNamespace = "agricraft", template = "starfield_probe")
    public static void nativeMenuTransferAndFullInventoryDoNotLoseOrDuplicateSeeds(GameTestHelper helper) {
        var plot = plot(helper, "menu-transfer");
        try {
            var menu = menu(plot); var seed = variedSeed(plot, "potato", 17); var expected = seed.copy();
            plot.player.getInventory().setItem(9, seed);
            helper.assertTrue(!menu.quickMoveStack(plot.player, 2).isEmpty() && plot.player.getInventory().getItem(9).isEmpty(), "Shift insertion failed");
            helper.assertTrue(ItemStack.matches(analyzer(plot).getSeed(), expected), "Shift insertion lost count or components");
            for (int slot = 0; slot < 36; slot++) plot.player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
            helper.assertTrue(menu.quickMoveStack(plot.player, 0).isEmpty() && ItemStack.matches(analyzer(plot).getSeed(), expected), "Full inventory lost analyzer seeds");
            plot.player.getInventory().setItem(0, ItemStack.EMPTY);
            helper.assertTrue(!menu.quickMoveStack(plot.player, 0).isEmpty() && analyzer(plot).getSeed().isEmpty()
                    && ItemStack.matches(plot.player.getInventory().getItem(0), expected), "Shift extraction lost/duplicated native seed");
            helper.assertTrue(menu.quickMoveStack(plot.player, 0).isEmpty(), "Repeated extraction created seeds"); helper.succeed();
        } finally { cleanup(plot); }
    }

    @GameTest(batch = "starfield_tools", templateNamespace = "agricraft", template = "starfield_probe")
    public static void analyzerRejectsBadSeedsAndStaleDistantClosedOrForeignMenus(GameTestHelper helper) {
        var plot = plot(helper, "menu-guards");
        try {
            var menu = menu(plot); var seed = variedSeed(plot, "potato", 2);
            var tag = seed.get(DataComponents.CUSTOM_DATA).copyTag(); tag.getCompound(StarfieldGenomes.SEED_KEY).putInt("schema", 99);
            var bad = seed.copy(); bad.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            helper.assertTrue(!menu.getSlot(0).mayPlace(bad) && ItemStack.matches(analyzer(plot).insertSeed(bad), bad) && !analyzer(plot).hasSeed(), "Future seed schema was consumed");
            tag = seed.get(DataComponents.CUSTOM_DATA).copyTag(); tag.getCompound(StarfieldGenomes.SEED_KEY).getCompound("genome").getCompound("genes").remove("gain");
            bad = seed.copy(); bad.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            helper.assertTrue(!menu.getSlot(0).mayPlace(bad), "Incomplete native genome accepted");
            plot.player.setShiftKeyDown(true); plot.player.setItemInHand(InteractionHand.MAIN_HAND, seed);
            helper.assertTrue(StarfieldTools.use(context(plot.player, plot.analyzer)).orElseThrow() == InteractionResult.SUCCESS
                    && seed.getCount() == 1 && analyzer(plot).getSeed().getCount() == 1, "Direct insertion did not consume exactly one seed");
            var stored = analyzer(plot).getSeed().copy();
            helper.assertTrue(StarfieldTools.use(context(plot.player, plot.analyzer)).orElseThrow() == InteractionResult.FAIL && seed.getCount() == 1, "Occupied analyzer consumed held seed");
            var foreign = foreign(plot); stand(foreign, plot.analyzer); foreign.setShiftKeyDown(true); foreign.setItemInHand(InteractionHand.MAIN_HAND, seed.copy());
            helper.assertTrue(!menu.stillValid(foreign) && StarfieldTools.use(context(foreign, plot.analyzer)).orElseThrow() == InteractionResult.FAIL
                    && ItemStack.matches(stored, analyzer(plot).getSeed()), "Foreign farm analyzer modification succeeded");
            plot.player.setPos(plot.analyzer.getX() + 40, plot.analyzer.getY(), plot.analyzer.getZ());
            helper.assertTrue(!menu.stillValid(plot.player) && menu.quickMoveStack(plot.player, 0).isEmpty(), "Distant menu allowed extraction");
            stand(plot.player, plot.analyzer); plot.player.containerMenu = plot.player.inventoryMenu;
            helper.assertTrue(menu.quickMoveStack(plot.player, 0).isEmpty(), "Closed menu allowed extraction");
            plot.player.containerMenu = menu; plot.level.setBlock(plot.analyzer, Blocks.AIR.defaultBlockState(), 3);
            plot.level.setBlock(plot.analyzer, com.agricraft.agricraft.common.registry.ModBlocks.SEED_ANALYZER.get().defaultBlockState(), 3);
            helper.assertTrue(!menu.stillValid(plot.player) && menu.quickMoveStack(plot.player, 0).isEmpty(), "Replacement analyzer kept stale menu valid"); helper.succeed();
        } finally { cleanup(plot); }
    }

    @GameTest(batch = "starfield_tools", templateNamespace = "agricraft", template = "starfield_probe")
    public static void inspectionMatchesNativeDaysIncludingFertilizerAndInfertileSoil(GameTestHelper helper) {
        var plot = plot(helper, "timing");
        try {
            for (int mode = 0; mode < 3; mode++) {
                plant(plot, "potato", mode == 2);
                if (mode == 1) helper.assertTrue(FertilizerManager.get(plot.level).tryApplyFertilizer(plot.level, plot.root.below(), FertilizerType.DELUXE_SPEED_GRO), "Speed-Gro rejected");
                var growth = snapshotGrowth(plot); var before = report(plot);
                helper.assertTrue(snapshotGrowth(plot).equals(growth) && !before.getBoolean("mature") && before.getInt("phase") == 0, "Inspection advanced crop state");
                helper.assertTrue(before.getInt("remaining") == (mode == 0 ? 6 : mode == 2 ? 9 : 4), "Wrong native duration: " + before);
                var old = plot.level.getBlockState(plot.root.below()); plot.level.setBlock(plot.root.below(), old.setValue(FarmBlock.MOISTURE, 0), 3);
                helper.assertTrue(!report(plot).getBoolean("wet") && report(plot).getInt("remaining") == before.getInt("remaining"), "Dry soil created fictitious progress");
                settle(plot, 1, 0, false); helper.assertTrue(report(plot).getInt("remaining") == before.getInt("remaining"), "Unwatered day changed predicted growth");
                int wetDays = 0;
                while (!report(plot).getBoolean("mature") && wetDays < 20) { wetDays++; settle(plot, wetDays + 1, 0, true); }
                helper.assertTrue(wetDays == before.getInt("remaining") && report(plot).getInt("remaining") == 0, "Remaining days disagree with native maturity");
                clearPlant(plot);
            }
            helper.succeed();
        } finally { cleanup(plot); }
    }

    @GameTest(batch = "starfield_tools", templateNamespace = "agricraft", template = "starfield_probe")
    public static void upperPartRegrowAndLegacyInspectionNeverGuessVisualPercent(GameTestHelper helper) {
        var plot = plot(helper, "regrow");
        try {
            plant(plot, "corn", false); for (int day = 1; day <= 14; day++) settle(plot, day, 1, true);
            helper.assertTrue(report(plot).getBoolean("mature"), "Corn did not mature");
            helper.assertTrue(StarfieldCompat.inspection(plot.level, plot.root.above()).orElseThrow().equals(report(plot)), "Upper-part report differs from root");
            helper.assertTrue(StardewCropRuntime.harvestForPlayer(plot.player, plot.root.above(), InteractionHand.MAIN_HAND, false, false).harvested(), "Upper-part harvest failed");
            helper.assertTrue(report(plot).getBoolean("regrowing") && report(plot).getInt("remaining") == 4, "Regrow countdown interpreted as elapsed time");
            settle(plot, 15, 1, true); helper.assertTrue(report(plot).getInt("remaining") == 3, "Regrow report did not count down");
            var growth = CropGrowthManager.get(plot.level).getState(plot.level, plot.root); growth.sourcePhases = false;
            var unchanged = snapshotGrowth(plot);
            helper.assertTrue(report(plot).getInt("remaining") == -1 && snapshotGrowth(plot).equals(unchanged), "Inspection guessed/migrated legacy progress");
            var normal = StarfieldInspection.tooltip(report(plot), false); var extended = StarfieldInspection.tooltip(report(plot), true);
            helper.assertTrue(extended.size() >= normal.size() + 6 && normal.stream().noneMatch(text -> text.getString().contains("%")), "Tooltip fabricated a visual percentage or omitted genes"); helper.succeed();
        } finally { cleanup(plot); }
    }

    @GameTest(batch = "starfield_tools", templateNamespace = "agricraft", template = "starfield_probe")
    public static void inspectionRequestsValidateToolReachDimensionAndPlantingUuid(GameTestHelper helper) {
        var plot = plot(helper, "requests");
        try {
            plant(plot, "potato", false); stand(plot.player, plot.root);
            var entry = StarfieldCompat.planting(plot.level, plot.root).orElseThrow();
            var request = new StarfieldInspectionSync.Request(plot.level.dimension().location(), plot.root, entry.plantingId());
            helper.assertTrue(StarfieldInspectionSync.inspect(plot.player, request, true).isEmpty(), "Inspection accepted player without magnifier");
            plot.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MAGNIFYING_GLASS.get()));
            helper.assertTrue(StarfieldInspectionSync.inspect(plot.player, request, true).orElseThrow().equals(report(plot)), "Authorized report differs from native source");
            helper.assertTrue(StarfieldInspectionSync.inspect(plot.player, new StarfieldInspectionSync.Request(ResourceLocation.parse("minecraft:overworld"), plot.root, entry.plantingId()), true).isEmpty(), "Wrong dimension request accepted");
            helper.assertTrue(StarfieldInspectionSync.inspect(plot.player, new StarfieldInspectionSync.Request(request.dimension(), plot.root, UUID.randomUUID()), true).isEmpty(), "Forged planting UUID accepted");
            var far = plot.root.offset(120000, 0, 120000); boolean loaded = plot.level.hasChunkAt(far);
            helper.assertTrue(StarfieldInspectionSync.inspect(plot.player, new StarfieldInspectionSync.Request(request.dimension(), far, entry.plantingId()), true).isEmpty()
                    && plot.level.hasChunkAt(far) == loaded, "Remote inspection loaded a chunk");
            plot.player.setPos(plot.root.getX() + 20, plot.root.getY(), plot.root.getZ());
            helper.assertTrue(StarfieldInspectionSync.inspect(plot.player, request, true).isEmpty(), "Distant request accepted");
            stand(plot.player, plot.root); clearPlant(plot); plant(plot, "potato", false);
            helper.assertTrue(StarfieldInspectionSync.inspect(plot.player, request, true).isEmpty(), "Old UUID inspected replacement crop"); helper.succeed();
        } finally { cleanup(plot); }
    }

    @GameTest(batch = "starfield_tools", templateNamespace = "agricraft", template = "starfield_probe")
    public static void inspectionCodecAndClientCacheRejectStaleWorldsIdentitiesAndStates(GameTestHelper helper) {
        var plot = plot(helper, "cache");
        try {
            plant(plot, "potato", false); var tag = report(plot); var entry = StarfieldCompat.planting(plot.level, plot.root).orElseThrow();
            var dimension = plot.level.dimension().location(); var session = UUID.randomUUID();
            var request = new StarfieldInspectionSync.Request(dimension, plot.root, entry.plantingId());
            var response = new StarfieldInspectionSync.Response(session, dimension, plot.root, tag);
            var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), plot.level.registryAccess());
            try {
                StarfieldInspectionSync.Request.CODEC.encode(buf, request); helper.assertTrue(request.equals(StarfieldInspectionSync.Request.CODEC.decode(buf)), "Inspection request codec changed identity");
                buf.clear(); StarfieldInspectionSync.Response.CODEC.encode(buf, response); helper.assertTrue(response.equals(StarfieldInspectionSync.Response.CODEC.decode(buf)), "Inspection response codec changed report");
            } finally { buf.release(); }
            var cache = new StarfieldPlantCache(); var world = new Object(); cache.bind(world, dimension); cache.apply(session, dimension, plot.root, Optional.of(entry));
            cache.applyInspection(session, dimension, plot.root, tag, 100);
            helper.assertTrue(cache.inspection(plot.root, 100).orElseThrow().equals(tag) && StarfieldInspection.matches(plot.level, plot.root, Optional.of(entry), tag), "Valid inspection missing");
            cache.inspection(plot.root, 100).orElseThrow().putInt("remaining", 999); helper.assertTrue(cache.inspection(plot.root, 100).orElseThrow().equals(tag), "Client cache exposed mutable report");
            helper.assertTrue(cache.inspection(plot.root, 121).isEmpty() && cache.inspection(plot.root, 99).isEmpty(), "Expired/reversed-clock report remained visible");
            var invalid = tag.copy(); invalid.putString("dimension", "minecraft:overworld");
            cache.applyInspection(session, dimension, plot.root, invalid, 200); helper.assertTrue(cache.inspection(plot.root, 200).isEmpty(), "Wrong-dimension report revived old cache");
            cache.applyInspection(UUID.randomUUID(), dimension, plot.root, tag, 200); helper.assertTrue(cache.inspection(plot.root, 200).isEmpty(), "Old session report accepted");
            var changed = new StarfieldPlantData.Entry(UUID.randomUUID(), entry.type(), entry.genome(), entry.seedTemplate(), entry.sticks());
            cache.apply(session, dimension, plot.root, Optional.of(changed)); cache.applyInspection(session, dimension, plot.root, tag, 200);
            helper.assertTrue(cache.inspection(plot.root, 200).isEmpty(), "Old crop report accepted after replacement");
            cache.bind(new Object(), dimension); cache.applyInspection(session, dimension, plot.root, tag, 200); helper.assertTrue(cache.inspection(plot.root, 200).isEmpty(), "Reconnect retained old data");
            cache.bind(world, ResourceLocation.parse("minecraft:overworld")); cache.applyInspection(session, dimension, plot.root, tag, 200); helper.assertTrue(cache.inspection(plot.root, 200).isEmpty(), "Dimension switch retained old data");
            plot.level.setBlock(plot.root.below(), plot.level.getBlockState(plot.root.below()).setValue(FarmBlock.MOISTURE, 0), 3);
            helper.assertTrue(!StarfieldInspection.matches(plot.level, plot.root, Optional.of(entry), tag), "Old water state remained visible"); helper.succeed();
        } finally { cleanup(plot); }
    }

    @GameTest(batch = "starfield_tools", templateNamespace = "agricraft", template = "starfield_probe")
    public static void nativeToolsRefuseUnsafeActionsAndTrowelChecksDistanceAndOwnership(GameTestHelper helper) {
        var plot = plot(helper, "actions");
        try {
            plant(plot, "potato", false); var initial = report(plot); stand(plot.player, plot.root);
            for (var item : List.of(ModItems.CLIPPER.get(), ModItems.WOODEN_RAKE.get(), ModItems.IRON_RAKE.get(), ModItems.SEED_BAG.get())) {
                var stack = new ItemStack(item); plot.player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                helper.assertTrue(StarfieldTools.use(context(plot.player, plot.root)).orElseThrow() == InteractionResult.FAIL
                        && stack.getCount() == 1 && report(plot).equals(initial), "Unsafe Agri action changed native crop");
            }
            helper.assertTrue(SeedBagItem.add(new ItemStack(ModItems.SEED_BAG.get()), variedSeed(plot, "potato", 4)) == 0, "Bag stored native seeds as unsupported Agri seeds");
            plot.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MAGNIFYING_GLASS.get()));
            helper.assertTrue(StarfieldTools.use(context(plot.player, plot.root)).orElseThrow() == InteractionResult.SUCCESS && report(plot).equals(initial), "Magnifier changed native crop");
            var tool = new ItemStack(ModItems.TROWEL.get()); plot.player.setItemInHand(InteractionHand.MAIN_HAND, tool);
            plot.player.setPos(plot.root.getX() + 20, plot.root.getY(), plot.root.getZ());
            helper.assertTrue(StarfieldTools.use(context(plot.player, plot.root)).orElseThrow() == InteractionResult.FAIL && report(plot).equals(initial), "Distant trowel moved crop");
            var foreign = foreign(plot); stand(foreign, plot.root); foreign.setItemInHand(InteractionHand.MAIN_HAND, tool.copy());
            helper.assertTrue(StarfieldTools.use(context(foreign, plot.root)).orElseThrow() == InteractionResult.FAIL && report(plot).equals(initial), "Foreign trowel moved crop");
            stand(plot.player, plot.root); helper.assertTrue(StarfieldTools.use(context(plot.player, plot.root)).orElseThrow().consumesAction() && plot.level.getBlockState(plot.root).isAir(), "Owner trowel refused valid crop");
            var stored = tool.get(DataComponents.CUSTOM_DATA).copyTag().getCompound(StarfieldGenomes.TRANSFER_KEY);
            helper.assertTrue(stored.getCompound("entry").equals(initial.getCompound("entry")), "Trowel lost analyzed genome/identity"); helper.succeed();
        } finally { cleanup(plot); }
    }

    @GameTest(batch = "starfield_tools", templateNamespace = "agricraft", template = "starfield_probe")
    public static void directAnalyzerActionsAndJadeUseTheSameValidatedServerState(GameTestHelper helper) {
        var plot = plot(helper, "direct-jade"); var clock = StardewTimeManager.get(); int savedSeason = clock.getCurrentSeason();
        try {
            var seed = variedSeed(plot, "potato", 3); helper.assertTrue(analyzer(plot).insertSeed(seed).isEmpty(), "Fixture insertion failed");
            var stored = analyzer(plot).getSeed().copy(); var foreign = foreign(plot); stand(foreign, plot.analyzer);
            foreign.setShiftKeyDown(true); foreign.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            var hit = new BlockHitResult(Vec3.atCenterOf(plot.analyzer), Direction.UP, plot.analyzer, false);
            helper.assertTrue(plot.level.getBlockState(plot.analyzer).useWithoutItem(plot.level, foreign, hit) == InteractionResult.FAIL
                    && ItemStack.matches(stored, analyzer(plot).getSeed()), "Direct block action bypassed farm permission");
            stand(plot.player, plot.analyzer); plot.player.setShiftKeyDown(true); plot.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            helper.assertTrue(plot.level.getBlockState(plot.analyzer).useWithoutItem(plot.level, plot.player, hit).consumesAction()
                    && !analyzer(plot).hasSeed() && plot.player.getInventory().items.stream().anyMatch(stack -> ItemStack.matches(stack, stored)), "Direct extraction lost native components/count");
            plant(plot, "potato", false); stand(plot.player, plot.root); clock.setCurrentSeason(0);
            var state = plot.level.getBlockState(plot.root); helper.assertTrue(report(plot).getBoolean("inSeason"), "Native spring season not shown");
            clock.setCurrentSeason(3); helper.assertTrue(!report(plot).getBoolean("inSeason") && plot.level.getBlockState(plot.root) == state, "Season inspection advanced/killed crop");
            clock.setCurrentSeason(0);
            var accessor = (snownee.jade.api.BlockAccessor) java.lang.reflect.Proxy.newProxyInstance(
                    snownee.jade.api.BlockAccessor.class.getClassLoader(), new Class<?>[]{snownee.jade.api.BlockAccessor.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "getPlayer" -> plot.player; case "getPosition" -> plot.root; case "getLevel" -> plot.level;
                        case "getBlock" -> plot.level.getBlockState(plot.root).getBlock(); case "getBlockState" -> plot.level.getBlockState(plot.root);
                        case "isFakeBlock" -> false; default -> null;
                    });
            var provider = new StarfieldJadePlugin.Provider(); var data = new CompoundTag();
            helper.assertTrue(provider.shouldRequestData(accessor), "Jade did not request native crop data"); provider.appendServerData(data, accessor);
            helper.assertTrue(data.getCompound("agricraft:starfield_inspection").equals(report(plot)), "Jade source differs from magnifier/TOP report");
            clearPlant(plot); data = new CompoundTag();
            helper.assertTrue(!provider.shouldRequestData(accessor), "Jade requested data for an ordinary block"); provider.appendServerData(data, accessor);
            helper.assertTrue(data.isEmpty(), "Jade exposed stale crop data after removal"); helper.succeed();
        } finally { clock.setCurrentSeason(savedSeason); cleanup(plot); }
    }

    private static Plot plot(GameTestHelper helper, String name) {
        var level = helper.getLevel().getServer().getLevel(ModDimensions.STARDEW_VALLEY);
        var id = UUID.nameUUIDFromBytes(("agricraft:tools:" + name).getBytes(StandardCharsets.UTF_8));
        var registry = FarmInstanceRegistry.get(level.getServer()); var farm = registry.getFarm(id);
        if (farm == null) farm = registry.createFarm(id, name, "Tools probe", FarmType.STANDARD);
        farm.markInitialized(); farm.setGraceDaysLeft(0);
        var root = farm.getOrigin().offset(80, 10, 80); var analyzer = root.north(3); level.getChunk(root); level.getChunk(analyzer);
        level.setBlock(analyzer, com.agricraft.agricraft.common.registry.ModBlocks.SEED_ANALYZER.get().defaultBlockState(), 3);
        var player = FakePlayerFactory.get(level, new GameProfile(id, "ToolsProbe")); player.setGameMode(GameType.SURVIVAL);
        player.setShiftKeyDown(false); player.getInventory().clearContent(); stand(player, analyzer);
        return new Plot(level, player, root, analyzer);
    }
    private static ServerPlayer foreign(Plot plot) {
        var player = FakePlayerFactory.get(plot.level, new GameProfile(UUID.nameUUIDFromBytes("agricraft:tools:foreign".getBytes(StandardCharsets.UTF_8)), "ToolsForeign"));
        player.setGameMode(GameType.SURVIVAL); return player;
    }
    private static SeedAnalyzerBlockEntity analyzer(Plot plot) { return (SeedAnalyzerBlockEntity) plot.level.getBlockEntity(plot.analyzer); }
    private static SeedAnalyzerMenu menu(Plot plot) { var menu = new SeedAnalyzerMenu(7, plot.player.getInventory(), plot.player, plot.analyzer); plot.player.containerMenu = menu; return menu; }
    private static void stand(ServerPlayer player, BlockPos pos) { player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 2.0); }
    private static UseOnContext context(ServerPlayer player, BlockPos pos) { return new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)); }
    private static ResourceLocation type(String species) { return ResourceLocation.parse("stardewcraft:" + species + "_crop"); }
    private static ItemStack variedSeed(Plot plot, String species, int count) {
        var seed = new ItemStack(BuiltInRegistries.ITEM.get(StarfieldCompat.species(type(species)).orElseThrow().seed()), count);
        seed.set(DataComponents.CUSTOM_NAME, Component.literal("Tools native seed")); var tag = new CompoundTag(); tag.putString("foreign:marker", "preserve"); seed.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        var genome = StarfieldGenomes.baseline(type(species));
        for (var stat : AgriStatRegistry.getInstance()) { genome.getCompound("genes").getCompound(stat.getId()).putInt("dom", stat.getMax()); genome.getCompound("genes").getCompound(stat.getId()).putInt("rec", stat.getMin()); }
        StarfieldGenomes.writeSeed(seed, type(species), genome); return seed;
    }
    private static void plant(Plot plot, String species, boolean infertile) {
        clearPlant(plot);
        plot.level.setBlock(plot.root.below(), (infertile ? com.stardew.craft.block.ModBlocks.INFERTILE_FARMLAND.get() : com.stardew.craft.block.ModBlocks.FARMLAND.get()).defaultBlockState().setValue(FarmBlock.MOISTURE, 7), 3);
        plot.level.setBlock(plot.root, BuiltInRegistries.BLOCK.get(type(species)).defaultBlockState(), 3);
        StarfieldCompat.planting(plot.level, plot.root).orElseThrow(); StarfieldCompat.setSticks(plot.level, plot.root, "iron");
    }
    private static void settle(Plot plot, int day, int season, boolean watered) {
        plot.level.setBlock(plot.root.below(), plot.level.getBlockState(plot.root.below()).setValue(FarmBlock.MOISTURE, watered ? 7 : 0), 3);
        CropGrowthManager.get(plot.level).settleCrops(plot.level, List.of(GlobalPos.of(plot.level.dimension(), plot.root)), day, season, false);
    }
    private static CompoundTag report(Plot plot) { return StarfieldCompat.inspection(plot.level, plot.root).orElseThrow(); }
    private static CompoundTag snapshotGrowth(Plot plot) {
        var growth = CropGrowthManager.get(plot.level).getState(plot.level, plot.root); var tag = new CompoundTag();
        tag.putInt("phase", growth.phase); tag.putInt("day", growth.dayInPhase); tag.putInt("soil", growth.infertileProgress);
        tag.putBoolean("regrowing", growth.regrowing); tag.putBoolean("source", growth.sourcePhases); tag.putInt("version", growth.sourcePhaseVersion);
        tag.putInt("lastDay", growth.lastDailyDay); tag.putBoolean("lastWatered", growth.lastDailyWatered); return tag;
    }
    private static void clearPlant(Plot plot) {
        plot.level.setBlock(plot.root, Blocks.AIR.defaultBlockState(), 3); plot.level.setBlock(plot.root.above(), Blocks.AIR.defaultBlockState(), 3);
        FertilizerManager.get(plot.level).removeFertilizer(plot.level, plot.root.below());
    }
    private static void cleanup(Plot plot) {
        plot.player.containerMenu = plot.player.inventoryMenu; plot.player.getInventory().clearContent(); plot.player.setShiftKeyDown(false);
        clearPlant(plot); plot.level.setBlock(plot.root.below(), Blocks.AIR.defaultBlockState(), 3); plot.level.setBlock(plot.analyzer, Blocks.AIR.defaultBlockState(), 3);
        plot.level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(plot.root).inflate(8)).forEach(net.minecraft.world.entity.item.ItemEntity::discard);
    }
}
