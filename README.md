# AgriCraft

Agricultural farming extended.

Read the [English player wiki](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki) for every compatibility module, practical farm setups, crop catalogs and exact gene balances. The [compatibility index](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Compatibility-Overview) lists platform support and the status of unfinished adapters.

Original available on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/agricraft) and [Modrinth](https://modrinth.com/mod/agricraft).

Fork available on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/agricraft-rereloaded)

## Available versions

| Version | Support     |
|---------|-------------|
| 1.21.1  | Supported   |

Croptopia's 58 farmland crops are supported through a built-in compatibility pack on NeoForge and Fabric. The pack is enabled automatically when Croptopia is installed, unless automatic compatibility packs are disabled in AgriCraft's configuration. Native seeds can be converted through the seed analyzer (sneak-right-click), then planted and bred with AgriCraft genes. Harvesting returns Croptopia produce. This pack adds no species mutation recipes and does not convert fruit trees.

On NeoForge, The One Probe displays growth percentages for all AgriCraft plants, including datapack crops. Sneaking also displays species and visible gene stats. The One Probe is optional.

Optional **Starfield Pastoral 0.6.3fix1** compatibility is available on NeoForge from **4.0.20**. All 50 supported native seed types retain Starfield's daily calendar, models, seasons, water and quality rules while using AgriCraft's six genes, crop sticks, breeding, analyzer, trowel and irrigation. Native produce passes its genome through the seed maker; Create, Industrial Foregoing and PneumaticCraft can harvest supported ordinary native crops. The integration activates only for the verified Starfield version with `compat.starfield` enabled. Starfield is not bundled and this integration is not available on Fabric.

See the [user guide](docs/starfield/USER-GUIDE.md) for setup and gameplay, [release notes](docs/releases/4.0.20.md) for supported features and limits, and [acceptance report](docs/starfield/ACCEPTANCE.md) for verification.

For source builds, generate the compatibility data with `gradlew :neoforge:runData` before running `gradlew build`. To check the Croptopia packs against an installed Croptopia 4.x jar, run `python tests/verify_croptopia_pack.py <Croptopia.jar> <AgriCraft-neoforge.jar> <AgriCraft-fabric.jar>`.
