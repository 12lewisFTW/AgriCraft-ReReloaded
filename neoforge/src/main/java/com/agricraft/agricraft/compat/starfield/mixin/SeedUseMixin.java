package com.agricraft.agricraft.compat.starfield.mixin;

import com.agricraft.agricraft.compat.starfield.StarfieldCompat;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemStack.class)
public abstract class SeedUseMixin {
    @WrapMethod(method = "useOn")
    private InteractionResult agricraft$nativeSeed(UseOnContext context, Operation<InteractionResult> original) {
        var sticks = StarfieldCompat.sticks(context);
        if (sticks.isPresent()) return sticks.get();
        var transfer = StarfieldCompat.trowel(context);
        if (transfer.isPresent()) return transfer.get();
        return StarfieldCompat.useSeed(context, () -> original.call(context));
    }
}
