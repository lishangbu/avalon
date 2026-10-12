package io.github.lishangbu.avalon.inventory.application.model;

/**
 * EnhancementCommandService 使用的命令或结果数据，包含success、item等字段。
 * 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param success 是否成功
 * @param item 物品代码或物品数据
 */
public record EnhancementResult(boolean success, InventoryItemView item) {
}
