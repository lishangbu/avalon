package io.github.lishangbu.avalon.battle.application.model;

import io.github.lishangbu.avalon.content.battle.model.BattleSkillEffect;
import io.github.lishangbu.avalon.battle.domain.BattleEngine;
import io.github.lishangbu.avalon.battle.domain.BattleRules;
import io.github.lishangbu.avalon.battle.domain.BattleState;
import io.github.lishangbu.avalon.battle.domain.BattleStateUnit;
import io.github.lishangbu.avalon.battle.domain.PoisonPolicy;
import io.github.lishangbu.avalon.content.battle.AcademyTrainingCatalog;
import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import io.github.lishangbu.avalon.content.battle.CaptureBallCatalog;
import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.content.battle.TrainingCatalog;
import io.github.lishangbu.avalon.content.battle.model.BattleSkill;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.item.PotionRecoveryRules;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 投影服务集中隐藏信息与概率预览。库存和容量读取均不修改玩家状态。
 */
@Component
@RequiredArgsConstructor
public class BattleViews {

	private final PotionRecoveryRules potions;

	private final BattleCatalog catalog;

	private final BattleRules rules;

	private final BattleEngine engine;

	private final InventoryQueryService inventory;

	private final CreatureQueryService creatures;

	private final GrowthRules growth;

	private final CaptureBallCatalog balls;

	private final ItemCatalog items;

	private final TrainingCatalog training;

	private final AcademyTrainingCatalog academy;

	public Map<String, Integer> supplies(long owner) {
		Map<String, Integer> result = new HashMap<>();
		inventory.list(owner).forEach(i -> result.merge(i.code(), i.quantity(), Integer::sum));
		return result;
	}

	public boolean room(long owner, int playerLevel, int targetLevel) {
		var pets = creatures.list(owner);
		return (targetLevel <= playerLevel && pets.stream()
			.filter(p -> p.partyPosition() >= 2 && p.partyPosition() < growth.carryingCapacity(playerLevel))
			.count() < growth.carryingCapacity(playerLevel) - 2)
				|| pets.stream().filter(p -> p.partyPosition() >= 100).count() < 30;
	}

	public BattleView view(long owner, long id, BattleState state) {
		var hero = state.getUnits().stream().filter(BattleStateUnit::isHero).findFirst().orElseThrow();
		boolean captureLesson = state.getTraining() != null && state.getTraining().capturePractice();
		var supplies = captureLesson ? state.getTraining().getSupplies()
				: state.getTraining() == null ? supplies(owner) : Map.<String, Integer>of();
		boolean tutorial = state.getTraining() == null && catalog.encounter(state.getEncounter()).tutorial();
		boolean canCapture = state.isCaptureAllowed() && hero.alive()
				&& state.getUnits()
					.stream()
					.anyMatch(target -> target.isEnemy() && target.alive()
							&& (captureLesson || room(owner, hero.getLevel(), target.getLevel())));
		var units = state.getUnits().stream().map(u -> {
			List<BattleViewAction> actions = new ArrayList<>();
			if (!u.isEnemy()) {
				actions.add(new BattleViewAction("ATTACK", "普通攻击", "ENEMY", 0, 0, true));
				actions.add(new BattleViewAction("DEFEND", "防御", "SELF", 0, 0, true));
				for (var name : u.getSkills()) {
					var skill = catalog.skill(name);
					actions.add(new BattleViewAction(name, name, skill.target(), engine.manaCost(u, skill),
							Math.max(0, u.getReady().getOrDefault(name, 0) - state.getRound()),
							engine.available(u, skill, state.getRound())));
				}
				if (u.isHero() && (state.getTraining() == null || captureLesson)) {
					var consumables = new ArrayList<>(
							captureLesson ? potions.academyPotions() : potions.usablePotions());
					balls.all().forEach(ball -> consumables.add(ball.code()));
					for (var code : consumables)
						actions.add(new BattleViewAction(code, items.item(code).name(),
								balls.find(code) != null ? "ENEMY" : "ALLY", 0, 0,
								supplies.getOrDefault(code, 0) > 0
										&& (balls.find(code) == null || (canCapture && state.getUnits()
											.stream()
											.anyMatch(target -> target.isEnemy() && target.alive()
													&& rules.acceptsBall(target, balls.find(code)))))));
					if (!captureLesson)
						actions.add(new BattleViewAction("RETREAT", "撤退", "SELF", 0, 0,
								catalog.encounter(state.getEncounter()).boss() == null));
				}
				else if (!u.isHero() && (state.getTraining() == null || state.getTraining().isSwapPractice()))
					actions.add(new BattleViewAction("SWAP", "换入替补", "RESERVE", 0, 0,
							state.getUnits()
								.stream()
								.anyMatch(p -> !p.isEnemy() && !p.isHero() && p.getPosition() >= 2
										&& p.getPosition() < 4 && p.alive())));
			}
			boolean catchable = u.isEnemy() && u.alive() && state.isCaptureAllowed() && hero.alive()
					&& (captureLesson || room(owner, hero.getLevel(), u.getLevel()));
			return new BattleViewUnitView(u.getId(), u.getName(), u.getElement(), u.getLevel(), u.isEnemy(), u.isHero(),
					u.getPosition(), u.getHp(), u.getMp(), u.getStats(), u.isCaptured(),
					u.isEnemy() && !u.isCaptured() ? null : u.getAbility(), u.getShield(),
					Math.max(0, u.getSlowUntil() - state.getRound() + 1), effects(u, state.getRound()),
					List.copyOf(actions),
					balls.all()
						.stream()
						.filter(ball -> catchable && supplies.getOrDefault(ball.code(), 0) > 0
								&& rules.acceptsBall(u, ball))
						.map(ball -> new BattleViewCaptureOption(ball.code(), items.item(ball.code()).name(),
								ball.level(), rules.capture(u, hero.getLevel(), tutorial, ball, state.getRound())))
						.toList(),
					u.isEnemy() && state.isCaptureAllowed()
							? captureLesson ? u.getStats() : rules.stats(u.getSpecies(), u.getLevel(), false, Map.of())
							: null);
		}).toList();
		return new BattleView(Long.toString(id), state.getRound(), state.getRevision(), state.getStatus(),
				state.getTraining() == null ? catalog.encounter(state.getEncounter()).name()
						: academy.course(state.getTraining().getAcademyCourse()).name(),
				units, List.copyOf(state.getLog()), Map.copyOf(supplies),
				balls.all()
					.stream()
					.filter(ball -> supplies.getOrDefault(ball.code(), 0) > 0)
					.map(ball -> new BattleViewCaptureSupply(ball.code(), items.item(ball.code()).name(), ball.level(),
							supplies.get(ball.code())))
					.toList(),
				telegraph(state), state.getTraining() != null, academyCapture(state),
				state.getLoot()
					.stream()
					.filter(loot -> loot.owner() == owner)
					.map(loot -> new BattleViewLoot(loot.code(), items.item(loot.code()).name(), loot.quantity()))
					.toList());
	}

	private BattleViewAcademyCapture academyCapture(BattleState state) {
		if (state.getTraining() == null || !state.getTraining().capturePractice())
			return null;
		var target = state.getUnits().stream().filter(BattleStateUnit::isEnemy).findFirst().orElseThrow();
		return new BattleViewAcademyCapture(state.getTraining().isCaptureInfoViewed(),
				state.getTraining().getSelectedBall(), target.getId(),
				"比较目标公开属性与封印球：I覆盖10级，II覆盖20级；削弱后合法捕获即可，I成功也通过。训练个体与物品不能带出。", List.of("SEAL_I", "SEAL_II"),
				target.isCaptured() ? target.getAbilitySlot() : null, target.isCaptured() ? target.getAbility() : null);
	}

	/** 只投影仍生效的公开技能状态，不暴露敌方隐藏特性或未来行动。 */
	private List<String> effects(BattleStateUnit unit, int round) {
		var effects = new ArrayList<String>();
		if (unit.getPoisonUntil() >= round && unit.alive())
			effects.add("中毒 " + (unit.getPoisonUntil() - Math.max(round, unit.getPoisonStarted() + 1) + 1) + "跳，预计每跳 "
					+ PoisonPolicy.damage(unit, catalog) + "伤害");
		if (unit.getWallUntil() >= round)
			effects.add("盾墙 " + (unit.getWallUntil() - round + 1) + "回合");
		if (unit.getRenewUntil() >= round && unit.getRenewAmount() > 0)
			effects.add("恢复 " + (unit.getRenewUntil() - round + 1) + "回合");
		if (unit.getStatuses().values().stream().anyMatch(mark -> mark.kind().equals("MARK") && mark.until() >= round))
			effects.add("猎人印记");
		return List.copyOf(effects);
	}

	/**
	 * 首领伤害范围按当前攻防估算，不预抽随机数。未提交的防御、保护和治疗不会被伪装成既定结果。
	 */
	private String telegraph(BattleState state) {
		// 终局已推进回合计数，不再读取下一回合脚本或展示未发生的攻击预估。
		if (!state.getStatus().equals("ACTIVE"))
			return null;
		if (state.getTraining() != null || state.getTelegraph() == null
				|| catalog.encounter(state.getEncounter()).boss() == null)
			return state.getTelegraph();
		var order = state.getEnemyOrders().getFirst();
		if (order.action().startsWith("WARN_"))
			return state.getTelegraph();
		var boss = state.getUnits().stream().filter(BattleStateUnit::isEnemy).findFirst().orElseThrow();
		boolean area = order.action().equals("BOSS_QUAKE");
		var attack = new BattleSkill("首领预估",
				List.of(new BattleSkillEffect("DAMAGE", area ? 70 : order.action().equals("BOSS_HEAVY") ? 110 : 50)),
				"无", area ? "MA" : "PA", area ? "MD" : "PD", 0, 0, 0, "ENEMY", null, null);
		var estimates = new ArrayList<String>();
		for (var target : state.getUnits()) {
			if (target.isEnemy() || !target.active() || !target.alive()
					|| (!area && !target.getId().equals(order.target())))
				continue;
			int low = rules.damage(boss, target, attack, false, false, .9375, state.getRound());
			int high = rules.damage(boss, target, attack, false, false, 1.0625, state.getRound());
			estimates.add(target.getName() + " " + low + "～" + high);
		}
		return state.getTelegraph() + " 当前未防御伤害（护盾吸收前）：" + String.join("；", estimates) + "。";
	}

}
