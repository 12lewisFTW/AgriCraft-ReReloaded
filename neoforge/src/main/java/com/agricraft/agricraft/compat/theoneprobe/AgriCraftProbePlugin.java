package com.agricraft.agricraft.compat.theoneprobe;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.crop.AgriCrop;
import com.agricraft.agricraft.api.stat.AgriStatRegistry;
import com.agricraft.agricraft.common.util.LangUtils;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.api.ProbeMode;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Comparator;
import java.util.function.Function;

/** Loaded through IMC only when The One Probe is installed. Runs on the server. */
public final class AgriCraftProbePlugin implements Function<ITheOneProbe, Void>, IProbeInfoProvider {

	@Override
	public Void apply(ITheOneProbe probe) {
		probe.registerProvider(this);
		return null;
	}

	@Override
	public ResourceLocation getID() {
		return ResourceLocation.fromNamespaceAndPath(AgriApi.MOD_ID, "crops");
	}

	@Override
	public void addProbeInfo(ProbeMode mode, IProbeInfo info, Player player, Level level, BlockState state, IProbeHitData hit) {
		if (com.agricraft.agricraft.compat.starfield.StarfieldCompat.isActive()
                && player instanceof net.minecraft.server.level.ServerPlayer server)
			com.agricraft.agricraft.compat.starfield.StarfieldInspectionSync.inspect(server, hit.getPos()).ifPresent(tag ->
					com.agricraft.agricraft.compat.starfield.StarfieldInspection.tooltip(tag, mode != ProbeMode.NORMAL).forEach(info::text));
		AgriApi.getCrop(level, hit.getPos()).ifPresent(crop -> addCropInfo(mode, info, crop));
	}

	private static void addCropInfo(ProbeMode mode, IProbeInfo info, AgriCrop crop) {
		if (crop.hasPlant()) {
			int percent = (int) Math.round(crop.getGrowthPercent() * 100);
			info.text(Component.translatable("agricraft.tooltip.jade.growth", percent));
			if (mode != ProbeMode.NORMAL) {
				info.text(Component.translatable("agricraft.tooltip.jade.species")
						.append(LangUtils.plantName(crop.getGenome().getSpeciesGene().getTrait())));
				AgriStatRegistry.getInstance().stream()
						.filter(stat -> !stat.isHidden())
						.map(stat -> crop.getGenome().getStatGene(stat))
						.sorted(Comparator.comparing(pair -> pair.getGene().getId()))
						.forEach(pair -> info.text(Component.translatable("agricraft.tooltip.jade.stat." + pair.getGene().getId(), pair.getTrait())));
			}
		} else {
			info.text(Component.translatable("agricraft.tooltip.magnifying.no_plant"));
		}
		if (crop.hasWeeds()) {
			info.text(Component.translatable("agricraft.tooltip.magnifying.weeds").append(LangUtils.weedName(crop.getWeedId())));
		}
	}
}
