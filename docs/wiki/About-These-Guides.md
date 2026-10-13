# Version scope and evidence

These English player guides describe **AgriCraft ReReloaded 4.0.20** for **Minecraft 1.21.1**. They explain observable farming behavior and its tradeoffs rather than promise features from unfinished adapters or transfer every source-mod mechanic to converted plants.

## How the numbers were checked

All six built-in conversion packs were extracted from the released NeoForge and Fabric JARs. Their plants, mutations, soils and fertilizers were compared and found identical. The catalogs come from those shipped records, including product quantities/chances and the exact mutation parent IDs.

Harvest, growth, parent selection, allele mutation and native Starfield formulas were checked against the release source. Where a descriptive comment differs from the executable calculation, these guides use the calculation. In particular, ordinary Gain uses multiple trials, Mutativity uses integer division in its direction roll, and default positive sprinkler consumption is rounded up to one mB per active tick.

Starfield's native inventory comes from the verified 0.6.3fix1 catalog. Native phases and crop behavior are not inferred from AgriCraft's ordinary plant pack rules.

## What verification does and does not mean

The [release verification](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/1.21.1/docs/releases/4.0.20-VALIDATION.md) records the required automated checks and release artifact validation. The [gameplay acceptance report](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/1.21.1/docs/starfield/GAMEPLAY-ACCEPTANCE.md) records actual client interaction, native machine operation, a full harvesting/pickup/export drone route and save/reload evidence. The [completion report](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/1.21.1/docs/starfield/ACCEPTANCE.md) provides further integration scope.

An archived crop definition is not proof that every source mod exists on both loaders. A registered recipe/HUD plugin is not a claim of a separate complete gameplay run for every loader/mod version. Native Starfield activation is limited to the exact verified release. These distinctions are reflected in the [Compatibility Overview](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Compatibility-Overview).

## Server-specific balance

Settings, replacement datapacks and third-party addons can change the rules. Use the local recipe viewer and server configuration to resolve differences. Product ranges in the catalogs are per trial; raw mutation chances are evaluated after parent selection. Expected yield is an average, not a guaranteed stack size.

The source copies of these wiki pages are maintained in [the repository](https://github.com/Vivoxti/AgriCraft-ReReloaded/tree/1.21.1/docs/wiki). Historical iteration reports preserve their original milestone and can contain older limits; the current gameplay guide and acceptance evidence describe the supported release.
