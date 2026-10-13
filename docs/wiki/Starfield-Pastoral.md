# Starfield Pastoral: native farming with inherited genes

This integration is available on **NeoForge**, starting with AgriCraft **4.0.20**, for the verified **Starfield Pastoral 0.6.3fix1** release. Install Starfield separately and keep `compat.starfield=true`. Other versions and Fabric do not activate this native integration.

## The gameplay contract

All **50 supported seed types** keep their native blocks and models: 46 crops and four seasonal wild-seed types. Starfield controls daily growth, water, native seasons, soil/location access, fertilizer, quality, professions, XP and harvest tools. AgriCraft attaches a full inherited genome and crop-stick material to the planting.

This lets selective breeding improve a seasonal farm while preserving its original scheduling and progression. It does not turn a day-based plant into an ordinary random-tick crop or replace native seed items with generic AgriCraft seeds.

## Planting, tools and breeding

Use a native seed on supporting soil or empty **single** sticks. The usual native season, placement, location and space checks still apply. Empty **double** sticks are breeding targets. Two mature neighboring roots can contribute to a cross even if they have no attached sticks; source maturity, Fertility, cloning and actual native placement rules remain relevant. The integration adds no new cross-species mutation recipes.

Use wooden, iron or obsidian sticks on a root or upper part to attach one material. Sneak with an empty hand to remove it before replacing it. Material cannot be attached in a garden pot. A one-shot harvest leaves empty single sticks; a regrowing plant keeps its attached material. Destructive removal returns material once when the applicable drop rules allow it. These materials do not add the ordinary AgriCraft weed engine to native plants.

The seed analyzer accepts native seed items while retaining their identity and other components. The magnifying glass shows the planting's genes and native progress. The trowel transfers the whole plant, native progress and both parts where applicable. If placement fails, the planting remains on the tool. Native seeds are not converted for AgriCraft seed bags; unsupported native clipping and raking actions are refused.

## Gene balance

Native gene bonuses use `u = clamp((expressed value − configured minimum) / (maximum − minimum), 0, 1)`. With default bounds 1–10, gene 1 gives `u=0` and gene 10 gives `u=1`. Equal or reversed bounds use `u=0`.

| Gene | Native Starfield effect | Default endpoints |
|---|---|---|
| Growth | A positive ordinary phase/regrowth duration `D` becomes `max(1, ceil(D × (1 − 0.5u)))`, after native fertilizer/profession adjustments | Baseline at 1; up to half the phase days at 10, rounded upward |
| Gain | Primary produce count `N` becomes `floor(N × (1 + u))` once | Baseline at 1; double primary count at 10 |
| Strength | Recovers fraction `u` of historically watered growth days skipped by native infertile soil, through stable sampling of the date | Baseline penalty at 1; removes that penalty at 10 |
| Resistance | Adds `floor(3u)` survival days at the beginning of a disallowed season | 0 extra days at 1; 3 at 10 |
| Fertility | Uses ordinary AgriCraft parent selection | With default multiplier 1.2, eligible selection is 10% at 1, 60% at 5, 100% at 8–10 |
| Mutativity | Uses ordinary inherited-allele mutation | High values favor +1 changes within bounds |

For a native five-day phase, Growth 10 gives `ceil(5 × 0.5) = 3` days; a one-day phase stays one day. Zero/negative special durations and the 99999 sentinel are unchanged. Apply rounding to each phase, not once to the whole crop's total.

For a primary harvest of three items, Gain 5 gives `floor(3 × (1 + 4/9)) = 4`, and Gain 10 gives six. Quality, secondary produce and XP are not multiplied. Oversized outputs are split into valid stacks. **This is not the four-trial Gain rule used by converted crops.**

Strength repairs the infertile-soil delay, not dry days. Where native infertile soil ordinarily permits roughly two thirds of watered growing days, the expected fraction becomes `2/3 + u/3`. Reloading the same historical date cannot reroll that recovery.

Resistance 1–3 grants zero extra days, 4–6 one, 7–9 two, and 10 three. Growth and regrowth pause in that added window, giants cannot form there, and mature crops can still be harvested. The window begins immediately after the suitable season ends; it does not restart whenever the world reloads. Existing native seasonal exceptions remain.

## Irrigation, wild seeds and giant crops

AgriCraft sprinklers water eligible native soil and pots. They do not advance phases, add daily growth or force regrowth. Native rice/taro automatic watering, fiber's water independence and cactus location restrictions remain native. AgriCraft's greenhouse detector does not override native Starfield greenhouse/location permissions.

Planted seasonal wild seeds retain their genome and planting identity when they become mature forage. Naturally generated forage receives no invented genome. Giants keep their original **3×3** conditions and **1% daily formation chance** across the five native giant species. Attached materials are returned once during successful giant formation. Giant produce receives no arbitrary parent genome or Gain bonus.

## Passing a genome through production

Primary produce from a supported planting carries that source species' genome. The real native seed maker keeps its own duration, result probabilities and quantities. If its resulting seeds match the source species, they inherit both alleles. Mixed seeds, ancient-seed exceptions and other-species results receive no unrelated genome. Native grape-to-summer-wild-seed output does not transfer the grape genome.

Coffee beans replanted directly retain the harvested genome. Native seed-maker restrictions on coffee and fiber remain. Wine, dried fruit and other artisan goods retain native quality without becoming hereditary seed material. Production does not provide a route around native recipe or machine restrictions.

## Machines and inspection

[Create](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Create), [Industrial Foregoing](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Industrial-Foregoing) and [PneumaticCraft](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/PneumaticCraft) can harvest supported ordinary native crops through native automation rules. Automation uses the native Junimo context with no player, no farming XP and no player profession bonus. Primary Gain, output genomes and native regrowth still apply once. Seasonal forage and giants are outside this automation path. Immature native crops are refused without Create's ordinary destructive fallback.

Use [Jade](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Jade), [The One Probe](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/The-One-Probe) or the magnifying glass for actual native phase and genome information. Remaining watered days are a conditional progress estimate, not a guarantee of the calendar date of harvest. [WTHIT](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/WTHIT) has no dedicated native Starfield inspection provider in this release.

## Existing saves and switching support off

Back up the world before installing another version. Existing supported native plantings receive a stable baseline genome without recreating native progress. With `compat.starfield=false`, native farming continues, genetic effects and integration tools stop, and unchanged plantings retain their saved genome and material for later reactivation. A real removal or harvest still changes the planting; disabling support does not promise indefinite recovery of a destroyed crop or refund old materials. Seeds harvested without a genetic component while disabled are baseline seeds.

Removing Starfield itself is different from disabling AgriCraft's integration: the original mod owns its blocks and items. Keep the matching mod installed when retaining a native farm.

See the [50-species native catalog](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Starfield-Crop-Catalog) and the [full user guide](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/1.21.1/docs/starfield/USER-GUIDE.md). [Release verification](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/1.21.1/docs/releases/4.0.20-VALIDATION.md) and [ordinary gameplay acceptance](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/1.21.1/docs/starfield/GAMEPLAY-ACCEPTANCE.md) distinguish automated checks from actual client play, including machine operation, a complete drone route and save/reload checks.

## Basis of this guide

Defaults refer to AgriCraft ReReloaded **4.0.20**, Minecraft **1.21.1**. Modpacks can override data and settings. [Native gene effects](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/6d6935f09e749923566ff0de3d0c76e0da5b3c4c/neoforge/src/main/java/com/agricraft/agricraft/compat/starfield/StarfieldGeneEffects.java) · [Version activation](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/6d6935f09e749923566ff0de3d0c76e0da5b3c4c/neoforge/src/main/java/com/agricraft/agricraft/compat/starfield/StarfieldCompat.java).
