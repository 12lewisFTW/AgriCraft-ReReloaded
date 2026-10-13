# Iteration 2: genome seed and saving planting

Date: 8 October 2026. Iteration 2 completed. Starfield Version Verified: Official `0.6.3fix1`.

## Genome and seed components

Uses standard `AgriGenome` AgriCraft with a dominant and recessive allele for each
of the six existing characteristics. On the native seed it is stored in a separate key
`CUSTOM_DATA["agricraft:starfield_seed"]`: schema version, crop ID and regular NBT genome.
The remaining components and keys are preserved, including native quality, item model,
name and third-party data. The seed is not replaced by item `agricraft:seed`.

A regular seed without a genome receives customized minimums. Reading doesn't throw random values
and does not change the item identity. After successful planting, the genome is retained on the remaining seeds of the stack;
a copy of the original seed with one item is saved along with the planting. Getting back
seed via `StarfieldCompat.seedFromPlant` returns this copy with the same genome and components.
This is an API for downstream tools; no new game seed drop or recipe added.

The base genome is built from the current minima without accessing the outdated allele cache.
Boundaries `4/8`, `7/7` and `7/6` checked: both base alleles are equal to the configured minimum.
Saved pairs are not overwritten when read due to configuration changes. Effects and
normalization of bonuses refers to iterations 8–9; higher values do not yet provide bonuses.

Incomplete genome, mismatched dominant species and unknown envelope schema version are rejected
planting before changing the world or spending an item. Integration does not replace them with another type.

## Identity and storage

`StarfieldPlantData` is a real production SavedData `agricraft_starfield_plants` with schema 1.
The key is the dimension and the root; the record contains the species ID, UUID of a specific planting, genome,
a complete copy of the seed and a crop sticks option. Modified NBTs are returned in copies.

Phase, days within phase, regrowth, infertile-soil fractional remainder, date processed,
watering, date of the giant, version of the original phases and UUID of the planter remain in the native
`stardew_crop_manager`. When saving the world normally, both records and the blocks themselves are saved.
There is no second schedule or growth executor in sidecar.

For the old planting, a record is created when loading/sending a chunk or the first request.
A stable base genome is assigned; native phases and dates do not change.
The top part resolves to the same root and UUID. Identical coordinates of different dimensions
do not unite. Replanting the same species after deletion receives a new UUID.

The unknown version of SavedData is retained in its entirety and recording is disabled. Damaged
lines of a known circuit are saved with diagnostics; identifiable damaged root
does not silently receive a new base genome. Explicitly deleting a plant clears its record.
A temporarily excluded catalog species is not removed from the save due to reload.

## Narrow lifecycle hooks

NeoForge mixin `SeedUseMixin` wraps `ItemStack.useOn`, preserving the context of the original
seed until routine planting and placement events are completed. The context is cleared at `finally`.
Native `onPlace` and `onRemove` create/remove sidecars for the root only; change of age
or the state of regrowth of the same block does not create a new planting.

For a regular rollback of a rejected giant plan, hook `restoreGrowthState` returns the previous
entry if Starfield restores the same native state object. It doesn't allow
the new planting will inherit the old UUID only if the coordinates match. Full mechanics
giants and return crop sticks remain in iteration 11.

When this setting is disabled, no new genetic records are created. Already existing entry
the remote plant is cleared by neutral interception without loading the native bridge; rollback
regular deletion retains the previous record. Starfield interceptions only apply to
verified version; Without a mod and with an unconfirmed version, native mixins are skipped.

## Spade transfer

The existing AgriCraft paddle transports the native crop into the Starfield dimension.
The snapshot in `CUSTOM_DATA["agricraft:starfield_plant"]` contains version, dimension, record
fits, exact conditions of both parts and all fields `CropGrowthState` of the verified version.
The transfer preserves the UUID, genome, components of the original seed, crop sticks variant and processed day.
Your own and third-party blade data is saved. The genome of another plant is not overwritten on the instrument.

Regular catalog roots with one/two parts are supported. Pots, giant blocks,
incomplete geometry, unknown layout, unsuitable/occupied soil and other dimension
are not allowed to be transferred. Failure leaves a snapshot on the instrument. Native checks
farm suitability and permits are maintained; NeoForge hosting events may cancel
transplant. Then the new root is removed, and the image remains on the trowel.

`setSticks` changes only the saved version of crop sticks, preserving the block, UUID and native
condition. Game staging, shooting, model and double crop sticks refer to iteration 4.

## Client

The server sends a snapshot after the chunk is actually sent (`ChunkWatchEvent.Sent`), then
changes to his observers' records. The snapshot pre-clears the previous chunk entries,
including the case of an empty chunk. When monitoring stops, the server deletes the subscription.

The cache is associated with a client world object, a dimension, a server session, and a root.
Packets from another link/world/dimension are rejected. Changing world/dimension, unloading chunk
and the output clears the corresponding entries. The server does not accept genome from client packets.
Catalog of species, crop sticks models and external `AgriApi.getCrop` are not yet synchronized/connected.

## Tests and replays

```powershell
$env:JAVA_HOME = 'C:\Vivoderin\Mod\jdk-21.0.10'
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldRestartPhase=write --console=plain
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldRestartPhase=read --console=plain
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=disabled --console=plain
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=absent --console=plain
.\gradlew.bat :neoforge:build :fabric:build --console=plain
```

Each enabled launch performs 13 GameTest: six previous stages and seven new ones.
The new checks cover all 50 base genomes; real planting `ItemStack.useOn`;
backseed with both alleles and native quality; erroneous shells; deletion, repeat
planting, disabling support and native rollback; different measurements; changing saved
crop sticks; transfer of full two-part crop with refusal/cancellation; SavedData and old planting;
network codec and cache clearing; full restart of two separate JVMs with re-day suppression.

Planting tests use a real registered test player farm
in the ignored world of NeoForge. Public building rights, growth calendar and probability
there is no need for selection. The write phase leaves the plant for regular server conservation;
read reads real world files. Witness only stores expected values and does not restore the world.
Without `starfieldRestartPhase`, the restart check is skipped and does not serve as evidence.

Codec and client cache isolation tested without a graphical client; manual check
connection of an individual client to the server has not yet been completed. Game model crop sticks,
analyzer/magnifying glass/scissors, selection, irrigation and gene effects are not included in this stage.

Logs: `build/starfield-iteration0/iteration2-enabled-write.log`, `iteration2-enabled-read.log`,
`iteration2-disabled.log`, `iteration2-absent.log`, `iteration2-normal-build.log`.
Graduation archives are checked by `tests/starfield/verify_release.py`; links to Starfield are allowed
only in the explicit list of native NeoForge implementations. Test materials are not packaged.

Result: all 13 tests passed in two separate JVM write/read; additionally passed the launch
with disabled support and launch without Starfield (28 performing mandatory tests).
NeoForge/Fabric builds are successful. NeoForge release/shadow tested after test profile
with genetics, then five archives of the regular build, including Fabric and both Common builds.
There are no test classes, structure, catalog or preset left in the normal output directories.

Bytecode checking distinguishes between direct native types and two explicitly permitted string targets
`@Pseudo`/`@Inject`, which Loom writes through `/`. mixins themselves do not contain JVM references
to Starfield classes in fields, parameters or calls. The next stage is iteration 3.
