package com.agricraft.agricraft.common.block.entity;

import com.agricraft.agricraft.common.block.SeedAnalyzerBlock;
import com.agricraft.agricraft.common.inventory.container.SeedAnalyzerMenu;
import com.agricraft.agricraft.common.item.AgriSeedItem;
import com.agricraft.agricraft.common.item.JournalItem;
import com.agricraft.agricraft.common.registry.ModBlockEntityTypes;
import com.agricraft.agricraft.common.util.ExtraDataMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SeedAnalyzerBlockEntity extends BlockEntity implements WorldlyContainer, ExtraDataMenuProvider {

	public static final int SEED_SLOT = 0;
	public static final int JOURNAL_SLOT = 1;
	private SimpleContainer inventory;


	public SeedAnalyzerBlockEntity(BlockPos blockPos, BlockState blockState) {
		super(ModBlockEntityTypes.SEED_ANALYZER.get(), blockPos, blockState);
		this.inventory = new SimpleContainer(2);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		if (tag.contains("journal")) {
			ItemStack journal = ItemStack.parseOptional(registries, tag.getCompound("journal"));
			this.inventory.setItem(JOURNAL_SLOT, journal);
		}
		if (tag.contains("seed")) {
			ItemStack seed = ItemStack.parseOptional(registries, tag.getCompound("seed"));
			this.inventory.setItem(SEED_SLOT, seed);
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		if (hasJournal()) {
			tag.put("journal", getJournal().save(registries));
		}
		if (hasSeed()) {
			tag.put("seed", getSeed().save(registries));
		}
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

	private ItemStack insertItem(int slot, ItemStack stack) {
		if (level == null || level.isClientSide || !canPlaceItem(slot, stack)) return stack.copy();
		ItemStack current = this.inventory.getItem(slot);
		if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, stack)) return stack.copy();
		int transfer = Math.min(stack.getCount(), Math.min(stack.getMaxStackSize(), inventory.getMaxStackSize()) - current.getCount());
		if (transfer <= 0) return stack.copy();
		if (current.isEmpty()) inventory.setItem(slot, stack.copyWithCount(transfer));
		else current.grow(transfer);
		ItemStack result = stack.copyWithCount(stack.getCount() - transfer);
		this.setChanged();
		this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
		return result;
	}

	public boolean hasSeed() {
		return !this.inventory.getItem(SEED_SLOT).isEmpty();
	}

	public ItemStack getSeed() {
		return this.inventory.getItem(SEED_SLOT);
	}

	public ItemStack insertSeed(ItemStack seed) {
		ItemStack prepared = com.agricraft.agricraft.api.compat.ExternalSeedSupport.accepts(seed, level)
				? com.agricraft.agricraft.api.compat.ExternalSeedSupport.prepare(seed, level) : seed;
		if (prepared.isEmpty()) return seed.copy();
		ItemStack stack = insertItem(SEED_SLOT, prepared);
		if (hasJournal() && stack.getCount() == 0 && seed.getItem() instanceof AgriSeedItem) {
			JournalItem.researchPlant(this.getJournal(), ResourceLocation.parse(AgriSeedItem.getSpecies(seed)));
		}
		return stack;
	}

	public ItemStack extractSeed() {
		ItemStack stack = this.inventory.getItem(SEED_SLOT).copy();
		this.inventory.setItem(SEED_SLOT, ItemStack.EMPTY);
		this.setChanged();
		this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
		return stack;
	}

	public boolean hasJournal() {
		return !this.inventory.getItem(JOURNAL_SLOT).isEmpty();
	}

	public ItemStack getJournal() {
		return this.inventory.getItem(JOURNAL_SLOT);
	}

	public ItemStack insertJournal(ItemStack seed) {
		ItemStack stack = insertItem(JOURNAL_SLOT, seed);
		BlockState state = this.getBlockState().setValue(SeedAnalyzerBlock.JOURNAL, true);
		level.setBlock(this.worldPosition, state, Block.UPDATE_ALL);
		return stack;
	}

	public ItemStack extractJournal() {
		ItemStack stack = this.inventory.getItem(JOURNAL_SLOT).copy();
		this.inventory.setItem(JOURNAL_SLOT, ItemStack.EMPTY);
		BlockState state = this.getBlockState().setValue(SeedAnalyzerBlock.JOURNAL, false);
		level.setBlock(this.worldPosition, state, Block.UPDATE_ALL);
		this.setChanged();
		this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
		return stack;
	}

	public SimpleContainer getInventory() {
		return inventory;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return new int[]{JOURNAL_SLOT};
	}

	@Override
	public boolean canPlaceItemThroughFace(int index, ItemStack itemStack, @Nullable Direction direction) {
		return this.canPlaceItem(index, itemStack);
	}

	@Override
	public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
		return true;
	}

	@Override
	public int getContainerSize() {
		return inventory.getContainerSize();
	}

	@Override
	public boolean isEmpty() {
		return inventory.isEmpty();
	}

	@Override
	public ItemStack getItem(int slot) {
		return inventory.getItem(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		return inventory.removeItem(slot, amount);
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return inventory.removeItemNoUpdate(slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		this.inventory.setItem(slot, stack);
	}

	@Override
	public boolean stillValid(Player player) {
		return level != null && player.level() == level && !isRemoved()
				&& level.getBlockEntity(worldPosition) == this
				&& com.agricraft.agricraft.api.compat.ExternalSeedSupport.mayUseAnalyzer(player, worldPosition)
				&& player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) < 64;
	}

	@Override public boolean canPlaceItem(int slot, ItemStack stack) {
		if (slot == JOURNAL_SLOT) return stack.getItem() instanceof JournalItem;
		return slot == SEED_SLOT && (stack.getItem() instanceof AgriSeedItem
				|| com.agricraft.agricraft.api.compat.ExternalSeedSupport.accepts(stack, level)
				|| com.agricraft.agricraft.api.AgriApi.getPlantRegistry().map(registry -> registry.stream().anyMatch(plant -> plant.isSeedItem(stack))).orElse(false));
	}

	@Override
	public void clearContent() {
		this.inventory.clearContent();
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("screen.agricraft.seed_analyzer");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
		return new SeedAnalyzerMenu(i, inventory, player, this.worldPosition);
	}

	@Override
	public void writeExtraData(ServerPlayer player, FriendlyByteBuf buffer) {
		buffer.writeBlockPos(this.getBlockPos());
	}

}
