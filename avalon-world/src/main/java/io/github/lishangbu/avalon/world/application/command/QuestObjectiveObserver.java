package io.github.lishangbu.avalon.world.application.command;

import io.github.lishangbu.avalon.character.application.model.CharacterActionRecorded;
import io.github.lishangbu.avalon.quest.model.QuestEvent;
import io.github.lishangbu.avalon.world.quest.QuestRuntimeService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 真实行为仅进入实例引擎，资格、事件防重和数量全部由统一状态管理。 */
@Component
@RequiredArgsConstructor
public class QuestObjectiveObserver {

	private final QuestRuntimeService runtime;

	@EventListener
	@Transactional(Transactional.TxType.MANDATORY)
	public void recorded(CharacterActionRecorded event) {
		if (event.owner() > 0)
			runtime.record(event.owner(), new QuestEvent(event.eventId(), event.code(), event.amount(), event.at()));
	}

}
