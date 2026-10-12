package io.github.lishangbu.avalon.inventory.domain;

import java.util.List;
import java.util.Map;

/**
 * EquipmentLoadoutRules 使用的类型化规则数据，包含stats、defendShield、sets等字段。
 * 保持原有数据语义和构造规则；拆为顶层类型后，调用方无需依赖规则实现的内部声明。
 *
 * @param stats 属性数据
 */
public record EquipmentLoadoutRulesLoadout(Map<String, Integer> stats, int defendShield,
		List<EquipmentLoadoutRulesSetView> sets) {
	public EquipmentLoadoutRulesLoadout {
		stats = Map.copyOf(stats);
		sets = List.copyOf(sets);
	}
}
