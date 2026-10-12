package io.github.lishangbu.avalon.battle.application.model;

import java.util.List;

/**
 * DungeonRecruitmentQueryService 使用的独立数据投影，包含offers、next等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param offers 可用供应项
 * @param next 继续查询的游标
 */
public record DungeonRecruitmentPage(List<DungeonLobbyViewRecruitment> offers, String next) {
}
