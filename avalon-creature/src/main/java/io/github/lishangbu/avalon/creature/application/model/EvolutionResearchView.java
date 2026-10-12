package io.github.lishangbu.avalon.creature.application.model;

import java.util.List;

/**
 * EvolutionResearchQueryService 使用的独立数据投影，包含available、taught、selected、routes等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param available 当前是否满足可用条件
 * @param taught 是否已经学会
 * @param selected 当前选择
 * @param routes 演化路线声明列表
 * @param tokens 令牌数量
 * @param offers 可用供应项
 */
public record EvolutionResearchView(boolean available, boolean taught, String selected,
		List<EvolutionResearchRoute> routes, long tokens, List<EvolutionResearchOffer> offers, int requiredLevel) {
}
