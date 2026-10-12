package io.github.lishangbu.avalon.content.item;

import io.github.lishangbu.avalon.content.item.model.ItemCategory;
import io.github.lishangbu.avalon.content.item.model.ItemConsumableType;
import io.github.lishangbu.avalon.content.item.model.ItemDefinition;
import io.github.lishangbu.avalon.content.item.model.ItemItem;
import io.github.lishangbu.avalon.content.item.model.ItemRecovery;
import io.github.lishangbu.avalon.content.item.model.ItemScope;
import io.github.lishangbu.avalon.content.item.model.ItemTool;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import tools.jackson.databind.json.JsonMapper;

class ItemDescriptionsTest {

	@Test
	void allKindsUseOneEntryAndRawRecordsRoundTripWithoutAddingText() {
		var world = new WorldCatalog();
		var potion = new ItemItem("P", "药", "补给。", ItemCategory.CONSUMABLE, ItemConsumableType.POTION,
				ItemScope.GENERAL, false, null, new ItemRecovery(13, 7), null, null, null, null);
		var meal = new ItemItem("F", "料理", "食用。", ItemCategory.CONSUMABLE, ItemConsumableType.FOOD, ItemScope.GENERAL,
				false, null, new ItemRecovery(17, 9), null, null, null, null);
		var tool = new ItemItem("T", "工具", "使用。", ItemCategory.TOOL, null, ItemScope.GENERAL, false, null, null,
				new ItemTool("AXE", 4, 100), null, null, null);
		var material = new ItemItem("M", "材料", "材料。", ItemCategory.MATERIAL, null, ItemScope.GENERAL, false, null, null,
				null, null, null, null);
		var items = new ItemCatalog(new ItemDefinition(List.of(potion, meal, tool, material)), world);
		var descriptions = new ItemDescriptions(items);
		for (int repeat = 0; repeat < 2; repeat++) {
			assertEquals("补给。恢复13生命、7魔力。", descriptions.description("P"));
			assertEquals("食用。恢复17生命、9魔力。", descriptions.description("F"));
			assertEquals("使用。覆盖生活技能9至10级。", descriptions.description("T"));
			assertEquals("材料。", descriptions.description("M"));
		}
		var mapper = JsonMapper.builder().build();
		assertEquals(potion, mapper.readValue(mapper.writeValueAsString(potion), ItemItem.class));
		var error = assertThrows(IllegalArgumentException.class, () -> descriptions.description("MISSING"));
		assertTrue(error.getMessage().contains("/content/items.json [MISSING] items.id=MISSING"));
		var missingEquipment = assertThrows(IllegalArgumentException.class, () -> items.equipment("MISSING"));
		assertTrue(missingEquipment.getMessage().contains("equipment.id=MISSING"));
	}

}
