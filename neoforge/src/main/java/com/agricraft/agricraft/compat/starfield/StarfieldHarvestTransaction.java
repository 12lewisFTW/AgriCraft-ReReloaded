package com.agricraft.agricraft.compat.starfield;

import com.agricraft.agricraft.AgriCraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import java.util.ArrayList;
import java.util.function.BooleanSupplier;

/** Server-thread scope; keeps native drops unpublished until native crop mutation completes. */
public final class StarfieldHarvestTransaction {
    private static final ThreadLocal<StarfieldHarvestTransaction> CURRENT = new ThreadLocal<>();
    private final ServerLevel level;
    private final ArrayList<Drop> drops = new ArrayList<>();
    private final ArrayList<BooleanSupplier> experience = new ArrayList<>();
    private final double gain;
    private final java.util.function.Consumer<net.minecraft.world.item.ItemStack> decorate;
    private boolean primary;
    private boolean publishing;
    private record Drop(ItemEntity entity, BooleanSupplier publish, boolean primary) {}

    public static final class Aborted extends RuntimeException {
        public Aborted(Throwable cause) { super("Native harvest rejected", cause); }
    }
    private StarfieldHarvestTransaction(ServerLevel level, double gain,
            java.util.function.Consumer<net.minecraft.world.item.ItemStack> decorate) {
        this.level = level; this.gain = gain; this.decorate = decorate;
    }

    public static void run(ServerLevel level, Runnable original, Runnable restoreCrop, Runnable restoreExperience) {
        run(level, 0, original, restoreCrop, restoreExperience);
    }

    public static void run(ServerLevel level, double gain, Runnable original, Runnable restoreCrop, Runnable restoreExperience) {
        run(level, gain, stack -> {}, original, restoreCrop, restoreExperience);
    }

    public static void run(ServerLevel level, double gain, java.util.function.Consumer<net.minecraft.world.item.ItemStack> decorate,
            Runnable original, Runnable restoreCrop, Runnable restoreExperience) {
        // A synchronous event listener must not start another harvest inside publication.
        if (CURRENT.get() != null) throw new Aborted(new IllegalStateException("Reentrant harvest"));
        var transaction = new StarfieldHarvestTransaction(level, gain, decorate);
        CURRENT.set(transaction);
        boolean experienceStarted = false;
        try {
            original.run();
            transaction.applyGain();
            transaction.publishing = true;
            for (var drop : transaction.drops) {
                if (!drop.publish.getAsBoolean() || drop.entity.isRemoved())
                    throw new IllegalStateException("Harvest item creation rejected");
            }
            experienceStarted = true;
            for (var award : transaction.experience) award.getAsBoolean();
        } catch (RuntimeException failure) {
            transaction.publishing = true;
            for (var drop : transaction.drops) drop.entity.discard();
            // Restore failures are fatal: never report a cleanly aborted operation in that case.
            restoreCrop.run();
            if (experienceStarted) restoreExperience.run();
            AgriCraft.LOGGER.warn("Starfield harvest rolled back: {}", failure.toString());
            throw new Aborted(failure);
        } finally { CURRENT.remove(); }
    }

    public static boolean addEntity(ServerLevel level, Entity entity, BooleanSupplier original) {
        var transaction = CURRENT.get();
        if (transaction == null || transaction.publishing || transaction.level != level || !(entity instanceof ItemEntity item))
            return original.getAsBoolean();
        transaction.drops.add(new Drop(item, original, transaction.primary));
        return true;
    }

    /** Only the base native primary-output method participates; subclass extras stay native. */
    public static void primary(Runnable original) {
        var transaction = CURRENT.get();
        if (transaction == null || transaction.publishing) { original.run(); return; }
        boolean previous = transaction.primary;
        transaction.primary = true;
        try { original.run(); } finally { transaction.primary = previous; }
    }

    private void applyGain() {
        var additional = new ArrayList<Drop>();
        for (var drop : drops) {
            if (!drop.primary) continue;
            var stack = drop.entity.getItem();
            decorate.accept(stack);
            int count = com.agricraft.agricraft.api.compat.NativeCropGeneRules.yield(stack.getCount(), gain);
            int limit = stack.getMaxStackSize();
            drop.entity.setItem(stack.copyWithCount(Math.min(count, limit)));
            count -= Math.min(count, limit);
            while (count > 0) {
                var motion = drop.entity.getDeltaMovement();
                var copy = new ItemEntity(level, drop.entity.getX(), drop.entity.getY(), drop.entity.getZ(),
                        stack.copyWithCount(Math.min(count, limit)), motion.x, motion.y, motion.z);
                // Keep native pickup delay, movement, age and ownership without duplicating UUID.
                var saved = new net.minecraft.nbt.CompoundTag(); drop.entity.saveWithoutId(saved);
                saved.remove("UUID"); copy.load(saved); copy.setItem(stack.copyWithCount(Math.min(count, limit)));
                additional.add(new Drop(copy, () -> level.addFreshEntity(copy), false));
                count -= Math.min(count, limit);
            }
        }
        drops.addAll(additional);
    }

    public static boolean awardExperience(BooleanSupplier original) {
        var transaction = CURRENT.get();
        if (transaction == null || transaction.publishing) return original.getAsBoolean();
        transaction.experience.add(original);
        return false; // Native caller ignores the boolean (which means level-up, not success).
    }
}
