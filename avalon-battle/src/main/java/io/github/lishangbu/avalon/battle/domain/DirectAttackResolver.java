package io.github.lishangbu.avalon.battle.domain;

import io.github.lishangbu.avalon.combat.model.CombatStatus;
import io.github.lishangbu.avalon.combat.model.CombatStatusStacking;

import io.github.lishangbu.avalon.content.battle.model.BattleSkill;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 一次完整直接攻击的结算边界。群攻共享传入随机倍率，援护不连锁，所有分量计算完才扣生命。 因此保护者在同次群攻中倒下也不会把已经接下的伤害退回原目标。
 */
@Component
@RequiredArgsConstructor
public class DirectAttackResolver {

	private final BattleRules rules;

	private final TrainingPolicy training;

	public void apply(BattleState state, BattleStateUnit attacker, List<BattleStateUnit> targets, BattleSkill skill,
			boolean area, double random, Set<String> defending,
			Map<String, List<DirectAttackResolverProtection>> protections) {
		Map<String, Integer> losses = new HashMap<>();
		List<BattleStateUnit> hit = new ArrayList<>();
		var thorns = new LinkedHashMap<String, BattleStateUnit>();
		var guarded = DefensiveAbilityPolicy.friends(state);
		// 完整攻击期间不修改生命，以保证自身受击和代接部分使用同一攻击前状态。
		for (var original : targets) {
			if (!original.alive() || !original.active())
				continue;
			var receiver = original;
			boolean covered = false;
			var candidates = protections.getOrDefault(original.getId(), List.of());
			for (var protection : new ArrayList<>(candidates)) {
				if (area && !protection.acceptsArea())
					continue;
				var protector = state.getUnits()
					.stream()
					.filter(unit -> unit.getId().equals(protection.protector()))
					.findFirst()
					.orElse(null);
				if (protector == null || !protector.alive() || !protector.active())
					continue;
				candidates.remove(protection);
				receiver = protector;
				covered = true;
				break;
			}
			boolean lightningRedirect = false;
			if (!covered && !area && "雷".equals(skill.element())) {
				String owner = original.isHero() ? original.getId() : original.getOwnerHero();
				var lightning = state.getUnits()
					.stream()
					.filter(unit -> unit != original && unit.alive() && unit.active() && !unit.isHero()
							&& unit.isEnemy() == original.isEnemy() && Objects.equals(unit.getOwnerHero(), owner)
							&& "引雷".equals(unit.effect()) && unit.getLightningRedirectRound() != state.getRound())
					.sorted(Comparator.comparingInt(BattleStateUnit::getPosition).thenComparing(BattleStateUnit::getId))
					.findFirst();
				if (lightning.isPresent()) {
					receiver = lightning.get();
					receiver.setLightningRedirectRound(state.getRound());
					covered = true;
					lightningRedirect = true;
				}
			}
			if (DefensiveAbilityPolicy.absorb(receiver, skill)) {
				if (!receiver.getObservedImmunities().contains(skill.element()))
					receiver.getObservedImmunities().add(skill.element());
				state.getLog().add(receiver.getName() + "的" + receiver.effect() + "使" + skill.id() + "无效。");
				continue;
			}
			// 复制等临时免疫消失后，以已观察到的正常命中更新野生决策记忆。
			receiver.getObservedImmunities().remove(skill.element());
			int damage = rules.damage(attacker, receiver, skill, defending.contains(receiver.getId()), covered,
					random * BattleEnvironment.damageMultiplier(state, skill) * (lightningRedirect ? .75 : 1),
					state.getRound(), guarded.contains(receiver.getId()));
			receiver.setRelayGuardUntil(0);
			String attackOwner = attacker.isHero() || attacker.getOwnerHero() == null ? attacker.getId()
					: attacker.getOwnerHero();
			if (!area && "PA".equals(skill.attack()))
				receiver.getArmorBreaks().remove(attackOwner);
			// 先用实际承伤者的套装护盾，余量才交给魔力护盾；援护不会携带原目标护盾。
			int ward = Math.min(damage, receiver.getSetShield());
			receiver.setSetShield(receiver.getSetShield() - ward);
			damage -= ward;
			int absorbed = Math.min(damage, Math.min(receiver.getShield(), receiver.getMp() * 5));
			receiver.setShield(receiver.getShield() - absorbed);
			receiver.setMp(receiver.getMp() - (absorbed + 4) / 5);
			if (receiver.getMp() == 0)
				receiver.setShield(0);
			int loss = damage - absorbed;
			int hpBefore = receiver.getHp() - losses.getOrDefault(receiver.getId(), 0);
			if ("结实".equals(receiver.effect()) && hpBefore == receiver.getStats().health() && loss >= hpBefore)
				loss = Math.max(0, hpBefore - 1);
			losses.merge(receiver.getId(), loss, Integer::sum);
			if (loss > 0 && hpBefore > 0 && !area && "PA".equals(skill.attack()) && "破甲".equals(attacker.effect()))
				receiver.getArmorBreaks().put(attackOwner, state.getRound() + 1);
			if (loss > 0 && hpBefore > loss && "雷".equals(skill.element()) && "雷能充蓄".equals(receiver.effect())
					&& receiver.getFreeThunderCasts() < 2)
				receiver.setThunderCharge(true);
			hit.add(receiver);
			if ("铁刺".equals(receiver.effect()) && Set.of("普攻", "藤鞭", "雷牙", "烈焰爪").contains(skill.id()))
				thorns.put(receiver.getId(), receiver);
			state.getLog()
				.add(attacker.getName() + "使用" + skill.id() + "，" + (covered ? "由" : "命中") + receiver.getName() + "承受 "
						+ loss + "伤害" + (absorbed + ward > 0 ? "，护盾吸收 " + (absorbed + ward) : "") + "。");
			if (skill.primaryKind().equals("SLOW") && !receiver.isMechanism() && !"强行".equals(attacker.effect()))
				receiver.installStatus(new CombatStatus("SLOW", attacker.getId(), state.getRound(),
						state.getRound() + 2, 20, Map.of(), Map.of()), CombatStatusStacking.REPLACE, 20);
		}
		for (var unit : state.getUnits()) {
			unit.setHp(Math.max(0, unit.getHp() - losses.getOrDefault(unit.getId(), 0)));
			if (!unit.alive()) {
				unit.setSlowUntil(0);
				unit.setPoisonUntil(0);
				DefensiveAbilityPolicy.leave(unit);
				unit.setSetShield(0);
			}
		}
		if ("POISON".equals(skill.primaryKind()) && !"强行".equals(attacker.effect()))
			for (var target : hit)
				if (target.alive() && !target.isMechanism())
					PoisonPolicy.apply(state, attacker, target);
		for (var thorn : thorns.values()) {
			int reflected = Math.min(thorn.getStats().health() / 16, thorn.getLevel() * 3);
			int absorbed = Math.min(reflected, attacker.getSetShield());
			attacker.setSetShield(attacker.getSetShield() - absorbed);
			attacker.setHp(Math.max(0, attacker.getHp() - reflected + absorbed));
			state.getLog().add(thorn.getName() + "的铁刺反射" + (reflected - absorbed) + "伤害。");
		}
		if (!attacker.alive()) {
			attacker.setPoisonUntil(0);
			DefensiveAbilityPolicy.leave(attacker);
		}
		training.attack(state, attacker, hit, skill.primaryKind());
	}

}
