package io.github.lishangbu.avalon.content.battle;

import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.creature.SpeciesStatsCatalog;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CaptureBallCatalogTest {

	@Test
	void everyOpenLevelHasAnAppropriateRenewableBallAndDropsPreserveItsFamily() {
		var world = new WorldCatalog();
		var items = new ItemCatalog(world);
		var creatures = new CreatureCatalog();
		var quests = new QuestCatalog(world, items, creatures);
		var battles = new BattleCatalog(creatures, items, world, quests, new WildCatalog(creatures, items),
				new SpeciesStatsCatalog());
		var balls = new CaptureBallCatalog(items);
		for (int level = 1; level <= GrowthRules.LEVEL_CAP; level++) {
			for (String base : new String[] { "SEAL_I", "STRONG_SEAL_I" }) {
				var selected = balls.find(balls.dropForLevel(base, level));
				assertEquals(((level - 1) / 10 + 1) * 10, selected.level());
				assertEquals(balls.find(base).reinforced(), selected.reinforced());
				assertTrue(battles.definition()
					.products()
					.stream()
					.anyMatch(product -> product.code().equals(selected.code())));
			}
		}
		assertEquals("SEAL_V", balls.dropForLevel("SEAL_V", 1));
		assertEquals("NONE", balls.dropForLevel("NONE", 50));
		assertEquals("HEALTH_POTION", balls.dropForLevel("HEALTH_POTION", 50));
	}

}
