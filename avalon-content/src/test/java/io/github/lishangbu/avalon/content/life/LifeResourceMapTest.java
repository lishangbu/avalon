package io.github.lishangbu.avalon.content.life;

import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;

import java.util.Set;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LifeResourceMapTest {

	@Test
	void everyMaterialTierAndSkillLevelHasAnInfiniteResourceSource() {
		var world = new WorldCatalog();
		var gathering = new GatheringCatalog(new ItemCatalog(world), world);
		for (int level = 1; level <= 10; level++) {
			int required = level;
			for (String type : Set.of("ORE", "WOOD", "HERB", "HIDE", "MEAT")) {
				String product = "LIFE_" + type + "_" + ((level - 1) / 2);
				assertTrue(
						gathering.nodes()
							.stream()
							.anyMatch(node -> node.level() == required && node.product().equals(product)),
						product + ":" + level);
			}
		}
	}

	@Test
	void higherFishUseConnectedRegionalWatersWithExistingStoryGates() {
		var world = new WorldCatalog();
		var fishing = new FishingCatalog(new ItemCatalog(world), world);
		String[] locations = { "RIVERSIDE", "COPPER_WETLAND", "IRON_HIGHLAND", "SILVER_FROSTWOOD", "GOLD_DAWN_VALLEY" };
		for (var fish : fishing.definition().fish())
			assertEquals(List.of(locations[(fish.level() - 1) / 2]), fish.locations());
		for (int tier = 1; tier < locations.length; tier++) {
			var place = world.place(locations[tier]);
			String required = new String[] { "", "QUEST:O015", "QUEST:N025", "QUEST:F001", "QUEST:J025" }[tier];
			assertFalse(world.accessible(place, fact -> false));
			assertTrue(world.accessible(place, fact -> fact.equals(required)));
			assertTrue(place.exits().stream().anyMatch(parent -> world.place(parent).exits().contains(place.id())));
		}
	}

}
