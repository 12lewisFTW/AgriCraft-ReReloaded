package com.agricraft.agricraft.compat.starfield.mixin;

import com.agricraft.agricraft.compat.starfield.StarfieldCompat;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets="com.stardew.craft.block.crop.WildSeedCropBlock", remap=false)
public abstract class NativeWildFormMixin {
    @WrapMethod(method="transformToForage")
    private void agricraft$form(ServerLevel level, BlockPos pos, Operation<Void> original) {
        StarfieldCompat.transformed(level, pos, () -> original.call(level, pos));
    }
}
