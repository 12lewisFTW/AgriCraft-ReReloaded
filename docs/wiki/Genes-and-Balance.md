# Genes and balance: converted AgriCraft crops

These rules apply to ordinary AgriCraft crop blocks, including converted compatibility crops. [Native Starfield crops](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Starfield-Pastoral) use a separate daily-growth and yield model. Default gene bounds are **1–10** for all six traits.

## Inheritance and selection

Each stat has two alleles. The higher allele is expressed; the lower allele can still pass to offspring. A 10/1 parent therefore behaves like 10, but is less reliable for breeding than 10/10. A two-parent cross picks one allele from each parent for each gene, then applies the stat mutation rule. Use a magnifying glass to inspect the whole pair.

Empty double sticks consider the four horizontal neighboring crop roots. Parents must be mature. If more than two pass selection, the two with highest Fertility are used. With one selected parent, cloning must be allowed by that species and its spread roll must pass. The six packs use a 10% spread roll; Mystical Agriculture disables this one-parent cloning path.

By default, `core.clone_mutations=false`: a clone preserves both alleles. Two-parent breeding is the route for improving traits. Different-species outcomes also depend on species mutation recipes; Mutativity alone cannot create an undocumented crop.

## Gain: harvest trials

Ordinary harvests perform `floor((Gain + 3) / 3)` independent produce trials. Each product has its own chance and inclusive quantity range on every trial.

| Expressed Gain | Produce trials |
|---|---:|
| 1–2 | 1 |
| 3–5 | 2 |
| 6–8 | 3 |
| 9–10 | 4 |

For a product with chance `p` and quantity `a` through `b`, expected output is `trials × p × (a + b) / 2`. This is a long-run average, not a guaranteed individual harvest. A crop may reset its growth stage even if every product roll fails. Clipping uses its separate product list once; it does not receive this Gain trial multiplier.

Croptopia's guaranteed 1–3 produce gives an average of 2 at Gain 1 and 8 at Gain 10. Mystical Agriculture's 75% roll for one essence gives an average of 0.75 and 3 respectively. Reaching Gain 9 already unlocks four trials; a 10/10 genome remains useful for reliable inheritance.

## Growth: chance per eligible attempt

The ordinary growth roll is:

`soil modifier × (base chance + Growth × growth bonus × core.growth_multiplier)`

An AgriCraft greenhouse additionally multiplies the soil modifier by its configured growth modifier. The result is compared against a random value from 0 to 1; values at or above 1 guarantee that roll. Weed state and growth requirements still apply first. These are chances per eligible attempt, **not seconds to maturity**.

| Pack group | Base | Bonus per Growth | Growth 1, soil ×1 | Growth 10, soil ×1 |
|---|---:|---:|---:|---:|
| BOP, Croptopia, Farmer's Delight, IE, Pam's | 0.75 | 0.025 | 77.5% | 100% |
| Mystical Agriculture | 0.65 | 0.025 | 67.5% | 90% |

Assumes the default growth multiplier of 1.0. A soil bonus can reach the roll's ceiling, so a 1.8 soil modifier does not imply an 80% reduction in elapsed growing time.

## Strength: environmental tolerance

Strength widens a plant's allowed humidity, acidity, nutrients and light ranges. Each requirement supplies a tolerance factor; the widening is `floor(factor × Strength)` steps on that requirement's scale. A factor of 0.15 gives zero extra steps at Strength 1 and one at Strength 10. Requirements with different factors have different breakpoints.

This is tolerance, not a replacement for all requirements. Specific block, biome or Mystical Agriculture effective-farmland checks can remain mandatory. Use the crop's growth requirements and the actual soil readout together.

## Resistance: weed activation

On a planted crop, the default base weed-activation roll passes when `random integer 0–9 >= floor((10 + Resistance) / 2)`.

| Resistance | Base chance in an eligible weed activation |
|---|---:|
| 1 | 50% |
| 2–3 | 40% |
| 4–5 | 30% |
| 6–7 | 20% |
| 8–9 | 10% |
| 10 | 0% |

Wooden, iron and obsidian crop sticks apply multipliers of 1.0, 0.7 and 0.4 to this base, respectively. Empty sticks have a separate 50% base roll. The weed engine still needs an applicable weed source and conditions: these numbers are not a flat chance to spawn a weed every world tick. Resistance 10 prevents the planted-crop activation roll under these defaults.

Mature weeds can kill plants by default. Keep breeding beds maintained, or change the [server weed settings](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Configuration-and-Troubleshooting). Stick material on native Starfield crops does not attach this ordinary weed engine.

## Fertility: entering the parent pool

The parent-selection threshold is `min(max Fertility, round(Fertility × core.mutation_chance_multiplier))`. With maximum 10 and the default multiplier 1.2:

| Fertility | Chance for one eligible parent to pass selection |
|---|---:|
| 1 | 10% |
| 5 | 60% |
| 8–10 | 100% |

This is one parent's selection roll, not the final chance of offspring per tick. Maturity, the other parent's selection, cloning restrictions and species recipes still matter.

## Mutativity: changing inherited alleles

For each inherited stat allele, the default engine attempts a change with probability `1 − (1 − Mutativity / max) / 2`. If it changes, the chance of a +1 is `floor((max + Mutativity) / 2) / max`; otherwise it receives −1. Values are clamped to that gene's allowed bounds.

At Mutativity 1 with maximum 10, the raw outcomes are **27.5% +1, 45% unchanged, 27.5% −1**. At Mutativity 10, +1 is selected every time; an allele already at its maximum stays there. Boundary clamping can make a nominal change leave the actual value unchanged. Higher Mutativity favors improvement; it does not increase harvest quantity directly.

## Basis of this guide

Defaults refer to AgriCraft ReReloaded **4.0.20**, Minecraft **1.21.1**. Modpacks can override data and settings. [Harvest and growth](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/6d6935f09e749923566ff0de3d0c76e0da5b3c4c/common/src/main/java/com/agricraft/agricraft/common/block/entity/CropBlockEntity.java) · [Parent selection](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/6d6935f09e749923566ff0de3d0c76e0da5b3c4c/common/src/main/java/com/agricraft/agricraft/api/genetic/AgriCrossBreedEngine.java) · [Allele mutation](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/6d6935f09e749923566ff0de3d0c76e0da5b3c4c/common/src/main/java/com/agricraft/agricraft/api/genetic/AgriMutationHandler.java) · [Stat bounds](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/6d6935f09e749923566ff0de3d0c76e0da5b3c4c/common/src/main/java/com/agricraft/agricraft/api/config/StatsConfig.java).
