package io.github.lishangbu.avalon.content.battle.model;

/**
 * WildCatalog 使用的类型化配置，声明weight、minLevel、maxLevel等数据。 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param weight 抽取权重
 * @param minLevel 等级下界
 * @param maxLevel 等级上界
 */
public record WildSpawn(int weight, int minLevel, int maxLevel) {
}
