# Compatibility overview

This is the player index for **AgriCraft ReReloaded 4.0.20**, Minecraft **1.21.1**. Each module page describes the farming loop, balance, practical setup and support limits. Defaults can be changed by a server's datapacks and configuration.

## Crop conversion packs

All six packs are included in both AgriCraft archives. This does **not** make a NeoForge source mod work on Fabric: you need its matching loader build and required items. See [Crop Packs and Seed Conversion](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Crop-Packs-and-Seed-Conversion) for activation.

| Module | Definitions / mutation recipes | Gameplay focus | Reference |
|---|---|---|---|
| [Biomes O' Plenty](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Biomes-O-Plenty) | 11 / 11 | Flowers, dyes, clipping, mushrooms | [Catalog](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Biomes-O-Plenty-Catalog) |
| [Croptopia](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Croptopia) | 58 / 0 | Food crops, full-cycle reset, same-species breeding | [Catalog](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Croptopia-Catalog) |
| [Farmer's Delight](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Farmers-Delight) | 4 / 4 | Vegetables, rice panicles, two rich soils | [Catalog](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Farmers-Delight-Catalog) |
| [Immersive Engineering](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Immersive-Engineering) | 1 / 1 | Hemp fiber and a two-block crop | [Catalog](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Immersive-Engineering-Catalog) |
| [Mystical Agriculture](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Mystical-Agriculture) | 136 / 0 | Resource progression, no one-parent cloning, tier soil checks | [Catalog](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Mystical-Agriculture-Catalog) |
| [Pam's HarvestCraft 2 Crops](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Pams-HarvestCraft-2-Crops) | 97 / 97 | Diverse conditions and a broad mutation graph | [Catalog](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Pams-HarvestCraft-2-Crops-Catalog) |

## Native farming and automation

| Module | AgriCraft implementation | What it adds |
|---|---|---|
| [Starfield Pastoral](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Starfield-Pastoral) | NeoForge; exact 0.6.3fix1 | 50 native seed types, inherited genes, tools, irrigation and production heredity; keeps the native calendar |
| [Create](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Create) | NeoForge | Mechanical Harvester route for ordinary crop blocks and supported native Starfield crops |
| [Industrial Foregoing](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Industrial-Foregoing) | NeoForge | Plant Gatherer harvesting, with normal machine costs and work rules |
| [PneumaticCraft](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/PneumaticCraft) | NeoForge | Drone harvest handler; pickup and delivery must be programmed separately |

Converted crops and native Starfield do not use the same Gain/Growth balance. Ordinary Gain reaches four produce trials; native Starfield Gain doubles primary quantity at the maximum. Read their own guides before planning throughput.

## Recipe viewers and crop inspection

| Module | Available plugin scope | Use |
|---|---|---|
| [JEI](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/JEI) | Ordinary registry plugin; NeoForge and Fabric entrypoint | Produce, clipping, mutation and growth-requirement categories |
| [EMI](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/EMI) | Ordinary registry plugin on both loaders | The same four crop-planning categories |
| [Jade](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Jade) | Ordinary crops/soil on both loaders; native Starfield provider on NeoForge | Live conditions and planting information |
| [WTHIT](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/WTHIT) | Ordinary crops/soil on both loaders | Live crop and soil inspection; no dedicated native Starfield provider |
| [The One Probe](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/The-One-Probe) | NeoForge; ordinary crops and active native Starfield | Growth/native progress and extended gene information |

Plugin inclusion and entrypoints are supported by archive/source inspection. The documented release gameplay checks use the NeoForge client environment; they are not a claim of a full client playthrough of every listed mod/loader combination. Information modules do not grant farming buffs.

## Settings and current limitations

| Module | Status in 4.0.20 |
|---|---|
| [Resourceful Config](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Resourceful-Config) | Required on both loaders; supplies AgriCraft configuration |
| [Mod Menu](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Mod-Menu) | Optional Fabric entry into the settings screen |
| [Serene Seasons](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Serene-Seasons) | Inactive/unrestricted NeoForge adapter; no active external-calendar restriction |
| [Fabric Seasons](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Fabric-Seasons) | Fabric adapter reports unrestricted seasons; no active external-calendar restriction |
| [Botania](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Botania) | No dedicated crop pack shipped in the release |
| [REI](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/REI) | No dedicated implemented crop-information plugin |

## Core player guides

- [Getting Started](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Getting-Started)
- [Genes and Balance](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Genes-and-Balance): exact ordinary yield, growth, inheritance and selection rules
- [Soil Irrigation and Greenhouses](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Soil-Irrigation-and-Greenhouses): beds, water capacity, actual consumption and enclosures
- [Configuration and Troubleshooting](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Configuration-and-Troubleshooting): option names, defaults and symptom checks
- [Starfield Crop Catalog](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Starfield-Crop-Catalog): native phases, seasons, regrowth and harvest forms
- [About These Guides](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/About-These-Guides): sources, version scope and verification
