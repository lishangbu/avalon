package io.github.lishangbu.avalon.battle.application.model;

/** 准备页只展示人物及四只拟出战伙伴；生命魔力来自实际存档而非假定满值。 */
public record DungeonLobbyViewUnit(String id, String name, int level, boolean hero, int position, int health,
		int maxHealth, int mana, int maxMana) {
}
