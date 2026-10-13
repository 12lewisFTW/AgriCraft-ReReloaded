# Starfield × AgriCraft: Iteration Specification 0

Date: 8 October 2026. Iteration 0 completed: native block architecture with separate
confirmed by crop sticks data. This is a prototype, not ready-made game compatibility.

## Fixed dependencies

| Component | Version |
|---|---|
| Minecraft/Java | 1.21.1 / 21 |
| AgriCraft | 4.0.19, current working project |
| NeoForge | 21.1.226 |
| Resourceful Config | 3.0.11 |
| Starfield Pastoral | official `0.6.3fix1`, tag `v0.6.3fix1` |
| Starfield sources | `df2151c66a2a9c02855e4db23108e57c422ce78e` |

Build source: [official release](https://github.com/ChangQingElysium/Starfield-Pastoral/releases/tag/v0.6.3fix1).
SHA-256 NeoForge JAR: `87a67827417f4a87abb7a80f0581003e79d3a2cb5d45e19250777162f4bd3758`.
Size: 239 668 522 bytes. Full commit - `tests/starfield/dependencies.json`.
JAR requires NeoForge >= 21.1.217 and Minecraft 1.21.1; JEI is an optional client dependency.
The prototype uses the standard Resourceful Config AgriCraft. The archive of someone else's patch and its JAR are not used.

The Starfield dependency only appears at `-PstarfieldPrototype` in NeoForge. Common and Fabric
do not receive references to its classes. Sources, JARs, test world and logs remain ignored
directories `build`. The prototype classes, its structure and the test flat preset are excluded from NeoForge archived tasks.
The full integration switch belongs to iteration 1 and has not yet been added.

**Note after iteration 1:** The historical description above refers to the original
prototype. NeoForge now uses the same official JAR in `compileOnly` for a separate
protected catalog; added switch `compat.starfield`. Links to Starfield are allowed
only in its native NeoForge implementation. Details - [ITERATION-1.md](ITERATION-1.md).

The official license permits study of source code and private modifications, but limits
public modified builds and redistribution. There is a custom verification code written here;
sources/resources of Starfield and someone else's patch are not included in the released files.
License: [reviewed version](https://github.com/ChangQingElysium/Starfield-Pastoral/blob/df2151c66a2a9c02855e4db23108e57c422ce78e/LICENSE.md).

## Catalog and control views

Officially registered 50 live planting species: original 46 plus
`spring_wild_seed_crop`, `summer_wild_seed_crop`, `fall_wild_seed_crop`, `winter_wild_seed_crop`.
Wild seeds are replaced by seasonal `ForageBlock` when ripe; they cannot be considered ordinary
crop with one fixed crop item. Mixed seeds choose a different type when
planting and are not an additional crop block. `dead_crop` is not included in the live catalog.

Full table: [CROP-CATALOG.md](CROP-CATALOG.md). Machine version: `crop-catalog.json`.
The generator reads the official registrations and inherited methods of the classes and then checks
presence of appropriate classes, blockstates and item models in the committed JAR.
This is a catalog of research, not a promise of already working support for 50 species.

| Representative | Reason for choice | Basic calendar |
|---|---|---|
| potato | Disposable Single Block Crop Extra Drop | `[1,1,1,2,1]`, 6 watered days, spring |
| broccoli | Single block crop with repeat harvest | `[1,2,2,3]`, 8 days, repeat 4 days, autumn |
| corn | Two parts and re-harvest | `[2,3,3,3,3]`, 14 days, repeat 4 days, summer/autumn |
| cauliflower | Single block crop with giant shape | `[1,2,4,4,1]`, 12 days, spring |

Additional mandatory cases: rice/taro with native irrigation from neighboring water;
fiber with growth without watering and fixed quality; cactus only in permitted closed areas
places/on the island; qi fruit with fixed quality; sunflower with extra seeds;
wild seeds with block replacement. The exact geometry is determined by the real properties of the class,
and not according to the usual type of crop in another game.

## What the API actually provides

| Region | Available mechanism | Limit |
|---|---|---|
| Recognition | `StardewCropTypes.register`, `StardewCropRuntime.inspect` | Native blocks are recognized before adapters; addon ID cannot use namespace `stardewcraft` |
| Scheduler | `track`, `untrack`, manager with `GlobalPos` | For a regular date, the deduplication is in `settleCrops`, not in the direct `growOneDay` |
| Historic day | `StardewCropDailyContext` | The season/date is taken from the context; direct public call itself displays the current date |
| Water | List `soilPositions`, humidity `FarmBlock`, offline policy | The addon receives the result of the check; irrigation AgriCraft is not connected yet |
| Infertile soil | Native `advanceOnSoil` before addon callback | The slowdown may come as `context.watered=false`; it cannot be re-accrued |
| Metadata | `StardewCropData`, providers and Data Maps | Data is not the executor of the phases; not all species features are described by these fields |
| Daily mutation | `StardewCropRuntimeAdapter.growOneDay` | Default `UNCHANGED`; the adapter itself masters phases, maturity, regrowth and death |
| Collection | Player/automation context and staging outputs | For addon blocks, crop calculation and plant modification again belong to the adapter |
| Seed maker | `registerSeedMakerOutput` | Resolver receives `Item`, returns `Item`; original `ItemStack` and its genome/components are missing |
| Giants | `StardewGiantCrops` | The native process prohibits the replacement of any block entities, including `CropBlockEntity` AgriCraft |

Daily treatment of plants with addon includes native retardation of infertile soil:
During a watered day, 2/3 days of growth accumulate; a dry day adds nothing. Humidity in context
after this check means admission to growth, and not always a literal absence of water.
Seasonal treatment should also occur in case of such growth failure.

For offline catch-up, the official manager considers the planting to be watered. This is native policy
which cannot be silently replaced by checking the current humidity. Scheduler suppresses replay
same and older date, saves `lastDailyDay` and a separate giant check date.

Key limitation: `StardewCropBlock.growCropOneDayInternal` and additional methods
specific species work with the native block in the world and can change both parts of the plant.
Calling this method with crop sticks instead of its block is not a valid reuse.
`StardewCropData.phaseDays` by itself does not replace such a call.

## View: test result

Source view "`CropBlockEntity` AgriCraft + native state + runtime adapter"
Suitable for registering with scheduler and transmitting daily entries. It **doesn't go all the way
architectural milestone** in the current API:

1. To actually mature, the adapter will have to independently implement native phases,
   fertilization, species exclusion, regrowth and harvesting. This will violate the requirement of one executor.
2. Native giants will reject a site with block entities regardless of the species/maturity match.
3. The seed resolver does not allow you to transfer the genome of a specific item.

The test adapter therefore only changes the diagnostic counters to those marked empty
crop sticks It does not pass this counter off as potato growth and does not add a second growth algorithm.
Another check passes the potato metadata to the adapter without an executor and confirms that
the plant remains unchanged. This is a reproducible proof of the API limit.

To continue, the **unchanged Starfield block + separate SavedData crop sticks/genome** was selected.
`NativeStickAttachments` stores the dimension/root key, planting UUID, species ID and normal NBT
`AgriGenome` with six existing attributes at the configured minimum. Phases and processed
dates remain exclusively in `CropGrowthManager`; sidecar does not contain a growth algorithm.
The top of the corn resolves to the same root and the same UUID. Block entity is not added.

The prototype tests dry day, deduplication of each date, native ripening and harvesting of potatoes
(6 watered days), broccoli (8 + repeat 4), corn (14 + repeat 4) and cauliflower (12).
After each day, the blocks of both parts are compared, phase, progress, regrowth and processed
date of regular boarding and sidecar boarding. Native automation performs the collection; test does not implement
own yield calculation. Quantity, quality and experience are not yet comparable here.

The native cauliflower giant is also tested at the registered Starfield farm:
All nine roots have a sidecar, but only the leading root is exposed as mature. Selected
the date on which the regular deterministic function wins the 1% roll; the chance does not change.
The native scheduler creates a real giant and removes nine roots from the schedule.
Sidecar no longer recognizes them as active plantings. Automatic deletion of records and return
crop sticks in such a transaction apply to subsequent iterations.

The tests are carried out in the present `ServerLevel` with the Starfield key and measurement type.
`GameTestServer` selects only preset `minecraft:flat` and ignores individual dimension entries;
therefore the test profile generates its own flat preset with an additional dimension.
This is creating a true test dimension, not replacing the key of an existing world. He has
own flat area; Starfield resources/map are not copied to our preset.

Remaining AgriCraft expansion points:

- `AgriApi.getCrop` must resolve external root through neutral provider. Now he
  only sees block entity. You cannot substitute a dummy `CropBlockEntity` for the sake of tools.
- Crop sticks display and client sidecar synchronization are performed separately from the model
  plants. Empty/double crop sticks can remain regular AgriCraft blocks for now.
- Landing, deleting, carrying and collecting must explicitly update the UUID and sidecar. View check
  when read, it does not in itself distinguish between removal and replanting of the same species in the same place.
- External `AgriCrop` should delegate collection to native API and disable growth promotion
  via regular random tick/bonemeal/setGrowthStage. Currently, such a representation is not yet connected.
- The only current call `asBlockEntity()` to `MinecraftPlantModifiers` should remain
  limited to its vanilla plants before the outer `AgriCrop` becomes available.

Subclasses of native crop blocks with block entities are not selected: they retain the ban on giants.
The common executor for someone else's block and the resolution of its block entity are no longer
preconditions of the chosen scheme. Narrow hook for transferring seed components
The device remains needed for iteration 12. For gene effects in iterations 8–9 are still required
fix bottlenecks in changes in native parameters; baseline does not prove these effects.

## State and gene contract for the next prototype

Landing identity: dimension + root + species ID + specific planting identifier.
Phases/days/regrowth belong to one executor; the visual stage is its projection.
Minimally saved: schema version, genome, identity, phase, day/regrowth progress,
Barren Soil Remaining, Last Tilled Date and Season Delay Data.
When you transfer a view, the entire image is transferred, without re-seating or resetting the date.

The Baseline for each of the six existing attributes is its configured minimum.
Normalized value: `n = clamp((g - min) / (max - min), 0, 1)`;
at `max <= min` the value is `n = 0`. The old or normal seed gets a stable baseline,
rather than a new random genome on every read. The boundaries are taken from `StatsConfig`, without new 20/30.

| min/max settings | Gene | n | Interpretation |
|---|---|---|---|
| 1/10 | 1 | 0 | Native behavior |
| 1/10 | 10 | 1 | Maximum future bonus |
| 4/8 | 4 | 0 | Native behavior with custom limits |
| 4/8 | 6 | 0.5 | Half of the future bonus |
| 7/7 | 7 | 0 | The sign is frozen; without division by zero |

The coefficients of growth, number, vigor and seasonal delay are not implemented here.
Before iterations 8–9, separate balance tables and a validated impact method will be required
to the general executor. For baseline, all these corrections are zero; the gain gene does not change quality.

## Reproduction and proof limit

```powershell
$env:JAVA_HOME = 'C:\Vivoderin\Mod\jdk-21.0.10'
python tests/starfield/prepare.py
python tests/starfield/catalog.py --offline
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype --console=plain
# Full save and read in a new JVM, using the same test world:
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldRestartPhase=write --console=plain
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldRestartPhase=read --console=plain
```

`prepare.py --offline` re-checks official JAR and Git blob hashes without downloading.
Preparation does not launch downloaded sources. The prototype loads only the committed
official dependence. The GameTest world is located in `neoforge/build/starfield-prototype-run`.

Diagnostic crop sticks stand on an already maintained vanilla bed: soil registration
Starfield has yet to be added to the 1 iteration. The verification does not confirm the readiness of this registration.
The first three GameTests test day/water/season transfer and deduplication; block entity serialization
and a manager with a repeat date after restoration; lack of a phase executor
adapter with only metadata. Serialization in the test is a round-trip NBT, not a complete restart of the JVM.
Seven more checks relate to immutable native blocks with sidecars: four species cycles,
recovery of two SavedData from NBT, native giant and two-stage full JVM restart.
Without `starfieldRestartPhase`, the last test skips its job; to prove restart
Both sequential launches of `write`/`read` are required. The first one leaves the planting in the world and
expected snapshot `build/starfield-iteration0/restart-witness.nbt`; the server saves chunks and
SavedData during normal stop. The second one loads them from disk, compares the block/UUID/genome/phase/date,
checks for no repeat day 7 and one growth per day 8, then removes the test fit.

Not tested: separate unloading/loading chunk without stopping the server, client model crop sticks,
game methods for setting them up, tools, selection, irrigation, gene effects above baseline,
quality/experience/quantity of harvest, other types and full sidecar cleaning for all replacements.
All these restrictions remain in the corresponding subsequent iterations; sidecar is not issued
for already working tools or for the completed iteration 2.

Tested on October 8 2026: all ten GameTests passed in two separate JVM runs,
`write` and `read`, including the native giant and loading the saved planting from disk.
The architectural milestone of iteration 0 has been passed; the next dependent stage is optional loading
and runtime-catalog iteration 1. Catalog is reproduced and contains 50 entries
(37 species with two parts, five giant products).
Regular `:neoforge:build` and `:fabric:build` without `starfieldPrototype` completed successfully.
This is a build test, not a proof of all loading modes from iteration 1.
Checking archives - `tests/starfield/verify_release.py`: NeoForge/Fabric JAR releases and
NeoForge shadow JARs do not contain Starfield classes, prototype, its structure, test preset or direct
links to Starfield in bytecode. This is also tested after assembly with the prototype enabled.
Current run logs:
`build/starfield-iteration0/native-probe-run1.log`, `native-restart-write-final.log`,
`native-restart-read-final.log`, `native-profile-archives.log` and `native-normal-build.log`
in the same catalog.
