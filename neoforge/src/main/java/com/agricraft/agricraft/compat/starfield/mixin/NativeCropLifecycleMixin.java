package com.agricraft.agricraft.compat.starfield.mixin;

import com.agricraft.agricraft.compat.starfield.StarfieldCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.stardew.craft.block.crop.StardewCropBlock", remap = false)
public abstract class NativeCropLifecycleMixin {
    @Inject(method = "onPlace", at = @At("RETURN"))
    private void agricraft$placed(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving, CallbackInfo ci) {
        StarfieldCompat.placed(state, level, pos, old);
    }
    @Inject(method = "onRemove", at = @At("HEAD"))
    private void agricraft$removing(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving, CallbackInfo ci) {
        StarfieldCompat.removing(state, level, pos, next);
    }
}
