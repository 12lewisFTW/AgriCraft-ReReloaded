# Biomes O' Plenty: flowers, mushrooms and mutation discovery

The pack contains **11 species and 11 mutation recipes**. It supplies AgriCraft versions of decorative flowers and mushrooms while retaining their native clipping items or dye outputs. Data is included in both AgriCraft builds; install the matching source mod for your loader.

## Starting a species

Most flowers have **no convertible native seed item** in this pack. Their native flower item is not automatically an AgriCraft seed. Obtain the species through its supplied mutation recipe, then use the resulting genetic seed and plant for further breeding.

Glowshroom and toadstool are exceptions with native mushroom items registered as seed inputs. Convert those through the analyzer. Burning blossom, for example, can be bred from Minecraft orange tulip and red tulip; its raw recipe evaluation chance is 50% after selection.

## Flowers and mushrooms have different jobs

Flower harvests often supply a dye product with a **75% chance for one item per Gain trial**. Clipping can separately return the native decorative flower. A clipping entry of 0–1 at 50% can still return nothing when its chance passes and its quantity is zero. Clipping is one separate roll and does not use the normal Gain trial multiplier.

Mushrooms yield **2–5 mushrooms at 100% per produce trial**. Their requirements use damp, neutral soil, medium-or-lower nutrients and light **0–10** before tolerance, unlike the bright-light flower beds. At Gain 1 this product pattern averages 3.5; Gain 9–10 averages 14.

All species use base growth chance 0.75, Growth bonus 0.025 and reset to stage 4 after harvest. One-parent cloning is allowed with the standard 10% spread roll after selection. The [catalog](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Biomes-O-Plenty-Catalog) lists individual soils, outputs, clipping and all mutation pairs.

## Special behavior and scope

Glowshroom retains its authored brightness modifier. Burning blossom has a burning modifier and flame/smoke particles at the mature stage; account for it when designing a decorative bed. These are species traits, not a blanket effect for all BOP plants.

The pack does not convert every BOP block, biome feature or tree into a farm crop. Its seasonal tags are authored data; external seasonal restrictions are not active through the current Serene Seasons/Fabric Seasons adapters. See [Crop Packs and Seed Conversion](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Crop-Packs-and-Seed-Conversion) and [Genes and Balance](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Genes-and-Balance).
