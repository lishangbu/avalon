package io.github.lishangbu.avalon.world.domain;

import java.util.Map;

/**
 * GatheringSupplyRules 使用的类型化规则数据，包含sufficient、consumed、health、mana等字段。
 * 保持原有数据语义和构造规则；拆为顶层类型后，调用方无需依赖规则实现的内部声明。
 *
 * @param health 生命属性
 * @param mana 魔力属性
 */
public record GatheringSupplyRulesPlan(boolean sufficient, Map<Long, Integer> consumed, int health, int mana) {
}
