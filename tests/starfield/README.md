# Starfield compatibility tests

The test code is development-only. Iterations 1–2 ship optional loading, a native catalog,
genotype persistence and trowel transfer. Iteration 3 verifies native daily growth,
iteration 4 adds harvest transactions, and iteration 5 adds native-seed analysis
and shared tool inspection. Iteration 6 connects paid irrigation to native soil.
Iteration 7 connects the active crossbreeding engine to native offspring on empty
double sticks. Iteration 8 applies growth/gain rules to native schedules and transactional
hand/scythe output. Iterations 9–10 add strength/resistance and native multipart/special
crops, including wild-seed forage transitions. Iteration 11 completes native giant
formation with transactional stick restitution. Gameplay stick placement/rendering, seed-maker lineage, machine harvesting and
release acceptance are implemented in the current working tree. Current results
and limits are recorded in `docs/starfield/ACCEPTANCE-RU.md`. Iteration 14 was canceled.
Use Java 21 and Python 3.11+ (standard library only), from the repository root:

```powershell
python tests/starfield/prepare.py
python tests/starfield/catalog.py --offline
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype --console=plain
```

`dependencies.json` locks the official release, source commit, size and SHA-256.
Preparation fetches only from the official GitHub project, verifies the JAR and each
reference Git blob, and stores them under ignored `build/starfield-iteration0`.
`--offline` verifies an existing preparation without downloading anything.
No patch archive or its bundled dependencies are used.

The first three GameTests run on a real NeoForge server with the official release:

- Empty, marked AgriCraft sticks receive wet/dry days and historical contexts once per date.
- Block-entity and scheduler NBT round-trip retains progress and prevents replay of a date.
- Real potato metadata alone does not supply an addon phase executor.

Those diagnostic sticks use vanilla farmland to isolate scheduling from pending soil registration.
Seven additional tests exercise unchanged native blocks with a separate SavedData stick/genome
attachment: potato, broccoli, corn, cauliflower, native scheduler/attachment reload, a native giant,
and an opt-in two-process persistence test. Native code alone executes growth and harvest.
The baseline genome uses AgriCraft's existing six traits at their configured minima.
That initial diagnostic attachment does not provide gameplay placement, a sticks
renderer, tool lookup, irrigation, crossbreeding, or effects above baseline. Later
production lifecycle and tool support are described in iterations 2 and 5 below.

For the real JVM restart test, run these sequentially in the same scratch test world:

```powershell
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldRestartPhase=write --console=plain
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldRestartPhase=read --console=plain
```

The write phase leaves a crop for normal server shutdown to save; read compares the native
block, planting UUID, genotype, phase and processed date loaded from disk. It verifies that
the same date is not replayed and the next date advances once. With no restart phase, this
test is a no-op and must not be cited as restart evidence. A tiny expected-state witness is
written under ignored `build/starfield-iteration0`; it is never used to restore world state.
The test world/logs live in `neoforge/build/starfield-prototype-run`.

GameTestServer bakes only the flat preset, so the profile generates a test-only flat world
preset containing a real Starfield ServerLevel with its native dimension type. There is no
dimension-key replacement, copied native crop executor or copied native world preset.

The opt-in profile adds test sources only to NeoForge's development scanner.
The main release archive tasks always exclude those classes and the generated empty
test structure and flat preset, including after a previous profile run. Ordinary resource
processing also removes stale probe resources before a development launch. Ordinary NeoForge
builds use the verified Starfield JAR only for compilation, with a guarded native catalog
implementation. Common/Fabric never reference Starfield classes.

After building, check archive isolation with `python tests/starfield/verify_release.py`
followed by the NeoForge/Fabric release JAR paths. This checks both leaked files and
Starfield references in release bytecode outside the explicitly listed guarded NeoForge implementations.
Optional native mixins may contain only their exact target/selector annotation strings, never
native JVM class/member/parameter references; these strings are remapped by Loom to slashes.

Iteration 1 loading/catalog tests:

```powershell
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled --console=plain
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=disabled --console=plain
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=absent --console=plain
.\gradlew.bat :fabric:runStarfieldLoading -PstarfieldFabricSmoke --console=plain
```

These use separate ignored scratch worlds/configs. Enabled tests compare every native species
with the source-verified research catalog and check native soils, upper-part roots, contextual
metadata providers, and a real asynchronous datapack reload. Invalid data is quarantined with
diagnostics without resetting the existing plant's phase or processed date. Disabled/absent
NeoForge and a real Fabric headless server verify safe loading. A descriptor-only development
JAR registers the Fabric test; no test entrypoint is added to the release manifest.

See [`ITERATION-1-RU.md`](../../docs/starfield/ITERATION-1-RU.md) for the runtime boundaries.

Iteration 2 production persistence/lifecycle tests (13 tests per enabled launch):

```powershell
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldRestartPhase=write --console=plain
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldRestartPhase=read --console=plain
```

These exercise actual seed use on a registered native farm, all 50 deterministic baselines,
both alleles and native/foreign components, corrupt/future schemas, root/dimension/UUID
identity, native removal/replant/rollback, trowel transfer with placement cancellation, and
the client cache/network codec. The restart pair uses the production SavedData and real world
files in the enabled scratch world; its separate expected-value witness never restores data.
The native phase/day executor is neither replaced nor copied. See
[`ITERATION-2-RU.md`](../../docs/starfield/ITERATION-2-RU.md) for supported transfers and remaining limits.

Find the completed architecture result in
[`ITERATION-0-SPEC-RU.md`](../../docs/starfield/ITERATION-0-SPEC-RU.md).

Iteration 3 paired daily-growth tests:

```powershell
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests --console=plain
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests -PstarfieldRestartPhase=write --console=plain
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests -PstarfieldRestartPhase=read --console=plain
```

Eight new tests compare real native potato roots with and without production stick attachments:
normal daily water consumption/retention, six-day baseline maturity and dry days, native soils
and all three stick variants, historical seasons and native farm grace, Speed-Gro/date replay,
random ticks/bone meal, an actual server chunk unload followed by native one-/three-day farm
catch-up, and a full two-JVM restart. No native executor is copied or replaced. Offline watering
keeps Starfield's own policy. The normal combined launch runs 21 tests (14 without genetics).
The restart pair runs 20 (13 without genetics): its generator excludes the global `growDaily`
test, which would legitimately advance another test's checkpoint before shutdown. The first,
separate launch covers that real global pass. Restart tests without a phase are no-ops and
must not be cited as persistence evidence. Daily and genotype restart witnesses are separate.
See [`ITERATION-3-RU.md`](../../docs/starfield/ITERATION-3-RU.md) for boundaries and reproduction.

Iteration 4 native player-harvest tests (29 tests with the previous profiles):

```powershell
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests -PstarfieldHarvestTests --console=plain
```

Eight additional tests compare real native produce components, quantity and XP against
the disabled-integration control; cover broccoli/corn regrowth, upper-part harvest,
native scythe swings, farm permissions, Agri tool fallback, full inventory, canceled
and throwing item joins (including a partially published harvest), and reentrant
event listeners. Test observers capture actual item joins on remote native farms,
whose chunks may not yet be included in ordinary entity lookup. The control must
produce a nonempty output. See [`ITERATION-4-RU.md`](../../docs/starfield/ITERATION-4-RU.md)
for the transaction boundary and unsupported destructive/automation paths.

Iteration 5 analyzer and tool-inspection tests (38 tests with the previous profiles):

```powershell
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests -PstarfieldHarvestTests -PstarfieldToolTests --console=plain
```

Nine new tests cover all 50 native seed types, both alleles and foreign components,
ordinary wheat conversion, menu stack transfer and inventory conservation, malformed
genomes, occupied slots, native farm permissions, distance, closed/replaced menus,
actual maturity timing with fertilizer and infertile soil, regrow countdown, unknown
legacy progress, tall-crop roots, read-only request validation, codecs and cache
invalidation, every Agri tool's applicability, direct analyzer block actions and the
actual Jade server provider. Magnifying glass, Jade and The One Probe share a server
report and formatter; no AGE-based percentage or additional growth executor is used.
Native seeds remain native items: native-to-AgriSeed conversion and seed-bag storage
are unsupported to avoid losing components or replacing the native crop engine.

Remaining days mean watered days in season under the current soil and fertilizer,
not a calendar-date forecast. Reflection accessors invoke the pinned native schedule
methods; they do not copy their algorithms. A scratch native state accounts for
infertile-soil fractional progress. Legacy non-phase progress stays unknown and is
not migrated by inspection. The two old restart checkpoints are still no-ops without
a restart phase and must not count as new restart evidence.

Use the disabled/absent/Fabric launches above and ordinary builds to verify isolation.
The release verifier also checks archives produced while the test profile is enabled.
Graphical client layout and a complete TOP client session were not manually tested.
See [`ITERATION-5-RU.md`](../../docs/starfield/ITERATION-5-RU.md) for precise behavior,
limitations and reproduction. At that milestone gameplay sticks, irrigation,
breeding and gene effects above the baseline were still pending.

Iteration 6 paid irrigation tests (47 tests with the previous profiles):

```powershell
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests -PstarfieldHarvestTests -PstarfieldToolTests -PstarfieldIrrigationTests --console=plain
```

Nine additional tests exercise actual native watering-can use and water cost,
native `SprinklerBlock.waterNow`, paid Agri block-entity sweeps, tank/channel flow,
empty/disconnected/closed supply, exact water conservation, radius/height/obstacles,
tall crop traversal, restricted native soils and garden pots, repeated/overlapping
passes, same-date replay, native corn regrowth, actual global daily water consumption
and fertilizer retention, partial buffer/sweep NBT restore and dirty marking, all
50 native species, and unloaded chunk boundaries. Native irrigation never calls
randomTick, bone meal or daily growth. Existing zero-consumption configuration keeps
its original free-water semantics; positive-consumption empty systems cannot water.
The existing real datapack-reload test also checks that irrigation ignores a
quarantined species and resumes after its validated catalog entry recovers.

The Fabric smoke profile now also runs a real ordinary sprinkler regression: empty
buffer, paid sweeps, vanilla farmland moisture and continued vanilla wheat growth.
It does not link native NeoForge classes. Run the disabled/absent/Fabric profiles
and ordinary builds above, then verify archive isolation as in earlier iterations.
Buffer persistence is an actual block-entity NBT restore with chunk dirty checking,
not a new two-JVM restart test. See [`ITERATION-6-RU.md`](../../docs/starfield/ITERATION-6-RU.md)
for reproduction and limits. Gameplay stick rendering/placement, breeding, genetic
bonuses and giant-form irrigation are not completed by this milestone.

Iteration 7 native offspring tests (57 tests with all previous profiles):

```powershell
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests -PstarfieldHarvestTests -PstarfieldToolTests -PstarfieldIrrigationTests -PstarfieldBreedingTests --console=plain
```

Ten additional tests cover the actual empty-double growth tick, fresh native state
and six-day watered maturity, preserved parent progress, both clone alleles,
fertility/multiplier and mutativity effects, configured bounds, immature parents,
upper carriers, no arbitrary interspecies mixing, an explicit registry mutation,
soil/season/space gates, canceled/throwing placement rollback, all 50 species,
offspring seed/SavedData round-trip, disabled integration and ordinary wheat breeding.
The active AgriCraft engine executes genetic rolls; Starfield alone executes growth.
Native readiness is read without calling the legacy phase-repairing native probe.

The opt-in profile authors one test-only mutation registry entry. It is deleted
from runtime resources when breeding tests are absent and excluded from every
archive even after the profile runs. Native species participate in ordinary mutation
validity/complexity without registering virtual plants. No mutation recipe is shipped.
Run the disabled/absent/Fabric profiles and ordinary builds, then verify isolation.
SavedData recovery is an NBT round-trip, not a new two-process restart.
See [`ITERATION-7-RU.md`](../../docs/starfield/ITERATION-7-RU.md) for exact scope and limits.

Iteration 8 growth/gain checks use the same combined command above (66 tests). Nine additional
scenarios in the daily/harvest sources exercise documented formulas and custom bounds,
all 50 native schedules (including wild-seed transformation to forage), fertilizer,
dry/same-date behavior, inspection, disabled integration, hand/scythe output components,
sunflower seeds and wheat side products, regrow initialization, stack overflow and
rejected original/overflow output. The helper edits test genomes only through production
SavedData serialization; no runtime genome editor is shipped.

The harvest profile also runs independently with only `-PstarfieldHarvestTests` (18 tests).
It compares native quality rolls at equal farming XP and verifies that overflow entities
do not consume additional world randomness. Bonus entities inherit native movement/pickup
metadata and receive distinct UUIDs.

For the actual two-JVM genetic persistence check, run sequentially:

```powershell
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests -PstarfieldRestartPhase=write --console=plain
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests -PstarfieldRestartPhase=read --console=plain
```

The new checkpoint compares native serialized progress and both halves with production
UUID/genome/sticks after normal world saving/loading. The same dates must remain unchanged,
and the next regrow day must subtract exactly one from the saved genetic countdown.
`iteration8-restart-witness.nbt` is a comparison witness only. Without a restart phase
that checkpoint is a no-op; ordinary passing test totals do not prove a restart.
Run disabled/absent/Fabric loading and verify normal/profile archive isolation as above.
See [`ITERATION-8-RU.md`](../../docs/starfield/ITERATION-8-RU.md) for balance and remaining limits.

Iteration 11 giant tests (104 tests with all previous profiles):

```powershell
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests -PstarfieldHarvestTests -PstarfieldToolTests -PstarfieldIrrigationTests -PstarfieldBreedingTests -PstarfieldSpecialTests -PstarfieldGiantTests --console=plain
```

Fourteen additional scenarios cover all five native giants, their actual footprints,
native one-percent rolls, mature/watered trigger and immature neighbors, missing or
wrong crops, obstacles/containers/support, dry/repeated/offline days, the actual daily
scheduler and mixed stick attachment, native farm-slot boundaries, invalid/future data,
partial canceled/throwing restitution and pending scheduler rollback, reentrancy,
native harvest tools/quantity/quality/XP without arbitrary parent genes, legacy crops,
disabled integration and doTileDrops. Native code alone executes formation and giant
harvest. The test profile adds no production giant recipe or chance modifier.

For real two-JVM giant persistence, run sequentially:

```powershell
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests -PstarfieldSpecialTests -PstarfieldGiantTests -PstarfieldRestartPhase=write --console=plain
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests -PstarfieldSpecialTests -PstarfieldGiantTests -PstarfieldRestartPhase=read --console=plain
```

The first JVM leaves an actual giant and returned stick entities for normal world
saving. The second compares saved blocks, entity UUIDs/components, and absence of all
nine consumed sidecars/scheduler roots, then harvests the saved giant once. The
iteration11-restart-witness.nbt file is only a comparison witness; it never recreates
world state. With no restart phase this checkpoint is a no-op. Re-run disabled/absent/
Fabric loading and verify both profile-built and ordinary release archives. See
[`ITERATION-11-RU.md`](../../docs/starfield/ITERATION-11-RU.md) for exact scope and limits.


## Iteration 12 — hereditary production and automation

`-PstarfieldAutomationTests` enables 14 server checks for manual/automatic
seed → crop → produce → native seed maker → seed cycles, both alleles,
selected output/deadline persistence, simulated/partial machine transfers,
ancient/mixed output and exceptional native mappings, invalid heredity,
quality/side products/native randomness/zero farming XP, one multipart/regrow
transition, internal rollback, a throwing inventory sink, final addon plans,
artisan goods and planted seasonal forage. The opt-in restart check uses a
real placed SeedMakerBlockEntity in the saved world, not a restored witness.

`-PstarfieldCreateTests` requires the automation profile and adds the real
Create 6.0.10-280 runtime plus two actual HarvesterMovementBehaviour checks.
Output is collected by Create into a contraption's external inventory; repeated
upper/lower visits retain native regrowth, and immature/forage/giant paths never
fall through to Create's generic destruction/replanting. Ponder 1.0.82,
Flywheel 1.0.6 and Registrate MC1.21-1.3.0+67 are development-only dependencies.

Full regression (restart checkpoints are no-ops without the phase property):

```powershell
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests -PstarfieldHarvestTests -PstarfieldToolTests -PstarfieldIrrigationTests -PstarfieldBreedingTests -PstarfieldSpecialTests -PstarfieldGiantTests -PstarfieldAutomationTests -PstarfieldCreateTests --console=plain
```

Two-JVM persistence uses the established isolated restart profile plus automation:

```powershell
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests -PstarfieldSpecialTests -PstarfieldGiantTests -PstarfieldAutomationTests -PstarfieldRestartPhase=write --console=plain
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests -PstarfieldSpecialTests -PstarfieldGiantTests -PstarfieldAutomationTests -PstarfieldRestartPhase=read --console=plain
```

The full irrigation suite runs a whole-world native daily process, which can
advance saved checkpoint crops after their write witness. Run that regression
without a restart phase, separately from the persistence pair. The native energy
of the persistent watering probe is replenished at fixture setup so repeated
JVM runs still test a payable survival-mode action.

`build/starfield-iteration0/iteration12-restart-witness.nbt` records only the
machine position, chosen product/genome and absolute deadline. The read JVM
loads the existing native block entity, checks these fields, extracts seeds
once and replants them. The witness never creates the machine or its inventory.
Internal failures before delivery roll back the native crop; arbitrary throwing
consumer sinks retain the committed crop and are not retryable. See
[`ITERATION-12-RU.md`](../../docs/starfield/ITERATION-12-RU.md) for limits and results.


## Iteration 13 — release regression and migration

Add `-PstarfieldReleaseTests` to the complete iteration 12 enabled/Create command.
For a focused release-only investigation, also pass `-PstarfieldReleaseOnly`; this
excludes catalog/reload probes and is not the full acceptance profile.
The five additional GameTests verify 16,384 SavedData entries across two dimensions,
1,000 chunk snapshots without chunk loading, actual placement/replacement/removal,
requests by two ordinary players (dimension/UUID/reach/unloaded chunk checks),
and migration of plants added during a live disabled interval, including section
offsets at negative and positive heights. The live tests deliver the public
ServerTickEvent.Post through the event bus before reactivation.
This is a correctness and allocation-scope test, not a client FPS benchmark or a
16,384-block native daily-growth benchmark.

The cross-mode migration runs three separate JVMs against one dedicated test world:

```powershell
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldMigrationPhase=write --console=plain
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=disabled -PstarfieldMigrationPhase=disabled --console=plain
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldMigrationPhase=read --console=plain
```

These profiles use `neoforge/build/starfield-migration-run`; they do not modify the
ordinary enabled/disabled/absent worlds. The write phase saves actual native crops,
full sidecar entries and a real chest containing inherited native seeds. The disabled
phase compares the world and invokes the pinned native wild-seed transformation;
reactivation verifies its dormant genotype. The NBT witness contains expected values
only and never restores world state. Do not combine these phases with unrelated
whole-world calendar suites. Catalog/reload probes are deliberately excluded from
these migration JVMs and are covered by the ordinary complete enabled profile. All prototype classes/resources remain excluded from JARs.

Archive verification also rejects native `assets/stardewcraft` and `data/stardewcraft`
namespaces. See [the user guide](../../docs/starfield/USER-GUIDE-RU.md) for actual
release limitations and completed stick presentation and graphical client QA.


## Completion acceptance (after iteration 13)

The absent/incompatible loading checks also inspect the actual global event bus
and transformed ItemStack class: optional gameplay callbacks and the native
item-use mixin must be absent. An ordinary wheat/sprinkler regression verifies
paid watering and normal vanilla growth in each loading mode.

An actual graphical client can verify the absence of render/cache callbacks and
the magnifying inspector after its normal startup:

```powershell
.\gradlew.bat :neoforge:runStarfieldAbsentClient -PstarfieldPrototype -PstarfieldTestMode=absent -PstarfieldAbsentClientTests --console=plain
```

It writes `build/starfield-iteration0/no-starfield-client-result.txt` and stops its
own test client. This fixture is excluded from archives. Do not change build
profiles while the test client is running.

The enabled combined profile includes native placement/removal, harvest retention,
coffee produce-to-seed lineage, the renderer chunk index and a real 4096-crop farm.
Add `-PstarfieldMachineTests` to include the real Industrial Foregoing gatherer and
PneumaticCraft DroneEntity/DroneAIHarvest fixtures. They use IF 1.21-3.6.39,
Titanium 1.21-4.0.45 and PNC 8.2.21, without substituting fake worker implementations.
The drone fixture calls its real block interaction; it does not verify pathfinding.

```powershell
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests -PstarfieldHarvestTests -PstarfieldToolTests -PstarfieldIrrigationTests -PstarfieldBreedingTests -PstarfieldSpecialTests -PstarfieldGiantTests -PstarfieldAutomationTests -PstarfieldCreateTests -PstarfieldReleaseTests -PstarfieldMachineTests --console=plain
python tests/starfield/prepare.py --incompatible
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=incompatible --console=plain
```

The incompatible profile uses the original official 0.6.3 release with its locked
SHA-256. It does not rewrite metadata or patch the native dependency.

The graphical profile connects two full Minecraft clients to a local ordinary
dedicated server. Use the same flags for all three processes. Compile first, then
run these in three terminals; do not build another source profile while they run:

```powershell
.\gradlew.bat :neoforge:compileJava -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldClientAcceptance
.\gradlew.bat :neoforge:runAcceptanceServer -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldClientAcceptance
.\gradlew.bat :neoforge:runAcceptanceClientA -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldClientAcceptance
.\gradlew.bat :neoforge:runAcceptanceClientB -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldClientAcceptance
```

It verifies authoritative UUID/genome/template/material/form snapshots, live changes,
dimension changes, tombstones, real chunk unload and real disconnect/reconnect.
Screenshots are saved by Minecraft in each test client's `screenshots` directory
after native terrain section compilation and twenty unobstructed world frames.
The result witness is `build/starfield-iteration0/client-acceptance-result.txt`.
Native onboarding is closed and test cameras are positioned by test-only hooks.
Models/assets are unchanged. This harness is excluded from release archives.
After ordinary server saving and ServerStopped hooks, a daemon completion thread
joins the server thread before explicitly exiting the test JVM. This avoids the
Architectury classpath reader's non-daemon worker pools keeping a completed dev
launch alive; it cannot bypass a stalled server thread or its save operation.
Development launches set ForkJoinPool common parallelism to 1 to reduce an
Architectury Transformer concurrency failure; this is not a production setting.

To repeat the previously stalled migration + catalog/datapack reload combination:

```powershell
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldMigrationPhase=write -PstarfieldMigrationReloadTests --console=plain
```

For read-only chunk-generation/unload observations, additionally pass
`-PstarfieldSaveDiagnostics -PstarfieldSaveRun=historical-1`. This creates a separate
world and witness for that run name. `-PstarfieldSaveHistoricalTests` restores the
original seven-test set and exact grid position from the stalled log (8701193,
-59, 11182101); it excludes only the later vanilla-wheat test. A separate local
marker mod activates the observers, without referencing them from the release
manifest. Observation begins at the entry of MinecraftServer.stopServer, before
other mods' ServerStopping handlers. The observers log generation origins, holder reference
counts/save-future state, and periodic samples if saving stalls. They never load
chunks or change readiness/unload behavior. Diagnostics and marker resources are
excluded from archives. See [investigation and limits](../../docs/starfield/SAVE-HANG-INVESTIGATION-RU.md).

`standalone.py` installs an ordinary release JAR and the original dependencies into
an independently installed NeoForge 21.1.226 server under ignored
`build/starfield-standalone`. Run the official installer there with `--installServer`
first. The check uses two normal dedicated-server JVMs, console block placement,
ordinary stop/save, and persisted authoritative genotype/UUID comparison. No dev
classpath, GameTest runner or probe mod is installed in this server.

`--reload-before-stop` waits for the real
datapack reload catalog, removes the native crop's forced ticket, then stops both
ordinary server JVMs. Separate reload logs/result preserve the baseline evidence.
If shutdown times out, the script records a thread dump before terminating its
own test JVM and reports failure.

```powershell
python tests/starfield/standalone.py neoforge/build/libs/AgriCraft-neoforge-1.21.1-4.0.20.jar <resourcefulconfig-neoforge-1.21-3.0.11.jar>
python tests/starfield/standalone.py neoforge/build/libs/AgriCraft-neoforge-1.21.1-4.0.20.jar <resourcefulconfig-neoforge-1.21-3.0.11.jar> --reload-before-stop
```

The script creates only the ignored scratch installation, binds it to localhost,
and accepts the Minecraft EULA for that test server. It checks native baseline
placement and restart persistence; it does not replace the full genetics matrix
or prove every client/machine scenario using an installed release artifact.
