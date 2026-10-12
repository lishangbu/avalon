package io.github.lishangbu.avalon.content.life.model;

/**
 * LifeProductionCatalog 使用的类型化配置，声明item、kind、tier、breakDenominator等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param item 物品代码或物品数据
 * @param kind 业务种类
 * @param tier 等级档位
 */
public record LifeProductionTool(String item, String kind, int tier, int breakDenominator) {
}
