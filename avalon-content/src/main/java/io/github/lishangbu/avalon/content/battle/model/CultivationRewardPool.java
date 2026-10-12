package io.github.lishangbu.avalon.content.battle.model;

/**
 * CultivationRewardCatalog 使用的类型化配置，声明common、special、book等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param book 技能书引用
 */
public record CultivationRewardPool(String common, String special, String book) {
}
