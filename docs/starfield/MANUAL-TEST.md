# Manual check AgriCraft + Starfield

Prepared 10 October 2026. Regular standalone installation of Minecraft 1.21.1 / NeoForge 21.1.226 with release AgriCraft 4.0.19 and official Starfield 0.6.3fix1. Installed Jade, JEI, Create 6.0.10, Industrial Foregoing 3.6.39 and PneumaticCraft 8.2.21 with their dependencies. Test Java classes and automatic harness are not used.

Updated 13 October: the last three game experiences are completed, the return of the giant's crop sticks to the ground has been fixed. The client has a fixed regular JAR installed; control again RMB/LMB and hold Shift. The working world has returned to spring, the day 11, before a separate seasonal experience; collected rare seeds, crops and materials are preserved. Step by step results and restrictions are in [ordinary gameplay log](GAMEPLAY-ACCEPTANCE.md). The diagnostic tick observer is disabled.

## Login and control

Client: `build/starfield-manual/client`. Re-launch: the local `.cmd` test-client launcher in this folder. World: **AgriCraft + Starfield - manual test**, folder `saves/AgriCraft-Starfield-Test`. Player: `StarfieldTester`; his own farm is located in the Starfield dimension. Prepared 64 plants with specified genes and phases, signed stands A–G, chests, tools, water and bed. Phase and gene preparation serve only as initial conditions; The test result is your actual game action.

Start in survival: this is the only way to check the consumption of seeds and the return of items. The difficulty of the world is easy, inventory is preserved after death; Normal mob spawning is disabled. Native Starfield crows can still attack crops at night. Deluxe scarecrows 20079/30/20075 and 20102/30/20075 have been added to the stands. To build cars, you can temporarily turn on creative mode.

| Chat command | Action |
| --- | --- |
| `/function manual:home` | Return to start: approximately 20068, 30, 20073 in Starfield. |
| `/gamemode creative` / `/gamemode survival` | Switch mode. |
| `/function manual:water` | Wet the native regular soil on the stands without promoting growth. |
| `/function manual:fast` | Speed up Starfield's own clock to ×100 to await the change of day. |
| `/function manual:normal` | Return Starfield clock to ×1. |
| `/stardew timespeed 0.1` | Slow down the clock for a quiet inspection. This is the initial world setting. |
| `/function manual:far` | Go to a safe site more than 1000 blocks away to check unloading. |

Speeding up the clock affects the entire native world: check the calendar in the HUD and do not leave it on for long. Water the crops before the next day. Starfield has its own calendar; the normal Minecraft command `/time set` is not a daily growth checker. Bone meal also does not replace the natural day change. After completing the fast experience, return to normal or slow speed.

Do not run `/function manual:setup` again: this technical function rebuilds stands and removes changes within them.

## Basic checks

1. **A - Planting and Material.** Plant potatoes in empty wooden, iron and obsidian single crop sticks. The seed must be consumed one at a time, the plant must appear with the appropriate model. On an already growing potato, remove the material via Shift + RMB with an empty hand: one item will return, the plant itself and its genes will be preserved. Attach other material. Clicking the same material again should not consume another item. Try planting a seed in an occupied space: without losing the seed.

2. **B - harvest and tall plants.** Potatoes, beans, cauliflower and coffee are already mature. First examine them with a magnifying glass, then collect them using real native action. After harvesting potatoes once, there should be empty crop sticks left. The beans should remain alive with the same material and proceed to ripen again. For tall beans/coffee, try inspecting, attaching and removing material along the top: this is one planting, no double return. Repeat the harvest immediately - there should not be a second harvest. For complete destruction, use a separate plant: the material is returned once.

3. **C - Magnifying glass, Jade and Analyzer.** Compare the information of Magnifying glass and Jade on the same plant. On the left three potatoes, all characteristics are equal to 1; on the right, growth and gain are equal to 10. Nearby are an analyzer and a seed maker. Insert the native seed into the analyzer, wait for the analysis, take it out and plant it: the object remains native, the data is not lost. For a quick check, use Shift + RMB with the seed on the analyzer. The interface text should be readable without raw translation keys.

4. **B/A - Spade transfer.** Record the material, genes and maturity of the tall bean. RMB the plant with a trowel, then the suitable loose soil. Both parts, material and progress must be transferred together. First, try placing it in a occupied place: the plant should remain on the trowel. Save peace with the plant on the instrument and after re-entering, complete the transfer.

5. **C - height and strength.** Water the stand before changing the day. Compare the same potatoes with growth 1 and 10: increased growth should give an advantage in the native phases. On two separate areas of poor soil, compare the strength of 1 and 10: the maximum strength removes the delay from the poor soil. The magnifying glass shows the phase and the remaining days; bone meal click should not independently promote native crop. The exact amount of harvest is random, so compare gain across several plants, not just one harvest.

6. **D - selection.** On each mini-plot there are two mature potatoes with high fertility and empty double crop sticks between them. Let the area work for a few minutes, check the appearance of the offspring and its genes. Do not plant seeds in double crop sticks by hand. The descendant has the same type of potato; There is no special test recipe for mutation in this world. The appearance of a child is random; one short pause does not prove a failure.

7. **E - Irrigation.** Inspect the tank, channels and overhead sprinkler. Water must be consumed, the native soil underneath must be moistened. Irrigation alone should not advance the Starfield phase. Remove water from the tank or disconnect the channel: the sprinkler will stop working after using up its small buffer. Reconnect the connection and fill the tank from the bucket. The water source is located by the bed.

8. **C - inheritance through the seed maker.** Collect potatoes with known increased genes in stand B, feed this particular crop into the native seed maker and wait for the result. If the solution is potato seeds, check them with an analyzer and planting: both alleles should be preserved. Rare mixed/ancient or seeds of another species should not receive a foreign genome. Test harvested coffee beans by planting them directly; the seminal apparatus does not accept them natively.

9. **F - former AgriCraft.** Plant regular wheat seeds on empty crop sticks over vanilla soil, check analyzer, magnifying glass, growth, collection and transfer. There is ripe vanilla wheat nearby. In the Starfield dimension, bone meal is banned by its own processor for all plants. To check the usual fertilizer, use the prepared platform of the Upper World: 20093/101/20093, analyzer 20095/101/20093. First, go to an already loaded area in a safe way; do not start rebuilding the site after planting.

10. **Saving and unloading.** Record the genes, material and phase of one intact plant. Exit to the main menu and open the world again: the data and appearance should match. To unload, run `/function manual:far`, wait 10 seconds and return after `/function manual:home`: the material should appear correctly, without the old model or missing fit. Forced loading of test chunks has been removed. After inspection, run `/reload` and re-inspect the same fit.

## Additional checks

- **G is giant.** Prepared ripe cauliflower 3×3. Water it before the next native days. When a giant is formed, the materials must return once; there should be no arbitrary parent genome when it is collected. The chance remains the original, about 1%; the absence of a giant for a few days is normal. Strength does not increase this chance.
- **Cars.** There are spare sets left at F. Already assembled and tested: IF Plant Gatherer 20082/30/20073 with a generator nearby; Create with bearing 20145/29/20135 and coffee 20145/30/20133; drone with route along A75, B88/70 and C97/68, unloading into chest 20100/30/20078. After harvest, crops undergo normal regrowth; a repeated pass does not duplicate the harvest. The Create rotating chest contains products with genes, the drone chest contains beans, coffee and potatoes. Do not restart the preparation functions of these mechanisms: they change the original blocks of the stand. The actual results are recorded in `GAMEPLAY-ACCEPTANCE.md`.
- **Seasonal resistance.** On C there are potatoes with resistance 10. The test requires a true change of season: it must survive three days after the end of the season with suspended growth. Do not change season until major spring inspections have been completed.

## What to write down when there is an error

Stand letter, action, game mode, Starfield date, expected and actual result. For items - quantity before/after; for plants - a magnifying glass and coordinates. Screenshot - F2. The installation log is located in `build/starfield-manual/client/logs/latest.log`; upon departure, the report will be in `crash-reports`. Do not rebuild the problem area until disassembled.
