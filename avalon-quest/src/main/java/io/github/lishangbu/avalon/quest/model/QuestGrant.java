package io.github.lishangbu.avalon.quest.model;

/** 来源含实例、轮次、步骤与效果序号，避免重复任务被宿主的一次性奖励账本误判为重复。 */
public record QuestGrant(String source, QuestEffect effect) {
}
