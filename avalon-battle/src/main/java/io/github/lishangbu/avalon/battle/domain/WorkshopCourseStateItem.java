package io.github.lishangbu.avalon.battle.domain;

/** 训练库存仍按正式背包的装备逐件、同物同绑定堆叠规则计算。 */
public record WorkshopCourseStateItem(String id, String name, String code, int quantity, int pending, boolean equipment,
		boolean equipped, int requiredLevel, int health, int mana) {
	public WorkshopCourseStateItem equipped(boolean value) {
		return new WorkshopCourseStateItem(id, name, code, quantity, pending, equipment, value, requiredLevel, health,
				mana);
	}

	public WorkshopCourseStateItem stock(int available, int waiting) {
		return new WorkshopCourseStateItem(id, name, code, available, waiting, equipment, equipped, requiredLevel,
				health, mana);
	}
}
