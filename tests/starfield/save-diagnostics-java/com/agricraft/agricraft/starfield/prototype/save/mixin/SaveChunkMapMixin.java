package com.agricraft.agricraft.starfield.prototype.save.mixin;

import com.agricraft.agricraft.starfield.prototype.save.SaveDiagnostics;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkGenerationTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkMap.class)
public abstract class SaveChunkMapMixin {
    @Shadow @Final private ServerLevel level;

    @Inject(method = "scheduleGenerationTask", at = @At("HEAD"))
    private void recordGeneration(ChunkStatus status, ChunkPos pos, CallbackInfoReturnable<ChunkGenerationTask> ci) {
        SaveDiagnostics.generation(level, pos, status);
    }

    @Inject(method = "scheduleUnload", at = @At("HEAD"))
    private void recordUnload(long position, ChunkHolder holder, CallbackInfo ci) {
        SaveDiagnostics.unload(level, holder);
    }
}
