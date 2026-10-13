# AgriCraft and Starfield Pastoral integration plan

Date: 7 October 2026. Updated: 10 October 2026. Status: iterations 0–12 completed in the server scope recorded at those milestones. The server part of iteration 13 has been tested: 125 tests and three-process migration. The remaining in-game placement/display of crop sticks has been implemented; the complete server profile has passed 136 checks. Additional client acceptance, standalone installation, non-Starfield handler disabling, and shutdown investigation are reflected in [completion report](starfield/ACCEPTANCE.md).

## Progress

- Iteration 13: fixed height offset when migrating old plantings; SavedData index by dimension/chunk, one-time migration of a loaded chunk, saving dormant wild-seed genome when disconnected, checking a large saved farm/regular players and three-process migration. [Report and limitations](starfield/ITERATION-13.md), [guideline](starfield/USER-GUIDE.md).
- Game placement and models are implemented; real client tests and separate installation of release JAR passed. Integration included in release 4.0.20; The current changelog is in [release description](releases/4.0.20.md). The previous single stop failure has not been reproduced; By the user's decision dated 10 October 2026, it was excluded from the release blockers, the investigation is resumed only if it is repeated. The cause has not been established. Iteration 14 canceled by user 9 October 2026; the strength bonus to the chance of giants is excluded from scope.

- Official Starfield `0.6.3fix1` and commit `df2151c66a2a9c02855e4db23108e57c422ce78e` are pinned; the JAR is validated against SHA-256.
- A reproducible [catalog 50 planting species](starfield/CROP-CATALOG.md) has been compiled: 46 source crops and four seasonal species of wild seeds.
- Added a separate diagnostic scheduler prototype to `tests/starfield`, included only for NeoForge development.
- Representation selected: real Starfield block, separate SavedData genome/crop sticks, single native scheduler and growth executor.
- All ten GameTests ran in two separate JVMs: four species cycles, regrowth, dry day, tall-crop geometry, giant, preserve genome/UUID/phase/date, and prevent repeated-day processing after a real restart.
- The limits of the current API have been revealed: the adapter must execute the phases/collection itself, giants prohibit the replacement of block entities, the seed resolver does not pass stack components.
- The result and checks are recorded in [iteration 0 specification](starfield/ITERATION-0-SPEC.md); prepared [proposal for narrow API hooks](starfield/API-EXTENSION-PROPOSAL.md).
- The architectural milestone of iteration 0 has been passed. At this point, game placement/model of crop sticks, tools, selection and gene effects are not yet connected.
- Iteration 1: added `compat.starfield`, version checking before loading native bridge and catalog of all 50 types using public native accessors/API. A manually copied table is not executed as a growth algorithm.
- Tested real server runs with support enabled/disabled, without Starfield in NeoForge and on Fabric. Reload Data Maps preserves planting identity and progress: the invalid species is excluded from the catalog with diagnostics and restored after correction.
- Loading and catalog details - [iteration result 1](starfield/ITERATION-1.md).
- Iteration 2: native seeds retain the standard genome and foreign components; production SavedData indexes planting by dimension/root/species/UUID. The existing trowel transfers both parts and full native progress; refusal and cancellation of placement keep the plant on the instrument.
- All 13 GameTest passed in two separate JVM write/read: genome, UUID, seed components, phase and processed day are restored from disk without regrowth. Disabled support, lack of Starfield, regular builds and archive isolation have been tested.
- Client cache and network codec added and verified without a graphical client. Game staging/model crop sticks and other tools are not yet connected. [Iteration result 2](starfield/ITERATION-2.md).
- Iteration 3: eight new checks confirm the same native daily growth of paired plantings, water and soil, historical season and farm benefit, fertilizers, protection against growth through random ticks and bone meal, real chunk unloading and restoration of missed days, full restart. The growth executor remains only in Starfield; no gene bonuses added.
- Passed the usual run from 21 test and 20 tests in separate JVM write/read. The global daily passage is checked separately from the control plantings left for restart. NeoForge/Fabric builds and isolation of five regular archives are successful. [Result and boundaries of iteration 3](starfield/ITERATION-3.md).
- Iteration 4: The original hand harvest and Starfield scythe maintain quality, quantity, farming experience, rights and reharvest timing. Canceled or throwing item creation rolls back the plant, progress and partially published harvest; Repeated clicks and nested harvests do not duplicate products. AgriCraft tools no longer trigger random manual harvesting.
- All 29 GameTest of the joint profile passed; Disabled integration, absence of Starfield on NeoForge and loading Fabric were dealt with separately. Regular builds and checking of five archives are successful. Block destruction and automation remain separate native paths, game placement/model of crop sticks is still to come. [Result and iteration boundaries 4](starfield/ITERATION-4.md).
- Iteration 5: the analyzer reads real seeds of all 50 species, preserving the genome and components. Magnifying glass, Jade and The One Probe receive a common server report on native phases, conditional remaining days, water, seasons and genes. Menu, direct block actions and trowel check the current status and rights; rakes, shears and seed bag safely fail unsupported actions.
- All 38 GameTest of the joint profile passed; Disabled integration, absence of Starfield on NeoForge and loading Fabric were dealt with separately. Regular builds and isolation of five archives are successful. Virtual Starfield conversion to `AgriSeedItem` is not supported to preserve the native executor. Graphical verification of the client interface remains separate. [Result and boundaries of iteration 5](starfield/ITERATION-5.md).
- Iteration 6: the standard sprinkler AgriCraft, through a water-budgeted sweep, waters native soil and allows both parts of the tall crop to pass through without accidental growth. The prohibitions of sandy/poor soil, water flow, valves, obstructions and native holding fertilizer remain; overlapping sweeps do not add calendar days. Fixed marking of water buffer and bypass position for saving during continuous operation.
- All 47 GameTest of the joint profile passed; The disabled integration, the absence of Starfield on NeoForge, and two Fabric tests, including the previous watering/growth of wheat, were tested separately. Regular builds and isolation of five archives are successful. Watering all 50 species does not change genome/UUID/progress; Potatoes and re-harvesting corn maintain native dates. Special giant shapes and crop sticks game model remain ahead. [Result and iteration boundaries 6](starfield/ITERATION-6.md).

- Iteration 7: empty double crop sticks create true native offspring from mature horizontal Starfield roots in the bed or with sidecar-crop sticks. The standard AgriCraft engine performs selection for fertility, cloning and mutation of both alleles; explicit native mutation recipes are involved in the complexity and dominance of genes without virtual plants for GUI. Selection preserves the progress of the parents; the child receives a new UUID and an initial native state. Tested all 50 species, season/soil/location, rollback/exception and regular wheat. Details - [iteration result 7](starfield/ITERATION-7.md).

- All 57 GameTest profile iterations 7 passed; Disabled integration, absence of Starfield, both Fabric tests and regular builds are separately successful. The isolation of two NeoForge archives after a test profile and five regular archives was checked. Child recovery at this stage is verified via SavedData NBT; this is not new evidence of two-process restarting.

- Iteration 8: General rules module normalizes existing growth/gain to configured limits. Minimum preserves native behavior; the maximum cuts the positive phases/regrowth to half, rounded up, and doubles the primary hand/scythe produce. Quality, by-products, experience and native water/seasons are preserved; the stack overflow is split, failed output publication is rolled back. The term in the shared inspection hints takes into account the same gene. [Formulas, table and iteration results 8](starfield/ITERATION-8.md).
- All 66 GameTest of the joint profile passed. A separate JVM write/read pair passed 25 checks: genetic phases and reharvest counter were loaded from disk, the processed day is not repeated, the next day decreases the saved counter by exactly one. Growth tested for all 50 species, including native conversion of four wild seeds to forage. At this point, the conservation of genome special forms was postponed until the stage 10, strength/resistance - until the stage 9. The complete issuance of automation products remains a stage 12, the crop sticks model is a subsequent task.
- The final individual harvest profile passed all 18 checks, including maintaining a sequence of random numbers when splitting the stack. Successful disabled/absent, both Fabric tests, regular NeoForge/Fabric builds, isolation of two profile and five regular archives. Release outputs are cleared of test classes and probe resources; version/publication remains stage 13.

- Iteration 9: strength recovers a fraction of native growth skips on infertile soil without a second executor and the consumption of world random numbers. Resistance gives up to three calendar days after the end of the eligible season, with growth stopping/regrow and a new giant being banned; mature collection and native seasonal exceptions are preserved. The forecast takes into account the strength, general tips show the remainder of the deferment. All six genes are involved in cultivation or selection. [Iteration balance and checks 9](starfield/ITERATION-9.md).
- The full profile has passed 78 checks; separate JVM write/read - according to 37. Real saved files retain the fractional soil residue, genome and calendar delay period, the repeated day is not executed. Disabled/absent, Fabric and isolation of specialized/regular archives have been confirmed. The next stage is tall, trellis and special crops; crop sticks model and publication remain ahead.

- Iteration 10: checked all 37 multipart species, their native states/models, top collection, common root and deletion upon loss of support; separately - watering/acceleration of rice and taro, fiber's dry growth, scythe requirement and quality and cactus limitations. The transfer and selection of the cactus tests the original rule of place. Wild seeds retain the genome/UUID/crop sticks material on the native forage form; the mature form supports general reporting, transfer, selection of the original species and one-time collection with genome gain and rollback. Natural forage does not receive a genome. [Result and boundaries of iteration 10](starfield/ITERATION-10.md).
- All 90 GameTest of the joint profile passed; individual JVM write/read passed 49 checks. Mature forage retains its shape and original fit on the disc without daily registration. Tested with disabled integration, lack of Starfield on NeoForge, and both Fabric tests. Regular builds of both platforms are successful; the isolation of two specialized and five regular archives has been confirmed. The graphical client has not been manually tested, the crop sticks model remains a follow-up task.

- Iteration 11: all five native giants retain the original 1% chance, conditions 3×3, watered mature trigger, allowed immature neighbors, model/height and farm slot border. A successful formation returns each attached crop sticks material once; cancellation, exception and nested release restore both parts, genome, UUID and native progress/queue, without rerolling on the same day. The collection remains native, without assigning genome products to a random parent and without a general gain bonus. Damaged/future entries are not consumed. [Iteration rules and checks 11](starfield/ITERATION-11.md).
- All 104 GameTest of the full profile passed; real individual JVM write/read passed 63 checks. The giant's footprint and three stacks of return crop sticks with the same UUID are saved on the disk, the old nine plantings/scheduler records are missing, re-formation does not produce materials. Tested disabled/absent on NeoForge, both Fabric tests, regular builds of both platforms, and isolation of two profile/five regular archives. Game staging/crop sticks model and graphical verification remain downstream tasks; publication was not performed.

- Iteration 12: the main product retains both alleles and the original species; the real seed maker transfers them only to the corresponding seeds after native selection and third-party plan transformations. Quantity, timing, simulation, partial release and rare/mixed/foreign seeds remain native; unsuitable artisan products do not receive genome. AgriCraft/Create use a single native automation context without a dummy player, tool and agricultural experience, with a one-time gain and multipart/regrow transition. Unsupported forage/giants reject destructive fallback. [Behaviour, checks and iteration boundaries 12](starfield/ITERATION-12.md). The complete profile with this Create has passed 120 mandatory GameTest; separate JVM write/read - by 77. Tested disabled/absent, both Fabric tests, builds of both platforms, and isolation of two profile/five regular archives.

## Purpose and rules

A Starfield crop with the same genome should maintain the same growth and yield rules on the native bed and on AgriCraft crop sticks. Crop sticks add native AgriCraft selection, tools and interactions; they do not transform the plant into a crop with a different calendar.

- Starfield controls the calendar, phases, daily watering, seasons, fertilizers, product quality, re-harvest, species characteristics and conditions for assembling giants.
- AgriCraft manages six existing genes, inheritance, crossbreeding on double crop sticks, analyzer, magnifying glass and irrigation system.
- Integration links systems, stores missing data and applies genes to Starfield parameters. It does not run two independent growth algorithms for one planting.
- Random teak, irrigation, bone meal and automatic harvesting should not create additional calendar days or bypass crop restrictions.
- Gene effects for Starfield are the same for both planting methods. Additional interactions of crop sticks, for example with weeds, do not replace the daily rules of the crop.
- Influence is limited to supported Starfield crops. Vanilla crops and other AgriCraft integrations retain the same behavior.
- We use existing characteristics and effective limits from the AgriCraft configuration. We are not adding seven alien patch traits, global 20/30 limits, a gene editor, special interdimensional mutations, or new species recipes.
- Starfield dimension limitation is maintained the same for beds and crop sticks. Growing beyond dimension is a separate future task.
- Harvest quality and processing initially retain Starfield rules. The gain gene increases the number, and not automatically the star rating.

Consensus general meaning of genes:

| Gene | Action |
|---|---|
| Growth | Reduction of calendar periods for initial and regrowth |
| Gain | Additional quantity of products upon successful collection |
| Strength | Reducing the penalty of native infertile soil; does not negate the suitability of the soil and watering |
| Resistance | Limited ability to survive the end of the season; out of season growth stops |
| Fertility | Success of full-time crossbreeding on crop sticks |
| Mutability | Changes in heritable characteristics during routine selection |

The exact coefficients are design values, and not an already approved balance. Before their implementation, we fix the formulas and examples in the specification file. The base genome should produce native behavior without additional benefits. Normalization takes into account the configured minimums and maximums, including the case of equal boundaries. Maximum genes do not mean instant ripening, guaranteed endless harvest, or immortality out of season.

## Architecture and material for work

The first target option is optional integration into the NeoForge part of the current AgriCraft for Minecraft 1.21.1. Common contains only the necessary extension points and independent types; Fabric does not receive direct references to Starfield classes. Working without Starfield installed and with integration disabled is required.

As a basis for the study, we take the already verified official API: `StardewCropRuntime`, `StardewCropRuntimeAdapter`, `StardewCropDailyContext`, collection contexts, crop data and daily scheduler. At the time of research, local AgriCraft is 4.0.19; Starfield's official initial commit is `df2151c66a2a9c02855e4db23108e57c422ce78e`. At the start of implementation, we select and fix the corresponding official release assembly and its dependencies. The sent archive does not serve as a source of running dependencies.

Representation tested in iteration 0: a real Starfield block remains on both the regular bed and the crop sticks. Genome and the presence of crop sticks are stored separately; crop sticks will be an additional display and interaction for AgriCraft. Native phases and dates belong to `CropGrowthManager`. AgriCraft frontend lookup, client view and sidecar lifecycle are added in future iterations.

The initial assumption “AgriCraft plant + runtime adapter” is rejected: the API requires the adapter to execute its own phases, and the giants prohibit the replacement of block entities. The public API is not considered a universal algorithm for other people's blocks. The chosen preservation of the native block bypasses these obstacles without second growth. Dimension substitution, temporary block substitution, and copying of a large proprietary internal algorithm are not a fallback solution.

We use someone else's patch as a reference book for IDs and errors. We do not transfer its mixin hooks entirely. Before borrowing code, we find out the permission of the author; its metadata says `All Rights Reserved`. Publishing other people's resources or modified Starfield assemblies also requires a separate check of the conditions. Links to official models are preferable to copying resources.

## Iterations

Each iteration is a separate completed subfeature with minor changes and testing. We start the next dependent subfeature after its readiness criteria. Partial support is clearly noted and is not presented as preservation of all mechanics.

### 0. Architecture verification and behavior baseline

**Result:** technical specification, crop list and reproducible minimum prototype of daily update on crop sticks.

- Fix versions of official dependencies and integration conditions, check the availability of the necessary APIs and hooks for watering, growth, collection and seed maker.
- Create a table of supported species: seeds, products, `phaseDays`, seasons, special soil, re-harvest, geometry, tools and giants. 46 crops from the studied patch are the starting point; check them with the official version and explicitly take into account additional species discovered.
- Select test representatives: potatoes, one repeat crop, one tall crop and one giant crop.
- Prove that scheduler Starfield sees the plant on crop sticks, transmits the correct day/watering and can survive saving and rebooting.
- Describe which rules can be reused, where an extension hook is needed, and approve the way to represent the plant before expanding to all species.

**Done when:** in the prototype, one daily call changes one planting once, and the saved state is restored. There is no second independent growth and no need to run someone else's build. If the architecture does not pass the test, we continue this iteration rather than masking the problem with subsequent features.

**Dependencies:** none. This is the main technical milestone of the project.

### 1. Optional loading and catalog of crops

**Status:** completed 8 October 2026. [Implementation and testing](starfield/ITERATION-1.md).

**Result:** The integration recognizes crops and suitable native soils, but does not yet promise a full game loop.

- Add an integration switch and safe loading if you have a compatible Starfield.
- Compare species, seeds and harvests with the data of the official mod; use its data/API where possible instead of a second manually divergent table.
- Separate the type data and the state of a specific planting. The visual stage is not counted by the number of days that have passed.
- When changing the datapack, check the data without destroying existing saves; unknown ID is not silently converted to another species.

**Ready when:** AgriCraft launches without Starfield, with integration disabled and on Fabric; With support enabled, all declared types and IDs are verified. Erroneous data provides clear diagnostics.

**Dependencies:** 0.

### 2. Seed Genome and Planting Conservation

**Status:** completed 8 October 2026. [Implementation, supported porting, and test limits](starfield/ITERATION-2.md).

**Result:** seed, plant and backseed retain the same heredity.

- Use the standard AgriCraft genome and store it on native seeds while preserving foreign components, quality and data.
- For ordinary seeds and old plantings, assign a stable base genome. Re-reading or rebooting should not inadvertently improve performance.
- Index the native planting by root, dimension and crop identity. For crop sticks, save the genome along with phase, progress days, regrowth and treated day.
- Provide versions of saved data, cleaning when deleting a plant and saving when adding/removing crop sticks. If changing the planting method is unacceptable for a given state, refuse until the world changes.
- Link the client cache to the world, dimension and root; update after change and clear on exit.

**Done when:** genes and progress are not lost when planted, carried by a supported tool, saved, restarted, or changed view; old plants are not accidentally deleted or overridden.

**Dependencies:** 1.

### 3. Single daily growth without genetic bonuses

**Status:** completed 8 October 2026. [Native executor, pair checks and game representation boundaries](starfield/ITERATION-3.md).

**Result:** The basic single-block crop grows equally on the bed and on crop sticks.

- Connect both views to the same Starfield day semantics, individual `phaseDays`, soil suitability, season and watering.
- Precisely limit the shutdown of regular random growth to only the corresponding planted crop. Empty double crop sticks, weeds and other plants do not lose treatment.
- Preserve native seasonal exceptions and effects of fertilizers. Data from a historical day should be taken from context, not from the current global season.
- Save processing of missed game days and do not re-accrue the day after loading a chunk or restart.
- Do not allow bone meal and other third-party accelerators to bypass the selected rules; an unsupported action is not explicitly applied.

**Ready when:** two identical plantings with a base genome, the same soil and watering mature on the same day; without water they cost the same; When the season changes, they react the same way. Tests include one or more missed days and an unloaded chunk.

**Dependencies:** 2. Until this milestone, gene bonuses will not be realized.

### 4. Harvest, quality and re-fruiting

**Result:** The plant's complete basic cycle corresponds to Starfield in both views.

- Use Starfield context for player, tool, farm level, fertilizer, quality and experience. Do not release products until the operation is successfully completed.
- Maintain a distinction between a disposable crop and a re-bearing crop; recovery occurs through its `regrowDays` rather than a fixed number of visual stages.
- Coordinate harvesting by hand, scythe and AgriCraft tools, tool strength and farm permits.
- Handling full inventory and harvest errors should not produce duplicates or remove the plant before the operation is completed.

**Done when:** the potatoes are harvested once, the re-fruiting species ripens again on the right day, the right tool is a must and star quality/experience works the same on the bed and crop sticks. Repeated clicks do not duplicate the harvest.

**Dependencies:** 3.

### 5. Analyzer, magnifying glass and tools

**Status:** completed 8 October 2026. [Implementation, checks and limitations of seed conversion](starfield/ITERATION-5.md).

**Result:** the player sees the real genome and the state of the crop and can safely use AgriCraft tools.

- The analyzer reads native seeds and saves the genome with supported conversion to and from AgriCraft seeds.
- Magnifying glass shows phase, remaining days, maturity, water, season and genes without fictitious percentage progress from the same eight stages.
- Check the trowel, rake, scissors and seed bag. For each action, describe the applicability; if the action violates the native cycle, it is correct to refuse.
- Jade/other already supported tooltips get the same state; there is no mandatory dependence on them.
- All changes are confirmed by the server: distance, open menu, rights and current boarding identity. We are not adding a gene editor.

**Done when:** the information is the same on the client and server, does not become out of date after changing measurements, and the tools do not bypass collection deadlines and restrictions and do not lose the genome.

**Dependencies:** 4.

### 6. AgriCraft irrigation as daily watering

**Status:** completed 8 October 2026. [Implementation, testing and support boundaries](starfield/ITERATION-6.md).

**Result:** The channel and sprinkler system caters for both planting methods.

- Bring water to the native soil/irrigation state that Starfield uses at the end of the day.
- Use existing water flow, area of effect and obstacles of AgriCraft; do not give free watering through integration.
- Take into account all soil positions of a multi-block plant and native water retention by fertilizers.
- Eliminate second acceleration via call `randomTick`. Frequent watering on the same day does not accrue additional growth days.

**Ready when:** manual watering, native sprinkler and AgriCraft give the same daily results; an empty system does not water, obstacles work, several sprinklers do not speed up the calendar.

**Dependencies:** 3–5. After this iteration, an internal preview of basic compatibility is possible for already tested simple crops; This is not a full release yet.

### 7. Regular crossbreeding and inheritance

**Result:** Starfield crops are selected for selection on AgriCraft double crop sticks.

- Connect the mature parents of both views if they are arranged according to the usual AgriCraft rules and are uniquely recognized by the root.
- Use standard inheritance rules and existing fertility/mutativity. Do not add a periodic complete bypass of all plants and the loss of offspring next to the bed.
- Do not consider parts of the same high crop to be different parents. Do not cancel processing of empty double crop sticks.
- By default, support breeding within a species; interspecific emergence of new species is allowed only according to explicitly defined existing mutation recipes, without arbitrary mixing of species.
- The descendant receives the native conditions of its own species and the usual starting state; does not inherit the maturity, water and used fertilizers of its parents.

**Ready when:** double crop sticks produce correct offspring with inherited genes; increased fertility/mutativity affects regular selection; other plants and integrations have not changed.

**Dependencies:** 2–5. The tooltip should reflect the actually connected effects. After iteration 9, all six genes are involved in cultivation or selection.

### 8. Growth and gain genes

**Result:** The first two general benefits work on the bed and crop sticks.

- Place the formulas in the general module of rules and check them on tables of examples from the base to the maximum gene.
- The growth gene influences the calendar timing of phases and regrowth, in accordance with the native fertilizer. Do not change the deadlines again every day and do not apply the bonus twice after saving.
- With a minimum gene, maintain the original terms; provide lower limits for positive phases and re-harvest. Process native zero phases/service phases according to the original rules.
- Apply the gain gene once to a successfully formed crop, maintaining its composition, quality, special additional drops and stack size restrictions.
- Do not award bonuses when you remove a plant, fail to harvest, or imitate an automatic action.

**Done when:** high genes provide a predictable advantage that is the same in both views; basic values are the same as Starfield; fertilizer and genes are added according to a fixed formula without instantaneous cycles.

**Dependencies:** 4, 7.

### 9. Soil strength and resistance to changing seasons

**Result:** the remaining two genes have an understandable effect in native agriculture.

- Strength reduces stunting on infertile soil through its actual rule. Does not override daily water, season, and soil block suitability.
- On crop sticks, do not apply the same penalty or bonus a second time through the standard AgriCraft conditions.
- Resistance gives a limited lifespan after the end of the appropriate season. Fixed balance: base gene - initial behavior, maximum - up to three additional game days; The formula and table are given in [iteration specification 9](starfield/ITERATION-9.md).
- Out of season, growth and regrowth are stopped; the mature crop can be harvested during the delay. In protected areas and for species that ignore the seasons, a delay is not necessary.
- The countdown is tied to the historical day of the season transition. Restarting, removing/adding crop sticks and changing views do not reset it.
- If a random soil rule is necessary, ensure reproducibility of processing missed days, without re-rolling the beneficial result when loading.

**Done when:** the same genes give the same response to poor soil and season, all six characteristics are useful in cultivation or breeding, and high genes do not allow year-round endless growth.

**Dependencies:** 3, 7–8.

### 10. Tall, trellis and specialty crops

**Result:** uniform behavior extends to crops with complex geometries and special conditions.

- Reconcile root, top and accessory parts, free space checks, soil positions, patterns and maturity with official species data.
- Each planting can be updated and collected once, regardless of the part selected by the player.
- Planting, adding/removing crop sticks, destroying the support and removing the plant are carried out consistently for all parts.
- Check for special view conditions identified in iteration 0 without replacing them with a general rules table.

**Done when:** the high view looks the same and goes through the entire cycle in both views; the upper part does not produce a second harvest; partial destruction does not leave an invisible genome or scheduler entry. All declared types have passed their testing.

**Dependencies:** 4–9.

### 11. Giant crops preserving native rules

**Result:** supported species are capable of forming giants even in the presence of crop sticks.

- Save official valid species, starting root maturity, planting conditions 3×3, calendar check and initial probability at base genome. In the tested version, the remaining eight roots may be immature if their native production is the same.
- On crop sticks, provide the native mechanism with correct information about the types and maturity of all nine plants; do not redefine the global chance for all crops.
- Before replacing, check the entire area. Only after successful formation, delete nine plantings, their saved states and crop sticks. Return the crop sticks material once in the usual way, without duplicating it when called again.
- Harvesting a giant uses its tool and native harvest. The genome rule for returnable seeds/products should be explicitly stated; do not randomly select a gene from one of the nine crops.
- For old plantings without an explicit genome entry, use a stable baseline; they retain the native possibility of forming a giant.

**Ready when:** control plantings on the bed and crop sticks have the same basic formation condition; a giant does not appear when the grid is incomplete; state and crop sticks are not lost or duplicated.

**Dependencies:** 9–10.

### 12. Seed maker and automatic collection

**Result:** heredity and harvest rules are maintained throughout the full production cycle.

- Transfer the genome from the product to the appropriate seeds of the seed maker; take into account native exceptions when the device produces a different species. Do not transfer the gene of one species to the seeds of another.
- Do not multiply the native seed yield with a new separate trait. Processing of wine, dried fruit and other goods maintains the original quality rules and does not transfer genome to unsuitable items.
- Test AgriCraft automatic collection and existing Create integration through a single native context. Do not attribute player rights/tools to the automation and do not award farming experience that Starfield does not give it.
- The conditions of the special instrument are preserved; if the API is insufficient, the automation receives an explicit refusal for the corresponding type, rather than a free bypass.
- Perform one harvest and one state mutation per logical plant, including multipart crops. The re-harvest is not harvested at the same time after the stage is reset.

**Ready when:** the cycle “seed → plant → harvest → seed maker → seed” stores the correct genome; manual and automatic harvesting do not duplicate products, do not bypass ripening, and respect native contextual differences.

**Dependencies:** 7–11. If the native API does not allow the safe collection of a specific crop by automation, this is a documented limit, and not a reason to change its mechanics.

### 13. Regressions, migration and release

**Result:** ready-made optional integration with the described support limits.

- Go through the matrix below on the official dependencies in a separate test installation and dedicated server.
- Check that integration is disabled: do not silently delete native plants, do not transform incompatible plantings into another species, describe the behavior of saved seeds and crop sticks.
- Check updating the existing world, saving genes and delaying the season, reloading the datapack, changing dimensions and multiple players.
- Check a farm with a large number of plants: bypassing only the necessary plantings, the absence of unintentional loading chunks and periodic full scanning of worlds.
- Add documentation about growth owner, six genes, their formulas, common/special crops, irrigation, tools and automation; update changelog.
- Before release, double-check compatible versions and terms of use of materials. Publishing and changing the release version is a separate stage after the code is ready.

**Done when:** mandatory scenarios pass, support boundaries are clear, the base crop maintains the same behavior in the bed and crop sticks, existing integrations have not regressed.

**Dependencies:** 0–12.

## Checks and order of execution

Default order: **0 → 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → 10 → 11 → 12 → 13**. There is no additional step 14.

For each subfeature we use checks that confirm behavior: tests of formulas and state transitions, integration checks of the world/scheduler and game scenarios where blocks, tools and network are important. We do not replace calendar checking with successful compilation.

| Region | Required Scripts |
|---|---|
| Loading | Starfield missing, integration disabled, incompatible version, Fabric, NeoForge, dedicated server |
| Submission | Native bed and crop sticks; same species, genome, soil, water and historical day |
| Calendar | Normal day, a few missed days, end of season, seasonal exception, restart, unloaded chunk |
| Genes | Baseline, maximums, custom limits, immutativity of vanilla crops and other compat packs |
| Breeding | Empty double crop sticks, mature/immature parents, inheritance, parts of one plant, no new species without a prescription |
| Harvest | One-time and repeat, quality, experience, right tool, failed collection, few clicks, automatic context |
| Geometry | Top, lack of space, support destruction, collection/removal, giant 3×3 and return crop sticks |
| Data and Network | Old seeds/plantings, saving NBT/components, changing world/dimension, outdated cache, regular player without administrative rights |
| Productivity | Large farm, multiple dimensions, multiple players, no unnecessary loading chunks |

The plan does not set a fixed project deadline: iteration 0 tests the riskiest architecture first. Its result allows you to evaluate the remaining work much more accurately.

## Material from previous research

Detailed audit of someone else's project: `C:/Vivoderin/Mod/compat-review/starfield-2026-10-07/VERDICT-RU.md`.

The scheme chosen here clarifies and replaces the recommendation of that audit about the possible simplified cultivation according to AgriCraft rules. In this regard, we preserve Starfield farming in both representations and adapt existing genes to it.
