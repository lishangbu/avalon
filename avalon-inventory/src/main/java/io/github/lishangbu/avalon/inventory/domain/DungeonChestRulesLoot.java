package io.github.lishangbu.avalon.inventory.domain;

import java.util.List;

/**
 * DungeonChestRules 使用的类型化规则数据，包含equipment、sellPrice、gold、extras等字段。
 * 保持原有数据语义和构造规则；拆为顶层类型后，调用方无需依赖规则实现的内部声明。
 *
 * @param equipment 装备声明或装备标志
 * @param sellPrice 出售价格
 * @param gold 金币数量
 */
public record DungeonChestRulesLoot(String equipment, int sellPrice, int gold, List<DungeonChestRulesExtra> extras) {
	public DungeonChestRulesLoot {
		extras = List.copyOf(extras);
	}
}
