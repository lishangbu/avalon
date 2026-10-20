package io.github.lishangbu.avalon.content.world;

import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorldServicesAccessTest {

	@Test
	void facilitiesAndResourceRegionsRequireTheirChapter() {
		var world = new WorldCatalog();
		var gates = Map.ofEntries(Map.entry("ACADEMY_SHOP", "O012"), Map.entry("ACADEMY_PETS", "O012"),
				Map.entry("GNOME_FORGE", "O014"), Map.entry("GNOME_LEATHERWORKS", "O014"),
				Map.entry("GNOME_APOTHECARY", "O014"), Map.entry("GNOME_KITCHEN", "O014"),
				Map.entry("GNOME_ROWAN_SHOP", "O014"), Map.entry("GNOME_GARDEN", "O014"),
				Map.entry("COPPER_WETLAND", "O015"), Map.entry("IRON_HIGHLAND", "N025"),
				Map.entry("SILVER_FROSTWOOD", "F001"), Map.entry("GOLD_DAWN_VALLEY", "J025"),
				Map.entry("UNDINE_CARE_STATION", "N025"), Map.entry("UNDINE_WHARF", "N025"));
		gates.forEach((id, quest) -> {
			var place = world.place(id);
			assertTrue(world.accessible(place, fact -> fact.equals("QUEST:" + quest)), id);
			assertFalse(world.accessible(place, fact -> false), id);
		});
		for (String id : java.util.List.of("GNOME_CITY", "CITY_PETS", "CITY_SHOP", "GNOME_OLD_TRAIL", "GNOME_MINE_GATE",
				"UNDINE_CITY", "UNDINE_SHALLOWS"))
			assertFalse(world.accessible(world.place(id), fact -> false), id);
	}

	@Test
	void regionalRevivalPrecedesGeneralUnlockedCityAndVillageFallback() {
		var world = new WorldCatalog();
		var returns = world.definition().safeReturns();
		for (String origin : java.util.List.of("SALAMANDER_ASH_ROAD", "F_ARMORY_ENTRANCE")) {
			var destination = returns.stream()
				.filter(entry -> entry.fromLocations().isEmpty() || entry.fromLocations().contains(origin))
				.filter(entry -> entry.requires() == null
						|| java.util.Set.of("QUEST:O012", "QUEST:O014", "QUEST:F001").contains(entry.requires()))
				.findFirst()
				.orElseThrow();
			assertEquals("SALAMANDER_CITY", destination.location());
		}
		assertEquals("SQUARE", returns.getLast().location());
	}

}
