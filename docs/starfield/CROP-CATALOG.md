# Starfield crop catalog: iteration 0

Release `0.6.3fix1`, commit `df2151c66a2a9c02855e4db23108e57c422ce78e`. 46 crops and 4 wild-seed species.

This is a design inventory, not a list of already implemented integrations.

All IDs below use the `stardewcraft` namespace. Base soils: native
`farmland`, `sandy_farmland`, `infertile_farmland` or `garden_pot`, with additional
species-specific restrictions. `phaseDays` excludes the internal harvest sentinel.

| Species | Seed | Produce | phaseDays | Seasons | Regrow, days | Geometry | Harvest | Giant | Notes |
|---|---|---|---|---|---|---|---|---|---|
| amaranth_crop | amaranth_seeds | amaranth | 1,2,2,2 | fall | — | 2 parts | scythe | — | — |
| ancient_fruit_crop | ancient_fruit_seeds | ancient_fruit | 2,7,7,7,5 | spring, summer, fall | 7 | 2 parts | grab | — | — |
| artichoke_crop | artichoke_seeds | artichoke | 2,2,1,2,1 | fall | — | 2 parts | grab | — | — |
| beet_crop | beet_seeds | beet | 1,1,2,2 | fall | — | 1 block | grab | — | — |
| blue_jazz_crop | blue_jazz_seeds | blue_jazz | 1,2,2,2 | spring | — | 2 parts | grab | — | — |
| blueberry_crop | blueberry_seeds | blueberry | 1,3,3,4,2 | summer | 4 | 2 parts | grab | — | — |
| bok_choy_crop | bok_choy_seeds | bok_choy | 1,1,1,1 | fall | — | 1 block | grab | — | — |
| broccoli_crop | broccoli_seeds | broccoli | 1,2,2,3 | fall | 4 | 1 block | grab | — | — |
| cactus_fruit_crop | cactus_seeds | cactus_fruit | 2,2,2,3,3 | spring, summer, fall, winter | 3 | 2 parts | grab | — | All seasons; planting only indoors/in a greenhouse or on the island |
| carrot_crop | carrot_seeds | carrot | 1,1,1 | spring | — | 2 parts | grab | — | — |
| cauliflower_crop | cauliflower_seeds | cauliflower | 1,2,4,4,1 | spring | — | 1 block | grab | yes | — |
| coffee_bean_crop | coffee_bean | coffee_bean | 1,2,2,3,2 | spring, summer | 2 | 2 parts | grab | — | Produce also serves as seed; multiple beans per harvest |
| corn_crop | corn_seeds | corn | 2,3,3,3,3 | summer, fall | 4 | 2 parts | grab | — | — |
| cranberry_crop | cranberry_seeds | cranberry | 1,2,1,1,2 | fall | 5 | 2 parts | grab | — | — |
| eggplant_crop | eggplant_seeds | eggplant | 1,1,1,1,1 | fall | 5 | 2 parts | grab | — | — |
| fairy_rose_crop | fairy_rose_seeds | fairy_rose | 1,4,4,3 | fall | — | 2 parts | grab | — | — |
| fall_wild_seed_crop | fall_seeds | seasonal forage | 3,4 | fall | — | 1 block | transforms_to_forage | — | Replaced by a seasonal ForageBlock at maturity |
| fiber_crop | fiber_seeds | fiber | 1,2,2,2 | spring, summer, fall, winter | — | 2 parts | scythe | — | Grows without watering; all seasons; normal quality; special drop composition |
| garlic_crop | garlic_seeds | garlic | 1,1,1,1 | spring | — | 2 parts | grab | — | — |
| grape_crop | grape_seeds | grape | 1,1,2,3,3 | fall | 3 | 2 parts | grab | — | Trellis; the seed maker returns summer wild seeds |
| green_bean_crop | green_bean_seeds | green_bean | 1,1,1,3,4 | spring | 3 | 2 parts | grab | — | — |
| hops_crop | hops_seeds | hops | 1,1,2,3,4 | summer | 1 | 2 parts | grab | — | — |
| hot_pepper_crop | hot_pepper_seeds | hot_pepper | 1,1,1,1,1 | summer | 3 | 2 parts | grab | — | — |
| kale_crop | kale_seeds | kale | 1,2,2,1 | spring | — | 2 parts | scythe | — | — |
| melon_crop | melon_seeds | melon | 1,2,3,3,3 | summer | — | 2 parts | grab | yes | — |
| parsnip_crop | parsnip_seeds | parsnip | 1,1,1,1 | spring | — | 1 block | grab | — | — |
| pineapple_crop | pineapple_seeds | pineapple | 1,3,3,4,3 | summer | 7 | 2 parts | grab | — | — |
| poppy_crop | poppy_seeds | poppy | 1,2,2,2 | summer | — | 2 parts | grab | — | — |
| potato_crop | potato_seeds | potato | 1,1,1,2,1 | spring | — | 1 block | grab | — | Additional native drops; 20% chance with repeat rolls |
| powder_melon_crop | powder_melon_seeds | powder_melon | 1,1,1,2,2 | winter | — | 2 parts | grab | yes | — |
| pumpkin_crop | pumpkin_seeds | pumpkin | 1,2,3,4,3 | fall | — | 2 parts | grab | yes | — |
| qi_fruit_crop | qi_bean | qi_fruit | 1,1,1,1 | spring, summer, fall, winter | — | 2 parts | grab | yes | Normal quality; four one-day phases |
| radish_crop | radish_seeds | radish | 2,1,2,1 | summer | — | 1 block | grab | — | — |
| red_cabbage_crop | red_cabbage_seeds | red_cabbage | 2,1,2,2,2 | summer | — | 1 block | grab | — | — |
| rhubarb_crop | rhubarb_seeds | rhubarb | 2,2,2,3,4 | spring | — | 2 parts | grab | — | — |
| rice_crop | rice_shoot | unmilled_rice | 1,2,2,3 | spring | — | 2 parts | scythe | — | Near water (radius 3): automatic watering and +25% growth speed; special geometry |
| spring_wild_seed_crop | spring_seeds | seasonal forage | 3,4 | spring | — | 1 block | transforms_to_forage | — | Replaced by a seasonal ForageBlock at maturity |
| starfruit_crop | starfruit_seeds | starfruit | 2,3,2,3,3 | summer | — | 2 parts | grab | — | — |
| strawberry_crop | strawberry_seeds | strawberry | 1,1,2,2,2 | spring | 4 | 2 parts | grab | — | — |
| summer_spangle_crop | summer_spangle_seeds | summer_spangle | 1,2,3,2 | summer | — | 2 parts | grab | — | — |
| summer_squash_crop | summer_squash_seeds | summer_squash | 1,1,1,1,2 | summer | 3 | 2 parts | grab | — | — |
| summer_wild_seed_crop | summer_seeds | seasonal forage | 3,4 | summer | — | 1 block | transforms_to_forage | — | Replaced by a seasonal ForageBlock at maturity |
| sunflower_crop | sunflower_seeds | sunflower | 1,2,3,2 | summer, fall | — | 2 parts | grab | — | Additional seeds on harvest |
| sweet_gem_berry_crop | rare_seed | sweet_gem_berry | 2,4,6,6,6 | fall | — | 2 parts | grab | — | — |
| taro_root_crop | taro_tuber | taro_root | 1,2,3,4 | summer | — | 2 parts | grab | — | Near water: automatic watering and faster growth; special geometry |
| tomato_crop | tomato_seeds | tomato | 2,2,2,2,3 | summer | 4 | 2 parts | grab | — | — |
| tulip_crop | tulip_seeds | tulip | 1,1,2,2 | spring | — | 2 parts | grab | — | — |
| wheat_crop | wheat_seeds | wheat | 1,1,1,1 | summer, fall | — | 2 parts | scythe | — | — |
| winter_wild_seed_crop | winter_seeds | seasonal forage | 3,4 | winter | — | 1 block | transforms_to_forage | — | Replaced by a seasonal ForageBlock at maturity |
| yam_crop | yam_seeds | yam | 1,3,3,3 | fall | — | 1 block | grab | — | — |

Source: official registrations and inherited crop-class methods;
IDs/classes/models are validated against the JAR pinned in `tests/starfield/dependencies.json`.
Reproduce with `python tests/starfield/catalog.py`. The generator deliberately stops
on unsupported source shapes instead of substituting guesses for data.
