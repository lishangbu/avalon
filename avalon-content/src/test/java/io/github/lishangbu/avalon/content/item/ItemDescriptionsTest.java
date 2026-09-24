package io.github.lishangbu.avalon.content.item;

import io.github.lishangbu.avalon.content.world.WorldCatalog;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

class ItemDescriptionsTest {

	@Test
	void allKindsUseOneEntryAndRawRecordsRoundTripWithoutAddingText() {
		var world = new WorldCatalog();
		var potion = new ItemCatalog.Item("P", "药", "补给。", ItemCatalog.Category.CONSUMABLE,
				ItemCatalog.ConsumableType.POTION, ItemCatalog.Scope.GENERAL, false, null,
				new ItemCatalog.Recovery(13, 7), null, null, null, null);
		var meal = new ItemCatalog.Item("F", "料理", "食用。", ItemCatalog.Category.CONSUMABLE,
				ItemCatalog.ConsumableType.FOOD, ItemCatalog.Scope.GENERAL, false, null,
				new ItemCatalog.Recovery(17, 9), null, null, null, null);
		var tool = new ItemCatalog.Item("T", "工具", "使用。", ItemCatalog.Category.TOOL, null, ItemCatalog.Scope.GENERAL,
				false, null, null, new ItemCatalog.Tool("AXE", 4, 100), null, null, null);
		var material = new ItemCatalog.Item("M", "材料", "材料。", ItemCatalog.Category.MATERIAL, null,
				ItemCatalog.Scope.GENERAL, false, null, null, null, null, null, null);
		var items = new ItemCatalog(new ItemCatalog.Definition(List.of(potion, meal, tool, material)), world);
		var descriptions = new ItemDescriptions(items);
		for (int repeat = 0; repeat < 2; repeat++) {
			assertEquals("补给。恢复13生命、7魔力。", descriptions.description("P"));
			assertEquals("食用。恢复17生命、9魔力。", descriptions.description("F"));
			assertEquals("使用。覆盖生活技能9至10级。", descriptions.description("T"));
			assertEquals("材料。", descriptions.description("M"));
		}
		var mapper = JsonMapper.builder().build();
		assertEquals(potion, mapper.readValue(mapper.writeValueAsString(potion), ItemCatalog.Item.class));
		var error = assertThrows(IllegalArgumentException.class, () -> descriptions.description("MISSING"));
		assertTrue(error.getMessage().contains("/content/items.json [MISSING] items.id=MISSING"));
		var missingEquipment = assertThrows(IllegalArgumentException.class, () -> items.equipment("MISSING"));
		assertTrue(missingEquipment.getMessage().contains("equipment.id=MISSING"));
	}

}
