package io.github.lishangbu.avalon.content.life;

import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.item.model.ItemConsumableType;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FoodCatalogTest {

	@Test
	void mealsAndBattlePotionsAreDisjointAndViewsAreImmutable() {
		var items = new ItemCatalog(new WorldCatalog());
		var catalog = new FoodCatalog(items);
		assertNull(catalog.food("HEALTH_POTION"));
		assertNull(catalog.food("MISSING"));
		assertNull(catalog.food(null));
		assertThrows(UnsupportedOperationException.class, () -> catalog.foods().clear());
		for (var food : catalog.foods()) {
			assertSame(food, catalog.food(food.item()));
			assertEquals(ItemConsumableType.FOOD, items.item(food.item()).type());
			assertEquals(items.item(food.item()).recovery().health(), food.health());
			assertEquals(items.item(food.item()).recovery().mana(), food.mana());
		}
	}

	@Test
	void everyCropHasAUseAndEachFoodHasACookingRecipe() {
		var world = new WorldCatalog();
		var items = new ItemCatalog(world);
		var food = new FoodCatalog(items);
		var recipes = new LifeProductionCatalog(items, world);
		var farming = new FarmingCatalog(items, world);
		assertEquals(20, food.foods().size());
		for (var crop : farming.definition().crops())
			assertTrue(recipes.recipes().stream().anyMatch(recipe -> recipe.ingredients().containsKey(crop.product())),
					crop.id());
		for (var meal : food.foods()) {
			assertTrue(meal.health() > 0 || meal.mana() > 0);
			assertTrue(recipes.recipes()
				.stream()
				.anyMatch(recipe -> recipe.product().equals(meal.item()) && recipe.skill().equals("COOKING")));
		}
		assertEquals("HEALTH_POTION", recipes.recipe("BREW_HEALTH_POTION").product());
		assertEquals("MANA_POTION", recipes.recipe("BREW_MANA_POTION").product());
	}

	@Test
	void allFishHaveRecipesAndFishOilFeedsAlchemyAtTheSameTier() {
		var world = new WorldCatalog();
		var items = new ItemCatalog(world);
		var fishing = new FishingCatalog(items, world);
		var production = new LifeProductionCatalog(items, world);
		for (var fish : fishing.definition().fish()) {
			var uses = production.recipes()
				.stream()
				.filter(recipe -> recipe.ingredients().containsKey(fish.id()))
				.toList();
			assertFalse(uses.isEmpty(), fish.id());
			assertTrue(uses.stream().allMatch(recipe -> recipe.level() == fish.level()));
		}
		for (int tier = 0; tier < 5; tier++) {
			String oil = "FISH_OIL_" + tier;
			var recipe = production.recipe("OIL_REAGENT_" + tier);
			assertEquals("ALCHEMY", recipe.skill());
			assertEquals(tier * 2 + 2, recipe.level());
			assertEquals(1, recipe.ingredients().get(oil));
			assertEquals("LIFE_REAGENT_" + tier, recipe.product());
			assertEquals(2, production.recipes().stream().filter(value -> value.product().equals(oil)).count());
		}
	}

}
