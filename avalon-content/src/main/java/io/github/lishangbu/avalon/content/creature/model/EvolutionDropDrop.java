package io.github.lishangbu.avalon.content.creature.model;

/**
 * EvolutionDropCatalog 使用的类型化配置，声明sourceLevel、common、commonPercent、family等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param family 幻兽家族声明
 */
public record EvolutionDropDrop(int sourceLevel, String common, int commonPercent, String family, int familyPercent) {
}
