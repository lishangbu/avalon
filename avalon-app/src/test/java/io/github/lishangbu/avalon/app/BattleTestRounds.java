package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.domain.BattleEngine;
import io.github.lishangbu.avalon.battle.domain.BattleEngineResources;
import io.github.lishangbu.avalon.battle.domain.BattleResourceSnapshot;
import io.github.lishangbu.avalon.battle.domain.BattleState;
import io.github.lishangbu.avalon.battle.domain.BattleStateOrder;
import java.util.List;
import java.util.Map;

/** 测试宿主：先运行纯规则，再应用模拟库存；生产引擎不提供资产回调执行入口。 */
final class BattleTestRounds {

	private BattleTestRounds() {
	}

	/** 无补位场景使用空补位表，所有测试均调用唯一的纯结算接口。 */
	static void resolve(BattleEngine engine, BattleState state, List<BattleStateOrder> orders, boolean surrender,
			BattleEngineResources resources) {
		resolve(engine, state, orders, surrender, resources, Map.of());
	}

	/** 模拟应用层在规则成功后提交完整状态，并按资源结果更新测试库存。 */
	static void resolve(BattleEngine engine, BattleState state, List<BattleStateOrder> orders, boolean surrender,
			BattleEngineResources resources, Map<String, Integer> replacements) {
		var result = engine.advance(state, orders, surrender, BattleResourceSnapshot.read(state, orders, resources),
				replacements);
		state.copyFrom(result.state());
		for (var change : result.changes()) {
			if (change.kind().equals("CONSUME")) {
				var actor = state.getUnits()
					.stream()
					.filter(unit -> unit.getId().equals(change.owner()))
					.findFirst()
					.orElseThrow();
				resources.take(actor, change.reference());
			}
			else if (change.kind().equals("CAPTURE")) {
				var target = state.getUnits()
					.stream()
					.filter(unit -> unit.getId().equals(change.target()))
					.findFirst()
					.orElseThrow();
				resources.capture(target);
			}
			else {
				throw new IllegalStateException("未定义的测试资产结果：" + change.kind());
			}
		}
	}

}
