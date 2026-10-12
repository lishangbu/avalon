package io.github.lishangbu.avalon.world.domain;

/**
 * GatheringSupplyRules 使用的类型化规则数据，包含id、quantity、health、mana等字段。
 * 保持原有数据语义和构造规则；拆为顶层类型后，调用方无需依赖规则实现的内部声明。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param quantity 物品或资源数量
 * @param health 生命属性
 * @param mana 魔力属性
 */
public record GatheringSupplyRulesSupply(long id, int quantity, int health, int mana) {
}
