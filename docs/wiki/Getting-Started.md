# Getting started with compatible crops

Install the AgriCraft build for your loader, its required dependencies, and the source mod for each crop you want to grow. A compatibility pack supplies crop definitions; it does not install the other mod. The supported release uses Minecraft 1.21.1 and Java 21. Starfield Pastoral requires the NeoForge build and the exact supported Starfield version.

## Two kinds of farm

**Converted crops** use AgriCraft crop blocks. This includes the six [built-in crop packs](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Crop-Packs-and-Seed-Conversion). Their original seeds become AgriCraft seeds, their harvest returns the source mod's items, and AgriCraft controls growth, weeds and soil requirements.

**Native Starfield crops** keep Starfield's blocks, seed items, calendar, water, seasons and quality. AgriCraft adds a genome and tools to that planting. Read [Starfield Pastoral](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Starfield-Pastoral) before building this kind of farm: its Gain and Growth rules differ from converted crops.

## Your first converted crop

1. Obtain a supported seed in the source mod's usual way. Install that mod's matching loader version and leave automatic compatibility packs enabled.
2. Place an empty seed analyzer. Sneak-right-click it with the source seed to convert one seed, then analyze the resulting AgriCraft seed. The analyzer interface also accepts supported seeds.
3. Prepare suitable soil and light. Place **single** crop sticks and plant the AgriCraft seed. Use the journal or [JEI](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/JEI) / [EMI](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/EMI) to check requirements.
4. Let it mature and harvest without breaking the crop. The species determines which stage it returns to. Croptopia resets to stage 0; most other compatible packs reset to stage 4.
5. Keep two mature parents beside empty **double** crop sticks to breed. Inspect offspring and retain the traits you need.

The magnifying glass reveals both alleles. A trowel moves a planting with its genome; a rake removes ordinary weeds. The analyzer and journal help you compare varieties before expanding the farm.

## Choose an improvement

Use [Genes and Balance](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Genes-and-Balance) to plan for higher yield, faster growth, wider soil tolerance or more reliable breeding. Good soil still matters; high Growth cannot make an unsuitable location fertile. For a large farm, add [irrigation](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Soil-Irrigation-and-Greenhouses) and select a compatible harvesting machine from the [module index](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Compatibility-Overview).

Some decorative plants have no convertible seed item. Obtain them through the documented mutation recipes instead. Different species do not automatically cross into a third species: a matching recipe must exist.

## Basis of this guide

Defaults refer to AgriCraft ReReloaded **4.0.20**, Minecraft **1.21.1**. Modpacks can override data and settings. [Core settings](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/6d6935f09e749923566ff0de3d0c76e0da5b3c4c/common/src/main/java/com/agricraft/agricraft/api/config/CoreConfig.java) · [Breeding rules](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/6d6935f09e749923566ff0de3d0c76e0da5b3c4c/common/src/main/java/com/agricraft/agricraft/api/genetic/AgriCrossBreedEngine.java).
