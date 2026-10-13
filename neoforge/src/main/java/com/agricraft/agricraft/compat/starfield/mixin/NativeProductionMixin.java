package com.agricraft.agricraft.compat.starfield.mixin;

import com.agricraft.agricraft.compat.starfield.StarfieldCompat;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import java.util.Optional;

@Pseudo
@Mixin(targets = "com.stardew.craft.blockentity.TimedProductionBlockEntity", remap = false)
public abstract class NativeProductionMixin {
    @WrapMethod(method = "prepareProduction")
    private Optional<?> agricraft$heredity(ItemStack input, ItemStack output, int minutes, Player player,
            boolean automation, Operation<Optional<?>> original) {
        return StarfieldCompat.production(this, input, original.call(input, output, minutes, player, automation));
    }
}
