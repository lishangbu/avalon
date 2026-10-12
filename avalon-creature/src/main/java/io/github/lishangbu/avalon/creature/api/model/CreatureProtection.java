package io.github.lishangbu.avalon.creature.api.model;

/**
 * CreatureResource 使用的命令或结果数据，包含locked等字段。 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 */
public record CreatureProtection(Boolean locked) {
}
