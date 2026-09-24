package io.github.lishangbu.avalon.content.item;

import io.github.lishangbu.avalon.content.ContentChecks;

import io.github.lishangbu.avalon.content.creature.LearningCatalog;
import io.github.lishangbu.avalon.content.ContentLoader;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;

/** 碎片配方在启动时校验，技能书成品必须对应已实现的学习目录，客户端不得覆盖成本。 */
@Component
public class CraftingCatalog {

	private final List<Recipe> recipes;

	public CraftingCatalog(ItemCatalog items, LearningCatalog learning) {
		recipes = ContentLoader.read("/content/crafting.json", Definition.class).recipes();
		var ids = new HashSet<String>();
		var fragments = new HashSet<String>();
		for (var recipe : recipes) {
			ContentChecks.checkId("/content/crafting.json", "recipes.id", ids, recipe.id());
			if (!items.hasItem(recipe.fragment()) || learning.book(recipe.product()) == null
					|| !fragments.add(recipe.fragment()) || recipe.quantity() <= 0 || recipe.gold() < 0)
				throw ContentChecks.invalid("/content/crafting.json", recipe.id(), "recipe", recipe, "碎片配方无效");
		}
	}

	public List<Recipe> recipes() {
		return recipes;
	}

	public Recipe recipe(String id) {
		return recipes.stream().filter(recipe -> recipe.id().equals(id)).findFirst().orElse(null);
	}

	public record Recipe(String id, String fragment, String product, int quantity, int gold) {
	}

	public record Definition(List<Recipe> recipes) {
		public Definition {
			recipes = List.copyOf(recipes);
		}
	}

}
