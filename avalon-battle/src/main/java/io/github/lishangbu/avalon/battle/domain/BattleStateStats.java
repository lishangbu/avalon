package io.github.lishangbu.avalon.battle.domain;

/**
 * BattleState 使用的类型化规则数据，包含health、mana、physicalAttack、magicAttack等字段。
 * 保持原有数据语义和构造规则；拆为顶层类型后，调用方无需依赖规则实现的内部声明。
 *
 * @param health 生命属性
 * @param mana 魔力属性
 * @param physicalAttack 物理攻击属性
 * @param magicAttack 魔法攻击属性
 * @param physicalDefense 物理防御属性
 * @param magicDefense 魔法防御属性
 * @param speed 速度属性
 */
public record BattleStateStats(int health, int mana, int physicalAttack, int magicAttack, int physicalDefense,
		int magicDefense, int speed) {
}
