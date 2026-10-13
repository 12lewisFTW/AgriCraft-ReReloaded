package com.agricraft.agricraft.compat.starfield;

import com.stardew.craft.block.crop.StardewCropBlock;
import com.stardew.craft.block.crop.WildSeedCropBlock;
import com.stardew.craft.block.nature.ForageBlock;
import com.stardew.craft.item.crop.CactusSeedItem;
import com.stardew.craft.player.PlayerDataManager;
import com.stardew.craft.player.PlayerStardewData;
import com.stardew.craft.time.StardewTimeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import java.lang.reflect.Method;
import java.util.*;

/** Native special-form identity and gates; no forage selection or quality algorithm is copied. */
final class NativeStarfieldSpecials {
    private static final Method PICK, CACTUS;
    private static final java.lang.reflect.Field FORAGE_SEASONS;
    private static final Map<ResourceLocation, Set<ResourceLocation>> FORMS = new HashMap<>();
    static {
        try {
            PICK = WildSeedCropBlock.class.getDeclaredMethod("pickRandomForage", net.minecraft.util.RandomSource.class); PICK.setAccessible(true);
            CACTUS = CactusSeedItem.class.getDeclaredMethod("isPlantingAllowed", net.minecraft.world.level.Level.class, BlockPos.class, int.class); CACTUS.setAccessible(true);
            FORAGE_SEASONS = ForageBlock.class.getDeclaredField("allowedSeasonMask"); FORAGE_SEASONS.setAccessible(true);
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException("Verified native special accessors missing", failure); }
    }

    static boolean validForm(StarfieldPlantData.Entry entry, BlockState state) {
        if (!(state.getBlock() instanceof ForageBlock) || entry.form().equals(entry.type())
                || !entry.form().equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()))
                || !(BuiltInRegistries.BLOCK.get(entry.type()) instanceof WildSeedCropBlock wild)) return false;
        return FORMS.computeIfAbsent(entry.type(), ignored -> {
            var forms = new HashSet<ResourceLocation>();
            // The pinned picker has at most four choices. Enumerate its own branches, not a copied ID table.
            for (int choice = 0; choice < 4; choice++) {
                final int index = choice;
                var random = new net.minecraft.world.level.levelgen.LegacyRandomSource(0) {
                    @Override public int nextInt(int bound) { return Math.floorMod(index, bound); }
                };
                try { forms.add(BuiltInRegistries.BLOCK.getKey((net.minecraft.world.level.block.Block) PICK.invoke(wild, random))); }
                catch (ReflectiveOperationException failure) { throw new IllegalStateException("Cannot enumerate native forage forms", failure); }
            }
            return Set.copyOf(forms);
        }).contains(entry.form());
    }

    static Optional<StarfieldPlantData.Entry> forageEntry(ServerLevel level, BlockPos root) {
        var data = StarfieldPlantData.existing(level);
        return data == null ? Optional.empty() : data.get(GlobalPos.of(level.dimension(), root))
                .filter(entry -> validForm(entry, level.getBlockState(root)) && StarfieldCompat.species(entry.type()).isPresent());
    }

    static void transformed(ServerLevel level, BlockPos root, Runnable original) {
        var entry = NativeStarfieldLifecycle.ensure(level, root);
        original.run();
        if (entry.isEmpty()) return;
        var old = entry.get();
        var next = new StarfieldPlantData.Entry(old.plantingId(), old.type(), old.genome(), old.seedTemplate(), old.sticks(),
                BuiltInRegistries.BLOCK.getKey(level.getBlockState(root).getBlock()));
        if (validForm(next, level.getBlockState(root))) StarfieldPlantData.get(level).put(level, root, next);
    }

    static boolean plantingAllowed(ServerLevel level, BlockPos root, BlockState state) {
        if (!(state.getBlock() instanceof StardewCropBlock crop)) return false;
        var seed = BuiltInRegistries.ITEM.get(StarfieldCompat.species(BuiltInRegistries.BLOCK.getKey(crop)).orElseThrow().seed());
        if (!(seed instanceof CactusSeedItem)) return true;
        try { return (boolean) CACTUS.invoke(seed, level, root, StardewTimeManager.get().getCurrentSeason()); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException("Cannot read native cactus location gate", failure); }
    }

    static void removing(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState next) {
        if (level instanceof ServerLevel server && !state.is(next.getBlock())) {
            var data = StarfieldPlantData.existing(server);
            if (data != null) data.get(GlobalPos.of(level.dimension(), pos)).filter(entry -> validForm(entry, state)).ifPresent(entry -> {
                NativeStarfieldLifecycle.detached(server,pos,entry); data.remove(server,pos);
            });
        }
    }

    static void harvestForage(ServerLevel level, BlockPos root, Player player, Runnable original) {
        if (!(player instanceof ServerPlayer server) || !StarfieldCompat.mayUseTools(server, root))
            throw new StarfieldHarvestTransaction.Aborted(new IllegalStateException("Native forage access denied"));
        if (!StarfieldPlantData.get(level).writable(GlobalPos.of(level.dimension(), root)))
            throw new StarfieldHarvestTransaction.Aborted(new IllegalStateException("Unsupported forage sidecar"));
        var crop = NativeStarfieldTransfer.capture(level, root);
        var manager = PlayerDataManager.get(); var data = manager == null ? null : manager.getData(player.getUUID());
        var saved = data == null ? null : data.toNBT(level.registryAccess());
        StarfieldHarvestTransaction.run(level, StarfieldGeneEffects.gain(level, root),
                stack -> StarfieldGenomes.writeProduce(stack, crop.entry()), () -> {
            original.run();
            if (level.getBlockState(root).getBlock() == crop.lower().getBlock()) throw new IllegalStateException("Forage pickup did not remove root");
            NativeStarfieldSticks.afterHarvest(level, root, crop.entry());
        }, () -> {
            if (!NativeStarfieldTransfer.restore(level, root, crop)) throw new IllegalStateException("Cannot restore rejected forage pickup");
        }, () -> {
            if (manager != null) {
                if (saved == null) manager.removePlayerData(player.getUUID());
                else manager.savePlayerData(player.getUUID(), PlayerStardewData.fromNBT(saved, player.getUUID(), level.registryAccess()));
            }
        });
        NativeStarfieldLifecycle.consumed(crop.entry().plantingId());
    }

    static Optional<net.minecraft.nbt.CompoundTag> inspection(ServerLevel level, BlockPos root) {
        var entry = forageEntry(level, root); if (entry.isEmpty()) return Optional.empty();
        var state = level.getBlockState(root); var soil = level.getBlockState(root.below());
        var tag = new net.minecraft.nbt.CompoundTag(); tag.putInt("schema",1);
        tag.putString("dimension",level.dimension().location().toString()); tag.putLong("root",root.asLong());
        tag.put("entry",entry.get().encode()); tag.putString("state",state.toString()); tag.putString("soil",soil.toString());
        tag.putBoolean("mature",true); tag.putBoolean("forage",true); tag.putInt("remaining",0); tag.putInt("phase",-1); tag.putInt("phases",0);
        tag.putBoolean("wet",soil.hasProperty(net.minecraft.world.level.block.FarmBlock.MOISTURE) && soil.getValue(net.minecraft.world.level.block.FarmBlock.MOISTURE)>0);
        int season = StardewTimeManager.get().getCurrentSeason(); tag.putString("season",List.of("spring","summer","fall","winter").get(season));
        try {
            int mask = FORAGE_SEASONS.getInt(state.getBlock());
            tag.putBoolean("inSeason",mask==0 || !level.canSeeSky(root) || (mask & (1<<season))!=0);
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException("Cannot read native forage seasons", failure); }
        return Optional.of(tag);
    }
}
