package com.agricraft.agricraft.compat.starfield.mixin;

import com.agricraft.agricraft.compat.starfield.StarfieldCompat;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Pseudo
@Mixin(targets = "com.stardew.craft.api.v1.internal.giant.GiantCropGrowth", remap = false)
public abstract class NativeGiantLifecycleMixin {
    private static final String PROCESS = "process(Lnet/minecraft/server/level/ServerLevel;Ljava/util/Collection;Ljava/util/Set;IZLjava/util/Collection;)Lcom/stardew/craft/api/v1/internal/giant/GiantCropGrowth$Statistics;";

    @Inject(method = PROCESS, at = @At("HEAD"))
    private static void agricraft$begin(ServerLevel level, Collection<BlockPos> roots, Set<BlockPos> watered,
            int day, boolean offline, Collection<BlockPos> triggers, CallbackInfoReturnable<Object> ci) {
        StarfieldCompat.giantBegin();
    }

    @Inject(method = PROCESS, at = @At("RETURN"))
    private static void agricraft$end(ServerLevel level, Collection<BlockPos> roots, Set<BlockPos> watered,
            int day, boolean offline, Collection<BlockPos> triggers, CallbackInfoReturnable<Object> ci) {
        StarfieldCompat.giantEnd();
    }

    @WrapOperation(method = PROCESS, at = @At(value = "INVOKE", target = "Lcom/stardew/craft/api/v1/agriculture/StardewGiantCrops$Handler;plan(Lcom/stardew/craft/api/v1/agriculture/StardewGiantCrops$Context;Ljava/util/List;)Ljava/util/List;"))
    private static List<?> agricraft$snapshot(@Coerce Object handler, @Coerce Object context, List<?> crops,
            Operation<List<?>> original) {
        return StarfieldCompat.giantPlan(context, crops, original.call(handler, context, crops));
    }

    @WrapOperation(method = PROCESS, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;updateNeighborsAt(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;)V"))
    private static void agricraft$returnSticks(ServerLevel level, BlockPos pos, Block block, Operation<Void> original) {
        StarfieldCompat.giantCommit(level);
        original.call(level, pos, block);
    }
}
