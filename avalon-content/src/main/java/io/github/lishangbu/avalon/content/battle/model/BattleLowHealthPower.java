package io.github.lishangbu.avalon.content.battle.model;

/** 在实际受击者生命不高于百分比阈值时使用替代威力，不读取提交时的旧生命。 */
public record BattleLowHealthPower(int percent, int power) {
}
