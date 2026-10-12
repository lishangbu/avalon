package io.github.lishangbu.avalon.content.item.model;

/**
 * ItemCatalog 使用的类型化配置，声明level、multiplier、reinforced等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param level 等级
 * @param multiplier 数值倍率
 */
public record ItemCapture(int level, double multiplier, boolean reinforced) {
}
