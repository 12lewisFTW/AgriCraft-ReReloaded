# Serene Seasons: current integration status

**AgriCraft 4.0.20 does not actively enforce Serene Seasons calendar restrictions on converted crops.** A NeoForge seasonal adapter remains in the source, but its initialization call is inactive and its season result is unrestricted (`ANY`).

## What this means for a player

Compatibility crop definitions may contain spring, summer, autumn or winter tags. Those tags are authored crop data, not proof that the current Serene Seasons calendar is being consulted. Installing Serene Seasons does not, through this AgriCraft adapter, turn a tagged converted crop into a working seasonal crop.

Serene Seasons can still have its own effects elsewhere in your modpack. This page only describes AgriCraft's adapter and does not promise that all plants from all installed mods ignore native seasonal behavior.

## Balance implications

Do not plan a seasonal food shortage or claim an AgriCraft greenhouse unlocks year-round growing specifically through this adapter in 4.0.20. Ordinary AgriCraft soil, light, weed and gene rules remain the documented balance. Additional modpack scripts or replacement season providers can change that; document those separately.

Native [Starfield seasons](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Starfield-Pastoral) are a different functioning calendar system. Their restrictions and Resistance survival window do not depend on this adapter. See [Fabric Seasons](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Fabric-Seasons) for the analogous Fabric status and [Soil Irrigation and Greenhouses](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Soil-Irrigation-and-Greenhouses) for ordinary greenhouse settings.

## Basis of this guide

Defaults refer to AgriCraft ReReloaded **4.0.20**, Minecraft **1.21.1**. Modpacks can override data and settings. [Season adapter](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/6d6935f09e749923566ff0de3d0c76e0da5b3c4c/neoforge/src/main/java/com/agricraft/agricraft/common/plugin/SereneSeasonPlugin.java).
