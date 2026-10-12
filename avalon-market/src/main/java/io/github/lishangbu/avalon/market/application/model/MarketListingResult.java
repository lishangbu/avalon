package io.github.lishangbu.avalon.market.application.model;

import java.time.Instant;

/**
 * MarketListingCommandService 使用的命令或结果数据，包含listingId、expiresAt等字段。
 * 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param listingId 已有市场挂牌编号
 * @param expiresAt 失效时刻
 */
public record MarketListingResult(String listingId, Instant expiresAt) {
}
