package io.github.lishangbu.avalon.battle.api.model;

/**
 * BattleResource 使用的命令或结果数据，包含encounter、requestId等字段。 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param encounter 遭遇引用
 * @param requestId 本次命令的幂等请求号，重试沿用原值
 */
public record BattleStart(String encounter, String requestId) {
}
