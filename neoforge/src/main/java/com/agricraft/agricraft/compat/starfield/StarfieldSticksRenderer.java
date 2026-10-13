package com.agricraft.agricraft.compat.starfield;

import com.agricraft.agricraft.api.config.CompatConfig;
import com.agricraft.agricraft.common.util.PlatformClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Existing AgriCraft models at the real native root, including tall crops and planted forage. */
public final class StarfieldSticksRenderer {
    private StarfieldSticksRenderer() {}

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES || !CompatConfig.enableStarfield) return;
        var minecraft = Minecraft.getInstance(); var level = minecraft.level;
        if (level == null) return;
        var camera = event.getCamera().getPosition();
        var poses = event.getPoseStack();
        var buffers = minecraft.renderBuffers().bufferSource();
        int radius = Math.min(minecraft.options.getEffectiveRenderDistance(), 4);
        StarfieldPlantClient.forEachSticks(new ChunkPos(BlockPos.containing(camera)), radius, (root, entry) -> {
            if (!level.getChunkSource().hasChunk(root.getX() >> 4, root.getZ() >> 4) || root.distToCenterSqr(camera) > 64 * 64
                    || !event.getFrustum().isVisible(new AABB(root).inflate(0.25))) return;
            var state = level.getBlockState(root);
            if (!BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(entry.form())) return;
            var model = PlatformClient.get().getStandaloneModel(ResourceLocation.fromNamespaceAndPath("agricraft",
                    "block/" + entry.sticks() + "_crop_sticks"));
            if (model == null) return;
            poses.pushPose();
            poses.translate(root.getX() - camera.x, root.getY() - camera.y, root.getZ() - camera.z);
            minecraft.getBlockRenderer().getModelRenderer().renderModel(poses.last(), buffers.getBuffer(RenderType.cutoutMipped()),
                    state, model, 1, 1, 1, LevelRenderer.getLightColor(level, root), OverlayTexture.NO_OVERLAY);
            poses.popPose();
        });
        buffers.endBatch(RenderType.cutoutMipped());
    }
}
