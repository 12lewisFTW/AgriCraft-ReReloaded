# Каталог Starfield: итерация 0

Сборка `0.6.3fix1`, commit `df2151c66a2a9c02855e4db23108e57c422ce78e`. 46 культур и 4 вида диких семян.

Это инвентаризация для проектирования, а не список уже работающих интеграций.

Все ID ниже имеют namespace `stardewcraft`. Базовая почва: только нативные
`farmland`, `sandy_farmland`, `infertile_farmland` или `garden_pot`, с дополнительными
ограничениями конкретного вида. `phaseDays` не включает служебный harvest sentinel.

| Вид | Семя | Продукция | phaseDays | Сезоны | Повтор, дни | Геометрия | Сбор | Гигант | Особенности |
|---|---|---|---|---|---|---|---|---|---|
| amaranth_crop | amaranth_seeds | amaranth | 1,2,2,2 | fall | — | 2 части | scythe | — | — |
| ancient_fruit_crop | ancient_fruit_seeds | ancient_fruit | 2,7,7,7,5 | spring, summer, fall | 7 | 2 части | grab | — | — |
| artichoke_crop | artichoke_seeds | artichoke | 2,2,1,2,1 | fall | — | 2 части | grab | — | — |
| beet_crop | beet_seeds | beet | 1,1,2,2 | fall | — | 1 блок | grab | — | — |
| blue_jazz_crop | blue_jazz_seeds | blue_jazz | 1,2,2,2 | spring | — | 2 части | grab | — | — |
| blueberry_crop | blueberry_seeds | blueberry | 1,3,3,4,2 | summer | 4 | 2 части | grab | — | — |
| bok_choy_crop | bok_choy_seeds | bok_choy | 1,1,1,1 | fall | — | 1 блок | grab | — | — |
| broccoli_crop | broccoli_seeds | broccoli | 1,2,2,3 | fall | 4 | 1 блок | grab | — | — |
| cactus_fruit_crop | cactus_seeds | cactus_fruit | 2,2,2,3,3 | spring, summer, fall, winter | 3 | 2 части | grab | — | Все сезоны; посадка только в помещении/теплице или на острове |
| carrot_crop | carrot_seeds | carrot | 1,1,1 | spring | — | 2 части | grab | — | — |
| cauliflower_crop | cauliflower_seeds | cauliflower | 1,2,4,4,1 | spring | — | 1 блок | grab | да | — |
| coffee_bean_crop | coffee_bean | coffee_bean | 1,2,2,3,2 | spring, summer | 2 | 2 части | grab | — | Продукция также служит семенем; несколько бобов за сбор |
| corn_crop | corn_seeds | corn | 2,3,3,3,3 | summer, fall | 4 | 2 части | grab | — | — |
| cranberry_crop | cranberry_seeds | cranberry | 1,2,1,1,2 | fall | 5 | 2 части | grab | — | — |
| eggplant_crop | eggplant_seeds | eggplant | 1,1,1,1,1 | fall | 5 | 2 части | grab | — | — |
| fairy_rose_crop | fairy_rose_seeds | fairy_rose | 1,4,4,3 | fall | — | 2 части | grab | — | — |
| fall_wild_seed_crop | fall_seeds | сезонный forage | 3,4 | fall | — | 1 блок | transforms_to_forage | — | При созревании заменяется сезонным ForageBlock |
| fiber_crop | fiber_seeds | fiber | 1,2,2,2 | spring, summer, fall, winter | — | 2 части | scythe | — | Рост без полива; все сезоны; нормальное качество; особый состав дропа |
| garlic_crop | garlic_seeds | garlic | 1,1,1,1 | spring | — | 2 части | grab | — | — |
| grape_crop | grape_seeds | grape | 1,1,2,3,3 | fall | 3 | 2 части | grab | — | Шпалера; семенной аппарат возвращает летние дикие семена |
| green_bean_crop | green_bean_seeds | green_bean | 1,1,1,3,4 | spring | 3 | 2 части | grab | — | — |
| hops_crop | hops_seeds | hops | 1,1,2,3,4 | summer | 1 | 2 части | grab | — | — |
| hot_pepper_crop | hot_pepper_seeds | hot_pepper | 1,1,1,1,1 | summer | 3 | 2 части | grab | — | — |
| kale_crop | kale_seeds | kale | 1,2,2,1 | spring | — | 2 части | scythe | — | — |
| melon_crop | melon_seeds | melon | 1,2,3,3,3 | summer | — | 2 части | grab | да | — |
| parsnip_crop | parsnip_seeds | parsnip | 1,1,1,1 | spring | — | 1 блок | grab | — | — |
| pineapple_crop | pineapple_seeds | pineapple | 1,3,3,4,3 | summer | 7 | 2 части | grab | — | — |
| poppy_crop | poppy_seeds | poppy | 1,2,2,2 | summer | — | 2 части | grab | — | — |
| potato_crop | potato_seeds | potato | 1,1,1,2,1 | spring | — | 1 блок | grab | — | Дополнительный нативный дроп; 20% с возможностью повторения |
| powder_melon_crop | powder_melon_seeds | powder_melon | 1,1,1,2,2 | winter | — | 2 части | grab | да | — |
| pumpkin_crop | pumpkin_seeds | pumpkin | 1,2,3,4,3 | fall | — | 2 части | grab | да | — |
| qi_fruit_crop | qi_bean | qi_fruit | 1,1,1,1 | spring, summer, fall, winter | — | 2 части | grab | да | Нормальное качество; четыре однодневные фазы |
| radish_crop | radish_seeds | radish | 2,1,2,1 | summer | — | 1 блок | grab | — | — |
| red_cabbage_crop | red_cabbage_seeds | red_cabbage | 2,1,2,2,2 | summer | — | 1 блок | grab | — | — |
| rhubarb_crop | rhubarb_seeds | rhubarb | 2,2,2,3,4 | spring | — | 2 части | grab | — | — |
| rice_crop | rice_shoot | unmilled_rice | 1,2,2,3 | spring | — | 2 части | scythe | — | Рядом с водой (радиус 3): автополив и +25% скорости; особая геометрия |
| spring_wild_seed_crop | spring_seeds | сезонный forage | 3,4 | spring | — | 1 блок | transforms_to_forage | — | При созревании заменяется сезонным ForageBlock |
| starfruit_crop | starfruit_seeds | starfruit | 2,3,2,3,3 | summer | — | 2 части | grab | — | — |
| strawberry_crop | strawberry_seeds | strawberry | 1,1,2,2,2 | spring | 4 | 2 части | grab | — | — |
| summer_spangle_crop | summer_spangle_seeds | summer_spangle | 1,2,3,2 | summer | — | 2 части | grab | — | — |
| summer_squash_crop | summer_squash_seeds | summer_squash | 1,1,1,1,2 | summer | 3 | 2 части | grab | — | — |
| summer_wild_seed_crop | summer_seeds | сезонный forage | 3,4 | summer | — | 1 блок | transforms_to_forage | — | При созревании заменяется сезонным ForageBlock |
| sunflower_crop | sunflower_seeds | sunflower | 1,2,3,2 | summer, fall | — | 2 части | grab | — | Дополнительные семена при сборе |
| sweet_gem_berry_crop | rare_seed | sweet_gem_berry | 2,4,6,6,6 | fall | — | 2 части | grab | — | — |
| taro_root_crop | taro_tuber | taro_root | 1,2,3,4 | summer | — | 2 части | grab | — | Рядом с водой: автополив и ускорение; особая геометрия |
| tomato_crop | tomato_seeds | tomato | 2,2,2,2,3 | summer | 4 | 2 части | grab | — | — |
| tulip_crop | tulip_seeds | tulip | 1,1,2,2 | spring | — | 2 части | grab | — | — |
| wheat_crop | wheat_seeds | wheat | 1,1,1,1 | summer, fall | — | 2 части | scythe | — | — |
| winter_wild_seed_crop | winter_seeds | сезонный forage | 3,4 | winter | — | 1 блок | transforms_to_forage | — | При созревании заменяется сезонным ForageBlock |
| yam_crop | yam_seeds | yam | 1,3,3,3 | fall | — | 1 блок | grab | — | — |

Источник: официальные регистрации и наследуемые методы crop-классов;
ID/классы/модели сверены с JAR из `tests/starfield/dependencies.json`.
Воспроизведение: `python tests/starfield/catalog.py`. Генератор намеренно прекращает
работу при неизвестной форме исходников, чтобы не подменять данные догадкой.
