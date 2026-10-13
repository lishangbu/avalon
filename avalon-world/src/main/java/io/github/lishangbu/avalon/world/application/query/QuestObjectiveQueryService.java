package io.github.lishangbu.avalon.world.application.query;

import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.quest.model.QuestContentStep;
import io.github.lishangbu.avalon.quest.QuestInstance;
import io.github.lishangbu.avalon.quest.model.QuestMeasure;
import io.github.lishangbu.avalon.world.persistence.repository.QuestRuntimeInstanceRepository;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 从实例数量进度生成指引读模型，永久目标按元数据中的 measure 判定。 */
@Component
@RequiredArgsConstructor
public class QuestObjectiveQueryService {

	private final QuestRuntimeInstanceRepository instances;

	private final QuestCatalog catalog;

	private final ObjectMapper json;

	public Map<String, Set<String>> progress(long characterId) {
		Map<String, Set<String>> result = new HashMap<>();
		for (var row : instances.findByCharacterIdOrderByAcceptedAtAscAttemptAscIdAsc(characterId)) {
			var instance = json.readValue(row.getState(), QuestInstance.class);
			for (var step : catalog.definition().steps()) {
				if (!step.quest().equals(instance.quest()))
					continue;
				Set<String> completed = new HashSet<>();
				for (var goal : step.objectives())
					if (goal.measure() == QuestMeasure.EVENT
							&& instance.progress().getOrDefault(step.id(), Map.of()).getOrDefault(goal.id(), 0L) >= goal
								.amount())
						completed.add(goal.code());
				result.put(step.id(), completed);
			}
		}
		return result;
	}

	public static Set<String> factsFor(QuestContentStep step, Set<String> permanent,
			Map<String, Set<String>> progress) {
		Set<String> result = new HashSet<>(progress.getOrDefault(step.id(), Set.of()));
		step.objectives()
			.stream()
			.filter(goal -> goal.measure() == QuestMeasure.HISTORY && permanent.contains(goal.code()))
			.forEach(goal -> result.add(goal.code()));
		return result;
	}

}
