package io.github.lishangbu.avalon.world.application.model;

import io.github.lishangbu.avalon.content.life.model.FarmingCrop;
import io.github.lishangbu.avalon.content.life.model.LifeProductionTool;
import io.github.lishangbu.avalon.inventory.application.model.InventoryItemView;
import java.util.List;

/**
 * FarmQueryService 使用的独立数据投影，包含atFarm、learned、level、maximumPlots等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param learned 本次学会的技能
 * @param level 等级
 * @param maximumPlots 田地数量上限
 * @param plots 田地声明列表
 * @param crops 作物声明列表
 * @param inventory 库存投影
 * @param tools 工具声明或工具数量
 */
public record FarmView(boolean atFarm, boolean learned, int level, int maximumPlots, int expansionGold,
		List<FarmPlot> plots, List<FarmingCrop> crops, List<InventoryItemView> inventory,
		List<LifeProductionTool> tools) {
}
