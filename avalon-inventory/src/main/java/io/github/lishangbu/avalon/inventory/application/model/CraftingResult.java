package io.github.lishangbu.avalon.inventory.application.model;

/** 公开结果不依赖成品是否仍在背包；同一请求始终返回这次结算的物品与绑定状态。 */
public record CraftingResult(String product, String name, boolean bound) {
}
