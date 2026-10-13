package com.agricraft.agricraft.starfield.prototype;

import com.agricraft.agricraft.compat.starfield.StarfieldCompat;
import com.stardew.craft.block.ModBlocks;
import com.stardew.craft.manager.CropGrowthManager;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@GameTestHolder("agricraft")
@PrefixGameTestTemplate(false)
public final class StarfieldDatapackGameTests {
    @GameTest(templateNamespace = "agricraft", template = "starfield_probe", batch = "starfield_reload", timeoutTicks = 1200)
    public static void realDatapackReloadPreservesPlantAndQuarantinesInvalidMetadata(GameTestHelper helper) {
        new ReloadProbe(helper).start();
    }

    private static final class ReloadProbe {
        private static final ResourceLocation POTATO = ResourceLocation.parse("stardewcraft:potato_crop");
        private final GameTestHelper helper;
        private final BlockPos root;
        private final Path pack;
        private final Path metadata;
        private final List<String> originalPacks;
        private final CropGrowthManager.CropGrowthState growth;

        ReloadProbe(GameTestHelper helper) {
            this.helper = helper;
            var level = helper.getLevel();
            root = helper.absolutePos(new BlockPos(1, 2, 1));
            pack = level.getServer().getWorldPath(LevelResource.DATAPACK_DIR).resolve("agricraft-starfield-reload-probe");
            metadata = pack.resolve("data/stardewcraft/data_maps/block/stardew_crop_data.json");
            originalPacks = List.copyOf(level.getServer().getPackRepository().getSelectedIds());
            level.setBlock(root.below(), ModBlocks.FARMLAND.get().defaultBlockState(), 3);
            level.setBlock(root, ModBlocks.POTATO_CROP.get().defaultBlockState(), 2);
            growth = CropGrowthManager.get(level).getOrCreateState(level, root);
            growth.phase = 2;
            growth.dayInPhase = 1;
            growth.lastDailyDay = 17;
        }

        void start() {
            checked(() -> {
                write(pack.resolve("pack.mcmeta"), "{\"pack\":{\"pack_format\":48,\"description\":\"AgriCraft reload test\"}}");
                writeMetadata("stardewcraft:potato");
                var selected = new ArrayList<>(originalPacks);
                selected.add("file/agricraft-starfield-reload-probe");
                reload(selected, this::checkValid);
            });
        }

        void checkValid() {
            helper.assertTrue(StarfieldCompat.species(POTATO).orElseThrow().metadata().orElseThrow()
                            .farmingExperience() == 999, "Datapack reload did not refresh the native catalog");
            assertPlantUnchanged();
            writeMetadata("stardewcraft:unknown_produce");
            reload(helper.getLevel().getServer().getPackRepository().getSelectedIds(), this::checkInvalid);
        }

        void checkInvalid() {
            helper.assertTrue(StarfieldCompat.species(POTATO).isEmpty()
                            && StarfieldCompat.species().size() == 49
                            && StarfieldCompat.diagnostics().stream().anyMatch(value -> value.contains("unknown_produce")),
                    "Invalid global metadata was not isolated with a useful diagnostic");
            helper.assertTrue(com.agricraft.agricraft.api.compat.ExternalIrrigationSupport.irrigate(
                    helper.getLevel(), root, helper.getLevel().getBlockState(root))
                            == com.agricraft.agricraft.api.compat.ExternalIrrigationSupport.Result.UNHANDLED,
                    "Native irrigation processed a quarantined species");
            assertPlantUnchanged();
            deleteProbeFiles();
            reload(originalPacks, () -> {
                helper.assertTrue(StarfieldCompat.species(POTATO).isPresent() && StarfieldCompat.species().size() == 50,
                        "Native catalog did not recover after removing the invalid datapack");
                helper.assertTrue(com.agricraft.agricraft.api.compat.ExternalIrrigationSupport.irrigate(
                        helper.getLevel(), root, helper.getLevel().getBlockState(root))
                                == com.agricraft.agricraft.api.compat.ExternalIrrigationSupport.Result.CONTINUE,
                        "Native irrigation did not recover with the validated catalog");
                assertPlantUnchanged();
                helper.getLevel().setBlock(root, Blocks.AIR.defaultBlockState(), 3);
                helper.succeed();
            });
        }

        void assertPlantUnchanged() {
            helper.assertTrue(helper.getLevel().getBlockState(root).is(ModBlocks.POTATO_CROP.get())
                            && growth.phase == 2 && growth.dayInPhase == 1 && growth.lastDailyDay == 17,
                    "Reload reset or replaced an existing native planting");
        }

        void reload(Collection<String> packs, Runnable next) {
            var server = helper.getLevel().getServer();
            var selected = List.copyOf(packs);
            server.getPackRepository().reload();
            server.reloadResources(selected).whenComplete((ignored, failure) -> server.execute(() ->
                    helper.runAfterDelay(1, () -> checked(() -> {
                        if (failure != null) throw new IllegalStateException("Resource reload failed", failure);
                        next.run();
                    }))));
        }

        void checked(Runnable action) {
            try { action.run(); }
            catch (RuntimeException failure) {
                deleteProbeFiles();
                helper.getLevel().setBlock(root, Blocks.AIR.defaultBlockState(), 3);
                throw failure;
            }
        }

        void writeMetadata(String produce) {
            write(metadata, """
                    {"values":{"stardewcraft:potato_crop":{
                        "seasons":["spring"],"phase_days":[1,1,1,2,1],"regrow_days":-1,
                        "farming_experience":999,"harvest_method":"stardewcraft:grab",
                        "produce":"%s","seed":"stardewcraft:potato_seeds"
                    }}}
                    """.formatted(produce));
        }

        void deleteProbeFiles() {
            try {
                Files.deleteIfExists(metadata);
                Files.deleteIfExists(pack.resolve("pack.mcmeta"));
            } catch (IOException failure) { throw new UncheckedIOException(failure); }
        }

        static void write(Path path, String text) {
            try { Files.createDirectories(path.getParent()); Files.writeString(path, text); }
            catch (IOException failure) { throw new UncheckedIOException(failure); }
        }
    }
}
