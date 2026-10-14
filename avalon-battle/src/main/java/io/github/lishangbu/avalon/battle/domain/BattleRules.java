package io.github.lishangbu.avalon.battle.domain;

import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import io.github.lishangbu.avalon.content.battle.CaptureBallCatalog;
import io.github.lishangbu.avalon.content.battle.model.BattleSkill;
import io.github.lishangbu.avalon.content.battle.model.CaptureBall;
import io.github.lishangbu.avalon.content.creature.SpeciesStatsCatalog;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 无数据库依赖的成长、直接伤害和捕捉公式，供预览及实际结算共用。
 */
@Component
@RequiredArgsConstructor
public class BattleRules {

	private final BattleCatalog catalog;

	private final CaptureBallCatalog balls;

	private final SpeciesStatsCatalog species;

	public BattleStateStats stats(String kind, int level, boolean hero, Map<String, Integer> equipment) {
		// 玩家幻兽和野生个体读取同一形态基础值；进化无需读取原形态或叠加倍率。
		if (!hero) {
			var s = species.atLevel(kind, level);
			return new BattleStateStats(s.health(), s.mana(), s.physicalAttack(), s.magicAttack(), s.physicalDefense(),
					s.magicDefense(), s.speed());
		}
		// 人物职业成长继续按职业目录和真实装备计算，不能套用幻兽公式。
		var g = catalog.growth(kind);
		int[] base = { 30 * (level + 5), 20 + 5 * level, 20 + 5 * level, 20 + 5 * level, 20 + 5 * level,
				20 + 2 * level };
		String[] keys = { "health", "physicalAttack", "magicAttack", "physicalDefense", "magicDefense", "speed" };
		// 职业成长已经包含初始装备预算；先扣预算再加真实穿戴，防止装备计算两次。
		int[] budget = { 40, Set.of("WARRIOR", "RANGER").contains(kind) ? 8 : 0,
				Set.of("MAGE", "PRIEST").contains(kind) ? 8 : 0, 6, 4, 2 };
		for (int i = 0; i < base.length; i++)
			base[i] = Math.max(1, (int) Math.floor(base[i] * g.coefficients().get(i)) - budget[i]
					+ equipment.getOrDefault(keys[i], 0));
		return new BattleStateStats(base[0],
				g.manaBase() + level * g.manaPerLevel() + equipment.getOrDefault("mana", 0), base[1], base[2], base[3],
				base[4], base[5]);
	}

	/**
	 * 只在最后一次向下取整；护盾由调用者在取整后吸收，最低一点不穿透护盾。
	 */
	public int damage(BattleStateUnit actor, BattleStateUnit target, BattleSkill skill, boolean defending,
			boolean covered, double random, int round) {
		return damage(actor, target, skill, defending, covered, random, round, false);
	}

	public int damage(BattleStateUnit actor, BattleStateUnit target, BattleSkill skill, boolean defending,
			boolean covered, double random, int round, boolean friendGuard) {
		// 灵爪只改变通用普攻的进攻数值来源，不改变物理分类、物防、属性或共享技能模板。
		boolean spiritClaw = "灵爪".equals(actor.effect()) && "普攻".equals(skill.id());
		double attack = ("MA".equals(skill.attack()) || spiritClaw) ? actor.getStats().magicAttack()
				: actor.getStats().physicalAttack();
		double defense = "MD".equals(skill.defense()) ? target.getStats().magicDefense()
				: target.getStats().physicalDefense();
		if (actor.isWoodAbsorbed() && "PA".equals(skill.attack()) && !spiritClaw)
			attack *= 1.15;
		String owner = actor.isHero() ? actor.getId() : actor.getOwnerHero();
		if ("PA".equals(skill.attack()) && !"AREA".equals(skill.primaryKind())
				&& target.getArmorBreaks().getOrDefault(owner, 0) >= round && "PD".equals(skill.defense()))
			defense *= .9;
		int power = skill.power();
		var condition = skill.lowHealthPower();
		if (condition != null && (long) target.getHp() * 100 <= (long) target.getStats().health() * condition.percent())
			power = condition.power();
		double value = (actor.getLevel() + 5) * (power / 10.0) * attack / Math.max(1, defense);
		if (!actor.isHero() && !"无".equals(skill.element()) && Objects.equals(actor.getElement(), skill.element()))
			value *= "适应力".equals(actor.effect()) ? 1.35 : 1.2;
		value *= catalog.element(skill.element(), target.getElement());
		if ("追猎".equals(actor.effect()) && "PA".equals(skill.attack())
				&& (long) target.getHp() * 2 <= target.getStats().health())
			value *= 1.1;
		if (actor.isActionCharge() && (("蓄势".equals(actor.effect()) && "PA".equals(skill.attack()))
				|| ("凝神".equals(actor.effect()) && "MA".equals(skill.attack()))))
			value *= 1.1;
		if ("强行".equals(actor.effect()) && Set.of("SLOW", "POISON").contains(skill.primaryKind()))
			value *= 1.3;
		if (friendGuard)
			value *= .9;
		if (actor.isFireAbsorbed() && "火".equals(skill.element()))
			value *= 1.2;
		if (target.getRelayGuardUntil() >= round)
			value *= .75;
		if (defending)
			value *= .7;
		if (target.getWallUntil() >= round)
			value *= 1 - target.getWallPercent() / 100.0;
		var mark = target.getStatuses().get("MARK:" + (actor.isHero() ? actor.getId() : actor.getOwnerHero()));
		if (mark != null && mark.until() >= round)
			value *= 1 + mark.strength() / 100.0;
		if (target.isMagicGuard() && "MA".equals(skill.attack()))
			value *= .85;
		if ("完好防护".equals(target.effect()) && target.getHp() == target.getStats().health())
			value *= .9;
		if ("坚岩".equals(target.effect()) && "PD".equals(skill.defense()))
			value *= .9;
		if ("守护".equals(target.effect()) && covered)
			value *= .85;
		if ("明净".equals(target.effect()) && target.getHp() == target.getStats().health() && "MA".equals(skill.attack()))
			value *= .85;
		if ("聚炎".equals(actor.effect()) && "火".equals(skill.element()))
			value *= 1.05;
		if ("猛火".equals(actor.effect()) && actor.getHp() * 3 <= actor.getStats().health()
				&& "火".equals(skill.element()))
			value *= 1.15;
		if ("雷怒".equals(actor.effect()) && actor.getHp() * 3 <= actor.getStats().health()
				&& "雷".equals(skill.element()))
			value *= 1.15;
		// 时间戳会留在持久化战局中，因此必须同时判断当前回合，不能只检查是否曾被减速。
		if (target.getSlowUntil() >= round && (("乘风".equals(actor.effect()) && "风".equals(skill.element()))
				|| ("夜袭".equals(actor.effect()) && "暗".equals(skill.element()))))
			value *= 1.15;
		return Math.max(1, (int) Math.floor(value * random));
	}

	/** 先对轻盈的基础速度独立向下取整，再应用当前减速；回合排序在所有动作执行前固定。 */
	public double speed(BattleStateUnit actor, int round) {
		int speed = actor.getStats().speed();
		if ("轻盈".equals(actor.effect()))
			speed = (int) Math.floor(speed * 1.1);
		return speed * (actor.getSlowUntil() >= round ? .8 : 1);
	}

	public double capture(BattleStateUnit target, int playerLevel, boolean tutorial, boolean reinforced) {
		return capture(target, playerLevel, tutorial, balls.find(reinforced ? "STRONG_SEAL_I" : "SEAL_I"),
				Integer.MAX_VALUE);
	}

	/** 预览与行动使用同一球配置及实际回合；过期减速不能继续提高捕捉率。 */
	public double capture(BattleStateUnit target, int playerLevel, boolean tutorial, CaptureBall ball, int round) {
		if (!acceptsBall(target, ball))
			return 0;
		double health = (double) target.getHp() / target.getStats().health();
		double multiplier = health > .75 ? .5 : health > .5 ? .75 : health >= .25 ? 1 : 1.5;
		double chance = catalog.growth(target.getSpecies()).captureRate() * multiplier * ball.multiplier()
				* (target.getSlowUntil() >= round ? 1.1 : 1);
		chance = (chance - .1 * Math.max(0, target.getLevel() - ball.level()))
				* Math.max(.1, 1 - .1 * Math.max(0, target.getLevel() - playerLevel));
		return Math.clamp(chance, .01, .85);
	}

	public int experience(int enemyLevel, int entryLevel) {
		return experience(enemyLevel, entryLevel, 1);
	}

	/** 精英基础经验先翻倍，再进行等级修正及一次向下取整。 */
	public int experience(int enemyLevel, int entryLevel, int multiplier) {
		return (int) Math
			.floor(enemyLevel * 10 * multiplier * Math.clamp(1 + .02 * (enemyLevel - entryLevel), .5, 1.5));
	}

	/** 普通目标允许越级衰减，精英必须使用覆盖目标等级的强化球。 */
	public boolean acceptsBall(BattleStateUnit target, CaptureBall ball) {
		return ball != null && (!target.isElite() || (ball.reinforced() && ball.level() >= target.getLevel()));
	}

}
