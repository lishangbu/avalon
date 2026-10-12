package io.github.lishangbu.avalon.battle.domain;

import io.github.lishangbu.avalon.content.battle.AcademyTrainingCatalog;
import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import io.github.lishangbu.avalon.content.battle.CaptureBallCatalog;
import io.github.lishangbu.avalon.content.battle.TrainingCatalog;
import io.github.lishangbu.avalon.content.battle.model.BattleSkill;
import io.github.lishangbu.avalon.content.item.PotionRecoveryRules;
import io.github.lishangbu.avalon.platform.BusinessException;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 手动回合结算，支持单体及明确选定目标的群攻。数据库操作通过窄接口加入外层事务；引擎不认识 HTTP、账号或仓储。
 */
@Component
@RequiredArgsConstructor
public class BattleEngine {

	private final PotionRecoveryRules potions;

	private static final BattleSkill ATTACK = new BattleSkill("普攻", "DAMAGE", "无", "PA", "PD", 50, 0, 0, 0, "ENEMY");

	private final BattleCatalog catalog;

	private final BattleRules rules;

	private final DirectAttackResolver attacks;

	private final CaveBossPolicy caveBoss;

	private final WildActionPolicy wildActions;

	private final CaptureBallCatalog balls;

	private final TrainingPolicy training;

	private final TrainingCatalog trainingCatalog;

	private final AcademyTrainingCatalog academy;

	private final DungeonBattlePolicy dungeon;

	public BattleSkill skill(String action) {
		return "ATTACK".equals(action) ? ATTACK : catalog.skill(action);
	}

	/**
	 * 必须在上一轮保存前调用，敌方选招不读取下一轮玩家输入，且隐藏在服务器存档中。
	 */
	public void planEnemies(BattleState state) {
		BattleEnvironment.enter(state);
		if (state.getTraining() != null) {
			training.plan(state);
			return;
		}
		if (dungeon.scripted(state)) {
			dungeon.plan(state);
			return;
		}
		if (state.getDungeonStage() == null && catalog.encounter(state.getEncounter()).boss() != null) {
			var boss = state.getUnits().stream().filter(BattleStateUnit::isEnemy).findFirst().orElseThrow();
			state.setEnemyOrders(List.of(caveBoss.plan(state, boss)));
			return;
		}
		var random = new Random(state.getSeed() ^ ((long) state.getRound() << 32));
		List<BattleStateOrder> orders = new ArrayList<>();
		for (var enemy : state.getUnits())
			if (enemy.isEnemy() && enemy.alive() && !enemy.isMechanism()) {
				var targets = state.getUnits().stream().filter(u -> !u.isEnemy() && u.active() && u.alive()).toList();
				if (targets.isEmpty())
					targets = state.getUnits().stream().filter(u -> !u.isEnemy() && u.alive()).toList();
				if (targets.isEmpty())
					break;
				var target = targets.get(random.nextInt(targets.size()));
				orders.add(wildActions.choose(state, enemy, target));
			}
		state.setEnemyOrders(orders);
	}

	public boolean available(BattleStateUnit actor, BattleSkill skill, int round) {
		return actor.getMp() >= manaCost(actor, skill) && actor.getReady().getOrDefault(skill.id(), 0) <= round;
	}

	/** 预览、提交校验和实际扣费共用；持续恢复不属于直接治疗。 */
	public int manaCost(BattleStateUnit actor, BattleSkill skill) {
		return abilityCost(actor, skill);
	}

	static int abilityCost(BattleStateUnit actor, BattleSkill skill) {
		if ("雷能充蓄".equals(actor.effect()) && actor.isThunderCharge() && actor.getFreeThunderCasts() < 2
				&& "雷".equals(skill.element()) && skill.mana() > 0 && "ENEMY".equals(skill.target()))
			return 0;
		if ("节制".equals(actor.effect()) && "HEAL".equals(skill.effect()))
			return Math.max(1, skill.mana() - 1);
		if ("舍控强攻".equals(actor.effect()) && "SLOW".equals(skill.effect()))
			return Math.max(1, skill.mana() - 2);
		return skill.mana();
	}

	private BattleSkill abilitySkill(BattleStateUnit actor, BattleSkill skill) {
		if ("舍控强攻".equals(actor.effect()) && "SLOW".equals(skill.effect()))
			return new BattleSkill(skill.id(), "DAMAGE", skill.element(), skill.attack(), skill.defense(),
					skill.power() + 40, manaCost(actor, skill), skill.cooldown(), skill.priority(), skill.target());
		return skill;
	}

	private void paySkill(BattleStateUnit actor, BattleSkill skill) {
		int cost = manaCost(actor, skill);
		if (cost == 0 && skill.mana() > 0 && "雷能充蓄".equals(actor.effect())) {
			actor.setThunderCharge(false);
			actor.setFreeThunderCasts(actor.getFreeThunderCasts() + 1);
		}
		actor.setMp(actor.getMp() - cost);
	}

	private void beginAction(BattleStateUnit actor) {
		actor.setActionCharge(actor.isDefenseCharge());
		actor.setDefenseCharge(false);
	}

	/**
	 * 整轮先验证完整指令集，拒绝重复角色、遗漏单位、非法技能与目标，不先扣部分资源。
	 */
	public void resolve(BattleState state, List<BattleStateOrder> input, boolean surrender,
			BattleEngineResources resources) {
		resolve(state, input, surrender, resources, Map.of());
	}

	/**
	 * 倒下补位先于整轮校验，替补可正常选招；主动换宠仍由 SWAP 消耗原位置行动。
	 */
	public void resolve(BattleState state, List<BattleStateOrder> input, boolean surrender,
			BattleEngineResources resources, Map<String, Integer> replacements) {
		if (surrender) {
			state.setStatus("SURRENDERED");
			state.getLog().add(state.getTraining() == null ? "你认输了，返回已解锁的安全城镇。" : "训练已退出，真实队伍资源保持不变。");
			return;
		}
		replace(state, replacements);
		BattleEnvironment.enter(state);
		validateOrders(state, input, resources, null);
		List<BattleStateOrder> orders = new ArrayList<>(input);
		orders.addAll(state.getEnemyOrders());
		execute(state, orders, resources);
	}

	/** 提交前在隔离副本上校验单个成员，不提前执行行动或消耗真实物品。 */
	public void validateMember(BattleState preview, String hero, List<BattleStateOrder> input,
			Map<String, Integer> replacements, BattleEngineResources resources) {
		if (input == null || input.stream().anyMatch(order -> order == null || "NO_ACTION".equals(order.action())))
			fail("请提交手动行动");
		for (String id : replacements.keySet()) {
			var pet = unit(preview, id);
			if (pet == null || !Objects.equals(hero, pet.getOwnerHero()))
				fail("不能调动其他成员的替补");
		}
		replace(preview, replacements);
		BattleEnvironment.enter(preview);
		// 全部所属单位已经倒下时可手动确认等待，不伪造行动；仍有替补则必须先补位。
		if (input.isEmpty() && preview.getUnits()
			.stream()
			.noneMatch(unit -> !unit.isEnemy() && unit.alive()
					&& hero.equals(unit.isHero() ? unit.getId() : unit.getOwnerHero())))
			return;
		validateOrders(preview, input, resources, hero);
	}

	private void replace(BattleState state, Map<String, Integer> replacements) {
		Set<String> filled = new HashSet<>();
		for (var entry : replacements.entrySet()) {
			var replacement = unit(state, entry.getKey());
			Integer position = entry.getValue();
			if (position == null || position < 0 || position > 1 || replacement == null || replacement.isEnemy()
					|| replacement.isHero() || !replacement.alive() || replacement.getPosition() < 2
					|| replacement.getPosition() > 3)
				fail("补位必须选择本场存活替补及空缺出战位");
			if (!filled.add(replacement.getOwnerHero() + ":" + position))
				fail("同一成员的出战位不能重复补位");
			var prior = state.getUnits()
				.stream()
				.filter(u -> !u.isEnemy() && !u.isHero() && u.getPosition() == position
						&& Objects.equals(u.getOwnerHero(), replacement.getOwnerHero()))
				.findFirst()
				.orElse(null);
			if (prior != null && prior.alive())
				fail("存活出战位不能免费补位");
			if (prior != null)
				prior.setPosition(replacement.getPosition());
			replacement.setPosition(position);
			state.getLog().add(replacement.getName() + "补入第" + (position + 1) + "出战位。");
		}
	}

	private void validateOrders(BattleState state, List<BattleStateOrder> input, BattleEngineResources resources,
			String hero) {
		var actors = state.getUnits()
			.stream()
			.filter(u -> !u.isEnemy() && u.active() && u.alive())
			.filter(u -> hero == null || hero.equals(u.isHero() ? u.getId() : u.getOwnerHero()))
			.toList();
		if (actors.isEmpty() && (hero != null || state.getDungeonStage() == null))
			fail("请先选择存活替补补位，或认输");
		if (input == null || input.size() != actors.size())
			fail("请提交所有存活出战单位的行动");
		Set<String> seen = new HashSet<>();
		for (var order : input) {
			if (order == null || !seen.add(order.actor()))
				fail("行动重复或无效");
			BattleStateUnit actor = actors.stream()
				.filter(u -> u.getId().equals(order.actor()))
				.findFirst()
				.orElseThrow(() -> new BusinessException(400, "行动单位无效"));
			validate(state, actor, order, resources);
		}
	}

	private void execute(BattleState state, List<BattleStateOrder> orders, BattleEngineResources resources) {
		var random = new Random(state.getSeed() + state.getRound());
		// 先固定同速随机顺序，再稳定排序；同一轮重试读取凭证，不再重新排序。
		Collections.shuffle(orders, random);
		orders.sort(Comparator.<BattleStateOrder>comparingInt(o -> priority(o.action()))
			.reversed()
			.thenComparing(Comparator
				.comparingDouble((BattleStateOrder o) -> speed(unit(state, o.actor()), state.getRound())
						* BattleEnvironment.speedMultiplier(state, unit(state, o.actor())))
				.reversed()));
		Set<String> defending = new HashSet<>();
		Map<String, List<DirectAttackResolverProtection>> covers = new HashMap<>();
		state.getLog().add("第 " + state.getRound() + " 回合");
		boolean normalSorted = false;
		for (int orderIndex = 0; orderIndex < orders.size(); orderIndex++) {
			if (!normalSorted && !orders.get(orderIndex).action().equals("SWAP")) {
				orders.subList(orderIndex, orders.size())
					.sort(Comparator.<BattleStateOrder>comparingInt(o -> priority(o.action()))
						.reversed()
						.thenComparing(Comparator
							.comparingDouble((BattleStateOrder o) -> speed(unit(state, o.actor()), state.getRound())
									* BattleEnvironment.speedMultiplier(state, unit(state, o.actor())))
							.reversed()));
				normalSorted = true;
			}
			var order = orders.get(orderIndex);
			var actor = unit(state, order.actor());
			if (!actor.alive() || !actor.active() || !state.getStatus().equals("ACTIVE"))
				continue;
			var target = unit(state, order.target());
			if (order.action().equals("NO_ACTION")) {
				state.getLog().add(actor.getName() + "本轮未提交行动。");
				continue;
			}
			if (actor.isEnemy() && dungeon.scripted(state)) {
				dungeon.execute(state, actor, target, .9375 + random.nextDouble() * .125, defending, covers);
				if (state.getUnits().stream().noneMatch(member -> !member.isEnemy() && member.alive()))
					state.setStatus("LOST");
				continue;
			}
			if (actor.isEnemy() && order.action().equals("TRAINING_HIT") && state.getTraining() != null) {
				var courseId = state.getTraining().getAcademyCourse();
				int power = courseId != null && !courseId.equals("BQ023") ? academy.course(courseId).power()
						: trainingCatalog.course(state.getTraining().getProfession()).opponent().power();
				var attack = new BattleSkill("训练攻击", "DAMAGE", "无", "PA", "PD", power, 0, 0, 0, "ENEMY");
				if (target != null && target.alive() && target.active()) {
					int before = target.getHp();
					attacks.apply(state, actor, List.of(target), attack, false, .9375 + random.nextDouble() * .125,
							defending, covers);
					if ("BQ018".equals(courseId) && target.isHero() && defending.contains(target.getId())
							&& target.getHp() < before)
						state.getTraining().setDefendedHit(true);
				}
				continue;
			}
			if (actor.isEnemy() && state.getTraining() == null && state.getDungeonStage() == null
					&& catalog.encounter(state.getEncounter()).boss() != null) {
				if (order.action().startsWith("WARN_"))
					caveBoss.warn(state, order.action());
				else {
					boolean area = order.action().equals("BOSS_QUAKE");
					var targets = area
							? state.getUnits()
								.stream()
								.filter(member -> !member.isEnemy() && member.active() && member.alive())
								.toList()
							: target != null && target.alive() && target.active() ? List.of(target)
									: List.<BattleStateUnit>of();
					var attack = new BattleSkill(area ? "全体震击" : order.action().equals("BOSS_HEAVY") ? "重击" : "普通攻击",
							"DAMAGE", "无", area ? "MA" : "PA", area ? "MD" : "PD",
							area ? 70 : order.action().equals("BOSS_HEAVY") ? 110 : 50, 0, 0, 0, "ENEMY");
					attacks.apply(state, actor, targets, attack, area, .9375 + random.nextDouble() * .125, defending,
							covers);
				}
				if (state.getUnits().stream().noneMatch(member -> !member.isEnemy() && member.alive()))
					state.setStatus("LOST");
				continue;
			}
			if (order.action().equals("DEFEND")) {
				training.acted(state, actor);
				defending.add(actor.getId());
				actor.setDefenseCharge(true);
				actor.setActionCharge(false);
				state.getLog().add(actor.getName() + "防御。");
				EquipmentSetPolicy.defend(state, actor);
				continue;
			}
			if (order.action().equals("RETREAT")) {
				beginAction(actor);
				int level = state.getUnits()
					.stream()
					.filter(BattleStateUnit::isEnemy)
					.mapToInt(BattleStateUnit::getLevel)
					.max()
					.orElse(actor.getLevel());
				if (random.nextDouble() < Math.clamp(.7 + .05 * (actor.getLevel() - level), .2, .95))
					state.setStatus("RETREATED");
				state.getLog().add(state.getStatus().equals("RETREATED") ? "撤退成功。" : "撤退失败。");
				continue;
			}
			if (order.action().equals("SWAP")) {
				if (target == null || !target.alive() || target.active()) {
					state.getLog().add("替换目标无效，本次行动落空。");
					continue;
				}
				if ("再生力".equals(actor.effect()))
					actor.setHp(Math.min(actor.getStats().health(), actor.getHp() + actor.getStats().health() / 3));
				if ("接力守护".equals(actor.effect()) && !actor.isRelayGuardUsed()) {
					actor.setRelayGuardUsed(true);
					target.setRelayGuardUntil(state.getRound() + 1);
				}
				DefensiveAbilityPolicy.leave(actor);
				int old = actor.getPosition();
				actor.setSetShield(0);
				target.setSetShield(0);
				actor.setPosition(target.getPosition());
				target.setPosition(old);
				BattleEnvironment.enter(state);
				if (state.getTraining() != null && state.getTraining().isSwapPractice())
					state.getTraining().setSwapped(true);
				state.getLog().add(actor.getName() + "换下，" + target.getName() + "出战。");
				continue;
			}
			var selectedSkill = skill(order.action());
			if (selectedSkill != null && selectedSkill.effect().equals("AREA")) {
				if (!available(actor, selectedSkill, state.getRound())) {
					state.getLog().add(actor.getName() + "魔力不足，本次行动落空。");
					continue;
				}
				var targets = order.targets()
					.stream()
					.map(id -> unit(state, id))
					.filter(u -> u != null && u.alive() && u.active())
					.toList();
				if (targets.isEmpty()) {
					state.getLog().add(actor.getName() + "的目标已失效，本次行动落空。");
					continue;
				}
				beginAction(actor);
				paySkill(actor, selectedSkill);
				actor.getReady().put(selectedSkill.id(), state.getRound() + selectedSkill.cooldown() + 1);
				attacks.apply(state, actor, targets, selectedSkill, true, .9375 + random.nextDouble() * .125, defending,
						covers);
				if (state.getUnits().stream().noneMatch(u -> u.isEnemy() && !u.isMechanism() && u.alive()))
					state.setStatus("WON");
				continue;
			}
			if (target == null || !target.alive() || !target.active()) {
				state.getLog().add(actor.getName() + "的目标已失效，本次行动落空。");
				continue;
			}
			if (selectedSkill != null && !available(actor, selectedSkill, state.getRound())) {
				state.getLog().add(actor.getName() + "魔力不足，本次行动落空。");
				continue;
			}
			beginAction(actor);
			training.acted(state, actor);
			if (balls.find(order.action()) != null) {
				if (!resources.room(target)) {
					state.getLog().add("容量不足，未消耗封印球。");
					continue;
				}
				resources.take(actor, order.action());
				boolean tutorial = state.getTraining() == null && catalog.encounter(state.getEncounter()).tutorial();
				if (random.nextDouble() < rules.capture(target, actor.getLevel(), tutorial, balls.find(order.action()),
						state.getRound())) {
					resources.capture(target);
					if (state.getTraining() != null && state.getTraining().capturePractice())
						state.getTraining().setCapturedAfterDamage(target.getHp() < target.getStats().health());
					target.setCaptured(true);
					state.getLog().add("成功捕获" + target.getName() + "，特性：" + target.getAbility() + "。");
				}
				else
					state.getLog().add("捕捉失败，封印球已消耗。");
			}
			else if (potions.recovery(order.action(),
					state.getTraining() != null && state.getTraining().capturePractice()) != null) {
				resources.take(actor, order.action());
				var recovery = potions.recovery(order.action(),
						state.getTraining() != null && state.getTraining().capturePractice());
				target.setHp(Math.min(target.getStats().health(), target.getHp() + recovery.health()));
				target.setMp(Math.min(target.getStats().mana(), target.getMp() + recovery.mana()));
				state.getLog().add(actor.getName() + "为" + target.getName() + "使用了药品。");
			}
			else {
				var skill = abilitySkill(actor, skill(order.action()));
				if (!available(actor, skill, state.getRound())) {
					state.getLog().add(actor.getName() + "的魔力不足，本次行动落空。");
					continue;
				}
				paySkill(actor, skill);
				actor.getReady().put(skill.id(), state.getRound() + skill.cooldown() + 1);
				switch (skill.effect()) {
					case "WALL" -> {
						actor.setWallUntil(state.getRound() + 1);
						actor.setWallPercent(skill.power());
						state.getLog().add(actor.getName() + "施放盾墙，本轮及下一轮直接伤害降低" + skill.power() + "%。");
					}
					case "RENEW" -> {
						target.setRenewAmount((int) Math.floor(actor.getStats().magicAttack() * skill.power() / 100.0));
						target.setRenewStarted(state.getRound());
						target.setRenewUntil(state.getRound() + 2);
						state.getLog().add(actor.getName() + "为" + target.getName() + "施放恢复。");
					}
					case "MARK" -> {
						target.getMarks().put(actor.getId(), new BattleStateMark(state.getRound() + 2, skill.power()));
						state.getLog().add(actor.getName() + "为" + target.getName() + "施加猎人印记。");
					}
					case "MAGIC_GUARD" -> {
						target.setMagicGuard(true);
						state.getLog().add(actor.getName() + "为" + target.getName() + "施放光幕，本回合法术直接攻击减伤15%。");
					}
					case "HEAL" -> {
						double amount = actor.getStats().magicAttack() * skill.power() / 100.0;
						if ("回春".equals(actor.effect()) && target.getHp() * 2 <= target.getStats().health())
							amount *= 1.15;
						if ("滋养".equals(actor.effect()))
							amount *= 1.05;
						int healing = Math.min(target.getStats().health() - target.getHp(), (int) Math.floor(amount));
						target.setHp(target.getHp() + healing);
						if ("生命共鸣".equals(actor.effect()) && !actor.isHero() && !target.isHero() && actor != target
								&& actor.isEnemy() == target.isEnemy()
								&& Objects.equals(actor.getOwnerHero(), target.getOwnerHero()))
							actor.setHp(Math.min(actor.getStats().health(), actor.getHp() + healing / 4));
						if ("净羽".equals(actor.effect()) && actor.getCleanseRound() != state.getRound()
								&& target.getSlowUntil() >= state.getRound()) {
							target.setSlowUntil(0);
							actor.setCleanseRound(state.getRound());
						}
						state.getLog()
							.add(actor.getName() + "使用" + skill.id() + "，恢复" + target.getName() + " " + healing
									+ "生命。");
					}
					case "COVER" -> {
						covers.computeIfAbsent(target.getId(), k -> new ArrayList<>())
							.add(new DirectAttackResolverProtection(actor.getId(), skill.id().equals("援护")));
						state.getLog().add(actor.getName() + "保护" + target.getName() + "。");
					}
					case "SHIELD" -> {
						actor.setShield(actor.getStats().magicAttack() * 2);
						actor.setShieldUntil(state.getRound() + 2);
						state.getLog().add(actor.getName() + "展开法力护盾。");
					}
					default -> {
						attacks.apply(state, actor, List.of(target), skill, false, .9375 + random.nextDouble() * .125,
								defending, covers);
					}
				}
			}
			if (state.getUnits().stream().noneMatch(u -> u.isEnemy() && !u.isMechanism() && u.alive()))
				state.setStatus("WON");
			if (state.getUnits().stream().noneMatch(u -> !u.isEnemy() && u.alive()))
				state.setStatus("LOST");
		}
		for (var u : state.getUnits()) {
			if (u.alive() && u.active() && u.getRenewUntil() >= state.getRound() && u.getRenewAmount() > 0) {
				int amount = Math.min(u.getRenewAmount(), u.getStats().health() - u.getHp());
				u.setHp(u.getHp() + amount);
				training.healed(state, u, amount);
				state.getLog().add(u.getName() + "的恢复生效，回复 " + amount + "生命。");
			}
			if (!u.alive()) {
				u.setRenewUntil(0);
				u.setPoisonUntil(0);
				u.setWallUntil(0);
				u.getMarks().clear();
			}
			u.setMagicGuard(false);
			u.setSetShield(0);
			if (u.getShieldUntil() <= state.getRound() || !u.alive())
				u.setShield(0);
		}
		PoisonPolicy.finish(state, catalog);
		BattleEnvironment.finish(state);
		if (state.getTraining() != null)
			training.finishRound(state);
		if (state.getDungeonStage() != null)
			dungeon.finish(state);
		if (state.getDungeonStage() == null && state.getTraining() == null && state.getStatus().equals("ACTIVE")
				&& catalog.encounter(state.getEncounter()).boss() != null) {
			var boss = state.getUnits().stream().filter(BattleStateUnit::isEnemy).findFirst().orElseThrow();
			caveBoss.advance(state, boss, state.getEnemyOrders().getFirst().action());
		}
		state.setRound(state.getRound() + 1);
		if (state.getStatus().equals("ACTIVE"))
			planEnemies(state);
	}

	private void validate(BattleState state, BattleStateUnit actor, BattleStateOrder order,
			BattleEngineResources resources) {
		String action = order.action();
		if (action == null)
			fail("缺少行动");
		// 仅由副本回合协调器为超时成员补入；公开提交入口拒绝此动作。
		if (action.equals("NO_ACTION") && state.getDungeonStage() != null)
			return;
		boolean captureLesson = state.getTraining() != null && state.getTraining().capturePractice();
		if (potions.academyPotions().contains(action) && !captureLesson)
			fail("本物品仅可在捕捉练习使用");
		if (state.getTraining() != null
				&& (action.equals("RETREAT") || (action.equals("SWAP") && !state.getTraining().isSwapPractice())
						|| !captureLesson && (balls.find(action) != null || potions.usablePotions().contains(action))))
			fail("当前训练不支持此行动或真实道具");
		if (action.equals("DEFEND"))
			return;
		if (action.equals("RETREAT")) {
			if (!actor.isHero())
				fail("只有人物可以撤退");
			if (state.getDungeonStage() != null)
				fail("副本内请使用队伍退出操作");
			if (catalog.encounter(state.getEncounter()).boss() != null)
				fail("首领战只能认输，不能撤退");
			return;
		}
		var selected = skill(action);
		if (selected != null && selected.effect().equals("AREA")) {
			if (!actor.getSkills().contains(action) || !available(actor, selected, state.getRound()))
				fail("技能不可用");
			if (order.targets() == null || order.targets().isEmpty() || order.targets().size() > 3
					|| new HashSet<>(order.targets()).size() != order.targets().size())
				fail("请选择一至三个不同敌人");
			for (var id : order.targets()) {
				var target = unit(state, id);
				if (target == null || !target.alive() || !target.active() || target.isEnemy() == actor.isEnemy())
					fail("群攻目标不合法");
			}
			return;
		}
		if (order.targets() != null && !order.targets().isEmpty())
			fail("单体行动不能附加群攻目标");
		var target = unit(state, order.target());
		if (target == null || !target.alive())
			fail("目标不存在或已倒下");
		if (action.equals("SWAP")) {
			if (actor.isHero() || target.isEnemy() || target.isHero()
					|| !Objects.equals(actor.getOwnerHero(), target.getOwnerHero()) || target.getPosition() < 2
					|| target.getPosition() > 3)
				fail("只能与本场替补交换");
			return;
		}
		if (!target.active())
			fail("目标不在场上");
		if (balls.find(action) != null || potions.recovery(action,
				state.getTraining() != null && state.getTraining().capturePractice()) != null) {
			if (!actor.isHero() || resources.count(actor, action) < 1)
				fail("人物才能使用道具，且库存必须足够");
			if (balls.find(action) != null) {
				if (!target.isEnemy() || !state.isCaptureAllowed() || !resources.room(target))
					fail("当前目标不可捕捉或容量不足");
				if (!rules.acceptsBall(target, balls.find(action)))
					fail("精英需要符合等级的强化封印球");
			}
			else if (target.isEnemy())
				fail("药品只能对存活友方使用");
			return;
		}
		var skill = skill(action);
		if (skill == null || (!action.equals("ATTACK") && !actor.getSkills().contains(action))
				|| !available(actor, skill, state.getRound()))
			fail("技能不可用");
		boolean enemy = actor.isEnemy() != target.isEnemy();
		if (skill.target().equals("ENEMY") != enemy || (skill.target().equals("SELF") && target != actor)
				|| (skill.target().equals("OTHER") && target == actor))
			fail("技能目标不合法");
	}

	private int priority(String action) {
		return action.equals("SWAP") ? 2
				: action.equals("DEFEND") ? 1 : skill(action) == null ? 0 : skill(action).priority();
	}

	private double speed(BattleStateUnit actor, int round) {
		return rules.speed(actor, round);
	}

	private BattleStateUnit unit(BattleState state, String id) {
		return state.getUnits().stream().filter(u -> u.getId().equals(id)).findFirst().orElse(null);
	}

	private void fail(String message) {
		throw new BusinessException(400, message);
	}

}
