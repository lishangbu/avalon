package io.github.lishangbu.avalon.battle.domain;

/**
 * 多人提交窗口的结算结果。未到期且未收齐时没有回合结果；全体倒下直接判负时也没有规则动作及资产变更。 实际回合结果保留退出前的行动单位，供宿主在成员移除后仍能定位消耗归属。
 */
public record DungeonRoundResult(boolean changed, BattleRoundResult round) {
}
