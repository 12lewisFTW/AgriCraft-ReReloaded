# Industrial Foregoing: Plant Gatherer farms

This **NeoForge** module lets the **Plant Gatherer** harvest ready AgriCraft crops. It is enabled by default with `compat.industrialforegoing=true`. The verified release environment used Industrial Foregoing **1.21-3.6.39** and Titanium **1.21-4.0.45**.

## Farm setup

Supply the machine's normal energy, configure its work area, place crop beds inside that area and provide output storage. Establish the seeds and soil normally. The integration recognizes harvestable crops and uses AgriCraft's harvest action rather than breaking their blocks and hoping for ordinary block loot.

Converted crops produce the same [Gain trials](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Genes-and-Balance) as a manual harvest and return to their authored post-harvest stage. Immature crops are skipped by the harvestability check. The adapter provides harvesting, not automatic genome selection, breeding, watering or a general Plant Sower seed adapter.

## Native Starfield

The supported [native module](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Starfield-Pastoral) extends harvesting to ordinary native crops. It retains their harvest/regrowth transitions and source genome in primary produce. The operation uses native automation without a player, so it grants no farming XP or player profession bonus. Native season, maturity and access restrictions remain; forage and giants are not harvested by this route. A one-shot field still needs native replanting.

## Balance and troubleshooting

Energy, range upgrades, work scheduling, output handling and byproduct behavior such as sludge remain Industrial Foregoing's own mechanics. AgriCraft does not add a new universal energy price, guarantee a fixed sludge amount, or accelerate crop growth because the gatherer is present. Improving genes changes harvest output or growth under the crop's own rules; machine capacity can become the limiting factor.

If a ready crop is ignored, check the integration setting, actual work area, power, storage and native access requirements. Restart after changing compatibility settings. Separate genuine readiness from a visually large but immature tall crop.

Real gatherer operation and native regrowth/output preservation are recorded in the [gameplay acceptance report](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/1.21.1/docs/starfield/GAMEPLAY-ACCEPTANCE.md). See [Configuration and Troubleshooting](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Configuration-and-Troubleshooting).

## Basis of this guide

Defaults refer to AgriCraft ReReloaded **4.0.20**, Minecraft **1.21.1**. Modpacks can override data and settings. [Gatherer adapter](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/6d6935f09e749923566ff0de3d0c76e0da5b3c4c/neoforge/src/main/java/com/agricraft/agricraft/common/plugin/industrialforegoing/AgriCraftPlantRecollectable.java).
