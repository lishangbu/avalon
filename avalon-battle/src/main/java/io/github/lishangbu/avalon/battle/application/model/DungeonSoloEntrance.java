package io.github.lishangbu.avalon.battle.application.model;

/** 当前地点的单人副本入口；可见性与服务端实际准入分别校验。 */
public record DungeonSoloEntrance(String id, String name, int level, boolean unlocked, int remainingStarts) {
}
