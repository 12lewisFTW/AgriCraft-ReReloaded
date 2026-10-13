package com.agricraft.agricraft.common.block.entity;

import com.agricraft.agricraft.api.config.IrrigationConfig;
import com.agricraft.agricraft.common.block.SprinklerBlock;
import com.agricraft.agricraft.common.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity of the sprinkler: hangs below an irrigation channel, drains water from it and
 * irrigates a 7x7 area up to 5 blocks below itself, keeping farmland moist and randomly giving
 * growth ticks to plants.
 */
public class SprinklerBlockEntity extends BlockEntity {

	public static final int WORK_HEIGHT = 5;
	public static final int WORK_RADIUS = 3;
	public static final int WORK_DIAMETER = 2 * WORK_RADIUS + 1;
	public static final int WORK_AREA = WORK_DIAMETER * WORK_DIAMETER;

	private static final float MAX_ROTATION_SPEED = 9.0F;
	private static final float ROTATION_ACCELERATION = 0.5F;

	private int waterBuffer;
	private int columnCounter = -1;
	/** Ticks elapsed since the current (or last) irrigation cycle started. */
	private int cycleTimer;

	/** client-side rotation angle of the sprinkler head, in degrees */
	public float angle;
	/** client-side rotation speed (degrees/tick), eases towards 0 or {@link #MAX_ROTATION_SPEED} */
	public float rotationSpeed;

	public SprinklerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntityTypes.SPRINKLER.get(), pos, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, SprinklerBlockEntity sprinkler) {
		if (level.isClientSide) {
			sprinkler.clientTick(level, pos, state);
		} else {
			sprinkler.serverTick(level, pos, state);
		}
	}

	private int getBufferSize() {
		return 2 * IrrigationConfig.sprinklerWaterConsumption;
	}

	/** Consumption is configured in mB/second, spread over 20 ticks. */
	private int getConsumptionPerTick() {
		int consumption = IrrigationConfig.sprinklerWaterConsumption;
		return consumption <= 0 ? 0 : Math.max(1, consumption / 20);
	}

	protected void serverTick(Level level, BlockPos pos, BlockState state) {
		int previousBuffer = this.waterBuffer;
		int previousColumn = this.columnCounter;
		this.pullWater(level, pos);
		boolean active = false;
		// sprinklerInterval is the minimum time between the START of two cycles, not a pause
		// after a cycle finishes: a full sweep of the working area takes WORK_AREA ticks (with
		// water available), so as long as the interval is shorter than that (the default), the
		// timer has already elapsed by the time the sweep completes and the next one starts
		// immediately, with no visible stop.
		this.cycleTimer++;
		if (this.columnCounter < 0 && this.cycleTimer >= IrrigationConfig.sprinklerInterval) {
			this.columnCounter = 0;
			this.cycleTimer = 0;
		}
		if (this.columnCounter >= 0) {
			int consumption = this.getConsumptionPerTick();
			if (this.waterBuffer >= consumption) {
				this.waterBuffer -= consumption;
				this.irrigateColumn(level, pos, this.columnCounter);
				this.columnCounter++;
				if (this.columnCounter >= WORK_AREA) {
					this.columnCounter = -1;
				}
				active = true;
			}
		}
		if (state.getValue(SprinklerBlock.ACTIVE) != active) {
			level.setBlock(pos, state.setValue(SprinklerBlock.ACTIVE, active), Block.UPDATE_ALL);
		}
		if (previousBuffer != this.waterBuffer || previousColumn != this.columnCounter) this.setChanged();
	}

	protected void clientTick(Level level, BlockPos pos, BlockState state) {
		float target = state.getValue(SprinklerBlock.ACTIVE) ? MAX_ROTATION_SPEED : 0.0F;
		if (this.rotationSpeed < target) {
			this.rotationSpeed = Math.min(target, this.rotationSpeed + ROTATION_ACCELERATION);
		} else if (this.rotationSpeed > target) {
			this.rotationSpeed = Math.max(target, this.rotationSpeed - ROTATION_ACCELERATION);
		}
		if (this.rotationSpeed > 0) {
			this.angle = (this.angle + this.rotationSpeed) % 360.0F;
			this.spawnParticles(level, pos);
		}
	}

	protected void pullWater(Level level, BlockPos pos) {
		if (this.waterBuffer >= this.getBufferSize()) {
			return;
		}
		BlockEntity above = level.getBlockEntity(pos.above());
		if (above instanceof IrrigationChannelBlockEntity channel && channel.canTransfer()) {
			this.waterBuffer += channel.drainWater(this.getBufferSize() - this.waterBuffer, true);
		}
	}

	/** Irrigates a single column of the 7x7 working area, from top to bottom. */
	protected void irrigateColumn(Level level, BlockPos pos, int column) {
		int dx = column % WORK_DIAMETER - WORK_RADIUS;
		int dz = column / WORK_DIAMETER - WORK_RADIUS;
		RandomSource random = level.getRandom();
		for (int dy = 1; dy <= WORK_HEIGHT; dy++) {
			BlockPos target = pos.offset(dx, -dy, dz);
			if (target.getY() < level.getMinBuildHeight()) {
				return;
			}
			if (!level.hasChunkAt(target)) return;
			BlockState state = level.getBlockState(target);
			if (state.isAir()) {
				continue;
			}
			var external = com.agricraft.agricraft.api.compat.ExternalIrrigationSupport.irrigate(level, target, state);
			if (external == com.agricraft.agricraft.api.compat.ExternalIrrigationSupport.Result.STOP) return;
			if (external == com.agricraft.agricraft.api.compat.ExternalIrrigationSupport.Result.CONTINUE) continue;
			if (state.getBlock() instanceof FarmBlock) {
				if (state.getValue(FarmBlock.MOISTURE) < FarmBlock.MAX_MOISTURE) {
					level.setBlock(target, state.setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE), Block.UPDATE_ALL);
				}
				return;
			}
			if (state.getBlock() instanceof BonemealableBlock) {
				if (level instanceof ServerLevel serverLevel && random.nextDouble() < IrrigationConfig.sprinklerGrowthChance) {
					state.randomTick(serverLevel, target, random);
				}
				continue;
			}
			// any other solid block shields the area below from irrigation
			return;
		}
	}

	protected void spawnParticles(Level level, BlockPos pos) {
		if (!IrrigationConfig.sprinklerParticles) {
			return;
		}
		// SplashParticle (SPLASH) follows the velocity it's given only if the Y velocity is EXACTLY
		// 0.0 - otherwise its constructor discards the whole vector and applies a fixed upward pop.
		// Emit droplets from partway along each spinning arm (not the center) so they read as water
		// being flung off the rotating head rather than teleporting around the middle.
		RandomSource random = level.getRandom();
		double cx = pos.getX() + 0.5;
		double cy = pos.getY() + 5.0 / 16.0;
		double cz = pos.getZ() + 0.5;
		for (int i = 0; i < 4; i++) {
			double alpha = Math.toRadians(this.angle + i * 90.0);
			// match the direction the arms actually turn: the renderer spins them with
			// Axis.YP (JOML rotationAxis around +Y), which maps +X to (cos, -sin) in XZ,
			// so the droplets must use -sin for Z to fly the same way, not the opposite.
			double dirX = Math.cos(alpha);
			double dirZ = -Math.sin(alpha);
			for (int j = 0; j < 2; j++) {
				double radius = (5 + j * 2) / 16.0;
				double px = cx + dirX * radius;
				double pz = cz + dirZ * radius;
				double speed = 0.28 + random.nextDouble() * 0.1;
				level.addParticle(ParticleTypes.SPLASH, px, cy, pz, dirX * speed, 0.0, dirZ * speed);
			}
		}
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		this.waterBuffer = tag.getInt("water_buffer");
		this.columnCounter = tag.getInt("column_counter");
		this.cycleTimer = tag.getInt("cycle_timer");
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putInt("water_buffer", this.waterBuffer);
		tag.putInt("column_counter", this.columnCounter);
		tag.putInt("cycle_timer", this.cycleTimer);
	}

	@NotNull
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return this.saveWithoutMetadata(registries);
	}

	@Nullable
	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

}
