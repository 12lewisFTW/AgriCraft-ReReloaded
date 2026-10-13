# Iteration 3: single daily growth without genetic bonuses

Date: 8 October 2026. Starfield Version Verified: Official `0.6.3fix1`.
Status: Iteration 3 completed.

## Executor and stage boundary

Both views use the real `StardewCropBlock` registered in
`CropGrowthManager`. Crop sticks - production data field `StarfieldPlantData`, not the second one
block with `CropBlockEntity`. Toggling this field does not change the native phases and dates.
The only executors are full-time `growDaily` / `settleCrops` and `OfflineFarmCatchUp`.
This iteration did not require a new executor, runtime adapter, or growth hook:
the iteration architecture 0–2 already provides the required routing. Added playable
checks of this contract, a separate test profile and fixation of its boundaries.

AgriCraft stores the genome; the effects of increased characteristics are not yet applied.
The main paired representative is potato, standard base genome and the same soil.
Wooden, iron and obsidian versions of preserved crop sticks have been tested.
Game staging/removal and display of crop sticks are not yet connected. This is the daily limit
semantics of the selected representation, and not game compatibility ready for publication.
Harvesting, quality, re-fruiting, tools and irrigation remain the next steps.

## Date, water, soil and season

- At the normal end of the day, Starfield reads water from its soil and dries it after growing.
  A dry day updates the native processed date, but does not advance the phase.
- Potatoes without fertilizer ripen after six watered days. Individual `phaseDays`
  performs Starfield; AgriCraft does not count visual stages in days.
- Native ordinary, sandy and infertile beds retain their rules. On
  infertile soil the rest of the growth belongs to `CropGrowthState.infertileProgress`.
  Replacing the base with a vanilla bed removes the plant with a regular `updateShape`; record
  genome/crop sticks are also cleared. A similar name or inheritance from `FarmBlock` does not resolve the soil.
- Speed-Gro reduces time with its native algorithm; calling the same date again
  does not give an extra day. Deluxe Retaining Soil retains water after a normal daily pass.
- Historical days use the transferred season, even if the current global calendar
  is in another season. Native death out of season and farm's own benefit
  `graceDaysLeft` act the same. No new genetic resistance has been added.

## Missed days and loading

`settleCrops` does not load the missing chunk itself. In the test, the real server unloads
farmer's chunk; until its return, the phase and processed day do not change. Next is the real one
`OfflineFarmCatchUp.catchUp` gets the farm registered and initialized,
temporarily downloads the necessary chunks and transfers each missed day to the native scheduler.
Tested one day and three more days; UUID, genome and crop sticks option remain the same.
Repeating catch-ups and repeating an already processed day do not promote growth.

Important: official Starfield considers crops to be watered during offline catch-up.
This is the initial rule, the same for both plantings. Checking stop without water
refers to normal daily days; integration does not enter its own watering history.

The paired restart test leaves both plants for regular server conservation.
The second JVM reads the real world files, the native SavedData and the AgriCraft release SavedData.
Witness only contains expected values and never restores state.
Block, phase, progress, soil residue, processed date, UUID and genome are checked;
the repeat of the previous date and the next day are checked separately.

## Random ticks and boosters

Native blocks do not have random growth, and `AgriApi.getCrop` does not yet allow them as
regular `CropBlockEntity`. Two hundred apparent random ticks do not change the phase and date.
So there is no global disabling of ticks: regular double crop sticks are still
cause standard processing, and the general code of crops and weeds is not changed.

Bone meal is rejected without consuming an item or changing the calendar. In the Starfield dimension
the mod itself limits the actual interaction, including vanilla plants; checking the saving of the usual
bone meal is performed on wheat in Overworld. Native seeds do not become seeds
AgriCraft, and the native plant does not receive the general AgriCraft fertilizer application route.
Arbitrary third-party code that directly rewrites the block or `CropGrowthState`, this step
is not declared as a supported accelerator.

## Play

```powershell
$env:JAVA_HOME = 'C:\Vivoderin\Mod\jdk-21.0.10'
python tests/starfield/prepare.py --offline
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests --console=plain
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests -PstarfieldRestartPhase=write --console=plain
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests -PstarfieldRestartPhase=read --console=plain
.\gradlew.bat :neoforge:build :fabric:build --console=plain
```

Eight new GameTests are found only in `tests/starfield/daily-java` and are included
`starfieldDailyTests` in `enabled` mode. Without a genetic profile, 14 tests are run;
with him - 21 (13 previous ones and eight new ones). Without `starfieldRestartPhase` restart tests
are ignored and do not serve as proof of preservation. In a write/read pair, 20 tests are launched:
the generator excludes a separate global `growDaily` so that it does not promote the left ones
other tests control plantings before storage. `growDaily` itself is checked first,
separate launch without restart phase. This is test isolation, not growth disabling in fashion.
The two profiles have different witness files.
Regular resources, user client config and work worlds are not changed.

Logs: `build/starfield-iteration0/iteration3-enabled-daily.log`, `iteration3-enabled-write.log`,
`iteration3-enabled-read.log`, `iteration3-profile-archives.log`, `iteration3-normal-build.log`.
Checking for the absence of test materials and packaged Starfield in the release JARs -
`tests/starfield/verify_release.py`. No new references to native classes have been added to the release code.

Test result: normal launch - all 21 tests passed; separate JVM write/read - all
20 tests passed in each run. NeoForge/Fabric builds are successful. After test profile
tested NeoForge release/shadow, then five archives of the regular build, including Fabric and
both platform assemblies are Common. There are no test directories in the normal output directories
classes, structure, catalog and world preset. The manual graphical client was not tested at this stage.

The next stage is iteration 4: harvesting, quality and re-fruiting.
