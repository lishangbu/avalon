package io.github.lishangbu.avalon.inventory.application.model;

import java.util.List;

/** 可见配方及可逐项选择的库存来源，不返回其他玩家物品或尚未取得的特色碎片。 */
public record CraftingView(int usedSlots, int capacity, List<CraftingViewRecipe> recipes) {

}
