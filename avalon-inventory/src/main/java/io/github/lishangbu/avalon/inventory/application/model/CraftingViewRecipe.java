package io.github.lishangbu.avalon.inventory.application.model;

import java.util.List;

/**
 * CraftingView 使用的独立数据投影，包含id、product、quantity、gold等数据。 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param product 产物代码
 * @param quantity 物品或资源数量
 * @param gold 金币数量
 * @param fragments 碎片材料声明
 */
public record CraftingViewRecipe(String id, String product, int quantity, int gold,
		List<CraftingViewFragment> fragments) {
}
