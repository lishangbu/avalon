package io.github.lishangbu.avalon.battle.domain;

/** 套装防御触发只查找本人当前出战伙伴；训练使用固定规则，不继承真实装备。 */
public final class EquipmentSetPolicy {

	private EquipmentSetPolicy() {
	}

	public static void defend(BattleState state, BattleState.Unit hero) {
		if (!hero.isHero() || hero.getDefendShield() <= 0 || state.getTraining() != null)
			return;
		state.getUnits()
			.stream()
			.filter(unit -> !unit.isHero() && !unit.isEnemy() && unit.alive() && unit.active()
					&& hero.getId().equals(unit.getOwnerHero()))
			.min((left, right) -> {
				// 交叉相乘避免生命比例的浮点误差；相同比例按出战位置选择。
				int ratio = Long.compare((long) left.getHp() * right.getStats().health(),
						(long) right.getHp() * left.getStats().health());
				return ratio != 0 ? ratio : Integer.compare(left.getPosition(), right.getPosition());
			})
			.ifPresent(pet -> {
				pet.setSetShield(Math.max(pet.getSetShield(), hero.getDefendShield()));
				state.getLog().add(pet.getName() + "获得套装护盾，可吸收 " + pet.getSetShield() + "伤害。");
			});
	}

}
