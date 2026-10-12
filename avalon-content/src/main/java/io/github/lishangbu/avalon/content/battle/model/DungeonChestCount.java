package io.github.lishangbu.avalon.content.battle.model;

/**
 * DungeonChestCatalog 使用的类型化配置，声明amount、weight等数据。 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param amount 本次规则使用的数量
 * @param weight 抽取权重
 */
public record DungeonChestCount(int amount, int weight) {
}
