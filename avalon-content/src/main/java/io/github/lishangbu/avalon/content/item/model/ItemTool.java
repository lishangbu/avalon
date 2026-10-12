package io.github.lishangbu.avalon.content.item.model;

/**
 * ItemCatalog 使用的类型化配置，声明kind、tier、breakDenominator等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param kind 业务种类
 * @param tier 等级档位
 */
public record ItemTool(String kind, int tier, int breakDenominator) {
}
