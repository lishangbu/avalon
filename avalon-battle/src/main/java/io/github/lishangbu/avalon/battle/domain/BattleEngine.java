package io.github.lishangbu.avalon.battle.domain;

import io.github.lishangbu.avalon.content.battle.model.BattleSkillEffect;
import java.util.stream.Collectors;

import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import io.github.lishangbu.avalon.content.battle.CaptureBallCatalog;
import io.github.lishangbu.avalon.content.battle.model.BattleSkill;
import io.github.lishangbu.avalon.content.item.PotionRecoveryRules;
import io.github.lishangbu.avalon.platform.BusinessException;
import java.util.*;
import io.github.lishangbu.avalon.combat.*;
import io.github.lishangbu.avalon.combat.model.*;
import org.springframework.stereotype.Component;

/**
 * 手动回合结算，支持单体及明确选定目标的群攻。数据库操作通过窄接口加入外层事务；引擎不认识 HTTP、账号或仓储。
 */
@Component
public class BattleEngine {

	private final PotionRecoveryRules potions;

	private static final BattleSkill ATTACK = new BattleSkill("普攻", List.of(new BattleSkillEffect("DAMAGE", 50)), "无",
			"PA", "PD", 0, 0, 0, "ENEMY", null, null);

	private final BattleCatalog catalog;

	private final BattleRules rules;

	private final CaptureBallCatalog balls;

	private final TrainingPolicy training;

	/** 有序技能效果注册器，未知类型启动拒绝；引擎不再维护固定技能switch。 */
	private final BattleSkillEffects skillEffects;

	/** 敌方玩法目录独立注册，回合调度不包含训练或首领动作分支。 */
	private final BattleEnemyPolicies enemyPolicies;

	/** 可注册阶段钩子使用核心固定顺序和同一触发预算。 */
	private final CombatPhaseHooks<BattleState, BattleRoundInput> phaseHooks;

	/** 扩展目录在装配时固定并校验，重复钩子立即阻止启动，不等到首个玩家回合才失败。 */
	public BattleEngine(PotionRecoveryRules potions, BattleCatalog catalog, BattleRules rules, CaptureBallCatalog balls,
			TrainingPolicy training, BattleSkillEffects skillEffects, BattleEnemyPolicies enemyPolicies,
			List<BattlePhaseHook> phaseHooks) {
		this.potions = potions;
		this.catalog = catalog;
		this.rules = rules;
		this.balls = balls;
		this.training = training;
		this.skillEffects = skillEffects;
		this.enemyPolicies = enemyPolicies;
		this.phaseHooks = new CombatPhaseHooks<>(phaseHooks);
	}

	public BattleSkill skill(String action) {
		return "ATTACK".equals(action) ? ATTACK : catalog.skill(action);
	}

	/**
	 * 必须在上一轮保存前调用，敌方选招不读取下一轮玩家输入，且隐藏在服务器存档中。
	 */
	public void planEnemies(BattleState state) {
		if (state.isPvp()) {
			state.setEnemyOrders(List.of());
			return;
		}
		BattleEnvironment.enter(state);
		enemyPolicies.require(state).plan(state, Random::new);
	}

	/** 回合关闭时使用核心记录的具名随机流锁定下一轮意图。 */
	private void planEnemies(BattleState state, CombatFrame<BattleState, BattleRoundInput> frame) {
		if (state.isPvp()) {
			state.setEnemyOrders(List.of());
			return;
		}
		BattleEnvironment.enter(state, frame::random);
		enemyPolicies.require(state).plan(state, seed -> frame.random("enemy-intent-" + state.getRound(), seed));
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
		if ("节制".equals(actor.effect()) && skill.hasEffect("HEAL"))
			return Math.max(1, skill.mana() - 1);
		if ("舍控强攻".equals(actor.effect()) && skill.hasEffect("SLOW"))
			return Math.max(1, skill.mana() - 2);
		return skill.mana();
	}

	/** 特性逐项转换减速效果，保留同技能其他效果、条件与配置顺序。 */
	private BattleSkill abilitySkill(BattleStateUnit actor, BattleSkill skill) {
		if ("舍控强攻".equals(actor.effect()) && skill.hasEffect("SLOW")) {
			var effects = skill.effects()
				.stream()
				.map(effect -> effect.kind().equals("SLOW")
						? new BattleSkillEffect("DAMAGE", Math.addExact(effect.power(), 40)) : effect)
				.toList();
			return new BattleSkill(skill.id(), effects, skill.element(), skill.attack(), skill.defense(), skill.mana(),
					skill.cooldown(), skill.priority(), skill.target(), skill.lowHealthPower(), skill.description());
		}
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

	/** 唯一纯执行入口：状态与资源均隔离，输出由宿主原子应用，不执行任何真实资产写入。 */
	public BattleRoundResult advance(BattleState state, List<BattleStateOrder> input, boolean surrender,
			BattleResourceSnapshot snapshot, Map<String, Integer> replacements) {
		var roundInput = new BattleRoundInput(input, surrender, replacements, snapshot);
		var ledger = new BattleResourceLedger(snapshot);
		var session = new BattleRoundSession(state, input, surrender, ledger, replacements);
		var transition = new CombatRoundEngine().resolve(state, roundInput,
				new CombatRoundRules<BattleState, BattleRoundInput>() {
					public BattleState copy(BattleState original) {
						return BattleStateCodec.copy(original);
					}

					public int round(BattleState value) {
						return value.getRound();
					}

					public boolean terminal(BattleState value) {
						return !"ACTIVE".equals(value.getStatus());
					}

					public void apply(CombatPhase phase, CombatFrame<BattleState, BattleRoundInput> frame) {
						session.state = frame.state();
						session.frame = frame;
						applyPhase(phase, session, frame);
						phaseHooks.apply(frame);
					}
				}, CombatLimits.defaults());
		var before = BattleStateCodec.copy(state);
		return new BattleRoundResult(before, transition.state(), roundInput, ledger.changes(), transition.events());
	}

	/** 固定阶段由JDK核心推进；这里仅实现Avalon玩法，不改变阶段顺序。 */
	private void applyPhase(CombatPhase phase, BattleRoundSession session,
			CombatFrame<BattleState, BattleRoundInput> frame) {
		var state = session.state;
		switch (phase) {
			case OPEN -> {
				if (session.surrender) {
					state.setStatus("SURRENDERED");
					state.getLog().add(state.getTraining() == null ? "你认输了，返回已解锁的安全城镇。" : "训练已退出，真实队伍资源保持不变。");
				}
			}
			case INTENT -> frame.event("INTENT", Map.of("count", Integer.toString(state.getEnemyOrders().size())));
			case COMMANDS -> {
				// 指令校验使用独立预览登记补位；正式状态只在REPLACEMENTS阶段变更。
				var preview = BattleStateCodec.copy(state);
				replace(preview, session.replacements);
				BattleEnvironment.enter(preview);
				validateOrders(preview, session.input, session.resources, null);
				session.orders.addAll(session.input);
				if (!state.isPvp())
					session.orders.addAll(state.getEnemyOrders());
			}
			case REPLACEMENTS -> {
				replace(state, session.replacements);
				BattleEnvironment.enter(state, frame::random);
			}
			case SWAPS -> {
				session.random = frame.random("actions", state.getSeed() + state.getRound());
				Collections.shuffle(session.orders, session.random);
				sortOrders(state, session.orders);
				state.getLog().add("第 " + state.getRound() + " 回合");
				var swaps = session.orders.stream().filter(order -> order.action().equals("SWAP")).toList();
				executeActions(session, swaps);
				session.orders.removeIf(order -> order.action().equals("SWAP"));
			}
			case ORDER -> {
				sortOrders(state, session.orders);
				frame.event("ORDER", Map.of("actors",
						session.orders.stream().map(BattleStateOrder::actor).collect(Collectors.joining(","))));
			}
			case ACTIONS -> executeActions(session, session.orders);
			case END -> finishRound(state);
			case CLOSE -> {
				if (!state.isPvp())
					enemyPolicies.require(state).close(state, session.surrender);
				state.setRevision(Math.incrementExact(state.getRevision()));
				if (!session.surrender)
					state.setRound(state.getRound() + 1);
				if (state.getStatus().equals("ACTIVE"))
					planEnemies(state, frame);
			}
		}
	}

	/** 排序读取当前阶段有效速度；队列锁定后执行期间不得重新排序。 */
	private void sortOrders(BattleState state, List<BattleStateOrder> orders) {
		orders.sort(Comparator.<BattleStateOrder>comparingInt(o -> priority(o.action()))
			.reversed()
			.thenComparing(Comparator
				.comparingDouble((BattleStateOrder o) -> speed(unit(state, o.actor()), state.getRound())
						* BattleEnvironment.speedMultiplier(state, unit(state, o.actor())))
				.reversed()));
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
			.noneMatch(unit -> (preview.isPvp() || !unit.isEnemy()) && unit.alive()
					&& hero.equals(unit.isHero() ? unit.getId() : unit.getOwnerHero())))
			return;
		validateOrders(preview, input, resources, hero);
	}

	private void replace(BattleState state, Map<String, Integer> replacements) {
		Set<String> filled = new HashSet<>();
		for (var entry : replacements.entrySet()) {
			var replacement = unit(state, entry.getKey());
			Integer position = entry.getValue();
			if (position == null || position < 0 || position > 1 || replacement == null
					|| (!state.isPvp() && replacement.isEnemy()) || replacement.isHero() || !replacement.alive()
					|| replacement.getPosition() < 2 || replacement.getPosition() > 3)
				fail("补位必须选择本场存活替补及空缺出战位");
			if (!filled.add(replacement.getOwnerHero() + ":" + position))
				fail("同一成员的出战位不能重复补位");
			var prior = state.getUnits()
				.stream()
				.filter(u -> u.isEnemy() == replacement.isEnemy() && !u.isHero() && u.getPosition() == position
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
			.filter(u -> (state.isPvp() || !u.isEnemy()) && u.active() && u.alive())
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

	/** 已锁定队列的动作执行；同一session共享防御、保护与随机流。 */
	private void executeActions(BattleRoundSession session, List<BattleStateOrder> orders) {
		var state = session.state;
		var resources = session.resources;
		var random = session.random;
		var defending = session.defending;
		var covers = session.covers;
		for (int orderIndex = 0; orderIndex < orders.size(); orderIndex++) {
			var order = orders.get(orderIndex);
			var actor = unit(state, order.actor());
			if (!actor.alive() || !actor.active() || !state.getStatus().equals("ACTIVE"))
				continue;
			var target = unit(state, order.target());
			if (order.action().equals("NO_ACTION")) {
				state.getLog().add(actor.getName() + "本轮未提交行动。");
				continue;
			}
			if (!state.isPvp() && actor.isEnemy() && enemyPolicies.require(state)
				.execute(new BattleEnemyAction(state, actor, target, order, random, defending, covers))) {
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
				BattleEnvironment.enter(state, session.frame::random);
				if (state.getTraining() != null && state.getTraining().isSwapPractice())
					state.getTraining().setSwapped(true);
				state.getLog().add(actor.getName() + "换下，" + target.getName() + "出战。");
				continue;
			}
			var selectedSkill = skill(order.action());
			if (selectedSkill != null && selectedSkill.hasEffect("AREA")) {
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
				skillEffects.apply(new BattleEffectContext(state, actor, targets.getFirst(), targets,
						abilitySkill(actor, selectedSkill), true, random, defending, covers), session.frame);
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
				var originalSkill = skill(order.action());
				var skill = abilitySkill(actor, originalSkill);
				if (!available(actor, originalSkill, state.getRound())) {
					state.getLog().add(actor.getName() + "的魔力不足，本次行动落空。");
					continue;
				}
				paySkill(actor, originalSkill);
				actor.getReady().put(skill.id(), state.getRound() + skill.cooldown() + 1);
				skillEffects.apply(new BattleEffectContext(state, actor, target, List.of(target), skill, false, random,
						defending, covers), session.frame);
			}
			if (state.getUnits().stream().noneMatch(u -> u.isEnemy() && !u.isMechanism() && u.alive()))
				state.setStatus("WON");
			if (state.getUnits().stream().noneMatch(u -> !u.isEnemy() && u.alive()))
				state.setStatus("LOST");
		}
	}

	/** 当前游戏已实现的回合末规则；候选聚合公式尚未交付，不在抽取时隐式改写数值。 */
	private void finishRound(BattleState state) {
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
				u.getStatuses().values().removeIf(status -> status.kind().equals("MARK"));
			}
			u.setMagicGuard(false);
			u.setSetShield(0);
			if (u.getShieldUntil() <= state.getRound() || !u.alive())
				u.setShield(0);
		}
		PoisonPolicy.finish(state, catalog);
		BattleEnvironment.finish(state);
		if (!state.isPvp())
			enemyPolicies.require(state).finish(state);
	}

	private void validate(BattleState state, BattleStateUnit actor, BattleStateOrder order,
			BattleEngineResources resources) {
		String action = order.action();
		if (action == null)
			fail("缺少行动");
		// 仅由副本回合协调器为超时成员补入；公开提交入口拒绝此动作。
		if (action.equals("NO_ACTION") && (state.getDungeonStage() != null || state.isPvp()))
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
			if (state.isPvp())
				fail("玩家对战请使用认输操作");
			if (!actor.isHero())
				fail("只有人物可以撤退");
			if (state.getDungeonStage() != null)
				fail("副本内请使用队伍退出操作");
			if (catalog.encounter(state.getEncounter()).boss() != null)
				fail("首领战只能认输，不能撤退");
			return;
		}
		var selected = skill(action);
		if (selected != null && selected.hasEffect("AREA")) {
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
			if (actor.isHero() || target.isEnemy() != actor.isEnemy() || target.isHero()
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
				if (state.isPvp() || !target.isEnemy() || !state.isCaptureAllowed() || !resources.room(target))
					fail("当前目标不可捕捉或容量不足");
				if (!rules.acceptsBall(target, balls.find(action)))
					fail("精英需要符合等级的强化封印球");
			}
			else if (target.isEnemy() != actor.isEnemy())
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
