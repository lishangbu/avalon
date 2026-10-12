package io.github.lishangbu.avalon.world.application.model;

import io.github.lishangbu.avalon.content.life.model.LifeProductionTool;
import io.github.lishangbu.avalon.creature.application.model.CreatureView;
import io.github.lishangbu.avalon.inventory.application.model.InventoryItemView;
import java.util.List;

/**
 * GatheringQueryService 使用的独立数据投影，包含session、nodes、creatures、inventory等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param session 当前会话引用
 * @param creatures 幻兽投影列表
 * @param inventory 库存投影
 * @param tools 工具声明或工具数量
 * @param supplies 补给声明
 */
public record GatheringView(GatheringSession session, List<GatheringNodeView> nodes, List<CreatureView> creatures,
		List<InventoryItemView> inventory, List<LifeProductionTool> tools, List<InventoryItemView> supplies) {
}
