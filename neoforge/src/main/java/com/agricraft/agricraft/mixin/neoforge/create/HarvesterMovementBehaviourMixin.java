package com.agricraft.agricraft.mixin.neoforge.create;

import com.agricraft.agricraft.common.block.entity.CropBlockEntity;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.actors.harvester.HarvesterMovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets Create's harvester actor harvest AgriCraft crops using AgriCraft's own harvesting
 * mechanics (weed/growth progression, genetics driven drops).
 * <p>
 * Originally the standalone "AgriCraft Create Compat" mod by Kuki2008 (MIT); integrated here so it
 * builds against this fork's NeoForge version. Only applied when Create is present
 * (see {@code agricraft-neoforge.mixins.json}, {@code required=false}).
 */
@Mixin(HarvesterMovementBehaviour.class)
public class HarvesterMovementBehaviourMixin {

	@Inject(
			method = "visitNewPosition(Lcom/simibubi/create/content/contraptions/behaviour/MovementContext;Lnet/minecraft/core/BlockPos;)V",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
					ordinal = 0
			),
			cancellable = true
	)
	private void agricraft$harvest(MovementContext context, BlockPos pos, CallbackInfo ci) {
		Level world = context.world;

		if (world.isClientSide()) {
			return;
		}

		var nativeCrop = com.agricraft.agricraft.api.compat.ExternalHarvestSupport.inspect(world, pos);
		if (nativeCrop != com.agricraft.agricraft.api.compat.ExternalHarvestSupport.Availability.UNHANDLED) {
			com.agricraft.agricraft.api.AgriApi.harvest(world, pos,
					stack -> ((MovementBehaviour) (Object) this).collectOrDropItem(context, stack));
			ci.cancel();
			return;
		}

		if (!(world.getBlockEntity(pos) instanceof CropBlockEntity crop)) {
			return;
		}

		if (!crop.hasPlant()) {
			return;
		}

		if (crop.harvest(stack -> {
			if (!stack.isEmpty()) {
				((MovementBehaviour) (Object) this).collectOrDropItem(context, stack);
			}
		}, null)) {
			if (crop.hasWeeds()) {
				crop.setWeedGrowthStage(crop.getWeed().getInitialGrowthStage());
			}
		} else {
			crop.setGrowthStage(crop.getGrowthStage().getPrevious(crop, world.random));
			if (crop.hasWeeds()) {
				crop.setWeedGrowthStage(crop.getWeedGrowthStage().getPrevious(crop, world.random));
			}
		}
	}
}
