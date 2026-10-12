package io.github.lishangbu.avalon.battle.application.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DungeonLobbyView 使用的独立数据投影，包含id、name、profession、level等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param name 展示名称
 * @param profession 职业代码
 * @param level 等级
 * @param self 本人角色资料
 * @param confirmed 是否已确认
 * @param ready 是否已准备好
 * @param deadline 截止时刻
 * @param party 队伍资料
 * @param supplies 补给声明
 */
public record DungeonLobbyViewMember(String id, String name, String profession, int level, boolean self,
		boolean confirmed, boolean ready, boolean inBattle, Instant deadline, List<DungeonLobbyViewUnit> party,
		Map<String, Integer> supplies) {
}
