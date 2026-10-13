# Iteration 1: optional loading and native catalog

Date: 8 October 2026. Iteration 1 completed. Starfield Version Verified: Official `0.6.3fix1`.

## Implemented

Added setting `compat.starfield` to `config/agricraft.jsonc`, default `true`.
After the change, you need to restart the server. The Russian and English labels clearly indicate NeoForge.
On Fabric, the setting does not include the Starfield implementation.

The `StarfieldCompat` loader checks that the setup is configured, that `stardewcraft` is present, and that the
version before loading `NativeStarfieldCatalog`. Without a mod, with the setting turned off or with another
The native bridge version does not connect. The untested version issues a warning, but not
prevents AgriCraft from starting. An API binding error also disables the bridge with diagnostics.
Starfield remains an optional dependency of NeoForge; Common/Fabric do not import its classes.

The official JAR is anchored in `tests/starfield/dependencies.json` and is used by the normal build
NeoForge only in `compileOnly`. Task `prepareStarfieldCompileDependency` checks the size and
SHA-256, if there is no cache, downloads the pinned official release. In `--offline` mode
you need a pre-prepared cache. The AgriCraft release does not include classes, JARs or resources
Starfield. Test mode separately adds this mod to runtime.

## Where does the data come from?

The issued catalog does not contain a manually rewritten table of 50 species. He's fingering
registered blocks `stardewcraft`, inheriting the native `StardewCropBlock`, and uses
Starfield public methods:

- `isSeedItem` - actual match of the seed;
- `getCloneItemStack` - native icon and the main item of a regular product;
- `getPhaseDaysForDisplay`, `canRegrowForDisplay`, `getRegrowDaysForDisplay` - descriptive
  data from initial phases and regrowth;
- `requiresScytheHarvest` and property `DOUBLE_BLOCK_HALF` - collection method and geometry;
- `StardewAgricultureDataApi.crop(state)` - optional global Data Map metadata;
- `StardewAgricultureDataApi.crop(level, root, state)` - metadata taking into account specific
  positions and registered native providers when inspecting the planting;
- `StardewCropRuntime.inspect` - native root, part, visual stage and readiness;
- `TerrainSoils.cropSupport`, `sandy`, `infertile`, `blocksSprinklers` - native soil classification.

`StardewCropTypes.definitions()` lists third-party runtime types and is therefore not used
as a source of native catalog. If there is no Data Map, the metadata remains missing:
integration does not substitute fictitious seasons or its own rules of growth.

Native phases for display and optional `Metadata.phaseDays` are stored separately.
None of these arrays are executed by AgriCraft as a growth algorithm. `Plant.visualStage`
does not count as a phase or the number of days that have passed. Progress and dates belong to Starfield.

The readiness inspection is delegated to Starfield himself. Its `isMature` can fix the inconsistent
phase of old planting with already mature `AGE`; This is a standard native migration, not an additional one
AgriCraft day. Reassembling the catalog does not cause this operation and does not read/change the progress of plantings.

All 50 species found. Four species of wild seeds are labeled as seasonal forage; they don't have
fixed `produce`. The seed icon is not issued as a final harvest item.
Mixed seeds are not added as another type. Catalog describes the main subject of ordinary
crops; additional drops, quantity, quality and transformations are performed by the native mod.

Soils are accepted according to their native identity: `farmland`, `sandy_farmland`, `infertile_farmland`
and `garden_pot`. Vanilla bed and random blocks with a similar name are not allowed.
The specific requirements of a particular plant continue to be tested by Starfield. This is recognition
in the catalog, rather than connecting the game placement of crop sticks or completed irrigation.

## Update and erroneous data

The first update is performed after the server starts. Subsequent updates are enabled
to `OnDatapackSyncEvent` for a full server data reload; individual player entry
does not rebuild catalog. Readers get an entire, immutable snapshot of one version.

The existence of seed/product items, seed ambiguity, known
seasons and method of collection, acceptable values of phases/regrowth/experience. Invalid species
excluded from the new snapshot; the message contains its ID and reason. Position error
provider declines inspection of a specific planting and also provides diagnostics.
Unknown ID returns an empty result rather than another crop.

Updating the catalog does not change blocks, BlockEntity, genome, SavedData or scheduler entries.
Data correction restores species recognition without re-planting and without resetting
dates. Catalog exists in the memory of the current server and is cleared when it stops.

## Checks

```powershell
$env:JAVA_HOME = 'C:\Vivoderin\Mod\jdk-21.0.10'
python tests/starfield/prepare.py
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled --console=plain
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=disabled --console=plain
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=absent --console=plain
.\gradlew.bat :fabric:runStarfieldLoading -PstarfieldFabricSmoke --console=plain
.\gradlew.bat :neoforge:build :fabric:build --console=plain
```

NeoForge has separate ignored worlds `build/starfield-compat-{enabled,disabled,absent}-run`;
settings for tests are written only to them. Iteration prototype world and configuration 0
are not used. Test classes, expected catalog and flat preset are excluded from release
JAR. Fabric uses a separate JAR test descriptor; production `fabric.mod.json`
does not receive test entrypoint.

Six enabled mode checks cover the loader; all 50 ID/seeds/items/phases/
regrowth/geometry/collection methods according to the verified original catalog; native soils;
inspection of the top; positional providers and real reload Data Maps.
The last test sequentially loads the correct entry, an entry with a non-existent
subject and source data. Catalog changes `50 → 49 → 50`, and the existing block, phase,
the progress within the phase and the processed day remain the same. Chance, quality and growth algorithm
are not replaced by dough.

Additional server checks confirm the setting is disabled, lack of Starfield
in the NeoForge runtime and actually running AgriCraft on Fabric without native NeoForge classes.
Unchecked versions are covered by checking the list of valid versions; other binary releases
Starfield is not launched or advertised as compatible.

The logs are located in `build/starfield-iteration0`: `iteration1-enabled-final.log`,
`iteration1-disabled-final.log`, `iteration1-absent-final.log`, `iteration1-fabric-run1.log`
and `iteration1-normal-build.log`. For Fabric, `fabric-loading-tests.xml` is also recorded.

Checking release archives - `tests/starfield/verify_release.py`: native links allowed
only inside `NativeStarfieldCatalog.class` NeoForge; Common/Fabric do not contain native
implementation. Test materials and classes of Starfield itself are prohibited in all archived versions.

Result of testing 8 October: all nine server GameTests passed (6 enabled, 1 disabled,
1 absent, 1 Fabric). In absent mode, the absence of Starfield classes is additionally confirmed
in the runtime loader. Regular NeoForge/Fabric builds are successful; five release archives verified
archives, including shadow NeoForge and both platform builds of Common. After test runs
a normal build removes test classes and resources from its output catalog.

## Stage boundary

The iteration boundary is 1 at the time of its completion: loading and descriptive recognition. Game seeds are not received yet
genome, `AgriApi.getCrop` does not yet allow external native root, and the client catalog
does not synchronize with the remote server. Crop sticks, tools, inheritance and effects
genes belong to the following stages. The next one is iteration 2: seed genome and planting preservation.

Update 8 October: genome storage and transfer added in [iterations 2](ITERATION-2.md).
The results and boundaries of the very first stage are preserved above.
