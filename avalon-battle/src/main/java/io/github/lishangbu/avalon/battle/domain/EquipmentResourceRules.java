package io.github.lishangbu.avalon.battle.domain;

/** 正式换装与隔离练习共同使用的资源截断；增加上限不治疗，倒下也不会被换装复活。 */
public final class EquipmentResourceRules {

	private EquipmentResourceRules() {
	}

	public static int clamp(int current, int maximum) {
		if (current < 0 || maximum < 0)
			throw new IllegalArgumentException("资源及上限不能为负");
		return Math.min(current, maximum);
	}

}
