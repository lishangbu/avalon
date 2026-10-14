package io.github.lishangbu.avalon.battle.domain;

import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import java.util.ArrayList;
import java.util.Random;
import java.util.function.LongFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 野外及副本普通敌人使用统一选招；脚本首领、训练由各自策略独立处理。 */
@Component
@RequiredArgsConstructor
public class WildEnemyPolicy implements BattleEnemyPolicy {

	/** 只读遭遇配置用于区分单人首领。 */
	private final BattleCatalog catalog;

	/** 普通敌人技能选择规则，不读取数据库。 */
	private final WildActionPolicy actions;

	/** 副本目录判断当前阶段是否采用脚本。 */
	private final DungeonBattlePolicy dungeon;

	public String kind() {
		return "WILD";
	}

	public boolean supports(BattleState state) {
		return state.getTraining() == null && !dungeon.scripted(state)
				&& (state.getDungeonStage() != null || catalog.encounter(state.getEncounter()).boss() == null);
	}

	/** 维持原有目标抽样次序，固定随机流确保重试和复现一致。 */
	public void plan(BattleState state, LongFunction<Random> randoms) {
		var random = randoms.apply(state.getSeed() ^ ((long) state.getRound() << 32));
		var orders = new ArrayList<BattleStateOrder>();
		for (var enemy : state.getUnits()) {
			if (!enemy.isEnemy() || !enemy.alive() || enemy.isMechanism())
				continue;
			var targets = state.getUnits()
				.stream()
				.filter(unit -> !unit.isEnemy() && unit.active() && unit.alive())
				.toList();
			if (targets.isEmpty())
				targets = state.getUnits().stream().filter(unit -> !unit.isEnemy() && unit.alive()).toList();
			if (targets.isEmpty())
				break;
			var target = targets.get(random.nextInt(targets.size()));
			orders.add(actions.choose(state, enemy, target));
		}
		state.setEnemyOrders(orders);
	}

	/** 普通敌人使用通用技能处理器，不绕过费用与冷却规则。 */
	public boolean execute(BattleEnemyAction action) {
		return false;
	}

	/** 普通遭遇没有额外回合末脚本。 */
	public void finish(BattleState state) {
		if (state.getDungeonStage() != null)
			dungeon.finish(state);
	}

	/** 普通遭遇没有额外终局资格或附属机制。 */
	public void close(BattleState state, boolean surrender) {
		if (state.getDungeonStage() != null && !state.getStatus().equals("ACTIVE"))
			dungeon.finish(state);
	}

}
