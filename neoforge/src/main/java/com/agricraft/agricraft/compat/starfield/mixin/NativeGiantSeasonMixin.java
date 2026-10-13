package com.agricraft.agricraft.compat.starfield.mixin;

import com.agricraft.agricraft.compat.starfield.StarfieldCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import java.util.Collection;
import java.util.Set;

/** Paused out-of-season crops cannot turn their grace window into a new giant form. */
@Pseudo
@Mixin(targets = "com.stardew.craft.api.v1.internal.giant.GiantCropGrowth", remap = false)
public abstract class NativeGiantSeasonMixin {
    @ModifyVariable(method = "process(Lnet/minecraft/server/level/ServerLevel;Ljava/util/Collection;Ljava/util/Set;IZLjava/util/Collection;)Lcom/stardew/craft/api/v1/internal/giant/GiantCropGrowth$Statistics;",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static Collection<BlockPos> agricraft$seasonRoots(Collection<BlockPos> roots, ServerLevel level,
            Collection<BlockPos> positions, Set<BlockPos> watered, int day, boolean offline, Collection<BlockPos> triggers) {
        return StarfieldCompat.giantSeasonEligible(level, roots, day, offline);
    }
}
