# The One Probe: ordinary and native planting information

This optional information integration is available in the **NeoForge** build. It registers when The One Probe is installed. It adds readable crop information without changing crop balance or granting farming bonuses.

## Ordinary AgriCraft crops

The normal view shows a rounded **growth percentage** for every ordinary AgriCraft species, including compatible datapack crops. Extended mode also shows the species and visible expressed gene stats. How you enter extended mode depends on your The One Probe settings, commonly by sneaking. Weed information is included when weeds are present.

The percentage represents ordinary growth progress. It does not tell you a guaranteed remaining time, and the displayed expressed values do not reveal both alleles. Use the magnifying glass for full genetic inspection and Jade/WTHIT or a recipe viewer for soil planning.

## Native Starfield crops

When the [verified native integration](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Starfield-Pastoral) is active, The One Probe uses the common native inspection report. It describes actual native planting progress and gene data; it does not substitute a fictional eight-stage AgriCraft percentage. Extended mode exposes the expanded report.

Native remaining watered days are conditional on continued watering, valid season and access rules. A tall crop's two parts refer to the same planting. Unsupported Starfield versions and `compat.starfield=false` do not provide this native genetic report.

## Choosing a HUD

Use The One Probe if it fits your pack's existing interface. Multiple HUD providers do not stack farming effects; configure duplicate displays for readability. See [Genes and Balance](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Genes-and-Balance) to interpret ordinary stats and [Starfield Pastoral](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Starfield-Pastoral) for native daily bonuses.

## Basis of this guide

Defaults refer to AgriCraft ReReloaded **4.0.20**, Minecraft **1.21.1**. Modpacks can override data and settings. [Probe provider](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/6d6935f09e749923566ff0de3d0c76e0da5b3c4c/neoforge/src/main/java/com/agricraft/agricraft/compat/theoneprobe/AgriCraftProbePlugin.java).
