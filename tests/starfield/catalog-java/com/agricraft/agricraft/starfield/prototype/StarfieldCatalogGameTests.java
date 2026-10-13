package com.agricraft.agricraft.starfield.prototype;

import com.agricraft.agricraft.compat.starfield.StarfieldCompat;
import com.google.gson.JsonParser;
import com.stardew.craft.api.v1.agriculture.StardewAgricultureDataApi;
import com.stardew.craft.api.v1.agriculture.StardewCropData;
import com.stardew.craft.block.ModBlocks;
import com.stardew.craft.block.crop.CornCropBlock;
import com.stardew.craft.block.crop.StardewCropBlock;
import com.stardew.craft.manager.CropGrowthManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@GameTestHolder("agricraft")
@PrefixGameTestTemplate(false)
public final class StarfieldCatalogGameTests {
    private static volatile StardewCropData providerData;

    @GameTest(templateNamespace = "agricraft", template = "starfield_probe")
    public static void allNativeSpeciesMatchVerifiedCatalog(GameTestHelper helper) throws Exception {
        try (var stream = StarfieldCatalogGameTests.class.getResourceAsStream("/data/agricraft/starfield_probe_catalog.json")) {
            var expected = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonArray("crops");
            helper.assertTrue(StarfieldCompat.species().size() == expected.size(),
                    "Not all 50 native species resolved: " + StarfieldCompat.diagnostics());
            for (var element : expected) {
                var row = element.getAsJsonObject();
                var id = ResourceLocation.parse(row.get("id").getAsString());
                var actual = StarfieldCompat.species(id).orElseThrow();
                helper.assertTrue(actual.seed().toString().equals(row.get("seed").getAsString()), "Wrong native seed: " + id);
                helper.assertTrue(StarfieldCompat.seed(new ItemStack(BuiltInRegistries.ITEM.get(actual.seed())))
                                .orElseThrow().id().equals(id), "Seed lookup failed: " + id);
                if (!row.get("produce").isJsonNull()) helper.assertTrue(actual.produce().orElseThrow().toString()
                        .equals(row.get("produce").getAsString()), "Wrong native produce: " + id);
                else helper.assertTrue(actual.produce().isEmpty() && actual.seasonalForage(), "Wild seeds gained a fixed yield");
                var phases = row.getAsJsonArray("phaseDays").asList().stream().map(value -> value.getAsInt()).toList();
                helper.assertTrue(actual.nativePhaseDays().equals(phases), "Native phases changed: " + id);
                helper.assertTrue(actual.twoParts() == row.get("geometry").getAsString().equals("two_parts"), "Wrong geometry: " + id);
                int regrow = row.get("regrowDays").isJsonNull() ? -1 : row.get("regrowDays").getAsInt();
                helper.assertTrue(actual.nativeRegrowDays() == regrow, "Wrong native regrowth: " + id);
                helper.assertTrue(actual.requiresScythe() == row.get("harvest").getAsString().equals("scythe"), "Wrong native harvest method: " + id);
            }
            helper.assertTrue(StarfieldCompat.diagnostics().isEmpty(), "Official catalog has invalid entries");
            helper.assertTrue(StarfieldCompat.species(ResourceLocation.parse("stardewcraft:missing_crop")).isEmpty()
                            && StarfieldCompat.seed(new ItemStack(Items.DIAMOND)).isEmpty(), "Unknown identity was substituted");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "agricraft", template = "starfield_probe")
    public static void nativeSoilAllowlist(GameTestHelper helper) {
        helper.assertTrue(StarfieldCompat.soil(ModBlocks.FARMLAND.get().defaultBlockState()).isPresent(), "Native farmland missing");
        helper.assertTrue(StarfieldCompat.soil(ModBlocks.SANDY_FARMLAND.get().defaultBlockState()).orElseThrow().blocksSprinklers(), "Sandy soil allows sprinklers");
        helper.assertTrue(StarfieldCompat.soil(ModBlocks.INFERTILE_FARMLAND.get().defaultBlockState()).orElseThrow().infertile(), "Infertile soil misclassified");
        helper.assertTrue(StarfieldCompat.soil(ModBlocks.GARDEN_POT.get().defaultBlockState()).isPresent(), "Garden pot missing");
        helper.assertTrue(StarfieldCompat.soil(Blocks.FARMLAND.defaultBlockState()).isEmpty()
                        && StarfieldCompat.soil(Blocks.DIRT.defaultBlockState()).isEmpty(), "Foreign soil was silently admitted");
        helper.succeed();
    }

    @GameTest(templateNamespace = "agricraft", template = "starfield_probe")
    public static void upperPartResolvesOneRoot(GameTestHelper helper) {
        var level = helper.getLevel();
        var root = helper.absolutePos(new BlockPos(1, 2, 1));
        var crop = ModBlocks.CORN_CROP.get();
        level.setBlock(root.below(), ModBlocks.FARMLAND.get().defaultBlockState(), 3);
        level.setBlock(root, crop.defaultBlockState(), 2);
        level.setBlock(root.above(), crop.defaultBlockState().setValue(CornCropBlock.HALF, DoubleBlockHalf.UPPER), 2);
        try {
            var lower = StarfieldCompat.inspect(level, root).orElseThrow();
            var upper = StarfieldCompat.inspect(level, root.above()).orElseThrow();
            helper.assertTrue(lower.root().equals(upper.root()) && upper.part() == StarfieldCompat.Part.UPPER,
                    "Upper crop part has a different planting root");
            helper.assertTrue(upper.species().id().equals(ResourceLocation.parse("stardewcraft:corn_crop")), "Wrong upper species");
            helper.succeed();
        } finally {
            level.setBlock(root.above(), Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(root, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    @GameTest(templateNamespace = "agricraft", template = "starfield_probe")
    public static void contextualMetadataRejectsUnknownIdsWithoutChangingPlant(GameTestHelper helper) {
        var level = helper.getLevel();
        var root = helper.absolutePos(new BlockPos(1, 2, 1));
        var crop = ModBlocks.POTATO_CROP.get();
        StardewAgricultureDataApi.registerCropProvider(ResourceLocation.parse("agricraft:catalog_probe"), 1000,
                (world, position, state) -> state.is(crop) ? providerData : null);
        level.setBlock(root.below(), ModBlocks.FARMLAND.get().defaultBlockState(), 3);
        level.setBlock(root, crop.defaultBlockState(), 2);
        var before = level.getBlockState(root);
        var manager = CropGrowthManager.get(level);
        var growth = manager.getOrCreateState(level, root);
        growth.lastDailyDay = 7;
        try {
            providerData = data(ResourceLocation.parse("stardewcraft:potato"));
            helper.assertTrue(StarfieldCompat.inspect(level, root).orElseThrow().metadata().orElseThrow()
                            .farmingExperience() == 999, "World-aware metadata provider was ignored");
            providerData = data(ResourceLocation.parse("stardewcraft:unknown_produce"));
            helper.assertTrue(StarfieldCompat.inspect(level, root).isEmpty()
                            && StarfieldCompat.diagnostics().stream().anyMatch(value -> value.contains("unknown_produce")),
                    "Unknown metadata item lacks a useful diagnostic");
            StarfieldCompat.refresh(level);
            helper.assertTrue(level.getBlockState(root) == before && growth.lastDailyDay == 7 && growth.phase == 0,
                    "Metadata refresh destroyed/reset the native planting");
            providerData = null;
            helper.assertTrue(StarfieldCompat.inspect(level, root).isPresent(), "Fixed metadata did not recover lookup");
            helper.succeed();
        } finally {
            providerData = null;
            StarfieldCompat.refresh(level);
            level.setBlock(root, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private static StardewCropData data(ResourceLocation produce) {
        return new StardewCropData(List.of("spring"), List.of(1, 1, 1, 2, 1), -1, 999,
                ResourceLocation.parse("stardewcraft:grab"), produce, ResourceLocation.parse("stardewcraft:potato_seeds"));
    }
}
