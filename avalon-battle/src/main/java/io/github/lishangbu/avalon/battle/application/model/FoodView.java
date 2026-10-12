package io.github.lishangbu.avalon.battle.application.model;

import io.github.lishangbu.avalon.content.life.model.FoodItem;
import io.github.lishangbu.avalon.inventory.application.model.InventoryItemView;
import java.util.List;

/**
 * FoodQueryService 使用的独立数据投影，包含inventory、foods、targets等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param inventory 库存投影
 * @param foods 食品声明列表
 * @param targets 目标编号列表
 */
public record FoodView(List<InventoryItemView> inventory, List<FoodItem> foods, List<FoodTarget> targets) {
}
