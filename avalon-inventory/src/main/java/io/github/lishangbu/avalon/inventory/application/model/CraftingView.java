package io.github.lishangbu.avalon.inventory.application.model;

import java.util.List;

/** 可见配方及可逐项选择的库存来源，不返回其他玩家物品或尚未取得的特色碎片。 */
public record CraftingView(int usedSlots, int capacity, List<Recipe> recipes) {
	public record Recipe(String id, String product, int quantity, int gold, List<Fragment> fragments) {
	}

	public record Fragment(String id, String name, int quantity, boolean bound) {
	}
}
