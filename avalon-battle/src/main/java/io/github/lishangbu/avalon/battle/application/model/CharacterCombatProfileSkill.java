package io.github.lishangbu.avalon.battle.application.model;

/** 冷却按完整后续回合计数，和战斗配置保持同一语义。 */
public record CharacterCombatProfileSkill(String name, String element, int mana, int cooldown) {
}
