package io.github.lishangbu.avalon.battle.domain;

import io.github.lishangbu.avalon.platform.BusinessException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 双方锁定指令后同时结算；超时未提交者本轮不行动，连续离线不会阻止对手结算。 */
@Component
@RequiredArgsConstructor
public class PvpRoundCoordinator {

	public static final int ROUND_SECONDS = 60;

	private final BattleEngine engine;

	public PvpRoundState open(BattleState battle, Instant now) {
		if (!battle.isPvp() || !"ACTIVE".equals(battle.getStatus()) || battle.getDungeonStage() != null
				|| battle.getTraining() != null)
			throw new IllegalArgumentException("需要独立玩家对战战局");
		var heroes = battle.getUnits().stream().filter(BattleStateUnit::isHero).toList();
		if (heroes.size() != 2 || heroes.getFirst().isEnemy() == heroes.getLast().isEnemy()
				|| battle.getUnits().stream().map(BattleStateUnit::getId).distinct().count() != battle.getUnits()
					.size())
			throw new IllegalArgumentException("玩家对战需要两个不同阵营人物及唯一单位编号");
		for (var unit : battle.getUnits())
			if (!unit.isHero() && heroes.stream()
				.noneMatch(hero -> hero.getId().equals(unit.getOwnerHero()) && hero.isEnemy() == unit.isEnemy()))
				throw new IllegalArgumentException("玩家对战伙伴归属无效");
		battle.setCaptureAllowed(false);
		engine.planEnemies(battle);
		var state = new PvpRoundState();
		state.setBattle(battle);
		state.setDeadline(now.plusSeconds(ROUND_SECONDS));
		return state;
	}

	public void submit(PvpRoundState state, String hero, int round, DungeonRoundStateSubmission input, Instant now,
			BattleEngineResources resources) {
		requireHero(state, hero);
		if (!"ACTIVE".equals(state.getBattle().getStatus()) || state.getBattle().getRound() != round || input == null)
			fail("对战回合已变化");
		var prior = state.getSubmissions().get(hero);
		if (prior != null) {
			if (!prior.equals(input))
				fail("本轮行动已锁定");
			return;
		}
		if (!now.isBefore(state.getDeadline()))
			fail("本轮提交时间已结束");
		engine.validateMember(BattleStateCodec.copy(state.getBattle()), hero, input.orders(), input.replacements(),
				resources);
		state.getSubmissions().put(hero, input);
	}

	public DungeonRoundResult settle(PvpRoundState state, Instant now, BattleEngineResources resources) {
		var battle = state.getBattle();
		if (!"ACTIVE".equals(battle.getStatus())
				|| state.getSubmissions().size() < 2 && now.isBefore(state.getDeadline()))
			return new DungeonRoundResult(false, null);
		var orders = new ArrayList<BattleStateOrder>();
		var replacements = new LinkedHashMap<String, Integer>();
		for (var hero : battle.getUnits().stream().filter(BattleStateUnit::isHero).toList()) {
			var input = state.getSubmissions().get(hero.getId());
			if (input != null) {
				orders.addAll(input.orders());
				replacements.putAll(input.replacements());
			}
			else
				battle.getUnits()
					.stream()
					.filter(unit -> owned(unit, hero.getId()) && unit.active() && unit.alive())
					.forEach(unit -> orders.add(new BattleStateOrder(unit.getId(), "NO_ACTION", null)));
		}
		// 上轮出战全部倒下后仅给当前窗口一次补位机会。
		boolean friendlyMissing = missingReplacement(battle, false, replacements);
		boolean hostileMissing = missingReplacement(battle, true, replacements);
		if (friendlyMissing || hostileMissing) {
			battle.setStatus(friendlyMissing && hostileMissing ? "DRAW" : friendlyMissing ? "LOST" : "WON");
			battle.getLog().add("出战单位全部倒下且未补位，对战结束。");
			state.getSubmissions().clear();
			return new DungeonRoundResult(true, null);
		}
		var transition = engine.advance(battle, orders, false, BattleResourceSnapshot.read(battle, orders, resources),
				replacements);
		battle.copyFrom(transition.state());
		state.getSubmissions().clear();
		state.setDeadline(now.plusSeconds(ROUND_SECONDS));
		return new DungeonRoundResult(true, transition);
	}

	public void surrender(PvpRoundState state, String hero) {
		var unit = requireHero(state, hero);
		if (!"ACTIVE".equals(state.getBattle().getStatus()))
			return;
		state.getBattle().setStatus(unit.isEnemy() ? "WON" : "LOST");
		state.getBattle().getLog().add(unit.getName() + "认输了，返回复活点。");
		state.getSubmissions().clear();
	}

	private boolean missingReplacement(BattleState battle, boolean enemy, java.util.Map<String, Integer> replacements) {
		return battle.getUnits().stream().noneMatch(unit -> unit.isEnemy() == enemy && unit.active() && unit.alive())
				&& battle.getUnits()
					.stream()
					.noneMatch(unit -> unit.isEnemy() == enemy && replacements.containsKey(unit.getId()));
	}

	private BattleStateUnit requireHero(PvpRoundState state, String hero) {
		return state.getBattle()
			.getUnits()
			.stream()
			.filter(unit -> unit.isHero() && unit.getId().equals(hero))
			.findFirst()
			.orElseThrow(() -> new BusinessException(404, "你不在本场对战中"));
	}

	private boolean owned(BattleStateUnit unit, String hero) {
		return Objects.equals(hero, unit.isHero() ? unit.getId() : unit.getOwnerHero());
	}

	private void fail(String message) {
		throw new BusinessException(409, message);
	}

}
