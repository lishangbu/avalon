package io.github.lishangbu.avalon.quest.model;

import static io.github.lishangbu.avalon.quest.model.QuestModelValidation.text;

/** 同一group只能选择一个option；所选路线和所有公共步骤共同决定任务完成。 */
public record QuestBranch(String group, String option, QuestBranchMode mode) {
	/** 既有内容缺省为互斥路线，避免将旧选择静默变为可切换方法。 */
	public QuestBranch {
		text(group, "分支组");
		text(option, "分支选项");
		mode = mode == null ? QuestBranchMode.EXCLUSIVE : mode;
	}

	public QuestBranch(String group, String option) {
		this(group, option, QuestBranchMode.EXCLUSIVE);
	}
}
