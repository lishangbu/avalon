package io.github.lishangbu.avalon.content.item.model;

/**
 * ItemCatalog 使用的类型化配置，声明health、mana等数据。 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param health 生命属性
 * @param mana 魔力属性
 */
public record ItemRecovery(int health, int mana) {
}
