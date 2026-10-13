# Immersive Engineering crop catalog

Pack reference for AgriCraft ReReloaded **4.0.20**, Minecraft **1.21.1**. Read [Immersive Engineering gameplay guide](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Immersive-Engineering) first. These tables list packaged data; only species with their required source items installed are usable. Custom datapacks may change the values.

Ranges and chances are **per produce trial**. [Gain](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Genes-and-Balance) determines the number of ordinary harvest trials. Environmental values are the authored targets **before Strength tolerance**. Season tags are not listed as active restrictions because the external season adapters are unrestricted in this release. Stage numbers are zero-based; all listed plants have eight stages.

## Crops, seed inputs and produce

| Species | Native seed input | Produce per trial | Stage after harvest |
|---|---|---|---:|
| Hemp | `immersiveengineering:seed` | `immersiveengineering:hemp_fiber`: 1–2 at 85% | 4 |

## Growing conditions

| Species | Humidity | Acidity | Nutrients | Light | Mature height (blocks) |
|---|---|---|---|---|---:|
| Hemp | Wet | Slightly Acidic | High or higher | 10–16 | 2 |

## Species mutation recipes

A raw recipe chance is evaluated after the breeding engine selects parents. It is not a probability per tick. Both parent species must be loaded and available. Parent IDs distinguish similarly named crops from different mods.

| Child species | First parent | Second parent | Raw chance |
|---|---|---|---:|
| `immersiveengineering:hemp` | `minecraft:pumpkin` | `minecraft:wheat` | 50% |

Data basis: the matching pack inside both [4.0.20 release JARs](https://github.com/Vivoxti/AgriCraft-ReReloaded/releases/tag/4.0.20). The archive comparison covered plants, mutations, soils and fertilizers.
