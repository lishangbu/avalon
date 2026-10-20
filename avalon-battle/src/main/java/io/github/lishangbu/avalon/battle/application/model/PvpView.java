package io.github.lishangbu.avalon.battle.application.model;

import java.time.Instant;

/** 只公开提交是否锁定，不公开对方未结算指令。 */
public record PvpView(BattleView battle, boolean submitted, boolean opponentSubmitted, Instant deadline) {
}
