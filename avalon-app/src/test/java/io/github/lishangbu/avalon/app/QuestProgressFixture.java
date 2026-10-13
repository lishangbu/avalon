package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.quest.QuestInstance;
import io.github.lishangbu.avalon.world.persistence.repository.QuestRuntimeInstanceRepository;
import tools.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;

/** 测试直接读取正式实例进度，检查任务行为归属而不维护第二份目标表。 */
@TestComponent
@RequiredArgsConstructor
public class QuestProgressFixture {

	private final QuestRuntimeInstanceRepository instances;

	private final ObjectMapper json;

	public List<QuestProgressGoal> goals(long character) {
		List<QuestProgressGoal> result = new ArrayList<>();
		for (var row : instances.findByCharacterIdOrderByAcceptedAtAscAttemptAscIdAsc(character)) {
			var state = json.readValue(row.getState(), QuestInstance.class);
			state.progress().forEach((step, goals) -> goals.forEach((fact, amount) -> {
				if (amount > 0)
					result.add(new QuestProgressGoal(step, fact));
			}));
		}
		return result;
	}

	public boolean contains(long character, String step, String fact) {
		return goals(character).stream().anyMatch(goal -> goal.step().equals(step) && goal.fact().equals(fact));
	}

}
