package com.agricraft.agricraft.compat.starfield.mixin;

import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** Early presence/version gate; config/runtime guards remain in the safe facade. */
public final class StarfieldMixinPlugin implements IMixinConfigPlugin {
    @Override public boolean shouldApplyMixin(String target, String mixin) {
        return LoadingModList.get().getMods().stream().anyMatch(mod -> mod.getModId().equals("stardewcraft")
                && mod.getVersion().toString().equals("0.6.3fix1"));
    }
    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
