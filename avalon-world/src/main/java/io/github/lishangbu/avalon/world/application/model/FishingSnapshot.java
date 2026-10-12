package io.github.lishangbu.avalon.world.application.model;

import io.github.lishangbu.avalon.world.domain.FishingRulesState;

/**
 * FishingViews 使用的独立数据投影，包含id、fish、revision、state等数据。 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param fish 鱼类声明或代码
 * @param revision 状态修订，供后续命令并发检查
 * @param state 当前规则状态
 * @param broken 是否已经断线
 * @param fishPosition 当前鱼的位置
 * @param tolerance 容许偏差
 */
public record FishingSnapshot(String id, String fish, long revision, FishingRulesState state, boolean broken,
		double fishPosition, double tolerance) {
}
