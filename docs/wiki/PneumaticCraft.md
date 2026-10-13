# PneumaticCraft: harvesting, pickup and delivery drones

This **NeoForge** module registers AgriCraft crops with PneumaticCraft's harvesting system. It is enabled by `compat.pneumaticcraft=true`. The release environment used PneumaticCraft **8.2.21**.

## Program the complete job

1. Define the farm work area and a harvesting step that reaches the mature plants.
2. Add an **item pickup** step: this adapter spawns harvested items into the world.
3. Add a delivery or inventory-export step and a destination with enough room.
4. Provide pressure/charge and the normal drone upgrades and access permissions required by the program.

Harvesting alone does not automatically insert produce into the drone's inventory. A crop can be correctly harvested while its output remains on the ground if the pickup step is missing. Test a short route before extending the farm.

## Crop behavior and filters

Only ready plants pass the adapter's harvest check. Ordinary AgriCraft crops use their normal produce rolls, Gain trials and stage reset. The adapter does not provide a Growth bonus, free pressure or a shortcut to higher stats.

Native Starfield crop filters use the native primary produce identity, allowing the program to identify ordinary supported crop outputs instead of depending on destructive block loot. Looking up a filter does not run a harvest or simulate extra Gain. Use the actual expected produce in your pickup/export filters and allow for side products where needed.

## Native Starfield support

With the [verified native integration](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Starfield-Pastoral) active, ordinary native crops retain their calendar, readiness, access gates, output quality and regrowth transitions. Primary produce keeps both gene alleles and receives Gain once. Harvesting uses native automation with no player XP or profession bonus. Tall parts belong to one planting; a second visit to an unready crop does not produce a second harvest. Seasonal forage and giants are excluded from this route.

## Throughput and evidence

Pressure use, movement speed, upgrades, work-area rules and delivery scheduling remain PneumaticCraft's responsibility. Extra yield increases pickup and cargo demand, so improving Gain can require more storage or more frequent deliveries rather than a larger field.

A real drone completed harvest → pickup → chest export for native beans, coffee and potatoes in the [gameplay acceptance report](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/1.21.1/docs/starfield/GAMEPLAY-ACCEPTANCE.md). The save was reopened and another pass over immature crops produced no duplicate yield. This full route check is separate from the narrower automated harvest-handler tests.

If a drone harvests but delivers nothing, first inspect the ground output and pickup/export steps. If it refuses to harvest, check readiness, the integration setting, work area and native permissions. See [Configuration and Troubleshooting](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Configuration-and-Troubleshooting).

## Basis of this guide

Defaults refer to AgriCraft ReReloaded **4.0.20**, Minecraft **1.21.1**. Modpacks can override data and settings. [Drone harvest adapter](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/6d6935f09e749923566ff0de3d0c76e0da5b3c4c/neoforge/src/main/java/com/agricraft/agricraft/common/plugin/pneumaticcraft/AgriCraftHarvestHandler.java).
