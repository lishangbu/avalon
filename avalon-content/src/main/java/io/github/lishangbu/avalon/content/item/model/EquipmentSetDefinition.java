package io.github.lishangbu.avalon.content.item.model;

import java.util.List;

/**
 * EquipmentSetCatalog 使用的类型化配置，声明id、name、pieces、healthPieces等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param name 展示名称
 * @param health 生命属性
 */
public record EquipmentSetDefinition(String id, String name, List<String> pieces, int healthPieces, int health,
		int shieldPieces, int shieldPerLevel, int shieldLevelCap, List<String> weapons, int weaponHealth,
		int weaponShield) {
	public EquipmentSetDefinition {
		pieces = List.copyOf(pieces);
		weapons = weapons == null ? List.of() : List.copyOf(weapons);
	}
}
