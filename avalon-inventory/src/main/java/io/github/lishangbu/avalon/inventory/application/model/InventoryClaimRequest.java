package io.github.lishangbu.avalon.inventory.application.model;

/**
 * InventoryClaimService 使用的命令或结果数据，包含requestId、quantity等字段。
 * 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param requestId 本次命令的幂等请求号，重试沿用原值
 * @param quantity 物品或资源数量
 */
public record InventoryClaimRequest(String requestId, int quantity) {
}
