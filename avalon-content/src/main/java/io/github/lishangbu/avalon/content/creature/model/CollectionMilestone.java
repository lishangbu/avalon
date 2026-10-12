package io.github.lishangbu.avalon.content.creature.model;

/**
 * CollectionCatalog 使用的类型化配置，声明count、reward等数据。 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param count 业务计数
 * @param reward 奖励声明
 */
public record CollectionMilestone(int count, CollectionReward reward) {
}
