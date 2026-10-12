package io.github.lishangbu.avalon.content.battle.model;

/**
 * DungeonChestCatalog 使用的类型化配置，声明code、amount、weight、bound等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param code 内容目录中的稳定代码
 * @param amount 本次规则使用的数量
 * @param weight 抽取权重
 * @param bound 资产是否绑定
 */
public record DungeonChestExtra(String code, int amount, int weight, boolean bound) {
}
