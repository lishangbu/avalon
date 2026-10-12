package io.github.lishangbu.avalon.creature.application.model;

/**
 * CreatureEvolutionCommandService 使用的命令或结果数据，包含creatureId、expected等字段。
 * 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param creatureId 已有幻兽实例编号
 * @param expected 客户端看见的预期数据，执行时由服务端重新比较
 */
public record CreatureEvolutionPayload(long creatureId, CreatureEvolutionPreview expected) {
}
