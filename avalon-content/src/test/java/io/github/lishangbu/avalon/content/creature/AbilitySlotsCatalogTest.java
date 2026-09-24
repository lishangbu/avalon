package io.github.lishangbu.avalon.content.creature;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AbilitySlotsCatalogTest {

	private final CreatureCatalog creatures = new CreatureCatalog(
			new CreatureCatalog.Definition(
					List.of(new CreatureCatalog.Pet("TEST", "测试", "水", 1, "A", "共享特性", List.of(), null))),
			new EvolutionCatalog.Definition(6, List.of()));

	private final Map<String, Map<String, String>> slots = Map.of("TEST",
			Map.of("A", "共享特性", "B", "共享特性", "H", "隐藏特性"));

	@Test
	void sharedAbilitiesNeedOnlyOneDescriptionAndMissingOrExtraIdsAreReported() {
		var valid = new AbilitySlotsCatalog(
				new AbilitySlotsCatalog.Definition(slots, Map.of("共享特性", "共用说明", "隐藏特性", "隐藏说明")), creatures);
		assertEquals("共用说明", valid.description(valid.ability("TEST", "B")));
		var error = assertThrows(IllegalArgumentException.class, () -> new AbilitySlotsCatalog(
				new AbilitySlotsCatalog.Definition(slots, Map.of("共享特性", "共用说明", "多余特性", "多余说明")), creatures));
		assertTrue(error.getMessage().contains("/content/ability-slots.json"));
		assertTrue(error.getMessage().contains("缺少=[隐藏特性]"));
		assertTrue(error.getMessage().contains("多余=[多余特性]"));
	}

	@Test
	void definitionsCopyNestedSlotsAndRejectBlankDescriptionsAndMissingSlots() {
		var mutable = new HashMap<>(slots.get("TEST"));
		var definition = new AbilitySlotsCatalog.Definition(Map.of("TEST", mutable),
				Map.of("共享特性", "说明", "隐藏特性", "说明"));
		mutable.clear();
		assertEquals(3, definition.species().get("TEST").size());
		assertThrows(UnsupportedOperationException.class, () -> definition.species().get("TEST").clear());
		assertThrows(IllegalArgumentException.class, () -> new AbilitySlotsCatalog(
				new AbilitySlotsCatalog.Definition(slots, Map.of("共享特性", " ", "隐藏特性", "说明")), creatures));
		assertThrows(IllegalArgumentException.class,
				() -> new AbilitySlotsCatalog(
						new AbilitySlotsCatalog.Definition(Map.of("TEST", Map.of("A", "共享特性")), Map.of("共享特性", "说明")),
						creatures));
	}

}
