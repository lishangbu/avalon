package io.github.lishangbu.avalon.content.life;

import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CraftedEquipmentTest {

	@Test
	void everyTierHasWeaponsAndArmorWithFixedStatsAndRefinementInputs() {
		var world = new WorldCatalog();
		var items = new ItemCatalog(world);
		var production = new LifeProductionCatalog(items, world);
		var kinds = List.of("SWORD", "BOW", "STAFF", "WAND", "HEAD", "BODY", "HANDS", "LEGS", "FEET");
		for (int level = 1; level <= 10; level++) {
			int tier = (level - 1) / 2;
			for (String kind : kinds) {
				String code = "CRAFT_" + level + "_" + kind;
				var gear = items.equipment(code);
				var recipe = production.recipe(code);
				assertEquals(level * 5, gear.level());
				assertEquals(level, recipe.level());
				assertEquals(tier, production.minimumToolTier(recipe));
				assertEquals(gear.slot().equals("WEAPON") ? "ARTISAN" : "LEATHERWORK", recipe.skill());
				assertFalse(gear.slot().equals("ACCESSORY"));
				if (level % 2 == 0)
					assertEquals(1, recipe.ingredients().get("CRAFT_" + (level - 1) + "_" + kind));
				if (tier >= 2)
					assertTrue(recipe.ingredients().keySet().stream().anyMatch(value -> value.startsWith("RARE_")));
				if (level == 3)
					assertEquals(items.equipment("CRYSTAL_" + kind).stats(), gear.stats());
			}
		}
	}

}
