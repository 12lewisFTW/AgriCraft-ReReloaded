# Mystical Agriculture: resource crops and progression gates

The packaged compatibility data contains **136 resource-crop definitions**, five essence farmland soils and one fertilizer. Some definitions depend on optional resources or addons: the count is an inventory of the pack, not 136 guaranteed crops in every installation. Both AgriCraft archives contain this data, but the native effective-farmland API check described below is a **NeoForge** integration.

## Obtain the species through Mystical Agriculture

Use the source mod's normal crafting or progression to obtain the initial native seeds. Convert supported seeds through the analyzer and grow them on single AgriCraft sticks. The pack adds **no species mutation recipes**; it does not let wheat mutate directly into diamond seeds or invent resource species.

Every definition sets `cloneable=false`. A single parent cannot spread a clone. **Two mature parents of the same species can still breed offspring and improve genes**, so this restriction does not prohibit all seed multiplication after acquiring two starters. Fertility selection and the ordinary breeding engine still apply.

## Growth and essence balance

Resource crops use base growth chance **0.65**, Growth bonus **0.025**, and reset to **stage 3** after harvesting. On soil with a 1.0 modifier, the eligible growth roll rises from 67.5% at Growth 1 to 90% at Growth 10. Ordinary environmental requirements remain wet, slightly acidic, high-or-better nutrients and light 10–16 before tolerance.

The packaged essence product is a **75% roll for one essence per trial**. Default Gain 1 gives one trial and expected yield 0.75. Gain 9–10 gives four trials and expected yield 3, with possible actual output from 0 to 4. There is no guaranteed four-essence harvest.

The tradeoff is a slower growth roll, no one-parent cloning, and retained access to the source mod's resource progression, while selective breeding improves production once established.

## Essence farmland

Inferium, prudentium, tertium, imperium and supremium farmland are recognized as AgriCraft soils. All five have wet humidity, slightly acidic acidity, high nutrients and an AgriCraft growth modifier of **1.0**. Do not assume an additional 1.8 rich-soil bonus or native yield bonuses from this table.

On NeoForge with `compat.mysticalagriculture=true`, non-inferium crops also obey the native tier's effective-farmland check when Mystical Agriculture's `requiresEffectiveFarmland` setting requires it. This uses Mystical Agriculture's effective-farmland rules, not a claim that the block must have one exact name. Inferium is exempt from this additional check, but still needs valid AgriCraft growing conditions. Strength cannot bypass the tier requirement.

## Mystical fertilizer

`mysticalagriculture:mystical_fertilizer` is registered with potency **5**, meaning five fertilized growth attempts. It does not guarantee five successful stages or instant maturity. Its default definition triggers neither mutation nor weeds and contains no negative plant matches. This is a targeted growing aid, not an extra resource-species mutation route.

Native Growth Accelerators and other boosters that specifically inspect Mystical Agriculture crop blocks should not be assumed to affect converted AgriCraft blocks. This module does not implement a general transfer of every native farming bonus.

See [all packaged resource species](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Mystical-Agriculture-Catalog), [Genes and Balance](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Genes-and-Balance) and [Configuration and Troubleshooting](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Configuration-and-Troubleshooting).

## Basis of this guide

Defaults refer to AgriCraft ReReloaded **4.0.20**, Minecraft **1.21.1**. Modpacks can override data and settings. [Native farmland integration](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/6d6935f09e749923566ff0de3d0c76e0da5b3c4c/neoforge/src/main/java/com/agricraft/agricraft/common/plugin/MysticalAgricultureCompat.java).
