package io.github.lishangbu.avalon.world.application.model;

/**
 * SceneActionQueryService 使用的独立数据投影，包含id、label、description、npcId等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param label 操作展示文字
 * @param description 玩家可读说明
 * @param npcId 稳定人物编号
 */
public record SceneActionInformationView(String id, String label, String description, String npcId) {
}
