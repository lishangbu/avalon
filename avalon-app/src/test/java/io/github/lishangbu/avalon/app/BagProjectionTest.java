package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.inventory.application.model.BagProjection;
import io.github.lishangbu.avalon.inventory.application.model.InventoryItemView;

import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** 显示格数必须与服务端容量一致，来源不同可以合并，绑定不同不能合并，任务不占格。 */
class BagProjectionTest extends SpringIntegrationTest {

	@Autowired
	BagProjection projection;

	@Test
	void productionPotionUsesExplicitCategoryDespiteItsNumberedCode() {
		var bag = projection.from(List.of(item("1", "LIFE_HEALTH_POTION_1", 1, false, null)));
		assertEquals("药品", bag.stacks().getFirst().category());
	}

	@Test
	void mergesSourcesSplitsAtNinetyNineAndKeepsEquipmentIdentity() {
		var first = item("1", "HEALTH_POTION", 98, false, null);
		var second = item("2", "HEALTH_POTION", 3, false, null);
		var bound = item("3", "HEALTH_POTION", 2, true, null);
		var task = item("4", "PET_GUIDE", 1, true, null);
		var gear = item("5", "STARTER_WEAPON", 1, true, "WEAPON");
		var bag = projection.from(List.of(first, second, bound, task, gear));
		assertEquals(4, bag.usedSlots());
		assertEquals(60, bag.capacity());
		assertEquals(List.of(99, 2, 2), bag.stacks().stream().map(stack -> stack.quantity()).toList());
		assertEquals(103, bag.stacks().stream().mapToInt(stack -> stack.quantity()).sum());
		assertEquals(2, bag.stacks().stream().filter(stack -> !stack.bound()).count());
		assertEquals(3, bag.stacks().stream().map(stack -> stack.key()).distinct().count());
		assertEquals(List.of(task), bag.tasks());
		assertEquals(List.of(gear), bag.equipment());
		assertEquals(98, first.quantity(), "投影不修改库存来源数量");
	}

	private InventoryItemView item(String id, String code, int quantity, boolean bound, String slot) {
		return new InventoryItemView(id, code, code, quantity, slot, 1, Map.of(), false, bound, null);
	}

}
