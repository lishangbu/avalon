package io.github.lishangbu.avalon.world.application.model;

import io.github.lishangbu.avalon.content.world.model.SceneActionChoice;
import io.github.lishangbu.avalon.content.world.model.SceneActionType;
import java.util.List;

/**
 * SceneActionQueryService 使用的独立数据投影，包含id、type、label、description等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param type 业务分类
 * @param label 操作展示文字
 * @param description 玩家可读说明
 * @param choices 可选分支声明
 * @param completed 是否完成
 * @param npcId 稳定人物编号
 */
public record SceneActionView(String id, SceneActionType type, String label, String description,
		List<SceneActionChoice> choices, boolean completed, String npcId) {
}
