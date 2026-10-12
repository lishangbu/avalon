package io.github.lishangbu.avalon.content.battle.model;

/**
 * effect 决定结算器，attack/defense 决定属性通道，target 决定目标阵营。
 */
public record BattleSkill(String id, String effect, String element, String attack, String defense, int power, int mana,
		int cooldown, int priority, String target, BattleLowHealthPower lowHealthPower, String description) {
	public BattleSkill(String id, String effect, String element, String attack, String defense, int power, int mana,
			int cooldown, int priority, String target, BattleLowHealthPower lowHealthPower) {
		this(id, effect, element, attack, defense, power, mana, cooldown, priority, target, lowHealthPower, null);
	}

	/** 普攻及首领脚本构造的固定威力攻击不携带低生命条件。 */
	public BattleSkill(String id, String effect, String element, String attack, String defense, int power, int mana,
			int cooldown, int priority, String target) {
		this(id, effect, element, attack, defense, power, mana, cooldown, priority, target, null);
	}
}
