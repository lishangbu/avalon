package io.github.lishangbu.avalon.inventory.application.model;

import java.util.List;

/** 背包格是展示投影，不是可消费实例编号；装备仍携带真实实例，学习与合成继续明确选择库存来源。 */
public record BagView(int usedSlots, int capacity, List<InventoryItemView> equipment, List<Stack> stacks,
		List<InventoryItemView> tasks) {
	public record Stack(String key, String code, String name, String category, boolean bound, int quantity) {
	}
}
