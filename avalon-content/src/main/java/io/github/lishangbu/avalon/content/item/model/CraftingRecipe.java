package io.github.lishangbu.avalon.content.item.model;

/**
 * CraftingCatalog 使用的类型化配置，声明id、fragment、product、quantity等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param fragment 碎片引用
 * @param product 产物代码
 * @param quantity 物品或资源数量
 * @param gold 金币数量
 */
public record CraftingRecipe(String id, String fragment, String product, int quantity, int gold) {
}
