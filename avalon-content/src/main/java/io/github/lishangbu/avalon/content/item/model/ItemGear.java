package io.github.lishangbu.avalon.content.item.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ItemCatalog 使用的类型化配置，声明slot、level、names、stats等数据。 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param slot 资产槽位
 * @param level 等级
 * @param stats 属性数据
 * @param quality 品质
 * @param profession 职业代码
 */
public record ItemGear(String slot, int level, Map<String, String> names, Map<String, Integer> stats,
		Map<String, Map<String, Integer>> professionStats, String quality, String profession) {
	public ItemGear {
		names = names == null ? Map.of() : Map.copyOf(names);
		stats = stats == null ? Map.of() : Map.copyOf(stats);
		var copy = new LinkedHashMap<String, Map<String, Integer>>();
		if (professionStats != null)
			professionStats.forEach((k, v) -> copy.put(k, Map.copyOf(v)));
		professionStats = Map.copyOf(copy);
	}
}
