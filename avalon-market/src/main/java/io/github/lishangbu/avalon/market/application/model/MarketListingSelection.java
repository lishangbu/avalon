package io.github.lishangbu.avalon.market.application.model;

import io.github.lishangbu.avalon.inventory.application.model.InventoryItemView;

/**
 * MarketListingCommandService 使用的命令或结果数据，包含itemId、quantity、expected等字段。
 * 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param itemId 已有库存实例编号
 * @param quantity 物品或资源数量
 * @param expected 客户端看见的预期数据，执行时由服务端重新比较
 */
public record MarketListingSelection(long itemId, int quantity, InventoryItemView expected) {
}
