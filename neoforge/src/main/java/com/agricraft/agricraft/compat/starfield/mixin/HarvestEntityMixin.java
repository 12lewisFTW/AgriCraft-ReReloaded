package com.agricraft.agricraft.compat.starfield.mixin;

import com.agricraft.agricraft.compat.starfield.StarfieldHarvestTransaction;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerLevel.class)
public abstract class HarvestEntityMixin {
    @WrapMethod(method = "addFreshEntity")
    private boolean agricraft$stageHarvestItem(Entity entity, Operation<Boolean> original) {
        return StarfieldHarvestTransaction.addEntity((ServerLevel) (Object) this, entity, () -> original.call(entity));
    }
}
