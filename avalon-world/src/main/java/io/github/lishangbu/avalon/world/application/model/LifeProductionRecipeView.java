package io.github.lishangbu.avalon.world.application.model;

import java.util.List;

/**
 * LifeProductionQueryService 使用的独立数据投影，包含id、skill、level、minimumToolTier等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param skill 技能代码
 * @param level 等级
 * @param minimumToolTier 工具最低档位
 * @param product 产物代码
 * @param name 展示名称
 * @param quantity 物品或资源数量
 * @param points 规则使用的点数
 * @param skillLevel 技能等级
 * @param proficiencyGain 增加的熟练度
 * @param ingredients 原料声明
 * @param locations 允许的地点代码
 * @param available 当前是否满足可用条件
 */
public record LifeProductionRecipeView(String id, String skill, int level, int minimumToolTier, String product,
		String name, int quantity, int points, int skillLevel, Double proficiencyGain,
		List<LifeProductionIngredient> ingredients, List<String> locations, boolean available) {
}
