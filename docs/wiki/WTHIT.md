# WTHIT: ordinary crop and soil inspection

AgriCraft includes a WTHIT plugin descriptor in both NeoForge and Fabric release JARs. Install the appropriate WTHIT build and use its crop/soil display. This optional information module does not alter yields or growing speed.

## What it helps explain

The ordinary crop provider displays growth and applicable crop information, with extra species and visible gene data in the extended/sneaking view. Soil inspection exposes humidity, acidity and nutrients. Weed and growing-condition information help you understand why a bed is not progressing.

Use the HUD to inspect a live ordinary AgriCraft crop, and a recipe viewer for the species' possible outputs and requirements. Hidden-stat configuration is respected. A displayed stat is the expressed trait; use the magnifying glass when you need both inherited alleles.

## Native Starfield limitation

WTHIT has **no dedicated native Starfield inspection provider** in AgriCraft 4.0.20. Its ordinary CropBlock provider does not extend itself to every native crop block. For native genes, daily phases and conditional watered-day progress use [Jade](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Jade), [The One Probe](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/The-One-Probe) on NeoForge, or the magnifying glass.

This distinction preserves accurate information: native Starfield uses a calendar rather than the ordinary growth stages. It should not be assigned a made-up generic growth percentage by assuming all compatible crops share the same engine.

If ordinary crop details are absent, check that WTHIT's server/client requirements and plugin settings are satisfied, the block is actually an AgriCraft crop, and the crop data is loaded. See [Crop Packs and Seed Conversion](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Crop-Packs-and-Seed-Conversion).

## Basis of this guide

Defaults refer to AgriCraft ReReloaded **4.0.20**, Minecraft **1.21.1**. Modpacks can override data and settings. [WTHIT provider](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/6d6935f09e749923566ff0de3d0c76e0da5b3c4c/common/src/main/java/com/agricraft/agricraft/compat/wthit/AgriCraftWthitPlugin.java).
