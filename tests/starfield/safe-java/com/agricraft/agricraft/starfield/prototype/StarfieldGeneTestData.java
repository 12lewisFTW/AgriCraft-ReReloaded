package com.agricraft.agricraft.starfield.prototype;

import com.agricraft.agricraft.compat.starfield.StarfieldPlantData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;

/** Test-only genome edits through the public SavedData serialization surface. */
final class StarfieldGeneTestData {
    static void stat(ServerLevel level, BlockPos pos, String id, int value) {
        var saved = StarfieldPlantData.get(level).save(new CompoundTag(), level.registryAccess());
        for (var valueTag : saved.getList("plants", Tag.TAG_COMPOUND)) {
            var row = (CompoundTag) valueTag;
            if (row.getLong("root") == pos.asLong() && row.getString("dimension").equals(level.dimension().location().toString())) {
                var pair = row.getCompound("genome").getCompound("genes").getCompound(id);
                pair.putInt("dom", value); pair.putInt("rec", value);
            }
        }
        level.getServer().overworld().getDataStorage().set(StarfieldPlantData.STORAGE, StarfieldPlantData.load(saved, level.registryAccess()));
    }
}
