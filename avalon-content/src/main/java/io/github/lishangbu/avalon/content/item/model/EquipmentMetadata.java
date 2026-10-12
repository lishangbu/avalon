package io.github.lishangbu.avalon.content.item.model;

/**
 * EquipmentCatalog 使用的类型化配置，声明quality、price、profession等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param quality 品质
 * @param price 价格
 * @param profession 职业代码
 */
public record EquipmentMetadata(String quality, int price, String profession) {
}
