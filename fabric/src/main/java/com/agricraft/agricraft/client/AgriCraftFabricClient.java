package com.agricraft.agricraft.client;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.client.ber.CropBlockEntityRenderer;
import com.agricraft.agricraft.client.ber.SeedAnalyzerEntityRenderer;
import com.agricraft.agricraft.client.ber.SprinklerRenderer;
import com.agricraft.agricraft.client.bewlr.AgriSeedBEWLR;
import com.agricraft.agricraft.client.gui.MagnifyingGlassOverlay;
import com.agricraft.agricraft.client.gui.SeedAnalyzerScreen;
import com.agricraft.agricraft.common.registry.ModBlockEntityTypes;
import com.agricraft.agricraft.common.registry.ModBlocks;
import com.agricraft.agricraft.common.registry.ModItems;
import com.agricraft.agricraft.common.registry.ModMenus;
import com.agricraft.agricraft.common.util.LangUtils;
import com.agricraft.agricraft.common.util.PlatformClient;
import com.agricraft.agricraft.common.util.fabric.FabricPlatformClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.server.packs.resources.Resource;

import java.util.Map;

public class AgriCraftFabricClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		PlatformClient.setup(new FabricPlatformClient());
		AgriCraftClient.init();
		BuiltinItemRendererRegistry.INSTANCE.register(ModItems.SEED.get(), AgriSeedBEWLR.INSTANCE::renderByItem);
		ModelLoadingPlugin.register(pluginContext -> {
			pluginContext.addModels(SprinklerRenderer.BLADES_MODEL);
			for (Map.Entry<ResourceLocation, Resource> entry : FileToIdConverter.json("models/seed").listMatchingResources(Minecraft.getInstance().getResourceManager()).entrySet()) {
				ResourceLocation seed = ResourceLocation.fromNamespaceAndPath(entry.getKey().getNamespace(), entry.getKey().getPath().replace("models/seed", "seed").replace(".json", ""));
				pluginContext.addModels(seed);
			}
			for (Map.Entry<ResourceLocation, Resource> entry : FileToIdConverter.json("models/crop").listMatchingResources(Minecraft.getInstance().getResourceManager()).entrySet()) {
				ResourceLocation crop = ResourceLocation.fromNamespaceAndPath(entry.getKey().getNamespace(), entry.getKey().getPath().replace("models/crop", "crop").replace(".json", ""));
				pluginContext.addModels(crop);
			}
			for (Map.Entry<ResourceLocation, Resource> entry : FileToIdConverter.json("models/weed").listMatchingResources(Minecraft.getInstance().getResourceManager()).entrySet()) {
				ResourceLocation crop = ResourceLocation.fromNamespaceAndPath(entry.getKey().getNamespace(), entry.getKey().getPath().replace("models/weed", "weed").replace(".json", ""));
				pluginContext.addModels(crop);
			}
			// add the crop sticks models else they're not loaded
			pluginContext.addModels(ResourceLocation.parse("agricraft:block/wooden_crop_sticks"), ResourceLocation.parse("agricraft:block/iron_crop_sticks"), ResourceLocation.parse("agricraft:block/obsidian_crop_sticks"),
					ResourceLocation.parse("agricraft:block/wooden_cross_crop_sticks"), ResourceLocation.parse("agricraft:block/iron_cross_crop_sticks"), ResourceLocation.parse("agricraft:block/obsidian_cross_crop_sticks"));
		});

		BlockEntityRenderers.register(ModBlockEntityTypes.CROP.get(), CropBlockEntityRenderer::new);
		BlockEntityRenderers.register(ModBlockEntityTypes.SEED_ANALYZER.get(), SeedAnalyzerEntityRenderer::new);
		MenuScreens.register(ModMenus.SEED_ANALYZER_MENU.get(), SeedAnalyzerScreen::new);

		HudRenderCallback.EVENT.register((guiGraphics, deltaTracker) -> {
			MagnifyingGlassOverlay.renderOverlay(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false));
		});
		ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipType, lines) -> {
			if (stack.has(DataComponents.CUSTOM_DATA) && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("magnifying")) {
				lines.add(1, Component.translatable("agricraft.tooltip.magnifying").withStyle(ChatFormatting.DARK_GRAY).withStyle(ChatFormatting.ITALIC));
			}
			AgriApi.getSoilRegistry().ifPresent(registry -> registry.forEach(soil -> {
						if (soil.isVariant(stack.getItem())) {
							lines.add(1, Component.translatable("agricraft.tooltip.magnifying.soil.soil").withStyle(ChatFormatting.DARK_GRAY));
							lines.add(2, Component.literal("  ").withStyle(ChatFormatting.DARK_GRAY)
									.append(Component.translatable("agricraft.tooltip.magnifying.soil.humidity"))
									.append(LangUtils.soilPropertyName("humidity", soil.humidity())));
							lines.add(3, Component.literal("  ").withStyle(ChatFormatting.DARK_GRAY)
									.append(Component.translatable("agricraft.tooltip.magnifying.soil.acidity"))
									.append(LangUtils.soilPropertyName("acidity", soil.acidity())));
							lines.add(4, Component.literal("  ").withStyle(ChatFormatting.DARK_GRAY)
									.append(Component.translatable("agricraft.tooltip.magnifying.soil.nutrients"))
									.append(LangUtils.soilPropertyName("nutrients", soil.nutrients())));
						}
					}
			));
		});
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.SEED_ANALYZER.get(), RenderType.cutout());
	}

}
