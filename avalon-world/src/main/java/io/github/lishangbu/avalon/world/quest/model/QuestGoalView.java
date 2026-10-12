package io.github.lishangbu.avalon.world.quest.model;

import io.github.lishangbu.avalon.quest.model.QuestMeasure;

/** 目标数量和资格由服务器计算，浏览器只渲染玩家可读名称与进度。 */
public record QuestGoalView(String step, String title, String id, QuestMeasure measure, long value, long amount) {
}
