# Crop packs and seed conversion

AgriCraft 4.0.20 includes six compatibility crop packs in **both** release archives. Their data was compared between the NeoForge and Fabric JARs and is identical. This describes pack availability, not a promise that every source mod has a release for both loaders. You still need a compatible version of the source mod and any relevant addons.

| Pack | Crop definitions | Species mutation recipes | New soils | Fertilizers |
|---|---:|---:|---:|---:|
| [Biomes O' Plenty](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Biomes-O-Plenty) | 11 | 11 | 0 | 0 |
| [Croptopia](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Croptopia) | 58 | 0 | 0 | 0 |
| [Farmer's Delight](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Farmers-Delight) | 4 | 4 | 2 | 0 |
| [Immersive Engineering](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Immersive-Engineering) | 1 | 1 | 0 | 0 |
| [Mystical Agriculture](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Mystical-Agriculture) | 136 | 0 | 5 | 1 |
| [Pam's HarvestCraft 2 Crops](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Pams-HarvestCraft-2-Crops) | 97 | 97 | 0 | 0 |

These are packaged definitions. Optional resource crops whose required native items are missing are not all usable merely because the pack contains a definition.

## Conversion changes the farming system

A supported source seed can be converted into an AgriCraft seed through an empty analyzer: sneak-right-click with the seed. Automatic conversion during planting also depends on the seed definition, `core.override_vanilla_farming=true`, and `core.converts_seed_only_in_analyzer=false`. For a predictable setup, convert explicitly, then plant the AgriCraft seed on single sticks.

Converted plants use AgriCraft's eight-stage growth, soil requirements, gene inheritance, weed rules and [Gain harvest trials](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Genes-and-Balance). Harvesting returns source-mod produce, allowing normal food and resource recipes to use it. Cooking and processing recipes are not redesigned by these crop packs.

The exact seed item matters. Farmer's Delight rice uses `farmersdelight:rice`; Croptopia mostly uses singular `*_seed` IDs, with `vanilla_seeds` as an exception. Most BOP flowers have no convertible seed item; obtain their genetic seeds through mutation recipes.

## Breeding and mutation recipes

Single-parent cloning and two-parent breeding are different operations. A pack with no species mutation recipes can still support same-species stat breeding. An entry with recipe chance 50% means a 50% species-recipe evaluation **after** parent selection and other checks; it is not a 50% chance per world tick.

Recipes can depend on plants from another pack. If a parent species is unavailable, that recipe cannot be completed. Use the module catalogs or a recipe viewer to locate the actual parent pair.

## Pack activation and seasons

Automatic compatibility pack registration is enabled by `core.register_packs_by_default=true` when the corresponding source mod is present. The crop data and its resource models must both be available. Custom datapacks can override these defaults, so server-specific documentation takes precedence over the tables here.

Some definitions contain season tags. In 4.0.20 the [Serene Seasons](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Serene-Seasons) and [Fabric Seasons](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Fabric-Seasons) adapters report unrestricted seasons. Do not treat the authored tags as an active external-calendar restriction. Native Starfield has its own functioning seasonal model and is not one of these conversion packs.
