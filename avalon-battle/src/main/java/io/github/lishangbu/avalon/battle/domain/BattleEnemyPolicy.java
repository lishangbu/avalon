package io.github.lishangbu.avalon.battle.domain;

import java.util.Random;
import java.util.function.LongFunction;

/** 敌方玩法扩展：目录唯一匹配策略，决策在上一回合关闭时锁定，行动与阶段结束由同一策略处理。 */
public interface BattleEnemyPolicy {

	/** 注册代码必须唯一，不受组件扫描顺序影响。 */
	String kind();

	/** 当前战局只能匹配一个策略；零匹配或多匹配均拒绝。 */
	boolean supports(BattleState state);

	/** 保存下一轮意图，不读取尚未提交的玩家指令。 */
	void plan(BattleState state, LongFunction<Random> randoms);

	/** 特殊行动返回true；普通行动返回false，由通用选招规则执行。 */
	boolean execute(BattleEnemyAction action);

	/** 活动战局回合末钩子，终局不额外触发伤害或治疗。 */
	void finish(BattleState state);

	/** 关闭阶段只做资格判定和机制清理，不执行额外跳点。 */
	void close(BattleState state, boolean surrender);

}
