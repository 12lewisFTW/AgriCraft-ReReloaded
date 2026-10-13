# Iteration 13 - regressions, migrations and release preparation

Date: 9 October 2026. Tested dependency - official Starfield Pastoral 0.6.3fix1 for Minecraft 1.21.1 / NeoForge 21.1.226, commit `df2151c66a2a9c02855e4db23108e57c422ce78e`. Continued [iteration 12](ITERATION-12.md).

Subsequent work closed the game placement and display of crop sticks, brought the server matrix to 135 checks, and added real client, machine, and installation checks. The current result is in [completion report](ACCEPTANCE.md); Below is the original iteration volume 13.

## Changes

`StarfieldPlantData` now maintains a derived index by dimension and chunk. `inChunk` copies only the records of the desired chunk, instead of traversing all plants in all dimensions. Inserting, changing material, deleting and loading SavedData updates the index. The file format does not change: schema 1 and the saved UUID/genome remain the same. Future schemas and malformed entries are saved in the same way.

Fixed migration coordinate: `baseY + localY` instead of `baseY`. Previously, a plant in the middle of a section was looked for at its lower border and could have been missed during the initial scanning. A separate script checks the heights of −60, −49 and 85, preservation of native progress, and stability of the UUID when viewed again.

The first migration of a loaded chunk only scans through sections with native crops. Repeated loading/observation requests for the same chunk object do not rescan the blocks. The cache uses weak references and allows for SavedData replacement. Updating the catalog and changing the integration state clears the cache. This does not add periodic full world traversal or loading of remote chunks. Writing a complete file when saving a world remains a normal SavedData operation.

Disabled native conversion of a planted wild seed into forage now retains its original genome, UUID, seed template and crop sticks material, updating only the shape. Selecting forage and changing the native block are performed using the original method. Natural forage does not acquire a genome; truly deleting a plant still clears its record. No genetic effects are executed in disabled mode.

Added five release checks and a separate migration profile across three JVMs. It changes the actual setting between runs of the same test world. Comparative NBT contains the expected values, but does not restore the plant or chest. The seeds themselves are in the actual saved chest.

Archive checking has been expanded: native classes, nested dependencies, resources `assets/stardewcraft`, `data/stardewcraft` and all test classes/resources are prohibited. Added [general guide](USER-GUIDE.md) about calendar, six gene formulas, crops, tools, production, shutdown and support limits.

## Checks

All tests were performed in separate local installations of NeoForge GameTestServer without a graphical client. The main enabled profile uses the real runtime Create; migration uses a separate shared world of three JVMs.

| Check | Result | Log in `build/starfield-iteration0` |
|---|---|---|
| Full profile included: genes, calendar, harvest, tools, watering, selection, special forms, giants, machines, Create, five new regressions | **125 mandatory tests passed** | `iteration13-enabled-final.log` |
| Migration: recording with support enabled | **2 passed**, normal save/stop | `iteration13-migration-write-final.log` |
| The same world with the setting actually disabled; native wild seed transformation | **2 passed**, normal save/stop | `iteration13-migration-disabled-final.log` |
| The same world after switching on; reading plants and real chest | **2 passed**, normal save/stop | `iteration13-migration-read-final.log` |
| NeoForge without binary Starfield | **1 passed**, the native API class is actually missing in runtime | `iteration13-absent.log` |
| Fabric without native bridge | **2 passed** | `iteration13-fabric.log` |
| Regular `:neoforge:build :fabric:build` without test properties | **Success** | `iteration13-release-build.log` |
| Isolation of five common JARs: common dev, NeoForge/Fabric release and dev-shadow | **All passed** | `verify_release.py`, recheck after assembly |
| Previous Croptopia pack in both release builds | **58 crops, 464 crop models, 58 seed models, four locales and optional TOP** | `verify_croptopia_pack.py` for now `build/compat-validation/mods/croptopia.jar` |

Isolated checks of real machine restarts, days and giant sections from iteration 12 (77 + 77) remain a separate historical result; We do not pass them off as new runs of this iteration.

A large farm check loads **16 384 SavedData** records in two dimensions and performs 1000 samples of one chunk. It checks for index completeness, dimension isolation, immutable snapshot, persistence/loading, and no new chunks loaded. This is not a measurement of the client's FPS and does not measure the full native daily calculation of 16 384 real blocks.

The setup switch scripts deliver the public `ServerTickEvent.Post` via the event bus and test the migration via the normal `scan` input. They do not clear the internal cache directly.

Two regular server players check for available inspection, invalid dimension, stale UUID, and deleted unloaded chunk. Existing payload/cache checks check for world/dimension changes and cache clearing. These are server-based GameTests with FakePlayer and offline caches; two real graphic clients did not start at this stage.

Native migration planting synchronizes the external stage with the given phase through the original `syncVisualStage` before saving. The first diagnostic comparison revealed mismatched test preparation: native loading naturally restored `growth_stage` in phase. It was the preparation of the test that was corrected; an arbitrary mismatched visual stage is not declared to be a conservation invariant.

In the first combined migration profile with catalog/datapack-reload, all 7 tests passed, but the test server went into a loop when stopped at `ChunkMap.scheduleUnload/processUnloads`. After recording the diagnosis, only this test process is stopped; such a run is not counted as a successful save. Migration profiles are now isolated from other people's reload scripts; the full regular profile continues to check the datapack reload separately. The cause of the one-time freeze has not been proven; log `iteration13-migration-write-stalled.log` and thread snapshot saved for investigation.

## Official version and materials

Re-checked [official release](https://github.com/ChangQingElysium/Starfield-Pastoral/releases/tag/v0.6.3fix1), SHA-256 JAR and 251 pinned source blob, including LICENSE.md. Loading of the verified version and absence/disabling are checked by separate server runs. Untested versions are excluded by early mixin plugin and whitelist bridge; the actual installation of someone else's incompatible version did not run in this iteration.

Starfield has its own non-commercial license](https://github.com/ChangQingElysium/Starfield-Pastoral/blob/v0.6.3fix1/LICENSE.md), not MIT. Resource records and dependency content are not included in the AgriCraft release archives. The external mod is installed separately from the official source. Starfield was not repackaged, the build was published, the version was upgraded, or permissions were obtained to distribute derivative materials. This is a record of tested conditions and not a conclusion about the permissibility of all future distribution methods.

## State at the time of the initial iteration 13

The server part of the preparation is confirmed by the final matrix. Additional fixes did not change version 4.0.19 and were not published. Full public release **not declared ready**: game addition/display of crop sticks remains unfinished, graphical acceptance and two real client connections are not verified. A single crash on the combined test server when saving also remains to be investigated before release; successful isolated runs do not prove its cause. This boundary cannot be replaced by successful builds or server-side tests.

The limits of previous iterations are preserved: automation fails for forage/giants; arbitrary external storage after partial acceptance and exclusion is not rolled back; canceled external drop Create is not compensated. PneumaticCraft/Industrial Foregoing are collected, gaming machines have not been tested separately. Direct reuse of the harvested coffee bean does not transfer the genome through the product component. Other known limitations are listed in the manual.

The incomplete game and client items listed here are subsequently closed; Current results and remaining limits are given in [completion report](ACCEPTANCE.md). Publishing and changing the version remains a separate step. Iteration 14 was canceled by user; strength does not increase the giant's native chance.
