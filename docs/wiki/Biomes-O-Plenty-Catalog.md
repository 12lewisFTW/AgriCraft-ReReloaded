# Biomes O' Plenty crop catalog

Pack reference for AgriCraft ReReloaded **4.0.20**, Minecraft **1.21.1**. Read [Biomes O' Plenty gameplay guide](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Biomes-O-Plenty) first. These tables list packaged data; only species with their required source items installed are usable. Custom datapacks may change the values.

Ranges and chances are **per produce trial**. [Gain](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Genes-and-Balance) determines the number of ordinary harvest trials. Environmental values are the authored targets **before Strength tolerance**. Season tags are not listed as active restrictions because the external season adapters are unrestricted in this release. Stage numbers are zero-based; all listed plants have eight stages.

## Crops, seed inputs and produce

| Species | Native seed input | Produce per trial | Stage after harvest |
|---|---|---|---:|
| Burning Blossom | No convertible seed; use mutation | `minecraft:orange_dye`: 1–1 at 75% | 4 |
| Glowflower | No convertible seed; use mutation | `minecraft:cyan_dye`: 1–1 at 75% | 4 |
| Glowshroom | `biomesoplenty:glowshroom` | `biomesoplenty:glowshroom`: 2–5 at 100% | 4 |
| Lavender | No convertible seed; use mutation | `minecraft:purple_dye`: 1–1 at 75% | 4 |
| Orange Cosmos | No convertible seed; use mutation | `minecraft:orange_dye`: 1–1 at 75% | 4 |
| Pink Daffodil | No convertible seed; use mutation | `minecraft:pink_dye`: 1–1 at 75% | 4 |
| Pink Hibiscus | No convertible seed; use mutation | `minecraft:pink_dye`: 1–1 at 75% | 4 |
| Rose | No convertible seed; use mutation | `minecraft:red_dye`: 1–1 at 75% | 4 |
| Toadstool | `biomesoplenty:toadstool` | `biomesoplenty:toadstool`: 2–5 at 100% | 4 |
| Violet | No convertible seed; use mutation | `minecraft:purple_dye`: 1–1 at 75% | 4 |
| Wilted Lily | No convertible seed; use mutation | `minecraft:gray_dye`: 1–1 at 75% | 4 |

## Growing conditions

| Species | Humidity | Acidity | Nutrients | Light | Mature height (blocks) |
|---|---|---|---|---|---:|
| Burning Blossom | Damp | Slightly Acidic | Very High or higher | 10–16 | 0.75 |
| Glowflower | Damp | Slightly Acidic | Very High or higher | 10–16 | 0.75 |
| Glowshroom | Damp | Neutral | Medium or lower | 0–10 | 1 |
| Lavender | Damp | Slightly Acidic | Very High or higher | 10–16 | 0.75 |
| Orange Cosmos | Damp | Slightly Acidic | Very High or higher | 10–16 | 0.75 |
| Pink Daffodil | Damp | Slightly Acidic | Very High or higher | 10–16 | 0.75 |
| Pink Hibiscus | Damp | Slightly Acidic | Very High or higher | 10–16 | 0.75 |
| Rose | Damp | Slightly Acidic | Very High or higher | 10–16 | 0.75 |
| Toadstool | Damp | Neutral | Medium or lower | 0–10 | 1 |
| Violet | Damp | Slightly Acidic | Very High or higher | 10–16 | 0.75 |
| Wilted Lily | Damp | Slightly Acidic | Very High or higher | 10–16 | 0.75 |

## Clipping outputs

One clipping operation uses the separate clipping list once; ordinary Gain trials do not multiply it. A zero minimum means a successful chance roll can still give no item.

| Species | Clipping output |
|---|---|
| Burning Blossom | `biomesoplenty:burning_blossom`: 0–1 at 50% |
| Glowflower | `biomesoplenty:glowflower`: 0–1 at 50% |
| Lavender | `biomesoplenty:lavender`: 0–1 at 50% |
| Orange Cosmos | `biomesoplenty:orange_cosmos`: 0–1 at 50% |
| Pink Daffodil | `biomesoplenty:pink_daffodil`: 0–1 at 50% |
| Pink Hibiscus | `biomesoplenty:pink_hibiscus`: 0–1 at 50% |
| Rose | `biomesoplenty:rose`: 0–1 at 50% |
| Violet | `biomesoplenty:violet`: 0–1 at 50% |
| Wilted Lily | `biomesoplenty:wilted_lily`: 0–1 at 50% |

## Species mutation recipes

A raw recipe chance is evaluated after the breeding engine selects parents. It is not a probability per tick. Both parent species must be loaded and available. Parent IDs distinguish similarly named crops from different mods.

| Child species | First parent | Second parent | Raw chance |
|---|---|---|---:|
| `biomesoplenty:burning_blossom` | `minecraft:orange_tulip` | `minecraft:red_tulip` | 50% |
| `biomesoplenty:glowflower` | `minecraft:lily_of_the_valley` | `minecraft:blue_orchid` | 50% |
| `biomesoplenty:glowshroom` | `minecraft:brown_mushroom` | `minecraft:blue_orchid` | 50% |
| `biomesoplenty:lavender` | `minecraft:azure_bluet` | `biomesoplenty:violet` | 50% |
| `biomesoplenty:orange_cosmos` | `minecraft:oxeye_daisy` | `minecraft:carrot` | 50% |
| `biomesoplenty:pink_daffodil` | `minecraft:pink_tulip` | `minecraft:azure_bluet` | 50% |
| `biomesoplenty:pink_hibiscus` | `minecraft:pink_tulip` | `minecraft:seagrass` | 50% |
| `biomesoplenty:rose` | `minecraft:poppy` | `minecraft:sweet_berries` | 50% |
| `biomesoplenty:toadstool` | `minecraft:brown_mushroom` | `minecraft:carrot` | 50% |
| `biomesoplenty:violet` | `minecraft:poppy` | `minecraft:cornflower` | 50% |
| `biomesoplenty:wilted_lily` | `minecraft:lily_of_the_valley` | `minecraft:brown_mushroom` | 50% |

Data basis: the matching pack inside both [4.0.20 release JARs](https://github.com/Vivoxti/AgriCraft-ReReloaded/releases/tag/4.0.20). The archive comparison covered plants, mutations, soils and fertilizers.
