package io.github.lishangbu.avalon.content.creature.model;

/**
 * CollectionCatalog 使用的类型化配置，声明gold、item、quantity等数据。 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param gold 金币数量
 * @param item 物品代码或物品数据
 * @param quantity 物品或资源数量
 */
public record CollectionReward(int gold, String item, int quantity) {
}
