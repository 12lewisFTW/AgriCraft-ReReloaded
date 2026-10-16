package com.agricraft.agricraft.datagen;

import com.agricraft.agricraft.common.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;


public class ModRecipeProvider extends RecipeProvider {

	public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	protected void buildRecipes(RecipeOutput output) {
		TagKey<Item> stick = TagKey.create(Registries.ITEM, ResourceLocation.parse("agricraft:rods/wooden"));
		TagKey<Item> ironNugget = TagKey.create(Registries.ITEM, ResourceLocation.parse("agricraft:nuggets/iron"));
		TagKey<Item> obsidian = TagKey.create(Registries.ITEM, ResourceLocation.parse("agricraft:obsidian"));
		TagKey<Item> seed = TagKey.create(Registries.ITEM, ResourceLocation.parse("agricraft:seeds"));
		TagKey<Item> ironIngot = TagKey.create(Registries.ITEM, ResourceLocation.parse("agricraft:ingots/iron"));
		ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.WOODEN_CROP_STICKS.get(), 8)
				.pattern("##")
				.pattern("##")
				.define('#', stick)
				.unlockedBy("has_stick", has(stick))
				.save(output);
		ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.IRON_CROP_STICKS.get(), 8)
				.pattern("#")
				.pattern("#")
				.define('#', ironNugget)
				.unlockedBy("has_nugget", has(ironNugget))
				.save(output);
		ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.OBSIDIAN_CROP_STICKS.get(), 8)
				.pattern("#")
				.pattern("#")
				.define('#', obsidian)
				.unlockedBy("has_obsidian", has(obsidian))
				.save(output);
		ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ModItems.JOURNAL.get())
				.requires(Items.WRITABLE_BOOK)
				.requires(seed)
				.unlockedBy("has_seed", has(seed))
				.save(output);
		ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.SEED_ANALYZER.get())
				.pattern("sgs")
				.pattern(" bs")
				.pattern("www")
				.define('s', stick)
				.define('g', TagKey.create(Registries.ITEM, ResourceLocation.parse("agricraft:glass_panes/colorless")))
				.define('b', Items.SMOOTH_STONE_SLAB)
				.define('w', TagKey.create(Registries.ITEM, ResourceLocation.parse("minecraft:wooden_slabs")))
				.unlockedBy("has_seed", has(seed))
				.save(output);
		ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.CLIPPER.get())
				.pattern(" i ")
				.pattern("irs")
				.pattern(" s ")
				.define('i', ironIngot)
				.define('r', Items.SHEARS)
				.define('s', stick)
				.unlockedBy("has_iron", has(ironIngot))
				.save(output);
		ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.MAGNIFYING_GLASS.get())
				.pattern(" g")
				.pattern("s ")
				.define('g', TagKey.create(Registries.ITEM, ResourceLocation.parse("agricraft:glass_panes/colorless")))
				.define('s', stick)
				.unlockedBy("has_stick", has(stick))
				.save(output);
		ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.WOODEN_RAKE.get())
				.pattern("f  ")
				.pattern(" s ")
				.pattern("  s")
				.define('f', ModItems.WOODEN_CROP_STICKS.get())
				.define('s', stick)
				.unlockedBy("has_stick", has(stick))
				.save(output);
		ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.IRON_RAKE.get())
				.pattern("b  ")
				.pattern(" s ")
				.pattern("  s")
				.define('b', Items.IRON_BARS)
				.define('s', stick)
				.unlockedBy("has_stick", has(stick))
				.save(output);
		ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.TROWEL.get())
				.pattern("iS")
				.pattern(" s")
				.define('s', stick)
				.define('i', ironIngot)
				.define('S', TagKey.create(Registries.ITEM, ResourceLocation.parse("agricraft:string")))
				.unlockedBy("has_iron", has(ironIngot))
				.save(output);
		ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.SEED_BAG.get())
				.pattern(" s ")
				.pattern("l l")
				.pattern(" l ")
				.define('s', TagKey.create(Registries.ITEM, ResourceLocation.parse("agricraft:string")))
				.define('l', TagKey.create(Registries.ITEM, ResourceLocation.parse("agricraft:leather")))
				.unlockedBy("has_seed", has(seed))
				.save(output);

		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.AMETHYST_SHARD)
				.pattern("ppp")
				.pattern("ppp")
				.pattern("ppp")
				.define('p', ModItems.AMATHYLLIS_PETAL.get())
				.unlockedBy("has_petal", has(ModItems.AMATHYLLIS_PETAL.get()))
				.save(output, ResourceLocation.parse("agricraft:amethyst_shard"));
		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.COAL)
				.pattern("ppp")
				.pattern("ppp")
				.pattern("ppp")
				.define('p', ModItems.COAL_PEBBLE.get())
				.unlockedBy("has_pebble", has(ModItems.COAL_PEBBLE.get()))
				.save(output, ResourceLocation.parse("agricraft:coal"));
		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.COPPER_INGOT)
				.pattern("nnn")
				.pattern("nnn")
				.pattern("nnn")
				.define('n', ModItems.COPPER_NUGGET.get())
				.unlockedBy("has_nugget", has(ModItems.COPPER_NUGGET.get()))
				.save(output, ResourceLocation.parse("agricraft:copper_ingot"));
		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.DIAMOND)
				.pattern("sss")
				.pattern("sss")
				.pattern("sss")
				.unlockedBy("has_shard", has(ModItems.DIAMOND_SHARD.get()))
				.define('s', ModItems.DIAMOND_SHARD.get())
				.save(output, ResourceLocation.parse("agricraft:diamond"));
		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.EMERALD)
				.pattern("sss")
				.pattern("sss")
				.pattern("sss")
				.unlockedBy("has_shard", has(ModItems.EMERALD_SHARD.get()))
				.define('s', ModItems.EMERALD_SHARD.get())
				.save(output, ResourceLocation.parse("agricraft:emerald"));
		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.NETHERITE_SCRAP)
				.pattern("sss")
				.pattern("sss")
				.pattern("sss")
				.unlockedBy("has_sliver", has(ModItems.NETHERITE_SLIVER.get()))
				.define('s', ModItems.NETHERITE_SLIVER.get())
				.save(output, ResourceLocation.parse("agricraft:netherite_scrap"));
		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.QUARTZ)
				.pattern("sss")
				.pattern("sss")
				.pattern("sss")
				.unlockedBy("has_shard", has(ModItems.QUARTZ_SHARD.get()))
				.define('s', ModItems.QUARTZ_SHARD.get())
				.save(output, ResourceLocation.parse("agricraft:quartz"));
	}

}
