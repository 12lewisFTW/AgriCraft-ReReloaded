# Configuration and troubleshooting

AgriCraft stores its settings in **`config/agricraft.jsonc`**, using [Resourceful Config](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Resourceful-Config). Names below are `category.entry` paths into that file, not command names. Defaults refer to **4.0.20**; a server or modpack may intentionally change them. Restart after changes to integration activation, stat bounds or automatic pack registration.

## Crop activation and conversion

| Setting | Default | Effect |
|---|---|---|
| `core.register_packs_by_default` | `true` | Enables automatic compatibility packs when their source mod is present |
| `core.override_vanilla_farming` | `true` | Allows the native-seed planting interception/conversion path |
| `core.converts_seed_only_in_analyzer` | `false` | When override is active, limits that conversion path to the analyzer if set true |
| `core.plant_off_crop_sticks` | `true` | Allows AgriCraft seeds to be planted outside existing sticks |
| `core.only_mature_seed_drops` | `false` | When true, only mature ordinary crops drop seeds |

Analyzer conversion remains the clearest way to obtain a known AgriCraft seed before planting. A supported native Starfield seed follows the separate native module and retains its item identity; it is not an ordinary conversion-pack seed.

## Breeding and speed

| Setting | Default | Balance role |
|---|---|---|
| `core.only_fertile_crops_spread` | `false` | If true, parent candidates must also pass fertility conditions |
| `core.allow_fertilizer_mutation` | `true` | Allows an applicable fertilizer to trigger breeding on cross crops |
| `core.clone_mutations` | `false` | Clones preserve both alleles unless enabled |
| `core.mutation_chance_multiplier` | `1.2` | Multiplies the parent-selection threshold based on Fertility |
| `core.growth_multiplier` | `1.0` | Scales the Growth-stat bonus in ordinary crop growth rolls |

The growth formula is `soil × (base + Growth × bonus × multiplier)`. Setting the multiplier to zero removes that bonus; it does not remove the base growth chance. The mutation multiplier is not a blanket multiplication of every species recipe chance. See [Genes and Balance](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Genes-and-Balance) for exact rolls.

Each of `gain`, `growth`, `strength`, `resistance`, `fertility` and `mutativity` has `stats.<name>_min=1`, `stats.<name>_max=10` and `stats.<name>_hidden=false`. Hiding affects displayed information, not whether the gene exists. Equal bounds freeze breeding at that value. Native Starfield normalizes bonuses against the bounds; ordinary formulas use the expressed value directly where specified.

## Weeds and handling

| Setting | Default |
|---|---|
| `core.disable_weeds` | `false` |
| `core.mature_weeds_kill_plants` | `true` |
| `core.weeds_spreading` | `true` |
| `core.weeds_destroy_crop_sticks` | `false` |
| `core.disable_fertilizer_weeds` | `true` |
| `core.raking_drops_items` | `true` |
| `core.crop_sticks_prevent_trampling` | `true` |
| `core.crop_sticks_collide` | `true` |

The actual raking option is **`core.raking_drops_items`**. Crop-stick trampling protection does not promise that the underlying farmland itself cannot turn to dirt. These ordinary weed settings do not create weeds on native Starfield crops.

Ordinary seed bags default to 64 seeds and an enchantment cost of 10 levels. The file key is spelled **`core.seed_bag_capactity`** in this release, with that existing spelling. Do not silently substitute `capacity` when editing the file. Native Starfield seeds are not supported as converted seed-bag contents.

## Native compatibility switches

| Setting | Default | Relevant scope |
|---|---|---|
| `compat.starfield` | `true` | Verified Starfield 0.6.3fix1 on NeoForge |
| `compat.mysticalagriculture` | `true` | Mystical Agriculture activation and NeoForge native farmland checks |
| `compat.industrialforegoing` | `true` | NeoForge Plant Gatherer adapter |
| `compat.pneumaticcraft` | `true` | NeoForge harvesting-drone adapter |

There is no active `compat.create`, `compat.botania` or REI integration switch. A setting's existence alone does not add a loader-specific implementation to Fabric. The separate [irrigation and greenhouse guide](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Soil-Irrigation-and-Greenhouses) documents capacities, actual sprinkler water use and enclosure defaults.

## Diagnose by symptom

| Symptom | Check first |
|---|---|
| Source seed does not convert | Exact supported seed item, loaded source mod, enabled crop data, analyzer input; BOP flowers may have no native seed input |
| Compatible crop is missing from a viewer | Loaded plant registry, required native items/addons and viewer plugin; native Starfield is outside ordinary custom categories |
| Ordinary plant does not grow | Soil, light, weeds and special block/tier requirements; Growth cannot bypass them |
| Ordinary crops regress under Create | Harvest passes are reaching immature crops; schedule passes for maturity |
| Native Starfield has no genes | NeoForge, exact verified Starfield release, `compat.starfield`, restart; inspect with the actual native tool/provider |
| Native plant has water but no extra immediate stage | Irrigation waters; it does not advance Starfield's daily calendar |
| Drone harvests but chest is empty | Ground drops, pickup step, delivery/export step, filters and cargo capacity |
| Gatherer ignores a crop | Maturity, range, power, storage, compatibility switch and native access gates |
| Seasonal tags appear ineffective | Serene Seasons/Fabric Seasons adapters are unrestricted in this release |

Back up the world before changing installed mods or native integration versions. If reporting a problem, include AgriCraft/source-mod versions, loader, relevant settings, the exact crop/item, the action taken and what happened. For native plants, include the season, soil/location and whether it was a tall crop, forage or giant. Logs help distinguish missing data from an actual harvest failure.

## Basis of this guide

Defaults refer to AgriCraft ReReloaded **4.0.20**, Minecraft **1.21.1**. Modpacks can override data and settings. [Core defaults](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/6d6935f09e749923566ff0de3d0c76e0da5b3c4c/common/src/main/java/com/agricraft/agricraft/api/config/CoreConfig.java) · [Compatibility switches](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/6d6935f09e749923566ff0de3d0c76e0da5b3c4c/common/src/main/java/com/agricraft/agricraft/api/config/CompatConfig.java) · [Stat options](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/6d6935f09e749923566ff0de3d0c76e0da5b3c4c/common/src/main/java/com/agricraft/agricraft/api/config/StatsConfig.java).
