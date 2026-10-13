# Iteration 11 - giant crops with native rules

Date: 9 October 2026. Base: official Starfield Pastoral `0.6.3fix1`, commit `df2151c66a2a9c02855e4db23108e57c422ce78e`, Minecraft 1.21.1 / NeoForge 21.1.226. Continued [iteration 10](ITERATION-10.md).

## Formation

All five original giants are supported: cauliflower, melon, pumpkin, powder melon and Qi fruit. Their definitions, footprints and height come from the actual `StardewGiantCrops`; `GiantCropGrowth` remains the sole executor. Native blocks and models, soils, the whole area 3×3, free volume without containers, loaded chunks, farm slot border, historical day and probability 1% are saved. The strength gene does not change the chance: this expansion refers to the optional stage 14.

Clarification of the original rule: **the triggering northwestern root must be mature and watered**. The remaining eight should remain whole crops with the same products; they may be immature. The native API allows different types with common output. Integration does not add a maturity requirement for all nine or rewrite the index of acceptable species. The off-season deferment constraint from iteration 9 remains.

Real crop blocks with sidecar-crop sticks already pass these conditions. The added optional mixin frames the native pass and remembers the state only after calling its checked `Handler.plan`. It does not execute the calendar, throw an extra chance, or replace the scheduling algorithm. Native daily growth records are deleted as usual after posting. Old crops without sidecar retain the native ability to form; reading assigns the same stable baseline without fictitious crop sticks.

## Return crop sticks and rollback

After successfully replacing all blocks and deleting nine native entries, regular AgriCraft items of the corresponding materials are returned before the first native notification of neighbors. One attached planting returns one set; a missing binding produces nothing. Items are grouped by material, respecting the stack size. The output does not consume random world numbers. With `doTileDrops=false`, items are not created, as with the normal block drops being disabled.

The return uses an existing issue transaction. Canceling a second entity or deleting after a partial publication removes already published items and restores the original blocks of both parts, UUIDs, both alleles, seed templates, materials and the same native progress objects. Daily dates are preserved; The `lastGiantDay` of the launching root retains the date of the attempt, so the failure cannot be used to re-throw on the same day. During daily processing, the restored roots are also returned to the native queue, including its deferred additions/deletions.

Calling again after success does not see the old roots and does not issue items again. Nesting from a publish handler is prohibited until the world has changed; the outer attempt is rolled back and the next normal attempt works. An incomplete plot, incorrect product, obstacle, container, inappropriate support, or someone else's farm slot will not receive the issue. An unknown SavedData schema and a blocked corrupted row stops the replacement until the plant is removed, saving the data for later migration.

The native rollback of a failed allocation remains the same: restoring the original object `CropGrowthState` uses the existing iteration lifecycle hook 2. Items will not be returned before this milestone. The new issuance failure scenario tests the already fully hosted giant; a separate failure injection in the middle of `setBlock` was not added.

## Collection and rule of heredity

The giant remains a real `GiantCropBlock`. In the tested version, it **does not require an axe**: destroying any cell owned by it with an empty hand or tool performs a native collection. The original range of 15–21 main product, normal quality, possible secret note, 5 units of agricultural experience and native creative/normal mode differences are retained. The entire owned footprint is deleted; repeated destruction of another cell does not produce a second harvest.

All nine plantings are consumed during formation. The giant and its products are not assigned a random parent's genome or an artificial average; the gain gene of one of the parents does not multiply the overall giant harvest. There is no return of genetic seeds in the original giant collection. Its regular products do not receive the genetic tag. Future seed processing of these products should follow the rule for genome-free products rather than restoring one of the nine parents. Processing of the seed maker and automatic collection remains stage 12.

## Checks

The official JAR and 225 primary sources are re-verified locally using SHA-256/Git blob. The original Starfield is unchanged.

| Check | Result |
|---|---|
| Full enabled profile | Passed all 104 mandatory GameTest |
| Separate JVM write | Passed all 63 mandatory GameTest |
| Separate JVM read | Passed all 63 mandatory GameTest |
| Starfield installed, integration disabled | 1 mandatory GameTest passed |
| Starfield is missing from NeoForge | 1 mandatory GameTest passed |
| Fabric without Starfield | Passed both mandatory GameTest |
| NeoForge Profile Archives | Isolation of both JARs confirmed |
| Regular builds | NeoForge/Fabric are successful; isolation of all five archives confirmed |

Fourteen new GameTests cover all five species and their full heights, nine independent fits and three materials, a sequence of original 1%-throws without/with crop sticks and no RNG consumption; incomplete mesh, invalid output, immature trigger and valid immature neighbors; obstacles/containers/support; dry and repeat date, offline pass; a real daily scheduler and a mixed plot with missing crop sticks; corrupted/future data and native slot boundary; canceled/abandoned partial issuance, restoration of all data and queue, nested formation; native manual collection of all five species, quantity/quality/experience/lack of random genome and repeated collection; legacy baseline, disabled and `doTileDrops`; a real reboot.

```powershell
$env:JAVA_HOME = 'C:\Vivoderin\Mod\jdk-21.0.10'
python tests/starfield/prepare.py --offline
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests -PstarfieldHarvestTests -PstarfieldToolTests -PstarfieldIrrigationTests -PstarfieldBreedingTests -PstarfieldSpecialTests -PstarfieldGiantTests --console=plain
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests -PstarfieldSpecialTests -PstarfieldGiantTests -PstarfieldRestartPhase=write --console=plain
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests -PstarfieldSpecialTests -PstarfieldGiantTests -PstarfieldRestartPhase=read --console=plain
```

Logs: ignored `build/starfield-iteration0/iteration11-*.log`. `iteration11-restart-witness.nbt` is a comparison witness only. The first JVM leaves a real footprint of a giant and real entities of return crop sticks; a regular stop keeps the peace. The second checks the same entities by UUID/stacks and the total number of materials, blocks/height, absence of nine old genome/scheduler records, prohibition of re-formation and one-time collection of the saved giant. In both phases, the test temporarily holds chunks of the area and waits for 40 server ticks: loading FULL blocks does not in itself mean the availability of entities. His own tickets are removed, the previously existing ones are retained. The Witness does not restore peace. Without write/read this test is no-op and does not prove restart.

## Boundaries

Game staging and a separate sidecar-crop sticks model remain a follow-up task; This stage supports already attached materials and real native models of giants. The graphical client has not been manually verified. The collection preserves the giant's original destruction path; publication failure rollback of **the most gigantic harvest** has not been added to this path. Strengthening the chance of genome strength, genome products of the giant and automatic collection are not declared.

Common/Fabric does not receive links to Starfield. The new optional mixin contains only the tested target/selectors lines; Starfield JVM types remain in the guarded NeoForge bridge. AgriCraft version has not been upgraded and the release has not been published. The next iteration is 12: seed maker and automatic collection.
