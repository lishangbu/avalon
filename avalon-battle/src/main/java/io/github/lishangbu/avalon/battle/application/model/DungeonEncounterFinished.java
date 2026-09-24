package io.github.lishangbu.avalon.battle.application.model;

import io.github.lishangbu.avalon.battle.domain.DungeonProgressState;
import io.github.lishangbu.avalon.battle.domain.DungeonRoundState;

/** 同事务内部结算通知；收件方不得提交子事务，奖励失败须回滚回合及进度。 */
public record DungeonEncounterFinished(long run, long battle, DungeonProgressState progress, DungeonRoundState result) {
}
