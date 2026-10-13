# Botania: no shipped crop compatibility pack in 4.0.20

**Botania has no active dedicated crop compatibility pack in the 4.0.20 release archives.** Legacy translation labels and a data-generation helper are present in the project, but they do not establish a shipped, enabled farming module.

## Avoid a misleading progression promise

This release does not document a Botania flower-to-AgriCraft-seed conversion, mana-powered growth bonus, mana cost, functional-flower automation adapter or new Botania mutation route. There is no active `compat.botania` option in AgriCraft's compatibility category.

Some other crop packs can reference resource species or plants whose items come from additional mods. That is not equivalent to a general Botania farming integration. An optional resource-crop definition in [Mystical Agriculture](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Mystical-Agriculture) still requires its own native seeds and supporting installed items.

## For a modpack

A separately supplied custom datapack or addon may implement Botania crops. Its recipes, yields, costs and compatibility are its own rules; do not attribute those balances to the base release. Use the [compatibility index](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Compatibility-Overview) for the modules actually supplied and [Crop Packs and Seed Conversion](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Crop-Packs-and-Seed-Conversion) for the six shipped conversion packs.

## Basis of this guide

Defaults refer to AgriCraft ReReloaded **4.0.20**, Minecraft **1.21.1**. Modpacks can override data and settings. [Active compatibility settings](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/6d6935f09e749923566ff0de3d0c76e0da5b3c4c/common/src/main/java/com/agricraft/agricraft/api/config/CompatConfig.java).
