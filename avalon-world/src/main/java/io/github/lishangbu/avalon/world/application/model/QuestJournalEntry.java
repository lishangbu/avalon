package io.github.lishangbu.avalon.world.application.model;

import io.github.lishangbu.avalon.content.quest.QuestRulesCatalog;
import java.util.List;

/** 按整项任务组织记录；只包含已开启或已完成的故事，避免提前暴露后续任务。 */
public record QuestJournalEntry(String id, String title, QuestRulesCatalog.Type type, List<QuestRulesCatalog.Tag> tags,
		QuestRulesCatalog.RepeatPolicy repeatPolicy, boolean completed, int completedSteps, int totalSteps,
		List<Step> steps, String status, boolean abandonable, long revision) {

	/** 步骤状态由服务器回执派生；地点名称仅用于展示，不能作为交付授权。 */
	public record Step(String id, String title, String location, boolean completed) {
	}

}
