package io.github.lishangbu.avalon.app;

/** 实例目标断言所需的步骤及目标身份。 */
public record QuestProgressGoal(String step, String fact) {
	public String getStep() {
		return step;
	}

	public String getFact() {
		return fact;
	}
}
