# Iteration 7: regular crossbreeding and inheritance

Date: 8 October 2026. Minecraft 1.21.1, AgriCraft 4.0.19, NeoForge,
official Starfield Pastoral `0.6.3fix1`, commit
`df2151c66a2a9c02855e4db23108e57c422ce78e`.

## Behavior

Empty double crop sticks AgriCraft get Starfield native descendant from
ordinary tick selection. Parents can grow in ordinary native farmland
or have sidecar-crop sticks. Four horizontal adjacent ones are taken into account
roots at the height of crop sticks. The top of high crop does not become
a separate parent. The adjacent chunk is not loaded to search for parents;
the native parent from the other farm is excluded.

Staff `AgriCrossBreedEngine` performs selection, fertility probability,
sorting, cloning, allele selection and mutation of characteristics. Used
active selector/clone/combine rules and existing settings:
`onlyFertileCropsSpread`, `mutationChanceMultiplier`, `cloneMutations`.
For native single parent, standard probability is applied
spread from the base AgriPlant - 10% after successful selection.
There are no new traits, mutativity formulas, or trait bounds.

Native species are recognized by checking existing mutation recipes,
Therefore, the standard complexity of the species and the dominance of the species allele work.
The difficulty cache is reset when the catalog is updated and the server is shut down.
The validity for selection is separate from the normal validity of GUI recipes:
JEI does not receive native species as missing virtual ingredients.
By default, plants of the same species are bred. Different types do not give
arbitrary mixture: the expressed appearance of the native descendant must correspond
an explicit mutation recipe for those parents. Ready-made new interspecies recipes into the product
not added; The test recipe only exists in the test profile.

Native readiness is read without phase change. In the committed version
public native maturity check can fix outdated phases
on the spot; it is therefore not called out of selection. The native trait is checked
mature stage and no pending regrowth countdown.
This is a test of readiness, not an executor of calendar phases. Terms
parent's suitability is taken from native `canPlantAt`, including season and
place exceptions. The usual setting for selecting only fertile plants is retained.

## Descendant and failure

The descendant replaces the empty double crop sticks with a real block of its kind
Starfield. Native `onPlace` creates its own initial progress and
both parts of high crop. The crop sticks material and both alleles are preserved
each of the six genes; a new UUID and a real native seed are created
with genome. Parental stages, dates, water, fertilizer and foreign ingredients
seeds are not copied. Existing water and soil fertilizer of the descendant itself
remain on its own terms.

For a native descendant, the Starfield dimension, suitable soil,
season, block survival, available space and SavedData writability.
Crop sticks submerged in water or lava are not replaced by the native descendant.
The occupied upper block of high crop is preserved. Single crop sticks,
an already planted or deleted block entity is not the target of selection.
Weed treatment and permitted selection from fertilizer continue
according to the existing rules of empty crop sticks.

Canceled or throwing placement handlers returns a block of doubles
crop sticks, its data and top position; native phases and rejected sidecar
descendants are removed by the native life cycle. Next try may
successfully plant a plant. Ordinary plants without native parents go
following the same AgriCraft path.

## Stage limits

Graphic display of crop sticks on top of the native plant and manual
Planting seeds in crop sticks belongs to the next stage of the presentation.
Placement of empty crop sticks on suitable native soil is allowed here
and their usual doubling; After selection, the material is stored in the sidecar.
Graphical gameplay verification was not performed.

The effects of growth, gain, strength and resistance are not yet implemented:
this is clearly indicated in the extended tooltip of magnifying glass/Jade/TOP in Russian
and English. Fertility and mutativity are already at work in selection.
The release version has not changed and publication has not occurred.

## Tests and replays

Ten new GameTests check:

- Real tick of empty double crop sticks, new UUID, unchanged parent state,
  dry offspring and its subsequent six-day native maturation.
- Cloning from a single parent with both alleles
  and disabled clone mutations.
- Zero fertility multiplier, minimum and maximum fertility.
- Change of inheritance with minimum/maximum mutativity
  and respecting the existing boundaries of all six characteristics.
- Exclusion of immature parents and the upper parts of high crop.
- Refusal to mix types without a prescription and the native result of an explicit
  recipe from an existing mutation registry.
- Season, soil, single crop sticks, obstacle over target
  and a single UUID of both parts of the native child.
- Cancel and exclude placement with full rollback of crop sticks and state.
- Cloning of all 50 species, native descendant seed and restoration
  its UUID/genome/material from SavedData NBT.
- Disabled integration and preservation of regular wheat crossbreeding.

```powershell
$env:JAVA_HOME = 'C:\Vivoderin\Mod\jdk-21.0.10'
python tests/starfield/prepare.py --offline
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests -PstarfieldHarvestTests -PstarfieldToolTests -PstarfieldIrrigationTests -PstarfieldBreedingTests --console=plain
.\gradlew.bat :neoforge:jar :neoforge:shadowJar :neoforge:remapJar -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests -PstarfieldHarvestTests -PstarfieldToolTests -PstarfieldIrrigationTests -PstarfieldBreedingTests --console=plain
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=disabled --console=plain
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=absent --console=plain
.\gradlew.bat :fabric:runStarfieldLoading -PstarfieldFabricSmoke --console=plain
.\gradlew.bat :neoforge:build :fabric:build --console=plain
```

Provisioning checks the official JAR and 219 source Git blobs. Test
recipe, classes and resources are excluded from archives, including the assembly immediately
after the test profile. This further checks
`tests/starfield/verify_release.py`.

All 57 joint profile tests passed on the final code, including
checking native dominance and separation of GUI recipes. Separate
NeoForge servers with disabled integration and without Starfield passed
one test at a time; Fabric passed both loading and previous irrigation tests.
Regular NeoForge/Fabric builds are successful. Verified release/shadow NeoForge
after the profile and five regular archives: release/shadow NeoForge, release
Fabric and both transformProduction common. In normal output directories
there are no test classes or test recipe left.

Logs - `build/starfield-iteration0/iteration7-enabled.log`,
`iteration7-disabled.log`, `iteration7-absent.log`, `iteration7-fabric.log`,
`iteration7-profile-archives.log` and `iteration7-normal-build.log`.
The first unsuccessful joint run and the diagnostic run are saved
like `iteration7-enabled-attempt1.log` and `iteration7-breeding-diagnostic.log`:
they identified side phase correction by native maturity check
and the absence of native species in complexity calculations. Both reasons have been corrected.
Interim successful run - `iteration7-enabled-attempt2.log`.

Descendant recovery here is verified via the real SavedData NBT.
rather than a separate JVM restart. Old restart-checkpoint tests without
restart phases are not new evidence of a restart;
checking two separate JVMs remains in iteration 3.

The next stage is iteration 8: growth and gain genes.
