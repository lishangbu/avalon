package io.github.lishangbu.avalon.battle.domain;

/**
 * 客户端只提交行动与目标，不能携带伤害、概率或随机数。
 */
public record BattleStateMark(int until, int percent) {
}
