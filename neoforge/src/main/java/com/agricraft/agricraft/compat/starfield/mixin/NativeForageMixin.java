package com.agricraft.agricraft.compat.starfield.mixin;

import com.agricraft.agricraft.compat.starfield.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo
@Mixin(targets="com.stardew.craft.block.nature.ForageBlock", remap=false)
public abstract class NativeForageMixin extends Block {
    protected NativeForageMixin(Properties properties) { super(properties); }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        StarfieldCompat.forageRemoving(state, level, pos, next);
        super.onRemove(state, level, pos, next, moving);
    }
    @WrapMethod(method="pickForage")
    private InteractionResult agricraft$pickup(Level level, BlockPos pos, Player player, Operation<InteractionResult> original) {
        if (StarfieldCompat.isActive() && level instanceof ServerLevel && level.getBlockState(pos).getBlock() != (Object)this)
            return InteractionResult.FAIL;
        if (!(level instanceof ServerLevel server) || StarfieldCompat.planting(server, pos).isEmpty()) return original.call(level,pos,player);
        var result = new InteractionResult[]{InteractionResult.FAIL};
        try { StarfieldCompat.harvest(server,pos,player,() -> result[0]=original.call(level,pos,player)); return result[0]; }
        catch (StarfieldHarvestTransaction.Aborted failure) { return InteractionResult.FAIL; }
    }
    @WrapMethod(method="harvestForage")
    private void agricraft$primary(ServerLevel level, BlockPos pos, ServerPlayer player, Operation<Void> original) {
        StarfieldHarvestTransaction.primary(() -> original.call(level,pos,player));
    }
    @WrapOperation(method="harvestForage", at=@At(value="INVOKE", target="Lcom/stardew/craft/player/PlayerStardewDataAPI;addExperience(Lnet/minecraft/server/level/ServerPlayer;Lcom/stardew/craft/player/SkillType;I)Z"))
    private boolean agricraft$experience(ServerPlayer player, @Coerce Object skill, int amount, Operation<Boolean> original) {
        return StarfieldHarvestTransaction.awardExperience(() -> original.call(player,skill,amount));
    }
    @WrapMethod(method="useItemOn")
    private ItemInteractionResult agricraft$tool(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit, Operation<ItemInteractionResult> original) {
        if (StarfieldCompat.inspect(level,pos).isPresent() && StarfieldCompat.rejectHarvestFallback(stack)) return ItemInteractionResult.FAIL;
        return original.call(stack,state,level,pos,player,hand,hit);
    }
}
