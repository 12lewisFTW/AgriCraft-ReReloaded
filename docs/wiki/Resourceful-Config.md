# Resourceful Config: the shared settings system

Resourceful Config is a **required dependency** for this AgriCraft release on both loaders. The verified release environment uses **3.0.11**. It provides AgriCraft's configuration categories and settings interface; it does not add plants, harvest bonuses or a new farming progression.

## Server rules and client preferences

AgriCraft stores its configuration in **`config/agricraft.jsonc`**. The categories include `core`, `stats`, `irrigation`, `greenhouse` and `compat`. In multiplayer, changing a local client's preferences does not authorize a change to the server's growth, breeding or compatibility rules. Coordinate gameplay settings with the server configuration.

On Fabric, [Mod Menu](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Mod-Menu) supplies the convenient screen entry. On NeoForge, the available mod configuration screen is supplied through the loader's mod UI. Restart after changing compatibility activation and stat bounds, then confirm the intended settings in the active instance.

## Balance choices

The default stat range is 1–10 with visible traits. A hidden stat still participates in gameplay; hiding is a presentation setting. Equal minimum and maximum bounds freeze breeding at that defined value rather than universally erasing the stat's effect. Native Starfield's normalized bonus uses zero when the bounds are equal; ordinary crop formulas have their own behavior at the configured value.

For exact option names and defaults, use [Configuration and Troubleshooting](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Configuration-and-Troubleshooting). Crop products, soil targets and species mutation recipes are mostly datapack data, not options in this library's screen. Use the crop catalogs alongside the configuration guide when designing a server balance.

## Basis of this guide

Defaults refer to AgriCraft ReReloaded **4.0.20**, Minecraft **1.21.1**. Modpacks can override data and settings. [Registered configuration](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/6d6935f09e749923566ff0de3d0c76e0da5b3c4c/common/src/main/java/com/agricraft/agricraft/api/config/AgriCraftConfig.java).
