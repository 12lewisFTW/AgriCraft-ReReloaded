package com.agricraft.agricraft.compat.starfield.mixin;

import com.agricraft.agricraft.compat.starfield.StarfieldGeneEffects;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import java.util.function.Consumer;

/** Applies the gene once to each freshly calculated native schedule, never to saved progress. */
@Pseudo
@Mixin(targets = "com.stardew.craft.block.crop.StardewCropBlock", remap = false)
public abstract class NativeCropGeneMixin {
    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method = "growCropOneDayInternal")
    private void agricraft$seasonPause(ServerLevel level, BlockPos pos, BlockState state, boolean wet, @Coerce Object growth, Operation<Void> original) {
        if (!com.agricraft.agricraft.compat.starfield.StarfieldCompat.seasonPaused(level, pos, state)) original.call(level, pos, state, wet, growth);
    }

    @WrapOperation(method = "growCropOneDayInternal", at = @At(value = "INVOKE", target =
            "Lcom/stardew/craft/manager/CropGrowthManager$CropGrowthState;advanceOnSoil(Z)Z"))
    private boolean agricraft$strength(@Coerce Object growth, boolean infertile, Operation<Boolean> original,
            ServerLevel level, BlockPos pos, BlockState state, boolean wet, @Coerce Object passedGrowth) {
        return com.agricraft.agricraft.compat.starfield.StarfieldCompat.soilAdvance(level, pos, infertile, () -> original.call(growth, infertile));
    }

    private static final String SCHEDULE = "Lcom/stardew/craft/block/crop/StardewCropBlock;applySpeedGroToPhaseDays([IF)[I";
    private static final String REGROW = "Lcom/stardew/craft/block/crop/StardewCropBlock;getRegrowDays()I";

    @WrapOperation(method = "isMature", at = @At(value = "INVOKE", target = SCHEDULE))
    private int[] agricraft$maturity(int[] days, float boost, Operation<int[]> original,
            ServerLevel level, BlockPos pos, BlockState state) {
        return StarfieldGeneEffects.phases(level, pos, original.call(days, boost));
    }

    @WrapOperation(method = "growCropOneDayInternal", at = @At(value = "INVOKE", target = SCHEDULE))
    private int[] agricraft$daily(int[] days, float boost, Operation<int[]> original,
            ServerLevel level, BlockPos pos, BlockState state, boolean wet, @Coerce Object growth) {
        return StarfieldGeneEffects.phases(level, pos, original.call(days, boost));
    }

    @WrapOperation(method = "syncVisualStage", at = @At(value = "INVOKE", target = SCHEDULE))
    private int[] agricraft$visual(int[] days, float boost, Operation<int[]> original,
            ServerLevel level, BlockPos pos, @Coerce Object growth) {
        return StarfieldGeneEffects.phases(level, pos, original.call(days, boost));
    }

    @WrapOperation(method = "harvest", at = @At(value = "INVOKE", target = SCHEDULE))
    private int[] agricraft$harvest(int[] days, float boost, Operation<int[]> original,
            ServerLevel level, BlockPos pos, BlockState state, Player player) {
        return StarfieldGeneEffects.phases(level, pos, original.call(days, boost));
    }

    @WrapOperation(method = "tryHarvestByJunimo", at = @At(value = "INVOKE", target = SCHEDULE))
    private int[] agricraft$junimo(int[] days, float boost, Operation<int[]> original,
            ServerLevel level, BlockPos pos, BlockState state, int farming, Consumer<ItemStack> output) {
        return StarfieldGeneEffects.phases(level, pos, original.call(days, boost));
    }

    @WrapOperation(method = "harvest", at = @At(value = "INVOKE", target = REGROW))
    private int agricraft$regrow(@Coerce Object crop, Operation<Integer> original,
            ServerLevel level, BlockPos pos, BlockState state, Player player) {
        return StarfieldGeneEffects.regrow(level, pos, original.call(crop));
    }

    @WrapOperation(method = "tryHarvestByJunimo", at = @At(value = "INVOKE", target = REGROW))
    private int agricraft$junimoRegrow(@Coerce Object crop, Operation<Integer> original,
            ServerLevel level, BlockPos pos, BlockState state, int farming, Consumer<ItemStack> output) {
        return StarfieldGeneEffects.regrow(level, pos, original.call(crop));
    }
}
