# Iteration 12 - seed maker and automatic collection

Date: 9 October 2026. Basis: official Starfield Pastoral `0.6.3fix1`, commit `df2151c66a2a9c02855e4db23108e57c422ce78e`, Minecraft 1.21.1 / NeoForge 21.1.226. Continued [iteration 11](ITERATION-11.md).

## Hereditary cycle

Basic hand, scythe and auto harvest products receive a separate component `agricraft:starfield_produce` inside `CUSTOM_DATA`: schema version, original planting species, actual item and full standard AgriCraft genome with both alleles. Native quality, product variations and extraneous components are retained. By-product sunflower seeds, hay and other additional items do not receive the genome of the main product. Grown from seasonal wild seeds, forage retains its original seasonal appearance. Natural forage and giant harvest remain without a hereditary component.

The seed maker still carries out its recipe, deterministic roll, consumption of one product, duration and choice of the number of seeds. The integration processes the **final** plan after both native 3rd party transformation pipelines, before spending the input. If the selected seed item matches the original planting species, the complete genome is transferred. A rare result: ancient fruit seeds, mixed seeds and seeds of another species do not inherit the foreign genome; the number of seeds does not receive bonus gain.

The native resolver has a significant feature: grapes produce summer seeds. The genome of a regular grape crop is not transferred to summer wild seeds. Produce from summer wild seeds, on the other hand, can return its original genome. Special correspondences sweet gem berry → rare seed, unmilled rice → rice shoot and Qi fruit → Qi bean remain native. The ban on the seed maker for coffee bean/fiber is not avoided.

Schema, source species, item correspondence and genome structure are validated. A damaged or unknown legacy component rejects the launch before input is consumed. The product without the component is processed as a normal native product; old stocks and products of the giant do not restore the fictitious parents. Wine, dried fruits and other results from conventional timed machines retain their native properties without the plant genome. Only native inheritance fields are removed from the result, including the case where a third party provider copied them onto an inappropriate item.

The selected seeds and their genome are stored in the present `SeedMakerBlockEntity`. Repeated loading does not recalculate the result. The existing insertion/extraction simulation, partial checkout, and loop completion continue to work with the native quantity and timing.

## Automation

Added generic optional `ExternalHarvestSupport` and `AgriApi.harvest(level, pos, output)`. Regular AgriCraft crops use the same harvest; supported native crops cause true `StardewCropRuntime.harvestForAutomation`. The Create integration overrides further general cutting/replanting for a recognized native plant, including failure due to maturity or unsupported action. The PneumaticCraft and Industrial Foregoing adapters use the same input; their game mechanisms were not launched separately.

The context remains native: `AUTOMATION`, without the player, dummy tool and farming experience. The external AgriCraft/Create input passes farming level 0. Junimo's own and direct Starfield API calls retain the farming level they pass. The native Junimo route accepts common crops with scythe harvesting, including wheat; this is allowing automation, not turning a mechanism into a player with a scythe. The manual requirements of the tool remain the same.

`tryHarvestByJunimo` results are first collected within the general collection boundary. Native code updates the root/both parts and regrow state once, then checks for completeness of the transition. Core products receive the genome and existing gain bonus once, with standard overflow stack sharing. Quality, native random rolls and by-products are preserved. A pre-issue exception restores both parts, the planting record and the native progress, without returning the accumulated results. Immediately re-calling the top or bottom does not reap the second harvest.

The external recipient's contract is to accept each full stack. If an arbitrary recipient has already accepted an item and then throws an exception, it is not possible to rollback their inventory via `Consumer<ItemStack>`. The plant remains collected, the error is passed to the calling code; reuse of the plant for duplication is prohibited. Automatic rollback of arbitrary third-party inventory and compensation for canceled issuances are not claimed.

The native crop API of this version does not collect mature forage; roots and giants require a separate action. For AgriCraft/Create they have an explicit status of `UNSUPPORTED`: general destructive fallback does not run. Unknown/damaged sidecar data and decorative plantings also do not allow such a bypass. The height, calendar, models and rules of the giants remain with Starfield.

## Checks

The full regular enabled profile has passed **120 mandatory GameTest**, including 14 new production/automation checks and two runs of the real `HarvesterMovementBehaviour`. Individual JVM write/reads have passed **the 77 mandatory checks**. At the new checkpoint, the first JVM leaves the real seed maker with the output already selected; the second reads the block/inventory from the world, compares the genome, quantity and absolute term, extracts the seeds once and replants them. The witness does not restore either the device or its items. The native crop/forage/giant checkpoints of the previous stages have also passed.

Restart uses a separate profile without a general watering test: it causes a daily treatment of all plants in the world and, after recording evidence, can advance other control plantings. A full regression run is performed without a restart phase. In a repeatable watering fixture, the native energy of the test player is restored before the paid action; The production cost of the tool does not change. Before the new write of the device, the old fixture block is removed so that the restart does not use the previous incomplete loop.

Profiles `starfieldAutomationTests` and `starfieldCreateTests` are enabled only during development; the second one adds the real Create 6.0.10-280, Ponder 1.0.82, Flywheel 1.0.6 and Registrate MC1.21-1.3.0+67. Isolation of two profile archives has been confirmed. Running instructions are located in [tests/starfield/README.md](../../tests/starfield/README.md); logs - ignored `build/starfield-iteration0/iteration12-*.log`. The official JAR is re-checked for size/SHA-256, 251 source file - according to the Git blob of the pinned commit.

| Check | Result |
|---|---|
| Full enabled profile with real Create, without restart phase | Passed all 120 mandatory GameTest |
| Isolated JVM write/read | Passed 77 mandatory GameTest |
| Starfield installed, integration disabled | 1 mandatory GameTest passed |
| Starfield is missing from NeoForge | 1 mandatory GameTest passed |
| Fabric without Starfield | Passed both mandatory GameTest |
| NeoForge Profile Archives | Isolation of both JARs confirmed |
| Regular NeoForge/Fabric builds | Successful; isolation of five archives confirmed |

`git diff --check` passed. Checks include failure to issue and intentionally thrown exceptions; corresponding rollback/error entries in the negative script logs are expected. Checks for disabled/missing integration confirm inactive common harvest-provider and preservation of original operations via facade; Fabric does not receive NeoForge bridge.

The graphic client, the motion animation of the real structure, and the in-game display of crop sticks have not been manually tested. The mod version remains 4.0.19; publication was not carried out. The next stage is [iteration 13: regression, migrations and release preparation ](../STARFIELD-COMPAT-PLAN.md).
