package io.github.lishangbu.avalon.world.application.query;

import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.world.persistence.repository.QuestObjectiveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 永久掌握的功能与任务期间的行动分开判定；查询不会从历史行为补发任务进度。 */
@Component
@RequiredArgsConstructor
public class QuestObjectiveQueryService {

	private final QuestObjectiveRepository objectives;

	public Map<String, Set<String>> progress(long characterId) {
		return objectives.findByCharacterId(characterId)
			.stream()
			.collect(Collectors.groupingBy(value -> value.getStep(),
					Collectors.mapping(value -> value.getFact(), Collectors.toSet())));
	}

	/** 已掌握的界面操作和技能无需为新任务重学；到访、战斗、捕获及课程实践必须是有效任务行为。 */
	public static boolean historical(String fact) {
		return fact.startsWith("ACTION:") || fact.startsWith("SKILL:");
	}

	public static Set<String> factsFor(QuestCatalog.Step step, Set<String> permanent,
			Map<String, Set<String>> progress) {
		Set<String> result = permanent.stream()
			.filter(QuestObjectiveQueryService::historical)
			.collect(Collectors.toCollection(HashSet::new));
		result.addAll(progress.getOrDefault(step.id(), Set.of()));
		return result;
	}

}
