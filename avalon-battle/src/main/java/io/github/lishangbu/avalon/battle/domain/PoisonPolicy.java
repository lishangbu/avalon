package io.github.lishangbu.avalon.battle.domain;

import io.github.lishangbu.avalon.battle.domain.BattleState.Unit;
import io.github.lishangbu.avalon.content.battle.BattleCatalog;

/** 中毒保存来源快照，未来三个回合末结算；同步只反馈第一次新增事件。 */
public final class PoisonPolicy {

	private PoisonPolicy() {
	}

	public static void apply(BattleState state, Unit source, Unit target) {
		int strength = Math.multiplyExact(source.getLevel() + 5, source.getStats().magicAttack());
		boolean fresh = target.getPoisonUntil() < state.getRound();
		if (!target.alive() || target.getPeriodicResistance() >= 3 || (!fresh && strength < target.getPoisonStrength()))
			return;
		set(state, target, strength);
		if (fresh && "同步".equals(target.effect()) && source.alive() && source.active()
				&& source.isEnemy() != target.isEnemy() && source.getPeriodicResistance() < 3
				&& (source.getPoisonUntil() < state.getRound() || strength >= source.getPoisonStrength()))
			set(state, source, strength);
	}

	private static void set(BattleState state, Unit target, int strength) {
		target.setPoisonStrength(strength);
		target.setPoisonStarted(state.getRound());
		target.setPoisonUntil(state.getRound() + 3);
		state.getLog().add(target.getName() + "中毒，将在随后三个回合末结算。");
	}

	public static int damage(Unit target, BattleCatalog catalog) {
		double resistance = Math.max(0, 1 - target.getPeriodicResistance() * .25);
		if (target.getPeriodicResistance() >= 3)
			return 0;
		return (int) Math.floor(target.getPoisonStrength() * 2.0 / Math.max(1, target.getStats().magicDefense())
				* catalog.element("木", target.getElement()) * resistance);
	}

	public static void finish(BattleState state, BattleCatalog catalog) {
		if (!state.getStatus().equals("ACTIVE"))
			return;
		for (var unit : state.getUnits()) {
			if (!unit.alive() || !unit.active() || unit.getPoisonUntil() < state.getRound()
					|| unit.getPoisonStarted() >= state.getRound())
				continue;
			if ("毒疗".equals(unit.effect()) && unit.getPeriodicResistance() < 3) {
				int amount = (int) Math
					.floor(unit.getStats().health() / 16.0 * (1 - unit.getPeriodicResistance() * .25));
				unit.setHp(Math.min(unit.getStats().health(), unit.getHp() + amount));
				unit.setObservedPoisonHealing(true);
				state.getLog().add(unit.getName() + "将中毒转为恢复。");
			}
			else {
				int amount = damage(unit, catalog);
				unit.setHp(Math.max(0, unit.getHp() - amount));
				if (amount > 0)
					unit.setObservedPoisonHealing(false);
				state.getLog().add(unit.getName() + "受到" + amount + "点中毒伤害。");
				if (!unit.alive()) {
					unit.setPoisonUntil(0);
					DefensiveAbilityPolicy.leave(unit);
				}
			}
		}
		boolean friends = state.getUnits().stream().anyMatch(unit -> !unit.isEnemy() && unit.alive());
		boolean enemies = state.getUnits()
			.stream()
			.anyMatch(unit -> unit.isEnemy() && !unit.isMechanism() && unit.alive());
		if (!friends)
			state.setStatus("LOST");
		else if (!enemies)
			state.setStatus("WON");
	}

}
