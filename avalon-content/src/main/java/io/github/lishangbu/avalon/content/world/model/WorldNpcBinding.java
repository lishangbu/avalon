package io.github.lishangbu.avalon.content.world.model;

/** 仅使用任务事实驱动的接待位置，不引入自由寻路或全服人物日程。 */
public record WorldNpcBinding(String npcId, String requiredFact, String closedByFact) {
}
