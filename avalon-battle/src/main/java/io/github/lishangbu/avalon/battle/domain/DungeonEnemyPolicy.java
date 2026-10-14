package io.github.lishangbu.avalon.battle.domain;

import java.util.Random;
import java.util.function.LongFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 多人副本脚本的扩展适配；开局意图、脚本行动和机制清理统一归属此策略。 */
@Component
@RequiredArgsConstructor
public class DungeonEnemyPolicy implements BattleEnemyPolicy {

	/** 只操作战局副本的副本机制规则。 */
	private final DungeonBattlePolicy dungeon;

	public String kind() {
		return "DUNGEON";
	}

	public boolean supports(BattleState state) {
		return state.getTraining() == null && dungeon.scripted(state);
	}

	/** 脚本游标决定公开预告，不按玩家选招临时改变意图。 */
	public void plan(BattleState state, LongFunction<Random> randoms) {
		dungeon.plan(state, randoms.apply(state.getSeed() ^ ((long) state.getRound() << 32)));
	}

	/** 每次脚本攻击消耗一次原有伤害浮动抽样。 */
	public boolean execute(BattleEnemyAction action) {
		dungeon.execute(action.state(), action.actor(), action.target(), .9375 + action.random().nextDouble() * .125,
				action.defending(), action.covers());
		return true;
	}

	/** 回合末推进副本脚本。 */
	public void finish(BattleState state) {
		dungeon.finish(state);
	}

	/** 首领在动作中倒下时也清理附属机制，不执行新的回合末伤害。 */
	public void close(BattleState state, boolean surrender) {
		if (!state.getStatus().equals("ACTIVE"))
			dungeon.finish(state);
	}

}
