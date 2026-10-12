package io.github.lishangbu.avalon.creature.application.model;

import java.util.List;

/**
 * EvolutionResearchQueryService 使用的独立数据投影，包含source、target、name、level等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param target 目标编号或目标声明
 * @param name 展示名称
 * @param level 等级
 * @param materials 材料需求或材料投影
 */
public record EvolutionResearchRoute(String source, String target, String name, int level,
		List<EvolutionResearchMaterial> materials) {
}
