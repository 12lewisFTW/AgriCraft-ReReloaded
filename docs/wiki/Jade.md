# Jade: diagnose a planting and its soil

The ordinary AgriCraft Jade provider is included in both loader builds. Point at the actual crop to inspect it; use the extended/sneaking view for additional data. Jade is optional and does not change farming balance.

## Ordinary crops

The crop display provides growth information and exposes species, visible expressed stats, growing-condition information and weeds through its applicable views. Inspect soil in the extended/sneaking view to see humidity, acidity and nutrients. These readouts help distinguish an immature crop from a plant stalled by unsuitable conditions.

Hidden-stat settings are respected. The regular HUD's expressed stat is not a replacement for examining both alleles with the magnifying glass. A growth percentage describes the current ordinary growth stage; it is not a prediction of seconds until harvest.

## Native Starfield provider

On **NeoForge**, the active, version-verified [Starfield module](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Starfield-Pastoral) has a dedicated Jade inspection provider. It reports actual native progress and genes from the planting rather than interpreting the native block as an ordinary eight-stage AgriCraft crop. Enable the relevant Jade inspection option if your HUD hides that section.

Remaining watered days depend on future water, suitable season and native location conditions. They are conditional progress information, not an absolute date guarantee. Looking at either part of a tall planting refers to the same native root.

This native provider does not activate for unsupported Starfield versions or when `compat.starfield=false`. Use [Configuration and Troubleshooting](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Configuration-and-Troubleshooting) if you see ordinary Jade block information but no genetic native inspection.

## Design role

The integration makes soil and breeding choices visible without awarding buffs. Pair it with JEI/EMI for species-wide requirements and with the analyzer or magnifying glass for selecting a particular genome. If several HUD mods repeat the same information, adjust their display preferences; duplicate text does not indicate multiple gene bonuses.

## Basis of this guide

Defaults refer to AgriCraft ReReloaded **4.0.20**, Minecraft **1.21.1**. Modpacks can override data and settings. [Ordinary Jade provider](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/6d6935f09e749923566ff0de3d0c76e0da5b3c4c/common/src/main/java/com/agricraft/agricraft/compat/jade/AgriCraftJadePlugin.java) · [Native provider](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/6d6935f09e749923566ff0de3d0c76e0da5b3c4c/neoforge/src/main/java/com/agricraft/agricraft/compat/starfield/StarfieldJadePlugin.java).
