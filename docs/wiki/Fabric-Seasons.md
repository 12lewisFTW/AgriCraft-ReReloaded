# Fabric Seasons: current integration status

**The Fabric Seasons adapter in AgriCraft 4.0.20 is an unrestricted placeholder.** Its initializer is called on Fabric, but it reports `ANY`; it does not read the external calendar to enforce the authored crop season tags.

## Planning a farm

Do not assume a converted crop with a summer tag becomes unusable in winter through this adapter. Use its actual AgriCraft soil, light and weed requirements to plan production. Fabric Seasons may independently affect its own supported blocks; this page makes no blanket claim about every native plant in the modpack.

The current module adds no seasonal yield multiplier, custom calendar or documented seasonal mutation bonus. The AgriCraft greenhouse season-bypass setting does not become a new Fabric Seasons progression unlock just because these tags exist.

## Other calendars

[Serene Seasons](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Serene-Seasons) has a similar inactive/unrestricted AgriCraft integration on NeoForge. [Starfield Pastoral](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Starfield-Pastoral) uses a separate native seasonal system and is NeoForge-only; this Fabric placeholder does not provide that native support.

If a server supplies its own season provider, datapacks or scripts, use its server-specific rules rather than treating the release defaults as universal. See [Crop Packs and Seed Conversion](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Crop-Packs-and-Seed-Conversion) and [Configuration and Troubleshooting](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Configuration-and-Troubleshooting).

## Basis of this guide

Defaults refer to AgriCraft ReReloaded **4.0.20**, Minecraft **1.21.1**. Modpacks can override data and settings. [Fabric season adapter](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/6d6935f09e749923566ff0de3d0c76e0da5b3c4c/fabric/src/main/java/com/agricraft/agricraft/common/plugin/FabricSeasonPlugin.java).
