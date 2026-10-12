package io.github.lishangbu.avalon.content.item.model;

/**
 * EnhancementCatalog 使用的类型化配置，声明minimum、maximum、gold等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param minimum 范围下界
 * @param maximum 范围上界
 * @param gold 金币数量
 */
public record EnhancementFee(int minimum, int maximum, long gold) {
}
