# Iteration 9 - soil strength and seasonal resistance

Date: 8 October 2026. Base: official Starfield Pastoral `0.6.3fix1`, commit `df2151c66a2a9c02855e4db23108e57c422ce78e`, Minecraft 1.21.1 / NeoForge 21.1.226. Continued [iteration 8](ITERATION-8.md).

## Fixed balance

The existing genome characteristics, both alleles and the standard value of the phenotype are used. Normalizing to the configured limits retains the formula `t = clamp((value − min) / (max − min), 0, 1)`; equal or opposite limits give zero effect. There are no new characteristics or a separate genome.

| Effect | Minimum | Intermediate value | Maximum |
|---|---|---|---|
| Strength on infertile soil | Starfield's original penalty | Returns the fraction of `t` missed watered days | All watered days pass without soil penalty |
| Resistance | Initial seasonal mortality | `floor(3 × t)` deferment days | Three calendar days of delay |

With standard limits 1–10 resistance gives, respectively, **0, 0, 0, 1, 1, 1, 2, 2, 2, 3** days. The window includes the first day of the inappropriate season: at the maximum, potatoes survive 1–3 summer and die when treated 4 summer. Dry days also use up this window; it is not counted in watered days.

## Strength and the sole native executor

Native `CropGrowthState.advanceOnSoil` on infertile soil adds two to fractional progress and allows a step upon reaching three, subtracting three. Therefore, the original crop grows on approximately two out of three watered days. The integration calls this method exactly once and retains its fractional remainder. Only if the result is a skip, the strength gene can return permission to the normal step. On average, the share of allowed steps is `2/3 + t/3`.

The solution uses a fixed absolute historical day hash and a threshold of `t`. It is the same for the same genes and dates in a ordinary crop bed and with all crop sticks materials. Position, planting UUID and world random number generator are not involved. Reloading or moving to another position does not give a new roll for the same day. The increase in strength is monotonous: a step allowed at a smaller value remains allowed at a larger value.

The test is after the initial limitations of location, water, season and soil suitability. Strength does not speed up normal soil, does not add a second step in a day, does not replace watering, and does not allow planting on an off-limits block. For a repeat harvest, the same native fractional counter is used. A dry day doesn't change it. An already processed day is protected by native `lastDailyDay`.

Magnifying glass/Jade/TOP forecast the remaining days on a copy of the soil counter with the same future calendar samples. This is a conditional forecast with daily watering and allowed growth; a pause, skipping water or changing conditions can move the date. Reading the report does not change the plant's progress.

## Resistance, historical calendar and seasonal exceptions

Postponement is permitted only when the current season is prohibited for the crop, but the immediately preceding one was permitted. The balance is calculated from the first day of the current season: `max(0, graceDays − floorMod(absoluteDay − 1, 28))`. The transition from winter to spring next year is taken into account by the same rule. A later ineligible season does not start a new window.

There is no new editable counter or SavedData field. Saving, changing material, removing and adding crop sticks, changing UUID or position does not extend the calendar window. The native `CropGrowthManager` continues to mark processed dates. When restoring missing days, each historical day is used, even if the world clock is already in a different season.

When native dynamic metadata is available, the history pass reads it in the same way as Starfield. In the absence of these data, original crop methods are used. To check for a different season, only the native daily processing context of the current thread is temporarily set; the previous context is restored to `finally`, the global clock and progress are not changed. The table of valid seasons from species descriptions is not copied to the executor.

During the delay, normal growth, fractional soil progress, and the regrow countdown are paused. A mature crop can be collected in a regular way with the same rights, products and transaction rollback. Harvesting a repeat crop sets its usual genetic regrow period, but this period does not decrease until the permitted growth. After the window has expired, the original seasonal death is executed with the top and sidecar removed.

The native farm benefit and `SeasonLocationRules` retain the original behavior: in such places, the crop can continue to grow according to Starfield rules. The gene does not replace these exceptions or start a new window after they complete. All-season crops do not receive an unnecessary pause. Roots preserved precisely by genetic delay are excluded from the native passage of the formation of giants before constructing a list of candidates; the algorithm and probability for the remaining plantings have not been changed.

## Validation and reproducibility

The official JAR is verified using SHA-256, all 219 local sources are verified using Git blob. Testing is performed on a real server with the original mod and release integration code.

| Check | Result |
|---|---|
| Full enabled profile | 78/78 passed, including final testing of dynamic seasonal rules |
| Separate JVM write | 37/37 passed |
| Separate JVM read | 37/37 passed |
| Starfield installed, integration disabled | 1/1 passed |
| Starfield is missing from NeoForge | 1/1 passed |
| Fabric without Starfield | 2/2 passed |
| Test profiles | Isolation of two NeoForge archives confirmed |
| Regular builds | NeoForge and Fabric are successful; isolation of five archives confirmed |

Twelve iteration scenarios 9 test tables, boundaries and monotonicity, intermediate strength, dry days, normal soil, forecast, repeat date and no world random number consumption; seasonal pause, calendar death, removal/materials of crop sticks and loading sidecar; mature harvest, high repeat harvest, native protected sites and farm benefit, all-season species, historical days, wild seed year transition, giant pass exclusion and disabled integration. A separate branch of the giant script specifies dynamic seasonal metadata that is different from the original block method: the filter maintains the appropriate native distinction between normal and historical processing.

Script `realJvmRestartKeepsSoilRemainderAndSeasonAnchor` requires two separate processes. The first leaves real paired plantings of cauliflower with intermediate strength/soil residue and potatoes in the first day of delay. The server saves blocks, standard genome and `stardew_crop_manager` normal stop. The second compares the loaded data with the control NBT, checks the suppression of the repeat date, the next soil step and death in the previous calendar period. Witness `iteration9-restart-witness.nbt` is for comparison purposes only; recovery from it is prohibited and is not performed. By default, this scenario completes without performing a restart check.

```powershell
python tests/starfield/prepare.py --offline
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests -PstarfieldHarvestTests -PstarfieldToolTests -PstarfieldIrrigationTests -PstarfieldBreedingTests --console=plain
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests -PstarfieldRestartPhase=write --console=plain
.\gradlew.bat :neoforge:runStarfieldPrototype -PstarfieldPrototype -PstarfieldTestMode=enabled -PstarfieldGeneticsTests -PstarfieldDailyTests -PstarfieldRestartPhase=read --console=plain
```

Logs: ignored directory `build/starfield-iteration0/iteration9-*.log`. The first pass revealed the absence of seasonal Data Maps for some common crops. The fix uses their native season check; Starfield sources are not changed.

## Stage boundaries

All six existing genes are now active in cultivation or selection. Full support for custom geometries and conversion of wild seeds to forage is the next step 10; the issuance of automation products, destruction and giant harvests retain their separate native paths. The giant pass filter only closes the seasonal pause bypass and does not complete the entire 11 stage.

The client model and game staging of sidecar-crop sticks remain subsequent tasks. The graphical client has not been manually verified. Common/Fabric do not receive Starfield dependencies; The optional mixin contains only the tested target/selector strings, without JVM references to native classes. Archive isolation continues to prohibit test classes, resources, and the built-in native mod. AgriCraft version has not been upgraded and publication has not been completed.
