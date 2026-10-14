package com.agricraft.agricraft.client.ber;

import com.agricraft.agricraft.common.block.entity.SprinklerBlockEntity;
import com.agricraft.agricraft.common.util.PlatformClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the rotating arms of the sprinkler head using their resource-pack model and UVs.
 */
public class SprinklerRenderer implements BlockEntityRenderer<SprinklerBlockEntity> {

	public static final ResourceLocation BLADES_MODEL = ResourceLocation.fromNamespaceAndPath("agricraft", "block/sprinkler_blades");

	public SprinklerRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public void render(SprinklerBlockEntity sprinkler, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
		BakedModel blades = PlatformClient.get().getStandaloneModel(BLADES_MODEL);
		if (blades == null) {
			return;
		}
		// Match the smooth acceleration/deceleration on the logical side.
		float angle = sprinkler.angle + sprinkler.rotationSpeed * partialTick;
		poseStack.pushPose();
		poseStack.translate(0.5, 0, 0.5);
		poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(angle));
		poseStack.translate(-0.5, 0, -0.5);
		Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
				poseStack.last(), buffer.getBuffer(RenderType.cutout()), sprinkler.getBlockState(),
				blades, 1, 1, 1, packedLight, packedOverlay);
		poseStack.popPose();
	}

}
