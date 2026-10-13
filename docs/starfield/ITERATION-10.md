# Iteration 10 - multipart and special crops

Date: 8 October 2026. Base: official Starfield Pastoral `0.6.3fix1`, commit `df2151c66a2a9c02855e4db23108e57c422ce78e`, Minecraft 1.21.1 / NeoForge 21.1.226. Continued [iteration 9](ITERATION-9.md).

## Representation and geometry

All 50 planting species continue to use real Starfield blocks. 37 species have native tops, including espalier grapes, hops, and green beans. Crop sticks are stored in the sidecar; setting their material does not replace the block, its properties, collision/selection shape or native model. The separate AgriCraft-crop sticks model is not yet connected.

The original runtime identifies the root, upper part and maturity. An action on the upper part changes the same planting and UUID. The native scheduler grows one root, synchronizes the top part and does not add a second day from the call to the carrier. Manual or scythe harvesting from any part produces one harvest, saves farming experience, clears both parts during a single harvest, or installs one regrow counter. Clicking the other part again does not produce another harvest.

Lack of space above the root keeps the seed and block obstruction. Destruction of the top or loss of soil removes the bottom, genome, and native registration. The transfer preserves both parts and all their properties, including color, seasonal appearance and additional properties of the rice. Existing placement/issue rollbacks remain.

Rice and taro use the initial search for water sources with a radius of 3, auto-watering of native soil and an additional acceleration of 25%. Water outside the radius does not replace irrigation; removing the source stops dry growth. The growth gene is applied to the already calculated native accelerated schedule. Fiber continues to grow without external watering, retains its seasonal appearance, scythe requirement, original composition of main products and normal quality. His main crop receives the total gain bonus once.

## Cactus restrictions

The native cactus block itself tests soil and all-season, and the room/greenhouse/island constraint is in `CactusSeedItem.isPlantingAllowed`. Now select and trowel call this source method on the target position. Inappropriate transfer leaves the plant on the instrument; retrying in a permitted greenhouse saves its UUID, genome and crop sticks material. The selection also preserves normal seasonal/soil conditions and available space.

Transfer of other existing crops still follows native `CropPlanting.place`; a new season seed check is not added to it. The historical day and resistance delay period are not reset by the transfer.

## Wild seeds and mature forage

The native `WildSeedCropBlock` selects production and replaces the mature plant with `ForageBlock`. This choice, its probability and appearance are not replaced by the AgriCraft algorithm. After successful conversion, the sidecar retains the original wild seed appearance, both alleles, UUID, seed template with components and crop sticks material. The additional field `form` contains the ID of the actually selected forage. Genome refers to the original seed species, not a randomly selected product.

The `form` field is optional in the SavedData 1 schema: old regular records are read as `form = type`, their encoding remains the same. Client messages and report pass this field through the existing NBT. Validation of the actual block uses the current form. Only four species of seasonal wild seeds can have this record; unsupported structural variants are stored as locked rows. Valid forage forms are checked by calling the original picker with independent test sources to list its four possible branches; the list of product IDs is not copied to the executor and world random numbers are not consumed.

When converted, the original `CropGrowthManager` removes the registration. A mature forage does not receive a new daily entry, phase, or additional growth executor. Magnifying glass, Jade and The One Probe show the original species/genome, actual mature form and readiness for harvest. The trowel transfers mature forage without creating a fictitious growth counter. Native seasonal random tick and loss of support removes the sidecar along with the block. Genetic delay refers to the growing wild crop; Once transformed, forage's own seasonal rules apply.

A mature forage with preserved planting can be a parent on adjacent double crop sticks. The stock AgriCraft engine uses the original genome, fertility and mutativity; the child gets the original wild-seed species, the new UUID, and the initial native progress. The mature form of the parent does not change. A naturally appearing forage without such a record does not receive a baseline-genome and does not become a parent.

## Collection and rollback of forage

Right clicking on the grown forage executes the original `pickForage`: Starfield generates items, quality by gathering skill, Botanist/Gatherer effects and **gathering** experience, not farming. The main formed stacks receive the general gain gene, without additional quality/profession rolls and without multiplying experience. The issuance is delayed until the block is deleted via an existing transaction.

Failure to create an item restores the actual forage, original fit and crop sticks material, removes partially published items and preserves XP. Repeated clicks do not display products again. Grown forage checks the player's permissions; AgriCraft tools do not fall into random collection. Regular world forage retains the native path without genome assignment and bonus. Breaking a block still uses its separate native quality/production/experience path and does not receive a right-click genetic transaction.

## Checks

The official JAR is verified by SHA-256, 225 local primary sources - by Git blob. Additionally read the original forage, cactus location rules, greenhouse and logical locations. Starfield has not been changed.

| Check | Result |
|---|---|
| Full enabled profile | Passed all 90 mandatory GameTest |
| Separate JVM write | Passed all 49 mandatory GameTest |
| Separate JVM read | Passed all 49 mandatory GameTest |
| Starfield installed, integration disabled | 1 mandatory GameTest passed |
| Starfield is missing from NeoForge | 1 mandatory GameTest passed |
| Fabric without Starfield | Passed both mandatory GameTest |
| Profile archives | Isolation of both NeoForge JARs confirmed |
| Regular builds | NeoForge/Fabric are successful; isolation of all five archives confirmed |

Twelve 10 iteration scenarios cover the full cycle of all 37 multi-part species, identical native model states, bottom/top collection and experience, prohibition of second collection, regrow, all crop sticks materials, obstacle over all tall seeds, partial destruction and loss of support; radius/removal/return of water of rice and taro, dry fiber and scythe harvesting; permitted/prohibited planting and transfer of cactus; all four seasonal wild transitions, forage quality/experience/random sequence, save/scan, gain/rollback/rights/tools, world forage, transfer and seasonal removal, selection from mature forage, disabled integration and real restart.

The previous test for cloning all 50 species now sets a small logical indoor section through the native location table only for the duration of the test. This permits the cactus by its present rule; After the test, the original table is restored. A separate scenario confirms an outdoor failure. The fiber check compares its core products with a common gain multiplier while maintaining normal quality.

```powershell
python tests/starfield/prepare.py --offline
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests -PstarfieldHarvestTests -PstarfieldToolTests -PstarfieldIrrigationTests -PstarfieldBreedingTests -PstarfieldSpecialTests --console=plain
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests -PstarfieldSpecialTests -PstarfieldRestartPhase=write --console=plain
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests -PstarfieldSpecialTests -PstarfieldRestartPhase=read --console=plain
```

Logs: ignored `build/starfield-iteration0/iteration10-*.log`. Witness `iteration10-restart-witness.nbt` is used for comparison purposes only. The first JVM leaves the real mature forage blocks and sidecars, the server saves them by stopping normally. The second reads real world files and checks the saved form/UUID/genome/crop sticks, the absence of the old crop counter, the prohibition of repeated transformation and a successful one-time collection. Without `write/read` this script does not prove a restart.

## Stage boundaries

Native models of crops and forage are preserved; The graphical client has not been manually verified. Checks compare native block states and synchronization of parts on the server. Game staging/additional display of sidecar-crop sticks remains a follow-up task.

Full processing of giants is the next step 11. Seed maker, genome transfer into products and automatic collection - stage 12; these paths do not attribute the player's rights or experience to the automation. Common/Fabric do not receive dependencies from Starfield, new optional mixins use only proven targets/selectors lines. AgriCraft version has not been upgraded and publication has not been completed.
