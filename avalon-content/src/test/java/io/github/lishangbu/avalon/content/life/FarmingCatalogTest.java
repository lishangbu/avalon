package io.github.lishangbu.avalon.content.life;

import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FarmingCatalogTest {

	@Test
	void everyTierHasThreeCropsAndBothLevelsCanGainProficiency() {
		var world = new WorldCatalog();
		var items = new ItemCatalog(world);
		var farming = new FarmingCatalog(items, world);
		var shop = new LifeProductionCatalog(items, world);
		for (int tier = 0; tier < 5; tier++) {
			int current = tier;
			var crops = farming.definition()
				.crops()
				.stream()
				.filter(crop -> (crop.level() - 1) / 2 == current)
				.toList();
			assertEquals(3, crops.size());
			assertEquals(Set.of(current * 2 + 1, current * 2 + 2),
					crops.stream().map(FarmingCatalog.Crop::level).collect(Collectors.toSet()));
			assertEquals(Set.of(900, 3600, 14400),
					crops.stream().map(FarmingCatalog.Crop::growthSeconds).collect(Collectors.toSet()));
			for (var crop : crops)
				assertTrue(shop.offers()
					.stream()
					.anyMatch(offer -> offer.item().equals(crop.seed())
							&& offer.locations().containsAll(Set.of("SUPPLY_SHOP", "CITY_SHOP"))));
		}
	}

}
