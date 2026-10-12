package io.github.lishangbu.avalon.battle.application.model;

import java.time.Instant;

/**
 * DungeonInvitationQueryService 使用的独立数据投影，包含id、lobby、sender、dungeon等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param lobby 队伍大厅资料
 * @param dungeon 副本声明
 * @param members 成员列表
 * @param capacity 容量
 * @param expiresAt 失效时刻
 */
public record DungeonInvitationInvitation(String id, String lobby, String sender, String dungeon, int members,
		int capacity, Instant expiresAt) {
}
