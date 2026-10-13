# Changelog

## 1.21.1-4.0.20

### Added

- Optional NeoForge integration with **Starfield Pastoral 0.6.3fix1**, covering all 50 supported native seed types while preserving Starfield's crop models, daily calendar, watering, seasons, quality and farming rules.
- Native seeds can be planted into single crop sticks. Wooden, iron and obsidian sticks can be attached to existing native crops, rendered around their roots and removed with sneak + empty hand. One-shot harvesting retains empty sticks; removal and giant formation return materials once.
- All six AgriCraft genes work with native crops: growth, gain, strength, resistance, fertility and mutativity. Maximum growth halves positive phase/regrow durations with upward rounding; maximum gain doubles primary produce. Maximum strength removes infertile-soil growth delays; maximum resistance grants three days of out-of-season survival with growth paused.
- Native crop breeding on empty double sticks, seed analysis, trowel transfers and inspection through the magnifying glass, Jade and The One Probe. Native seeds and their components remain intact. Different species require explicit mutation recipes; no new species recipes are shipped.
- AgriCraft irrigation for native farmland and garden pots, using the existing water budget, range and obstacle rules.
- Genome inheritance through native produce and the seed maker, including direct replanting of harvested coffee beans. Native mixed/ancient/foreign seed outcomes and artisan goods retain their original behavior.
- Native crop harvesting through AgriCraft automation, Create, Industrial Foregoing and PneumaticCraft, preserving quality, side products and crop transitions without duplicate rewards.
- Support for native multipart crops, seasonal wild-seed forage and all five native giants. Giant formation chance and giant harvest rewards retain Starfield's original rules.
- English and Russian translations for native crop inspection and compatibility settings.

### Fixed

- Sprinklers now save consumed water and partial sweep progress, and avoid loading neighbouring chunks during irrigation.
- Seed Analyzer validates menu access and seed transfers while retaining native genomes and item components.
- Native crop snapshots clear correctly after distant teleports, chunk unloads and reconnects.
- Stick materials returned by native giants spawn above the full giant shape, so they remain collectible after cutting it.
- Fixed missing Jade setting translations when returning to the menu after disconnecting.
- Mod links and the NeoForge update checker now point to this fork and its Minecraft 1.21.1 releases.

### Compatibility and limits

- Minecraft **1.21.1**, **NeoForge** and **Fabric**. Starfield integration is NeoForge-only and activates only for the verified **0.6.3fix1** release with `compat.starfield` enabled. Missing or unverified Starfield installs no native gameplay, tick, rendering or inspection callbacks.
- Native Starfield-to-AgriSeed conversion, native seed-bag storage, unsupported rake/clipper actions and automated forage/giant harvesting remain unavailable. An external inventory that accepts produce and then throws cannot be rolled back by AgriCraft.
- Starfield and its assets are not bundled. Development tests, diagnostics and test resources are excluded from the release JARs.

## 1.21.1-4.0.19

### Added

- Built-in Croptopia compatibility for all 58 farmland crops on NeoForge and Fabric, enabled automatically when Croptopia is installed. Seeds support conversion, analysis, AgriCraft genes, planting, and harvesting using Croptopia's crop models and produce. No new species mutation recipes are included; fruit trees are outside this pack's scope.
- Optional The One Probe integration on NeoForge. AgriCraft plants, including datapack crops, now show their growth percentage; extended mode also shows species and visible gene stats.

## 1.21.1-4.0.18

### Added

- Added Simplified Chinese (`zh_cn`) localization for the base mod and all crop translations in the Biomes O' Plenty, Farmer's Delight, Immersive Engineering, Mystical Agriculture, and Pam's HarvestCraft 2 compatibility packs.

### Fixed

- Fixed the Pam's HarvestCraft 2 eggplant seed translation key and duplicated "Seeds" in mustard and sesame seed names.

## 1.21.1-4.0.17

### Fixed

- Clippers now perform plant clipping instead of triggering the crop's normal right-click harvest. Supported mature flowers once again drop their configured clipping product and return to their initial growth stage.
- Disabling Mystical Agriculture compatibility now prevents Mystical Agriculture seeds from being converted into AgriCraft seeds on both NeoForge and Fabric.

### Mystical Agriculture compatibility

- Added a dedicated `compat.mysticalagriculture` configuration option.
- AgriCraft crops now respect Mystical Agriculture's `requiresEffectiveFarmland` setting. When enabled, Mystical Agriculture crops require farmland matching their tier.

## 1.21.1-4.0.15

- ADDED: Iron and obsidian crop sticks now resist weeds taking root (30% and 60% less likely respectively), on top of their existing lava fire-resistance; shown as an item tooltip

## 1.21.1-4.0.14

- FIXED: Trowel silently failing to store a picked-up plant's genome/growth stage (and `AgriGenomeProviderItem.setGenome` in general), because the NBT tag mutation was applied to a detached copy that was never written back onto the item stack — the trowel would report success on pickup but say "there is nothing here to plant" afterwards

## 1.21.1-4.0.13

- ADDED: `mutation_chance_multiplier` config option, scaling the fertility-based base chance for a crop to be picked as a cross-breeding/cloning parent (default 1.2, i.e. +20%)

## 1.21.1-4.0.12

- CHANGED: Seed Analyzer stat labels now keep a 5px safety margin from the panel edge once shrunk to fit

## 1.21.1-4.0.11

- CHANGED: Magnifying glass tooltip max width increased by 30% to fit longer condition messages
- CHANGED: The "needs a specific block" condition line now always renders last among unmet growth conditions, and its intro line is shown before the block riddle instead of the riddle alone
- FIXED: Seed Analyzer stat labels (e.g. "Mutativity") overflowing past the GUI panel on longer translations by shrinking them to fit instead
- FIXED: Journal introduction pages — "Newly Ported Features" moved from page 2 to page 3, to declutter page 2
- FIXED: Journal "Plant Breeding Genetics" title overflowing on a single line on longer translations; it now wraps
- FIXED: Mutation page showing a pink/black missing-texture icon for undiscovered parents/results on Fabric instead of the proper unknown-plant icon

## 1.21.1-4.0.10

- ADDED: Magnifying glass now tells you whether a soil property needs to be higher or lower (e.g. "too dry, needs more moisture") instead of a generic mismatch message
- ADDED: Ore-requiring plants now show a short riddle hinting at the required block instead of a generic "needs a specific block" message
- ADDED: Line wrapping for the magnifying glass overlay tooltip so longer condition messages no longer run off-screen
- ADDED: Full Russian (ru_ru) and Ukrainian (uk_ua) translations
- ADDED: Irrigation system ported to 1.21.1 (tanks, channels, hollow channels, channel valves, sprinklers), with fluid capability support on NeoForge
- ADDED: Greenhouses ported to 1.21.1 (greenhouse monitor, season bypass and configurable growth bonus inside greenhouses)
- ADDED: Decorative grates ported to 1.21.1 (three positions per block, waterlogging, vines, climbable)
- ADDED: Grass loot modifier ported from the official 1.21 branch (grass has a chance to drop agricraft seeds, NeoForge)
- ADDED: Re-enabled Mystical Agriculture, Industrial Foregoing and PneumaticCraft: Repressurized compat plugins against their 1.21.1 builds
- ADDED: Irrigation and greenhouse config categories
- FIXED: datagen aborting on missing textures of optional compat mods, which left the built jar without plants/soils/mutations data
- FIXED: maintains_farmland block tag was in the pre-1.21 `tags/blocks` folder and was silently ignored

## 1.20.1-4.0.6

- ADDED: PNC:R Harvesting Drones support (PiotrO15)
- ADDED: Industrial Foregoing support (PiotrO15)
- FIXED: annotate emi plugin with EmiEntrypoint (unilock)
- FIXED: reduce particles when breaking crop sticks (PiotrO15)
- FIXED: crop blocks not being horn harvestable in forge (PiotrO15)
- FIXED: typos in mutations jsons (Wyrdix)
- FIXED: Crash when using the clipper via automation (joakime)
- FIXED: typos in pamhc2crops mutations jsons (Minerofmillions)

## 1.20.1-4.0.5

- FIXED: crash when mystical agriculture is not present

## 1.20.1-4.0.4

- FIXED: server crash because client only code is called
- FIXED: crash when fertilizing empty crop sticks

## 1.20.1-4.0.3

- ADDED: Config option to toggle enabling Agricraft's mod compatibility datapacks/resourcepacks by default (unilock)
- ADDED: Allow lavalogging of crops (unilock)
- ADDED: allow marking plants as fireproof (unilock)
- ADDED: Botania plants datapack (PiotrO15)
- ADDED: Mystical Agriculture plants compatibility (PiotrO15, Ketheroth)
- ADDED: Farmer's Delight plants compatibility (PiotrO15)
- ADDED: Botania compat (unilock, Ketheroth)
- CHANGED: iron and obsidian crop sticks are now fireproof and wooden crop sticks brun in lava (unilock)
- CHANGED: renamed clipProducts to clip_products in plant json
- FIXED: wrong light level used (unilock)
- FIXED: unmet growth conditions are not displayed in the tooltips
- FIXED: the vanilla seed conversion doesn't respect the seed.override_planting field in the plant json
- FIXED: plant modifiers are not used
- FIXED: seed analyzer interactions bugs (oitsjustjose)

## 1.20.1-4.0.2-beta

- ADDED: WTHIT plugin (unilock)
- CHANGED: allow harvesting crops with items in hand (unilock)
- FIXED: crash bonemealing empty crops (unilock)
- FIXED: only add nbt to item produces when it's not empty (unilock)
- FIXED: item tags for recipes (unilock)
- FIXED: crash when accessing server side registry from client (unilock)
- FIXED: crop sticks block translation key (unilock)

## 1.20.1-4.0.1-beta

- backport 1.20.4-4.0.1-beta to minecraft 1.20.1 (unilock)
