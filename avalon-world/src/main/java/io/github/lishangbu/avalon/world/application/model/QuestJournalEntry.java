package io.github.lishangbu.avalon.world.application.model;

import io.github.lishangbu.avalon.content.quest.model.QuestRepeatPolicy;
import io.github.lishangbu.avalon.content.quest.model.QuestTag;
import io.github.lishangbu.avalon.content.quest.model.QuestType;
import java.util.List;

/** 按整项任务组织记录；只包含已开启或已完成的故事，避免提前暴露后续任务。 */
public record QuestJournalEntry(String id, String title, QuestType type, List<QuestTag> tags,
		QuestRepeatPolicy repeatPolicy, boolean completed, int completedSteps, int totalSteps,
		List<QuestJournalStep> steps, String status, boolean abandonable, long revision) {

}
