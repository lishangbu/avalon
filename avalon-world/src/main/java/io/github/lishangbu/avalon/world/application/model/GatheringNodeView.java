package io.github.lishangbu.avalon.world.application.model;

/**
 * GatheringQueryService 使用的独立数据投影，包含id、name、skill、skillName等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param name 展示名称
 * @param skill 技能代码
 * @param toolKind 工具种类
 * @param level 等级
 * @param product 产物代码
 * @param quantity 物品或资源数量
 * @param health 生命属性
 * @param mana 魔力属性
 * @param available 当前是否满足可用条件
 */
public record GatheringNodeView(String id, String name, String skill, String skillName, String toolKind, int level,
		String product, int quantity, int health, int mana, boolean available) {
}
