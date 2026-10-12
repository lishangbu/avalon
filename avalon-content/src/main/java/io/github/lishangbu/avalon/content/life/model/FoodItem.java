package io.github.lishangbu.avalon.content.life.model;

/**
 * FoodCatalog 使用的类型化配置，声明item、health、mana等数据。 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param item 物品代码或物品数据
 * @param health 生命属性
 * @param mana 魔力属性
 */
public record FoodItem(String item, int health, int mana) {
}
