package io.github.lishangbu.avalon.market.application.model;

/**
 * MarketReturnCommandService 使用的命令或结果数据，包含listingId、remaining、itemId等字段。
 * 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param listingId 已有市场挂牌编号
 * @param remaining 剩余数量或余额
 * @param itemId 已有库存实例编号
 */
public record MarketReturnResult(String listingId, int remaining, String itemId) {
}
