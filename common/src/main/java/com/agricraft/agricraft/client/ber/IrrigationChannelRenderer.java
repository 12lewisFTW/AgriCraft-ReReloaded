package com.agricraft.agricraft.client.ber;

import com.agricraft.agricraft.common.block.IrrigationChannelBlock;
import com.agricraft.agricraft.common.block.entity.IrrigationChannelBlockEntity;
import com.agricraft.agricraft.common.block.entity.IrrigationComponentBlockEntity;
import com.agricraft.agricraft.common.util.PlatformClient;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Renders the water inside an irrigation channel plus, when a valve is installed, the animated
 * hand-wheel and the rising/lowering gate that visibly blocks or clears the flow.
 */
public class IrrigationChannelRenderer extends IrrigationComponentRenderer<IrrigationChannelBlockEntity> {

	private static final ResourceLocation IRON_BLOCK = ResourceLocation.withDefaultNamespace("block/iron_block");
	private static final ResourceLocation VALVE_WHEEL_MODEL = ResourceLocation.fromNamespaceAndPath("agricraft", "block/channel/valve_wheel");
	private static final float U = 1 / 16.0F;
	private static final RandomSource RANDOM = RandomSource.create();

	public IrrigationChannelRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public void render(IrrigationChannelBlockEntity channel, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
		super.render(channel, partialTick, poseStack, buffer, packedLight, packedOverlay);
		if (channel.getBlockState().getValue(IrrigationChannelBlock.VALVE) != IrrigationChannelBlock.Valve.NONE) {
			this.renderValve(channel, partialTick, poseStack, buffer, packedLight, packedOverlay);
		}
	}

	@Override
	protected void renderWater(IrrigationChannelBlockEntity channel, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
		BlockState state = channel.getBlockState();
		boolean open = channel.canTransfer();
		double height = channel.getMinFluidHeight() + channel.getFillFraction() * (channel.getMaxFluidHeight() - channel.getMinFluidHeight());
		float y = (float) Math.min(height, 10.0 / 16.0 - 0.001);
		float bottom = (float) channel.getMinFluidHeight();
		boolean north = state.getValue(IrrigationChannelBlock.connection(Direction.NORTH));
		boolean south = state.getValue(IrrigationChannelBlock.connection(Direction.SOUTH));
		boolean west = state.getValue(IrrigationChannelBlock.connection(Direction.WEST));
		boolean east = state.getValue(IrrigationChannelBlock.connection(Direction.EAST));
		// central trough surface: only while the valve (if any) is open/absent - matches the original,
		// where a closed valve visually blocks the water right where its gate sits, instead of the
		// water showing through/around the gate
		if (open) {
			this.drawWaterSurface(channel, poseStack, buffer, packedLight, packedOverlay, 6 / 16.0F, 6 / 16.0F, 10 / 16.0F, 10 / 16.0F, y);
		}
		// When the valve is closed, this channel's own content is frozen (it no longer balances with
		// anyone), but a neighbour past a closed side may still be draining (e.g. a sprinkler further
		// down the line still consuming). Track that neighbour's own level instead of ours, so the
		// water on that side visibly drains along with it instead of staying stuck at the old level.
		float northY = open ? y : this.sideLevel(channel, Direction.NORTH, y, bottom);
		float southY = open ? y : this.sideLevel(channel, Direction.SOUTH, y, bottom);
		float westY = open ? y : this.sideLevel(channel, Direction.WEST, y, bottom);
		float eastY = open ? y : this.sideLevel(channel, Direction.EAST, y, bottom);
		// connection surfaces
		if (north && northY > bottom) {
			this.drawWaterSurface(channel, poseStack, buffer, packedLight, packedOverlay, 6 / 16.0F, 0.0F, 10 / 16.0F, 6 / 16.0F, northY);
		}
		if (south && southY > bottom) {
			this.drawWaterSurface(channel, poseStack, buffer, packedLight, packedOverlay, 6 / 16.0F, 10 / 16.0F, 10 / 16.0F, 1.0F, southY);
		}
		if (west && westY > bottom) {
			this.drawWaterSurface(channel, poseStack, buffer, packedLight, packedOverlay, 0.0F, 6 / 16.0F, 6 / 16.0F, 10 / 16.0F, westY);
		}
		if (east && eastY > bottom) {
			this.drawWaterSurface(channel, poseStack, buffer, packedLight, packedOverlay, 10 / 16.0F, 6 / 16.0F, 1.0F, 10 / 16.0F, eastY);
		}
		// vertical side faces around the perimeter, so the water is not see-through from the side
		// when a neighbouring channel holds less water. Each side sits at the block edge if the
		// channel connects that way, otherwise at the trough wall.
		float nz = north ? 0.0F : 6 / 16.0F;
		float sz = south ? 1.0F : 10 / 16.0F;
		float wx = west ? 0.0F : 6 / 16.0F;
		float ex = east ? 1.0F : 10 / 16.0F;
		this.drawWaterSide(channel, poseStack, buffer, packedLight, packedOverlay, 6 / 16.0F, nz, 10 / 16.0F, nz, bottom, northY);
		this.drawWaterSide(channel, poseStack, buffer, packedLight, packedOverlay, 6 / 16.0F, sz, 10 / 16.0F, sz, bottom, southY);
		this.drawWaterSide(channel, poseStack, buffer, packedLight, packedOverlay, wx, 6 / 16.0F, wx, 10 / 16.0F, bottom, westY);
		this.drawWaterSide(channel, poseStack, buffer, packedLight, packedOverlay, ex, 6 / 16.0F, ex, 10 / 16.0F, bottom, eastY);
	}

	/** The render level to use for the connection on the given side: the neighbour's own current
	 *  fluid surface (clamped into this channel's trough range) if there is one, otherwise the given
	 *  fallback (this channel's own level). */
	private float sideLevel(IrrigationChannelBlockEntity channel, Direction dir, float ownY, float bottom) {
		if (channel.getLevel() == null) {
			return ownY;
		}
		if (channel.getLevel().getBlockEntity(channel.getBlockPos().relative(dir)) instanceof IrrigationComponentBlockEntity neighbour) {
			double localY = neighbour.getFluidSurfaceY() - channel.getBlockPos().getY();
			return Mth.clamp((float) localY, bottom, 10 / 16.0F - 0.001F);
		}
		return ownY;
	}

	private void renderValve(IrrigationChannelBlockEntity channel, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
		float f = channel.getValveProgress(partialTick);
		TextureAtlasSprite iron = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(IRON_BLOCK);
		VertexConsumer consumer = buffer.getBuffer(RenderType.cutout());

		// gate: sits low in the trough when closed (Y6-10, blocking the flow), rises when opened
		float minY = Mth.lerp(f, 6, 9) * U;
		float maxY = Mth.lerp(f, 10, 13) * U;
		this.drawBox(poseStack, consumer, iron, packedLight, packedOverlay, 6 * U, minY, 6 * U, 10 * U, maxY, 10 * U);
		// shaft from the gate top up to the hand-wheel
		this.drawBox(poseStack, consumer, iron, packedLight, packedOverlay, 7 * U, maxY, 7 * U, 9 * U, 15 * U, 9 * U);

		// hand-wheel: the original baked model (correct red hand-wheel texture), rotating 0->180 as it opens
		BakedModel wheelModel = PlatformClient.get().getStandaloneModel(VALVE_WHEEL_MODEL);
		if (wheelModel != null) {
			poseStack.pushPose();
			poseStack.translate(0.5, 0, 0.5);
			poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(Mth.lerp(f, 0, 180)));
			poseStack.translate(-0.5, 0, -0.5);
			PoseStack.Pose pose = poseStack.last();
			for (Direction dir : Direction.values()) {
				this.renderQuads(pose, consumer, wheelModel.getQuads(null, dir, RANDOM), packedLight, packedOverlay);
			}
			this.renderQuads(pose, consumer, wheelModel.getQuads(null, null, RANDOM), packedLight, packedOverlay);
			poseStack.popPose();
		}
	}

	private void renderQuads(PoseStack.Pose pose, VertexConsumer consumer, List<BakedQuad> quads, int packedLight, int packedOverlay) {
		for (BakedQuad quad : quads) {
			consumer.putBulkData(pose, quad, 1.0F, 1.0F, 1.0F, 1.0F, packedLight, packedOverlay);
		}
	}

	private void drawBox(PoseStack poseStack, VertexConsumer consumer, TextureAtlasSprite sprite, int light, int overlay,
	                     float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		Matrix4f m = poseStack.last().pose();
		float u0 = sprite.getU0();
		float u1 = sprite.getU1();
		float v0 = sprite.getV0();
		float v1 = sprite.getV1();
		this.quad(consumer, m, light, overlay, u0, v0, u1, v1, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, 0, -1, 0);
		this.quad(consumer, m, light, overlay, u0, v0, u1, v1, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, 0, 1, 0);
		this.quad(consumer, m, light, overlay, u0, v0, u1, v1, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, 0, 0, -1);
		this.quad(consumer, m, light, overlay, u0, v0, u1, v1, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, 0, 0, 1);
		this.quad(consumer, m, light, overlay, u0, v0, u1, v1, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, -1, 0, 0);
		this.quad(consumer, m, light, overlay, u0, v0, u1, v1, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, 1, 0, 0);
	}

	private void quad(VertexConsumer consumer, Matrix4f m, int light, int overlay, float u0, float v0, float u1, float v1,
	                  float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4,
	                  int nx, int ny, int nz) {
		consumer.addVertex(m, x1, y1, z1).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
		consumer.addVertex(m, x2, y2, z2).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
		consumer.addVertex(m, x3, y3, z3).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
		consumer.addVertex(m, x4, y4, z4).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
	}

}
