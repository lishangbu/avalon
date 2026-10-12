package io.github.lishangbu.avalon.market.application.model;

import java.util.List;

/**
 * MarketListingCommandService 使用的命令或结果数据，包含requestId、items、unitPrice等字段。
 * 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param requestId 本次命令的幂等请求号，重试沿用原值
 * @param items 物品声明列表
 * @param unitPrice 单价
 */
public record MarketListingBatchRequest(String requestId, List<MarketListingSelection> items, long unitPrice) {
}
