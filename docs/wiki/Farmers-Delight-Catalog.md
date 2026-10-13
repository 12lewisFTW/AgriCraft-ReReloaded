# Farmer's Delight crop catalog

Pack reference for AgriCraft ReReloaded **4.0.20**, Minecraft **1.21.1**. Read [Farmer's Delight gameplay guide](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Farmers-Delight) first. These tables list packaged data; only species with their required source items installed are usable. Custom datapacks may change the values.

Ranges and chances are **per produce trial**. [Gain](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Genes-and-Balance) determines the number of ordinary harvest trials. Environmental values are the authored targets **before Strength tolerance**. Season tags are not listed as active restrictions because the external season adapters are unrestricted in this release. Stage numbers are zero-based; all listed plants have eight stages.

## Crops, seed inputs and produce

| Species | Native seed input | Produce per trial | Stage after harvest |
|---|---|---|---:|
| Cabbage | `farmersdelight:cabbage_seeds` | `farmersdelight:cabbage`: 1–3 at 95% | 4 |
| Onion | `farmersdelight:onion` | `farmersdelight:onion`: 1–3 at 95% | 4 |
| Rice | `farmersdelight:rice` | `farmersdelight:rice_panicle`: 1–3 at 95% | 4 |
| Tomato | `farmersdelight:tomato_seeds` | `farmersdelight:tomato`: 1–3 at 95% | 4 |

## Growing conditions

| Species | Humidity | Acidity | Nutrients | Light | Mature height (blocks) |
|---|---|---|---|---|---:|
| Cabbage | Wet | Slightly Acidic | High or higher | 10–16 | 1 |
| Onion | Wet | Slightly Acidic | High or higher | 10–16 | 1 |
| Rice | Watery or higher | Slightly Acidic | Medium or higher | 5–16 | 2 |
| Tomato | Wet | Slightly Acidic | High or higher | 10–16 | 1 |

## Species mutation recipes

A raw recipe chance is evaluated after the breeding engine selects parents. It is not a probability per tick. Both parent species must be loaded and available. Parent IDs distinguish similarly named crops from different mods.

| Child species | First parent | Second parent | Raw chance |
|---|---|---|---:|
| `farmersdelight:cabbage` | `farmersdelight:onion` | `minecraft:seagrass` | 50% |
| `farmersdelight:onion` | `minecraft:allium` | `minecraft:potato` | 50% |
| `farmersdelight:rice` | `minecraft:seagrass` | `minecraft:wheat` | 50% |
| `farmersdelight:tomato` | `minecraft:wheat` | `minecraft:beetroot` | 50% |

Data basis: the matching pack inside both [4.0.20 release JARs](https://github.com/Vivoxti/AgriCraft-ReReloaded/releases/tag/4.0.20). The archive comparison covered plants, mutations, soils and fertilizers.
