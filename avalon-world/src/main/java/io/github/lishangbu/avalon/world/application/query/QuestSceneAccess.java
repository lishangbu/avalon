package io.github.lishangbu.avalon.world.application.query;

import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.world.SceneActionCatalog;
import io.github.lishangbu.avalon.world.persistence.entity.QuestParticipation;
import io.github.lishangbu.avalon.world.persistence.repository.QuestParticipationRepository;
import io.github.lishangbu.avalon.world.persistence.repository.QuestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/** 任务专属场景在接取后开放，连同中间采集、照护动作一起保护，避免提前消耗一次性行动。 */
@Component
@RequiredArgsConstructor
public class QuestSceneAccess {

	private final QuestCatalog quests;

	private final SceneActionCatalog scenes;

	private final QuestParticipationRepository participation;

	private final QuestRepository receipts;

	/** 一次读取参与状态和步骤回执，为整个地点列表生成可执行动作集合。 */
	public Set<String> available(long characterId) {
		var active = participation.findByCharacterId(characterId)
			.stream()
			.filter(state -> state.getStatus() == QuestParticipation.Status.ACTIVE)
			.map(QuestParticipation::getQuestCode)
			.collect(Collectors.toSet());
		var done = receipts.done(characterId);
		Set<String> linked = new HashSet<>();
		Set<String> allowed = new HashSet<>();
		for (var step : quests.definition().steps()) {
			Set<String> dependencies = new HashSet<>();
			step.facts().forEach(fact -> collect(fact, dependencies));
			linked.addAll(dependencies);
			if (active.contains(step.quest()) && !done.contains(step.id()) && done.containsAll(step.requires()))
				allowed.addAll(dependencies);
		}
		// 不属于任何任务目标的普通场景继续遵守自身目录条件，不强行要求接任务。
		scenes.actions()
			.stream()
			.map(SceneActionCatalog.Action::id)
			.filter(id -> !linked.contains(id))
			.forEach(allowed::add);
		return Set.copyOf(allowed);
	}

	private void collect(String fact, Set<String> result) {
		if (!fact.startsWith("SCENE:"))
			return;
		String id = fact.substring(6);
		if (!result.add(id))
			return;
		var action = scenes.action(id);
		if (action != null)
			action.requires().forEach(required -> collect(required, result));
	}

}
