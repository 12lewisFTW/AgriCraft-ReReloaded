# Proposal for narrow Starfield API extensions

Discussion draft; not sent to the author and does not modify Starfield.
Baseline: official `0.6.3fix1`, commit `df2151c66a2a9c02855e4db23108e57c422ce78e`.

Testing a native block with separate SavedData led to choosing that representation. Proposals 1–2 below concern the rejected third-party-block design and no longer block integration. Proposal 3 still applies to future seed-maker support.

## 1. Shared native species executor

The challenge is representing a Starfield plant on a third-party block while retaining its exact rules without a second implementation of phases, fertilizers or species-specific behavior. `StardewCropRuntimeAdapter` provides scheduling and context but requires the addon to mutate the plant itself.

A public operation should calculate one native-species day from a state snapshot and `StardewCropDailyContext`, using a read/write view interface. The native block should use the same executor. Starfield owns the implementation and dispatches the special behavior of rice, taro, fiber, wild seeds and other species.

The contract must explicitly define:

- the source of species metadata and the authoritative snapshot;
- soil suitability, actual water and the infertile-soil allowance already applied;
- historical season/date and location exceptions, without reading the current global season;
- phase transitions, regrowth, death/replacement and actions on additional parts;
- applying native fertilizer once and adjusting durations for genes;
- ownership of the processed date and prevention of repeated days;
- committing the result after revalidating planting identity.

Reflection/access-transformer access to a private method, temporary block substitution or dimension substitution does not satisfy this contract. A simplified executor based only on `phaseDays` cannot retain all native behavior either.

Harvest calculation needs a similar shared operation: native species + snapshot + existing `StardewCropHarvestContext` → prepared stacks, XP and state transition. Failure before commit must publish no output. Star quality, extra drops and required tools remain Starfield's rules.

## 2. Safely replacing registered plants with a giant block entity

`GiantCropGrowth` currently rejects every cell containing a block entity. This protects containers but also excludes a registered plant on AgriCraft crop sticks.

An opt-in contract is needed only for recognized root plantings: complete snapshot preparation, permission to remove, commit and restoration if the whole operation fails. Unrelated containers remain rejected. Stick restitution and data cleanup occur once after the entire footprint succeeds, rather than on each removed part.

Correction to the original maturity plan: the official API requires the triggering plant to be mature and watered; the other roots must be intact, share the same produce and lie in the allowed area. The release contains five giant products: cauliflower, melon, pumpkin, powder_melon and qi_fruit, with a base 1% chance and 3×3 footprint. Requiring all nine plants to be mature would break parity with native farmland.

## 3. Seed-maker component transfer

`StardewArtisanResolvers.SeedMakerOutputResolver` receives an `Item` and selects an output `Item`. After native random rolls, `SeedMakerBlockEntity` creates a new `ItemStack`. That resolver cannot transfer the input item's genome.

A result hook should receive a defensive copy of the input stack and the final selected output stack, after native species/count selection but before the production plan is saved. It transfers only its own components without changing the native roll. Mismatched species, mixed seeds and bonus ancient seeds receive no unrelated genome. Native grapes may yield summer wild seeds; that is not a grape-seed result.

## Alternative representation

The prototype retains the real native block, storing crop sticks/genome in separate world data. AgriCraft then needs external-root `AgriCrop` lookup, stick rendering, breeding and tool interactions.

Four native crop cycles, regrowth and actual giant formation were verified. This is the architecture chosen for subsequent iterations; lookup, rendering and gameplay tools are not yet implemented at this historical milestone. Evidence and limits are in the [iteration 0 specification](ITERATION-0-SPEC.md).
