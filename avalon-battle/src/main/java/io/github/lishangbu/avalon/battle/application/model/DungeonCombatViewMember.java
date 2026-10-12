package io.github.lishangbu.avalon.battle.application.model;

/**
 * DungeonCombatView 使用的独立数据投影，包含id、submitted、removed、leaving等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param removed 被移除的技能或项目
 */
public record DungeonCombatViewMember(String id, boolean submitted, boolean removed, boolean leaving,
		int missedRounds) {
}
