package io.github.lishangbu.avalon.content.creature.model;

/**
 * SpeciesStatsCatalog 使用的类型化配置，声明health、mana、physicalAttack、magicAttack等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param health 生命属性
 * @param mana 魔力属性
 * @param physicalAttack 物理攻击属性
 * @param magicAttack 魔法攻击属性
 * @param physicalDefense 物理防御属性
 * @param magicDefense 魔法防御属性
 * @param speed 速度属性
 */
public record SpeciesStatsStats(int health, int mana, int physicalAttack, int magicAttack, int physicalDefense,
		int magicDefense, int speed) {
}
