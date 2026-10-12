package io.github.lishangbu.avalon.battle.application.query;

import io.github.lishangbu.avalon.battle.application.model.BattleViewAction;
import io.github.lishangbu.avalon.battle.application.model.BattleViewLoot;
import io.github.lishangbu.avalon.battle.application.model.BattleViewUnitView;
import io.github.lishangbu.avalon.battle.application.model.DungeonCombatView;
import io.github.lishangbu.avalon.battle.application.model.DungeonCombatViewMember;
import io.github.lishangbu.avalon.battle.application.model.DungeonCombatViewUnit;
import io.github.lishangbu.avalon.battle.domain.BattleEngine;
import io.github.lishangbu.avalon.battle.domain.BattleState;
import io.github.lishangbu.avalon.battle.domain.BattleStateUnit;
import io.github.lishangbu.avalon.battle.domain.PoisonPolicy;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import io.github.lishangbu.avalon.content.battle.DungeonCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.inventory.application.query.DungeonSupplyQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 先验证进度归属再读取战局；页面刷新只投影，不代替 内存调度器 结算到期回合。 */
@Component
@RequiredArgsConstructor
public class DungeonCombatQueryService {

	private final DungeonRunQueryService runs;

	private final DungeonBattleQueryService battles;

	private final CharacterQueryService characters;

	private final DungeonSupplyQueryService supplies;

	private final DungeonCatalog dungeons;

	private final BattleCatalog skills;

	private final ItemCatalog items;

	private final BattleEngine engine;

	@Transactional
	public DungeonCombatView view(long run, long battle, long owner) {
		var progress = runs.restore(run, owner);
		String hero = characters.requireId(owner).toString();
		// 退出历史用于防重，不赋予离队者观看后来补员战局的资格。
		if (progress.getMembers().get(hero).isExited())
			throw new BusinessException(404, "不在此副本进度中");
		if (!Objects.equals(progress.getActiveBattle(), battle) && !progress.getResolvedBattles().containsKey(battle))
			throw new BusinessException(404, "副本战局不存在");
		var state = battles.restore(battle, Long.parseLong(hero));
		var self = state.getMembers().get(hero);
		if (self == null)
			throw new BusinessException(404, "副本战局不存在");
		var combat = state.getBattle();
		var available = supplies.available(run, owner);
		boolean canSubmit = combat.getStatus().equals("ACTIVE") && !self.isRemoved() && !self.isLeaving()
				&& !state.getSubmissions().containsKey(hero) && Instant.now().isBefore(state.getDeadline());
		var units = combat.getUnits().stream().map(unit -> {
			String controller = unit.isEnemy() ? null : unit.isHero() ? unit.getId() : unit.getOwnerHero();
			boolean owned = hero.equals(controller);
			var detail = new BattleViewUnitView(unit.getId(), unit.getName(), unit.getElement(), unit.getLevel(),
					unit.isEnemy(), unit.isHero(), unit.getPosition(), unit.getHp(), unit.getMp(), unit.getStats(),
					false, unit.isEnemy() ? null : unit.getAbility(), unit.getShield(),
					Math.max(0, unit.getSlowUntil() - combat.getRound() + 1), effects(unit, combat.getRound()),
					owned && canSubmit && unit.alive() ? actions(unit, combat, available) : List.of(), List.of(), null);
			return new DungeonCombatViewUnit(controller, owned && canSubmit && unit.alive(), detail);
		}).toList();
		var members = state.getMembers()
			.entrySet()
			.stream()
			.map(entry -> new DungeonCombatViewMember(entry.getKey(),
					state.getSubmissions().containsKey(entry.getKey()), entry.getValue().isRemoved(),
					entry.getValue().isLeaving(), entry.getValue().getMissedRounds()))
			.toList();
		return new DungeonCombatView(Long.toString(battle), Long.toString(run), combat.getRound(), combat.getStatus(),
				dungeons.stage(combat.getDungeonStage()).name(), state.getDeadline(), hero, canSubmit, members, units,
				List.copyOf(combat.getLog()), available, combat.getTelegraph(),
				combat.getLoot()
					.stream()
					.filter(loot -> loot.owner() == owner)
					.map(loot -> new BattleViewLoot(loot.code(), items.item(loot.code()).name(), loot.quantity()))
					.toList());
	}

	/** 副本物资来自入本登记；不混入普通背包，也不提供捕捉或个人撤退指令。 */
	private List<BattleViewAction> actions(BattleStateUnit unit, BattleState state, Map<String, Integer> available) {
		var result = new ArrayList<BattleViewAction>();
		result.add(new BattleViewAction("ATTACK", "普通攻击", "ENEMY", 0, 0, true));
		result.add(new BattleViewAction("DEFEND", "防御", "SELF", 0, 0, true));
		for (String name : unit.getSkills()) {
			var skill = skills.skill(name);
			result.add(new BattleViewAction(name, name, skill.target(), engine.manaCost(unit, skill),
					Math.max(0, unit.getReady().getOrDefault(name, 0) - state.getRound()),
					engine.available(unit, skill, state.getRound())));
		}
		if (unit.isHero()) {
			for (String code : dungeons.definition().supplies())
				result.add(new BattleViewAction(code, items.item(code).name(), "ALLY", 0, 0,
						available.getOrDefault(code, 0) > 0));
		}
		else {
			boolean reserve = state.getUnits()
				.stream()
				.anyMatch(candidate -> !candidate.isEnemy() && !candidate.isHero()
						&& Objects.equals(candidate.getOwnerHero(), unit.getOwnerHero()) && candidate.alive()
						&& candidate.getPosition() >= 2 && candidate.getPosition() < 4);
			result.add(new BattleViewAction("SWAP", "换入替补", "RESERVE", 0, 0, reserve));
		}
		return List.copyOf(result);
	}

	private List<String> effects(BattleStateUnit unit, int round) {
		var result = new ArrayList<String>();
		if (unit.getPoisonUntil() >= round && unit.alive())
			result.add("中毒 " + (unit.getPoisonUntil() - Math.max(round, unit.getPoisonStarted() + 1) + 1) + "跳，预计每跳 "
					+ PoisonPolicy.damage(unit, skills) + "伤害");
		if (unit.getWallUntil() >= round)
			result.add("盾墙 " + (unit.getWallUntil() - round + 1) + "回合");
		if (unit.getRenewUntil() >= round && unit.getRenewAmount() > 0)
			result.add("恢复 " + (unit.getRenewUntil() - round + 1) + "回合");
		if (unit.getMarks().values().stream().anyMatch(mark -> mark.until() >= round))
			result.add("猎人印记");
		return List.copyOf(result);
	}

}
