# Create: mechanical harvesting

The **NeoForge** build includes the Create harvester adapter; a separate AgriCraft Create compatibility addon is not needed for this fork. The release gameplay checks used **Create 6.0.10-280**. This is a harvesting integration, not a new crop or a change to Create's power system.

## Build a harvesting contraption

Mount a Mechanical Harvester so its moving path intersects your crop bed. Provide contraption storage and an unloading route. Mature ordinary AgriCraft crops use their normal genetic harvest products and return to the species' post-harvest stage, without consuming a replanting seed. Create collects the output into contraption storage when possible or drops it into the world when storage cannot take it.

Plan enough storage and unloading capacity for [Gain 9–10 harvests](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Genes-and-Balance), which can perform four ordinary produce trials. A fast moving contraption increases the frequency of visits; it does not force crop growth or increase a species' yield per successful harvest.

## Time passes for mature ordinary crops

For an ordinary AgriCraft crop with a plant, the adapter calls the crop's harvest action. If that action does not harvest, the adapter moves its growth stage back to a previous stage; weeds can also regress. **Repeatedly sweeping immature ordinary AgriCraft plants can delay their growth.** Schedule harvest passes around maturity rather than leaving an early-stage bed under continuous passes.

This behavior differs from the native Starfield path. Do not infer the same handling for both kinds of crop.

## Native Starfield fields

With the supported [Starfield integration](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Starfield-Pastoral) active, Create uses the actual native harvest and regrowth behavior. Upper parts resolve to the same planting, avoiding an extra harvest of a tall crop. Immature native crops are refused without destructive fallback; seasonal forage and giants are not handled by this automation route.

Native primary produce gets its source genome and Gain bonus once. No player is supplied, so automation grants no player farming XP or profession bonus. One-shot crops leave empty attached single sticks; regrowing crops keep their planting and material. Plan native replanting separately for one-shot species.

## Capacity and support limits

World-dropped items still need collection before they despawn. Storage should accept full stacks. An external destination that accepts output and then throws an error cannot be safely rolled back into a second unharvested crop. This adapter does not promise recovery of arbitrary failing inventory systems or canceled item drops.

The adapter is built into NeoForge and has no separate `compat.create` switch. Real contraption operation, native output genomes and reload behavior are documented in the [gameplay acceptance report](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/1.21.1/docs/starfield/GAMEPLAY-ACCEPTANCE.md). Converted crops retain their ordinary harvesting rules.

## Basis of this guide

Defaults refer to AgriCraft ReReloaded **4.0.20**, Minecraft **1.21.1**. Modpacks can override data and settings. [Harvester behavior](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/6d6935f09e749923566ff0de3d0c76e0da5b3c4c/neoforge/src/main/java/com/agricraft/agricraft/mixin/neoforge/create/HarvesterMovementBehaviourMixin.java).
