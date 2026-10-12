package io.github.lishangbu.avalon.character.application.model;

import java.util.List;

/**
 * FriendshipQueryService 使用的独立数据投影，包含entries、next等数据。 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param entries 当前列表数据
 * @param next 继续查询的游标
 */
public record FriendshipPage(List<FriendshipEntry> entries, String next) {
}
