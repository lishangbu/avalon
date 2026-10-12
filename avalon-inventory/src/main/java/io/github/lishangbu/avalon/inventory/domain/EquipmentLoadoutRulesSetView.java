package io.github.lishangbu.avalon.inventory.domain;

/**
 * EquipmentLoadoutRules 使用的类型化规则数据，包含id、name、count、total等字段。
 * 保持原有数据语义和构造规则；拆为顶层类型后，调用方无需依赖规则实现的内部声明。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param name 展示名称
 * @param count 业务计数
 * @param health 生命属性
 */
public record EquipmentLoadoutRulesSetView(String id, String name, int count, int total, int healthPieces, int health,
		int shieldPieces, int shield, boolean hasWeaponBonus, boolean weaponActive, int weaponHealth,
		int weaponShield) {
}
