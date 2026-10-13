package com.agricraft.agricraft.starfield.prototype.save.mixin;

import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.TestFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Collection;

/** Replays the historical seven-test layout without the later vanilla wheat test. */
@Mixin(GameTestRegistry.class)
public abstract class HistoricalGameTestsMixin {
    @Inject(method = "getAllTestFunctions", at = @At("RETURN"), cancellable = true)
    private static void historicalTests(CallbackInfoReturnable<Collection<TestFunction>> ci) {
        if (Boolean.getBoolean("agricraft.starfield.saveHistoricalTests"))
            ci.setReturnValue(ci.getReturnValue().stream().filter(test ->
                    !test.testName().endsWith("ordinarysprinklerstillpayswatersandgrowsvanillawheat")).toList());
    }
}
