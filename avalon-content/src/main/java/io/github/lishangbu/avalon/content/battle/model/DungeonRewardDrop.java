package io.github.lishangbu.avalon.content.battle.model;

/**
 * DungeonRewardCatalog 使用的类型化配置，声明code、weight等数据。 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param code 内容目录中的稳定代码
 * @param weight 抽取权重
 */
public record DungeonRewardDrop(String code, int weight) {
}
