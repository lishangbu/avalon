package io.github.lishangbu.avalon.battle.domain;

import io.github.lishangbu.avalon.battle.domain.BattleState.Unit;
import io.github.lishangbu.avalon.content.battle.BattleCatalog;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** 直接攻击的免疫和同队光环；在护盾、最低伤害及可消费减伤之前处理。 */
public final class DefensiveAbilityPolicy {

	private DefensiveAbilityPolicy() {
	}

	public static boolean absorb(Unit receiver, BattleCatalog.Skill skill) {
		String ability = receiver.effect();
		boolean water = "储水".equals(ability) && "水".equals(skill.element());
		boolean thunder = "蓄电".equals(ability) && "雷".equals(skill.element());
		boolean fire = "引火".equals(ability) && "火".equals(skill.element());
		boolean wood = "食草".equals(ability) && "木".equals(skill.element());
		boolean earth = "飘浮".equals(ability) && "地".equals(skill.element());
		if (water || thunder)
			receiver.setHp(Math.min(receiver.getStats().health(), receiver.getHp() + receiver.getStats().health() / 4));
		if (fire)
			receiver.setFireAbsorbed(true);
		if (wood)
			receiver.setWoodAbsorbed(true);
		return water || thunder || fire || wood || earth;
	}

	/** 同次群攻固定来源资格；所属小队以人物编号区分，绝不扩散到副本其他成员。 */
	public static Set<String> friends(BattleState state) {
		Set<String> guarded = new HashSet<>();
		for (var source : state.getUnits()) {
			if (source.isHero() || !source.alive() || !source.active() || !"友情防守".equals(source.effect()))
				continue;
			for (var target : state.getUnits()) {
				String owner = target.isHero() ? target.getId() : target.getOwnerHero();
				if (source != target && target.alive() && target.active() && source.isEnemy() == target.isEnemy()
						&& Objects.equals(source.getOwnerHero(), owner))
					guarded.add(target.getId());
			}
		}
		return guarded;
	}

	public static void leave(Unit unit) {
		unit.setEntered(false);
		unit.setCopiedAbility(null);
		unit.setThunderCharge(false);
		unit.getArmorBreaks().clear();
		unit.setDefenseCharge(false);
		unit.setActionCharge(false);
		unit.setFireAbsorbed(false);
		unit.setWoodAbsorbed(false);
		unit.setRelayGuardUntil(0);
	}

}
