# Iteration 5: analyzer, magnifying glass and tools

Date: 8 October 2026. Target version: Minecraft 1.21.1, AgriCraft 4.0.19,
NeoForge, official Starfield Pastoral `0.6.3fix1`, commit
`df2151c66a2a9c02855e4db23108e57c422ce78e`.

## Result

The analyzer accepts real seeds of all 50 supported Starfield species
and shows the existing six AgriCraft genes with both alleles. Magnifying glass, Jade
and The One Probe use the same server report and the same prompt format:
native phase, conditional number of remaining days, maturity, regrowth, water,
season and genome. Reading does not trigger growth or harvest.

The trowel transfers the plant with preservation of the genome and full native progress.
The remaining tools clearly refuse to perform actions that are not yet possible
execute without losing data or bypassing Starfield rules.

The native block and `CropGrowthManager` remain the only growth executor.
Game placement and display of crop sticks, selection, irrigation and gene effects
above basic values have not yet been implemented. The release version has not changed;
publication was not carried out.

## Seeds and analyzer

An independent extension point `ExternalSeedSupport` has been added to common.
The Starfield provider connects to NeoForge only after checking the settings,
availability of the mod and the fixed version. Common/Fabric don't link to native ones
Starfield JVM types.

The analyzer preserves the original native item. A copy of the seed gets verified
genome envelope; quantity, both alleles, other components and foreign custom data
are saved. The original passed stack is not modified by the prepare operation.
The native name of a crop is taken from the translation of its present block.
Invalid or future genome format is rejected.

Supports normal slot operation, stack transfer via Shift and insertion of one
seed via Shift with the item in hand. When the slot is occupied, incompatible components
or a full inventory, there is no loss or duplication of items. Native seeds
are not added as unknown research entries to the AgriCraft log.

Converting regular registered seeds to `AgriSeedItem` keeps the same
path; it has been tested separately on wheat. Converting Starfield to virtual
`AgriSeedItem` and back **not supported**: such a carrier would now crash
regular `AgriCrop` and replaced the native executor. Analysis of native seeds costs
without such conversion and retains the possibility of their usual Starfield planting.

The server makes the changes. The menu checks the distance is less than eight blocks,
coincidence of dimensions, existence of the original block entity and native rights
farms. Fast transfer requires the player to keep this menu open.
Direct block interactions also check construction and interaction rights;
The client does not extract the seeds or log himself. Opening and pasting via
NeoForge interception additionally uses the real interaction range.

## What does the remaining time mean?

The report reads the real phase and `dayInPhase`, not the visual phase `AGE`.
For the first maturation, the current native schedule with fertilizer is used;
for reripening, `dayInPhase` is treated as the remaining countdown.
Fractional progress of infertile soil is taken into account by calling the native
`advanceOnSoil` on a separate temporary state.

In the fixed version of the calculation of the total acceleration and fertilizer schedule
closed. The protected NeoForge class is called through saved reflection-accessors
native `getTotalSpeedBoost` and `applySpeedGroToPhaseDays`; algorithms are not copied.
This is why support is limited to the tested version. Reading error gives
missing diagnostic report rather than independent height calculation.

The tooltip says "N watering days left per season." This is a conditional quantity
daily steps with current soil and fertilizer, rather than a promise of a calendar date:
Dry days, changing seasons and future changes in fertilizer will change the actual timing.
For an old state without native phases, the term is marked unknown. Inspection
does not migrate it and does not guess the percentages across eight visual stages.

In the basic test case the potatoes show six days, with Speed-Gro -
four, on infertile soil - nine. Tests verify the displayed balance
with true daily ripening. In corn, regrowth shows four
days immediately after collection and three after one suitable daily step.

The usual prompt contains name, phase or maturity, remainder, current moisture content
and season. Extended adds valid seasons, the fact of watering in the last
treated day, fertilizer and both characteristics of each of the six gene pairs.
Current season tolerance takes into account native planting site exclusion;
this is not a forecast of a farm's grace period or end-of-season experience.

## General tips and network

`NativeStarfieldInspection` creates a server report, `StarfieldInspection`
formats it for Loupe, Jade and The One Probe. Both parts of high crop
refer to one root and one planting UUID. Jade only requests data
for the recognized native plant, and its server provider additionally
verified by direct call in GameTest. There are no new required dependencies.

The lens request contains only the dimension, position and UUID. Server checks for live
player, loaded chunk, real interaction range, presence of a magnifying glass in hand
or a standard sign of a magnifying glass in the head object and the current UUID. Gene transfer
or there is no progress from the client for recording. Requests are limited to one
for ten server ticks; the client only questions the plant at gunpoint.
The remote request does not load the chunk.

The response is accepted only for the current connection, world, dimension, server
session and famous planting. The UUID/boarding data is verified before display,
condition of the block and soil. The report cache lives for a maximum of 20 game ticks;
changing a world, dimension, session, planting or unloading a chunk clears it.
While there is no current report, the magnifying glass shows waiting for server data.

## Applicability of tools

| Means | Behavior on Starfield crop |
|---|---|
| Magnifying glass | Reads the server report; does not harvest and does not change progress. |
| Trowel | Saves the root, both parts, genome, UUID and full native progress; checks the range and rights of the farm when transferring. |
| Rake | Disclaimer: Native plant does not provide AgriCraft weed support. |
| Scissors | Disclaimer: Free cloning or stage resetting would bypass native collection deadlines and rules. |
| Seed sac | Failure of native storage/planting: current bag stores genome and quantity, restoring `AgriSeedItem`; this would lose the Starfield components and land a different type of block. |
| Crop sticks | Failure during normal use on native plant; the presence of crop sticks is currently only sidecar metadata, game placement and model in front. |

Failure shows a short localized hint and does not go to random
manual collection. If rejected, the plant, genome, and instrument item are retained.
Server checks also apply when calling an operation directly, and not only through
client click prediction. There are no gene editors or new traits.

## Tests and replays

Nine new GameTest tests:

- Analysis of all 50 species, both alleles and foreign components; immutativity of the source,
  extraction of native seeds and former conversion of conventional wheat.
- Inserting/extracting stacks through the menu, full inventory and repeated empty transfer.
- Damaged genome, occupied slot, rights of someone else's farm, distance,
  closed menu and replaced analyzer.
- Real terms of first ripening, accelerating fertilizer, infertile soil,
  dry day and no change in reading progress.
- High crop, regrowth countdown, old state
  no phases, six gene pairs and no percentage guess.
- Queries without a tool, with a foreign dimension/UUID, deleted position
  and the ID of the previous planting.
- Codecs, report copying and cache reset by time, soil, UUID,
  world, dimension and server session.
- Applicability of all tools, preservation of condition in case of failure,
  transfer by the owner and refusal to a stranger or distant player.
- Direct extraction via block, ban on another player, current season,
  Jade general server report and missing data after deleting a plant.

```powershell
$env:JAVA_HOME = 'C:\Vivoderin\Mod\jdk-21.0.10'
python tests/starfield/prepare.py --offline
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests -PstarfieldHarvestTests -PstarfieldToolTests --console=plain
.\gradlew.bat :neoforge:jar :neoforge:shadowJar :neoforge:remapJar -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests -PstarfieldHarvestTests -PstarfieldToolTests --console=plain
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=disabled --console=plain
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=absent --console=plain
.\gradlew.bat :fabric:runStarfieldLoading -PstarfieldFabricSmoke --console=plain
.\gradlew.bat :neoforge:build :fabric:build --console=plain
```

The joint profile contains 38 tests: 29 previous ones and nine new ones. Two old
restart-checkpoint tests without a restart phase do not check restarts; separate
the proof of world preservation remains in [iteration 3](ITERATION-3.md).
The current stage logs are `build/starfield-iteration0/iteration5-*.log`.
Archive isolation is checked by `tests/starfield/verify_release.py`: test classes
and resources are excluded even after building with the test profile enabled;
direct native links are only allowed in the listed protected implementations.

All 38 joint profile tests passed. Separate NeoForge servers
with integration turned off and without Starfield passed one test each; separate
Fabric server also passed one test. Regular NeoForge/Fabric builds are successful.
Verified release/shadow NeoForge after enabled test profile and five
regular build archives: release/shadow NeoForge, release Fabric and both
transformProduction common. There are no test directories left in the normal output directories
classes, structure, catalog or world preset.

The graphical client was not launched manually at this stage: visual layout
The analyzer and hint windows require a separate game check. The One Probe
uses general validated report and formatting, but full client session
with TOP installed has not been verified. Server states, handlers, codecs,
permissions and cache were checked on the real server and in separate tests.

The next stage is iteration 6: AgriCraft irrigation as native daily irrigation.
