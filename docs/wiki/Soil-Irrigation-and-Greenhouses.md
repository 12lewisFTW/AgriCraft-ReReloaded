# Soil, irrigation and greenhouses

This page covers ordinary AgriCraft farms. Native Starfield watering and greenhouse permissions remain under [Starfield rules](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Starfield-Pastoral).

## Match the soil before improving speed

AgriCraft soils have **humidity, acidity and nutrients**, plus a growth modifier. A plant specifies acceptable values and Strength tolerances. Fertile means its current location passes these requirements. A nutrient-rich block can still have the wrong humidity or acidity.

Use the journal and [JEI](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/JEI) or [EMI](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/EMI) for plant requirements. [Jade](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Jade) and [WTHIT](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/WTHIT) provide soil information when inspecting the soil in their extended/sneaking view. Check light and any special conditions as well.

Farmer's Delight adds rich soil and rich soil farmland with very high nutrients and a 1.8 growth modifier, but their humidity and acidity differ. Mystical Agriculture adds five essence farmlands with a 1.0 AgriCraft growth modifier and a separate native effective-farmland requirement on NeoForge. These are distinct tradeoffs, not interchangeable universal upgrades.

## A sprinkler layout

Connect a water tank to irrigation channels and hang a sprinkler directly below a channel. Keep the area below clear. A sprinkler scans a **7×7** horizontal footprint and reaches **up to five blocks below** itself. Obstacles can stop water reaching a lower block. Irrigation operates in loaded chunks; it does not keep distant farms loaded.

| Default setting | Value | Practical meaning |
|---|---:|---|
| `irrigation.tank_capacity` | 8000 mB | Tank storage |
| `irrigation.channel_capacity` | 500 mB | Channel storage |
| `irrigation.rain_fill_rate` | 5 mB/tick | Configured rain collection rate |
| `irrigation.sprinkler_water_consumption` | 10 | See the actual consumption calculation below |
| `irrigation.sprinkler_growth_chance` | 0.2 | Ordinary eligible plants may receive a growth tick on a pass |
| `irrigation.sprinkler_interval` | 40 ticks | Minimum interval between sweep starts |

The water-consumption setting is labelled mB/second, but release 4.0.20 calculates each active tick as `max(1, floor(setting / 20))` for positive settings. At the default 10, that is **1 mB per active tick**, or about **20 mB/second** at 20 TPS while continuously sweeping. A value at or below zero consumes no water. Size your water supply using this actual behavior.

The sprinkler processes one column per active tick, so a full supplied sweep takes 49 ticks. The 40-tick default interval is measured between starts; it is not an additional pause after every sweep. Normal eligible plants can receive the 20% growth-tick roll when visited, followed by their own growth rules. It does not guarantee a growth stage on every pass.

For native Starfield crops the special irrigation path waters eligible native soil or pots. It does **not** advance the daily calendar, force extra phases or grant extra regrowth. Native sand, infertile-soil and automatic-watering cases retain their own restrictions.

## AgriCraft greenhouses

Default greenhouse settings are a block limit of **1024**, minimum glass fraction of **0.4** in the ceiling, `greenhouse.ignores_seasons=true`, and `greenhouse.growth_modifier=1.05`. Once recognized, an ordinary crop receives the configured 5% growth-roll modifier; the seasonal check can be ignored. This does not remove soil, light or other requirements.

The Serene Seasons and Fabric Seasons adapters do not currently enforce external seasonal restrictions, so the seasonal option does not provide an extra calendar benefit from those mods in 4.0.20. An AgriCraft enclosure also does not become a native Starfield greenhouse: Starfield's own location and season rules decide that.

## When a farm stalls

Check the actual soil and light, available water, channel connection, obstructions, weed state and loaded chunks. For a native Starfield planting, also check the native date, season, watered days and location. A faster sprinkler cannot repair a missing source-mod seed definition or an incompatible Starfield version.

## Basis of this guide

Defaults refer to AgriCraft ReReloaded **4.0.20**, Minecraft **1.21.1**. Modpacks can override data and settings. [Sprinkler behavior](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/6d6935f09e749923566ff0de3d0c76e0da5b3c4c/common/src/main/java/com/agricraft/agricraft/common/block/entity/SprinklerBlockEntity.java) · [Irrigation defaults](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/6d6935f09e749923566ff0de3d0c76e0da5b3c4c/common/src/main/java/com/agricraft/agricraft/api/config/IrrigationConfig.java) · [Greenhouse defaults](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/6d6935f09e749923566ff0de3d0c76e0da5b3c4c/common/src/main/java/com/agricraft/agricraft/api/config/GreenhouseConfig.java).
