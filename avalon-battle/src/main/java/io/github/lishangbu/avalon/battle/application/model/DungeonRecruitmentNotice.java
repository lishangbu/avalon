package io.github.lishangbu.avalon.battle.application.model;

import java.time.Instant;

/**
 * DungeonRecruitmentQueryService 使用的独立数据投影，包含id、confirmed、deadline、inBattle等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param confirmed 是否已确认
 * @param deadline 截止时刻
 * @param members 成员列表
 */
public record DungeonRecruitmentNotice(String id, boolean confirmed, Instant deadline, boolean inBattle, int members) {
}
