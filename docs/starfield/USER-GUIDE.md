# AgriCraft and Starfield Pastoral: behavior and support limits

Available from **AgriCraft Re-Reloaded 4.0.20**. [Release changelog](../releases/4.0.20.md).

Verified baseline: Minecraft 1.21.1, NeoForge 21.1.226 and official Starfield Pastoral **0.6.3fix1**. Other Starfield versions do not activate the native bridge. Fabric runs ordinary AgriCraft without this integration. The `compat.starfield` setting enables support on NeoForge.

Without Starfield, or with an unverified version, the integration registers no gameplay events, item-use interception, rendering or client-cache callbacks, magnifying-glass inspector or Jade provider. Presence and version are checked once against loader metadata. There are no separate tick/frame passes or chunk-loading tasks. Network type declarations at startup and brief common-API checks remain; they send no crop data and scan no world. This is not a claim of a measured zero FPS/TPS impact.

Gameplay planting and removal of crop sticks, retaining material after harvest, and client rendering are implemented. Earlier acceptance passed 136 server checks, real operations of three machine integrations, two graphical clients and a restart of an independently installed release JAR. Results and limits are recorded in the [completion report](ACCEPTANCE.md); the current release checks are recorded in [release verification](../releases/4.0.20-VALIDATION.md). The integration is included in 4.0.20. The earlier isolated shutdown hang was not reproduced and, by the user's decision on October 10, 2026, is no longer a release blocker; investigation resumes if it recurs.

## Which mod grows the plant

The plant remains a real Starfield block. Starfield executes its calendar, historical days, phases, soil, water, fertilizer, seasonal exceptions, quality and regrowth. Separate AgriCraft saved data stores the genome with both alleles, planting UUID, seed template, crop-stick material and special form. AgriCraft adds no second daily executor.

The integration supports 50 original planting species: 46 crops and four seasonal wild-seed species. For 37 multipart species, upper and lower parts belong to one root. Harvesting the upper part cannot produce a second harvest. The baseline genome uses the minima in the current AgriCraft settings; reading an old seed does not roll random genes.

## Six existing genes

Growth, gain, strength and resistance use `u = clamp((value − min)/(max − min), 0, 1)`. If `max <= min`, the effect is zero. The expressed trait comes from the standard AgriCraft genome; inheritance retains both alleles.

| Trait | Effect |
|---|---|
| Growth | Each positive native phase/regrow duration becomes `max(1, ceil(days × (1 − 0.5u)))`. Zero phases, negative values and native sentinel 99999 retain their original values. Native fertilizer is applied first. |
| Gain | Primary produce count becomes `floor(count × (1 + u))` once; the maximum doubles it. Side products, their drop chances, quality and machine-produced seeds do not receive this multiplier. Stacks respect the item's actual stack limit. |
| Strength | On a historically watered day skipped because of infertile soil, restores a fraction `u` of skipped growth through stable sampling of the absolute day. The maximum removes only this delay; dry days remain dry. Restarting or processing the same date again cannot reroll it. |
| Resistance | Grants `floor(3u)` survival days at the start of a disallowed season, up to three. Growth/regrowth pause during this window and giant formation is excluded. The window uses the historical date rather than a restarted counter. Native seasonal exceptions remain. |
| Fertility | Participates in the existing AgriCraft parent selection: `random.nextInt(max) < min(max, round(trait × mutationChanceMultiplier))`, followed by preference for more fertile candidates. |
| Mutativity | The current engine checks an allele change using `nextFloat() >= (1 − trait/max)/2`. On change, it selects +1 if `nextInt(max) < (max + trait)/2` (integer division), otherwise −1, within the allele's own bounds. Expressing a different species requires an existing mutation recipe; a third-party engine can replace this logic. |

## Ordinary and special crops

Irrigation waters native soil without advancing phases itself. Rice/taro's native automatic watering, fiber's growth without watering and cactus location restrictions remain. Tea, ancient crops and seasonal exceptions follow their original rules.

Planted seasonal wild seeds retain their source species, UUID and genome when they transform into mature forage. They support inspection, transfer, breeding and harvesting with native access checks. World-generated forage receives no invented genome. All five native giant species retain the original 3×3 conditions and chance; strength does not increase that chance. Successful formation returns saved crop-stick materials once. Giant harvest has no arbitrary parent genome or gain bonus.

## Planting and crop-stick material

Use a native seed on empty single crop sticks or supporting soil. Empty double sticks are for breeding. Planting enforces native season, location and space checks; canceled placement restores the original seed and sticks.

Use wooden, iron or obsidian crop sticks on the root or upper part of an existing plant to attach one material. Shift + empty hand removes it and returns the item to inventory without changing the plant. Remove the old material before replacing it. Material cannot be attached in a garden pot. Creative mode neither consumes an item nor generates material returns.

One-shot hand, scythe or automated harvest retains empty single sticks. For regrowing crops, material stays on the living plant. Destructive or natural removal returns material once, respecting the block-drop rule. The trowel transfers material with the planting; giant formation returns it through the previously documented transaction.

The client renders the existing material model around the actual native root, including tall crops and grown forage, within 64 blocks and loaded chunks. Native plant models are retained.

## Seeds, tools and production

The analyzer accepts native seeds while preserving their item identity and foreign components. The magnifying glass, Jade and The One Probe report data for one planting. Server checks validate dimension, reach, tool and requested UUID; administrator privileges are not required. The trowel carries the entire plant, both parts and native progress. Failed placement leaves the plant on the tool.

Hand/scythe harvesting retains native tool requirements, access, quality and XP. A failed transaction before harvest publication restores the plant and progress. Primary produce carries the source species' genome in `agricraft:starfield_produce` inside `CUSTOM_DATA`.

The seed maker selects its native result and quantity. Matching-species seeds inherit both alleles. Ancient, mixed and other-species results receive no unrelated genome. Native grapes produce summer wild seeds, which do not inherit the grape genome. Wine, dried fruit and other artisan goods keep native quality without a heredity component. Invalid heredity rejects before consuming input. Native coffee-bean/fiber restrictions remain. A harvested coffee bean replanted directly retains its genome through the produce component; malformed schemas or mismatched items are rejected.

AgriCraft/Create automation calls the native Junimo context without a player or farming XP. Native automation permits scythe crops. Create's ordinary destructive fallback cannot destroy immature crops, seasonal forage or giants. Real Create, Industrial Foregoing Plant Gatherer and PneumaticCraft DroneEntity/DroneAIHarvest operations were tested: genomes survive harvest and repeated operations cannot duplicate produce. That server drone test does not cover a complete programmed flight route.

Automated output consumers must accept full stacks. If an external inventory accepts a stack and then throws, the crop remains harvested: arbitrary external storage cannot be safely rolled back. Compensation for canceled external Create item drops is not implemented.

## Existing worlds and disabling support

Keep a full world backup before installing a different version. Existing native plantings receive a stable baseline genome on access or the first migration of their loaded chunk; native progress is not recreated. Save schema remains 1. Unknown future schemas are preserved in full and made read-only; malformed entries remain preserved and block reinitialization of their roots.

With `compat.starfield=false`, native blocks and saved seeds are neither converted nor deleted. Genetic effects and integration tools are disabled. Unchanged crops retain genomes, UUIDs and crop-stick material for later reactivation. Native wild-seed transformation to forage retains this record even while disabled. Native growth, death and harvest continue under Starfield's rules: actual removal deletes the attachment; the disabled mechanism does not return old sticks. Seeds harvested without a component while disabled remain baseline seeds. Seasonal grace does not protect crops while integration is disabled.

Disabling support and removing Starfield are different actions: loading a world without the original mod affects its own blocks and registries. Absent-mod loading is tested separately, rather than by removing the mod from a migrated world.

## Large farms and networking

Saved plants are indexed by dimension/chunk without loading the world. Migration accounts for local Y offsets within sections, including negative heights. On chunk load/first watch, it scans only sections whose palettes contain native crops. The same loaded chunk object is not rescanned for every player. A new chunk object, replaced SavedData or refreshed catalog permits another scan. Weak references prevent the scan cache from retaining chunks; there is no continuous full-world scan. Saving still writes all data as a save operation, not as a growth tick.

Snapshots and updates go to players watching the corresponding chunk and dimension. The client clears caches on disconnect, world/dimension changes and chunk unload; entries include session and planting UUID. Two real graphical clients passed initial snapshots, material changes, dimension transitions, forage removal, distant chunk unload, return and reconnect. When watching stops, the server explicitly clears the chunk record because distant teleports can skip the normal client unload event. Model screenshots and the exact verification scope are in the completion report.

## Sources and materials

Official project: [Starfield Pastoral](https://github.com/ChangQingElysium/Starfield-Pastoral), [release 0.6.3fix1](https://github.com/ChangQingElysium/Starfield-Pastoral/releases/tag/v0.6.3fix1). The October 9, 2026 review verifies that release; only the pinned Minecraft 1.21.1/NeoForge JAR in `tests/starfield/dependencies.json` is accepted.

[Starfield's license at the verified tag](https://github.com/ChangQingElysium/Starfield-Pastoral/blob/v0.6.3fix1/LICENSE.md) is a Custom Non-Commercial Fan Project License, not MIT. It restricts redistribution of Starfield itself, modified builds and extracted materials. AgriCraft remains MIT-licensed and does not relicense Starfield. Its release archive contains no Starfield JAR, classes, textures, models, sounds or test assets; users install the native dependency separately from the official source. Excluding those materials is not independent legal permission for future derivative builds. Repackaging Starfield or publishing derivative works requires a separate review of its terms.
