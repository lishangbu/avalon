package io.github.lishangbu.avalon.world.quest.model;

import io.github.lishangbu.avalon.quest.QuestInstance;

/** 不可变步骤投影只供页面显示，任何写命令仍重新检查服务器状态。 */
public record QuestStepView(QuestInstance instance, boolean available, boolean accepted, boolean ready,
		boolean completed) {
}
