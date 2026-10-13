# EMI: crop planning in a recipe viewer

AgriCraft's EMI plugin supplies ordinary crop information on the corresponding NeoForge or Fabric setup. Choose an EMI build for your loader. The plugin does not change growth speed, yields, mutation rates or server rules.

## Planning a field

Search for an AgriCraft species or its seed, then use the **produce, clipping, mutation and growth-requirement** categories. Requirements help match plants to beds; mutation recipes help plan which mature parents to keep. Loaded compatible datapack species use the same categories as built-in ordinary plants.

Seeds are identified by species for recipe-viewer comparison. That makes the list manageable, but does not mean every displayed seed has your planting's actual six-stat genome. Analyze the real item for breeding decisions.

## Balance interpretation

Produce lists describe possible output. The ordinary [Gain trial count](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Genes-and-Balance) and each product's chance/range determine a real harvest. Clipping is separate and is not multiplied by the ordinary Gain trial loop. Mutation chance belongs to the recipe evaluation after the breeding engine's checks, not to every world tick.

If an entry is absent, confirm the source mod and crop data are loaded and that any required mutation parents exist. Server datapacks can override the default tables in this wiki.

## Native Starfield scope

These custom categories use the ordinary plant registry. Native [Starfield crops](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Starfield-Pastoral) retain their original items and daily systems rather than becoming fake generic AgriCraft seeds. Use [Starfield Crop Catalog](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Starfield-Crop-Catalog), Jade/The One Probe or the magnifying glass for their native phase and genome information. Native machines may have separate recipe entries supplied by their own EMI integrations.

## Basis of this guide

Defaults refer to AgriCraft ReReloaded **4.0.20**, Minecraft **1.21.1**. Modpacks can override data and settings. [EMI registration](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/6d6935f09e749923566ff0de3d0c76e0da5b3c4c/common/src/main/java/com/agricraft/agricraft/compat/emi/AgriCraftEmiPlugin.java).
