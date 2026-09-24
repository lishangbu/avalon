package io.github.lishangbu.avalon.world.application.command;

import io.github.lishangbu.avalon.character.application.model.CharacterArrived;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 到达奖励由移动事务触发；普通读地图不会写进度，重访通过原任务回执防止重复发奖。 */
@Component
@RequiredArgsConstructor
public class ArrivalQuestObserver {

	private final QuestCatalog catalog;

	private final CharacterFactQueryService facts;

	private final QuestCommandService quests;

	@Transactional(Transactional.TxType.MANDATORY)
	@EventListener
	public void arrived(CharacterArrived event) {
		var completed = facts.codes(event.owner());
		for (var step : catalog.definition().steps()) {
			if (step.onArrival() && step.location().equals(event.location())
					&& !completed.contains("QUEST:" + step.id())
					&& step.requires().stream().allMatch(id -> completed.contains("QUEST:" + id))
					&& completed.containsAll(step.facts()))
				quests.interact(event.owner(), step.id());
		}
	}

}
