package io.github.lishangbu.avalon.world.application.model;

import io.github.lishangbu.avalon.content.life.model.FishingFish;
import io.github.lishangbu.avalon.content.life.model.LifeProductionTool;
import io.github.lishangbu.avalon.inventory.application.model.InventoryItemView;
import java.util.List;

/**
 * FishingQueryService 使用的独立数据投影，包含session、level、fish、inventory等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param session 当前会话引用
 * @param level 等级
 * @param fish 鱼类声明或代码
 * @param inventory 库存投影
 * @param tools 工具声明或工具数量
 * @param bait 鱼饵声明
 */
public record FishingView(FishingSnapshot session, int level, List<FishingFish> fish, List<InventoryItemView> inventory,
		List<LifeProductionTool> tools, String bait) {
}
