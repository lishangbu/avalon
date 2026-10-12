package io.github.lishangbu.avalon.content.world.model;

import java.util.List;

/** answer只存在服务端内容，不允许将本记录直接作为HTTP读投影返回。 */
public record SceneAction(String id, SceneActionType type, String location, String label, String description,
		List<String> requires, String closedByFact, String npcId, String itemCode, int quantity,
		List<SceneActionChoice> choices, String answer, List<String> answers, String success, String incorrect,
		int minimumActive) {
	public SceneAction {
		requires = requires == null ? List.of() : List.copyOf(requires);
		choices = choices == null ? List.of() : List.copyOf(choices);
		answers = answers == null ? answer == null ? List.of() : List.of(answer) : List.copyOf(answers);
	}

}
