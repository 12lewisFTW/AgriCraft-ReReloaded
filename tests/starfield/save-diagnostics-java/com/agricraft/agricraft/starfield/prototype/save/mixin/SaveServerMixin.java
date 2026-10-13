package com.agricraft.agricraft.starfield.prototype.save.mixin;

import com.agricraft.agricraft.starfield.prototype.save.SaveDiagnostics;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observe before any mod's ServerStopping handler can request another chunk. */
@Mixin(MinecraftServer.class)
public abstract class SaveServerMixin {
    @Inject(method = "stopServer", at = @At("HEAD"))
    private void beginObservation(CallbackInfo ci) {
        SaveDiagnostics.stopping((MinecraftServer)(Object)this);
    }
}
