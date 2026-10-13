# JEI: look up crops, requirements and mutation recipes

AgriCraft registers a JEI plugin for its ordinary plant registry. The plugin is included for NeoForge and has a Fabric entrypoint; use the JEI version appropriate to your loader. Release client verification includes the NeoForge setup. This is an information integration, with no growing or harvesting bonus.

## What you can look up

| AgriCraft category | Use it to answer |
|---|---|
| Crop produce | What can this species yield? |
| Crop clipping | What can the clipper collect? |
| Crop mutations | Which parent species can produce this child? |
| Growth requirements | Which soil, light and other conditions does it need? |

Search for the crop or its AgriCraft seed and open the available recipe/use categories. Compatible datapack crops appear when their species data is loaded. Mutation entries require a valid loaded recipe; a missing parent mod can make a path unavailable.

## How to read the display

AgriCraft seeds are distinguished by **species**, not enumerated as every possible six-stat genome. Two seeds of the same species can have very different farming performance. Inspect the actual seed in an analyzer or magnifying glass before choosing a parent.

Use [Genes and Balance](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Genes-and-Balance) to interpret the produce amounts and chances: ordinary Gain performs multiple independent trials. A displayed product is not a promise of exactly that amount from every harvest. Likewise, raw mutation chance is evaluated after parent selection.

## Native and converted plants

The custom categories describe the ordinary AgriCraft plant registry. Native [Starfield crops](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Starfield-Pastoral) are deliberately kept as native items and blocks rather than inserted as fake converted seeds or fictional mutation ingredients. Use the native catalog and inspection tools for their daily phases and gene effects. Other mods' JEI plugins may separately document their native crafting or production recipes.

The old magnifying-helmet crafting extension is not active in the current plugin. The four categories above are the supported crop-information features.

## Basis of this guide

Defaults refer to AgriCraft ReReloaded **4.0.20**, Minecraft **1.21.1**. Modpacks can override data and settings. [JEI registration](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/6d6935f09e749923566ff0de3d0c76e0da5b3c4c/common/src/main/java/com/agricraft/agricraft/compat/jei/AgriCraftJeiPlugin.java).
