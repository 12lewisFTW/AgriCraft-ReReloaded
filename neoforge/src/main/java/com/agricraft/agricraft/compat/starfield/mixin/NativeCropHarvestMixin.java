package com.agricraft.agricraft.compat.starfield.mixin;

import com.agricraft.agricraft.compat.starfield.StarfieldCompat;
import com.agricraft.agricraft.compat.starfield.StarfieldHarvestTransaction;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

@Pseudo
@Mixin(targets = "com.stardew.craft.block.crop.StardewCropBlock", remap = false)
public abstract class NativeCropHarvestMixin {
    @WrapMethod(method = "tryHarvestByJunimo")
    private ItemStack agricraft$automation(ServerLevel level, BlockPos pos, BlockState state, int farming,
            java.util.function.Consumer<ItemStack> output, Operation<ItemStack> original) {
        return StarfieldCompat.automation(level, pos, output, sink -> original.call(level, pos, state, farming, sink));
    }
    @WrapMethod(method = "spawnHarvestDrops")
    private void agricraft$primary(ServerLevel level, BlockPos pos, BlockState state, net.minecraft.util.RandomSource random,
            int fertilizer, int farming, Player player, Operation<Void> original) {
        StarfieldHarvestTransaction.primary(() -> original.call(level, pos, state, random, fertilizer, farming, player));
    }

    @WrapMethod(method = "harvest")
    private void agricraft$transaction(ServerLevel level, BlockPos pos, BlockState state, Player player, Operation<Void> original) {
        StarfieldCompat.harvest(level, pos, player, () -> original.call(level, pos, state, player));
    }

    @WrapOperation(method = "harvest", at = @At(value = "INVOKE", target =
            "Lcom/stardew/craft/player/PlayerStardewDataAPI;addExperience(Lnet/minecraft/server/level/ServerPlayer;Lcom/stardew/craft/player/SkillType;I)Z"))
    private boolean agricraft$deferExperience(ServerPlayer player, @Coerce Object skill, int amount, Operation<Boolean> original) {
        return StarfieldHarvestTransaction.awardExperience(() -> original.call(player, skill, amount));
    }

    @WrapMethod(method = "tryHarvestByHand")
    private boolean agricraft$handResult(ServerLevel level, BlockPos pos, BlockState state, Player player, Operation<Boolean> original) {
        try { return original.call(level, pos, state, player); }
        catch (StarfieldHarvestTransaction.Aborted failure) { return false; }
    }

    @WrapMethod(method = "tryHarvestByTool")
    private boolean agricraft$toolResult(ServerLevel level, BlockPos pos, BlockState state, Player player, boolean force, Operation<Boolean> original) {
        try { return original.call(level, pos, state, player, force); }
        catch (StarfieldHarvestTransaction.Aborted failure) { return false; }
    }

    @WrapMethod(method = "tryRightClickHarvest")
    private InteractionResult agricraft$clickResult(BlockState state, Level level, BlockPos pos, Player player, Operation<InteractionResult> original) {
        try { return original.call(state, level, pos, player); }
        catch (StarfieldHarvestTransaction.Aborted failure) { return InteractionResult.FAIL; }
    }

    @WrapMethod(method = "useItemOn")
    private ItemInteractionResult agricraft$toolAction(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit, Operation<ItemInteractionResult> original) {
        if (StarfieldCompat.rejectHarvestFallback(stack)) return ItemInteractionResult.FAIL;
        return original.call(stack, state, level, pos, player, hand, hit);
    }
}
