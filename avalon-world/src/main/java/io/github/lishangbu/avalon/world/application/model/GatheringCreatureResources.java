package io.github.lishangbu.avalon.world.application.model;

/** 派遣前的只读资源快照，不推进采集或恢复资源。 */
public record GatheringCreatureResources(int hp, int mp, int health, int mana) {
}
