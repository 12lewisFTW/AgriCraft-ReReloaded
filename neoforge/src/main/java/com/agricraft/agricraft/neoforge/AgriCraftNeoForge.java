package com.agricraft.agricraft.neoforge;

import com.agricraft.agricraft.AgriCraft;
import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.codecs.AgriMutation;
import com.agricraft.agricraft.api.codecs.AgriSoil;
import com.agricraft.agricraft.api.config.CompatConfig;
import com.agricraft.agricraft.api.config.CoreConfig;
import com.agricraft.agricraft.api.fertilizer.AgriFertilizer;
import com.agricraft.agricraft.api.plant.AgriPlant;
import com.agricraft.agricraft.api.plant.AgriWeed;
import com.agricraft.agricraft.common.commands.DumpRegistriesCommand;
import com.agricraft.agricraft.common.commands.GiveSeedCommand;
import com.agricraft.agricraft.common.handler.DenyBonemeal;
import com.agricraft.agricraft.common.handler.VanillaSeedConversion;
import com.agricraft.agricraft.common.greenhouse.Greenhouses;
import com.agricraft.agricraft.common.neoforge.IrrigationTankFluidHandler;
import com.agricraft.agricraft.common.plugin.MysticalAgricultureCompat;
import com.agricraft.agricraft.compat.theoneprobe.AgriCraftProbePlugin;
import com.agricraft.agricraft.compat.starfield.StarfieldCompat;
import com.agricraft.agricraft.common.neoforge.registry.NeoForgeGlobalLootModifiers;
import com.agricraft.agricraft.common.registry.ModBlockEntityTypes;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import com.agricraft.agricraft.common.util.Platform;
import com.agricraft.agricraft.common.util.neoforge.NeoForgePlatform;
import com.agricraft.agricraft.common.util.neoforge.NeoForgeRegistry;
import com.agricraft.agricraft.plugin.minecraft.MinecraftPlugin;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforgespi.language.IModInfo;

import java.nio.file.Files;
import java.nio.file.Path;

@Mod(AgriApi.MOD_ID)
public class AgriCraftNeoForge {

	public AgriCraftNeoForge(IEventBus bus) {
		NeoForgeRegistry.setModBus(bus);
		Platform.setup(new NeoForgePlatform());
		AgriCraft.init();
        StarfieldCompat.register(bus);
		NeoForgeGlobalLootModifiers.register(bus);
		bus.addListener(AgriCraftNeoForge::onCommonSetup);
		bus.addListener(AgriCraftNeoForge::onInterModEnqueue);
		bus.addListener(AgriCraftNeoForge::onRegisterDatapackRegistry);
		bus.addListener(AgriCraftNeoForge::onAddPackFinders);
		bus.addListener(AgriCraftNeoForge::onRegisterCapabilities);
		NeoForge.EVENT_BUS.addListener(AgriCraftNeoForge::onRegisterCommands);
		NeoForge.EVENT_BUS.addListener(AgriCraftNeoForge::onRightClick);
		NeoForge.EVENT_BUS.addListener(AgriCraftNeoForge::onRightClickBonemeal);
		NeoForge.EVENT_BUS.addListener(AgriCraftNeoForge::onNeighborNotify);
	}

	public static void onCommonSetup(FMLCommonSetupEvent event) {
		MinecraftPlugin.init();
		if (ModList.get().isLoaded(CompatConfig.MYSTICAL_AGRICULTURE_MOD_ID)) {
			event.enqueueWork(MysticalAgricultureCompat::init);
		}
//		SereneSeasonPlugin.init();
	}

	public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntityTypes.IRRIGATION_TANK.get(),
				(tank, side) -> new IrrigationTankFluidHandler(tank));
	}

	public static void onInterModEnqueue(InterModEnqueueEvent event) {
		if (ModList.get().isLoaded("theoneprobe")) {
			InterModComms.sendTo("theoneprobe", "getTheOneProbe", AgriCraftProbePlugin::new);
		}
	}

	public static void onRegisterDatapackRegistry(DataPackRegistryEvent.NewRegistry event) {
		event.dataPackRegistry(AgriApi.AGRIPLANTS, AgriPlant.CODEC, AgriPlant.CODEC);
		event.dataPackRegistry(AgriApi.AGRIWEEDS, AgriWeed.CODEC, AgriWeed.CODEC);
		event.dataPackRegistry(AgriApi.AGRISOILS, AgriSoil.CODEC, AgriSoil.CODEC);
		event.dataPackRegistry(AgriApi.AGRIMUTATIONS, AgriMutation.CODEC, AgriMutation.CODEC);
		event.dataPackRegistry(AgriApi.AGRIFERTILIZERS, AgriFertilizer.CODEC, AgriFertilizer.CODEC);
	}

	public static void onRegisterCommands(RegisterCommandsEvent event) {
		GiveSeedCommand.register(event.getDispatcher(), event.getBuildContext());
		DumpRegistriesCommand.register(event.getDispatcher(), event.getBuildContext());
	}

	public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
		InteractionResult result = VanillaSeedConversion.onRightClick(event.getEntity(), event.getHand(), event.getPos(), event.getHitVec());
		if (result != InteractionResult.PASS) {
			event.setCanceled(true);
		}
	}

	public static void onRightClickBonemeal(PlayerInteractEvent.RightClickBlock event) {
		if (DenyBonemeal.denyBonemeal(event.getEntity(), event.getHand(), event.getPos(), event.getLevel())) {
			event.setCanceled(true);
		}
	}

	public static void onNeighborNotify(net.neoforged.neoforge.event.level.BlockEvent.NeighborNotifyEvent event) {
		if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
			Greenhouses.onBlockChanged(serverLevel, event.getPos(), event.getState());
		}
	}

	public static void onAddPackFinders(AddPackFindersEvent event) {
		if (event.getPackType() == PackType.SERVER_DATA) {
			for (IModInfo mod : ModList.get().getMods()) {
				String modId = mod.getModId();
				if (!modId.equals("minecraft") && !modId.equals("agricraft") && CompatConfig.isCompatibilityEnabled(modId)) {
					addPack("datapacks", modId, PackType.SERVER_DATA, event);
				}
			}
		}
		if (event.getPackType() == PackType.CLIENT_RESOURCES) {
			for (IModInfo mod : ModList.get().getMods()) {
				String modId = mod.getModId();
				if (!modId.equals("minecraft") && !modId.equals("agricraft") && CompatConfig.isCompatibilityEnabled(modId)) {
					addPack("resourcepacks", modId, PackType.CLIENT_RESOURCES, event);
				}
			}
		}
	}

	public static void addPack(String type, String modid, PackType packType, AddPackFindersEvent event) {
		Path resourcePath = ModList.get().getModFileById(AgriApi.MOD_ID).getFile().findResource(type, modid);
		if (!Files.exists(resourcePath)) return;
		String id = "builtin/agricraft_" + type + "_" + modid;
		Pack.ResourcesSupplier resources = new PathPackResources.PathResourcesSupplier(resourcePath);
		PackLocationInfo locationInfo = new PackLocationInfo(id, Component.translatable("agricraft." + type + "." + modid), PackSource.BUILT_IN, java.util.Optional.empty());
		Pack pack = Pack.readMetaAndCreate(locationInfo, resources, packType, new PackSelectionConfig(CoreConfig.enablePacksByDefault, Pack.Position.TOP, false));
		if (pack != null) {
			event.addRepositorySource(packConsumer -> packConsumer.accept(pack));
		}
	}


}
