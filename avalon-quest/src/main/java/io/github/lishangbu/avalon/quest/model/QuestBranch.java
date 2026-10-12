package io.github.lishangbu.avalon.quest.model;

import static io.github.lishangbu.avalon.quest.model.QuestModelValidation.text;

/** 同一group只能选择一个option；所选路线和所有公共步骤共同决定任务完成。 */
public record QuestBranch(String group, String option) {
	public QuestBranch {
		text(group, "分支组");
		text(option, "分支选项");
	}
}
