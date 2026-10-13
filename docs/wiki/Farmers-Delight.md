# Farmer's Delight: crops and rich soils

The pack supplies **cabbage, onion, rice and tomato**, four species mutation recipes, and two AgriCraft soil definitions. Data is included in both AgriCraft archives; install a compatible source-mod build for your loader.

## Crop behavior

Convert the appropriate native seed in an analyzer, then plant an AgriCraft seed on single sticks. Rice uses **`farmersdelight:rice` as its seed** and produces **rice panicles**. Its mature model reaches two blocks. All four plants use eight stages and return to **stage 4** after ordinary harvesting.

The base growth chance is 0.75 with a 0.025 Growth bonus. Cabbage, onion and tomato target wet, slightly acidic, high-or-better nutrient soil and light 10–16 before tolerance. **Rice instead targets watery-or-higher humidity, slightly acidic soil, medium-or-better nutrients and light 5–16.** The [catalog](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Farmers-Delight-Catalog) lists each requirement and output. Each crop has a 95% roll for 1–3 of its native product per Gain trial: expected output is 1.9 at Gain 1 and 7.6 at Gain 10.

## Rich soil is a choice of conditions

| Native block | Humidity | Acidity | Nutrients | AgriCraft growth modifier |
|---|---|---|---|---:|
| `farmersdelight:rich_soil_farmland` | Wet | Slightly acidic | Very high | 1.8 |
| `farmersdelight:rich_soil` | Damp | Neutral | Very high | 1.8 |

Rich farmland aligns with the common wet/slightly acidic crop target. Untilled rich soil has different conditions; check Strength tolerance rather than assuming that more nutrients make every plant fertile. The 1.8 modifier applies to the growth roll and can saturate at 100%; it is not a guaranteed 80% improvement in real elapsed time.

## Breeding and kitchen use

The four supplied recipes allow species progression through specific parent pairs. For example, onion and Minecraft seagrass can produce cabbage with a raw recipe chance of 50%, after selection. The catalog includes all four recipes. One-parent cloning is allowed with a 10% spread roll, and two-parent crosses can improve stats.

Harvests remain Farmer's Delight items for its cooking and crafting. The compatibility pack does not rebalance meals or add a kitchen machine adapter. Converted rice uses AgriCraft's growing conditions; do not assume the original native rice's flooded planting behavior is transferred wholesale to the crop-stick plant.

See [Soil Irrigation and Greenhouses](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Soil-Irrigation-and-Greenhouses) and [Genes and Balance](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Genes-and-Balance).
