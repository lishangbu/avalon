package io.github.lishangbu.avalon.content.battle.model;

import java.util.List;

/**
 * DungeonChestCatalog 使用的类型化配置，声明dungeon、name、equipment、extraCounts等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param dungeon 副本声明
 * @param name 展示名称
 * @param equipment 装备声明或装备标志
 */
public record DungeonChestDefinition(String dungeon, String name, List<DungeonChestEquipment> equipment,
		List<DungeonChestCount> extraCounts, List<DungeonChestExtra> extras) {
	public DungeonChestDefinition {
		equipment = equipment == null ? List.of() : List.copyOf(equipment);
		extraCounts = extraCounts == null ? List.of() : List.copyOf(extraCounts);
		extras = extras == null ? List.of() : List.copyOf(extras);
	}
}
