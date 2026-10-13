# Iteration 6: AgriCraft irrigation as daily watering

Date: 8 October 2026. Target version: Minecraft 1.21.1, AgriCraft 4.0.19,
NeoForge, official Starfield Pastoral `0.6.3fix1`, commit
`df2151c66a2a9c02855e4db23108e57c422ce78e`.

## Result

Sprinkler AgriCraft moisturizes real Starfield soil through standard
channels and water buffer. It passes through both parts of high crop,
but does not cause `randomTick`, bone meal or daily allowance for the native plant
height Starfield itself reads the humidity at the end of the day, consumes water,
applies retention fertilizer and counts initial/regrowth.

The behavior is the same for a regular planting and a native block with sidecar metadata
genome/crop sticks. Game placement and crop sticks model have not yet been implemented;
This is not a full integration release. New gene coefficients, selection and automation
are not included in this stage. The release version has not changed and publication has not occurred.

## How the irrigation is connected

An independent extension point `ExternalIrrigationSupport` has been added to common.
Without a provider, the same processing of conventional soil and plants continues.
NeoForge connects `NativeStarfieldIrrigation` after checking the settings,
availability of the mod and the exact supported version. When integration is inactive
the provider does not process positions. Common/Fabric do not bind to Starfield JVM types.
Bypassing a crop only applies to its verified ID from the catalog. Excluded
if reload is incorrect, the view is not processed; support after data correction
restored along with catalog. This is verified by real datapack reload.

The extension point is called **after** the usual water buffer check and payment
current column. It does not dispense water, does not expand the work area and does not bypass
channels or valves. On native crop, the traversal continues downwards without random
teak. Once a suitable native soil has been reached, its usual soil is established.
`FarmBlock.MOISTURE = 7`, after which the column ends.

Saved work area 7×7, height up to five blocks below the sprinkler,
processing one column per working tick and AgriCraft obstacles. Solid block
blocks the path to the soil. There is no transfer of water directly to the root through the obstacle:
for a high crop, the upper part, the root and its
soil. In this version all 50 native species have one soil position
`root.below()`, including two-part crops; separate watering of the upper block
does not create an additional daily step.

An unloaded neighboring chunk is no longer loaded by sprinkler traversal.
Such a column is paid for as a regular part of the passage, but does not irrigate the remote part.
territory. There is no global search for plants or a new scheduler.

## Limitations of native soil

The rules are taken from `TerrainSoils.cropSupport` and `TerrainSoils.blocksSprinklers`
verified version. Comparison with real `SprinklerBlock.waterNow` Starfield
confirms the following results:

| Soil | Sprinkler AgriCraft | Native sprinkler |
|---|---|---|
| Regular native bed | Waters | Waters |
| Sandy native bed | Disclaimer | Disclaimer |
| Barren native bed | Disclaimer | Disclaimer |
| Native garden pot | Waters | Waters |

The rejected soil then does not go to the general processor `FarmBlock`, which
I would have gotten around this ban earlier. The Starfield handheld watering can can still water
sandy and infertile bed: integration does not change its behavior.
The soil is not replaced with vanilla, the fertilizer is not removed, the genome and UUID do not change.

## Consumption, repeat passes and saving

The existing AgriCraft consumption formula has been retained: with a positive value
settings per working tick is written off `max(1, consumption / 20)` mB.
Empty, blocked or inappropriate columns are also paid for. In the test setting
40 mB/sec gives 2 mB per column and 98 mB for a full pass of 49 columns.
Without water in the buffer, a positive flow rate prevents watering from starting or continuing;
a closed valve does not replenish the buffer; a separate, unconnected tank does not supply it.

The flow setting `0` retains the previous free mode of AgriCraft itself.
The integration does not introduce a new free water supply or change the meaning of this setting.
Closing the valve after an already paid water supply does not remove the remainder from the buffer:
The sprinkler can use it until it runs out.

A few sprinklers and repeated passes only maintain the moisture.
They do not increase `dayInPhase`, do not shorten the reharvest countdown,
do not reset the date of the last daily step. Re-completion of the same day
maintains native protection against double growth.

Fixed saving of the sprinkler itself: changing the water supply or position
bypass calls `setChanged()`. Previously, with continuous activity without change
block flag `active` consumption may not have marked the chunk for saving. NBT format
hasn't changed. After loading, the saved column and the remaining stock continue,
not a free new pass. The check marks the chunk as unsaved precisely when
continuous operation, then restores the real block entity from its NBT
and checks for water depletion.

## Checks

Nine new GameTest tests:

- Real use of native watering can with payment for water, native
  `SprinklerBlock.waterNow` and paid AgriCraft passes. Potatoes on regular
  planted and with sidecar-crop sticks ripens equally on the sixth suitable day.
- Empty system, closed valve, unconnected tank, real water exchange
  between the connected tank and the channel and maintaining the overall water balance.
- Radius/height boundaries, solid obstruction and soil access through both parts of the corn.
- The coincidence of the prohibitions of native soils and the support of a pot with a native sprinkler;
  maintaining the possibility of manual watering of soils prohibited for automatic watering.
- Repeated and intersecting passes with maximum probability of random
  AgriCraft acceleration, single daily step and native four-day re-harvest of corn.
- True global `growDaily`, normal soil drying out, water retention
  Deluxe Retaining Soil and the next day without additional watering.
- Saving a partial paid pass and buffer, marking a chunk for saving,
  restoration and stop strictly after the balance is exhausted.
- Watering all 50 native species without changing genome, UUID, block or progress.
- Border the loaded chunk and maintain watering with regular vanilla soil.

A separate Fabric server checks for the absence of a native implementation and the real
regular sprinkler: empty buffer, flow rate, vanilla soil moisture and
preserved random wheat growth ticks. Shared extension point does not disable
previous acceleration of conventional crops.

```powershell
$env:JAVA_HOME = 'C:\Vivoderin\Mod\jdk-21.0.10'
python tests/starfield/prepare.py --offline
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests -PstarfieldHarvestTests -PstarfieldToolTests -PstarfieldIrrigationTests --console=plain
.\gradlew.bat :neoforge:jar :neoforge:shadowJar :neoforge:remapJar -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests -PstarfieldHarvestTests -PstarfieldToolTests -PstarfieldIrrigationTests --console=plain
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=disabled --console=plain
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=absent --console=plain
.\gradlew.bat :fabric:runStarfieldLoading -PstarfieldFabricSmoke --console=plain
.\gradlew.bat :neoforge:build :fabric:build --console=plain
```

Provisioning checks SHA-256 of the official JAR and 218 of the official Git blobs;
For this stage, the sources of the watering can, sprinkler, its manager and pot have been added.
Source files are not included in the product; Someone else's daily growth algorithm is not copied.

All 47 joint profile tests passed: 38 previous ones and nine new ones.
The two previous restart-checkpoint tests without a restart phase do not check restarts.
Water buffer recovery here verified via NBT block entity and mark
chunk for writing, rather than through a separate JVM restart. Previous proof
a complete restart of plants remains in [iteration 3](ITERATION-3.md).
Logs - `build/starfield-iteration0/iteration6-*.log`; first unsuccessful run
with two corrected errors in test expectations, saved separately as
`iteration6-enabled-attempt1.log`.

Some NeoForge servers with integration disabled and without Starfield passed
one test at a time. Fabric passed two tests: safe loading and previous
irrigation/growth of vanilla wheat. Regular NeoForge/Fabric builds are successful.
Verified release/shadow NeoForge after enabled test profile and five
regular build archives: release/shadow NeoForge, release Fabric and both
transformProduction common. There are no test directories left in the normal output directories
classes, structure, catalog or world preset.

The graphical game client did not start at this stage.
Giant shapes and arbitrary third-party multi-position
adapters have not yet been declared as supported: native one- and two-part ones have been tested
plants. A complete check of special forms refers to iteration 13.

The next stage is iteration 7: regular crossbreeding and inheritance.
