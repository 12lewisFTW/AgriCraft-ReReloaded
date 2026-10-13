package com.agricraft.agricraft.starfield.prototype;

import com.agricraft.agricraft.client.gui.MagnifyingGlassOverlay;
import com.agricraft.agricraft.compat.starfield.StarfieldCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

/** Opt-in actual client startup check; never included in a release archive. */
@EventBusSubscriber(modid = "agricraft", value = Dist.CLIENT)
public final class MissingStarfieldClientAcceptance {
    private static int readyTicks;
    private static boolean done;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        String result = System.getProperty("agricraft.starfield.absentClientResult");
        var mc = Minecraft.getInstance();
        if (result == null || done || mc.getOverlay() != null || mc.screen == null || ++readyTicks < 20) return;
        done = true;
        try {
            if (StarfieldCompat.isAvailable() || StarfieldCompat.status() != StarfieldCompat.Status.ABSENT)
                throw new IllegalStateException("Absent native dependency is available on the client");
            var field = NeoForge.EVENT_BUS.getClass().getDeclaredField("listeners");field.setAccessible(true);
            var listeners = (Map<?, ?>) field.get(NeoForge.EVENT_BUS);
            if (listeners.keySet().stream().anyMatch(owner ->
                    (owner instanceof Class<?> type ? type : owner.getClass().getNestHost()).getName()
                            .startsWith("com.agricraft.agricraft.compat.starfield.")))
                throw new IllegalStateException("Absent native dependency registered client/gameplay listeners");
            var inspectorField = MagnifyingGlassOverlay.class.getDeclaredField("inspectors");inspectorField.setAccessible(true);
            var inspectors = (Collection<?>) inspectorField.get(null);
            if (inspectors.stream().anyMatch(inspector -> inspector.getClass().getNestHost().getName()
                    .startsWith("com.agricraft.agricraft.compat.starfield.")))
                throw new IllegalStateException("Absent native dependency registered an overlay inspector");
            if (java.util.Arrays.stream(ItemStack.class.getDeclaredMethods())
                    .anyMatch(method -> method.getName().contains("agricraft$nativeSeed")))
                throw new IllegalStateException("Absent native dependency applied the ItemStack mixin");
            write(result, "PASS real client without Starfield: no gameplay/render/cache listeners, no overlay inspector, no ItemStack mixin.\n");
            mc.stop();
        } catch (Exception failure) {
            write(result, "FAIL " + failure + "\n");
            throw new IllegalStateException("Missing-Starfield client acceptance failed", failure);
        }
    }
    private static void write(String result, String text) {
        try {var path = Path.of(result);Files.createDirectories(path.getParent());Files.writeString(path, text);}
        catch (java.io.IOException failure) {throw new IllegalStateException(failure);}
    }
}
