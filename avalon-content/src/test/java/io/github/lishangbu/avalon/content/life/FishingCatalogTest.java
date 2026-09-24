package io.github.lishangbu.avalon.content.life;

import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FishingCatalogTest {

	private final WorldCatalog world = new WorldCatalog();

	private final ItemCatalog items = new ItemCatalog(world);

	private final FishingCatalog.Definition source = new FishingCatalog(items, world).definition();

	@Test
	void memoryDefinitionsRejectInvalidNumbersWithoutSchema() {
		var fish = source.fish().getFirst();
		for (var invalid : List.of(new FishingCatalog.Fish(fish.id(), fish.name(), 0, 1, 1, fish.locations()),
				new FishingCatalog.Fish(fish.id(), fish.name(), 11, 1, 1, fish.locations()),
				new FishingCatalog.Fish(fish.id(), fish.name(), 1, 0, 1, fish.locations()),
				new FishingCatalog.Fish(fish.id(), fish.name(), 1, 11, 1, fish.locations()),
				new FishingCatalog.Fish(fish.id(), fish.name(), 1, 1, 0, fish.locations()),
				new FishingCatalog.Fish(fish.id(), fish.name(), 1, 1, -1, fish.locations()))) {
			var error = assertThrows(IllegalArgumentException.class, () -> catalog(invalid));
			assertTrue(error.getMessage().contains("/content/fishing.json [" + fish.id() + "]"));
		}
		for (int boundary : List.of(1, 10))
			assertDoesNotThrow(() -> catalog(
					new FishingCatalog.Fish(fish.id(), fish.name(), boundary, boundary, 1, fish.locations())));
	}

	@Test
	void memoryDefinitionsRejectBlankNamesAndEmptyOrDuplicateLocations() {
		var fish = source.fish().getFirst();
		assertThrows(IllegalArgumentException.class,
				() -> catalog(new FishingCatalog.Fish(fish.id(), " ", 1, 1, 1, fish.locations())));
		for (var locations : List.of(List.<String>of(),
				List.of(fish.locations().getFirst(), fish.locations().getFirst())))
			assertThrows(IllegalArgumentException.class,
					() -> catalog(new FishingCatalog.Fish(fish.id(), fish.name(), 1, 1, 1, locations)));
	}

	private FishingCatalog catalog(FishingCatalog.Fish fish) {
		return new FishingCatalog(new FishingCatalog.Definition(source.bait(), List.of(fish)), items, world);
	}

}
