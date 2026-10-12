package io.github.lishangbu.avalon.battle.application.model;

/** 库存独立于当前捕捉资格；等级不满足或没有可捕目标时仍可查看持有数量。 */
public record BattleViewCaptureSupply(String code, String name, int level, int quantity) {
}
