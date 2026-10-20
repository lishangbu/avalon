package io.github.lishangbu.avalon.content.item;

import io.github.lishangbu.avalon.content.ContentChecks;
import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.creature.LearningCatalog;
import io.github.lishangbu.avalon.content.item.model.CraftingDefinition;
import io.github.lishangbu.avalon.content.item.model.CraftingRecipe;
import java.util.HashSet;
import java.util.List;
import org.springframework.stereotype.Component;

/** 碎片配方在启动时校验，技能书成品必须对应已实现的学习目录，客户端不得覆盖成本。 */
@Component
public class CraftingCatalog {

	private final List<CraftingRecipe> recipes;

	public CraftingCatalog(ItemCatalog items, LearningCatalog learning) {
		recipes = ContentLoader.read("/content/items/crafting.json", CraftingDefinition.class).recipes();
		var ids = new HashSet<String>();
		var fragments = new HashSet<String>();
		for (var recipe : recipes) {
			ContentChecks.checkId("/content/items/crafting.json", "recipes.id", ids, recipe.id());
			if (!items.hasItem(recipe.fragment()) || learning.book(recipe.product()) == null
					|| !fragments.add(recipe.fragment()) || recipe.quantity() <= 0 || recipe.gold() < 0)
				throw ContentChecks.invalid("/content/items/crafting.json", recipe.id(), "recipe", recipe, "碎片配方无效");
		}
	}

	public List<CraftingRecipe> recipes() {
		return recipes;
	}

	public CraftingRecipe recipe(String id) {
		return recipes.stream().filter(recipe -> recipe.id().equals(id)).findFirst().orElse(null);
	}

}
