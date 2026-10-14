package io.github.lishangbu.avalon.content.battle.model;

import java.util.List;

/**
 * 唯一技能格式：effects是有序效果列表，attack/defense决定属性通道，target决定合法阵营。
 * 技能费用与冷却只支付一次，组合效果各自执行；不接受旧effect或顶层power字段。
 */
public record BattleSkill(String id, List<BattleSkillEffect> effects, String element, String attack, String defense,
		int mana, int cooldown, int priority, String target, BattleLowHealthPower lowHealthPower, String description) {
	/** 防御复制效果列表，空技能拒绝；具体组合约束由宿主启动校验。 */
	public BattleSkill {
		effects = List.copyOf(effects);
		if (effects.isEmpty())
			throw new IllegalArgumentException("技能必须声明效果");
	}

	/** Java规则构造单项技能的便利入口，不参与JSON旧格式解析。 */
	public BattleSkill(String id, String kind, String element, String attack, String defense, int power, int mana,
			int cooldown, int priority, String target, BattleLowHealthPower condition, String description) {
		this(id, List.of(new BattleSkillEffect(kind, power)), element, attack, defense, mana, cooldown, priority,
				target, condition, description);
	}

	/** 第一效果决定现有AI分类；复合效果的执行仍严格使用完整effects列表。 */
	public String primaryKind() {
		return effects.getFirst().kind();
	}

	/** 返回首项效果数值，伤害预览与既有公式在迁移时保持同一基础数值。 */
	public int power() {
		return effects.getFirst().power();
	}

	/** 检查技能是否包含指定能力，新增组合无需增设另一份技能分类字段。 */
	public boolean hasEffect(String kind) {
		return effects.stream().anyMatch(effect -> effect.kind().equals(kind));
	}

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
