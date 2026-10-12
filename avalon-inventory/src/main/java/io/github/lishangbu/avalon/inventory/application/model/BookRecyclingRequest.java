package io.github.lishangbu.avalon.inventory.application.model;

/**
 * BookRecyclingCommandService 使用的命令或结果数据，包含requestId、quantity、expected等字段。
 * 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param requestId 本次命令的幂等请求号，重试沿用原值
 * @param quantity 物品或资源数量
 * @param expected 客户端看见的预期数据，执行时由服务端重新比较
 */
public record BookRecyclingRequest(String requestId, int quantity, BookRecyclingOffer expected) {
}
