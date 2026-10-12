package io.github.lishangbu.avalon.market.domain;

/**
 * MarketRules 使用的类型化规则数据，包含gross、tax、sellerNet等字段。
 * 保持原有数据语义和构造规则；拆为顶层类型后，调用方无需依赖规则实现的内部声明。
 *
 * @param gross 交易总额
 * @param tax 交易税额
 * @param sellerNet 卖方净收益
 */
public record MarketRulesProceeds(long gross, long tax, long sellerNet) {
}
