package io.github.lishangbu.avalon.content.item.model;

import java.util.Map;

/** 带公共名称编号的装备只读投影，数值直接来自所属物品的装备属性。 */
public record ItemEquipment(String id, String name, String slot, int level, Map<String, String> names,
		Map<String, Integer> stats, Map<String, Map<String, Integer>> professionStats, String quality,
		String profession) {
}
