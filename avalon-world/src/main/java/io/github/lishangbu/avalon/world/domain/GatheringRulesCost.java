package io.github.lishangbu.avalon.world.domain;

/**
 * GatheringRules 使用的类型化规则数据，包含health、mana等字段。 保持原有数据语义和构造规则；拆为顶层类型后，调用方无需依赖规则实现的内部声明。
 *
 * @param health 生命属性
 * @param mana 魔力属性
 */
public record GatheringRulesCost(int health, int mana) {
}
