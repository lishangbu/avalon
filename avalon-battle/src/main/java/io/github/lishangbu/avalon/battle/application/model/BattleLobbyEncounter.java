package io.github.lishangbu.avalon.battle.application.model;

/**
 * BattleLobby 使用的独立数据投影，包含id、name、description等数据。 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param name 展示名称
 * @param description 玩家可读说明
 */
public record BattleLobbyEncounter(String id, String name, String description) {
	public BattleLobbyEncounter(String id, String name) {
		this(id, name, null);
	}
}
