package com.agricraft.agricraft.starfield.prototype.save;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Development observers. Never calls the world/chunk API to load or generate anything. */
@EventBusSubscriber(modid = "agricraft")
public final class SaveDiagnostics {
    private static final Logger LOG = LogUtils.getLogger();
    private static volatile boolean closing;
    private static final AtomicLong GENERATIONS = new AtomicLong();
    // Includes the initial not-ready scheduling attempt; repeated growth of this
    // counter with an unchanged holder is evidence of a loop, not one sample alone.
    private static final AtomicLong RETRIES = new AtomicLong();
    private static final AtomicLong ALL_GENERATIONS = new AtomicLong();
    private static final AtomicLong ALL_UNLOADS = new AtomicLong();
    private static final ConcurrentHashMap<ChunkHolder, String> BLOCKED = new ConcurrentHashMap<>();

    public static void generation(ServerLevel level, ChunkPos pos, ChunkStatus status) {
        ALL_GENERATIONS.incrementAndGet();
        if (closing && GENERATIONS.incrementAndGet() <= 24)
            LOG.warn("[SAVE_DIAG] generation while stopping: dimension={} chunk={} status={}",
                    level.dimension().location(), pos, status, new Exception("Generation request origin"));
    }

    public static void unload(ServerLevel level, ChunkHolder holder) {
        ALL_UNLOADS.incrementAndGet();
        if (!closing || holder.isReadyForSaving()) return;
        RETRIES.incrementAndGet();
        if (BLOCKED.putIfAbsent(holder, level.dimension().location() + " " + holder.getPos()) == null)
            LOG.warn("[SAVE_DIAG] unload not ready: {} refs={} saveDone={}",
                    BLOCKED.get(holder), holder.getGenerationRefCount(), holder.getSaveSyncFuture().isDone());
    }

    public static void stopping(MinecraftServer server) {
        if (closing) return;
        closing = true;
        LOG.info("[SAVE_DIAG] stopServer entry: tick={} pid={}", server.getTickCount(), ProcessHandle.current().pid());
        Thread observer = new Thread(() -> {
            try {
                while (closing) {
                    Thread.sleep(5000);
                    if (!closing) return;
                    LOG.warn("[SAVE_DIAG] stopping sample: retries={} generations={} blocked={}", RETRIES.get(), GENERATIONS.get(), BLOCKED.size());
                    BLOCKED.forEach((holder, dimension) -> {
                        if (!holder.isReadyForSaving()) LOG.warn("[SAVE_DIAG] still blocked: {} refs={} saveDone={}",
                                dimension, holder.getGenerationRefCount(), holder.getSaveSyncFuture().isDone());
                    });
                }
            } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
        }, "AgriCraft save observer");
        observer.setDaemon(true);
        observer.start();
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        closing = false;
        LOG.info("[SAVE_DIAG] ServerStopped: retries={} generations={} blocked={} observedGeneration={} observedUnload={}",
                RETRIES.get(), GENERATIONS.get(), BLOCKED.size(), ALL_GENERATIONS.get(), ALL_UNLOADS.get());
    }
}
