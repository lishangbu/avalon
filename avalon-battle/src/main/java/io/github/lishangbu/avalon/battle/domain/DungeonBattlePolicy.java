package io.github.lishangbu.avalon.battle.domain;

import io.github.lishangbu.avalon.content.battle.model.BattleSkillEffect;
import io.github.lishangbu.avalon.content.battle.DungeonCatalog;
import io.github.lishangbu.avalon.content.battle.model.BattleSkill;
import io.github.lishangbu.avalon.content.battle.model.DungeonAction;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import static io.github.lishangbu.avalon.battle.domain.BattleState.*;

/** 副本阶段仅负责敌人与机制行动，伤害、速度、保护和护盾继续使用通用战斗引擎。 */
@Component
@RequiredArgsConstructor
public class DungeonBattlePolicy {

	private final DungeonCatalog catalog;

	private final CreatureCatalog creatures;

	private final BattleRules rules;

	private final DirectAttackResolver attacks;

	/** 调用方传入已锁定的入本成员及实际资源；这里不恢复生命、不重新组织编队。 */
	public BattleState create(String stageId, List<BattleStateUnit> party, long seed) {
		var stage = catalog.stage(stageId);
		var state = new BattleState();
		state.setSeed(seed);
		state.setEncounter(stageId);
		state.setDungeonStage(stageId);
		state.setUnits(new ArrayList<>(party));
		int level = catalog.definition().level();
		if (stage.wildSpecies().isEmpty()) {
			var enemy = enemy("dungeon-boss", stage.name(), level, new BattleStateStats(stage.health(), 0,
					stage.attack(), stage.attack(), stage.defense(), stage.defense(), stage.speed()));
			enemy.setElement("地");
			state.getUnits().add(enemy);
			if (stage.guardHealth() > 0) {
				var guard = enemy("dungeon-guard", "护栏兽", level, new BattleStateStats(stage.guardHealth(), 0,
						stage.attack(), 0, stage.guardDefense(), stage.guardDefense(), stage.speed()));
				guard.setMechanism(true);
				state.getUnits().add(guard);
			}
		}
		else {
			for (int i = 0; i < stage.wildSpecies().size(); i++) {
				var species = creatures.pet(stage.wildSpecies().get(i));
				var base = rules.stats(species.id(), level, false, Map.of());
				var enemy = enemy("dungeon-enemy-" + i, species.name(), level,
						new BattleStateStats((int) Math.floor(base.health() * stage.healthMultiplier()), base.mana(),
								base.physicalAttack(), base.magicAttack(), base.physicalDefense(), base.magicDefense(),
								base.speed()));
				enemy.setSpecies(species.id());
				enemy.setElement(species.element());
				enemy.setAbilitySlot(species.abilitySlot());
				enemy.setAbility(species.ability());
				enemy.setSkills(species.skills());
				state.getUnits().add(enemy);
			}
		}
		return state;
	}

	private BattleStateUnit enemy(String id, String name, int level, BattleStateStats stats) {
		var enemy = new BattleStateUnit();
		enemy.setId(id);
		enemy.setName(name);
		enemy.setEnemy(true);
		enemy.setLevel(level);
		enemy.setStats(stats);
		enemy.setHp(stats.health());
		enemy.setMp(stats.mana());
		return enemy;
	}

	public boolean scripted(BattleState state) {
		return state.getDungeonStage() != null && !catalog.stage(state.getDungeonStage()).actions().isEmpty();
	}

	private DungeonAction action(BattleState state) {
		var sequence = catalog.stage(state.getDungeonStage()).actions();
		return sequence.get((state.getRound() - 1) % sequence.size());
	}

	/** 目标在上一轮结算后固定，不能根据玩家本轮输入重新点名。 */
	public void plan(BattleState state, Random random) {
		var action = action(state);
		var targets = state.getUnits()
			.stream()
			.filter(unit -> !unit.isEnemy() && unit.active() && unit.alive())
			.filter(unit -> !action.kind().equals("HEAVY") || unit.isHero())
			.toList();
		String target = targets.isEmpty() ? null : targets.get(random.nextInt(targets.size())).getId();
		state.setEnemyOrders(List.of(new BattleStateOrder("dungeon-boss", "DUNGEON_" + action.kind(), target)));
		state.setTelegraph(
				action.notice() + (target == null ? ""
						: " 目标：" + targetName(state,
								targets.stream().filter(u -> u.getId().equals(target)).findFirst().orElseThrow())
								+ "。"));
	}

	/** 同名伙伴在组队中很常见；公开主人名称，避免点名预告让多个玩家误判。 */
	private String targetName(BattleState state, BattleStateUnit target) {
		if (target.isHero())
			return target.getName();
		return state.getUnits()
			.stream()
			.filter(unit -> unit.isHero() && unit.getId().equals(target.getOwnerHero()))
			.findFirst()
			.map(owner -> owner.getName() + "的" + target.getName())
			.orElse(target.getName());
	}

	/** 已预告的目标退场时不转移伤害；连续扑击逐次结算护盾、援护和倒下状态。 */
	public void execute(BattleState state, BattleStateUnit actor, BattleStateUnit target, double random,
			Set<String> defending, Map<String, List<DirectAttackResolverProtection>> protections) {
		var action = action(state);
		if (action.kind().equals("COMMAND")) {
			state.getLog().add(actor.getName() + "向护兽发出号令。");
			return;
		}
		if (action.kind().equals("BEAST")) {
			var guard = state.getUnits()
				.stream()
				.filter(u -> u.getId().equals("dungeon-guard") && u.alive())
				.findFirst();
			if (guard.isEmpty()) {
				state.getLog().add("护兽已被压制，强化扑击未能发动。");
				return;
			}
			actor = guard.get();
		}
		if (target == null || !target.alive() || !target.active()) {
			state.getLog().add("预告目标已退场，本次攻击没有转向其他成员。");
			return;
		}
		var skill = new BattleSkill(action.kind().equals("POUNCE") ? "连续扑击" : "兽栏攻击",
				List.of(new BattleSkillEffect("DAMAGE", action.power())), "无", "PA", "PD", 0, 0, 0, "ENEMY", null,
				null);
		int hits = action.kind().equals("POUNCE") ? 2 : 1;
		for (int i = 0; i < hits && target.alive() && target.active(); i++)
			attacks.apply(state, actor, List.of(target), skill, false, random, defending, protections);
	}

	/** 首领败北立即清除护兽，护兽不额外产生掉落或阻止通关。 */
	public void finish(BattleState state) {
		if (state.getStatus().equals("WON"))
			state.getUnits().removeIf(BattleStateUnit::isMechanism);
		if (!state.getStatus().equals("ACTIVE"))
			state.setTelegraph(null);
	}

}
