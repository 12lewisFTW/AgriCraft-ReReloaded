# Iteration 4: harvesting, quality and re-fruiting

Date: 8 October 2026. Official Starfield Pastoral `0.6.3fix1`,
commit `df2151c66a2a9c02855e4db23108e57c422ce78e`.

## Implemented behavior

The usual collection by hand and native scythe is performed by the original `StardewCropBlock.harvest`.
Starfield determines farm level, fertilizer, quality, quantity, additional
items, experience, disposable crop removal and regrowth. The integration neither copies
these formulas nor adds gene bonuses at this milestone. Public player runtime and real scythe swing
maintain rights checks on the farm. In the tested version, the scythe has no durability;
the integration does not add wear or tear to the item.

For a supported crop, the operation receives a narrow server boundary:

1. The bottom/top blocks, all native progress fields, UUID, genome and crop sticks are saved.
2. The native algorithm calculates real `ItemEntity` with its components,
   coordinates and speed, but their publication and experience accrual are postponed.
3. After a regular change in the plant, it is checked that it is no longer ready for a second change.
   immediate harvest and no separate upper part of the removed crop remained.
4. Items are transferred to the original `ServerLevel.addFreshEntity`, including real ones
   NeoForge events and their cancellation. Then the initial accrual of experience is performed.
5. If refused or excluded, all items of this operation are deleted, including those already
   published, and the plant is restored with full progress and identity.
   If an error occurs when accruing experience, the saved server data are restored.
   player data. External irreversible actions of other people's event listeners are not rolled back.

The failure result is returned as `false` for native hand/tool methods and `FAIL` for
right click. The nested collection from the synchronous item creation listener is rejected;
Once the transaction is completed, the next normal collection is permitted. The border only exists
for the duration of the call, is not saved in the world and does not create a second scheduler.

The full inventory retains native behavior: crops appear as items in the world,
the plant enters the state after harvesting, experience is awarded once. Click again
does not create products or experiences. Broccoli and tall corn use their four
`regrowDays`; harvesting the top of the corn refers to the same root.

Scissors, rake, magnifying glass, crop sticks and seed bag no longer trigger normal manual
collection when clicking on native crop. Their special actions will be connected to
iterations 5. The trowel is already running through the iteration transfer handler 2; if he
refuses, the interaction also does not turn into harvesting. Successful transfer
still saves both blocks, genome and full progress.

## Boundaries

- Crop sticks are still production metadata SavedData. Accommodation
  player and the visual model of crop sticks on the native block has not yet been implemented.
- The new border is applied to the normal player harvest of a supported species when
  enabled integration. Integration disabled and types excluded from the catalog
  perform the original operation. The version is checked before native mixins are applied.
- Player block destruction (`playerWillDestroy`) remains a separate native path.
  A new rollback is not announced for destruction or other people's automation. Junimo/Create and
  AgriCraft automatic collection is addressed separately in iteration 12.
- Genome is still preserved on planting/seeds. Transfer of genome to production and seed
  the apparatus, the gain gene and the genetic acceleration of regrowth are still to come.
- If failure occurs, the state of the plant returns; global random generator
  world numbers and already played sound are not rewound.
- Tests are performed on a real server without a graphical client. Appearance
  crop sticks and client messages were not manually checked at this stage.

## Tests and replays

Eight GameTests have been added to `tests/starfield/harvest-java`. They check:

- Match the product stack, including quality and quantity, with the disabled one
  integration under the same random state; two levels of farming and
  no fertilizer/three qualities of fertilizer. Potatoes provide initial 14 XP.
- One-time collection and no repeated items/experience.
- Re-ripening of broccoli/corn exactly on the fourth day, preservation
  identity, both parts and collection through the top.
- Mandatory scythe for wheat, real `harvestSwing`, refusal to someone else's player
  for manual and instrumental collection and the initial durability/consumption of the scythe.
- Refusal of random collection by all listed AgriCraft tools.
- Full inventory and native items in the world.
- Cancellation of the creation of the first and second item, exception on the second item,
  a complete rollback and the only successful re-collection.
- Rollback of a high re-fruiting crop and refusal of the invested harvest.

Remote farms are outside of GameTest's entity tracking scope. Stand records
real item creation events and checks for live ones `ItemEntity`: otherwise normal
searching in an untracked chunk gives an empty list even if added successfully.
The control crop must additionally be non-empty. Test listeners and subjects
are cleared after each script and are not included in the release archives.

```powershell
$env:JAVA_HOME = 'C:\Vivoderin\Mod\jdk-21.0.10'
python tests/starfield/prepare.py --offline
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests -PstarfieldHarvestTests --console=plain
.\gradlew.bat :neoforge:jar :neoforge:shadowJar :neoforge:remapJar -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests -PstarfieldHarvestTests --console=plain
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=disabled --console=plain
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=absent --console=plain
.\gradlew.bat :fabric:runStarfieldLoading -PstarfieldFabricSmoke --console=plain
.\gradlew.bat :neoforge:build :fabric:build --console=plain
```

The joint enabled profile contains 29 tests: 21 previous and eight new ones.
The two previous restart-checkpoint tests without a restart phase do not perform checks
restart; this is not new evidence of a restart. Write/read runs and saving
worlds are described in [iteration 3](ITERATION-3.md). The logs for this stage are in
`build/starfield-iteration0/iteration4-*.log`. Archives are checked by `tests/starfield/verify_release.py`:
direct native references are only allowed in explicitly listed protected classes,
The optional mixin only allows precise text selectors without JVM type references.

Result: all 29 joint profile tests passed; disabled and absent - one each
successful test, separate Fabric server - one successful test. NeoForge/Fabric
collected successfully. Verified release/shadow NeoForge after enabled test
profile and five archives of regular assembly. In regular NeoForge/Fabric output directories
there are no test classes, structure, catalog or world preset left. Release version
was not changed, publication was not performed.

The next stage is iteration 5: analyzer, magnifying glass and tools.
