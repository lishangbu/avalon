package io.github.lishangbu.avalon.content.battle.model;

/** 技能的一项有序效果。kind引用宿主注册器，power为该效果自己的数值，不从其他效果隐式继承。 */
public record BattleSkillEffect(String kind, int power) {
	/** 目录只核验通用形状；目标、具体数值边界和引用由同一个注册处理器启动校验。 */
	public BattleSkillEffect {
		if (kind == null || !kind.matches("[A-Z][A-Z0-9_]*") || power < 0)
			throw new IllegalArgumentException("技能效果类型或数值无效");
	}
}
