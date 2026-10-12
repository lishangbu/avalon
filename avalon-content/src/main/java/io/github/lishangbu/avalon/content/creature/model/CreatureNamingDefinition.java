package io.github.lishangbu.avalon.content.creature.model;

/**
 * CreatureNamingCatalog 使用的类型化配置，声明firstPrice、repeatPrice、maxLength等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param firstPrice 首次费用
 * @param repeatPrice 重复费用
 */
public record CreatureNamingDefinition(int firstPrice, int repeatPrice, int maxLength) {
}
