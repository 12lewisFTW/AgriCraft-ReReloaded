package com.agricraft.agricraft.api.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigOption;
import com.teamresourceful.resourcefulconfig.api.types.options.EntryType;

/**
 * Agricraft mode compatibility configuration category.
 */
@Category("compat")
public final class CompatConfig {

	public static final String MYSTICAL_AGRICULTURE_MOD_ID = "mysticalagriculture";
	public static final String STARFIELD_MOD_ID = "stardewcraft";

	@ConfigEntry(id = "starfield", type = EntryType.BOOLEAN, translation = "config.agricraft.compat.starfield")
	@Comment("Enable Starfield Pastoral compatibility on NeoForge. Only verified versions are supported; restart after changing this option.")
	public static boolean enableStarfield = true;

	@ConfigEntry(id = "mysticalagriculture", type = EntryType.BOOLEAN, translation = "config.agricraft.compat.mysticalagriculture")
	@Comment("Set to false to disable compatibility with Mystical Agriculture (in case things break)")
	public static boolean enableMysticalAgriculture = true;

	@ConfigEntry(id = "pneumaticcraft", type = EntryType.BOOLEAN, translation = "config.agricraft.compat.pneumaticcraft")
	@Comment("If true, harvesting drones will be able to harvest AgriCraft crops")
	public static boolean enablePneumaticCraft = true;

	@ConfigEntry(id = "industrialforegoing", type = EntryType.BOOLEAN, translation = "config.agricraft.compat.industrialforegoing")
	@Comment("If set to true, plant gatherer will be able to harvest AgriCraft crops")
	public static boolean enableIndustrialForegoing = true;

	/**
	 * Checks whether AgriCraft compatibility for a mod is enabled.
	 *
	 * @param modId the mod namespace
	 * @return {@code false} when the matching compatibility option is disabled
	 */
	public static boolean isCompatibilityEnabled(String modId) {
		return switch (modId) {
			case MYSTICAL_AGRICULTURE_MOD_ID -> enableMysticalAgriculture;
			case STARFIELD_MOD_ID -> enableStarfield;
			default -> true;
		};
	}

}
