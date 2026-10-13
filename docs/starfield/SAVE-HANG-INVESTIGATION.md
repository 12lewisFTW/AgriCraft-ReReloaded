# Investigate hang on saving

Date: 10 October 2026. Minecraft 1.21.1, NeoForge 21.1.226,
Starfield 0.6.3fix1, AgriCraft 4.0.19, Java 21.0.10.

## Conclusion

By user decision dated 10 October 2026 this irreproducible
The incident has been excluded from the stable release blockers. Further investigation
not required for current release; investigation resumes if it recurs.
Logs and test diagnostics are saved.

The old incident has not yet been reproduced. Active cycle mechanism installed
unloading chunks, but the specific chunk and request that left it unready,
not recorded in old materials. Correction of the cause is not stated.
Minecraft/Starfield save code and production integration are unchanged.

## What the original dump confirms

The source materials are located in the ignored folder `build/starfield-iteration0`:
`iteration13-migration-write-stalled.log` and `iteration13-write-threads.log`.
All seven GameTests completed successfully on October 16:53:32 9, after which
the server stopped at `Saving worlds`.

In the 16:54:50 dump, the server thread is in the RUNNABLE state:
for 100,36 seconds of existence it used 96,81 seconds of CPU.
The stack goes through `MinecraftServer.stopServer` → `ServerChunkCache.tick`
→ `ChunkMap.processUnloads` → callback `scheduleUnload` → repeated
`scheduleUnload`. Three independent samples of Starfield built-in diagnostics
after 11, 21 and 31 seconds show the same loop or retrieving tasks from it
queues. The generation worker threads are in full dump waiting for new work.

This is actively repeating tasks rather than waiting for a disk or monitor to lock.
These stacks do not run the genetic or daily AgriCraft pass.
The absence of its frames does not exclude an indirect request for the chunk earlier.

## How does such a cycle occur?

The bytecode of the Minecraft used with the NeoForge patches 21.1.226 was checked.
`ChunkHolder.isReadyForSaving()` requires simultaneously zero
`generationRefCount` and completed `saveSync`.

`scheduleUnload` sets the callback to `getSaveSyncFuture()`. If callback
detects an unready holder, it calls `scheduleUnload` again.
When the future has already completed, the new task immediately goes to `unloadQueue`.
When stopped, the time condition in `processUnloads` is constantly true, so
a queue that replenishes itself may not transfer control to the executor
the rest of the work. Non-null references to generation - candidate being checked,
rather than the value measured at the original incident.

The old dump does not contain the contents of the holder, queues, or the number of the problematic chunk
and request history. It is impossible to honestly determine the initiator or
declare a specific mod's bug fixed.

In [NeoForge #1403](https://github.com/neoforged/NeoForge/issues/1403)
the author of a similar case established a call to `getBlockEntity` without checking
loading position, which started generation when unloading. Issue closed by ourselves
by the author; this is not a confirmed NeoForge bug and not proof of the same
initiator in our assembly.

## New diagnostics

The optional profile `starfieldSaveDiagnostics` includes only test
observers. They count real generation and unloading requests, record
source of requests from the moment of entry into `MinecraftServer.stopServer`, to the handlers
ServerStopping other mods, and for an unready holder - measurement,
coordinates, number of links to generation and state of the future. For prolonged periods
When stopped, the daemon writes these values again every five seconds.
Observers do not load chunks or change their readiness, future, or queue.
The counters also confirm that the implementations were actually carried out.
The `retries` field counts all scheduling calls of an unready holder, including
first: one non-zero pattern by itself does not prove hangup; important
repeating holder values and a continuous increase in the number of attempts.

Activation is in a separate local marker JAR. Graduation manifest
AgriCraft does not contain diagnostic configuration. Test classes
excluded from all archives; release check rejects the configuration as well,
and its link in manifest.

`starfieldSaveHistoricalTests` additionally restores the previous set
of seven tests, excluding the later added test of ordinary wheat, and puts
their site to the exact initial coordinates **8701193, -59, 11182101**.
The usual `--spawnPos` is not suitable for this: GameTestServer selects separately
random site coordinates. Only the placement/selection of tests changes.

## Repetitions and check limits

All ten completed GameTest processes saved their worlds and exited with code 0:

| Script | Result | Observable generation requests/unloading |
|---|---|---|
| Four new sites, current set | 8/8 in each process | In the last three: 609/10553, 633/10585, 604/10553 |
| Seven previous tests on original coordinates, new world | 7/7 | 595/10526 |
| Repeat write in the same world | 7/7 | 546/9421 |
| Disabling integration and native transformation wild seeds | 2/2 | 516/7880 |
| Powering on and checking from disk, including reload | 7/7 | 561/9426 |
| Copy of existing migration world, original coordinates | 7/7 | 578/12355 |
| New world at the original coordinates, observation from the entrance to stopServer | 7/7 | 575/10526 |

In all observed stops `retries=0`, `generations=0`, `blocked=0`:
no holders not ready for unloading or new generation requests were registered
after the diagnostic stop marker. Last run expands observation
to ServerStopping handlers of other mods, which in the first nine runs
could be executed before our handler. There are also zero indicators.
Measuring overall activity confirms that observers
executed rather than simply returning null values without connection.

Logs: `save-diagnostics-1-fixed.log`, `save-diagnostics-{2,3,4}-write.log`,
`save-historical-1-write-fixed.log`, `save-historical-1-repeat-{write,disabled,read}.log`,
`save-historical-clone-write.log`, `save-historical-entry-write.log`
in `build/starfield-iteration0`.
Errors in connecting the first diagnostic option and historical compilation
filters are corrected before these runs; unsuccessful preparatory launches
are not included in the table and are not considered an initial hang.

The regular installed release JAR is further tested in two independent
processes without diagnostic classes: both executed real `/reload`,
Removed the forced loading of the crop chunk and saved all five
measurements. The full base genotype and UUID matched after rerunning.
Logs: `save-investigation-installed.log` and
`build/starfield-standalone/reload-{write,read}.log`;
Witness: `build/starfield-standalone/reload-result.txt`.

Regular NeoForge/Fabric builds are successful (`save-investigation-build-final.log`),
six current archives 4.0.19 passed the isolation test
(`save-investigation-archives-final.log`; old archives have also been previously checked).
SHA-256 of the NeoForge JAR release remained
`c88f402b99910103675c72e7a4f195462238a8d216e08e6eb4a11f9a7aa1caaf`:
the production artifact of this investigation has not changed.

Current diagnostic profile isolates new worlds and NBT witnesses
from the usual migration site. A copy of the existing world - its current state,
rather than the missing snapshot just before the initial freeze.

Even successful repetitions do not prove the elimination of a rare error: the current
the code is different from the state of the original iteration, and the observation may change
time order. If the incident repeats, a recorded holder and
request source; the profile now collects this data without bypassing saving.

## Play

```powershell
$env:JAVA_HOME = 'C:/Vivoderin/Mod/jdk-21.0.10'
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldMigrationPhase=write -PstarfieldMigrationReloadTests -PstarfieldSaveDiagnostics -PstarfieldSaveHistoricalTests -PstarfieldSaveRun=historical-1 --console=plain
```

The first run creates `neoforge/build/starfield-save-diagnostics-historical-1`;
replay uses the same world. The new world is given a new name, SaveRun.
For disabled/read, the name is preserved, `starfieldMigrationPhase` and
corresponding to `starfieldTestMode`. The full profile of 136 tests is running
with a regular command without a historical filter.

For a regular installed JAR `standalone.py --reload-before-stop`
waits for the actual publication of the catalog after `/reload`, removes the forced
loading the chunk being checked and sends the usual `stop` in two independent
JVM. Compares the full genotype and UUID from the disk. When exit timeout saves
dump threads before completing their test process; time-out is considered
verification error. This is not a full game technique or a prolonged load.
