# Mod Menu: opening AgriCraft settings on Fabric

Mod Menu is an **optional Fabric client** convenience integration. AgriCraft registers an entry that opens the Resourceful Config screen from the mod list. It adds no crops, stat multipliers or server farming behavior.

## Use the settings screen

Open the Fabric mod list, select AgriCraft and open its configuration screen. [Resourceful Config](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Resourceful-Config) is still the required library that owns the settings. If the entry is absent, confirm Mod Menu is installed in the client instance and that its version matches your loader.

Use the screen to inspect the configured core, stat, irrigation, greenhouse and compatibility categories. For gameplay changes on a dedicated server, update that server's rules; a local client setting does not override its farm balance. Restart after changing compatibility activation or stat bounds.

## Scope

This module is Fabric-specific. It does not activate NeoForge-only integrations such as Starfield, Create's adapter, Plant Gatherer or harvesting drones. Displaying a configuration category is not evidence that a native compatibility module exists on that loader.

See [Configuration and Troubleshooting](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Configuration-and-Troubleshooting) for the actual configuration keys and [Compatibility Overview](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Compatibility-Overview) for platform support.

## Basis of this guide

Defaults refer to AgriCraft ReReloaded **4.0.20**, Minecraft **1.21.1**. Modpacks can override data and settings. [Fabric configuration entry](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/6d6935f09e749923566ff0de3d0c76e0da5b3c4c/fabric/src/main/java/com/agricraft/agricraft/common/config/fabric/ModMenuConfig.java).
