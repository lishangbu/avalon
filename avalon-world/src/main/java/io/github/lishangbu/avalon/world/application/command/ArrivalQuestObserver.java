package io.github.lishangbu.avalon.world.application.command;

import io.github.lishangbu.avalon.character.application.model.CharacterArrived;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
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

	private final CharacterFactCommandService factCommands;

	@Transactional(Transactional.TxType.MANDATORY)
	@EventListener
	public void arrived(CharacterArrived event) {
		// 到访目标来自实际移动，同一地点在不同任务阶段需重新经过；读地图不登记里程。
		String visit = "VISIT:" + event.location();
		if (catalog.definition()
			.steps()
			.stream()
			.anyMatch(step -> step.objectives().stream().map(objective -> objective.code()).toList().contains(visit)))
			factCommands.record(event.owner(), visit);
	}

}
