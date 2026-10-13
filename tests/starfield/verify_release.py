"""Check that development probes and the optional native dependency are absent from release JARs."""

import sys
import struct
from io import BytesIO
from pathlib import Path
from zipfile import ZipFile

FORBIDDEN = (
    "com/stardew/",
    "assets/stardewcraft/",
    "data/stardewcraft/",
    "com/agricraft/agricraft/starfield/prototype/",
    "data/agricraft/structure/starfield_probe.nbt",
    "data/minecraft/worldgen/world_preset/flat.json",
    "data/agricraft/starfield_probe_catalog.json",
    "data/agricraft/agricraft/mutations/starfield_probe.json",
    "agricraft-save-diagnostics.mixins.json",
)

# Loom writes optional @Pseudo targets and @Inject method selectors as slash-separated
# annotation strings. These are not JVM type/member references and do not link Starfield.
MIXIN_TEXT = {
    "com/agricraft/agricraft/compat/starfield/mixin/NativeProductionMixin.class": {
        b"com/stardew/craft/blockentity/TimedProductionBlockEntity",
    },
    "com/agricraft/agricraft/compat/starfield/mixin/NativeGiantLifecycleMixin.class": {
        b"com/stardew/craft/api/v1/internal/giant/GiantCropGrowth",
        b"process(Lnet/minecraft/server/level/ServerLevel;Ljava/util/Collection;Ljava/util/Set;IZLjava/util/Collection;)Lcom/stardew/craft/api/v1/internal/giant/GiantCropGrowth$Statistics;",
        b"Lcom/stardew/craft/api/v1/agriculture/StardewGiantCrops$Handler;plan(Lcom/stardew/craft/api/v1/agriculture/StardewGiantCrops$Context;Ljava/util/List;)Ljava/util/List;",
    },
    "com/agricraft/agricraft/compat/starfield/mixin/NativeWildFormMixin.class": {
        b"com/stardew/craft/block/crop/WildSeedCropBlock",
    },
    "com/agricraft/agricraft/compat/starfield/mixin/NativeForageMixin.class": {
        b"com/stardew/craft/block/nature/ForageBlock",
        b"Lcom/stardew/craft/player/PlayerStardewDataAPI;addExperience(Lnet/minecraft/server/level/ServerPlayer;Lcom/stardew/craft/player/SkillType;I)Z",
    },
    "com/agricraft/agricraft/compat/starfield/mixin/NativeCropGeneMixin.class": {
        b"com/stardew/craft/block/crop/StardewCropBlock",
        b"Lcom/stardew/craft/block/crop/StardewCropBlock;applySpeedGroToPhaseDays([IF)[I",
        b"Lcom/stardew/craft/block/crop/StardewCropBlock;getRegrowDays()I",
        b"growCropOneDayInternal(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;ZLcom/stardew/craft/manager/CropGrowthManager$CropGrowthState;)V",
        b"syncVisualStage(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lcom/stardew/craft/manager/CropGrowthManager$CropGrowthState;)V",
        b"Lcom/stardew/craft/manager/CropGrowthManager$CropGrowthState;advanceOnSoil(Z)Z",
    },
    "com/agricraft/agricraft/compat/starfield/mixin/NativeGiantSeasonMixin.class": {
        b"com/stardew/craft/api/v1/internal/giant/GiantCropGrowth",
        b"process(Lnet/minecraft/server/level/ServerLevel;Ljava/util/Collection;Ljava/util/Set;IZLjava/util/Collection;)Lcom/stardew/craft/api/v1/internal/giant/GiantCropGrowth$Statistics;",
    },
    "com/agricraft/agricraft/compat/starfield/mixin/NativeCropHarvestMixin.class": {
        b"com/stardew/craft/block/crop/StardewCropBlock",
        b"Lcom/stardew/craft/player/PlayerStardewDataAPI;addExperience(Lnet/minecraft/server/level/ServerPlayer;Lcom/stardew/craft/player/SkillType;I)Z",
    },
    "com/agricraft/agricraft/compat/starfield/mixin/NativeCropLifecycleMixin.class": {
        b"com/stardew/craft/block/crop/StardewCropBlock",
    },
    "com/agricraft/agricraft/compat/starfield/mixin/NativeGrowthRestoreMixin.class": {
        b"com/stardew/craft/manager/CropGrowthManager",
        b"restoreGrowthState(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lcom/stardew/craft/manager/CropGrowthManager$CropGrowthState;)V",
    },
}


def optional_mixin_strings_only(entry, data):
    allowed = MIXIN_TEXT.get(entry)
    if allowed is None:
        return False
    strings, classes = {}, []
    count = struct.unpack_from(">H", data, 8)[0]
    cursor, index = 10, 1
    sizes = {3: 4, 4: 4, 5: 8, 6: 8, 8: 2, 9: 4, 10: 4, 11: 4,
             12: 4, 15: 3, 16: 2, 17: 4, 18: 4, 19: 2, 20: 2}
    while index < count:
        kind = data[cursor]
        cursor += 1
        if kind == 1:
            length = struct.unpack_from(">H", data, cursor)[0]
            cursor += 2
            strings[index] = data[cursor:cursor + length]
            cursor += length
        elif kind == 7:
            classes.append(struct.unpack_from(">H", data, cursor)[0])
            cursor += 2
        else:
            cursor += sizes[kind]
            if kind in (5, 6):
                index += 1
        index += 1
    native_strings = {value for value in strings.values() if b"com/stardew/" in value}
    return native_strings <= allowed and not any(b"com/stardew/" in strings[name] for name in classes)


def verify(paths):
    if not paths:
        raise ValueError("Pass one or more release JAR paths")
    for path in paths:
        with ZipFile(path) as archive:
            entries = archive.namelist()
            neoforge = "META-INF/neoforge.mods.toml" in entries
            if neoforge and b"agricraft-save-diagnostics" in archive.read("META-INF/neoforge.mods.toml"):
                raise ValueError(f"Save diagnostics referenced by release manifest: {path}")
            leaked = [entry for entry in entries if entry.startswith(FORBIDDEN)]
            if leaked:
                raise ValueError(f"Development content leaked into {path}: {leaked}")
            native_bridge = "com/agricraft/agricraft/compat/starfield/NativeStarfieldCatalog.class"
            native_implementations = {native_bridge,
                "com/agricraft/agricraft/compat/starfield/NativeStarfieldLifecycle.class",
                "com/agricraft/agricraft/compat/starfield/NativeStarfieldSticks.class",
                "com/agricraft/agricraft/compat/starfield/NativeStarfieldTransfer.class",
                "com/agricraft/agricraft/compat/starfield/NativeStarfieldHarvest.class",
                "com/agricraft/agricraft/compat/starfield/NativeStarfieldAutomation.class",
                "com/agricraft/agricraft/compat/starfield/NativeStarfieldProduction.class",
                "com/agricraft/agricraft/compat/starfield/NativeStarfieldSeeds.class",
                "com/agricraft/agricraft/compat/starfield/NativeStarfieldInspection.class",
                "com/agricraft/agricraft/compat/starfield/NativeStarfieldIrrigation.class",
                "com/agricraft/agricraft/compat/starfield/NativeStarfieldBreeding.class",
                "com/agricraft/agricraft/compat/starfield/NativeStarfieldResilience.class",
                "com/agricraft/agricraft/compat/starfield/NativeStarfieldSpecials.class",
                "com/agricraft/agricraft/compat/starfield/NativeStarfieldGiants.class",
                "com/agricraft/agricraft/compat/starfield/NativeStarfieldGiants$Snapshot.class",
                "com/agricraft/agricraft/compat/starfield/NativeStarfieldTransfer$Snapshot.class"}
            facade = "com/agricraft/agricraft/compat/starfield/StarfieldCompat.class"
            if neoforge and facade in entries and native_bridge not in entries:
                raise ValueError(f"Optional implementation is missing from NeoForge: {path}")
            for entry in entries:
                if not neoforge and entry.startswith("com/agricraft/agricraft/compat/starfield/"):
                    raise ValueError(f"NeoForge compatibility leaked into another platform: {path}: {entry}")
                allowed_native_bridge = neoforge and entry in native_implementations
                if entry.endswith(".class"):
                    bytecode = archive.read(entry)
                    if b"com/stardew/" in bytecode and not allowed_native_bridge and not (
                            neoforge and optional_mixin_strings_only(entry, bytecode)):
                        raise ValueError(f"Release class directly references Starfield: {path}: {entry}")
                if entry.endswith(".jar"):
                    with ZipFile(BytesIO(archive.read(entry))) as nested:
                        if any(name.startswith(FORBIDDEN) for name in nested.namelist()):
                            raise ValueError(f"Native dependency or development code bundled in {path}: {entry}")
                        if "fabric.mod.json" in nested.namelist() and b"agricraft_starfield_loading_test" in nested.read("fabric.mod.json"):
                            raise ValueError(f"Fabric test descriptor bundled in {path}: {entry}")
        print(f"Prototype isolation verified: {Path(path).name}")


if __name__ == "__main__":
    verify(sys.argv[1:])
