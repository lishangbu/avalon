package io.github.lishangbu.avalon.world.application.model;

import io.github.lishangbu.avalon.content.life.model.LifeProductionTool;
import io.github.lishangbu.avalon.inventory.application.model.InventoryItemView;
import java.util.List;
import java.util.Map;

/**
 * LifeProductionQueryService 使用的独立数据投影，包含recipes、inventory、tools、offers等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param recipes 配方声明列表
 * @param inventory 库存投影
 * @param tools 工具声明或工具数量
 * @param offers 可用供应项
 * @param sellPrices 出售价格声明
 * @param canSell 当前是否允许出售
 */
public record LifeProductionView(List<LifeProductionRecipeView> recipes, List<InventoryItemView> inventory,
		List<LifeProductionTool> tools, List<LifeProductionOfferView> offers, Map<String, Integer> sellPrices,
		boolean canSell) {
}
