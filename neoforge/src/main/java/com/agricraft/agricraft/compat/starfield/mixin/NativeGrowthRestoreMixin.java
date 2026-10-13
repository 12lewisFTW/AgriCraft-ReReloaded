package com.agricraft.agricraft.compat.starfield.mixin;

import com.agricraft.agricraft.compat.starfield.StarfieldCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.stardew.craft.manager.CropGrowthManager", remap = false)
public abstract class NativeGrowthRestoreMixin {
    @Inject(method = "restoreGrowthState", at = @At("RETURN"))
    private void agricraft$restored(ServerLevel level, BlockPos pos, @Coerce Object growth, CallbackInfo ci) {
        StarfieldCompat.restored(level, pos, growth);
    }
}
