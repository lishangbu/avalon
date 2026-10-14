package io.github.lishangbu.avalon.battle.domain;

import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import io.github.lishangbu.avalon.content.battle.model.BattleSkill;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 普通野生幻兽的条件选招。输入只有上一回合保存的战局和已随机选定的敌对目标，不能读取玩家本轮指令。
 * 先自疗、再保护低血友方、减速、低血增威力攻击，最后基础攻击；魔力和冷却不足的技能提前排除。
 */
@Component
@RequiredArgsConstructor
public class WildActionPolicy {

	private final BattleCatalog catalog;

	public BattleStateOrder choose(BattleState state, BattleStateUnit actor, BattleStateUnit target) {
		List<BattleSkill> skills = actor.getSkills()
			.stream()
			.map(catalog::skill)
			.filter(skill -> actor.getMp() >= BattleEngine.abilityCost(actor, skill)
					&& actor.getReady().getOrDefault(skill.id(), 0) <= state.getRound())
			.toList();
		if (actor.getHp() * 2 <= actor.getStats().health())
			for (var skill : skills)
				if (skill.primaryKind().equals("HEAL"))
					return new BattleStateOrder(actor.getId(), skill.id(), actor.getId());

		// 只保护另一名存活出战友方，按血量比例选最低者；相同比例以稳定编号确定次序。
		var wounded = state.getUnits()
			.stream()
			.filter(unit -> unit.isEnemy() == actor.isEnemy() && !unit.getId().equals(actor.getId()) && unit.active()
					&& unit.alive() && (long) unit.getHp() * 100 <= (long) unit.getStats().health() * 35)
			.min(Comparator.comparingDouble((BattleStateUnit unit) -> (double) unit.getHp() / unit.getStats().health())
				.thenComparing(BattleStateUnit::getId));
		if (wounded.isPresent())
			for (var skill : skills)
				if (skill.primaryKind().equals("COVER"))
					return new BattleStateOrder(actor.getId(), skill.id(), wounded.get().getId());

		skills = skills.stream()
			.filter(skill -> !target.getObservedImmunities().contains(skill.element())
					|| !Set.of("DAMAGE", "SLOW", "POISON", "AREA").contains(skill.primaryKind()))
			.toList();
		for (var skill : skills)
			if ("POISON".equals(skill.primaryKind()) && ("强行".equals(actor.effect())
					|| !target.isObservedPoisonHealing() && (target.getPoisonUntil() < state.getRound()
							|| target.getPoisonStrength() < (actor.getLevel() + 5) * actor.getStats().magicAttack())))
				return new BattleStateOrder(actor.getId(), skill.id(), target.getId());
		if (target.getSlowUntil() < state.getRound())
			for (var skill : skills)
				if (skill.primaryKind().equals("SLOW"))
					return new BattleStateOrder(actor.getId(), skill.id(), target.getId());

		for (var skill : skills) {
			var condition = skill.lowHealthPower();
			if (condition != null
					&& (long) target.getHp() * 100 <= (long) target.getStats().health() * condition.percent())
				return new BattleStateOrder(actor.getId(), skill.id(), target.getId());
		}
		for (var skill : skills)
			if (skill.primaryKind().equals("DAMAGE") && skill.lowHealthPower() == null)
				return new BattleStateOrder(actor.getId(), skill.id(), target.getId());
		return new BattleStateOrder(actor.getId(), "ATTACK", target.getId());
	}

}
